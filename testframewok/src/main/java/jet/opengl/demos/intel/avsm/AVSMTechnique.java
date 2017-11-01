package jet.opengl.demos.intel.avsm;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;
import org.lwjgl.util.vector.Vector4i;

import java.util.ArrayList;
import java.util.List;

import jet.opengl.postprocessing.buffer.BufferGL;
import jet.opengl.postprocessing.common.Disposeable;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.texture.SamplerDesc;
import jet.opengl.postprocessing.texture.SamplerUtils;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.Texture2DDesc;
import jet.opengl.postprocessing.texture.TextureUtils;
import jet.opengl.postprocessing.util.CommonUtil;

/**
 * Created by mazhen'gui on 2017/10/9.
 */

final class AVSMTechnique implements Disposeable{
    static final int  MAX_AVSM_RT_COUNT  = (4);
    static final int  MAX_SHADER_VARIATIONS = (MAX_AVSM_RT_COUNT / 2);

    static final int POWER_PLANT_SCENE        = 0,
                     GROUND_PLANE_SCENE       = 1;

    private int mLisTexNodeCount;


//    ID3D11VertexShader* mFullScreenTriangleVS;
//    ID3D11ShaderReflection* mFullScreenTriangleVSReflector;

    // Particle and AVSM shaders
    /*ID3D11VertexShader*  mParticleShadingVS;
    ID3D11ShaderReflection* mParticleShadingVSReflector;
    ID3D11PixelShader*   mParticleShadingPS;
    ID3D11ShaderReflection* mParticleShadingPSReflector;*/
    private GLSLProgram mParticleShadingProgram;

    /*ID3D11PixelShader*   mParticleShadingPerPixelPS[MAX_SHADER_VARIATIONS];
    ID3D11ShaderReflection* mParticleShadingPerPixelPSReflector[MAX_SHADER_VARIATIONS];
    ID3D11PixelShader*   mParticleAVSMCapturePS; // Capture-all-fragments shaders
    ID3D11ShaderReflection* mParticleAVSMCapturePSReflector;
    ID3D11PixelShader*   mAVSMInsertionSortResolvePS[MAX_SHADER_VARIATIONS];
    ID3D11ShaderReflection* mAVSMInsertionSortResolvePSReflector[MAX_SHADER_VARIATIONS];
    ID3D11PixelShader*   mAVSMSinglePassInsertPS[MAX_SHADER_VARIATIONS]; // Fake render-target-read shaders
    ID3D11ShaderReflection* mAVSMSinglePassInsertPSReflector[MAX_SHADER_VARIATIONS];
    ID3D11PixelShader*   mAVSMClearStructuredBufPS;
    ID3D11ShaderReflection* mAVSMClearStructuredBufPSReflector;
    ID3D11PixelShader*   mAVSMConvertSUAVtoTex2DPS;
    ID3D11ShaderReflection* mAVSMConvertSUAVtoTex2DPSReflector;*/
    private GLSLProgram[] mParticleShadingPerPixelProgram = new GLSLProgram[MAX_SHADER_VARIATIONS];
    private GLSLProgram mParticleAVSMCaptureProgram;
    private GLSLProgram[] mAVSMInsertionSortResolveProgram = new GLSLProgram[MAX_SHADER_VARIATIONS];
    private GLSLProgram[] mAVSMSinglePassInsertProgram = new GLSLProgram[MAX_SHADER_VARIATIONS];
    private GLSLProgram mAVSMClearStructuredBufProgram;
    private GLSLProgram mAVSMConvertSUAVtoTex2DProgram;


    // New shaders for vertex texture lookup and Tessellation
    /*ID3D11DomainShader*  mParticleShadingDS[MAX_SHADER_VARIATIONS];
    ID3D11ShaderReflection* mParticleShadingDSReflector[MAX_SHADER_VARIATIONS];
    ID3D11HullShader*  mParticleShadingHS;
    ID3D11ShaderReflection* mParticleShadingHSReflector;
    ID3D11VertexShader*  mParticleShadingFinalVS[MAX_SHADER_VARIATIONS];
    ID3D11ShaderReflection* mParticleShadingFinalVSReflector[MAX_SHADER_VARIATIONS];
    ID3D11VertexShader*  mParticleShadingTessellationVS;
    ID3D11ShaderReflection* mParticleShadingTessellationVSReflector; TODO*/


    // AVSM Gen shaders
    /*ID3D11PixelShader*   mParticleAVSM_GenPS[MAX_SHADER_VARIATIONS]; // "in-place" generation of AVSM visibility function
    ID3D11ShaderReflection* mParticleAVSM_GenPSReflector[MAX_SHADER_VARIATIONS];*/
    private GLSLProgram[] mParticleAVSM_GenPS = new GLSLProgram[MAX_SHADER_VARIATIONS];

    // Constant buffers
    private BufferGL mPerFrameConstants;
    private BufferGL mParticlePerFrameConstants;
    private BufferGL mParticlePerPassConstants;
    private BufferGL mListTextureConstants;
    private BufferGL mAVSMConstants;
    // cput equivalents for autobinding
    private BufferGL mAVSMConstantBufferCPUT;
    private BufferGL mAVSMConstantsCPUT;
    private BufferGL mPerFrameConstantsCPUT;
    private BufferGL mParticlePerFrameConstantsCPUT;

    // Rasterizer state
    private Runnable mRasterizerState;
    private Runnable mDoubleSidedRasterizerState;
    private Runnable mParticleWireFrameState;
    private Runnable mShadowRasterizerState;
    private Runnable mParticleRasterizerState;

    private Runnable mDefaultDepthStencilState;
    private Runnable mParticleDepthStencilState;
    private Runnable mAVSMCaptureDepthStencilState;

    private Runnable mGeometryBlendState;
    private Runnable mLightingBlendState;
    private Runnable mParticleBlendState;

    private int mAVSMSampler;

//    std::vector< std::tr1::shared_ptr<Texture2D> > mGBuffer;
    private final List<Texture2D> mGBuffer = new ArrayList<>();

    // AVSM
    private final Vector4i          mAVSMShadowViewport = new Vector4i();
    private int                     mAVSMNodeCount;
    private int                     mShaderIdx;
//    std::tr1::shared_ptr<Texture2D> mAVSMTextures;
    private Texture2D mAVSMTextures;
    private Texture2D                mAVSMTexturesCPUT;
    private Texture2D                 mAVSMTexturesDebug;
    private BufferGL                   mAVSMStructBuf;
    private BufferGL      mAVSMStructBufUAV;
    private BufferGL       mAVSMStructBufSRV;

    // AVSM Gen
//    std::tr1::shared_ptr<Texture2D> mAVSMGenCtrlSurface;
    private Texture2D  mAVSMGenCtrlSurface;
    private Texture2D               mAVSMGenCtrlSurfaceCPUT;
    private BufferGL[]              mAVSMGenData = new BufferGL[MAX_SHADER_VARIATIONS];
    private BufferGL[]      mAVSMGenDataUAV = new BufferGL[MAX_SHADER_VARIATIONS];
    private BufferGL[]       mAVSMGenDataSRV = new BufferGL[MAX_SHADER_VARIATIONS];
    private BufferGL[]                 mAVSMGenDataSRVCPUT = new BufferGL[MAX_SHADER_VARIATIONS];
    private int				mAVSMGenSampler;

    // List texture
//    std::tr1::shared_ptr<Texture2D> mListTexFirstSegmentNodeOffset;
//    std::tr1::shared_ptr<Texture2D> mListTexFirstVisibilityNodeOffset;
    private Texture2D  mListTexFirstSegmentNodeOffset;
    private Texture2D mListTexFirstVisibilityNodeOffset;
    private BufferGL                   mListTexSegmentNodes;
    private BufferGL                   mListTexSegmentNodesDebug;
    private BufferGL                   mListTexVisibilityNodes;
    private BufferGL                   mListTexVisibilityNodesDebug;
    private BufferGL      mListTexSegmentNodesUAV;
    private BufferGL       mListTexSegmentNodesSRV;
    private BufferGL      mListTexVisibilityNodesUAV;
    private BufferGL       mListTexVisibilityNodesSRV;
    private Texture2D                mListTexFirstOffsetDebug;

