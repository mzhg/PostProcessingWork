package nv.samples.smoke;

import org.lwjgl.util.vector.Matrix4f;

import jet.opengl.demos.intel.cput.D3D11_INPUT_ELEMENT_DESC;
import jet.opengl.demos.intel.cput.ID3D11InputLayout;
import jet.opengl.demos.intel.va.VaDirectXTools;
import jet.opengl.postprocessing.buffer.BufferGL;
import jet.opengl.postprocessing.common.Disposeable;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.Texture2DDesc;
import jet.opengl.postprocessing.texture.Texture3D;
import jet.opengl.postprocessing.texture.TextureUtils;
import jet.opengl.postprocessing.util.Numeric;

/**
  Arbitrary Mesh Voxelizer.<br>
  Takes as input an arbitrary mesh and matrix transformation and generates a
   voxelized representation of the mesh in the destination texture, where voxels
   outside the object have 0 value and voxels inside have non-zero value.<p></p>

  The matrix transform is the object to voxel-volume transform, i.e. it is the
   object transform with respect to the coordinate system of the voxel-volume,
   which has its origin at 0,0,0.<p></p>

  Proposed Algorithm:<ul>

    <li>StencilClipVolume:
        per-slice stencil XOR or Non-Zero IN/OUT test with moving clip-plane.

 <li>The object is drawn into each slice in the volume, setting an orthogonal
     projection such that the near plane matches the slice in question and the far
     plane is at infinity. The left, right, top and bottom clip planes are set to
     match the volume's side walls.

 <li>A stencil buffer is used in each mesh drawing pass (the same buffer may be used
     for all slices).
     When drawing the mesh the stencil functions are set as follows:
      A) if using the XOR rule, we simply invert the value (STENCIL_OP_INVERT)
      B) if using the Non-Zero rule we increment (STENCIL_OP_INCR) for front faces
      and decrement for back faces  (D3D10_STENCIL_OP_DECR).

 <li>In both cases the end result is that non-zero values are found at those pixels
      of the slice (voxels) that are inside the mesh.

 <li>This works well only for  closed non-manifold (watertight) meshes.
     For non-closed meshes the results will vary depending on the object orientation
     with respect to the volume.
 <li>For example a noncapped cylinder, if aligned such that its axis is
     perpendicular to the slices will yield no voxels inside.
     Conversely, if the axis is parallel to the slices we would get voxels for the
     inside of the same cylinder if it was capped.
 </ul>

  ============================================================================= */

final class Voxelizer implements Disposeable{
    // INTERNAL STATE
    private boolean             m_bInOutEnabled;
    private boolean             m_bVelocityEnabled;
    // Destination state
//    ID3D10Device                *m_pD3DDevice;
    private Texture3D           m_pDstInOutTexture3D;
    private Texture3D           m_pDstInOutTexRTView;
    private Texture3D           m_pDstVelTexture3D;
    private Texture3D           m_pDstVelTexRTView;

    private int                 m_width;
    private int                 m_height;
    private int                 m_depth;

    //  for flat 3D texture
    private int                 m_cols;
    private int                 m_rows;

    // Source state
//    SkinnedMesh                 *m_pSkinnedMesh;
    private int                 m_srcSoBuffer;
    private int                 m_prevSrcSoBuffer;
    private float               m_timeStep =1;
    private float               m_velocityMultiplier = 1;

    private final Matrix4f      m_objToVolumeXForm = new Matrix4f();

    // Other state
    private boolean             m_initialized;

    // The depth-stencil buffer
    private Texture2D           m_pDSTex2D;
    private Texture2D           m_pDSTex2DDSView;
    private Texture2D           m_pDSTex2DSRView;

    // Effect/shader state
//    ID3D10Effect                *m_pVoxEffect;
    private VoxelizerProgram    m_etNZ;
    private VoxelizerProgram    m_etResolveWithPS;
    private VoxelizerProgram    m_etGenVelocityWireframe;

    private ID3D11InputLayout   m_pSOInputLayout;

    /*ID3D10EffectMatrixVariable          *m_evWorldViewProjectionMatrix;
    ID3D10EffectVectorVariable          *m_evProjSpacePixDim;
    ID3D10EffectVectorVariable          *m_evGridDim;
    ID3D10EffectScalarVariable          *m_evRecTimeStep;
    ID3D10EffectScalarVariable          *m_evVelocityMultiplier;
    ID3D10EffectScalarVariable          *m_evSliceIdx;
    ID3D10EffectScalarVariable          *m_evSliceZ;
    ID3D10EffectShaderResourceVariable  *m_evTexture_depthStencil;*/


