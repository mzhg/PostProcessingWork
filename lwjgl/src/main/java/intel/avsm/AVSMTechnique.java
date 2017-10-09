package intel.avsm;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4i;

import java.util.ArrayList;
import java.util.List;

import jet.opengl.postprocessing.buffer.BufferGL;
import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.texture.Texture2D;

/**
 * Created by mazhen'gui on 2017/10/9.
 */

final class AVSMTechnique {
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
    private final List<Texture2D> mAVSMTextures = new ArrayList<>();
    private Texture2D                mAVSMTexturesCPUT;
    private Texture2D                 mAVSMTexturesDebug;
    private BufferGL                   mAVSMStructBuf;
    private BufferGL      mAVSMStructBufUAV;
    private BufferGL       mAVSMStructBufSRV;

    // AVSM Gen
//    std::tr1::shared_ptr<Texture2D> mAVSMGenCtrlSurface;
    private final List<Texture2D> mAVSMGenCtrlSurface = new ArrayList<>();
    private Texture2D               mAVSMGenCtrlSurfaceCPUT;
    private BufferGL[]              mAVSMGenData = new BufferGL[MAX_SHADER_VARIATIONS];
    private BufferGL[]      mAVSMGenDataUAV = new BufferGL[MAX_SHADER_VARIATIONS];
    private BufferGL[]       mAVSMGenDataSRV = new BufferGL[MAX_SHADER_VARIATIONS];
    private BufferGL[]                 mAVSMGenDataSRVCPUT = new BufferGL[MAX_SHADER_VARIATIONS];
    private int				mAVSMGenSampler;

    // List texture
//    std::tr1::shared_ptr<Texture2D> mListTexFirstSegmentNodeOffset;
//    std::tr1::shared_ptr<Texture2D> mListTexFirstVisibilityNodeOffset;
    private final List<Texture2D> mListTexFirstSegmentNodeOffset = new ArrayList<>();
    private final List<Texture2D> mListTexFirstVisibilityNodeOffset = new ArrayList<>();
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

    int mShadowTextureDim;
    int mAVSMShadowTextureDim;

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