    /*ID3D11VertexShader*             mDrawTransmittanceVS;
    ID3D11ShaderReflection*         mDrawTransmittanceVSReflector;
    ID3D11PixelShader*              mDrawTransmittancePS;
    ID3D11ShaderReflection*         mDrawTransmittancePSReflector;
    ID3D11Buffer*                   mDrawTransmittanceVB;
    ID3D11InputLayout*              mDrawTransmittanceLayout;*/
    private GLSLProgram             mDrawTransmittanceProgram;
    private BufferGL                mDrawTransmittanceVB;
    private boolean              mDumpTransmittanceCurve;
    private int                             mDumpTransmittanceCurveIndex;
    private int                             mDrawTransmittanceMaxNodes;

    private float                           mLastTime;
    private GLFuncProvider gl;

    int mShadowTextureDim;
    int mAVSMShadowTextureDim;

    AVSMTechnique(//ID3D11Device* d3dDevice,
                  //ID3D11DeviceContext* d3dDeviceContext,
                  int nodeCount,
                  int shadowTextureDim,
                  int avsmShadowTextureDim){
        mShadowTextureDim = shadowTextureDim;
        mAVSMShadowTextureDim = avsmShadowTextureDim;

        // 50% coverage of screen (assuming resolution of 1680x1050
        // with average depth complexity of 50. Should be more than plenty.
        mLisTexNodeCount = (1 << 24);
        mLastTime = 0;

        // initialize all members
        for (int i = 0; i < MAX_SHADER_VARIATIONS; ++i)
            mAVSMGenDataSRVCPUT[i]=null;

        mAVSMGenCtrlSurfaceCPUT = null;
        mAVSMTexturesCPUT = null;
        for(int ii=0; ii<MAX_SHADER_VARIATIONS; ii++)
        {
            mAVSMGenDataSRVCPUT[ii] = null;
        }
        mAVSMConstantBufferCPUT = null;
        mAVSMConstantsCPUT = null;
        mPerFrameConstantsCPUT = null;
        mParticlePerFrameConstantsCPUT = null;

        // constant buffers
        mAVSMConstants = null;
        mPerFrameConstants = null;
        mParticlePerFrameConstants = null;

        mParticleDepthStencilState = null;
        mDefaultDepthStencilState = null;
        mAVSMCaptureDepthStencilState = null;
        mAVSMSampler = 0;
        mAVSMGenSampler = 0;
        mParticlePerPassConstants = null;
        mListTextureConstants = null;
        mLightingBlendState = null;
        mGeometryBlendState = null;
        mParticleBlendState = null;
        mShadowRasterizerState = null;
        mRasterizerState = null;
        mDoubleSidedRasterizerState = null;
        mParticleWireFrameState = null;

        mParticleRasterizerState = null;
        /*mFullScreenTriangleVS = NULL;
        mFullScreenTriangleVSReflector = NULL;
        mParticleShadingVS = NULL;
        mParticleShadingVSReflector = NULL;
        mAVSMClearStructuredBufPS = NULL;
        mAVSMClearStructuredBufPSReflector = NULL;
        mParticleAVSMCapturePS = NULL;
        mParticleAVSMCapturePSReflector = NULL;*/
        mListTexSegmentNodes = null;
        mListTexSegmentNodesUAV = null;
        mListTexSegmentNodesSRV = null;
        mListTexVisibilityNodes = null;
        mListTexVisibilityNodesUAV = null;
        mListTexVisibilityNodesSRV = null;
        mAVSMStructBuf = null;

        mAVSMTexturesDebug = null;
        mAVSMStructBufUAV = null;
        mAVSMStructBufSRV = null;

        /*mParticleShadingTessellationVS = NULL;
        mParticleShadingTessellationVSReflector = NULL;
        mParticleShadingHS = NULL;
        mParticleShadingHSReflector = NULL;
        mParticleShadingPS = NULL;
        mParticleShadingPSReflector = NULL;*/

        for (int i = 0; i < MAX_SHADER_VARIATIONS; ++i)
        {
            /*mAVSMInsertionSortResolvePS[i] = NULL;
            mAVSMInsertionSortResolvePSReflector[i] = NULL;
            mAVSMSinglePassInsertPS[i] = NULL;
            mAVSMSinglePassInsertPSReflector[i] = NULL;
            mParticleShadingPerPixelPS[i] = NULL;
            mParticleShadingPerPixelPSReflector[i] = NULL;*/
            mParticleAVSM_GenPS[i] = null;
            /*mParticleAVSM_GenPSReflector[i] = NULL;
            mParticleShadingDS[i] = NULL;
            mParticleShadingDSReflector[i] = NULL;
            mParticleShadingFinalVS[i] = NULL;
            mParticleShadingFinalVSReflector[i] = NULL;*/

            mAVSMGenData[i] = null;
            mAVSMGenDataUAV[i] = null;
            mAVSMGenDataSRV[i] = null;
        }

        gl = GLFuncProviderFactory.getGLFuncProvider();
    }



    void OnD3D11ResizedSwapChain(ID3D11Device* d3dDevice,
                                 const D3D11_TEXTURE2D_DESC* backBufferDesc);

