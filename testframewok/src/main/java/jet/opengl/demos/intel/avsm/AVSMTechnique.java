package jet.opengl.demos.intel.avsm;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.ReadableVector3f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;
import org.lwjgl.util.vector.Vector4i;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import jet.opengl.demos.intel.cput.CPUTCamera;
import jet.opengl.postprocessing.buffer.BufferGL;
import jet.opengl.postprocessing.common.Disposeable;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.shader.GLSLProgramPipeline;
import jet.opengl.postprocessing.shader.Macro;
import jet.opengl.postprocessing.shader.ShaderProgram;
import jet.opengl.postprocessing.shader.ShaderType;
import jet.opengl.postprocessing.texture.RenderTargets;
import jet.opengl.postprocessing.texture.SamplerDesc;
import jet.opengl.postprocessing.texture.SamplerUtils;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.Texture2DDesc;
import jet.opengl.postprocessing.texture.TextureUtils;
import jet.opengl.postprocessing.util.CacheBuffer;
import jet.opengl.postprocessing.util.CommonUtil;

/**
 * Created by mazhen'gui on 2017/10/9.
 */

final class AVSMTechnique implements Disposeable{
    static final int VOL_SHADOW_NO_SHADOW = 0;
    static final int VOL_SHADOW_AVSM = 1;
    static final int VOL_SHADOW_AVSM_GEN = 2;

    static final int  MAX_AVSM_RT_COUNT  = (4);
    static final int  MAX_SHADER_VARIATIONS = (MAX_AVSM_RT_COUNT / 2);

    static final int POWER_PLANT_SCENE        = 0,
                     GROUND_PLANE_SCENE       = 1;
    static final float EMPTY_NODE = 65504.0f; // max half prec

    private int mLisTexNodeCount;


//    ID3D11VertexShader* mFullScreenTriangleVS;
//    ID3D11ShaderReflection* mFullScreenTriangleVSReflector;

