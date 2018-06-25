package nv.samples.smoke;

import com.nvidia.developer.opengl.utils.NvImage;

import org.lwjgl.util.vector.Matrix4f;

import java.io.IOException;

import jet.opengl.demos.intel.cput.CPUTBufferDX11;
import jet.opengl.demos.intel.cput.ID3D11InputLayout;
import jet.opengl.postprocessing.buffer.BufferGL;
import jet.opengl.postprocessing.common.Disposeable;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.TextureUtils;

final class VolumeRenderer implements Disposeable{
    static final int
    RM_SMOKE    = 0,
    RM_FIRE     = 1,
    RM_LEVELSET = 2;

    private final float[]               m_gridDim = new float[3];
    private float                       m_maxDim;
    private final Matrix4f              m_gridMatrix = new Matrix4f();

    private String                      m_effectPath;
//    ID3D10Effect                *m_pEffect;
//    ID3D10EffectTechnique       *m_pTechnique;

    private GLSLProgram            m_epQuadRaycastLevelSet;
    private GLSLProgram            m_epQuadRaycastFire;
    private GLSLProgram            m_epQuadRaycastSmoke;
    private GLSLProgram            m_epGlowHorizontal;
    private GLSLProgram            m_epGlowVertical;
    private GLSLProgram            m_epQuadRaycastUpsampleLevelSet;
    private GLSLProgram            m_epQuadRaycastUpsampleFire;
    private GLSLProgram            m_epQuadRaycastUpsampleSmoke;
    private GLSLProgram            m_epCompRayData_Back;
    private GLSLProgram            m_epCompRayData_Front;
    private GLSLProgram            m_epCompRayData_FrontNOBLEND;
    private GLSLProgram            m_epQuadDownSampleRayDataTexture;
    private GLSLProgram            m_epQuadEdgeDetect;


    /*ID3D10EffectShaderResourceVariable  *m_evTexture_rayData;
    ID3D10EffectShaderResourceVariable  *m_evTexture_rayDataSmall;
    ID3D10EffectShaderResourceVariable  *m_evTexture_volume;
    ID3D10EffectShaderResourceVariable  *m_evTexture_rayCast;
    ID3D10EffectShaderResourceVariable  *m_evTexture_edge;
    ID3D10EffectShaderResourceVariable  *m_evTexture_glow;
    ID3D10EffectShaderResourceVariable  *m_evTexture_sceneDepthTex;

    ID3D10EffectScalarVariable  *m_evRTWidth;
    ID3D10EffectScalarVariable  *m_evRTHeight;
    ID3D10EffectScalarVariable  *m_evUseGlow;
    ID3D10EffectMatrixVariable  *m_evWorldViewProjectionMatrix;
    ID3D10EffectMatrixVariable  *m_evInvWorldViewProjectionMatrix;
    ID3D10EffectMatrixVariable  *m_evGrid2WorldMatrix;
    ID3D10EffectScalarVariable  *m_evZNear;
    ID3D10EffectScalarVariable  *m_evZFar;
    ID3D10EffectScalarVariable  *m_evGridScaleFactor;
    ID3D10EffectVectorVariable  *m_evEyeOnGrid;

    ID3D10EffectScalarVariable  *m_evGlowContribution;
    ID3D10EffectScalarVariable  *m_evFinalIntensityScale;
    ID3D10EffectScalarVariable  *m_evFinalAlphaScale;
    ID3D10EffectScalarVariable  *m_evSmokeColorMultiplier;
    ID3D10EffectScalarVariable  *m_evsmokeAlphaMultiplier;
    ID3D10EffectScalarVariable  *m_evRednessFactor;

    ID3D10EffectMatrixVariable  *m_evWorldViewMatrix;
    ID3D10EffectScalarVariable  *m_evTan_FovYhalf;
    ID3D10EffectScalarVariable  *m_evTan_FovXhalf;
    ID3D10EffectScalarVariable  *m_evRaycastBisection;
    ID3D10EffectScalarVariable  *m_evRaycastFilterTricubic;
    ID3D10EffectScalarVariable  *m_evRaycastShaderAsWater;*/