    void InitializeFrameContext( FrameContext outContext, Options options, /*ID3D11DeviceContext* d3dDeviceContext,*/ ParticleSystem  particleSystem,
                                    Matrix4f worldMatrix, CPUTCamera viewerCamera, CPUTCamera lightCamera, Vector4i viewport ){
        outContext.DepthBufferSRV   = NULL;
        outContext.D3dDeviceContext = d3dDeviceContext;
        outContext.Options          = options;
        outContext.ParticleSystem   = particleSystem;
        outContext.ViewerCamera     = viewerCamera;
        outContext.LightCamera      = lightCamera;
        outContext.Viewport         = viewport;
        memset( &outContext.Stats, 0, sizeof(outContext.Stats) );

        FrameMatrices & frameMatx = outContext.Matrices;

        frameMatx.worldMatrix = worldMatrix;

        assert((options.enableParticles && particleSystem) ||
                (options.enableParticles == false));

        frameMatx.cameraProj = DXUTFromCPUT( *viewerCamera->GetProjectionMatrix() );
        frameMatx.cameraView = DXUTFromCPUT( *viewerCamera->GetViewMatrix() );

        D3DXMatrixInverse(&frameMatx.cameraViewInv, 0, &frameMatx.cameraView);

        // We only use the view direction from the camera object
        // We then center the directional light on the camera frustum and set the
        // extents to completely cover it.
        frameMatx.lightView = DXUTFromCPUT( *lightCamera->GetViewMatrix() );
        {
            // NOTE: We don't include the projection matrix here, since we want to just get back the
            // raw view-space extents and use that to *build* the bounding projection matrix
            D3DXVECTOR3 min, max;
            ComputeFrustumExtents(frameMatx.cameraViewInv, frameMatx.cameraProj,
                    viewerCamera->GetNearPlaneDistance(), viewerCamera->GetFarPlaneDistance(),
                    frameMatx.lightView, &min, &max);

            // First adjust the light matrix to be centered on the extents in x/y and behind everything in z
            D3DXVECTOR3 center = 0.5f * (min + max);
            D3DXMATRIXA16 centerTransform;
            D3DXMatrixTranslation(&centerTransform, -center.x, -center.y, 0.0f);
            frameMatx.lightView *= centerTransform;

            // Now create a projection matrix that covers the extents when centered
            // Optimization: Again use scene AABB to decide on light far range - this one can actually clip out
            // any objects further away than the frustum can see if desired.
            D3DXVECTOR3 dimensions = max - min;
            D3DXMatrixOrthoLH(&frameMatx.lightProj, dimensions.x, dimensions.y, 0.0f, 1000.0f);
        }

        // Compute composite matrices
        frameMatx.cameraViewProj = frameMatx.cameraView * frameMatx.cameraProj;
        frameMatx.cameraWorldViewProj = frameMatx.worldMatrix * frameMatx.cameraViewProj;
        frameMatx.cameraWorldView = frameMatx.worldMatrix * frameMatx.cameraView;
        frameMatx.lightViewProj = frameMatx.lightView * frameMatx.lightProj;
        frameMatx.lightWorldViewProj = frameMatx.worldMatrix * frameMatx.lightViewProj;
        frameMatx.cameraViewToLightProj = frameMatx.cameraViewInv * frameMatx.lightViewProj;
        frameMatx.cameraViewToLightView = frameMatx.cameraViewInv * frameMatx.lightView;

        frameMatx.avsmLightProj = DXUTFromCPUT( *lightCamera->GetProjectionMatrix() );
        frameMatx.avsmLightView = DXUTFromCPUT( *lightCamera->GetViewMatrix() );

        if (options.enableAutoBoundsAVSM) {

            // doesn't work with CPUT shadows unfortunately!
            assert( false );

            // Get bounding boxes from transparent geometry
        const float bigNumf = 1e10f;
            D3DXVECTOR3 maxBB(-bigNumf, -bigNumf, -bigNumf);
            D3DXVECTOR3 minBB(+bigNumf, +bigNumf, +bigNumf);

            if (options.enableParticles) {
                // Initialize minBB, maxBB
                for (size_t p = 0; p < 3; ++p) {
                    minBB[p] = std::numeric_limits<float>::max();
                    maxBB[p] = std::numeric_limits<float>::min();
                }

                D3DXVECTOR3 particleMin, particleMax;
                particleSystem->GetBBox(&particleMin, &particleMax);

                for (size_t p = 0; p < 3; ++p) {
                    minBB[p] = std::min(minBB[p], particleMin[p]);
                    maxBB[p] = std::max(maxBB[p], particleMax[p]);
                }

                TransformBBox(&minBB, &maxBB, frameMatx.avsmLightView);
            }

            // First adjust the light matrix to be centered on the extents in x/y and behind everything in z
            D3DXVECTOR3 center = 0.5f * (minBB + maxBB);
            D3DXMATRIXA16 centerTransform;
            D3DXMatrixTranslation(&centerTransform, -center.x, -center.y, -minBB.z);
            frameMatx.avsmLightView *= centerTransform;

            // Now create a projection matrix that covers the extents when centered
            // Optimization: Again use scene AABB to decide on light far range - this one can actually clip out
            // any objects further away than the frustum can see if desired.
            D3DXVECTOR3 dimensions = maxBB - minBB;
            D3DXMatrixOrthoLH(&frameMatx.avsmLightProj, dimensions.x, dimensions.y, 0, dimensions.z);
        }

        // Compute composite matrices;
        frameMatx.avsmLightViewProj = frameMatx.avsmLightView * frameMatx.avsmLightProj;
        frameMatx.avmsLightWorldViewProj = frameMatx.worldMatrix * frameMatx.avsmLightViewProj;
        frameMatx.cameraViewToAvsmLightProj = frameMatx.cameraViewInv * frameMatx.avsmLightViewProj;
        frameMatx.cameraViewToAvsmLightView = frameMatx.cameraViewInv * frameMatx.avsmLightView;

        // Just set up some common defaults
        memset( &outContext.GPUUIConstants, 0, sizeof( outContext.GPUUIConstants ) );
        outContext.GPUUIConstants.lightingOnly              = 0;
        outContext.GPUUIConstants.faceNormals               = 0;
        outContext.GPUUIConstants.enableStats               = false;
        outContext.GPUUIConstants.volumeShadowMethod        = VOL_SHADOW_AVSM;
        outContext.GPUUIConstants.enableVolumeShadowLookup  = 1;
        outContext.GPUUIConstants.pauseParticleAnimaton     = 0;
        outContext.GPUUIConstants.particleSize              = 1.0f;
        outContext.GPUUIConstants.particleOpacity           = 33;
        outContext.GPUUIConstants.vertexShaderShadowLookup	= 0;
        outContext.GPUUIConstants.wireframe					= 0;
        outContext.GPUUIConstants.tessellate				= 1;
        outContext.GPUUIConstants.TessellationDensity		= 1.0f/14.0f;
    }

    void UpdateParticles( FrameContext & frameContext );

    void CreateAVSMShadowMap( FrameContext & frameContext, CPUTAssetSet *mpAssetSet, CPUTRenderParametersDX* pRenderParams );

    void RenderShadedParticles( FrameContext & frameContext, ID3D11RenderTargetView* backBuffer, ID3D11DepthStencilView* depthBuffer );

    void UpdateDebugViewD3D11( FrameContext & frameContext );

    void UpdateStatesAndConstantsForAVSMUse( FrameContext & frameContext, bool forCPUTGeometry );

    void Cleanup(/*ID3D11DeviceContext* d3dDeviceContext*/){

    }

    void DumpTransmittanceCurve() {
        mDumpTransmittanceCurve = true;
        ++mDumpTransmittanceCurveIndex;
    }

    void SetNodeCount(int nodeCount){
        mAVSMNodeCount = nodeCount;
        mShaderIdx = nodeCount / 4 - 1;
    }

    // Wrap specific constant buffers needed when drawing CPUT models.  By wrapping
    // them with a string name, the autobinding feature will auto-bind the constant
    // buffer for VS and PS shaders
    //-----------------------------------------------------------------------------
    void WrapBuffers(){
        // The autobinding directive is found infound in the *.mtl files
        // The string name specified here is whatever you'd like to use internally, but
        // the name specified in the .mtl file is the name that needs to match what is in the .fx file
        // so, syntax in .mtl file is:
        // [.fx buffer name] = [CPUTBufferDX11 internal buffer name]

        // Create the per-frame AVSM data constant buffer.
        {
            /*D3D11_BUFFER_DESC bd = {0};
            bd.ByteWidth = sizeof(XMVECTOR);
            bd.BindFlags = D3D11_BIND_CONSTANT_BUFFER;
            bd.Usage = D3D11_USAGE_DYNAMIC;
            bd.CPUAccessFlags = D3D11_CPU_ACCESS_WRITE;
            ID3D11Buffer *pTempConstantBuffer;
            HRESULT hr = (CPUT_DX11::GetDevice())->CreateBuffer( &bd, NULL, &pTempConstantBuffer );
            ASSERT( !FAILED( hr ), _L("Error creating constant buffer.") );
            CPUTSetDebugName( pTempConstantBuffer, _L("Per-Frame Constant buffer") );
            cString name = _L("$cbAVSMValues");
            mAVSMConstantBufferCPUT = new CPUTBufferDX11( name, pTempConstantBuffer );
            CPUTAssetLibrary::GetAssetLibrary()->AddConstantBuffer( name, mAVSMConstantBufferCPUT );
            SAFE_RELEASE(pTempConstantBuffer); // We're done with it.  The CPUTBuffer now owns it*/
            mAVSMConstantBufferCPUT = new BufferGL();
            mAVSMConstantBufferCPUT.initlize(GLenum.GL_UNIFORM_BUFFER, Vector4f.SIZE, null, GLenum.GL_DYNAMIC_DRAW);
        }

        // 'cbuffer AVSMConstants' found in AVSM.hlsl, data mirrored by 'struct AVSMConstants' in AppShaderConstant.h
        {
            // Wrap the per-frame AVSM data constant buffer.
            /*CPUTSetDebugName( mAVSMConstants, _L("AVSMConstants constant buffer in AVSM.hlsl") );
            cString name = _L("$cbAVSMConstants");
            mAVSMConstantsCPUT = new CPUTBufferDX11( name, mAVSMConstants );
            CPUTAssetLibrary::GetAssetLibrary()->AddConstantBuffer( name, mAVSMConstantsCPUT );*/
            mAVSMConstantsCPUT = mAVSMConstants;
        }

        // 'cbuffer PerFrameConstants' found in Gbuffer.hlsl, data mirrored by 'struct PerFrameConstants' in AppShaderConstant.h
        {
            // Wrap the per-frame AVSM data constant buffer.
            /*CPUTSetDebugName( mPerFrameConstants, _L("PerFrameConstants constant buffer in Gbuffer.hlsl") );
            cString name = _L("$cbPerFrameConstants");
            mPerFrameConstantsCPUT = new CPUTBufferDX11( name, mPerFrameConstants );
            CPUTAssetLibrary::GetAssetLibrary()->AddConstantBuffer( name, mPerFrameConstantsCPUT );*/
            mPerFrameConstantsCPUT = mPerFrameConstants;
        }

        // 'cbuffer ParticlePerFrameConstants' found in Gbuffer.hlsl, data mirrored by 'struct ParticlePerFrameConstants' in ConstantBuffers.hlsl
        {
            // Create the per-frame AVSM data constant buffer.
            /*CPUTSetDebugName( mParticlePerFrameConstants, _L("ParticlePerFrameConstants constant buffer in Gbuffer.hlsl") );
            cString name = _L("$cbParticlePerFrameConstants");
            mParticlePerFrameConstantsCPUT = new CPUTBufferDX11( name, mParticlePerFrameConstants );
            CPUTAssetLibrary::GetAssetLibrary()->AddConstantBuffer( name, mParticlePerFrameConstantsCPUT );*/
            mParticlePerFrameConstantsCPUT = mParticlePerFrameConstants;
        }
    }