    // Slices state
    private ID3D11InputLayout   m_pSlicesLayout;
    private BufferGL            m_pSlicesVB;
    private GLFuncProvider      gl;

    public Voxelizer(){

    }

    public void SetDestination(//ID3D10Device *pD3DDevice,
                           Texture3D pDstInOutTexture3D, Texture3D pDstVelocityTexture3D){
//        SAFE_ACQUIRE(m_pD3DDevice, pD3DDevice);
        m_pDstInOutTexture3D = pDstInOutTexture3D;
        m_pDstVelTexture3D =pDstVelocityTexture3D;

        Initialize();
    }

    /*HRESULT Voxelize(D3DXMATRIX& objToVolumeXForm, SkinnedMesh *pMesh,
                     int srcSoBuf, int prevSrcSoBuf, float timeStep);*/

    void SetVelocityMultiplier(float f)     { m_velocityMultiplier = f;     }
    float GetVelocityMultiplier()           { return m_velocityMultiplier;  }
    // enable disable different types of voxelization
    void SetEnableInOutVoxelize(boolean b)     { m_bInOutEnabled = b;      }
    void SetEnableVelocityVoxelize(boolean b)  { m_bVelocityEnabled = b;   }
    boolean GetEnableInOutVoxelize()       { return m_bInOutEnabled;      }
    boolean GetEnableVelocityVoxelize()    { return m_bVelocityEnabled;   }

    private void Initialize(){
        m_initialized = false;
        SAFE_RELEASE(m_pDstInOutTexRTView);
        SAFE_RELEASE(m_pDstVelTexRTView);

        // Assert inputs are valid
//        assert(m_pD3DDevice);
        assert(m_pDstInOutTexture3D != null);

        {
            // Create a rendertarget view for the InOut 3D texture
            /*D3D10_TEXTURE3D_DESC tex3Ddesc;
            m_pDstInOutTexture3D->GetDesc(&tex3Ddesc);
            D3D10_RENDER_TARGET_VIEW_DESC rtDesc;
            rtDesc.Format = tex3Ddesc.Format;
            rtDesc.ViewDimension = D3D10_RTV_DIMENSION_TEXTURE3D;
            rtDesc.Texture3D.MipSlice = 0;
            rtDesc.Texture3D.FirstWSlice = 0;
            rtDesc.Texture3D.WSize = tex3Ddesc.Depth;
            V_RETURN(m_pD3DDevice->CreateRenderTargetView(m_pDstInOutTexture3D, &rtDesc, &m_pDstInOutTexRTView));*/

            m_pDstInOutTexRTView = m_pDstInOutTexture3D;

            // Get witdh, height and depth
            m_width = m_pDstInOutTexture3D.getWidth();
            m_height = m_pDstInOutTexture3D.getHeight();
            m_depth = m_pDstInOutTexture3D.getDepth();

            long value = SmokeDemo.computeRowColsForFlat3DTexture(m_depth/*, &m_cols, &m_rows*/);

            m_cols = Numeric.decodeFirst(value);
            m_cols = Numeric.decodeSecond(value);

        }

        assert((m_width > 0) && (m_height > 0) && (m_depth > 0));
        assert((m_cols > 0) && (m_rows > 0));
        assert((m_cols * m_rows) >= m_depth);

        if( m_pDstVelTexture3D != null)
        {
            // Create a rendertarget view for the Velocity 3D texture
            /*D3D10_TEXTURE3D_DESC velTex3Ddesc;
            m_pDstVelTexture3D->GetDesc(&velTex3Ddesc);*/

            // Make sure both destination textures have the same dimensions
            if((m_pDstVelTexture3D.getWidth() != m_width) || (m_pDstVelTexture3D.getHeight() != m_height) ||
                    (m_pDstVelTexture3D.getDepth() != m_depth))
            {
                dispose();
                throw new IllegalArgumentException();
            }

            /*D3D10_RENDER_TARGET_VIEW_DESC velRtDesc;
            velRtDesc.Format = velTex3Ddesc.Format;
            velRtDesc.ViewDimension = D3D10_RTV_DIMENSION_TEXTURE3D;
            velRtDesc.Texture3D.MipSlice = 0;
            velRtDesc.Texture3D.FirstWSlice = 0;
            velRtDesc.Texture3D.WSize = velTex3Ddesc.Depth;
            V_RETURN(m_pD3DDevice->CreateRenderTargetView(m_pDstVelTexture3D, &velRtDesc, &m_pDstVelTexRTView));*/

            m_pDstVelTexRTView = m_pDstVelTexture3D;

        }

        // Initialize internal texture resources
        InitTextures();

        // Load Voxelizer.fx, and get techniques and variables to use (if needed)
        InitShaders();

        // Init vertex buffer for a m_depth quads (to convert a "flat 3D texture" to a "3D texture");
        InitSlices();
        /*if(FAILED(hr))
        {
            Cleanup();
            return hr;
        }*/

        // create input layout for use with streamout buffers from SkinnedMesh
        {
            final int DXGI_FORMAT_R32G32B32_FLOAT = GLenum.GL_RGB32F;
            int D3D10_APPEND_ALIGNED_ELEMENT = 0;
            int D3D10_INPUT_PER_VERTEX_DATA = 0;
            D3D11_INPUT_ELEMENT_DESC inputElemDesc[] =
            {
                    VaDirectXTools.CD3D11_INPUT_ELEMENT_DESC("POSITION",  0, DXGI_FORMAT_R32G32B32_FLOAT, 0, D3D10_APPEND_ALIGNED_ELEMENT, D3D10_INPUT_PER_VERTEX_DATA, 0 ),
                    VaDirectXTools.CD3D11_INPUT_ELEMENT_DESC( "POSITION",  1, DXGI_FORMAT_R32G32B32_FLOAT, 1, D3D10_APPEND_ALIGNED_ELEMENT, D3D10_INPUT_PER_VERTEX_DATA, 0 ),
            };
            /*UINT numElements = sizeof(inputElemDesc)/sizeof(inputElemDesc[0]);

            D3D10_PASS_DESC passDesc;
            m_etGenVelocityWireframe->GetPassByIndex(0)->GetDesc(&passDesc);
            V_RETURN(m_pD3DDevice->CreateInputLayout(inputElemDesc, numElements,
                    passDesc.pIAInputSignature, passDesc.IAInputSignatureSize, &m_pSOInputLayout));*/

            m_pSOInputLayout = ID3D11InputLayout.createInputLayoutFrom(inputElemDesc);
        }


        m_initialized = true;
    }