    private ID3D11InputLayout       m_pGridBoxLayout;
    private BufferGL                m_pGridBoxVertexBuffer;
    private BufferGL                m_pGridBoxIndexBuffer;

    private ID3D11InputLayout       m_pQuadLayout;
    private BufferGL                m_pQuadVertexBuffer;


    private Texture2D[]     pRayDataTex2D = new Texture2D[2];
    private Texture2D[]     m_pRayDataRTV = new Texture2D[2];
    private Texture2D[]     m_pRayDataSRV = new Texture2D[2];

    private Texture2D       m_pRayDataSmallTex2D;
    private Texture2D       m_pRayDataSmallRTV;
    private Texture2D       m_pRayDataSmallSRV;

    private Texture2D       m_pRayCastTex2D;
    private Texture2D       m_pRayCastRTV;
    private Texture2D       m_pRayCastSRV;

    private Texture2D       m_pEdgeTex2D;
    private Texture2D       m_pEdgeRTV;
    private Texture2D       m_pEdgeSRV;

    private Texture2D       m_pGlowTex2D;
    private Texture2D       m_pGlowRTV;
    private Texture2D       m_pGlowSRV;

    private Texture2D       m_pGlowTempTex2D;
    private Texture2D       m_pGlowTempRTV;
    private Texture2D       m_pGlowTempSRV;

    private Texture2D       m_pFireTransformFunctionTex2D;

    private boolean         m_bRaycastBisection = true;
    private boolean         m_bRaycastFilterTricubic = true;
    private boolean         m_bRaycastShadeAsWater = true;

    private int             m_eRenderMode = RM_SMOKE;

    private int             m_renderTextureWidth;
    private int             m_renderTextureHeight;

    private boolean         m_useFP32Blending = true;

    private GLFuncProvider  gl;

    @Override
    public void dispose() {
//        SAFE_RELEASE(m_pEffect);

//        SAFE_RELEASE(m_pGridBoxLayout);
        SAFE_RELEASE(m_pGridBoxVertexBuffer);
        SAFE_RELEASE(m_pGridBoxIndexBuffer);

//        SAFE_RELEASE(m_pQuadLayout);
        SAFE_RELEASE(m_pQuadVertexBuffer);

        SAFE_RELEASE(pRayDataTex2D[0]);
        SAFE_RELEASE(pRayDataTex2D[1]);
        SAFE_RELEASE(m_pRayDataSRV[0]);
        SAFE_RELEASE(m_pRayDataSRV[1]);
        SAFE_RELEASE(m_pRayDataRTV[0]);
        SAFE_RELEASE(m_pRayDataRTV[1]);
        SAFE_RELEASE(m_pRayDataSmallTex2D);
        SAFE_RELEASE(m_pRayDataSmallRTV);
        SAFE_RELEASE(m_pRayDataSmallSRV);
        SAFE_RELEASE(m_pRayCastTex2D);
        SAFE_RELEASE(m_pRayCastSRV);
        SAFE_RELEASE(m_pRayCastRTV);
        SAFE_RELEASE(m_pGlowTex2D);
        SAFE_RELEASE(m_pGlowSRV);
        SAFE_RELEASE(m_pGlowRTV);
        SAFE_RELEASE(m_pGlowTempTex2D);
        SAFE_RELEASE(m_pGlowTempSRV);
        SAFE_RELEASE(m_pGlowTempRTV);
        SAFE_RELEASE(m_pEdgeTex2D);
        SAFE_RELEASE(m_pEdgeSRV);
        SAFE_RELEASE(m_pEdgeRTV);
    }