    void LoadAVSMShaders(/*ID3D11Device *d3dDevice,
                         ID3D11DeviceContext* d3dDeviceContext,*/
                          int nodeCount,
                          int shadowTextureDim,
                          int avsmShadowTextureDim){
        mAVSMNodeCount = nodeCount;
        mShaderIdx = (nodeCount+3) / 4 - 1;

        // TODO
    }

    void CreateAVSMRenderStates(/*ID3D11Device *d3dDevice,
                                ID3D11DeviceContext* d3dDeviceContext,*/
                                int nodeCount,
                                int shadowTextureDim,
                                int avsmShadowTextureDim){
        // Create standard rasterizer state
        {
            /*CD3D11_RASTERIZER_DESC desc(D3D11_DEFAULT);
            d3dDevice->CreateRasterizerState(&desc, &mRasterizerState);*/
            mRasterizerState=()->
            {
                gl.glCullFace(GLenum.GL_BACK);
                gl.glFrontFace(GLenum.GL_CCW); // TODO ????
                gl.glEnable(GLenum.GL_CULL_FACE);
                gl.glPolygonMode(GLenum.GL_FRONT_AND_BACK, GLenum.GL_FILL);
            };
        }

        // LD: Create particle wireframe rasterizer state
        {
            /*CD3D11_RASTERIZER_DESC desc(D3D11_DEFAULT);
            desc.CullMode = D3D11_CULL_NONE;
            desc.DepthClipEnable = false;
            desc.FillMode = D3D11_FILL_WIREFRAME;
            d3dDevice->CreateRasterizerState(&desc, &mParticleWireFrameState);*/

            mParticleWireFrameState = ()->
            {
                gl.glDisable(GLenum.GL_CULL_FACE);
                gl.glPolygonMode(GLenum.GL_FRONT_AND_BACK, GLenum.GL_LINE);
            };
        }

        // Create double-sided standard rasterizer state
        {
            /*CD3D11_RASTERIZER_DESC desc(D3D11_DEFAULT);
            desc.CullMode = D3D11_CULL_NONE;
            d3dDevice->CreateRasterizerState(&desc, &mDoubleSidedRasterizerState);*/
            mDoubleSidedRasterizerState = ()->
            {
                gl.glDisable(GLenum.GL_CULL_FACE);
                gl.glPolygonMode(GLenum.GL_FRONT_AND_BACK, GLenum.GL_FILL);
            };
        }

        // Shadow rasterizer state has no back-face culling and multisampling enabled
        {
            /*CD3D11_RASTERIZER_DESC desc(D3D11_DEFAULT);
            desc.CullMode = D3D11_CULL_NONE;
            desc.MultisampleEnable = true;
            desc.DepthClipEnable = false;
            d3dDevice->CreateRasterizerState(&desc, &mShadowRasterizerState);*/
            mShadowRasterizerState = ()->
            {
                gl.glDisable(GLenum.GL_CULL_FACE);
                gl.glEnable(GLenum.GL_MULTISAMPLE);
            };
        }

        // Create particle rasterizer state
        {
            /*CD3D11_RASTERIZER_DESC desc(D3D11_DEFAULT);
            desc.CullMode = D3D11_CULL_NONE;
            desc.DepthClipEnable = false;
            d3dDevice->CreateRasterizerState(&desc, &mParticleRasterizerState);*/
            mParticleRasterizerState = ()->
            {
                gl.glDisable(GLenum.GL_CULL_FACE);
            };
        }

        // Create default depth-stencil state
        {
            /*CD3D11_DEPTH_STENCIL_DESC desc(D3D11_DEFAULT);
            desc.DepthFunc = D3D11_COMPARISON_LESS_EQUAL;
            d3dDevice->CreateDepthStencilState(&desc, &mDefaultDepthStencilState);*/
            mDefaultDepthStencilState = ()->
            {
                gl.glEnable(GLenum.GL_DEPTH_TEST);
                gl.glDepthFunc(GLenum.GL_LEQUAL);
                gl.glDepthMask(true);
                gl.glDisable(GLenum.GL_STENCIL_TEST);
            };
        }

        // Create particle depth-stencil state
        {
            /*CD3D11_DEPTH_STENCIL_DESC desc(D3D11_DEFAULT);
            desc.DepthWriteMask = D3D11_DEPTH_WRITE_MASK_ZERO;
            desc.DepthEnable = false;
            desc.DepthFunc = D3D11_COMPARISON_ALWAYS;
            d3dDevice->CreateDepthStencilState(&desc, &mParticleDepthStencilState);*/
            mParticleDepthStencilState = ()->
            {
                gl.glDisable(GLenum.GL_DEPTH_TEST);
                gl.glDepthMask(false);
                gl.glDepthFunc(GLenum.GL_ALWAYS);
                gl.glDisable(GLenum.GL_STENCIL_TEST);
            };
        }

        // Create particle depth-stencil state
        {
            /*CD3D11_DEPTH_STENCIL_DESC desc(D3D11_DEFAULT);
            desc.DepthWriteMask = D3D11_DEPTH_WRITE_MASK_ZERO;
            desc.DepthEnable    = false;
            d3dDevice->CreateDepthStencilState(&desc, &mAVSMCaptureDepthStencilState);*/
            mAVSMCaptureDepthStencilState = ()->
            {
                gl.glDisable(GLenum.GL_DEPTH_TEST);
                gl.glDepthMask(false);
                gl.glDepthFunc(GLenum.GL_LESS);
                gl.glDisable(GLenum.GL_STENCIL_TEST);
            };
        }

        // Create geometry phase blend state
        {
            /*CD3D11_BLEND_DESC desc(D3D11_DEFAULT);
            d3dDevice->CreateBlendState(&desc, &mGeometryBlendState);*/
            mGeometryBlendState = ()->
            {
                gl.glDisable(GLenum.GL_BLEND);
            };
        }


        // Create lighting phase blend state
        {
            /*CD3D11_BLEND_DESC desc(D3D11_DEFAULT);
            // Additive blending
            desc.RenderTarget[0].BlendEnable = true;
            desc.RenderTarget[0].SrcBlend = D3D11_BLEND_ONE;
            desc.RenderTarget[0].DestBlend = D3D11_BLEND_ONE;
            desc.RenderTarget[0].BlendOp = D3D11_BLEND_OP_ADD;
            d3dDevice->CreateBlendState(&desc, &mLightingBlendState);*/
            mLightingBlendState = ()->
            {
                gl.glEnable(GLenum.GL_BLEND);
                gl.glBlendFuncSeparate(GLenum.GL_ONE, GLenum.GL_ONE, GLenum.GL_ONE, GLenum.GL_ZERO);
            };
        }

        // Create alpha phase blend state
        {
            /*CD3D11_BLEND_DESC desc(D3D11_DEFAULT);
            desc.RenderTarget[0].BlendEnable = true;
            desc.RenderTarget[0].SrcBlend = D3D11_BLEND_SRC_ALPHA;
            desc.RenderTarget[0].DestBlend = D3D11_BLEND_INV_SRC_ALPHA;
            desc.RenderTarget[0].BlendOp = D3D11_BLEND_OP_ADD;
            d3dDevice->CreateBlendState(&desc, &mParticleBlendState);*/
            mParticleBlendState = ()->
            {
                gl.glEnable(GLenum.GL_BLEND);
                gl.glBlendFuncSeparate(GLenum.GL_SRC_ALPHA, GLenum.GL_ONE_MINUS_SRC_ALPHA, GLenum.GL_ONE, GLenum.GL_ZERO);
            };
        }

        // Create constant buffers
        {
            /*CD3D11_BUFFER_DESC desc(
                sizeof(PerFrameConstants),
                D3D11_BIND_CONSTANT_BUFFER,
                D3D11_USAGE_DYNAMIC,
                D3D11_CPU_ACCESS_WRITE);

            d3dDevice->CreateBuffer(&desc, 0, &mPerFrameConstants);*/
            mPerFrameConstants = new BufferGL();
            mPerFrameConstants.initlize(GLenum.GL_UNIFORM_BUFFER, PerFrameConstants.SIZE, null, GLenum.GL_DYNAMIC_DRAW);
        }

        {
            /*CD3D11_BUFFER_DESC desc(
                sizeof(ParticlePerFrameConstants),
                D3D11_BIND_CONSTANT_BUFFER,
                D3D11_USAGE_DYNAMIC,
                D3D11_CPU_ACCESS_WRITE);

            d3dDevice->CreateBuffer(&desc, 0, &mParticlePerFrameConstants);*/
            mParticlePerFrameConstants = new BufferGL();
            mParticlePerFrameConstants.initlize(GLenum.GL_UNIFORM_BUFFER, ParticlePerFrameConstants.SIZE, null, GLenum.GL_DYNAMIC_DRAW);
        }
        {
            /*CD3D11_BUFFER_DESC desc(
                sizeof(ParticlePerPassConstants),
                D3D11_BIND_CONSTANT_BUFFER,
                D3D11_USAGE_DYNAMIC,
                D3D11_CPU_ACCESS_WRITE);
            d3dDevice->CreateBuffer(&desc, 0, &mParticlePerPassConstants);*/
            mParticlePerPassConstants = new BufferGL();
            mParticlePerPassConstants.initlize(GLenum.GL_UNIFORM_BUFFER, ParticlePerPassConstants.SIZE, null, GLenum.GL_DYNAMIC_DRAW);
        }

        {
            /*CD3D11_BUFFER_DESC desc(
                sizeof(LT_Constants),
                D3D11_BIND_CONSTANT_BUFFER,
                D3D11_USAGE_DYNAMIC,
                D3D11_CPU_ACCESS_WRITE);
            d3dDevice->CreateBuffer(&desc, 0, &mListTextureConstants);*/
            mListTextureConstants = new BufferGL();
            mListTextureConstants.initlize(GLenum.GL_UNIFORM_BUFFER, LT_Constants.SIZE, null, GLenum.GL_DYNAMIC_DRAW);
        }

        {
            /*CD3D11_BUFFER_DESC desc(
                sizeof(AVSMConstants),
                D3D11_BIND_CONSTANT_BUFFER,
                D3D11_USAGE_DYNAMIC,
                D3D11_CPU_ACCESS_WRITE);
            d3dDevice->CreateBuffer(&desc, 0, &mAVSMConstants);*/
            mAVSMConstants = new BufferGL();
            mAVSMConstants.initlize(GLenum.GL_UNIFORM_BUFFER, AVSMConstants.SIZE, null, GLenum.GL_DYNAMIC_DRAW);
        }

        // Create sampler states
        {
            /*CD3D11_SAMPLER_DESC desc(D3D11_DEFAULT);
            desc.Filter = D3D11_FILTER_MIN_MAG_MIP_POINT;
            desc.AddressU = D3D11_TEXTURE_ADDRESS_CLAMP;
            desc.AddressV = D3D11_TEXTURE_ADDRESS_CLAMP;
            desc.AddressW = D3D11_TEXTURE_ADDRESS_CLAMP;
            d3dDevice->CreateSamplerState(&desc, &mAVSMSampler);*/
            SamplerDesc desc = new SamplerDesc();
            desc.magFilter = GLenum.GL_NEAREST;
            desc.minFilter = GLenum.GL_NEAREST_MIPMAP_NEAREST;
            desc.wrapR = GLenum.GL_CLAMP_TO_EDGE;
            desc.wrapS = GLenum.GL_CLAMP_TO_EDGE;
            desc.wrapT = GLenum.GL_CLAMP_TO_EDGE;
            mAVSMSampler = SamplerUtils.createSampler(desc);
        }

        {
            /*float borderColor[4] = {0.f, 0.f, 0.f, 0.f};
            CD3D11_SAMPLER_DESC desc(D3D11_DEFAULT);
            desc.Filter = D3D11_FILTER_MIN_MAG_MIP_LINEAR;
            desc.AddressU = D3D11_TEXTURE_ADDRESS_BORDER;
            desc.AddressV = D3D11_TEXTURE_ADDRESS_BORDER;
            desc.AddressW = D3D11_TEXTURE_ADDRESS_BORDER;
            *desc.BorderColor = *borderColor;
            d3dDevice->CreateSamplerState(&desc, &mAVSMGenSampler);*/
            SamplerDesc desc = new SamplerDesc();
            desc.magFilter = GLenum.GL_LINEAR;
            desc.minFilter = GLenum.GL_LINEAR_MIPMAP_LINEAR;
            desc.wrapR = GLenum.GL_CLAMP_TO_BORDER;
            desc.wrapS = GLenum.GL_CLAMP_TO_BORDER;
            desc.wrapT = GLenum.GL_CLAMP_TO_BORDER;
            desc.borderColor = 0;
            mAVSMGenSampler = SamplerUtils.createSampler(desc);
        }
    }