    private void InitTextures(){
        // release the textures if they were allocated before
        SAFE_RELEASE(m_pDSTex2D);
        SAFE_RELEASE(m_pDSTex2DDSView);
        SAFE_RELEASE(m_pDSTex2DSRView);

        // create DXGI_FORMAT_R24G8_TYPELESS depth-stencil buffer and view
        Texture2DDesc dsTexDesc = new Texture2DDesc();
        dsTexDesc.width = m_width * m_cols;
        dsTexDesc.height = m_height * m_rows;
        dsTexDesc.mipLevels = 1;
        dsTexDesc.arraySize = 1;
        dsTexDesc.format = /*DXGI_FORMAT_R24G8_TYPELESS*/ GLenum.GL_DEPTH24_STENCIL8;
        /*dsTexDesc.SampleDesc.Count = 1;
        dsTexDesc.SampleDesc.Quality = 0;
        dsTexDesc.Usage = D3D10_USAGE_DEFAULT;
        dsTexDesc.BindFlags = D3D10_BIND_DEPTH_STENCIL | D3D10_BIND_SHADER_RESOURCE;
        dsTexDesc.CPUAccessFlags = 0;
        dsTexDesc.MiscFlags = 0;
        V_RETURN(m_pD3DDevice->CreateTexture2D( &dsTexDesc, NULL, &m_pDSTex2D ));*/
        m_pDSTex2D = TextureUtils.createTexture2D(dsTexDesc, null);

        // Create the depth stencil view
        /*D3D10_DEPTH_STENCIL_VIEW_DESC dsViewDesc;
        dsViewDesc.Format = DXGI_FORMAT_D24_UNORM_S8_UINT;
        dsViewDesc.ViewDimension = D3D10_DSV_DIMENSION_TEXTURE2D;
        dsViewDesc.Texture2D.MipSlice = 0;
        V_RETURN(m_pD3DDevice->CreateDepthStencilView( m_pDSTex2D, &dsViewDesc, &m_pDSTex2DDSView ));*/

        m_pDSTex2DDSView = m_pDSTex2D;

        // Create the shader resource view for the depth-stencil buffer
        /*D3D10_SHADER_RESOURCE_VIEW_DESC srvDesc;
        srvDesc.Format = DXGI_FORMAT_X24_TYPELESS_G8_UINT;
        srvDesc.ViewDimension = D3D10_SRV_DIMENSION_TEXTURE2D;
        srvDesc.Texture2D.MipLevels = 1;
        srvDesc.Texture2D.MostDetailedMip = 0;

        V_RETURN(m_pD3DDevice->CreateShaderResourceView(m_pDSTex2D, &srvDesc, &m_pDSTex2DSRView));*/

        m_pDSTex2DSRView = m_pDSTex2D;
    }