    void Initialize(int gridWidth, int gridHeight, int gridDepth, int renderMode){
        Cleanup();

        gl = GLFuncProviderFactory.getGLFuncProvider();
        m_eRenderMode = renderMode;

        m_gridDim[0] = (gridWidth);
        m_gridDim[1] = (gridHeight);
        m_gridDim[2] = (gridDepth);

        m_maxDim = Math.max( Math.max( m_gridDim[0], m_gridDim[1] ), m_gridDim[2] );

        // Initialize the grid offset matrix
        {
            // Make a scale matrix to scale the unit-sided box to be unit-length on the
            //  side/s with maximum dimension
           /* D3DXMATRIX scaleM;
            D3DXMatrixIdentity(&scaleM);
            D3DXMatrixScaling(&scaleM, m_gridDim[0] / m_maxDim, m_gridDim[1] / m_maxDim, m_gridDim[2] / m_maxDim);
            // offset grid to be centered at origin
            D3DXMATRIX translationM;
            D3DXMatrixTranslation(&translationM, -0.5, -0.5, -0.5);

            m_gridMatrix = translationM * scaleM;*/
            m_gridMatrix.m00 = m_gridDim[0] / m_maxDim;
            m_gridMatrix.m11 = m_gridDim[1] / m_maxDim;
            m_gridMatrix.m22 = m_gridDim[2] / m_maxDim;
            m_gridMatrix.translate(-0.5f, -0.5f, -0.5f);
        }

        // Check if the device supports FP32 blending to choose the right codepath depending on this
//        UINT rgba32fFormatSupport;
//        m_pD3DDevice->CheckFormatSupport(DXGI_FORMAT_R32G32B32A32_FLOAT, &rgba32fFormatSupport);
        m_useFP32Blending = true;

        initShaders();
        createGridBox();
        createScreenQuad();

        createJitterTexture();
//        loadTextureFromFile(m_pD3DDevice, L"..\\..\\Media\\FireTransferFunction.dds", m_pEffect, m_effectPath, "fireTransferFunction"));

        NvImage image = new NvImage();
        try {
            image.loadImageFromFile("media\\FireTransferFunction.dds");
            int textureID = image.updaloadTexture();
            m_pFireTransformFunctionTex2D = TextureUtils.createTexture2D(GLenum.GL_TEXTURE_2D, textureID);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void SetScreenSize(int width, int height){
        createRayDataResources(width, height);
    }
    void Cleanup() {dispose();}
    void Draw(Texture2D  pSourceTexSRV){
        /*m_evTexture_volume.SetResource(pSourceTexSRV);
        // Set some variables required by the shaders:
        //=========================================================================

        m_evGlowContribution.SetFloat(g_glowContribution);
        m_evFinalIntensityScale.SetFloat(g_finalIntensityScale);
        m_evFinalAlphaScale.SetFloat(g_finalAlphaScale);
        m_evSmokeColorMultiplier.SetFloat(g_smokeColorMultiplier);
        m_evsmokeAlphaMultiplier.SetFloat(g_smokeAlphaMultiplier);
        m_evRednessFactor.SetInt(g_RednessFactor);

        // The near and far planes are used to unproject the scene's z-buffer values
        m_evZNear.SetFloat(g_zNear);
        m_evZFar.SetFloat(g_zFar);

        D3DMATRIX grid2World = m_gridMatrix * g_gridWorld;
        m_evGrid2WorldMatrix->SetMatrix( (float*) &grid2World );

        D3DXMATRIX worldView = g_gridWorld * g_View;

        // The length of one of the axis of the worldView matrix is the length of longest side of the box
        //  in view space. This is used to convert the length of a ray from view space to grid space.
        D3DXVECTOR3 worldXaxis = D3DXVECTOR3(worldView._11, worldView._12, worldView._13);
        float worldScale = D3DXVec3Length(&worldXaxis);
        m_evGridScaleFactor->SetFloat( worldScale );

        // We prepend the current world matrix with this other matrix which adds an offset (-0.5, -0.5, -0.5)
        //  and scale factors to account for unequal number of voxels on different sides of the volume box.
        // This is because we want to preserve the aspect ratio of the original simulation grid when
        //  raytracing through it, and this matrix allows us to do the raytracing in grid (texture) space:
        //  i.e. each side of the box spans the 0 to 1 range
//        worldView = m_gridMatrix * worldView;
        Matrix4f.mul(worldView, m_gridMatrix, worldView);

        m_evWorldViewMatrix->SetMatrix( (float*)&worldView );
        m_evTan_FovYhalf->SetFloat( tan(g_Fovy/2.0f) );
        m_evTan_FovXhalf->SetFloat( tan(g_Fovy/2.0f)*m_renderTextureWidth/m_renderTextureHeight );

        // options for the LevelSet raytracer
        m_evRaycastBisection->SetBool(m_bRaycastBisection);
        m_evRaycastFilterTricubic->SetBool(m_bRaycastFilterTricubic);
        m_evRaycastShaderAsWater->SetBool(m_bRaycastShadeAsWater);

        // worldViewProjection is used to transform the volume box to screen space
        D3DXMATRIX worldViewProjection;
        worldViewProjection = worldView * g_Projection;
        m_evWorldViewProjectionMatrix->SetMatrix( (float*)&worldViewProjection );

        // invWorldViewProjection is used to transform positions in the "near" plane into grid space
        D3DXMATRIX invWorldViewProjection;
        D3DXMatrixInverse(&invWorldViewProjection, NULL, &worldViewProjection);
        m_evInvWorldViewProjectionMatrix->SetMatrix((float*)&invWorldViewProjection);

        // Compute the inverse of the worldView matrix
        D3DXMATRIX worldViewInv;
        D3DXMatrixInverse(&worldViewInv, NULL, &worldView);
        // Compute the eye's position in "grid space" (the 0-1 texture coordinate cube)
        D3DXVECTOR4 eyeInGridSpace;
        D3DXVECTOR3 origin(0,0,0);
        D3DXVec3Transform(&eyeInGridSpace, &origin, &worldViewInv);
        m_evEyeOnGrid->SetFloatVector((float*)&eyeInGridSpace);

        final float color[] = {0, 0, 0, 0 };


        // Ray cast and render to a temporary buffer
        //=========================================================================

        // Partial init of viewport struct used below
        *//*D3D10_VIEWPORT rtViewport;
        rtViewport.TopLeftX = 0;
        rtViewport.TopLeftY = 0;
        rtViewport.MinDepth = 0;
        rtViewport.MaxDepth = 1;*//*


        // Compute the ray data required by the raycasting pass below.
        //  This function will render to a buffer of float4 vectors, where
        //  xyz is starting position of the ray in grid space
        //  w is the length of the ray in view space
        computeRayData();


        // Do edge detection on this image to find any
        //  problematic areas where we need to raycast at higher resolution
        computeEdgeTexture();


        // Raycast into the temporary render target:
        //  raycasting is done at the smaller resolution, using a fullscreen quad
        m_pD3DDevice->ClearRenderTargetView( m_pRayCastRTV, color );
        m_pD3DDevice->OMSetRenderTargets( 1, &m_pRayCastRTV , NULL );

      *//*  rtViewport.Width = m_renderTextureWidth;
        rtViewport.Height = m_renderTextureHeight;
        m_pD3DDevice->RSSetViewports(1,&rtViewport);*//*

        m_evRTWidth->SetFloat((float)m_renderTextureWidth);
        m_evRTHeight->SetFloat((float)m_renderTextureHeight);

        m_evTexture_rayDataSmall->SetResource(m_pRayDataSmallSRV);

        if( m_eRenderMode == RM_LEVELSET )
            m_epQuadRaycastLevelSet->Apply(0);
        else if(m_eRenderMode == RM_FIRE)
            m_epQuadRaycastFire->Apply(0);
        else
            m_epQuadRaycastSmoke->Apply(0);

        drawScreenQuad();


        m_pD3DDevice->ClearRenderTargetView( m_pGlowRTV, color );

        //blur the raycast image to get a blur texture
        m_evUseGlow->SetBool(g_bRenderGlow);
        if((m_eRenderMode == RM_FIRE) && g_bRenderGlow)
        {
            m_pD3DDevice->ClearRenderTargetView( m_pGlowTempRTV, color );
            m_pD3DDevice->OMSetRenderTargets( 1, &m_pGlowTempRTV , NULL );
            m_evTexture_glow->SetResource(m_pRayCastSRV);
            m_epGlowHorizontal->Apply(0);
            drawScreenQuad();

            m_pD3DDevice->OMSetRenderTargets( 1, &m_pGlowRTV , NULL );
            m_evTexture_glow->SetResource(m_pGlowTempSRV);
            m_epGlowVertical->Apply(0);
            drawScreenQuad();

        }


        // Render to the back buffer sampling from the raycast texture that we just created
        // If and edge was detected at the current pixel we will raycast again to avoid
        // smoke aliasing artifacts at scene edges
        ID3D10RenderTargetView* pRTV = DXUTGetD3D10RenderTargetView();
        ID3D10DepthStencilView* pDSV = DXUTGetD3D10DepthStencilView();
        m_pD3DDevice->OMSetRenderTargets( 1, &pRTV , pDSV );

        rtViewport.Width = g_Width;
        rtViewport.Height = g_Height;
        m_pD3DDevice->RSSetViewports(1,&rtViewport);

        m_evRTWidth->SetFloat((float)g_Width);
        m_evRTHeight->SetFloat((float)g_Height);

        m_evTexture_rayCast->SetResource(m_pRayCastSRV);
        m_evTexture_edge->SetResource(m_pEdgeSRV);
        m_evTexture_glow->SetResource(m_pGlowSRV);

        if( m_eRenderMode == RM_LEVELSET )
            m_epQuadRaycastUpsampleLevelSet->Apply(0);
        else if(m_eRenderMode == RM_FIRE)
            m_epQuadRaycastUpsampleFire->Apply(0);
        else
            m_epQuadRaycastUpsampleSmoke->Apply(0);

        drawScreenQuad();

        m_evTexture_rayCast->SetResource(NULL);
        m_evTexture_edge->SetResource(NULL);
        m_evTexture_glow->SetResource(NULL);
        m_epQuadRaycastUpsampleFire->Apply(0);*/
    }

    float GetMaxDim(){return m_maxDim;};

    private void initShaders(){}
    private void createGridBox(){}
    private void createScreenQuad(){}
    private void calculateRenderTextureSize(int screenWidth, int screenHeight){}
    private void createRayDataResources(int width, int height){}
    private void createJitterTexture(){}

    private void computeRayData(){
        // Clear the color buffer to 0
        float blackColor[] = {0, 0, 0, 0 };
//        m_pD3DDevice->ClearRenderTargetView(m_pRayDataRTV[0], blackColor);
        gl.glClearTexImage(m_pRayDataRTV[0].getTexture(), 0, TextureUtils.measureFormat(m_pRayDataRTV[0].getFormat()),
                TextureUtils.measureDataType(m_pRayDataRTV[0].getFormat()), null);

        if( !m_useFP32Blending )
//            m_pD3DDevice->ClearRenderTargetView(m_pRayDataRTV[1], blackColor);
            gl.glClearTexImage(m_pRayDataRTV[1].getTexture(), 0, TextureUtils.measureFormat(m_pRayDataRTV[1].getFormat()),
                    TextureUtils.measureDataType(m_pRayDataRTV[1].getFormat()), null);

        /*m_pD3DDevice->OMSetRenderTargets(1, &m_pRayDataRTV[0], NULL);

        m_evTexture_sceneDepthTex->SetResource(g_pSceneDepthSRV);

        // Setup viewport to match the window's backbuffer
        D3D10_VIEWPORT rtViewport;
        rtViewport.TopLeftX = 0;
        rtViewport.TopLeftY = 0;
        rtViewport.MinDepth = 0;
        rtViewport.MaxDepth = 1;
        rtViewport.Width = g_Width;
        rtViewport.Height = g_Height;
        m_pD3DDevice->RSSetViewports(1,&rtViewport);
        m_evRTWidth->SetFloat((float)g_Width);
        m_evRTHeight->SetFloat((float)g_Height);

        // Render volume back faces
        // We output xyz=(0,-1,0) and w=min(sceneDepth, boxDepth)
        m_epCompRayData_Back.enable();
        drawBox();

        if( !m_useFP32Blending )
        {
            // repeat the back face pass in m_pRayDataRTV[1] - we could also do CopySubResource
            m_pD3DDevice->OMSetRenderTargets(1, &m_pRayDataRTV[1], NULL);
            drawBox();
        }

        // Render volume front faces using subtractive blending or doing texture lookups
        //  depending on hw support for FP32 blending. Note that an FP16 RGBA buffer is
        //  does not have enough precision to represent the different XYZ postions
        //  for each ray entry point in most common circumstances.
        // We output xyz="position in grid space" and w=boxDepth,
        //  unless the pixel is occluded by the scene, in which case we output xyzw=(1,0,0,0)
        if( m_useFP32Blending )
        {
            m_epCompRayData_Front->Apply(0);
        }
        else
        {
            m_evTexture_rayData->SetResource(m_pRayDataSRV[0]);
            m_epCompRayData_FrontNOBLEND->Apply(0);
        }
        drawBox();*/
    }

    private void computeEdgeTexture(){
        // First setup viewport to match the size of the destination low-res texture
        /*D3D10_VIEWPORT rtViewport;
        rtViewport.TopLeftX = 0;
        rtViewport.TopLeftY = 0;
        rtViewport.MinDepth = 0;
        rtViewport.MaxDepth = 1;
        rtViewport.Width = m_renderTextureWidth;
        rtViewport.Height = m_renderTextureHeight;
        m_pD3DDevice->RSSetViewports(1,&rtViewport);
        m_evRTWidth->SetFloat((float)m_renderTextureWidth);
        m_evRTHeight->SetFloat((float)m_renderTextureHeight);

        // Downsample the rayDataTexture to a new small texture, simply using point sample (no filtering)
        m_pD3DDevice->OMSetRenderTargets( 1, &m_pRayDataSmallRTV , NULL );
        if( m_useFP32Blending )
            m_evTexture_rayData->SetResource(m_pRayDataSRV[0]);
        else
            m_evTexture_rayData->SetResource(m_pRayDataSRV[1]);
        m_epQuadDownSampleRayDataTexture->Apply(0);
        drawScreenQuad();

        // Create an edge texture, performing edge detection on 'rayDataTexSmall'
        m_pD3DDevice->OMSetRenderTargets( 1, &m_pEdgeRTV , NULL );
        m_evTexture_rayDataSmall->SetResource(m_pRayDataSmallSRV);
        m_epQuadEdgeDetect->Apply(0);
        drawScreenQuad();*/
    }

    private void drawBox(){
        /*UINT stride = sizeof( VsInput );
        UINT offset = 0;
        m_pD3DDevice->IASetVertexBuffers( 0, 1, &m_pGridBoxVertexBuffer, &stride, &offset );
        m_pD3DDevice->IASetIndexBuffer( m_pGridBoxIndexBuffer, DXGI_FORMAT_R32_UINT, 0 );
        m_pD3DDevice->IASetPrimitiveTopology( D3D10_PRIMITIVE_TOPOLOGY_TRIANGLELIST );
        m_pD3DDevice->IASetInputLayout(m_pGridBoxLayout);
        m_pD3DDevice->DrawIndexed(36, 0, 0);*/
    }

    private void drawScreenQuad(){
        /*UINT strides = sizeof(VsInput);
        UINT offsets = 0;
        m_pD3DDevice->IASetInputLayout( m_pQuadLayout );
        m_pD3DDevice->IASetVertexBuffers( 0, 1, &m_pQuadVertexBuffer, &strides, &offsets );
        m_pD3DDevice->IASetPrimitiveTopology( D3D10_PRIMITIVE_TOPOLOGY_TRIANGLESTRIP );
        m_pD3DDevice->Draw( 4, 0 );*/
    }
}