    void CreateAVSMBuffers(/*ID3D11Device *d3dDevice,
                           ID3D11DeviceContext* d3dDeviceContext,*/
                           int nodeCount,
                           int avsmShadowTextureDim){
        // Create AVSM textures and viewport
        {
            /*DXGI_SAMPLE_DESC sampleDesc;
            sampleDesc.Count = 1;
            sampleDesc.Quality = 0;
            mAVSMTextures = std::shared_ptr<Texture2D>(new Texture2D(
                    d3dDevice, mAVSMShadowTextureDim, mAVSMShadowTextureDim, DXGI_FORMAT_R32G32B32A32_FLOAT,
                    D3D11_BIND_RENDER_TARGET | D3D11_BIND_SHADER_RESOURCE, MAX_AVSM_RT_COUNT, sampleDesc,
                    D3D11_RTV_DIMENSION_TEXTURE2DARRAY, D3D11_UAV_DIMENSION_TEXTURE2DARRAY, D3D11_SRV_DIMENSION_TEXTURE2DARRAY));*/
            Texture2DDesc tex_desc = new Texture2DDesc(mAVSMShadowTextureDim, mAVSMShadowTextureDim, GLenum.GL_RGBA32F);
            tex_desc.arraySize = MAX_AVSM_RT_COUNT;
            mAVSMTextures = TextureUtils.createTexture2D(tex_desc, null);

            /*mAVSMShadowViewport.Width    = static_cast<float>(mAVSMShadowTextureDim);
            mAVSMShadowViewport.Height   = static_cast<float>(mAVSMShadowTextureDim);
            mAVSMShadowViewport.MinDepth = 0.0f;
            mAVSMShadowViewport.MaxDepth = 1.0f;
            mAVSMShadowViewport.TopLeftX = 0.0f;
            mAVSMShadowViewport.TopLeftY = 0.0f;*/
            mAVSMShadowViewport.x = 0;
            mAVSMShadowViewport.y = 0;
            mAVSMShadowViewport.z = mAVSMShadowTextureDim;
            mAVSMShadowViewport.w = mAVSMShadowTextureDim;
        }

        // Create AVSM debug textures
        {
            /*HRESULT hr;
            CD3D11_TEXTURE2D_DESC desc(
                DXGI_FORMAT_R32G32B32A32_FLOAT,
                mAVSMShadowTextureDim,
                mAVSMShadowTextureDim,
                MAX_AVSM_RT_COUNT,
                1,
                0,
                D3D11_USAGE_STAGING,
                D3D10_CPU_ACCESS_READ);
            V(d3dDevice->CreateTexture2D(&desc, 0, &mAVSMTexturesDebug));*/

            Texture2DDesc tex_desc = new Texture2DDesc(mAVSMShadowTextureDim, mAVSMShadowTextureDim, GLenum.GL_RGBA32F);
            tex_desc.arraySize = MAX_AVSM_RT_COUNT;
            mAVSMTexturesDebug = TextureUtils.createTexture2D(tex_desc, null);  // TODO
        }

        {
//            HRESULT hr;
            int structSize = /*sizeof(float)*/4 * 2 * mAVSMNodeCount;

            /*CD3D11_BUFFER_DESC desc(
                structSize * mAVSMShadowTextureDim * mAVSMShadowTextureDim,
                D3D11_BIND_SHADER_RESOURCE | D3D11_BIND_UNORDERED_ACCESS,
                D3D11_USAGE_DEFAULT,
                0,
                D3D11_RESOURCE_MISC_BUFFER_STRUCTURED,
                structSize);
            V(d3dDevice->CreateBuffer(&desc, 0, &mAVSMStructBuf));*/
            mAVSMStructBuf = new BufferGL();
            mAVSMStructBuf.initlize(GLenum.GL_ARRAY_BUFFER, structSize * mAVSMShadowTextureDim * mAVSMShadowTextureDim, null, GLenum.GL_STREAM_DRAW);

            /*CD3D11_UNORDERED_ACCESS_VIEW_DESC unorderedAccessResourceDesc(
                D3D11_UAV_DIMENSION_BUFFER,
                DXGI_FORMAT_UNKNOWN,
                0, mAVSMShadowTextureDim * mAVSMShadowTextureDim, 1, 0);
            V(d3dDevice->CreateUnorderedAccessView(mAVSMStructBuf, &unorderedAccessResourceDesc, &mAVSMStructBufUAV));
            CD3D11_SHADER_RESOURCE_VIEW_DESC shaderResourceDesc(
                D3D11_SRV_DIMENSION_BUFFER,
                DXGI_FORMAT_UNKNOWN,
                0, mAVSMShadowTextureDim * mAVSMShadowTextureDim, 1);
            V(d3dDevice->CreateShaderResourceView(mAVSMStructBuf, &shaderResourceDesc, &mAVSMStructBufSRV));*/
            mAVSMStructBufUAV = mAVSMStructBufSRV = mAVSMStructBuf;
        }

        // Create List Texture first segment node offset texture
        {
            /*DXGI_SAMPLE_DESC sampleDesc;
            sampleDesc.Count = 1;
            sampleDesc.Quality = 0;
            mListTexFirstSegmentNodeOffset = std::shared_ptr<Texture2D>(new Texture2D(
                    d3dDevice, mAVSMShadowTextureDim, mAVSMShadowTextureDim, DXGI_FORMAT_R32_UINT,
                    D3D11_BIND_UNORDERED_ACCESS | D3D11_BIND_SHADER_RESOURCE, 1, sampleDesc,
                    D3D11_RTV_DIMENSION_UNKNOWN, D3D11_UAV_DIMENSION_TEXTURE2D, D3D11_SRV_DIMENSION_TEXTURE2D));*/
            mListTexFirstSegmentNodeOffset = TextureUtils.createTexture2D(new Texture2DDesc(mAVSMShadowTextureDim, mAVSMShadowTextureDim, GLenum.GL_R32UI), null); //
        }

        // Create List Texture first visibility node offset texture
        {
            /*DXGI_SAMPLE_DESC sampleDesc;
            sampleDesc.Count = 1;
            sampleDesc.Quality = 0;
            mListTexFirstVisibilityNodeOffset = std::shared_ptr<Texture2D>(new Texture2D(
                    d3dDevice, mAVSMShadowTextureDim, mAVSMShadowTextureDim, DXGI_FORMAT_R32_UINT,
                    D3D11_BIND_UNORDERED_ACCESS | D3D11_BIND_SHADER_RESOURCE, 1, sampleDesc,
                    D3D11_RTV_DIMENSION_UNKNOWN, D3D11_UAV_DIMENSION_TEXTURE2D, D3D11_SRV_DIMENSION_TEXTURE2D));*/
            mListTexFirstVisibilityNodeOffset = TextureUtils.createTexture2D(new Texture2DDesc(mAVSMShadowTextureDim, mAVSMShadowTextureDim, GLenum.GL_R32UI), null);
        }

        // Create List Texture segment nodes buffer for AVSM data capture
        {
            /*HRESULT hr;
            UINT structSize = sizeof(SegmentNode);
            CD3D11_BUFFER_DESC desc(
                structSize * mLisTexNodeCount,
                D3D11_BIND_SHADER_RESOURCE | D3D11_BIND_UNORDERED_ACCESS,
                D3D11_USAGE_DEFAULT,
                0,
                D3D11_RESOURCE_MISC_BUFFER_STRUCTURED,
                structSize);
            V(d3dDevice->CreateBuffer(&desc, 0, &mListTexSegmentNodes));*/
            mListTexSegmentNodes = new BufferGL();
            mListTexSegmentNodes.initlize(GLenum.GL_SHADER_STORAGE_BUFFER, SegmentNode.SIZE * mLisTexNodeCount, null, GLenum.GL_STREAM_DRAW);

            /*CD3D11_UNORDERED_ACCESS_VIEW_DESC unorderedAccessResourceDesc(
                D3D11_UAV_DIMENSION_BUFFER,
                DXGI_FORMAT_UNKNOWN,
                0, mLisTexNodeCount, 1, D3D11_BUFFER_UAV_FLAG_COUNTER);
            V(d3dDevice->CreateUnorderedAccessView(mListTexSegmentNodes, &unorderedAccessResourceDesc, &mListTexSegmentNodesUAV));
            CD3D11_SHADER_RESOURCE_VIEW_DESC shaderResourceDesc(
                D3D11_SRV_DIMENSION_BUFFER,
                DXGI_FORMAT_UNKNOWN,
                0, mLisTexNodeCount, 1);
            V(d3dDevice->CreateShaderResourceView(mListTexSegmentNodes, &shaderResourceDesc, &mListTexSegmentNodesSRV));*/
            mListTexSegmentNodesUAV = mListTexSegmentNodesSRV = mListTexSegmentNodes;
        }

        // Create List Texture visibility nodes buffer for AVSM data capture
        {
            /*HRESULT hr;
            UINT structSize = sizeof(VisibilityNode);
            CD3D11_BUFFER_DESC desc(
                structSize * mLisTexNodeCount,
                D3D11_BIND_SHADER_RESOURCE | D3D11_BIND_UNORDERED_ACCESS,
                D3D11_USAGE_DEFAULT,
                0,
                D3D11_RESOURCE_MISC_BUFFER_STRUCTURED,
                structSize);
            V(d3dDevice->CreateBuffer(&desc, 0, &mListTexVisibilityNodes));*/
            mListTexVisibilityNodes = new BufferGL();
            mListTexVisibilityNodes.initlize(GLenum.GL_SHADER_STORAGE_BUFFER, VisibilityNode.SIZE * mLisTexNodeCount, null, GLenum.GL_STREAM_DRAW);

            /*CD3D11_UNORDERED_ACCESS_VIEW_DESC unorderedAccessResourceDesc(
                D3D11_UAV_DIMENSION_BUFFER,
                DXGI_FORMAT_UNKNOWN,
                0, mLisTexNodeCount, 1, D3D11_BUFFER_UAV_FLAG_COUNTER);
            V(d3dDevice->CreateUnorderedAccessView(mListTexVisibilityNodes, &unorderedAccessResourceDesc, &mListTexVisibilityNodesUAV));
            CD3D11_SHADER_RESOURCE_VIEW_DESC shaderResourceDesc(
                D3D11_SRV_DIMENSION_BUFFER,
                DXGI_FORMAT_UNKNOWN,
                0, mLisTexNodeCount, 1);
            V(d3dDevice->CreateShaderResourceView(mListTexVisibilityNodes, &shaderResourceDesc, &mListTexVisibilityNodesSRV));*/
            mListTexVisibilityNodesUAV = mListTexVisibilityNodesSRV = mListTexVisibilityNodes;
        }

        ////////////////////////////////////////////////////////////////////////////////////////////
        // AVSM Gen resources
        ////////////////////////////////////////////////////////////////////////////////////////////
        {
            /*DXGI_SAMPLE_DESC sampleDesc;
            sampleDesc.Count = 1;
            sampleDesc.Quality = 0;
            mAVSMGenCtrlSurface = std::shared_ptr<Texture2D>(new Texture2D(
                    d3dDevice, mAVSMShadowTextureDim, mAVSMShadowTextureDim, DXGI_FORMAT_R32_FLOAT,
                    D3D11_BIND_UNORDERED_ACCESS | D3D11_BIND_SHADER_RESOURCE, 1, sampleDesc,
                    D3D11_RTV_DIMENSION_UNKNOWN, D3D11_UAV_DIMENSION_TEXTURE2D, D3D11_SRV_DIMENSION_TEXTURE2D));*/
            mAVSMGenCtrlSurface = TextureUtils.createTexture2D(new Texture2DDesc(mAVSMShadowTextureDim, mAVSMShadowTextureDim, GLenum.GL_R32F), null);
        }

        // Create the mirrored CPUT container texture for mAVSMTextures
        if(mAVSMGenCtrlSurfaceCPUT == null)
        {
            // make an internal/system-generated texture
            /*cString name = _L("$gAVSMGenCtrlSurface");                                         // $ name indicates not loaded from file
            mAVSMGenCtrlSurfaceCPUT = new CPUTTextureDX11(name);
            // get needed pointers
            ID3D11Texture2D *pSourceTexture2D = mAVSMGenCtrlSurface->GetTexture();
            ID3D11ShaderResourceView* avsmGenCtrlSurface = mAVSMGenCtrlSurface->GetShaderResource();
            // wrap the previously created objects
            mAVSMGenCtrlSurfaceCPUT->SetTextureAndShaderResourceView(pSourceTexture2D, avsmGenCtrlSurface);*/
            mAVSMGenCtrlSurfaceCPUT = mAVSMGenCtrlSurface;
        }


        for (int i = 0; i < MAX_SHADER_VARIATIONS; ++i)
        {
//            HRESULT hr;
            int structSize = 16 * (i+1);

            /*CD3D11_BUFFER_DESC desc(
                structSize * mAVSMShadowTextureDim * mAVSMShadowTextureDim,
                D3D11_BIND_SHADER_RESOURCE | D3D11_BIND_UNORDERED_ACCESS,
                D3D11_USAGE_DEFAULT,
                0,
                D3D11_RESOURCE_MISC_BUFFER_STRUCTURED,
                structSize);
            V(d3dDevice->CreateBuffer(&desc, 0, &mAVSMGenData[i]));*/

            mAVSMGenData[i] = new BufferGL();
            mAVSMGenData[i].initlize(GLenum.GL_SHADER_STORAGE_BUFFER, structSize * mAVSMShadowTextureDim * mAVSMShadowTextureDim, null, GLenum.GL_STREAM_DRAW);

            /*CD3D11_UNORDERED_ACCESS_VIEW_DESC unorderedAccessResourceDesc(
                D3D11_UAV_DIMENSION_BUFFER,
                DXGI_FORMAT_UNKNOWN,
                0, mAVSMShadowTextureDim * mAVSMShadowTextureDim, 1, 0);
            V(d3dDevice->CreateUnorderedAccessView(mAVSMGenData[i], &unorderedAccessResourceDesc, &mAVSMGenDataUAV[i]));
            CD3D11_SHADER_RESOURCE_VIEW_DESC shaderResourceDesc(
                D3D11_SRV_DIMENSION_BUFFER,
                DXGI_FORMAT_UNKNOWN,
                0, mAVSMShadowTextureDim * mAVSMShadowTextureDim, 1);
            V(d3dDevice->CreateShaderResourceView(mAVSMGenData[i], &shaderResourceDesc, &mAVSMGenDataSRV[i]));*/
            mAVSMGenDataUAV[i] = mAVSMGenDataSRV[i] = mAVSMGenData[i];
        }
    }