    // Particle and AVSM shaders
    /*ID3D11VertexShader*  mParticleShadingVS;
    ID3D11ShaderReflection* mParticleShadingVSReflector;
    ID3D11PixelShader*   mParticleShadingPS;
    ID3D11ShaderReflection* mParticleShadingPSReflector;*/

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
    private final ShaderProgram[] mParticleShadingDS = new ShaderProgram[MAX_SHADER_VARIATIONS];
    private ShaderProgram mParticleShadingHS;
    private final ShaderProgram[] mParticleShadingFinalVS = new ShaderProgram[MAX_SHADER_VARIATIONS];
    private ShaderProgram mParticleShadingTessellationVS;
    private final ShaderProgram[] mParticleShadingPerPixelPS = new ShaderProgram[MAX_SHADER_VARIATIONS];
    private ShaderProgram mParticleShadingPS;
    private ShaderProgram mParticleShadingVS;

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
    private RenderTargets mRenderTargets;
    private GLSLProgramPipeline mProgramPipeline;

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
        mRenderTargets = new RenderTargets();
    }

    void InitializeFrameContext(FrameContext outContext, Options options, /*ID3D11DeviceContext* d3dDeviceContext,*/ ParticleSystem  particleSystem,
                                Matrix4f worldMatrix, CPUTCamera viewerCamera, CPUTCamera lightCamera, Vector4i viewport ){
        outContext.DepthBufferSRV   = null;
//        outContext.D3dDeviceContext = d3dDeviceContext;
        outContext.Options          .set(options);
        outContext.ParticleSystem   = particleSystem;
        outContext.ViewerCamera     = viewerCamera;
        outContext.LightCamera      = lightCamera;
        outContext.Viewport         .set(viewport);
//        memset( &outContext.Stats, 0, sizeof(outContext.Stats) );
        outContext.Stats            .reset();

        FrameMatrices frameMatx = outContext.Matrices;

        frameMatx.worldMatrix.load(worldMatrix);

        assert((options.enableParticles && particleSystem != null) ||
                (options.enableParticles == false));

//        frameMatx.cameraProj = DXUTFromCPUT( *viewerCamera->GetProjectionMatrix() );
//        frameMatx.cameraView = DXUTFromCPUT( *viewerCamera->GetViewMatrix() );
        frameMatx.cameraProj.load(viewerCamera.GetProjectionMatrix());
        frameMatx.cameraView.load(viewerCamera.GetViewMatrix());  // TODO a little strange.

//        D3DXMatrixInverse(&frameMatx.cameraViewInv, 0, &frameMatx.cameraView);
        Matrix4f.invert(frameMatx.cameraView, frameMatx.cameraViewInv);

        // We only use the view direction from the camera object
        // We then center the directional light on the camera frustum and set the
        // extents to completely cover it.
//        frameMatx.lightView = DXUTFromCPUT( *lightCamera->GetViewMatrix() );
        frameMatx.lightView.load(lightCamera.GetViewMatrix());
        {
            // NOTE: We don't include the projection matrix here, since we want to just get back the
            // raw view-space extents and use that to *build* the bounding projection matrix
            Vector3f min = new Vector3f(), max = new Vector3f();
            Utils.ComputeFrustumExtents(frameMatx.cameraViewInv, frameMatx.cameraProj,
                    viewerCamera.GetNearPlaneDistance(), viewerCamera.GetFarPlaneDistance(),
                    frameMatx.lightView, min, max);

            // First adjust the light matrix to be centered on the extents in x/y and behind everything in z
//            D3DXVECTOR3 center = 0.5f * (min + max);
//            D3DXMATRIXA16 centerTransform;
//            D3DXMatrixTranslation(&centerTransform, -center.x, -center.y, 0.0f);
//            frameMatx.lightView *= centerTransform;
//            Matrix4f centerTransform = new Matrix4f();
//            centerTransform.m30 = -(min.x + max.x)/2;
//            centerTransform.m31 = -(min.y + max.y)/2;
//            Matrix4f.mul(centerTransform, frameMatx.lightView, frameMatx.lightView);

            // Now create a projection matrix that covers the extents when centered
            // Optimization: Again use scene AABB to decide on light far range - this one can actually clip out
            // any objects further away than the frustum can see if desired.
//            D3DXVECTOR3 dimensions = max - min;
//            D3DXMatrixOrthoLH(&frameMatx.lightProj, dimensions.x, dimensions.y, 0.0f, 1000.0f);
            Matrix4f.ortho(min.x, max.x, min.y, max.y, 0.0f, 1000.0f, frameMatx.lightProj);
        }

        // Compute composite matrices
        /*frameMatx.cameraViewProj = frameMatx.cameraView * frameMatx.cameraProj;
        frameMatx.cameraWorldViewProj = frameMatx.worldMatrix * frameMatx.cameraViewProj;
        frameMatx.cameraWorldView = frameMatx.worldMatrix * frameMatx.cameraView;
        frameMatx.lightViewProj = frameMatx.lightView * frameMatx.lightProj;
        frameMatx.lightWorldViewProj = frameMatx.worldMatrix * frameMatx.lightViewProj;
        frameMatx.cameraViewToLightProj = frameMatx.cameraViewInv * frameMatx.lightViewProj;
        frameMatx.cameraViewToLightView = frameMatx.cameraViewInv * frameMatx.lightView;*/
        Matrix4f.mul(frameMatx.cameraProj, frameMatx.cameraView, frameMatx.cameraViewProj);
        Matrix4f.mul(frameMatx.cameraViewProj, frameMatx.worldMatrix, frameMatx.cameraWorldViewProj);
        Matrix4f.mul(frameMatx.cameraView, frameMatx.worldMatrix, frameMatx.cameraWorldView);
        Matrix4f.mul(frameMatx.lightProj, frameMatx.lightView, frameMatx.lightViewProj);
        Matrix4f.mul(frameMatx.lightViewProj, frameMatx.worldMatrix, frameMatx.lightWorldViewProj);
        Matrix4f.mul(frameMatx.lightViewProj, frameMatx.cameraViewInv, frameMatx.cameraViewToLightProj);
        Matrix4f.mul(frameMatx.lightView, frameMatx.cameraViewInv, frameMatx.cameraViewToLightView);

        frameMatx.avsmLightProj.load(lightCamera.GetProjectionMatrix());
        frameMatx.avsmLightView.load(lightCamera.GetViewMatrix() );

        if (options.enableAutoBoundsAVSM) {

            // doesn't work with CPUT shadows unfortunately!
            assert( false );

            // Get bounding boxes from transparent geometry
            final float bigNumf = 1e10f;
            Vector3f maxBB = new Vector3f(-bigNumf, -bigNumf, -bigNumf);
            Vector3f minBB = new Vector3f(+bigNumf, +bigNumf, +bigNumf);

            if (options.enableParticles) {
                // Initialize minBB, maxBB
                /*for (int p = 0; p < 3; ++p) {
                    minBB[p] = std::numeric_limits<float>::max();
                    maxBB[p] = std::numeric_limits<float>::min();
                }*/
                minBB.set(Float.MAX_VALUE,Float.MAX_VALUE,Float.MAX_VALUE);
                maxBB.set(-Float.MAX_VALUE,-Float.MAX_VALUE,-Float.MAX_VALUE);

                Vector3f particleMin = new Vector3f(), particleMax = new Vector3f();
                particleSystem.GetBBox(particleMin, particleMax);

                /*for (size_t p = 0; p < 3; ++p) {
                    minBB[p] = std::min(minBB[p], particleMin[p]);
                    maxBB[p] = std::max(maxBB[p], particleMax[p]);
                }*/

                Vector3f.min(minBB, particleMin, minBB);
                Vector3f.max(maxBB, particleMax, maxBB);

                Utils.TransformBBox(minBB, maxBB, frameMatx.avsmLightView);
            }

            // First adjust the light matrix to be centered on the extents in x/y and behind everything in z
            /*D3DXVECTOR3 center = 0.5f * (minBB + maxBB);
            D3DXMATRIXA16 centerTransform;
            D3DXMatrixTranslation(&centerTransform, -center.x, -center.y, -minBB.z);
            frameMatx.avsmLightView *= centerTransform;

            // Now create a projection matrix that covers the extents when centered
            // Optimization: Again use scene AABB to decide on light far range - this one can actually clip out
            // any objects further away than the frustum can see if desired.
            D3DXVECTOR3 dimensions = maxBB - minBB;
            D3DXMatrixOrthoLH(&frameMatx.avsmLightProj, dimensions.x, dimensions.y, 0, dimensions.z);*/
            Matrix4f.ortho(minBB.x, maxBB.x, minBB.y, maxBB.y, 0, maxBB.z - minBB.z, frameMatx.avsmLightProj); // TODO
        }

        // Compute composite matrices;
        /*frameMatx.avsmLightViewProj = frameMatx.avsmLightView * frameMatx.avsmLightProj;
        frameMatx.avmsLightWorldViewProj = frameMatx.worldMatrix * frameMatx.avsmLightViewProj;
        frameMatx.cameraViewToAvsmLightProj = frameMatx.cameraViewInv * frameMatx.avsmLightViewProj;
        frameMatx.cameraViewToAvsmLightView = frameMatx.cameraViewInv * frameMatx.avsmLightView;*/
        Matrix4f.mul(frameMatx.avsmLightProj, frameMatx.avsmLightView, frameMatx.avsmLightViewProj);
        Matrix4f.mul(frameMatx.avsmLightViewProj, frameMatx.worldMatrix, frameMatx.avmsLightWorldViewProj);
        Matrix4f.mul(frameMatx.avsmLightViewProj, frameMatx.cameraViewInv, frameMatx.cameraViewToAvsmLightProj);
        Matrix4f.mul(frameMatx.avsmLightView, frameMatx.cameraViewInv, frameMatx.cameraViewToAvsmLightView);

        // Just set up some common defaults
//        memset( &outContext.GPUUIConstants, 0, sizeof( outContext.GPUUIConstants ) );
        outContext.GPUUIConstants.lightingOnly              = 0;
        outContext.GPUUIConstants.faceNormals               = 0;
        outContext.GPUUIConstants.enableStats               = 0;
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

    static int parframecount = 0;
    private final ParticlePerFrameConstants m_particlePerFrameConstants = new ParticlePerFrameConstants();
    void UpdateParticles( FrameContext frameContext ){
        Options options                         = frameContext.Options;
        CPUTCamera viewerCamera                = frameContext.ViewerCamera;
        CPUTCamera lightCamera                 = frameContext.LightCamera;
        ParticleSystem   particleSystem         = frameContext.ParticleSystem;
//        ID3D11DeviceContext * d3dDeviceContext  = frameContext.D3dDeviceContext;
        FrameMatrices   frameMatx               = frameContext.Matrices;
        UIConstants   ui                        = frameContext.GPUUIConstants;

//        static unsigned int parframecount = 0;
        float[] particleDepthBounds = new float[2];
        if (options.enableParticles) {
            if (ui.pauseParticleAnimaton == 0) {
                // Update particles
                float deltaTime = 0.009f; //currTime - mLastTime;
                if (parframecount < 100) {
                    particleSystem.UpdateParticles(viewerCamera, lightCamera, deltaTime);
                }
            }

            particleSystem.SortParticles(particleDepthBounds, frameMatx.avsmLightView, false, 1, false);
            particleSystem.PopulateVertexBuffers(/*d3dDeviceContext*/);

            // Fill in particle emitter (per frame) constants
            {
                /*D3D11_MAPPED_SUBRESOURCE mappedResource;
                d3dDeviceContext->Map(mParticlePerFrameConstantsCPUT->GetNativeBuffer(), 0, D3D11_MAP_WRITE_DISCARD, 0, &mappedResource);
                ParticlePerFrameConstants* constants = static_cast<ParticlePerFrameConstants *>(mappedResource.pData);*/

                ParticlePerFrameConstants constants = m_particlePerFrameConstants;
                constants.mScale                        = 1.0f;
                constants.mParticleSize                 = (ui.particleSize / 3.0f);
                constants.mParticleAlpha                = 1.0f;
                constants.mbSoftParticles               = 1.0f;
                constants.mParticleOpacity              = 0.8f * ((float)ui.particleOpacity / 33.0f);
                constants.mSoftParticlesSaturationDepth = 1.0f;

//                d3dDeviceContext->Unmap(mParticlePerFrameConstantsCPUT->GetNativeBuffer(), 0);
                ByteBuffer buffer = CacheBuffer.getCachedByteBuffer(ParticlePerFrameConstants.SIZE);
                constants.store(buffer).flip();
                mParticlePerFrameConstantsCPUT.update(0, buffer);
            }
        }
    }

    void CreateAVSMShadowMap( FrameContext frameContext/*, CPUTAssetSet mpAssetSet, CPUTRenderParametersDX pRenderParams*/ ){
        Options  options                        = frameContext.Options;
//        ID3D11DeviceContext * d3dDeviceContext  = frameContext.D3dDeviceContext;
        ParticleSystem  particleSystem         = frameContext.ParticleSystem;
        CPUTCamera viewerCamera               = frameContext.ViewerCamera;
        CPUTCamera lightCamera                = frameContext.LightCamera;
        Vector4i viewport                      = frameContext.Viewport;
        UIConstants  ui                        = frameContext.GPUUIConstants;
        FrameStats  frameStats                 = frameContext.Stats;
        FrameMatrices  frameMatx               = frameContext.Matrices;
//        D3DXVECTOR4 cameraPos  = D3DXVECTOR4( DXUTFromCPUT( viewerCamera->GetPosition() ), 1.0f );
        Vector3f cameraPos             = new Vector3f();
        viewerCamera.GetPosition(cameraPos);

        // no lookup not supported at the moment
        assert( ui.enableVolumeShadowLookup!=0 );

        // set all the various camera, light, and per-frame constants in constant buffer
        FillInFrameConstants(/*d3dDeviceContext,*/ frameMatx, cameraPos, viewerCamera, lightCamera, frameContext.mScreenWidth, frameContext.mScreenHeight, ui);

        int savedVolMethod = ui.volumeShadowMethod;

        // This phase computes a visibility representation for our shadow volume
        if (ui.volumeShadowMethod !=0 && options.enableVolumeShadowCreation)
        {
            // Setup constants buffers
            FillListTextureConstants(/*d3dDeviceContext*/);
            FillAVSMConstants(/*d3dDeviceContext*/);

            ClearShadowBuffers(/*d3dDeviceContext,*/ ui);

            // First pass, capture all fragments
            if (options.enableParticles) {

                FillParticleRendererConstants(// d3dDeviceContext,
                        lightCamera,
                        frameMatx.avsmLightView,
                        frameMatx.avsmLightViewProj);

                CaptureFragments(/*d3dDeviceContext,*/ particleSystem, ui, true);
            }

            // Second pass, generate visibility curves (AVSM, uncompressed, deep shadow maps, etc..)
            GenerateVisibilityCurve(/*d3dDeviceContext,*/ ui);
        }

        // no lookup not supported at the moment
        assert( ui.enableVolumeShadowLookup!=0 );
    }

    void RenderShadedParticles( FrameContext  frameContext, Texture2D backBuffer, Texture2D depthBuffer ){
        Options  options                        = frameContext.Options;
//        ID3D11DeviceContext * d3dDeviceContext  = frameContext.D3dDeviceContext;
        ParticleSystem  particleSystem         = frameContext.ParticleSystem;
        CPUTCamera viewerCamera               = frameContext.ViewerCamera;
        CPUTCamera lightCamera                = frameContext.LightCamera;
        Vector4i    viewport                   = frameContext.Viewport;
        UIConstants  ui                        = frameContext.GPUUIConstants;
        FrameStats  frameStats                 = frameContext.Stats;
        FrameMatrices  frameMatx               = frameContext.Matrices;
//        D3DXVECTOR4 cameraPos  = D3DXVECTOR4( DXUTFromCPUT( viewerCamera->GetPosition() ), 1.0f );
        Vector3f cameraPos             = new Vector3f();
        viewerCamera.GetPosition(cameraPos);

        // lookup not supported
        assert( ui.enableVolumeShadowLookup !=0);

        if (options.enableParticles)
        {
            // Particle Alpha Pass
            // Update particles
            particleSystem.SortParticles(null, frameMatx.cameraView, true, 1, false);
            particleSystem.PopulateVertexBuffers(/*d3dDeviceContext*/);

            // Fill particle renderer constants and shade particles
            RenderShadedParticles(/*d3dDeviceContext,*/ particleSystem, backBuffer, depthBuffer, viewport, ui);
        }

        Cleanup( /*d3dDeviceContext*/ );
    }

    void UpdateDebugViewD3D11( FrameContext frameContext ){
        /*ID3D11ShaderResourceView* avsmGenCtrlSurface = mAVSMGenCtrlSurface->GetShaderResource();

        PSSetShaderResources( frameContext.D3dDeviceContext,
                mParticleShadingPerPixelPSReflector[mShaderIdx],
                "NONCPUT_gAVSMGenClearMaskSRV",
                1,
                &avsmGenCtrlSurface);*/
    }

    void UpdateStatesAndConstantsForAVSMUse( FrameContext  frameContext, boolean forCPUTGeometry ){
//        ID3D11DeviceContext* d3dDeviceContext   = frameContext.D3dDeviceContext;
        ParticleSystem  particleSystem         = frameContext.ParticleSystem;
        Vector4i viewport                      = frameContext.Viewport;
        UIConstants  ui                        = frameContext.GPUUIConstants;
        CPUTCamera viewerCamera                = frameContext.ViewerCamera;
        FrameMatrices  frameMatx               = frameContext.Matrices;

        FillParticleRendererConstants(/*d3dDeviceContext,*/ viewerCamera, frameMatx.cameraView, frameMatx.cameraViewProj);

//        ID3D11ShaderResourceView* asvmTextureSRV = mAVSMTextures->GetShaderResource();
//        ID3D11ShaderResourceView* avsmGenCtrlSurface = mAVSMGenCtrlSurface->GetShaderResource();

        if( !forCPUTGeometry )
        {
            /*PSSetShaderResources(d3dDeviceContext,  TODO
                    mParticleShadingPerPixelPSReflector[mShaderIdx],
                    "gDepthBuffer",
                    1,
                    &frameContext.DepthBufferSRV);*/
        }

        if(ui.vertexShaderShadowLookup!=0 && !forCPUTGeometry)
        {
            if(ui.tessellate!=0)
            {
                //set all hull shader constant buffers
                // current framework doesn't auto-bind GS/HS/DS buffers
                /*ID3D11Buffer* ParticlePerFrameConstants = mParticlePerFrameConstantsCPUT->GetNativeBuffer();
                ID3D11Buffer* PerFrameConstants = mPerFrameConstantsCPUT->GetNativeBuffer();
                HSSetConstantBuffers(d3dDeviceContext,
                        mParticleShadingHSReflector,
                        "PerFrameConstants",
                        1,
                        &PerFrameConstants); TODO

                HSSetConstantBuffers(d3dDeviceContext,
                        mParticleShadingHSReflector,
                        "ParticlePerFrameConstants",
                        1,
                        &ParticlePerFrameConstants);
                HSSetConstantBuffers(d3dDeviceContext,
                        mParticleShadingHSReflector,
                        "ParticlePerPassConstants",
                        1,
                        &mParticlePerPassConstants);

                // set all domain shader constant buffers
                DSSetConstantBuffers(d3dDeviceContext,
                        mParticleShadingDSReflector[mShaderIdx],
                        "ParticlePerFrameConstants",
                        1,
                        &ParticlePerFrameConstants);
                DSSetConstantBuffers(d3dDeviceContext,
                        mParticleShadingDSReflector[mShaderIdx],
                        "ParticlePerPassConstants",
                        1,
                        &mParticlePerPassConstants);

                DSSetConstantBuffers(d3dDeviceContext,
                        mParticleShadingDSReflector[mShaderIdx],
                        "PerFrameConstants",
                        1,
                        &PerFrameConstants);

                ID3D11Buffer* pmAVSMConstants = mAVSMConstantsCPUT->GetNativeBuffer();
                DSSetConstantBuffers(d3dDeviceContext,
                        mParticleShadingDSReflector[mShaderIdx],
                        "AVSMConstants",
                        1,
                        &pmAVSMConstants);

                DSSetShaderResources(d3dDeviceContext,
                        mParticleShadingDSReflector[mShaderIdx],
                        "NONCPUT_gAVSMTexture",
                        1,
                        &asvmTextureSRV);

                DSSetShaderResources(d3dDeviceContext,
                        mParticleShadingDSReflector[mShaderIdx],
                        "NONCPUT_gAVSMGenClearMaskSRV",
                        1,
                        &avsmGenCtrlSurface);

                DSSetShaderResources(d3dDeviceContext,
                        mParticleShadingDSReflector[mShaderIdx],
                        "NONCPUT_gAVSMGenDataSRV",
                        1,
                        &mAVSMGenDataSRV[mShaderIdx]);

                DSSetSamplers(d3dDeviceContext,
                        mParticleShadingDSReflector[mShaderIdx],
                        "gAVSMSampler",
                        1,
                        &mAVSMSampler);*/
            }
        }
        else
        {
            /*ID3D11Buffer* ParticlePerFrameConstants = mParticlePerFrameConstantsCPUT->GetNativeBuffer();
            VSSetConstantBuffers(d3dDeviceContext,  TODO
                    mParticleShadingVSReflector,
                    "ParticlePerPassConstants",
                    1,
                    &mParticlePerPassConstants);

            VSSetConstantBuffers(d3dDeviceContext,
                    mParticleShadingVSReflector,
                    "ParticlePerFrameConstants",
                    1,
                    &ParticlePerFrameConstants);*/
        }

        if( (ui.vertexShaderShadowLookup!=0 && !forCPUTGeometry) || (VOL_SHADOW_NO_SHADOW == ui.volumeShadowMethod))
        {
            // Buffers are auto-bound in this case for mParticleShadingPS:
            // mPerFrameConstants - PS
            // mParticlePerFrameConstants - PS
        }
        else
        {
            // Buffers are auto-bound in this case for mParticleShadingPerPixelPS[mShaderIdx]:
            // mPerFrameConstants - PS
            // mParticlePerFrameConstants - PS
            // mAVSMConstants - PS

            /*PSSetShaderResources(d3dDeviceContext,  TODO
                    mParticleShadingPerPixelPSReflector[mShaderIdx],
                    "NONCPUT_gAVSMTexture",
                    1,
                    &asvmTextureSRV);

            PSSetShaderResources(d3dDeviceContext,
                    mParticleShadingPerPixelPSReflector[mShaderIdx],
                    "NONCPUT_gAVSMGenClearMaskSRV",
                    1,
                    &avsmGenCtrlSurface);

            PSSetShaderResources(d3dDeviceContext,
                    mParticleShadingPerPixelPSReflector[mShaderIdx],
                    "NONCPUT_gAVSMGenDataSRV",
                    1,
                    &mAVSMGenDataSRV[mShaderIdx]);

            if( !forCPUTGeometry ) // no need to do this for CPUT geometry, we're setting samplers in CPUT code
            {
                PSSetSamplers(d3dDeviceContext,
                        mParticleShadingPerPixelPSReflector[mShaderIdx],
                        "gAVSMSampler",
                        1,
                        &mAVSMSampler);

                if( ui->tessellate )
                {
                    PSSetSamplers(d3dDeviceContext,
                            mParticleShadingPerPixelPSReflector[mShaderIdx],
                            "gAVSMGenCtrlSurfaceSampler",
                            1,
                            &mAVSMGenSampler);
                }
            }*/
        }
    }

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

        final String shaderPath = "Intel/AVSM/shaders/";
        final Macro nodeCount4 = new Macro("AVSM_NODE_COUNT", 4);
        final Macro nodeCount8 = new Macro("AVSM_NODE_COUNT", 8);
        final Macro MSAA = new Macro("SHADOWAA_SAMPLES", 4);

        // TODO
        try {
            mParticleAVSMCaptureProgram = GLSLProgram.createFromFiles(shaderPath + "DynamicParticlesShadingVS.vert", shaderPath + "ParticleAVSMCapturePS.frag");
            mParticleAVSMCaptureProgram.setName("ParticleAVSMCaptureProgram");

            mParticleAVSM_GenPS[0] = GLSLProgram.createFromFiles(shaderPath + "DynamicParticlesShadingVS.vert", shaderPath + "ParticleAVSM_Gen_PS.frag", nodeCount4);
            mParticleAVSM_GenPS[0].setName("ParticleAVSM_GenPS4");
            mParticleAVSM_GenPS[1] = GLSLProgram.createFromFiles(shaderPath + "DynamicParticlesShadingVS.vert", shaderPath + "ParticleAVSM_Gen_PS.frag", nodeCount8);
            mParticleAVSM_GenPS[1].setName("ParticleAVSM_GenPS8");

            mAVSMInsertionSortResolveProgram[0] = GLSLProgram.createFromFiles("shader_libs/PostProcessingDefaultScreenSpaceVS.vert", shaderPath + "AVSM_ResolvePS.frag", MSAA, nodeCount4);
            mAVSMInsertionSortResolveProgram[0].setName("AVSM_ResolvePS4");
            mAVSMInsertionSortResolveProgram[1] = GLSLProgram.createFromFiles("shader_libs/PostProcessingDefaultScreenSpaceVS.vert", shaderPath + "AVSM_ResolvePS.frag", MSAA, nodeCount8);
            mAVSMInsertionSortResolveProgram[1].setName("AVSM_ResolvePS8");

            mParticleShadingPS = GLSLProgram.createShaderProgramFromFile(shaderPath + "DynamicParticlesShading_PS.frag", ShaderType.FRAGMENT, nodeCount4);
            mParticleShadingPS.setName("ParticleShadingPS");
            mParticleShadingVS = GLSLProgram.createShaderProgramFromFile(shaderPath + "DynamicParticlesShadingVS.vert", ShaderType.VERTEX );
            mParticleShadingVS.setName("ParticleShadingVS");
            mParticleShadingPerPixelPS[0] = GLSLProgram.createShaderProgramFromFile(shaderPath + "DynamicParticlesPerPixelShading_PS.frag", ShaderType.FRAGMENT, nodeCount4);
            mParticleShadingPerPixelPS[0].setName("ParticleShadingPerPixelPS4");
            mParticleShadingPerPixelPS[1] = GLSLProgram.createShaderProgramFromFile(shaderPath + "DynamicParticlesPerPixelShading_PS.frag", ShaderType.FRAGMENT, nodeCount8);
            mParticleShadingPerPixelPS[1].setName("ParticleShadingPerPixelPS8");

            mParticleShadingFinalVS[0] = GLSLProgram.createShaderProgramFromFile(shaderPath + "DynamicParticlesShadingFinal_VS.vert", ShaderType.VERTEX, nodeCount4 );
            mParticleShadingFinalVS[0].setName("ParticleShadingFinalVS4");
            mParticleShadingFinalVS[1] = GLSLProgram.createShaderProgramFromFile(shaderPath + "DynamicParticlesShadingFinal_VS.vert", ShaderType.VERTEX, nodeCount8 );
            mParticleShadingFinalVS[1].setName("ParticleShadingFinalVS8");

            mParticleShadingTessellationVS = GLSLProgram.createShaderProgramFromFile(shaderPath + "DynamicParticlesShading_TessellationVS.vert", ShaderType.VERTEX );
            mParticleShadingTessellationVS.setName("ParticleShadingTessellationVS");

            mParticleShadingDS[0] = GLSLProgram.createShaderProgramFromFile(shaderPath + "DynamicParticlesShading_DS.glte", ShaderType.TESS_EVAL, nodeCount4);
            mParticleShadingDS[0].setName("ParticleShadingDS4");
            mParticleShadingDS[1] = GLSLProgram.createShaderProgramFromFile(shaderPath + "DynamicParticlesShading_DS.glte", ShaderType.TESS_EVAL, nodeCount8);
            mParticleShadingDS[1].setName("ParticleShadingDS8");

            mParticleShadingHS = GLSLProgram.createShaderProgramFromFile(shaderPath + "DynamicParticlesShading_HS.gltc", ShaderType.TESS_CONTROL);
        } catch (IOException e) {
            e.printStackTrace();
        }
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
//        d3dDeviceContext->HSSetShader( NULL, NULL, 0);
//        d3dDeviceContext->DSSetShader( NULL, NULL, 0);

        switch(ui.volumeShadowMethod) {
            case VOL_SHADOW_AVSM:
            {
                /*ID3D11Buffer* ParticlePerFrameConstants = mParticlePerFrameConstantsCPUT->GetNativeBuffer();
                VSSetConstantBuffers(d3dDeviceContext,
                        mParticleShadingVSReflector,
                        "ParticlePerFrameConstants",
                        1,
                        &ParticlePerFrameConstants);
                VSSetConstantBuffers(d3dDeviceContext,
                        mParticleShadingVSReflector,
                        "ParticlePerPassConstants",
                        1,
                        &mParticlePerPassConstants);
                d3dDeviceContext->VSSetShader(mParticleShadingVS, 0, 0);
                d3dDeviceContext->RSSetState(mParticleRasterizerState);
                d3dDeviceContext->RSSetViewports(1, &mAVSMShadowViewport);
                PSSetConstantBuffers(d3dDeviceContext,
                        mParticleAVSMCapturePSReflector,
                        "ParticlePerFrameConstants",
                        1,
                        &ParticlePerFrameConstants);
                PSSetConstantBuffers(d3dDeviceContext,
                        mParticleAVSMCapturePSReflector,
                        "LT_Constants",
                        1,
                        &mListTextureConstants);
                d3dDeviceContext->PSSetShader(mParticleAVSMCapturePS, 0, 0);
                ID3D11UnorderedAccessView* listTexFirstSegmentNodeOffsetUAV =
                        mListTexFirstSegmentNodeOffset->GetUnorderedAccess();*/
                mParticleRasterizerState.run();
                gl.glViewport(mAVSMShadowViewport.x, mAVSMShadowViewport.y, mAVSMShadowViewport.z, mAVSMShadowViewport.w);
                // TODO binding buffers
                mParticleAVSMCaptureProgram.enable();


                // Set List Texture UAVs (we don't need any RT!)
                /*static const char *paramUAVs[] = {
                    "gListTexFirstSegmentNodeAddressUAV",
                            "gListTexSegmentNodesUAV",
                };
                const UINT numUAVs = sizeof(paramUAVs) / sizeof(paramUAVs[0]);
                const UINT firstUAVIndex =
                    GetStartBindIndex(mParticleAVSMCapturePSReflector,
                            paramUAVs, numUAVs);
                UINT pUAVInitialCounts[numUAVs] = {0, 0};
                ID3D11UnorderedAccessView* pUAVs[numUAVs] = {
                        listTexFirstSegmentNodeOffsetUAV,
                        mListTexSegmentNodesUAV
                };
                d3dDeviceContext->OMSetRenderTargetsAndUnorderedAccessViews(
                        0, NULL, // render targets
                        NULL,    // depth-stencil
                        firstUAVIndex, numUAVs, pUAVs, initCounter ? pUAVInitialCounts : NULL);*/
                // TODO binding unorderedAceessViews
                // TODO There is no framebuffer binding...

                /*d3dDeviceContext->OMSetBlendState(mGeometryBlendState, 0, 0xFFFFFFFF);
                d3dDeviceContext->OMSetDepthStencilState(mAVSMCaptureDepthStencilState, 0x0);
                particleSystem->Draw(d3dDeviceContext, NULL, 0, particleSystem->GetParticleCount(),false);*/
                mGeometryBlendState.run();
                mAVSMCaptureDepthStencilState.run();
                // TODO There is no VAO binding...
                // TODO I don't kown the primity type.
                gl.glDrawArrays(GLenum.GL_POINTS, 0, particleSystem.GetParticleCount());

                // Cleanup (aka make the runtime happy)
                Cleanup(/*d3dDeviceContext*/);

                break;
            }

            case VOL_SHADOW_AVSM_GEN:
            {
                /*ID3D11Buffer* ParticlePerFrameConstants = mParticlePerFrameConstantsCPUT->GetNativeBuffer();
                VSSetConstantBuffers(d3dDeviceContext,
                        mParticleShadingVSReflector,
                        "ParticlePerFrameConstants",
                        1,
                        &ParticlePerFrameConstants);
                VSSetConstantBuffers(d3dDeviceContext,
                        mParticleShadingVSReflector,
                        "ParticlePerPassConstants",
                        1,
                        &mParticlePerPassConstants);
                d3dDeviceContext->VSSetShader(mParticleShadingVS, 0, 0);

                d3dDeviceContext->RSSetState(mParticleRasterizerState);
                d3dDeviceContext->RSSetViewports(1, &mAVSMShadowViewport);

                PSSetConstantBuffers(d3dDeviceContext,
                        mParticleAVSM_GenPSReflector[mShaderIdx],
                        "ParticlePerFrameConstants",
                        1,
                        &ParticlePerFrameConstants);
                d3dDeviceContext->PSSetShader(mParticleAVSM_GenPS[mShaderIdx], 0, 0);*/
                mParticleRasterizerState.run();
                mParticleAVSM_GenPS[mShaderIdx].enable();
                // TODO binding buffers
                gl.glViewport(mAVSMShadowViewport.x, mAVSMShadowViewport.y, mAVSMShadowViewport.z, mAVSMShadowViewport.w);

                /*static const char *paramUAVs[] = {
                    "gAVSMGenClearMaskUAV",
                            "gAVSMGenDataUAV",
                };
                const UINT numUAVs = sizeof(paramUAVs) / sizeof(paramUAVs[0]);
                const UINT firstUAVIndex =
                    GetStartBindIndex(mParticleAVSM_GenPSReflector[mShaderIdx],
                            paramUAVs, numUAVs);
                UINT pUAVInitialCounts[numUAVs] = {0, 0};
                ID3D11UnorderedAccessView* pUAVs[numUAVs] = {
                        mAVSMGenCtrlSurface->GetUnorderedAccess(),
                        mAVSMGenDataUAV[mShaderIdx],
                };

                d3dDeviceContext->OMSetRenderTargetsAndUnorderedAccessViews(
                        0, NULL, // render targets
                        NULL,    // depth-stencil
                        firstUAVIndex, numUAVs, pUAVs, initCounter ? pUAVInitialCounts : NULL);

                d3dDeviceContext->OMSetBlendState(mGeometryBlendState, 0, 0xFFFFFFFF);
                d3dDeviceContext->OMSetDepthStencilState(mAVSMCaptureDepthStencilState, 0x0);*/
                mGeometryBlendState.run();
                mAVSMCaptureDepthStencilState.run();
                // TODO binding unorderedAceessViews
                // TODO There is no framebuffer binding...

//                particleSystem->Draw(d3dDeviceContext, NULL, 0, particleSystem->GetParticleCount(),false);
                // TODO There is no VAO binding...
                // TODO I don't kown the primity type.
                gl.glDrawArrays(GLenum.GL_POINTS, 0, particleSystem.GetParticleCount());

                // Cleanup (aka make the runtime happy)
                Cleanup(/*d3dDeviceContext*/);
                break;
            }
        }
    }

    private void GenerateVisibilityCurve(//ID3D11DeviceContext* d3dDeviceContext,
                                 UIConstants ui){
//        ID3D11ShaderResourceView*  listTexFirstSegmentNodeOffsetSRV = mListTexFirstSegmentNodeOffset->GetShaderResource();

        if (VOL_SHADOW_AVSM == ui.volumeShadowMethod) {
            // Second (full screen) pass, sort fragments and insert them in our AVSM texture(s)
            /*d3dDeviceContext->IASetInputLayout(0);
            d3dDeviceContext->IASetPrimitiveTopology(D3D11_PRIMITIVE_TOPOLOGY_TRIANGLESTRIP);
            d3dDeviceContext->IASetVertexBuffers(0, 0, 0, 0, 0);
            d3dDeviceContext->VSSetShader(mFullScreenTriangleVS, 0, 0);
            d3dDeviceContext->RSSetState(mRasterizerState);
            d3dDeviceContext->RSSetViewports(1, &mAVSMShadowViewport);
            ID3D11ShaderReflection *shaderReflector = mAVSMInsertionSortResolvePSReflector[mShaderIdx];
            d3dDeviceContext->PSSetShader(mAVSMInsertionSortResolvePS[mShaderIdx], 0, 0);*/
            mRasterizerState.run();
            gl.glViewport(mAVSMShadowViewport.x, mAVSMShadowViewport.y, mAVSMShadowViewport.z,mAVSMShadowViewport.w);
            mAVSMInsertionSortResolveProgram[mShaderIdx].enable();
            gl.glBindVertexArray(0);

            // framework's autobind not in effect on UAV manipulation - manual bind cbuffer
            /*ID3D11Buffer* pAVSMConstants = mAVSMConstantsCPUT->GetNativeBuffer();
            PSSetConstantBuffers(d3dDeviceContext,
                    shaderReflector,
                    "AVSMConstants",
                    1,
                    &pAVSMConstants);

            PSSetShaderResources(d3dDeviceContext,
                    shaderReflector,
                    "gListTexSegmentNodesSRV",
                    1,
                    &mListTexSegmentNodesSRV);
            PSSetShaderResources(d3dDeviceContext,
                    shaderReflector,
                    "gListTexFirstSegmentNodeAddressSRV",
                    1,
                    &listTexFirstSegmentNodeOffsetSRV);

            ID3D11RenderTargetView* pRTs[16];
            const int avsmRTCount = mAVSMNodeCount / 2;
            for (int i = 0; i < avsmRTCount; ++i) {
                pRTs[i] = mAVSMTextures->GetRenderTarget(i);
            }
            d3dDeviceContext->OMSetRenderTargets(avsmRTCount, pRTs, 0);
            d3dDeviceContext->OMSetBlendState(mGeometryBlendState, 0, 0xFFFFFFFF);
            d3dDeviceContext->OMSetDepthStencilState(mDefaultDepthStencilState, 0x0);
            // Full-screen triangle
            d3dDeviceContext->Draw(3, 0);*/
            // TODO textures, buffers, and images.
            mRenderTargets.bind();
            final int avsmRTCount = mAVSMNodeCount / 2;
//            TextureGL[] pRTs = new TextureGL[avsmRTCount];
            for (int i = 0; i < avsmRTCount; ++i) {
//                pRTs[i] = .GetRenderTarget(i);
            }
            mRenderTargets.setRenderTexture(mAVSMTextures, null); // TODO

            // Cleanup (aka make the runtime happy)
            Cleanup(/*d3dDeviceContext*/);
        }
    }


    // Render the shadowed particles
    // This function, based on the settings of the technique, sets up the shaders
    // and buffers and draws the shadowed particles
    //-----------------------------------------------------------------------------
    private void RenderShadedParticles(//ID3D11DeviceContext* d3dDeviceContext,
                               ParticleSystem particleSystem,
                               Texture2D backBuffer,
                               Texture2D depthBuffer,
                               Vector4i viewport,
                        UIConstants ui){
//        ID3D11ShaderResourceView* asvmTextureSRV = mAVSMTextures->GetShaderResource();
//        ID3D11ShaderResourceView* avsmGenCtrlSurface = mAVSMGenCtrlSurface->GetShaderResource();

        gl.glUseProgram(0);
        mProgramPipeline.enable();

        if(ui.vertexShaderShadowLookup != 0)
        {
            if(ui.tessellate != 0)
            {
                // Gen+DX version with tesselation
                /*d3dDeviceContext->VSSetShader(mParticleShadingTessellationVS, 0, 0);
                d3dDeviceContext->HSSetShader( mParticleShadingHS, NULL, 0);
                d3dDeviceContext->DSSetShader( mParticleShadingDS[mShaderIdx], NULL, 0);*/
                mProgramPipeline.setVS(mParticleShadingTessellationVS);
                mProgramPipeline.setTC(mParticleShadingHS);
                mProgramPipeline.setTE(mParticleShadingDS[mShaderIdx]);

                /*VSSetConstantBuffers(d3dDeviceContext,  TODO
                        mParticleShadingVSReflector,
                        "ParticlePerFrameConstants",
                        1,
                        &mParticlePerFrameConstants);*/
            }
            else
            {
                // this version is NEVER hit by any condition
                /*d3dDeviceContext->VSSetShader(mParticleShadingFinalVS[mShaderIdx], 0, 0);
                d3dDeviceContext->HSSetShader( NULL, NULL, 0);
                d3dDeviceContext->DSSetShader( NULL, NULL, 0);*/

                mProgramPipeline.setVS(mParticleShadingFinalVS[mShaderIdx]);
                mProgramPipeline.setTC(null);
                mProgramPipeline.setTE(null);
            }
        }
        else
        {
            // GEN version with no tesselation
            /*d3dDeviceContext->VSSetShader(mParticleShadingVS, 0, 0);
            d3dDeviceContext->HSSetShader( NULL, NULL, 0);
            d3dDeviceContext->DSSetShader( NULL, NULL, 0);*/
            mProgramPipeline.setVS(mParticleShadingVS);
            mProgramPipeline.setTC(null);
            mProgramPipeline.setTE(null);

            /*VSSetConstantBuffers(d3dDeviceContext,  TODO
                    mParticleShadingVSReflector,
                    "ParticlePerFrameConstants",
                    1,
                    &mParticlePerFrameConstants);*/
        }

        if(ui.wireframe != 0)
//            d3dDeviceContext->RSSetState(mParticleWireFrameState);
            mParticleWireFrameState.run();
        else
//            d3dDeviceContext->RSSetState(mParticleRasterizerState);
            mParticleRasterizerState.run();

//        d3dDeviceContext->RSSetViewports(1, viewport);
        gl.glViewport(viewport.x, viewport.y, viewport.z, viewport.w);

        if(ui.vertexShaderShadowLookup != 0 || (VOL_SHADOW_NO_SHADOW == ui.volumeShadowMethod))
        {
            // Gen+DX version with tesselation
            // This path handles the cases when we are using particle tesselation
            // manually update the particle constant buffers we need since we're doing the drawing ourselves
            // and not using the framework's auto-bind features
//            ID3D11Buffer* PerFrameConstants = mPerFrameConstantsCPUT->GetNativeBuffer();
//            d3dDeviceContext->PSSetShader(mParticleShadingPS, 0, 0);
            mProgramPipeline.setPS(mParticleShadingPS);

            /*PSSetConstantBuffers(d3dDeviceContext,  TODO
                    mParticleShadingPSReflector,
                    "PerFrameConstants",
                    1,
                    &PerFrameConstants);
            ID3D11Buffer* ParticlePerFrameConstants = mParticlePerFrameConstantsCPUT->GetNativeBuffer();
            PSSetConstantBuffers(d3dDeviceContext,
                    mParticleShadingPSReflector,
                    "ParticlePerFrameConstants",
                    1,
                    &ParticlePerFrameConstants);*/
        }
        else
        {
            // GEN version with no tesselation

            // This path handles the cases when we are not using the particle tesselation features
            // manually update the particle constant buffers we need since we're doing the drawing ourselves
            // and not using the framework's auto-bind features
            /*ID3D11Buffer* PerFrameConstants = mPerFrameConstantsCPUT->GetNativeBuffer();
            PSSetConstantBuffers(d3dDeviceContext,  TODO
                    mParticleShadingPerPixelPSReflector[mShaderIdx],
                    "PerFrameConstants",
                    1,
                    &PerFrameConstants);

            ID3D11Buffer* ParticlePerFrameConstants = mParticlePerFrameConstantsCPUT->GetNativeBuffer();
            PSSetConstantBuffers(d3dDeviceContext,
                    mParticleShadingPerPixelPSReflector[mShaderIdx],
                    "ParticlePerFrameConstants",
                    1,
                    &ParticlePerFrameConstants);*/

//            d3dDeviceContext->PSSetShader(mParticleShadingPerPixelPS[mShaderIdx], 0, 0);
            mProgramPipeline.setPS(mParticleShadingPerPixelPS[mShaderIdx]);

            /*PSSetShaderResources(d3dDeviceContext,  TODO
                    mParticleShadingPerPixelPSReflector[mShaderIdx],
                    "NONCPUT_gAVSMTexture",
                    1,
                    &asvmTextureSRV);

            PSSetShaderResources(d3dDeviceContext,
                    mParticleShadingPerPixelPSReflector[mShaderIdx],
                    "NONCPUT_gAVSMGenClearMaskSRV",
                    1,
                    &avsmGenCtrlSurface);

            PSSetShaderResources(d3dDeviceContext,
                    mParticleShadingPerPixelPSReflector[mShaderIdx],
                    "NONCPUT_gAVSMGenDataSRV",
                    1,
                    &mAVSMGenDataSRV[mShaderIdx]);*/
        }


        // Additively blend into back buffer
        /*d3dDeviceContext->OMSetRenderTargets(1, &backBuffer, NULL); //depthBuffer);
        d3dDeviceContext->OMSetBlendState(mParticleBlendState, 0, 0xFFFFFFFF);
        d3dDeviceContext->OMSetDepthStencilState(mParticleDepthStencilState, 0x0);
        particleSystem->Draw( d3dDeviceContext, NULL, 0, particleSystem->GetParticleCount(), ui->tessellate != 0 );*/
        mRenderTargets.bind();
        mRenderTargets.setRenderTexture(backBuffer, null);
        mParticleBlendState.run();
        mParticleDepthStencilState.run();
        gl.glDrawArrays(GLenum.GL_TRIANGLES, 0, particleSystem.GetParticleCount());  // TODO
    }

    private final PerFrameConstants m_perFrameConstants = new PerFrameConstants();

    private void FillInFrameConstants(//ID3D11DeviceContext* d3dDeviceContext,
                              FrameMatrices m,
                              ReadableVector3f cameraPos,
                              CPUTCamera viewerCamera,
                              CPUTCamera lightCamera,
                              float ScreenWidth, float ScreenHeight,
                              UIConstants ui){
        // Compute light direction in view space
        Vector3f lightPosWorld = new Vector3f();
        lightCamera.GetPosition(lightPosWorld);
        /*ReadableVector3f lightTargetWorld = DXUTFromCPUT( lightCamera->GetPosition() + lightCamera->GetLook() );
        Vector3f lightPosView;
        D3DXVec3TransformCoord(&lightPosView, &lightPosWorld, &m.cameraView);
        D3DXVECTOR3 lightTargetView;
        D3DXVec3TransformCoord(&lightTargetView, &lightTargetWorld, &m.cameraView);
        D3DXVECTOR3 lightDirView = lightTargetView - lightPosView;
        D3DXVec3Normalize(&lightDirView, &lightDirView);*/
        Vector3f lightPosView = new Vector3f();
        Matrix4f.transformVector(m.cameraView, lightPosWorld, lightPosView);

        Vector3f lightTargetView  = new Vector3f();
        Vector3f lightPositon  = new Vector3f();
        Vector3f lightLookAt  = new Vector3f();

        lightCamera.GetPosition(lightPositon);
        lightCamera.GetLook(lightLookAt);
        Vector3f.add(lightPositon, lightLookAt, lightTargetView);
        Matrix4f.transformVector(m.cameraView, lightTargetView, lightTargetView);

        /*D3D11_MAPPED_SUBRESOURCE mappedResource;
        d3dDeviceContext->Map(mPerFrameConstantsCPUT->GetNativeBuffer(), 0, D3D11_MAP_WRITE_DISCARD, 0, &mappedResource);
        PerFrameConstants* constants = static_cast<PerFrameConstants *>(mappedResource.pData);*/
        PerFrameConstants constants = m_perFrameConstants;

        // No world matrix for now...
        constants.mCameraWorldViewProj.load(m.cameraWorldViewProj);
//        constants.mCameraWorldView = m.worldMatrix * m.cameraView;
        Matrix4f.mul(m.cameraView, m.worldMatrix, constants.mCameraWorldView);
        constants.mCameraViewProj.load(m.cameraViewProj);
        constants.mCameraProj.load(m.cameraProj);
        constants.mCameraPos.set(cameraPos);
        constants.mLightWorldViewProj.load(m.lightWorldViewProj);
        constants.mAvsmLightWorldViewProj.load(m.avmsLightWorldViewProj);
        constants.mCameraViewToLightProj.load(m.cameraViewToLightProj);
        constants.mCameraViewToLightView.load(m.cameraViewToLightView);
        constants.mCameraViewToAvsmLightProj.load(m.cameraViewToAvsmLightProj);
        constants.mCameraViewToAvsmLightView.load(m.cameraViewToAvsmLightView);
//        constants.mLightDir = D3DXVECTOR4(lightDirView, 0.0f);
        Vector3f.sub(lightTargetView, lightPosView, constants.mLightDir);
        constants.mLightDir.w = 0;
        constants.mLightDir.normalise();
        constants.mScreenResolution.x = ScreenWidth;
        constants.mScreenResolution.y = ScreenHeight;

        float clipNear = viewerCamera.GetNearPlaneDistance();
        float clipFar  = viewerCamera.GetFarPlaneDistance();
        float depthHackMul = ( clipFar * clipNear) / ( clipFar - clipNear );
        float depthHackAdd = clipFar / ( clipFar - clipNear );

        constants.mScreenToViewConsts.x = depthHackMul;
        constants.mScreenToViewConsts.y = depthHackAdd;
        constants.mScreenToViewConsts.z = 0.0f;
        constants.mScreenToViewConsts.w = 0.0f;

        constants.mUI = ui;  // TODO reference copy

//        d3dDeviceContext->Unmap(mPerFrameConstantsCPUT->GetNativeBuffer(), 0);
        ByteBuffer buffer = CacheBuffer.getCachedByteBuffer(PerFrameConstants.SIZE);
        constants.store(buffer).flip();
        mPerFrameConstantsCPUT.update(0, buffer);
    }

    private final ParticlePerPassConstants m_particlePerPassConstants = new ParticlePerPassConstants();

    private void FillParticleRendererConstants(//ID3D11DeviceContext* d3dDeviceContext,
                                               CPUTCamera lightCamera,
                                       Matrix4f cameraView,
                                       Matrix4f cameraViewProj){
        // Particle renderer constants
        /*D3D11_MAPPED_SUBRESOURCE mappedResource;
        d3dDeviceContext->Map(mParticlePerPassConstants, 0, D3D11_MAP_WRITE_DISCARD, 0, &mappedResource);
        ParticlePerPassConstants* constants = static_cast<ParticlePerPassConstants *>(mappedResource.pData);
        D3DXMATRIXA16 lightProj     = DXUTFromCPUT( *camera->GetProjectionMatrix() );
        D3DXMATRIXA16 lightView     = DXUTFromCPUT( *camera->GetViewMatrix() );
        D3DXMATRIXA16 lightWorld    = DXUTFromCPUT( *camera->GetWorldMatrix() );*/

        ParticlePerPassConstants constants = m_particlePerPassConstants;
        constants.mParticleWorldViewProj.load(cameraViewProj);
        constants.mParticleWorldView.load(cameraView);
//        constants->mEyeRight              = *(( D3DXVECTOR3* )&lightWorld._11); // *camera->GetWorldRight();
//        constants->mEyeUp                 = *(( D3DXVECTOR3* )&lightWorld._21); // *camera->GetWorldUp();
        Matrix4f.decompseRigidMatrix(lightCamera.GetWorldMatrix(), null, constants.mEyeRight, constants.mEyeUp, null);
//        d3dDeviceContext->Unmap(mParticlePerPassConstants, 0);

        ByteBuffer buffer = CacheBuffer.getCachedByteBuffer(ParticlePerPassConstants.SIZE);
        constants.store(buffer).flip();
        mParticlePerPassConstants.update(0, buffer);
    }

    private final LT_Constants m_lt_constants = new LT_Constants();

    private void FillListTextureConstants(/*ID3D11DeviceContext* d3dDeviceContext*/){
        // List texture related constants
        /*D3D11_MAPPED_SUBRESOURCE mappedResource;
        d3dDeviceContext->Map(mListTextureConstants, 0, D3D11_MAP_WRITE_DISCARD, 0, &mappedResource);
        LT_Constants* constants = static_cast<LT_Constants*>(mappedResource.pData);*/
        LT_Constants constants = m_lt_constants;

        constants.mMaxNodes = mLisTexNodeCount;
        constants.mFirstNodeMapSize = (float)mAVSMShadowTextureDim;

//        d3dDeviceContext->Unmap(mListTextureConstants, 0);
        ByteBuffer buffer = CacheBuffer.getCachedByteBuffer(LT_Constants.SIZE);
        constants.store(buffer).flip();
        mListTextureConstants.update(0, buffer);
    }

    private final AVSMConstants m_avsmConstants = new AVSMConstants();
    private void FillAVSMConstants(/*ID3D11DeviceContext* d3dDeviceContext*/){
        // AVSM related constants
        /*D3D11_MAPPED_SUBRESOURCE mappedResource;
        d3dDeviceContext->Map(mAVSMConstantsCPUT->GetNativeBuffer(), 0, D3D11_MAP_WRITE_DISCARD, 0, &mappedResource);
        AVSMConstants* constants = static_cast<AVSMConstants*>(mappedResource.pData);*/
        AVSMConstants constants = m_avsmConstants;
        constants.mMask0.set( 0.0f,  1.0f,  2.0f,  3.0f);
        constants.mMask1.set( 4.0f,  5.0f,  6.0f,  7.0f);
        constants.mMask2.set( 8.0f,  9.0f, 10.0f, 11.0f);
        constants.mMask3.set(12.0f, 13.0f, 14.0f, 15.0f);
        constants.mMask4.set(16.0f, 17.0f, 18.0f, 19.0f);
        constants.mEmptyNode = EMPTY_NODE;
        constants.mOpaqueNodeTrans = 1E-4f;
        constants.mShadowMapSize = (float)mAVSMShadowTextureDim;

//        d3dDeviceContext->Unmap(mAVSMConstantsCPUT->GetNativeBuffer(), 0);
        ByteBuffer buffer = CacheBuffer.getCachedByteBuffer(AVSMConstants.SIZE);
        constants.store(buffer).flip();
        mAVSMConstantsCPUT.update(0, buffer);
    }

    private void ClearShadowBuffers(/*ID3D11DeviceContext* d3dDeviceContext,*/UIConstants ui){
        switch(ui.volumeShadowMethod) {
            case VOL_SHADOW_NO_SHADOW:
            case VOL_SHADOW_AVSM:
            {
                /*ID3D11UnorderedAccessView* listTexFirstSegmentNodeOffsetUAV =
                        mListTexFirstSegmentNodeOffset->GetUnorderedAccess();

                // Initialize the first node offset RW UAV with a NULL offset (end of the list)
                UINT clearValues[4] = { 0xFFFFFFFFUL, 0xFFFFFFFFUL, 0xFFFFFFFFUL, 0xFFFFFFFFUL };
                d3dDeviceContext->ClearUnorderedAccessViewUint(listTexFirstSegmentNodeOffsetUAV, clearValues);*/
                mRenderTargets.bind();
                mRenderTargets.setRenderTexture(mListTexFirstSegmentNodeOffset, null);
                gl.glClearBufferiv(GLenum.GL_COLOR, 0, CacheBuffer.wrap(0xFFFFFFFF, 0xFFFFFFFF,0xFFFFFFFF,0xFFFFFFFF));
                mRenderTargets.unbind();
                break;
            }
            case VOL_SHADOW_AVSM_GEN: {
                // Clear AVSM Gen control surface
//                float clearValues[4] = {0.0f, 0.0f, 0.0f, 0.0f};
//                d3dDeviceContext->ClearUnorderedAccessViewFloat(mAVSMGenCtrlSurface->GetUnorderedAccess(), clearValues);
                gl.glClearTexImage(mAVSMGenCtrlSurface.getTexture(), 0, TextureUtils.measureFormat(mAVSMGenCtrlSurface.getFormat()),
                        TextureUtils.measureDataType(mAVSMGenCtrlSurface.getFormat()), null);
                break;
            }
            default:
                break;
        }
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

        void set(Options ohs){
            enableParticles = ohs.enableParticles;
            enableAutoBoundsAVSM = ohs.enableAutoBoundsAVSM;
            enableShadowPicking = ohs.enableShadowPicking;
            NodeCount = ohs.NodeCount;
            enableTransmittanceCurve = ohs.enableTransmittanceCurve;
            enableVolumeShadowCreation = ohs.enableVolumeShadowCreation;
            pickedX = ohs.pickedX;
            pickedY = ohs.pickedY;
        }
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
        CPUTCamera ViewerCamera;
        CPUTCamera LightCamera;
        final Vector4i Viewport = new Vector4i();
        final UIConstants                 GPUUIConstants = new UIConstants();
        float					    mScreenWidth;
        float					    mScreenHeight;
        final FrameStats                  Stats = new FrameStats();
        Texture2D DepthBufferSRV;
    }
}