    private void InitShaders(){
        Runnable RS_CullDisabled = ()->
        {
            gl.glDisable(GLenum.GL_CULL_FACE);
            gl.glEnable(GLenum.GL_SCISSOR_TEST);
        };

        Runnable BS_NoBlending = ()-> gl.glDisable(GLenum.GL_BLEND);

        Runnable DSS_NonZeroRule = ()->
        {
            gl.glDisable(GLenum.GL_DEPTH_TEST);
            gl.glEnable(GLenum.GL_STENCIL_TEST);

            gl.glStencilOpSeparate(GLenum.GL_FRONT, GLenum.GL_KEEP, GLenum.GL_KEEP, GLenum.GL_DECR);
            gl.glStencilOpSeparate(GLenum.GL_BACK, GLenum.GL_KEEP, GLenum.GL_KEEP, GLenum.GL_INCR);
            gl.glStencilFunc(GLenum.GL_ALWAYS, 0, 0);
        };

        Runnable DSS_Disabled = ()->
        {
            gl.glDisable(GLenum.GL_DEPTH_TEST);
            gl.glDisable(GLenum.GL_STENCIL_TEST);
        };

        m_etNZ = new VoxelizerProgram("VS_VOXELIZE.vert", null, null);
        m_etNZ.setRasterizerState(RS_CullDisabled);
        m_etNZ.setBlendState(BS_NoBlending);
        m_etNZ.setDepthStencilState(DSS_NonZeroRule);

        m_etResolveWithPS = new VoxelizerProgram("VS_RESOLVE.vert", "GS_RESOLVE.gemo", "PS_RESOLVE.frag");
        m_etResolveWithPS.setRasterizerState(RS_CullDisabled);
        m_etResolveWithPS.setBlendState(BS_NoBlending);
        m_etResolveWithPS.setDepthStencilState(DSS_Disabled);

        m_etGenVelocityWireframe = new VoxelizerProgram("VS_GENVELOCITY.vert", "GS_GENVELOCITY.gemo", "PS_GENVELOCITY.frag");
        m_etResolveWithPS.setRasterizerState(RS_CullDisabled);
        m_etResolveWithPS.setBlendState(BS_NoBlending);
        m_etResolveWithPS.setDepthStencilState(DSS_Disabled);
    }

    private void InitSlices(){

    }

    public void dispose(){
//        SAFE_RELEASE(m_pD3DDevice);

        SAFE_RELEASE(m_pDstInOutTexture3D);
        SAFE_RELEASE(m_pDstInOutTexRTView);
        SAFE_RELEASE(m_pDstVelTexture3D);
        SAFE_RELEASE(m_pDstVelTexRTView);

        m_initialized = false;

        SAFE_RELEASE(m_pDSTex2D);
        SAFE_RELEASE(m_pDSTex2DDSView);
        SAFE_RELEASE(m_pDSTex2DSRView);

//        SAFE_RELEASE(m_pVoxEffect);

        /*m_etNZ = NULL;
        m_etResolveWithPS = NULL;
        m_etGenVelocityWireframe = NULL;

        m_evWorldViewProjectionMatrix = NULL;
        m_evProjSpacePixDim = NULL;
        m_evGridDim = NULL;
        m_evRecTimeStep = NULL;
        m_evVelocityMultiplier = NULL;
        m_evSliceIdx = NULL;
        m_evSliceZ = NULL;
        m_evTexture_depthStencil = NULL;*/

//        SAFE_RELEASE(m_pSlicesLayout);
        SAFE_RELEASE(m_pSlicesVB);

//        SAFE_RELEASE(m_pSOInputLayout);
    }

    private void DrawSlices(){

    }

    private void DoVoxelization(){

    }

    private void StencilClipVolume(){

    }

    private void VoxelizeVelocity(){

    }

    private void RenderClippedMesh(float zNear, float zFar, VoxelizerProgram pTechnique){

    }
}