    void DeleteAVSMBuffers(/*ID3D11Device *d3dDevice,
                           ID3D11DeviceContext* d3dDeviceContext,*/
                           int nodeCount){
        for (int i = 0; i < MAX_SHADER_VARIATIONS; ++i)
        {
            CommonUtil.safeRelease(mAVSMGenDataSRV[i]);
            CommonUtil.safeRelease(mAVSMGenDataUAV[i]);
            CommonUtil.safeRelease(mAVSMGenData[i]);
        }

        ////////////////////////////////////////////////////////////////////////////////////////////
        // AVSM Gen resources
        ////////////////////////////////////////////////////////////////////////////////////////////
//        mAVSMGenCtrlSurface.reset();
        CommonUtil.safeRelease(mAVSMGenCtrlSurface);

        // Create List Texture visibility nodes buffer for AVSM data capture
        {
            CommonUtil.safeRelease(mListTexVisibilityNodesSRV);
            CommonUtil.safeRelease(mListTexVisibilityNodesUAV);
            CommonUtil.safeRelease(mListTexVisibilityNodes);
        }

        // Create List Texture segment nodes buffer for AVSM data capture
        {
            CommonUtil.safeRelease(mListTexSegmentNodesSRV);
            CommonUtil.safeRelease(mListTexSegmentNodesUAV);
            CommonUtil.safeRelease(mListTexSegmentNodes);
        }

        // Create List Texture first segment node offset texture
        CommonUtil.safeRelease(mListTexFirstSegmentNodeOffset);

        // Create List Texture first visibility node offset texture
        CommonUtil.safeRelease(mListTexFirstVisibilityNodeOffset);

        // Create List texture first offset debug texture
//	SAFE_RELEASE(mListTexFirstOffsetDebug);

        {
            CommonUtil.safeRelease(mAVSMStructBufSRV);
            CommonUtil.safeRelease(mAVSMStructBufUAV);
            CommonUtil.safeRelease(mAVSMStructBuf);
        }


        // Create AVSM debug textures
        {
            CommonUtil.safeRelease(mAVSMTexturesDebug);
        }

        // Create AVSM textures and viewport
        CommonUtil.safeRelease(mAVSMTextures);
    }


    private void CaptureFragments(//ID3D11DeviceContext* d3dDeviceContext,
                          ParticleSystem particleSystem,
                          UIConstants ui,
                          boolean initCounter){

    }

    private void GenerateVisibilityCurve(//ID3D11DeviceContext* d3dDeviceContext,
                                 UIConstants ui){

    }


    private void RenderShadedParticles(//ID3D11DeviceContext* d3dDeviceContext,
                               ParticleSystem particleSystem,
                               Texture2D backBuffer,
                               Texture2D depthBuffer,
                               Vector4i viewport,
                        UIConstants ui){

    }

    private void FillInFrameConstants(//ID3D11DeviceContext* d3dDeviceContext,
                              FrameMatrices m,
                              Vector3f cameraPos,
                              CPUTCamera viewerCamera,
                              CPUTCamera lightCamera,
                              float ScreenWidth, float ScreenHeight,
                              UIConstants ui){

    }

    private void FillParticleRendererConstants(//ID3D11DeviceContext* d3dDeviceContext,
                                       CPUTCamera lightCamera,
                                       Matrix4f cameraView,
                                       Matrix4f cameraViewProj){

    }

    private void FillListTextureConstants(/*ID3D11DeviceContext* d3dDeviceContext*/){

    }

    private void FillAVSMConstants(/*ID3D11DeviceContext* d3dDeviceContext*/){

    }

    private void ClearShadowBuffers(/*ID3D11DeviceContext* d3dDeviceContext,*/UIConstants ui){

    }

    @Override
    public void dispose() {
        CommonUtil.safeRelease(mAVSMGenCtrlSurfaceCPUT);
        CommonUtil.safeRelease(mAVSMTexturesCPUT);
        for(int ii=0; ii<MAX_SHADER_VARIATIONS; ii++)
        {
            CommonUtil.safeRelease(mAVSMGenDataSRVCPUT[ii]);
        }
        CommonUtil.safeRelease(mAVSMConstantBufferCPUT);
        CommonUtil.safeRelease(mAVSMConstantsCPUT);
        CommonUtil.safeRelease(mPerFrameConstantsCPUT);
        CommonUtil.safeRelease(mParticlePerFrameConstantsCPUT);

//        CommonUtil.safeRelease(mParticleDepthStencilState);
//        CommonUtil.safeRelease(mDefaultDepthStencilState);
//        CommonUtil.safeRelease(mAVSMCaptureDepthStencilState);
//        CommonUtil.safeRelease(mAVSMSampler);
//        CommonUtil.safeRelease(mAVSMGenSampler);
        CommonUtil.safeRelease(mPerFrameConstants);
        CommonUtil.safeRelease(mParticlePerFrameConstants);
        CommonUtil.safeRelease(mParticlePerPassConstants);
        CommonUtil.safeRelease(mListTextureConstants);
        CommonUtil.safeRelease(mAVSMConstants);
//        CommonUtil.safeRelease(mLightingBlendState);
//        CommonUtil.safeRelease(mGeometryBlendState);
//        CommonUtil.safeRelease(mParticleBlendState);
//        CommonUtil.safeRelease(mShadowRasterizerState);
//        CommonUtil.safeRelease(mRasterizerState);
//        CommonUtil.safeRelease(mDoubleSidedRasterizerState);
//        CommonUtil.safeRelease(mParticleWireFrameState);

//        CommonUtil.safeRelease(mParticleRasterizerState);
//        CommonUtil.safeRelease(mFullScreenTriangleVS);
//        CommonUtil.safeRelease(mFullScreenTriangleVSReflector);
//        CommonUtil.safeRelease(mParticleShadingVS);
//        CommonUtil.safeRelease(mParticleShadingVSReflector);
//        CommonUtil.safeRelease(mAVSMClearStructuredBufPS);
//        CommonUtil.safeRelease(mAVSMClearStructuredBufPSReflector);
//        CommonUtil.safeRelease(mParticleAVSMCapturePS);
//        CommonUtil.safeRelease(mParticleAVSMCapturePSReflector);
        CommonUtil.safeRelease(mListTexSegmentNodes);
        CommonUtil.safeRelease(mListTexSegmentNodesUAV);
        CommonUtil.safeRelease(mListTexSegmentNodesSRV);
        CommonUtil.safeRelease(mListTexVisibilityNodes);
        CommonUtil.safeRelease(mListTexVisibilityNodesUAV);
        CommonUtil.safeRelease(mListTexVisibilityNodesSRV);
        CommonUtil.safeRelease(mAVSMStructBuf);

        CommonUtil.safeRelease(mAVSMTexturesDebug);
        CommonUtil.safeRelease(mAVSMStructBufUAV);
        CommonUtil.safeRelease(mAVSMStructBufSRV);

//        CommonUtil.safeRelease(mParticleShadingTessellationVS);
//        CommonUtil.safeRelease(mParticleShadingTessellationVSReflector);
//        CommonUtil.safeRelease(mParticleShadingHS);
//        CommonUtil.safeRelease(mParticleShadingHSReflector);
//        CommonUtil.safeRelease(mParticleShadingPS);
//        CommonUtil.safeRelease(mParticleShadingPSReflector);

        for (int i = 0; i < MAX_SHADER_VARIATIONS; ++i) {
//            CommonUtil.safeRelease(mAVSMInsertionSortResolvePS[i]);
//            CommonUtil.safeRelease(mAVSMInsertionSortResolvePSReflector[i]);
//            CommonUtil.safeRelease(mAVSMSinglePassInsertPS[i]);
//            CommonUtil.safeRelease(mAVSMSinglePassInsertPSReflector[i]);
//            CommonUtil.safeRelease(mParticleShadingPerPixelPS[i]);
//            CommonUtil.safeRelease(mParticleShadingPerPixelPSReflector[i]);
            CommonUtil.safeRelease(mParticleAVSM_GenPS[i]);
//            CommonUtil.safeRelease(mParticleAVSM_GenPSReflector[i]);
//            CommonUtil.safeRelease(mParticleShadingDS[i]);
//            CommonUtil.safeRelease(mParticleShadingDSReflector[i]);
//            CommonUtil.safeRelease(mParticleShadingFinalVS[i]);
//            CommonUtil.safeRelease(mParticleShadingFinalVSReflector[i]);

            CommonUtil.safeRelease(mAVSMGenData[i]);
            CommonUtil.safeRelease(mAVSMGenDataUAV[i]);
            CommonUtil.safeRelease(mAVSMGenDataSRV[i]);
        }
    }

    /*void CreatePixelShadersFromCompiledObjs(//ID3D11Device* d3dDevice,
                                            int shaderCount,
                                           String prefix,
                                           String suffix,
                                            ID3D11PixelShader** shaderArray,
                                            ID3D11ShaderReflection** shaderReflArray);

    void CreateDomainShadersFromCompiledObjs(ID3D11Device* d3dDevice,
                                             int shaderCount,
                                           const char* prefix,
                                           const char* suffix,
                                             ID3D11DomainShader** shaderArray,
                                             ID3D11ShaderReflection** shaderReflArray);
    void CreateVertexShadersFromCompiledObjs(ID3D11Device* d3dDevice,
                                             int shaderCount,
                                           const char* prefix,
                                           const char* suffix,
                                             ID3D11VertexShader** shaderArray,
                                             ID3D11ShaderReflection** shaderReflArray);*/

    static final class Options{
        int scene = GROUND_PLANE_SCENE;
        boolean enableParticles = true;
        boolean enableAutoBoundsAVSM = true;
        boolean enableShadowPicking;
        int NodeCount = 8;
        boolean enableTransmittanceCurve;
        boolean enableVolumeShadowCreation = true;
        int pickedX;
        int pickedY;
    }

    static final class FrameMatrices{
        final Matrix4f worldMatrix = new Matrix4f();

        final Matrix4f cameraProj = new Matrix4f();
        final Matrix4f cameraView = new Matrix4f();
        final Matrix4f cameraViewInv = new Matrix4f();

        final Matrix4f cameraWorldViewProj = new Matrix4f();
        final Matrix4f cameraWorldView = new Matrix4f();

        final Matrix4f cameraViewProj = new Matrix4f();
        final Matrix4f cameraViewToLightProj = new Matrix4f();
        final Matrix4f cameraViewToLightView = new Matrix4f();
        final Matrix4f cameraViewToAvsmLightProj = new Matrix4f();
        final Matrix4f cameraViewToAvsmLightView = new Matrix4f();

        final Matrix4f lightProj = new Matrix4f();
        final Matrix4f lightView = new Matrix4f();
        final Matrix4f lightViewProj = new Matrix4f();
        final Matrix4f lightWorldViewProj = new Matrix4f();

        final Matrix4f avsmLightProj = new Matrix4f();
        final Matrix4f avsmLightView = new Matrix4f();
        final Matrix4f avsmLightViewProj = new Matrix4f();
        final Matrix4f avmsLightWorldViewProj = new Matrix4f();
    }

    static final class FrameContext{
        final FrameMatrices               Matrices = new FrameMatrices();
//        ID3D11DeviceContext*        D3dDeviceContext;
        final Options                     Options = new Options();
        ParticleSystem             ParticleSystem;
        CPUTCamera                 ViewerCamera;
        CPUTCamera                 LightCamera;
        final Vector4i Viewport = new Vector4i();
//        UIConstants                 GPUUIConstants;
        float					    mScreenWidth;
        float					    mScreenHeight;
        final FrameStats                  Stats = new FrameStats();
        Texture2D DepthBufferSRV;
    }
}
