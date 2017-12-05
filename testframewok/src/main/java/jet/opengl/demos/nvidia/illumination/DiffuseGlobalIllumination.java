package jet.opengl.demos.nvidia.illumination;

import com.nvidia.developer.opengl.app.NvSampleApp;
import com.nvidia.developer.opengl.models.sdkmesh.SDKmesh;

import org.lwjgl.util.vector.Matrix;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.ReadableVector3f;
import org.lwjgl.util.vector.Vector;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;
import org.lwjgl.util.vector.Vector4i;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import jet.opengl.demos.intel.cput.ID3D11InputLayout;
import jet.opengl.demos.scene.CameraData;
import jet.opengl.postprocessing.buffer.BufferGL;
import jet.opengl.postprocessing.common.GLFuncProvider;
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
import jet.opengl.postprocessing.texture.TextureGL;
import jet.opengl.postprocessing.texture.TextureUtils;
import jet.opengl.postprocessing.util.CacheBuffer;
import jet.opengl.postprocessing.util.CommonUtil;
import jet.opengl.postprocessing.util.Numeric;

import static javafx.scene.input.KeyCode.L;

/**
 * Created by Administrator on 2017/11/22 0022.
 */

public class DiffuseGlobalIllumination extends NvSampleApp {
    private final CameraData g_Camera = new CameraData();
    private final CameraData g_LightCamera = new CameraData();

    //scene parameters:
    int                         g_subsetToRender = -1;
    int                         g_numHierarchyLevels = 2; //number of levels in the hierarchy
    float                       g_LPVscale;
    final Vector3f              g_lightPos = new Vector3f();
    float                       g_lightRadius;
    final Vector3f            g_mUp = new Vector3f(0,1,0);

    enum PROP_TYPE
    {
        CASCADE,
        HIERARCHY,
    };
    PROP_TYPE g_propType;

    boolean g_useRSMCascade;
    boolean g_bUseSingleLPV;

    int g_PropLevel;
    int g_LPVLevelToInitialize = Defines.HIERARCHICAL_INIT_LEVEL;
    boolean bPropTypeChanged = false;

    float g_cascadeScale = 1.75f;
    final Vector3f g_cascadeTranslate = new Vector3f(0,0,0);
    boolean g_movableLPV;

    boolean g_depthPeelFromCamera = true;
    boolean g_depthPeelFromLight = false;
    int g_numDepthPeelingPasses;
    boolean g_useDirectionalLight = true;

    final Vector3f g_center = new Vector3f(0.0f,0.0f,-10.0f);
    boolean g_bVisualizeSM;
    boolean g_showUI = true;
    boolean g_printHelp = false;
    boolean g_bVisualizeLPV3D;
    boolean g_bUseSM;
    boolean g_bVizLPVBB;
    boolean g_renderMesh;
    float g_depthBiasFromGUI;
    int g_smTaps;
    boolean g_numTapsChanged=false;
    float g_smFilterSize = 0.8f;

    boolean g_useTextureForFinalRender;
    boolean g_useTextureForRSMs;

    float g_ambientLight =0.0f;
    float g_directLight;
    float g_normalMapMultiplier = 1.0f;
    boolean g_useDirectionalDerivativeClamping;
    float g_directionalDampingAmount;

    float g_fCameraFovy;
    float g_cameraNear;
    float g_cameraFar;

    float g_fLightFov;
    float g_lightNear;
    float g_lightFar;

    float g_diffuseInterreflectionScale;
    int g_numPropagationStepsLPV;
    boolean g_useDiffuseInterreflection;
    float g_directLightStrength;
    Grid g_grid;
    final Vector3f g_objectTranslate = new Vector3f(0,0,0);
    boolean g_showMovableMesh;
    boolean g_useRSMForLight = true;
    boolean g_useFinestGrid = true;
    boolean g_usePSPropagation = false;


    final Vector3f    g_camViewVector =new Vector3f();
    final Vector3f    g_camTranslate = new Vector3f();
    boolean g_resetLPVXform = true;

    final Vector3f g_vecEye = new Vector3f();
    final Vector3f g_vecAt = new Vector3f();

    //variables for animating light
    int g_lightPreset = 0;
    ReadableVector3f g_lightPosDest = Vector3f.ZERO;
    boolean g_animateLight = false;

    enum MOVABLE_MESH_TYPE
    {
        TYPE_BOX /*= 0*/,
        TYPE_SPHERE /*= 1*/,
    };
    MOVABLE_MESH_TYPE movableMeshType = MOVABLE_MESH_TYPE.TYPE_SPHERE;
    final String g_tstr_movableMeshType[] =
    {
            "Wall","Sphere"
    };

//LPVs used in the Hierarchical path
    RTCollection_RGB LPV0Propagate;
    RTCollection_RGB LPV0Accumulate;
    RTCollection GV0; //the geometry volume encoding all the occluders
    RTCollection GV0Color; //the color of the occluders in GV0

//Reflective shadow maps
    SimpleRT g_pRSMColorRT;
    SimpleRT g_pRSMAlbedoRT;
    SimpleRT g_pRSMNormalRT;
    DepthRT g_pShadowMapDS;
    DepthRT[] g_pDepthPeelingDS = new DepthRT[2];

    DepthRT g_pSceneShadowMap;

    Runnable g_pNoBlendBS;
    Runnable g_pAlphaBlendBS;
    Runnable g_pNoBlend1AddBlend2BS;
    Runnable g_pMaxBlendBS;
    Runnable g_normalDepthStencilState;
    Runnable depthStencilStateComparisonLess;
    Runnable g_depthStencilStateDisableDepth;

    final Matrix4f g_pSceneShadowMapProj = new Matrix4f();
    final Matrix4f g_pShadowMapProjMatrixSingle = new Matrix4f();
    static final int  SM_PROJ_MATS_SIZE = 2;
    final Matrix4f[] g_pRSMProjMatrices =  new Matrix4f[SM_PROJ_MATS_SIZE];

    //viewports
    final Vector4i g_shadowViewport = new Vector4i();
    final Vector4i g_shadowViewportScene = new Vector4i();
    final Vector4i g_LPVViewport3D = new Vector4i();

//rasterizer states
    Runnable       g_pRasterizerStateMainRender;
    Runnable       pRasterizerStateCullNone;
    Runnable       pRasterizerStateCullFront;
    Runnable       pRasterizerStateWireFrame;


//------------------------------------------------------
//variables needed for core RSM and LPV functions
//buffers:
    BufferGL g_pcbVSPerObject = null;
    BufferGL                g_pcbVSGlobal = null;
    BufferGL                g_pcbLPVinitVS = null;
    BufferGL                g_pcbLPVinitVS2 = null;
    BufferGL                g_pcbLPVinitialize_LPVDims = null;
    BufferGL                g_pcbLPVpropagate=null;
    BufferGL                g_pcbRender = null;
    BufferGL                g_pcbRenderLPV = null;
    BufferGL                g_pcbRenderLPV2 = null;
    BufferGL                g_pcbLPVpropagateGather = null;
    BufferGL                g_pcbLPVpropagateGather2 = null;
    BufferGL                g_pcbGV = null;
    BufferGL                g_pcbSimple = null;
    BufferGL                g_pcbLPVViz = null;
    BufferGL                g_pcbMeshRenderOptions = null;
    BufferGL                g_pcbSlices3D = null;
    BufferGL                g_reconPos = null;
    BufferGL                g_pcbPSSMTapLocations = null;
    BufferGL                g_pcbInvProjMatrix = null;


//shaders:
    /*ID3D11ComputeShader**/ShaderProgram         g_pCSAccumulateLPV = null;
    /*ID3D11ComputeShader**/ShaderProgram         g_pCSAccumulateLPV_singleFloats_8 = null;
    /*ID3D11ComputeShader**/ShaderProgram         g_pCSAccumulateLPV_singleFloats_4 = null;
    /*ID3D11ComputeShader**/ShaderProgram         g_pCSPropagateLPV = null;
    /*ID3D11ComputeShader**/ShaderProgram         g_pCSPropagateLPVSimple = null;
    /*ID3D11VertexShader**/ShaderProgram          g_pVSPropagateLPV = null;
    /*ID3D11GeometryShader**/ShaderProgram        g_pGSPropagateLPV = null;
    /*ID3D11PixelShader**/ShaderProgram           g_pPSPropagateLPV = null;
    /*ID3D11PixelShader**/ShaderProgram           g_pPSPropagateLPVSimple = null;
    /*ID3D11VertexShader**/ShaderProgram          g_pVSInitializeLPV = null;
    /*ID3D11VertexShader**/ShaderProgram          g_pVSInitializeLPV_Bilinear = null;
    /*ID3D11GeometryShader**/ShaderProgram        g_pGSInitializeLPV = null;
    /*ID3D11GeometryShader**/ShaderProgram        g_pGSInitializeLPV_Bilinear = null;
    /*ID3D11PixelShader**/ShaderProgram           g_pPSInitializeLPV = null;
    /*ID3D11PixelShader**/ShaderProgram           g_pPSInitializeGV = null;
    /*ID3D11VertexShader**/ShaderProgram          g_pVSRSM = null;
    /*ID3D11PixelShader**/ShaderProgram           g_pPSRSM = null;
    /*ID3D11VertexShader**/ShaderProgram          g_pVSSM = null;
    /*ID3D11VertexShader**/ShaderProgram          g_pVSRSMDepthPeeling = null;
    /*ID3D11PixelShader**/ShaderProgram           g_pPSRSMDepthPeel = null;

//misc:
    ID3D11InputLayout            g_pMeshLayout = null;
    final Vector4i               g_LPVViewport = new Vector4i();
    float                        g_VPLDisplacement;
    int                          g_pDepthPeelingTexSampler;
    int                          g_pLinearSampler;
    int                          g_pAnisoSampler;
    boolean                      g_useOcclusion;
    boolean                      g_useMultipleBounces;
    float                        g_fluxAmplifier;
    float                        g_reflectedLightAmplifier;
    float                        g_occlusionAmplifier;
    boolean                      g_useBilinearInit; //only have bilinear init enabled for initializing LPV, not GV
    boolean                      g_bilinearInitGVenabled = false;
    boolean                      g_bilinearWasEnabled = false;

    final String[] g_tstr_vizOptionLabels =
    {
        "Color RSM", "Normal RSM",
                "Albedo RSM",
                "Red LPV",
                "Green Accum LPV",
                "Blue Accum LPV",
                "GV",
                "GV Color",
    };

    enum VIZ_OPTIONS
    {
        COLOR_RSM ,
        NORMAL_RSM,
        ALBEDO_RSM,
        RED_LPV,
        GREEN_ACCUM_LPV,
        BLUE_ACCUM_LPV,
        GV,
        GV_COLOR,
    };
    VIZ_OPTIONS g_currVizChoice;

    enum ADITIONAL_OPTIONS_SELECT
    {
        SIMPLE_LIGHT,
        ADVANCED_LIGHT,
        SCENE,
        VIZ_TEXTURES,
    };
    ADITIONAL_OPTIONS_SELECT g_selectedAdditionalOption;
    final String[] g_tstr_addOpts =
    {
            "Simple Light Setup",
            "Advanced Light Setup",
            "Scene Setup","Viz Intermediates"
    };

// scene depth
    Texture2D g_pSceneDepth;
    Texture2D g_pSceneDepthRV;

// screen quad
    /*ID3D11VertexShader**/ShaderProgram            g_pScreenQuadVS = null;
    /*ID3D11VertexShader**/ShaderProgram            g_pScreenQuadPosTexVS2D = null;
    /*ID3D11VertexShader**/ShaderProgram            g_pScreenQuadPosTexVS3D = null;
    /*ID3D11PixelShader**/ShaderProgram             g_pScreenQuadDisplayPS2D = null;
    /*ID3D11PixelShader**/ShaderProgram             g_pScreenQuadDisplayPS2D_floatTextures = null;
    /*ID3D11PixelShader**/ShaderProgram             g_pScreenQuadReconstructPosFromDepth = null;
    /*ID3D11PixelShader**/ShaderProgram             g_pScreenQuadDisplayPS3D = null;
    /*ID3D11PixelShader**/ShaderProgram             g_pScreenQuadDisplayPS3D_floatTextures = null;
    BufferGL                  g_pScreenQuadVB = null;
    BufferGL                  g_pVizQuadVB = null;
    ID3D11InputLayout         g_pScreenQuadIL = null;
    ID3D11InputLayout         g_pScreenQuadPosTexIL = null;
    ID3D11InputLayout         g_pPos3Tex3IL = null;

    // simple shading
    /*ID3D11VertexShader**/ShaderProgram            g_pVS = null;
    /*ID3D11PixelShader**/ShaderProgram             g_pPS = null;
    /*ID3D11PixelShader**/ShaderProgram             g_pPS_separateFloatTextures = null;
    /*ID3D11VertexShader**/ShaderProgram            g_pSimpleVS = null;
    /*ID3D11PixelShader**/ShaderProgram             g_pSimplePS = null;
    /*ID3D11VertexShader**/ShaderProgram            g_pVSVizLPV = null;
    /*ID3D11PixelShader**/ShaderProgram             g_pPSVizLPV = null;


    // meshes
    SDKmesh                  g_LowResMesh;
    SDKmesh                  g_MeshArrow;
    SDKmesh                  g_MeshBox;
    final Vector3f           g_BoXExtents = new Vector3f();
    final Vector3f           g_BoxCenter = new Vector3f();


    RenderMesh                   g_MainMesh;
    RenderMesh                   g_MainMeshSimplified;
    RenderMesh                   g_MainMovableMesh;
    RenderMesh                   g_MovableBoxMesh;


    CB_LPV_PROPAGATE_GATHER      g_LPVPropagateGather = new CB_LPV_PROPAGATE_GATHER();
    CB_LPV_PROPAGATE_GATHER2     g_LPVPropagateGather2 = new CB_LPV_PROPAGATE_GATHER2();


    int           g_pDefaultSampler;
    int           g_pComparisonSampler;

    GLSLProgramPipeline g_Program;
    RenderTargets g_RenderTargets;
    GLFuncProvider gl;

    private static final int NUM_LIGHT_PRESETS = 6;
    private final Vector3f[] g_LightPresetPos = new Vector3f[6];

    public DiffuseGlobalIllumination(){
        //initialize variables
        g_shadowViewport.z = Defines.RSM_RES;
        g_shadowViewport.w = Defines.RSM_RES;
        g_shadowViewport.x = 0;
        g_shadowViewport.y = 0;

        g_shadowViewportScene.set(0,0,Defines.RSM_RES, Defines.RSM_RES);

        initializePresetLight();
    }

    void initializePresetLight()
    {
        g_LightPresetPos[0] = new Vector3f(-8.19899f, 19.372f, -7.36969f);
        g_LightPresetPos[1] = new Vector3f(-11.424f, 19.1181f, -0.895488f);
        g_LightPresetPos[2] = new Vector3f(-6.54583f, 21.3054f, 0.202703f);
        g_LightPresetPos[3] = new Vector3f(-6.06447f, 20.4372f, 6.50802f);
        g_LightPresetPos[4] = new Vector3f(-5.75932f, 19.2476f, 9.65256f);
        g_LightPresetPos[5] = new Vector3f(1.22359f, 11.173f, 21.2478f);
    }

    @Override
    protected void initRendering() {
        if(g_propType==PROP_TYPE.CASCADE)
            g_LPVLevelToInitialize = g_PropLevel;
        else
            g_LPVLevelToInitialize = Defines.HIERARCHICAL_INIT_LEVEL;

        /*D3D11_SAMPLER_DESC desc[1] = {
            D3D11_FILTER_MIN_MAG_MIP_POINT,
                    D3D11_TEXTURE_ADDRESS_CLAMP,
                    D3D11_TEXTURE_ADDRESS_CLAMP,
                    D3D11_TEXTURE_ADDRESS_CLAMP,
                    0.0, 0, D3D11_COMPARISON_NEVER, 0.0f, 0.0f, 0.0f, 0.0f, -1.0f, 1.0f,
        };
        pd3dDevice->CreateSamplerState(desc, &g_pDefaultSampler);
        pd3dDevice->CreateSamplerState(desc, &g_pDepthPeelingTexSampler);*/
        SamplerDesc desc = new SamplerDesc();
        desc.minFilter = GLenum.GL_NEAREST_MIPMAP_NEAREST;
        desc.magFilter = GLenum.GL_NEAREST;
        g_pDefaultSampler = SamplerUtils.createSampler(desc);
        g_pDepthPeelingTexSampler = SamplerUtils.createSampler(desc);


        /*D3D11_SAMPLER_DESC desc2[1] = {
            D3D11_FILTER_MIN_MAG_MIP_LINEAR,
                    D3D11_TEXTURE_ADDRESS_CLAMP,
                    D3D11_TEXTURE_ADDRESS_CLAMP,
                    D3D11_TEXTURE_ADDRESS_CLAMP,
                    0.0, 0, D3D11_COMPARISON_NEVER, 0.0f, 0.0f, 0.0f, 0.0f, -1.0f, 1.0f,
        };
        pd3dDevice->CreateSamplerState(desc2, &g_pLinearSampler);*/
        desc.minFilter = GLenum.GL_LINEAR_MIPMAP_LINEAR;
        desc.magFilter = GLenum.GL_LINEAR;
        g_pLinearSampler = SamplerUtils.createSampler(desc);

        /*D3D11_SAMPLER_DESC desc3 =
                {
                        D3D11_FILTER_ANISOTROPIC,// D3D11_FILTER Filter;
                        D3D11_TEXTURE_ADDRESS_WRAP, //D3D11_TEXTURE_ADDRESS_MODE AddressU;
                        D3D11_TEXTURE_ADDRESS_WRAP, //D3D11_TEXTURE_ADDRESS_MODE AddressV;
                        D3D11_TEXTURE_ADDRESS_WRAP, //D3D11_TEXTURE_ADDRESS_MODE AddressW;
                        0,//FLOAT MipLODBias;
                        D3DSAMP_MAXANISOTROPY,//UINT MaxAnisotropy;
                        D3D11_COMPARISON_NEVER , //D3D11_COMPARISON_FUNC ComparisonFunc;
                        0.0,0.0,0.0,0.0,//FLOAT BorderColor[ 4 ];
                        0,//FLOAT MinLOD;
                        D3D11_FLOAT32_MAX//FLOAT MaxLOD;
                };
        pd3dDevice->CreateSamplerState(&desc3, &g_pAnisoSampler);*/
        desc.anisotropic = 8;
        desc.wrapR = desc.wrapS = desc.wrapT = GLenum.GL_REPEAT;
        g_pAnisoSampler = SamplerUtils.createSampler(desc);


        /*D3D11_SAMPLER_DESC SamDescShad =
        {
                D3D11_FILTER_COMPARISON_MIN_MAG_LINEAR_MIP_POINT,// D3D11_FILTER Filter;
                D3D11_TEXTURE_ADDRESS_BORDER, //D3D11_TEXTURE_ADDRESS_MODE AddressU;
                D3D11_TEXTURE_ADDRESS_BORDER, //D3D11_TEXTURE_ADDRESS_MODE AddressV;
                D3D11_TEXTURE_ADDRESS_BORDER, //D3D11_TEXTURE_ADDRESS_MODE AddressW;
                0,//FLOAT MipLODBias;
                0,//UINT MaxAnisotropy;
                D3D11_COMPARISON_LESS , //D3D11_COMPARISON_FUNC ComparisonFunc;
                0.0,0.0,0.0,0.0,//FLOAT BorderColor[ 4 ];
                0,//FLOAT MinLOD;
                0//FLOAT MaxLOD;
        };
        V_RETURN( pd3dDevice->CreateSamplerState( &SamDescShad, &g_pComparisonSampler ) );*/

        desc.anisotropic = 0;
        desc.minFilter = GLenum.GL_NEAREST_MIPMAP_NEAREST;
        desc.magFilter = GLenum.GL_NEAREST;
        desc.wrapR = desc.wrapS = desc.wrapT = GLenum.GL_CLAMP_TO_BORDER;
        desc.borderColor = 0;
        desc.compareFunc = GLenum.GL_LESS;
        desc.compareMode = GLenum.GL_COMPARE_R_TO_TEXTURE;
        g_pComparisonSampler = SamplerUtils.createSampler(desc);

        try {
            loadShaders();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadShaders() throws IOException{
        final String path = "nvidia/DiffuseGlobalIllumination/shaders/";
        // create shaders
        /*V_RETURN( CompileShaderFromFile( L"ScreenQuad.hlsl", "VS", "vs_4_0", &pBlobVS ) );
        V_RETURN( pd3dDevice->CreateVertexShader( pBlobVS->GetBufferPointer(), pBlobVS->GetBufferSize(), NULL, &g_pScreenQuadVS ) );*/
        g_pScreenQuadVS = GLSLProgram.createShaderProgramFromFile("shader_libs/PostProcessingDefaultScreenSpaceVS.vert", ShaderType.VERTEX);

        // create the input layout
        /*D3D11_INPUT_ELEMENT_DESC layout[] =
                {
                        { "POSITION", 0, DXGI_FORMAT_R32G32B32_FLOAT, 0, 0, D3D11_INPUT_PER_VERTEX_DATA, 0 },
                };
        UINT numElements = sizeof( layout ) / sizeof( layout[0] );
        V_RETURN( pd3dDevice->CreateInputLayout( layout, numElements, pBlobVS->GetBufferPointer(), pBlobVS->GetBufferSize(), &g_pScreenQuadIL) );
        SAFE_RELEASE( pBlobVS );*/

        //vertex shader and input layout for screen quad with position and texture

        /*V_RETURN( CompileShaderFromFile( L"ScreenQuad.hlsl", "DisplayTextureVS", "vs_4_0", &pBlobVS ) );
        V_RETURN( pd3dDevice->CreateVertexShader( pBlobVS->GetBufferPointer(), pBlobVS->GetBufferSize(), NULL, &g_pScreenQuadPosTexVS3D ) );
        SAFE_RELEASE( pBlobVS );*/
        g_pScreenQuadPosTexVS3D = GLSLProgram.createShaderProgramFromFile(path + "DisplayTextureVS.vert", ShaderType.VERTEX);

        /*V_RETURN( CompileShaderFromFile( L"ScreenQuad.hlsl", "VS_POS_TEX", "vs_4_0", &pBlobVS ) );
        V_RETURN( pd3dDevice->CreateVertexShader( pBlobVS->GetBufferPointer(), pBlobVS->GetBufferSize(), NULL, &g_pScreenQuadPosTexVS2D ) );*/
        g_pScreenQuadPosTexVS2D = GLSLProgram.createShaderProgramFromFile(path + "VS_Simple.vert", ShaderType.VERTEX);

        /*D3D11_INPUT_ELEMENT_DESC layout2[] =
                {
                        { "POSITION", 0, DXGI_FORMAT_R32G32B32_FLOAT, 0, D3D11_APPEND_ALIGNED_ELEMENT, D3D11_INPUT_PER_VERTEX_DATA, 0 },
                        { "TEXCOORD", 0, DXGI_FORMAT_R32G32_FLOAT, 0, D3D11_APPEND_ALIGNED_ELEMENT, D3D11_INPUT_PER_VERTEX_DATA, 0 },
                };
        numElements = sizeof( layout2 ) / sizeof( layout2[0] );
        V_RETURN( pd3dDevice->CreateInputLayout( layout2, numElements, pBlobVS->GetBufferPointer(), pBlobVS->GetBufferSize(), &g_pScreenQuadPosTexIL) );
        SAFE_RELEASE( pBlobVS );*/
        g_pScreenQuadPosTexIL = new ID3D11InputLayout(){
            @Override
            public void bind() {
                gl.glEnableVertexAttribArray(0);
                gl.glVertexAttribPointer(0, 3, GLenum.GL_FLOAT, false, 20, 0);
                gl.glEnableVertexAttribArray(1);
                gl.glVertexAttribPointer(1, 2, GLenum.GL_FLOAT, false, 20, 12);
            }

            @Override
            public void unbind() {
                gl.glDisableVertexAttribArray(0);
                gl.glDisableVertexAttribArray(1);
            }
        };

        /*V_RETURN( CompileShaderFromFile( L"ScreenQuad.hlsl", "DisplayTexturePS2D", "ps_4_0", &pBlobPS ) );
        V_RETURN( pd3dDevice->CreatePixelShader( pBlobPS->GetBufferPointer(), pBlobPS->GetBufferSize(), NULL, &g_pScreenQuadDisplayPS2D ) );
        SAFE_RELEASE( pBlobPS );*/
        g_pScreenQuadDisplayPS2D = GLSLProgram.createShaderProgramFromFile(path + "DisplayTexturePS2D.frag", ShaderType.FRAGMENT);

        /*V_RETURN( CompileShaderFromFile( L"ScreenQuad.hlsl", "DisplayTexturePS2D_floatTextures", "ps_4_0", &pBlobPS ) );
        V_RETURN( pd3dDevice->CreatePixelShader( pBlobPS->GetBufferPointer(), pBlobPS->GetBufferSize(), NULL, &g_pScreenQuadDisplayPS2D_floatTextures ) );
        SAFE_RELEASE( pBlobPS );*/
        g_pScreenQuadDisplayPS2D_floatTextures = GLSLProgram.createShaderProgramFromFile(path + "DisplayTexturePS2D_floatTextures.frag", ShaderType.FRAGMENT);

        /*V_RETURN( CompileShaderFromFile( L"ScreenQuad.hlsl", "ReconstructPosFromDepth", "ps_4_0", &pBlobPS ) );
        V_RETURN( pd3dDevice->CreatePixelShader( pBlobPS->GetBufferPointer(), pBlobPS->GetBufferSize(), NULL, &g_pScreenQuadReconstructPosFromDepth ) );
        SAFE_RELEASE( pBlobPS );*/
        g_pScreenQuadReconstructPosFromDepth = GLSLProgram.createShaderProgramFromFile(path + "ReconstructPosFromDepth.frag", ShaderType.FRAGMENT);

        /*V_RETURN( CompileShaderFromFile( L"ScreenQuad.hlsl", "DisplayTexturePS3D", "ps_4_0", &pBlobPS ) );
        V_RETURN( pd3dDevice->CreatePixelShader( pBlobPS->GetBufferPointer(), pBlobPS->GetBufferSize(), NULL, &g_pScreenQuadDisplayPS3D ) );
        SAFE_RELEASE( pBlobPS );*/
        g_pScreenQuadDisplayPS3D = GLSLProgram.createShaderProgramFromFile(path + "DisplayTexturePS3D.frag", ShaderType.FRAGMENT);

        /*V_RETURN( CompileShaderFromFile( L"ScreenQuad.hlsl", "DisplayTexturePS3D_floatTextures", "ps_4_0", &pBlobPS ) );
        V_RETURN( pd3dDevice->CreatePixelShader( pBlobPS->GetBufferPointer(), pBlobPS->GetBufferSize(), NULL, &g_pScreenQuadDisplayPS3D_floatTextures ) );
        SAFE_RELEASE( pBlobPS );*/
        g_pScreenQuadDisplayPS3D_floatTextures = GLSLProgram.createShaderProgramFromFile(path + "DisplayTexturePS3D_floatTextures.frag", ShaderType.FRAGMENT);


        /*ID3DBlob* pBlob = NULL;
        V_RETURN( CompileShaderFromFile( L"LPV_Propagate.hlsl", "PropagateLPV", "cs_5_0", &pBlob ) );
        V_RETURN( pd3dDevice->CreateComputeShader( pBlob->GetBufferPointer(), pBlob->GetBufferSize(), NULL, &g_pCSPropagateLPV ) );
        SAFE_RELEASE( pBlob );*/
        g_pCSPropagateLPV = GLSLProgram.createShaderProgramFromFile(path + "PropagateLPV.comp", ShaderType.COMPUTE);

        /*V_RETURN( CompileShaderFromFile( L"LPV_Propagate.hlsl", "PropagateLPV_Simple", "cs_5_0", &pBlob ) );
        V_RETURN( pd3dDevice->CreateComputeShader( pBlob->GetBufferPointer(), pBlob->GetBufferSize(), NULL, &g_pCSPropagateLPVSimple ) );
        SAFE_RELEASE( pBlob );*/
        g_pCSPropagateLPVSimple = GLSLProgram.createShaderProgramFromFile(path + "PropagateLPV_Simple.comp", ShaderType.COMPUTE);

        /*V_RETURN( CompileShaderFromFile( L"LPV_Propagate.hlsl", "PropagateLPV_VS", "vs_4_0", &pBlob ) );
        V_RETURN( pd3dDevice->CreateVertexShader( pBlob->GetBufferPointer(), pBlob->GetBufferSize(), NULL, &g_pVSPropagateLPV ) );*/
        g_pVSPropagateLPV = GLSLProgram.createShaderProgramFromFile(path + "PropagateLPV_VS.vert", ShaderType.VERTEX);

        /*D3D11_INPUT_ELEMENT_DESC layout3[] =
                {
                        { "POSITION", 0, DXGI_FORMAT_R32G32B32_FLOAT, 0, D3D11_APPEND_ALIGNED_ELEMENT, D3D11_INPUT_PER_VERTEX_DATA, 0 },
                        { "TEXCOORD", 0, DXGI_FORMAT_R32G32B32_FLOAT, 0, D3D11_APPEND_ALIGNED_ELEMENT, D3D11_INPUT_PER_VERTEX_DATA, 0 },
                };
        numElements = sizeof( layout3 ) / sizeof( layout3[0] );
        V_RETURN( pd3dDevice->CreateInputLayout( layout3, numElements, pBlob->GetBufferPointer(), pBlob->GetBufferSize(), &g_pPos3Tex3IL) );
        SAFE_RELEASE( pBlob );*/
        g_pPos3Tex3IL = new ID3D11InputLayout() {
            @Override
            public void bind() {
                gl.glEnableVertexAttribArray(0);
                gl.glVertexAttribPointer(0, 3, GLenum.GL_FLOAT, false, 24, 0);
                gl.glEnableVertexAttribArray(1);
                gl.glVertexAttribPointer(1, 3, GLenum.GL_FLOAT, false, 24, 12);
            }

            @Override
            public void unbind() {
                gl.glDisableVertexAttribArray(0);
                gl.glDisableVertexAttribArray(1);
            }
        };


        /*V_RETURN( CompileShaderFromFile( L"LPV_Propagate.hlsl", "PropagateLPV_GS", "gs_4_0", &pBlob ) );
        V_RETURN( pd3dDevice->CreateGeometryShader( pBlob->GetBufferPointer(), pBlob->GetBufferSize(), NULL, &g_pGSPropagateLPV ) );
        SAFE_RELEASE( pBlob );*/
        g_pGSPropagateLPV = GLSLProgram.createShaderProgramFromFile(path + "PropagateLPV_GS.gemo", ShaderType.GEOMETRY);

        /*V_RETURN( CompileShaderFromFile( L"LPV_Propagate.hlsl", "PropagateLPV_PS", "ps_4_0", &pBlob ) );
        V_RETURN( pd3dDevice->CreatePixelShader( pBlob->GetBufferPointer(), pBlob->GetBufferSize(), NULL, &g_pPSPropagateLPV ) );
        SAFE_RELEASE( pBlob );*/
        g_pPSPropagateLPV = GLSLProgram.createShaderProgramFromFile(path + "PropagateLPV_PS.frag", ShaderType.FRAGMENT);

        /*V_RETURN( CompileShaderFromFile( L"LPV_Propagate.hlsl", "PropagateLPV_PS_Simple", "ps_4_0", &pBlob ) );
        V_RETURN( pd3dDevice->CreatePixelShader( pBlob->GetBufferPointer(), pBlob->GetBufferSize(), NULL, &g_pPSPropagateLPVSimple ) );
        SAFE_RELEASE( pBlob );*/
        g_pPSPropagateLPVSimple = GLSLProgram.createShaderProgramFromFile(path + "PropagateLPV_PS_Simple.frag", ShaderType.FRAGMENT);

        /*V_RETURN( CompileShaderFromFile( L"LPV_Accumulate.hlsl", "AccumulateLPV", "cs_5_0", &pBlob ) );
        V_RETURN( pd3dDevice->CreateComputeShader( pBlob->GetBufferPointer(), pBlob->GetBufferSize(), NULL, &g_pCSAccumulateLPV ) );
        SAFE_RELEASE( pBlob );*/
        g_pCSAccumulateLPV = GLSLProgram.createShaderProgramFromFile(path + "LPV_Accumulate.glsl", ShaderType.COMPUTE);


        /*V_RETURN( CompileShaderFromFile( L"LPV_Accumulate4.hlsl", "AccumulateLPV_singleFloats_8", "cs_5_0", &pBlob ) );
        V_RETURN( pd3dDevice->CreateComputeShader( pBlob->GetBufferPointer(), pBlob->GetBufferSize(), NULL, &g_pCSAccumulateLPV_singleFloats_8 ) );
        SAFE_RELEASE( pBlob );*/
        g_pCSAccumulateLPV_singleFloats_8 = GLSLProgram.createShaderProgramFromFile(path + "LPV_Accumulate4.glsl", ShaderType.COMPUTE,
                new Macro("ACCUMULATELPV_SINGLEFLOATS_8", 1));

        /*V_RETURN( CompileShaderFromFile( L"LPV_Accumulate4.hlsl", "AccumulateLPV_singleFloats_4", "cs_5_0", &pBlob ) );
        V_RETURN( pd3dDevice->CreateComputeShader( pBlob->GetBufferPointer(), pBlob->GetBufferSize(), NULL, &g_pCSAccumulateLPV_singleFloats_4 ) );
        SAFE_RELEASE( pBlob );*/
        g_pCSAccumulateLPV_singleFloats_4 = GLSLProgram.createShaderProgramFromFile(path + "LPV_Accumulate4.glsl", ShaderType.COMPUTE,
                new Macro("ACCUMULATELPV_SINGLEFLOATS_4", 1));

        // create the vertex buffer for a small visualization
        /*TexPosVertex verticesViz[] =
                {
                        TexPosVertex(D3DXVECTOR3( -1.0f, -1.0f, 0.0f ),D3DXVECTOR2( 0.0f, 1.0f)),
                        TexPosVertex(D3DXVECTOR3( -1.0f, -0.25f, 0.0f ),D3DXVECTOR2( 0.0f, 0.0f)),
                        TexPosVertex(D3DXVECTOR3( -0.25f, -1.0f, 0.0f ),D3DXVECTOR2( 1.0f, 1.0f)),
                        TexPosVertex(D3DXVECTOR3( -0.25f,-0.25f, 0.0f ),D3DXVECTOR2( 1.0f, 0.0f)),
                };
        bd.Usage = D3D11_USAGE_DEFAULT;
        bd.ByteWidth = sizeof( TexPosVertex ) * 4;
        bd.BindFlags = D3D11_BIND_VERTEX_BUFFER;
        bd.CPUAccessFlags = 0;
        bd.MiscFlags = 0;
        InitData.pSysMem = verticesViz;
        V_RETURN(  pd3dDevice->CreateBuffer( &bd, &InitData, &g_pVizQuadVB ) );*/
        g_pVizQuadVB = new BufferGL();

        FloatBuffer vertices = CacheBuffer.getCachedFloatBuffer(5*4);
        vertices.put(-1.0f).put(-1.0f).put(0.0f).put(0.0f).put(1.0f);
        vertices.put(-1.0f).put(-.25f).put(0.0f).put(0.0f).put(0.0f);
        vertices.put(-.25f).put(-1.0f).put(0.0f).put(1.0f).put(1.0f);
        vertices.put(-.25f).put(-.25f).put(0.0f).put(1.0f).put(0.0f);
        vertices.flip();
        g_pVizQuadVB.initlize(GLenum.GL_ARRAY_BUFFER, vertices.remaining() * 4, vertices, GLenum.GL_STATIC_DRAW);
        g_pVizQuadVB.unbind();

        //
        // load mesh and shading effects
        //
        /*V_RETURN( CompileShaderFromFile( L"SimpleShading.hlsl", "VS", "vs_4_0", &pBlobVS ) );
        V_RETURN( CompileShaderFromFile( L"SimpleShading.hlsl", "PS", "ps_4_0", &pBlobPS ) );
        V_RETURN( pd3dDevice->CreateVertexShader( pBlobVS->GetBufferPointer(), pBlobVS->GetBufferSize(), NULL, &g_pVS ) );
        V_RETURN( pd3dDevice->CreatePixelShader( pBlobPS->GetBufferPointer(), pBlobPS->GetBufferSize(), NULL, &g_pPS ) );*/
        g_pVS = GLSLProgram.createShaderProgramFromFile(path + "SimpleShadingVS.vert", ShaderType.VERTEX);
        g_pPS = GLSLProgram.createShaderProgramFromFile(path + "SimpleShadingPS.frag", ShaderType.FRAGMENT);


        // create the vertex input layout
        /*const D3D11_INPUT_ELEMENT_DESC meshLayout[] =
                {
                        { "POSITION",  0, DXGI_FORMAT_R32G32B32_FLOAT, 0, 0,  D3D11_INPUT_PER_VERTEX_DATA, 0 },
                        { "NORMAL",    0, DXGI_FORMAT_R32G32B32_FLOAT, 0, 12, D3D11_INPUT_PER_VERTEX_DATA, 0 },
                        { "TEXCOORD",  0, DXGI_FORMAT_R32G32_FLOAT,    0, 24, D3D11_INPUT_PER_VERTEX_DATA, 0 },
                        { "TANGENT",   0, DXGI_FORMAT_R32G32B32_FLOAT, 0, 32, D3D11_INPUT_PER_VERTEX_DATA, 0 },
                };

        V_RETURN( pd3dDevice->CreateInputLayout( meshLayout, ARRAYSIZE( meshLayout ), pBlobVS->GetBufferPointer(), pBlobVS->GetBufferSize(), &g_pMeshLayout ) );*/
        final int stride = (3+3+2+3) * 4;
        g_pMeshLayout = new ID3D11InputLayout() {
            @Override
            public void bind() {
                gl.glEnableVertexAttribArray(0);
                gl.glVertexAttribPointer(0, 3, GLenum.GL_FLOAT, false, stride, 0);
                gl.glEnableVertexAttribArray(1);
                gl.glVertexAttribPointer(1, 3, GLenum.GL_FLOAT, false, stride, 12);
                gl.glEnableVertexAttribArray(2);
                gl.glVertexAttribPointer(2, 2, GLenum.GL_FLOAT, false, stride, 24);
                gl.glEnableVertexAttribArray(3);
                gl.glVertexAttribPointer(3, 3, GLenum.GL_FLOAT, false, stride, 32);
            }

            @Override
            public void unbind() {
                gl.glDisableVertexAttribArray(0);
                gl.glDisableVertexAttribArray(1);
                gl.glDisableVertexAttribArray(2);
                gl.glDisableVertexAttribArray(3);
            }
        };

        /*SAFE_RELEASE( pBlobVS );
        SAFE_RELEASE( pBlobPS );*/

        /*V_RETURN( CompileShaderFromFile( L"SimpleShading.hlsl", "PS_separateFloatTextures", "ps_4_0", &pBlobPS ) );
        V_RETURN( pd3dDevice->CreatePixelShader( pBlobPS->GetBufferPointer(), pBlobPS->GetBufferSize(), NULL, &g_pPS_separateFloatTextures ) );*/
        g_pPS_separateFloatTextures = GLSLProgram.createShaderProgramFromFile(path + "SimpleShadingPS_Float.frag", ShaderType.FRAGMENT);

        /*V_RETURN( CompileShaderFromFile( L"SimpleShading.hlsl", "VS_Simple", "vs_4_0", &pBlobVS ) );
        V_RETURN( CompileShaderFromFile( L"SimpleShading.hlsl", "PS_Simple", "ps_4_0", &pBlobPS ) );
        V_RETURN( pd3dDevice->CreateVertexShader( pBlobVS->GetBufferPointer(), pBlobVS->GetBufferSize(), NULL, &g_pSimpleVS ) );
        V_RETURN( pd3dDevice->CreatePixelShader( pBlobPS->GetBufferPointer(), pBlobPS->GetBufferSize(), NULL, &g_pSimplePS ) );
        SAFE_RELEASE( pBlobVS );
        SAFE_RELEASE( pBlobPS );*/
        g_pSimpleVS = GLSLProgram.createShaderProgramFromFile(path + "VS_Simple.vert", ShaderType.VERTEX);
        g_pSimplePS = GLSLProgram.createShaderProgramFromFile(path + "PS_Simple.frag", ShaderType.FRAGMENT);

        /*V_RETURN( CompileShaderFromFile( L"SimpleShading.hlsl", "VS_RSM", "vs_4_0", &pBlobVS ) );
        V_RETURN( CompileShaderFromFile( L"SimpleShading.hlsl", "PS_RSM", "ps_4_0", &pBlobPS ) );
        V_RETURN( pd3dDevice->CreateVertexShader( pBlobVS->GetBufferPointer(), pBlobVS->GetBufferSize(), NULL, &g_pVSRSM ) );
        V_RETURN( pd3dDevice->CreatePixelShader( pBlobPS->GetBufferPointer(), pBlobPS->GetBufferSize(), NULL, &g_pPSRSM ) );
        SAFE_RELEASE( pBlobVS );
        SAFE_RELEASE( pBlobPS );*/
        g_pVSRSM = GLSLProgram.createShaderProgramFromFile(path + "VS_RSM.vert", ShaderType.VERTEX);
        g_pPSRSM = GLSLProgram.createShaderProgramFromFile(path + "PS_RSM.frag", ShaderType.FRAGMENT);

        /*V_RETURN( CompileShaderFromFile( L"SimpleShading.hlsl", "VS_ShadowMap", "vs_4_0", &pBlobVS ) );
        V_RETURN( pd3dDevice->CreateVertexShader( pBlobVS->GetBufferPointer(), pBlobVS->GetBufferSize(), NULL, &g_pVSSM ) );
        SAFE_RELEASE( pBlobVS );*/
        g_pVSSM = GLSLProgram.createShaderProgramFromFile(path + "VS_ShadowMap.vert", ShaderType.VERTEX);

        /*V_RETURN( CompileShaderFromFile( L"SimpleShading.hlsl", "VS_RSM_DepthPeeling", "vs_4_0", &pBlobVS ) );
        V_RETURN( CompileShaderFromFile( L"SimpleShading.hlsl", "PS_RSM_DepthPeeling", "ps_4_0", &pBlobPS ) );
        V_RETURN( pd3dDevice->CreateVertexShader( pBlobVS->GetBufferPointer(), pBlobVS->GetBufferSize(), NULL, &g_pVSRSMDepthPeeling ) );
        V_RETURN( pd3dDevice->CreatePixelShader( pBlobPS->GetBufferPointer(), pBlobPS->GetBufferSize(), NULL, &g_pPSRSMDepthPeel ) );
        SAFE_RELEASE( pBlobVS );
        SAFE_RELEASE( pBlobPS );*/
        g_pVSRSMDepthPeeling = GLSLProgram.createShaderProgramFromFile(path + "VS_RSM_DepthPeeling.vert", ShaderType.VERTEX);
        g_pPSRSMDepthPeel = GLSLProgram.createShaderProgramFromFile(path + "PS_RSM_DepthPeeling.frag", ShaderType.FRAGMENT);

        /*V_RETURN( CompileShaderFromFile( L"SimpleShading.hlsl", "VS_VizLPV", "vs_4_0", &pBlobVS ) );
        V_RETURN( CompileShaderFromFile( L"SimpleShading.hlsl", "PS_VizLPV", "ps_4_0", &pBlobPS ) );
        V_RETURN( pd3dDevice->CreateVertexShader( pBlobVS->GetBufferPointer(), pBlobVS->GetBufferSize(), NULL, &g_pVSVizLPV ) );
        V_RETURN( pd3dDevice->CreatePixelShader( pBlobPS->GetBufferPointer(), pBlobPS->GetBufferSize(), NULL, &g_pPSVizLPV ) );
        SAFE_RELEASE( pBlobVS );
        SAFE_RELEASE( pBlobPS );*/
        g_pVSVizLPV = GLSLProgram.createShaderProgramFromFile(path + "VS_VizLPV.vert", ShaderType.VERTEX);
        g_pPSVizLPV = GLSLProgram.createShaderProgramFromFile(path + "PS_VizLPV.frag", ShaderType.FRAGMENT);

        g_MainMesh = new RenderMesh();
        g_MainMeshSimplified = new RenderMesh();
        g_MainMovableMesh = new RenderMesh();
        g_MovableBoxMesh = new RenderMesh();

        LoadMainMeshes(/*pd3dDevice*/);

        g_MovableBoxMesh.m_Mesh.create( /*pd3dDevice,*/ "..\\Media\\box.sdkmesh", true, null );

        g_LowResMesh.create( "..\\Media\\unitSphere.sdkmesh", true, null );
        g_MeshArrow.create( "..\\Media\\arrow.sdkmesh", true, null );
        g_MeshBox.create( "..\\Media\\box.sdkmesh", true, null );

        g_BoXExtents.set(g_MeshBox.getMeshBBoxExtents(0));
        g_BoxCenter.set(g_MeshBox.getMeshBBoxCenter(0));

        g_MovableBoxMesh.setWorldMatrix(1.0f/g_BoXExtents.x,16.0f/g_BoXExtents.y,16.0f/g_BoXExtents.z,0,0,0,0,0,0);
        g_MovableBoxMesh.m_UseTexture = false;

        // setup constant buffers
        /*D3D11_BUFFER_DESC Desc;
        Desc.Usage = D3D11_USAGE_DYNAMIC;
        Desc.BindFlags = D3D11_BIND_CONSTANT_BUFFER;
        Desc.CPUAccessFlags = D3D11_CPU_ACCESS_WRITE;
        Desc.MiscFlags = 0;
        Desc.ByteWidth = sizeof( CB_VS_PER_OBJECT );
        V_RETURN( pd3dDevice->CreateBuffer( &Desc, NULL, &g_pcbVSPerObject ) );*/
        g_pcbVSPerObject = new BufferGL();
        g_pcbVSPerObject.initlize(GLenum.GL_UNIFORM_BUFFER, CB_VS_PER_OBJECT.SIZE, null, GLenum.GL_DYNAMIC_DRAW);

        /*Desc.ByteWidth = sizeof( CB_VS_GLOBAL );
        V_RETURN( pd3dDevice->CreateBuffer( &Desc, NULL, &g_pcbVSGlobal ) );*/
        g_pcbVSGlobal = new BufferGL();
        g_pcbVSGlobal.initlize(GLenum.GL_UNIFORM_BUFFER, CB_VS_GLOBAL.SIZE, null, GLenum.GL_DYNAMIC_DRAW);

        /*Desc.ByteWidth = sizeof( CB_LPV_INITIALIZE );
        V_RETURN( pd3dDevice->CreateBuffer( &Desc, NULL, &g_pcbLPVinitVS ) );*/
        g_pcbLPVinitVS = new BufferGL();
        g_pcbLPVinitVS.initlize(GLenum.GL_UNIFORM_BUFFER, CB_LPV_INITIALIZE.SIZE, null, GLenum.GL_DYNAMIC_DRAW);

        /*Desc.ByteWidth = sizeof( CB_LPV_INITIALIZE3 );
        V_RETURN( pd3dDevice->CreateBuffer( &Desc, NULL, &g_pcbLPVinitialize_LPVDims ) );*/
        g_pcbLPVinitialize_LPVDims = new BufferGL();
        g_pcbLPVinitialize_LPVDims.initlize(GLenum.GL_UNIFORM_BUFFER, CB_LPV_INITIALIZE3.SIZE, null, GLenum.GL_DYNAMIC_DRAW);

        /*Desc.ByteWidth = sizeof( CB_LPV_PROPAGATE );
        V_RETURN( pd3dDevice->CreateBuffer( &Desc, NULL, &g_pcbLPVpropagate ) );*/
        g_pcbLPVpropagate = new BufferGL();
        g_pcbLPVpropagate.initlize(GLenum.GL_UNIFORM_BUFFER, CB_LPV_PROPAGATE.SIZE, null, GLenum.GL_DYNAMIC_DRAW);

        /*Desc.ByteWidth = sizeof( CB_RENDER );
        V_RETURN( pd3dDevice->CreateBuffer( &Desc, NULL, &g_pcbRender ) );*/
        g_pcbRender = new BufferGL();
        g_pcbRender.initlize(GLenum.GL_UNIFORM_BUFFER, CB_RENDER.SIZE, null, GLenum.GL_DYNAMIC_DRAW);

        /*Desc.ByteWidth = sizeof( CB_RENDER_LPV );
        V_RETURN( pd3dDevice->CreateBuffer( &Desc, NULL, &g_pcbRenderLPV ) );*/
        g_pcbRenderLPV = new BufferGL();
        g_pcbRenderLPV.initlize(GLenum.GL_UNIFORM_BUFFER, CB_RENDER_LPV.SIZE, null, GLenum.GL_DYNAMIC_DRAW);

        /*Desc.ByteWidth = sizeof( CB_SM_TAP_LOCS );
        V_RETURN( pd3dDevice->CreateBuffer( &Desc, NULL, &g_pcbPSSMTapLocations ) );*/
        g_pcbPSSMTapLocations = new BufferGL();
        g_pcbPSSMTapLocations.initlize(GLenum.GL_UNIFORM_BUFFER, CB_SM_TAP_LOCS.SIZE, null, GLenum.GL_DYNAMIC_DRAW);

        /*Desc.ByteWidth = sizeof( CB_RENDER_LPV );
        V_RETURN( pd3dDevice->CreateBuffer( &Desc, NULL, &g_pcbRenderLPV2 ) );*/
        g_pcbRenderLPV2 = new BufferGL();
        g_pcbRenderLPV2.initlize(GLenum.GL_UNIFORM_BUFFER, CB_RENDER_LPV.SIZE, null, GLenum.GL_DYNAMIC_DRAW);

        /*Desc.ByteWidth = sizeof( CB_LPV_INITIALIZE2 );
        V_RETURN( pd3dDevice->CreateBuffer( &Desc, NULL, &g_pcbLPVinitVS2 ) );*/
        g_pcbLPVinitVS2 = new BufferGL();
        g_pcbLPVinitVS2.initlize(GLenum.GL_UNIFORM_BUFFER, CB_LPV_INITIALIZE2.SIZE, null, GLenum.GL_DYNAMIC_DRAW);

        /*Desc.ByteWidth = sizeof( CB_LPV_PROPAGATE_GATHER );
        V_RETURN( pd3dDevice->CreateBuffer( &Desc, NULL, &g_pcbLPVpropagateGather ) );*/
        g_pcbLPVpropagateGather = new BufferGL();
        g_pcbLPVpropagateGather.initlize(GLenum.GL_UNIFORM_BUFFER, CB_LPV_PROPAGATE_GATHER.SIZE, null, GLenum.GL_DYNAMIC_DRAW);

        /*Desc.ByteWidth = sizeof( CB_LPV_PROPAGATE_GATHER2 );
        V_RETURN( pd3dDevice->CreateBuffer( &Desc, NULL, &g_pcbLPVpropagateGather2 ) );*/
        g_pcbLPVpropagateGather2 = new BufferGL();
        g_pcbLPVpropagateGather2.initlize(GLenum.GL_UNIFORM_BUFFER, CB_LPV_PROPAGATE_GATHER2.SIZE, null, GLenum.GL_DYNAMIC_DRAW);

        /*Desc.ByteWidth = sizeof( CB_GV );
        V_RETURN( pd3dDevice->CreateBuffer( &Desc, NULL, &g_pcbGV ) );*/
        g_pcbGV = new BufferGL();
        g_pcbGV.initlize(GLenum.GL_UNIFORM_BUFFER, CB_GV.SIZE, null, GLenum.GL_DYNAMIC_DRAW);

        /*Desc.ByteWidth = sizeof( CB_SIMPLE_OBJECTS );
        V_RETURN( pd3dDevice->CreateBuffer( &Desc, NULL, &g_pcbSimple ) );*/
        g_pcbSimple = new BufferGL();
        g_pcbSimple.initlize(GLenum.GL_UNIFORM_BUFFER, CB_SIMPLE_OBJECTS.SIZE, null, GLenum.GL_DYNAMIC_DRAW);

        /*Desc.ByteWidth = sizeof( VIZ_LPV );
        V_RETURN( pd3dDevice->CreateBuffer( &Desc, NULL, &g_pcbLPVViz ) );*/
        g_pcbLPVViz = new BufferGL();
        g_pcbLPVViz.initlize(GLenum.GL_UNIFORM_BUFFER, Vector4f.SIZE, null, GLenum.GL_DYNAMIC_DRAW);

        /*Desc.ByteWidth = sizeof( CB_MESH_RENDER_OPTIONS );
        V_RETURN( pd3dDevice->CreateBuffer( &Desc, NULL, &g_pcbMeshRenderOptions ) );*/
        g_pcbMeshRenderOptions = new BufferGL();
        g_pcbMeshRenderOptions.initlize(GLenum.GL_UNIFORM_BUFFER, Vector4f.SIZE, null, GLenum.GL_DYNAMIC_DRAW);

        /*Desc.ByteWidth = sizeof( CB_DRAW_SLICES_3D );
        V_RETURN( pd3dDevice->CreateBuffer( &Desc, NULL, &g_pcbSlices3D ) );*/
        g_pcbSlices3D = new BufferGL();
        g_pcbSlices3D.initlize(GLenum.GL_UNIFORM_BUFFER, Vector4f.SIZE, null, GLenum.GL_DYNAMIC_DRAW);

        /*Desc.ByteWidth = sizeof( RECONSTRUCT_POS );
        V_RETURN( pd3dDevice->CreateBuffer( &Desc, NULL, &g_reconPos ) );*/
        g_reconPos = new BufferGL();
        g_reconPos.initlize(GLenum.GL_UNIFORM_BUFFER, Vector4f.SIZE, null, GLenum.GL_DYNAMIC_DRAW);

        /*Desc.ByteWidth = sizeof( CB_INVPROJ_MATRIX );
        V_RETURN( pd3dDevice->CreateBuffer( &Desc, NULL, &g_pcbInvProjMatrix ) );*/
        g_pcbInvProjMatrix = new BufferGL();
        g_pcbInvProjMatrix.initlize(GLenum.GL_UNIFORM_BUFFER, Matrix4f.SIZE, null, GLenum.GL_DYNAMIC_DRAW);


        //shadow map for the scene--------------------------------------------------------------

        /*D3D11_TEXTURE2D_DESC texDesc;
        texDesc.ArraySize          = 1;
        texDesc.BindFlags          = D3D11_BIND_DEPTH_STENCIL | D3D11_BIND_SHADER_RESOURCE;
        texDesc.CPUAccessFlags     = NULL;
        texDesc.Format             = DXGI_FORMAT_R32_TYPELESS;
        texDesc.Width              = SM_SIZE;
        texDesc.Height             = SM_SIZE;
        texDesc.MipLevels          = 1;
        texDesc.MiscFlags          = NULL;
        texDesc.SampleDesc.Count   = 1;
        texDesc.SampleDesc.Quality = 0;
        texDesc.Usage              = D3D11_USAGE_DEFAULT;*/
        Texture2DDesc texDesc = new Texture2DDesc(Defines.SM_SIZE, Defines.SM_SIZE, GLenum.GL_DEPTH_COMPONENT32F);

        g_pSceneShadowMap = new DepthRT( /*pd3dDevice,*/ texDesc );

        //stuff for RSMs and LPVs etc ----------------------------------------------------------

        //initialize the shadow map data
        initializeReflectiveShadowMaps(/*pd3dDevice*/);

        /*D3D11_RASTERIZER_DESC rasterizerState;
        rasterizerState.CullMode = D3D11_CULL_BACK;
        rasterizerState.FillMode = D3D11_FILL_SOLID;
        rasterizerState.FrontCounterClockwise = false;
        rasterizerState.DepthBias = 0;
        rasterizerState.DepthBiasClamp = 0.0f;
        rasterizerState.SlopeScaledDepthBias = 0.0f;
        rasterizerState.DepthClipEnable = true;
        rasterizerState.ScissorEnable = false;
        rasterizerState.MultisampleEnable = true;
        rasterizerState.AntialiasedLineEnable = false;
        pd3dDevice->CreateRasterizerState( &rasterizerState, &g_pRasterizerStateMainRender);
        rasterizerState.CullMode = D3D11_CULL_NONE;
        pd3dDevice->CreateRasterizerState( &rasterizerState, &pRasterizerStateCullNone);
        rasterizerState.CullMode = D3D11_CULL_FRONT;
        pd3dDevice->CreateRasterizerState( &rasterizerState, &pRasterizerStateCullFront);
        rasterizerState.FillMode = D3D11_FILL_WIREFRAME;
        pd3dDevice->CreateRasterizerState( &rasterizerState, &pRasterizerStateWireFrame);*/
        g_pRasterizerStateMainRender = ()->
        {
            gl.glEnable(GLenum.GL_CULL_FACE);
            gl.glCullFace(GLenum.GL_BACK);
            gl.glPolygonMode(GLenum.GL_FRONT_AND_BACK, GLenum.GL_FILL);
        };

        pRasterizerStateCullNone = ()->
        {
            gl.glDisable(GLenum.GL_CULL_FACE);
            gl.glPolygonMode(GLenum.GL_FRONT_AND_BACK, GLenum.GL_FILL);
        };

        pRasterizerStateCullFront = ()->
        {
            gl.glEnable(GLenum.GL_CULL_FACE);
            gl.glCullFace(GLenum.GL_FRONT);
            gl.glPolygonMode(GLenum.GL_FRONT_AND_BACK, GLenum.GL_FILL);
        };

        pRasterizerStateWireFrame = ()->
        {
            gl.glEnable(GLenum.GL_CULL_FACE);
            gl.glCullFace(GLenum.GL_FRONT);
            gl.glPolygonMode(GLenum.GL_FRONT_AND_BACK, GLenum.GL_LINE);
        };

        //create the grid used to render to our flat 3D textures (splatted into large 2D textures)

        /*ID3D11VertexShader* pLPV_init_VS = NULL;
        V_RETURN( CompileShaderFromFile( L"ScreenQuad.hlsl", "DisplayTextureVS", "vs_4_0", &pBlobVS ) );
        V_RETURN( pd3dDevice->CreateVertexShader( pBlobVS->GetBufferPointer(), pBlobVS->GetBufferSize(), NULL, &pLPV_init_VS ) );
        g_grid = new Grid(pd3dDevice,pd3dImmediateContext);
        g_grid->Initialize(g_LPVWIDTH,g_LPVHEIGHT,g_LPVDEPTH,pBlobVS->GetBufferPointer(), pBlobVS->GetBufferSize());
        SAFE_RELEASE( pBlobVS );
        SAFE_RELEASE( pLPV_init_VS );*/
        g_grid = new Grid();
        g_grid.Initialize(Defines.g_LPVWIDTH,Defines.g_LPVHEIGHT,Defines.g_LPVDEPTH);

        createPropagationAndGeometryVolumes(/*pd3dDevice*/);

        /*g_LPVViewport.MinDepth = 0;
        g_LPVViewport.MaxDepth = 1;
        g_LPVViewport.TopLeftX = 0;
        g_LPVViewport.TopLeftY = 0;
        g_LPVViewport3D.Width  = g_LPVWIDTH;
        g_LPVViewport3D.Height = g_LPVHEIGHT;
        g_LPVViewport3D.MinDepth = 0;
        g_LPVViewport3D.MaxDepth = 1;
        g_LPVViewport3D.TopLeftX = 0;
        g_LPVViewport3D.TopLeftY = 0;*/
        g_LPVViewport3D.set(0,0, Defines.g_LPVWIDTH,Defines.g_LPVHEIGHT);

        //load the shaders and create the layout for the LPV initialization
        /*V_RETURN( CompileShaderFromFile( L"LPV.hlsl", "VS_initializeLPV", "vs_4_0", &pBlobVS ) );
        V_RETURN( pd3dDevice->CreateVertexShader( pBlobVS->GetBufferPointer(), pBlobVS->GetBufferSize(), NULL, &g_pVSInitializeLPV ) );
        SAFE_RELEASE( pBlobVS );*/
        g_pVSInitializeLPV = GLSLProgram.createShaderProgramFromFile(path + "VS_initializeLPV.vert", ShaderType.VERTEX);

        /*V_RETURN( CompileShaderFromFile( L"LPV.hlsl", "VS_initializeLPV_Bilnear", "vs_4_0", &pBlobVS ) );
        V_RETURN( pd3dDevice->CreateVertexShader( pBlobVS->GetBufferPointer(), pBlobVS->GetBufferSize(), NULL, &g_pVSInitializeLPV_Bilinear ) );
        SAFE_RELEASE( pBlobVS );*/
        g_pVSInitializeLPV_Bilinear = GLSLProgram.createShaderProgramFromFile(path + "VS_initializeLPV_Bilnear.vert", ShaderType.VERTEX);

        /*V_RETURN( CompileShaderFromFile( L"LPV.hlsl", "PS_initializeLPV", "ps_4_0", &pBlobPS ) );
        V_RETURN( pd3dDevice->CreatePixelShader( pBlobPS->GetBufferPointer(), pBlobPS->GetBufferSize(), NULL, &g_pPSInitializeLPV ) );
        SAFE_RELEASE( pBlobPS );*/
        g_pPSInitializeLPV = GLSLProgram.createShaderProgramFromFile(path + "PS_initializeLPV.vert", ShaderType.VERTEX);

        /*V_RETURN( CompileShaderFromFile( L"LPV.hlsl", "GS_initializeLPV", "gs_4_0", &pBlobGS ) );
        V_RETURN( pd3dDevice->CreateGeometryShader( pBlobGS->GetBufferPointer(), pBlobGS->GetBufferSize(), NULL, &g_pGSInitializeLPV ) );
        SAFE_RELEASE( pBlobGS );*/
        g_pGSInitializeLPV = GLSLProgram.createShaderProgramFromFile(path + "GS_initializeLPV.gemo", ShaderType.GEOMETRY);

        /*V_RETURN( CompileShaderFromFile( L"LPV.hlsl", "GS_initializeLPV_Bilinear", "gs_4_0", &pBlobGS ) );
        V_RETURN( pd3dDevice->CreateGeometryShader( pBlobGS->GetBufferPointer(), pBlobGS->GetBufferSize(), NULL, &g_pGSInitializeLPV_Bilinear ) );
        SAFE_RELEASE( pBlobGS );*/
        g_pGSInitializeLPV_Bilinear = GLSLProgram.createShaderProgramFromFile(path + "GS_initializeLPV_Bilinear.gemo", ShaderType.GEOMETRY);

        //load the shaders and create the layout for the GV initialization
        /*V_RETURN( CompileShaderFromFile( L"LPV.hlsl", "PS_initializeGV", "ps_4_0", &pBlobPS ) );
        V_RETURN( pd3dDevice->CreatePixelShader( pBlobPS->GetBufferPointer(), pBlobPS->GetBufferSize(), NULL, &g_pPSInitializeGV ) );
        SAFE_RELEASE( pBlobPS );*/
        g_pPSInitializeGV = GLSLProgram.createShaderProgramFromFile(path + "PS_initializeGV.frag", ShaderType.FRAGMENT);

        Vector3f[] offsets = new Vector3f[6];
        //offsets to the six neighbors of a cell at 0,0,0
        offsets[0] = new Vector3f(0,0,1);
        offsets[1] = new Vector3f(1,0,0);
        offsets[2] = new Vector3f(0,0,-1);
        offsets[3] = new Vector3f(-1,0,0);
        offsets[4] = new Vector3f(0,1,0);
        offsets[5] = new Vector3f(0,-1,0);

        for(int neighbor=0; neighbor<6; neighbor++)
        {
            Vector3f neighborCellCenter = offsets[neighbor];

            Vector4f occlusionOffsets = new Vector4f(0,0,0,0);
            if(neighborCellCenter.x>0) occlusionOffsets.set(6,1,2,0);
            else if(neighborCellCenter.x<0) occlusionOffsets.set(7,5,4,3);
            else if(neighborCellCenter.y>0) occlusionOffsets.set(0,3,1,5);
            else if(neighborCellCenter.y<0) occlusionOffsets.set(2,4,6,7);
            else if(neighborCellCenter.z>0) occlusionOffsets.set(0,3,2,4);
            else if(neighborCellCenter.z<0) occlusionOffsets.set(1,5,6,7);

            Vector4f multiBounceOffsets = new Vector4f(0,0,0,0);
            if(neighborCellCenter.x>0) multiBounceOffsets.set(7,5,4,3);
            else if(neighborCellCenter.x<0) multiBounceOffsets.set(6,1,2,0);
            else if(neighborCellCenter.y>0) multiBounceOffsets.set(2,4,6,7);
            else if(neighborCellCenter.y<0) multiBounceOffsets.set(0,3,1,5);
            else if(neighborCellCenter.z>0) multiBounceOffsets.set(1,5,6,7);
            else if(neighborCellCenter.z<0) multiBounceOffsets.set(0,3,2,4);

            g_LPVPropagateGather2.propConsts2[neighbor].multiBounceOffsetX = (int)multiBounceOffsets.x;
            g_LPVPropagateGather2.propConsts2[neighbor].multiBounceOffsetY = (int)multiBounceOffsets.y;
            g_LPVPropagateGather2.propConsts2[neighbor].multiBounceOffsetZ = (int)multiBounceOffsets.z;
            g_LPVPropagateGather2.propConsts2[neighbor].multiBounceOffsetW = (int)multiBounceOffsets.w;
            g_LPVPropagateGather2.propConsts2[neighbor].occlusionOffsetX   = (int)occlusionOffsets.x;
            g_LPVPropagateGather2.propConsts2[neighbor].occlusionOffsetY   = (int)occlusionOffsets.y;
            g_LPVPropagateGather2.propConsts2[neighbor].occlusionOffsetZ   = (int)occlusionOffsets.z;
            g_LPVPropagateGather2.propConsts2[neighbor].occlusionOffsetW   = (int)occlusionOffsets.w;

            //for each of the six faces of a cell
            for(int face=0;face<6;face++)
            {
//                D3DXVECTOR3 facePosition = offsets[face]*0.5f;
                Vector3f facePosition = Vector3f.scale(offsets[face], 0.5f, null);
                //the vector from the neighbor's cell center
                Vector3f vecFromNCC = Vector3f.sub(facePosition, neighborCellCenter, null);
                float length = vecFromNCC.length();
                vecFromNCC.scale(1.0f/length);

                g_LPVPropagateGather.propConsts[neighbor*6+face].neighborOffset.set(neighborCellCenter.x, neighborCellCenter.y, neighborCellCenter.z,1.0f);
                g_LPVPropagateGather.propConsts[neighbor*6+face].x = vecFromNCC.x;
                g_LPVPropagateGather.propConsts[neighbor*6+face].y = vecFromNCC.y;
                g_LPVPropagateGather.propConsts[neighbor*6+face].z = vecFromNCC.z;
                //the solid angle subtended by the face onto the neighbor cell center is one of two values below, depending on whether the cell center is directly below
                //the face or off to one side.
                //note, there is a derivation for these numbers (based on the solid angle subtended at the apex of a foursided right regular pyramid)
                //we also normalize the solid angle by dividing by 4*PI (the solid angle of a sphere measured from a point in its interior)
                if(length<=0.5f)
                    g_LPVPropagateGather.propConsts[neighbor*6+face].solidAngle = 0.0f;
                else
                    g_LPVPropagateGather.propConsts[neighbor*6+face].solidAngle = length>=1.5f ? 22.95668f/(4*180.0f) : 24.26083f/(4*180.0f) ;
            }
        }

        ByteBuffer bytes = CacheBuffer.wrap(CB_LPV_PROPAGATE_GATHER.SIZE, g_LPVPropagateGather);
        g_pcbLPVpropagateGather.update(0, bytes);

        bytes = CacheBuffer.wrap(CB_LPV_PROPAGATE_GATHER2.SIZE, g_LPVPropagateGather2);
        g_pcbLPVpropagateGather2.update(0, bytes);

        /*D3D11_BLEND_DESC blendStateAlpha;
        blendStateAlpha.AlphaToCoverageEnable = FALSE;
        blendStateAlpha.IndependentBlendEnable = FALSE;
        for (int i = 0; i < 8; ++i)
        {
            blendStateAlpha.RenderTarget[i].BlendEnable = TRUE;
            blendStateAlpha.RenderTarget[i].BlendOp = D3D11_BLEND_OP_ADD;
            blendStateAlpha.RenderTarget[i].BlendOpAlpha = D3D11_BLEND_OP_ADD;
            blendStateAlpha.RenderTarget[i].DestBlend = D3D11_BLEND_INV_SRC_ALPHA;
            blendStateAlpha.RenderTarget[i].DestBlendAlpha = D3D11_BLEND_ONE;
            blendStateAlpha.RenderTarget[i].SrcBlend = D3D11_BLEND_SRC_ALPHA;
            blendStateAlpha.RenderTarget[i].SrcBlendAlpha = D3D11_BLEND_ONE;
            blendStateAlpha.RenderTarget[i].RenderTargetWriteMask = D3D11_COLOR_WRITE_ENABLE_ALL;
        }
        V( pd3dDevice->CreateBlendState(&blendStateAlpha, &g_pAlphaBlendBS) );*/

        g_pAlphaBlendBS = ()->
        {
            gl.glEnable(GLenum.GL_BLEND);
            gl.glBlendFuncSeparate(GLenum.GL_SRC_ALPHA, GLenum.GL_ONE_MINUS_SRC_ALPHA, GLenum.GL_ONE, GLenum.GL_ONE);
        };

        /*D3D11_BLEND_DESC blendState;
        blendState.AlphaToCoverageEnable = FALSE;
        blendState.IndependentBlendEnable = FALSE;
        for (int i = 0; i < 8; ++i)
        {
            blendState.RenderTarget[i].BlendEnable = FALSE;
            blendState.RenderTarget[i].RenderTargetWriteMask = D3D11_COLOR_WRITE_ENABLE_ALL;
        }
        V( pd3dDevice->CreateBlendState(&blendState, &g_pNoBlendBS) );*/
        g_pNoBlendBS = ()->
        {
            gl.glDisable(GLenum.GL_BLEND);
        };

        /*for (int i = 0; i < 8; ++i)
        {
            blendState.RenderTarget[i].BlendEnable = TRUE;
            blendState.RenderTarget[i].BlendOp = D3D11_BLEND_OP_MAX;
            blendState.RenderTarget[i].BlendOpAlpha = D3D11_BLEND_OP_MAX;
            blendState.RenderTarget[i].DestBlend = D3D11_BLEND_ONE;
            blendState.RenderTarget[i].DestBlendAlpha = D3D11_BLEND_ONE;
            blendState.RenderTarget[i].SrcBlend = D3D11_BLEND_ONE;
            blendState.RenderTarget[i].SrcBlendAlpha = D3D11_BLEND_ONE;
            blendState.RenderTarget[i].RenderTargetWriteMask = D3D11_COLOR_WRITE_ENABLE_ALL;
        }
        V( pd3dDevice->CreateBlendState(&blendState, &g_pMaxBlendBS) );*/
        g_pMaxBlendBS = ()->
        {
            gl.glEnable(GLenum.GL_BLEND);
            gl.glBlendFuncSeparate(GLenum.GL_ONE, GLenum.GL_ONE, GLenum.GL_ONE, GLenum.GL_ONE);
            gl.glBlendEquation(GLenum.GL_MAX);
        };

        /*blendState.IndependentBlendEnable = true;
        for (int i = 0; i < 3; ++i)
        {
            blendState.RenderTarget[i].BlendEnable = FALSE;
            blendState.RenderTarget[i].RenderTargetWriteMask = D3D11_COLOR_WRITE_ENABLE_ALL;
        }
        for (int i = 3; i < 6; ++i)
        {
            blendState.RenderTarget[i].BlendEnable = TRUE;
            blendState.RenderTarget[i].BlendOp = D3D11_BLEND_OP_ADD;
            blendState.RenderTarget[i].BlendOpAlpha = D3D11_BLEND_OP_ADD;
            blendState.RenderTarget[i].DestBlend = D3D11_BLEND_ONE;
            blendState.RenderTarget[i].DestBlendAlpha = D3D11_BLEND_ONE;
            blendState.RenderTarget[i].SrcBlend = D3D11_BLEND_ONE;
            blendState.RenderTarget[i].SrcBlendAlpha = D3D11_BLEND_ONE;
            blendState.RenderTarget[i].RenderTargetWriteMask = D3D11_COLOR_WRITE_ENABLE_ALL;
        }
        V( pd3dDevice->CreateBlendState(&blendState, &g_pNoBlend1AddBlend2BS) );*/
        g_pNoBlend1AddBlend2BS = ()->
        {
            for (int i = 0; i < 3; ++i){
                gl.glDisablei(GLenum.GL_BLEND, i);
            }

            for (int i = 3; i < 6; ++i){
                gl.glEnable(GLenum.GL_BLEND);
                gl.glBlendEquation(GLenum.GL_FUNC_ADD);
                gl.glBlendFuncSeparate(GLenum.GL_ONE, GLenum.GL_ONE, GLenum.GL_ONE, GLenum.GL_ONE);
            }
        };

        /*D3D11_DEPTH_STENCIL_DESC depthState;
        depthState.DepthEnable = TRUE;
        depthState.StencilEnable = FALSE;
        depthState.DepthFunc = D3D11_COMPARISON_LESS_EQUAL;
        depthState.DepthWriteMask = D3D11_DEPTH_WRITE_MASK_ALL;
        V(pd3dDevice->CreateDepthStencilState(&depthState,&g_normalDepthStencilState));*/
        g_normalDepthStencilState = ()->
        {
            gl.glEnable(GLenum.GL_DEPTH_TEST);
            gl.glDisable(GLenum.GL_STENCIL_TEST);
            gl.glDepthFunc(GLenum.GL_LEQUAL);
            gl.glDepthMask(true);
        };
        /*depthState.DepthFunc = D3D11_COMPARISON_LESS;
        V(pd3dDevice->CreateDepthStencilState(&depthState,&depthStencilStateComparisonLess));*/
        depthStencilStateComparisonLess = ()->
        {
            gl.glEnable(GLenum.GL_DEPTH_TEST);
            gl.glDisable(GLenum.GL_STENCIL_TEST);
            gl.glDepthFunc(GLenum.GL_LESS);
            gl.glDepthMask(true);
        };

        /*depthState.DepthEnable = FALSE;
        V(pd3dDevice->CreateDepthStencilState(&depthState,&g_depthStencilStateDisableDepth));*/
        g_depthStencilStateDisableDepth = ()->
        {
            gl.glDisable(GLenum.GL_DEPTH_TEST);
        };

        changeSMTaps(/*pd3dImmediateContext*/);
    }

    @Override
    protected void update(float dt) {
        updateProjectionMatrices();
    }



    // Poisson disk generated with http://www.coderhaus.com/?p=11
    void changeSMTaps(/*ID3D11DeviceContext* pd3dContext*/)
    {
        if(g_smTaps!=1 && g_smTaps!=8 && g_smTaps!=16 && g_smTaps!=28) g_smTaps = 1;

        Vector4f PoissonDisk1[/*1*/] = {new Vector4f(0.0f, 0.0f, 0, 0)};

        Vector4f PoissonDisk8[/*8*/] =
        {
                new Vector4f(0.02902336f, -0.762744f,0,0),
                new Vector4f(-0.4718729f, -0.09262539f,0,0),
                new Vector4f(0.1000665f, -0.09762577f,0,0),
                new Vector4f(0.2378338f, 0.4170297f,0,0),
                new Vector4f(0.9537742f, 0.1807702f,0,0),
                new Vector4f(0.6016041f, -0.4252017f,0,0),
                new Vector4f(-0.741717f, -0.5353929f,0,0),
                new Vector4f(-0.1786781f, 0.8091267f,0,0)
        };

        Vector4f PoissonDisk16[/*16*/] =
        {
                new Vector4f(0.1904656f, -0.6218426f,0,0),
                new Vector4f(-0.1258488f, -0.9434036f,0,0),
                new Vector4f(0.5911888f, -0.345617f,0,0),
                new Vector4f(0.1664507f, -0.04516677f,0,0),
                new Vector4f(-0.1608483f, -0.3104914f,0,0),
                new Vector4f(-0.5286239f, -0.6659128f,0,0),
                new Vector4f(-0.3251964f, 0.05574534f,0,0),
                new Vector4f(0.7012196f, 0.05406655f,0,0),
                new Vector4f(0.3361487f, 0.4192253f,0,0),
                new Vector4f(0.7241808f, 0.5223625f,0,0),
                new Vector4f(-0.599312f, 0.6524374f,0,0),
                new Vector4f(-0.8909158f, -0.3729527f,0,0),
                new Vector4f(-0.2111304f, 0.4643686f,0,0),
                new Vector4f(0.1620989f, 0.9808305f,0,0),
                new Vector4f(-0.8806558f, 0.09435279f,0,0),
                new Vector4f(-0.2311532f, 0.8682256f,0,0)
        };

        Vector4f PoissonDisk24[/*24*/] =
        {
                new Vector4f(0.3818467f, 0.5925183f,0,0),
                new Vector4f(0.1798417f, 0.8695328f,0,0),
                new Vector4f(0.09424125f, 0.3906686f,0,0),
                new Vector4f(0.1988628f, 0.05610655f,0,0),
                new Vector4f(0.7975256f, 0.6026196f,0,0),
                new Vector4f(0.7692417f, 0.1346178f,0,0),
                new Vector4f(-0.3684688f, 0.5602454f,0,0),
                new Vector4f(-0.1773221f, 0.1597976f,0,0),
                new Vector4f(-0.1607566f, 0.8796939f,0,0),
                new Vector4f(-0.766114f, 0.4488805f,0,0),
                new Vector4f(-0.601667f, 0.7814722f,0,0),
                new Vector4f(-0.506153f, 0.1493255f,0,0),
                new Vector4f(-0.8958725f, -0.01973226f,0,0),
                new Vector4f(0.8752386f, -0.4413323f,0,0),
                new Vector4f(0.5006013f, -0.07411311f,0,0),
                new Vector4f(0.4929055f, -0.4686971f,0,0),
                new Vector4f(-0.05599103f, -0.2501699f,0,0),
                new Vector4f(-0.5142418f, -0.3453796f,0,0),
                new Vector4f(-0.493443f, -0.762339f,0,0),
                new Vector4f(-0.2623769f, -0.5478004f,0,0),
                new Vector4f(0.1288256f, -0.5584031f,0,0),
                new Vector4f(-0.8512651f, -0.4920075f,0,0),
                new Vector4f(-0.1360606f, -0.9041532f,0,0),
                new Vector4f(0.3511299f, -0.8271493f,0,0)
        };

        Vector4f PoissonDisk28[/*28*/] =
        {
                new Vector4f(-0.6905488f, 0.09492259f,0,0),
                new Vector4f(-0.7239041f, -0.3711901f,0,0),
                new Vector4f(-0.1990684f, -0.1351167f,0,0),
                new Vector4f(-0.8588699f, 0.4396836f,0,0),
                new Vector4f(-0.4826424f, 0.320396f,0,0),
                new Vector4f(-0.9968387f, 0.01040132f,0,0),
                new Vector4f(-0.5230064f, -0.596889f,0,0),
                new Vector4f(-0.2146133f, -0.6254999f,0,0),
                new Vector4f(-0.6389362f, 0.7377159f,0,0),
                new Vector4f(-0.1776157f, 0.6040277f,0,0),
                new Vector4f(-0.01479932f, 0.2212604f,0,0),
                new Vector4f(-0.3635045f, -0.8955025f,0,0),
                new Vector4f(0.3450507f, -0.7505886f,0,0),
                new Vector4f(0.1438699f, -0.1978877f,0,0),
                new Vector4f(0.06733564f, -0.9922826f,0,0),
                new Vector4f(0.1302602f, 0.758476f,0,0),
                new Vector4f(-0.3056195f, 0.9038011f,0,0),
                new Vector4f(0.387158f, 0.5397643f,0,0),
                new Vector4f(0.1010145f, -0.5530168f,0,0),
                new Vector4f(0.6531418f, 0.08325134f,0,0),
                new Vector4f(0.3876107f, -0.4529504f,0,0),
                new Vector4f(0.7198777f, -0.3464415f,0,0),
                new Vector4f(0.9582281f, -0.1639438f,0,0),
                new Vector4f(0.6608706f, -0.7009276f,0,0),
                new Vector4f(0.2853746f, 0.1097673f,0,0),
                new Vector4f(0.715556f, 0.3905755f,0,0),
                new Vector4f(0.6451758f, 0.7568412f,0,0),
                new Vector4f(0.4597791f, -0.1513058f,0,0)
        };

        /*D3D11_MAPPED_SUBRESOURCE MappedResource;
        pd3dContext->Map( g_pcbPSSMTapLocations, 0, D3D11_MAP_WRITE_DISCARD, 0, &MappedResource );
        CB_SM_TAP_LOCS* pSMTaps = ( CB_SM_TAP_LOCS* )MappedResource.pData;
        for(int i=0;i<MAX_P_SAMPLES;++i) pSMTaps->samples[i] = D3DXVECTOR4(1.f, 1.f, 1.f, 1.f);
        if(g_smTaps==1) memcpy(pSMTaps->samples,PoissonDisk1,g_smTaps*sizeof(D3DXVECTOR4));
        else if(g_smTaps==8) memcpy(pSMTaps->samples,PoissonDisk8,g_smTaps*sizeof(D3DXVECTOR4));
        else if(g_smTaps==16) memcpy(pSMTaps->samples,PoissonDisk16,g_smTaps*sizeof(D3DXVECTOR4));
        else if(g_smTaps==28) memcpy(pSMTaps->samples,PoissonDisk28,g_smTaps*sizeof(D3DXVECTOR4));
        pSMTaps->numTaps = g_smTaps;
        pSMTaps->filterSize = g_smFilterSize;
        pd3dContext->Unmap( g_pcbPSSMTapLocations, 0 );
        pd3dContext->PSSetConstantBuffers( 9, 1, &g_pcbPSSMTapLocations );*/

        ByteBuffer bytes = CacheBuffer.getCachedByteBuffer(CB_SM_TAP_LOCS.SIZE);
        bytes.putInt(g_smTaps);
        bytes.putFloat(g_smFilterSize);
        bytes.position(bytes.position() + 8); // skip the padding.
        if(g_smTaps == 1){
            CacheBuffer.put(bytes, PoissonDisk1);
        }else if(g_smTaps == 8){
            CacheBuffer.put(bytes, PoissonDisk8);
        }else if(g_smTaps == 16){
            CacheBuffer.put(bytes, PoissonDisk16);
        }else if(g_smTaps == 28){
            CacheBuffer.put(bytes, PoissonDisk28);
        }

        bytes.position(CB_SM_TAP_LOCS.SIZE).flip();
        g_pcbPSSMTapLocations.update(0, bytes);
        gl.glBindBufferBase(GLenum.GL_UNIFORM_BUFFER, 9, g_pcbPSSMTapLocations.getBuffer());
    }

    void renderShadowMap(/*ID3D11DeviceContext* pd3dContext,*/ DepthRT pShadowMapDS, Matrix4f projectionMatrix, Matrix4f viewMatrix,
                         int numMeshes, RenderMesh[] meshes, Vector4i shadowViewport, ReadableVector3f lightPos,  float lightRadius, float depthBiasFromGUI)
    {
        /*ID3D11RenderTargetView* pRTVNULL[1] = { NULL };
        pd3dContext->ClearDepthStencilView( *pShadowMapDS, D3D11_CLEAR_DEPTH, 1.0, 0 );
        pd3dContext->OMSetRenderTargets( 0, pRTVNULL, *pShadowMapDS );
        pd3dContext->RSSetViewports(1, &shadowViewport);*/
        g_RenderTargets.bind();
        g_RenderTargets.setRenderTexture(pShadowMapDS.pDSV, null);
        gl.glViewport(shadowViewport.x, shadowViewport.y, shadowViewport.z, shadowViewport.w);

        /*ID3D11PixelShader* NULLPS = NULL;
        pd3dContext->VSSetShader( g_pVSSM, NULL, 0 );
        pd3dContext->PSSetShader( NULLPS, NULL, 0 );*/
        g_Program.enable();
        g_Program.setVS(g_pVSSM);
        g_Program.setPS(g_pSimplePS);

        final Matrix4f ViewProjClip2TexLight = CacheBuffer.getCachedMatrix();
        final Matrix4f WVPMatrixLight = CacheBuffer.getCachedMatrix();
        final Matrix4f WVMatrixITLight = CacheBuffer.getCachedMatrix();
        final Matrix4f WVMatrix = CacheBuffer.getCachedMatrix();
        final Vector3f max = CacheBuffer.getCachedVec3();

        //render the meshes
        for(int i=0; i<numMeshes; i++)
        {
            RenderMesh mesh = meshes[i];
//            D3DXMATRIX ViewProjClip2TexLight, WVPMatrixLight, WVMatrixITLight, WVMatrix;
            mesh.createMatrices( g_pSceneShadowMapProj, viewMatrix, WVMatrix, WVMatrixITLight, WVPMatrixLight, ViewProjClip2TexLight );

            UpdateSceneCB( /*pd3dContext,*/ lightPos, lightRadius, depthBiasFromGUI, false, WVPMatrixLight, WVMatrixITLight, (mesh.m_WMatrix), (ViewProjClip2TexLight) );

            if(g_subsetToRender==-1) {
                max.set(100000, 100000, 100000);
                mesh.m_Mesh.RenderBounded( /*pd3dContext,*/ Vector3f.ZERO, max, 0);
            }else {
                max.set(10000, 10000, 10000);
                mesh.m_Mesh.RenderSubsetBounded(0, g_subsetToRender, /*pd3dContext,*/ Vector3f.ZERO, max, false, 0);
            }
        }

        CacheBuffer.free(ViewProjClip2TexLight);
        CacheBuffer.free(WVPMatrixLight);
        CacheBuffer.free(WVMatrixITLight);
        CacheBuffer.free(WVMatrix);
        CacheBuffer.free(max);
    }

    //initialize all the shadowmap buffers and variables
    void initializeReflectiveShadowMaps(/*ID3D11Device* pd3dDevice*/)
    {
        final int DXGI_FORMAT_R8G8B8A8_UNORM = GLenum.GL_RGBA8;
        g_pRSMColorRT = new SimpleRT();
        g_pRSMColorRT.Create2D( /*pd3dDevice,*/ Defines.RSM_RES, Defines.RSM_RES, 1, 1, 1, DXGI_FORMAT_R8G8B8A8_UNORM );

        g_pRSMAlbedoRT = new SimpleRT();
        g_pRSMAlbedoRT.Create2D( /*pd3dDevice,*/ Defines.RSM_RES, Defines.RSM_RES, 1, 1, 1, DXGI_FORMAT_R8G8B8A8_UNORM );

        g_pRSMNormalRT = new SimpleRT();
        g_pRSMNormalRT.Create2D( /*pd3dDevice,*/ Defines.RSM_RES, Defines.RSM_RES, 1, 1, 1, DXGI_FORMAT_R8G8B8A8_UNORM );

        /*D3D11_TEXTURE2D_DESC texDesc;
        texDesc.ArraySize          = 1;
        texDesc.BindFlags          = D3D11_BIND_DEPTH_STENCIL | D3D11_BIND_SHADER_RESOURCE;
        texDesc.CPUAccessFlags     = NULL;
        texDesc.Format             = DXGI_FORMAT_R32_TYPELESS;
        texDesc.Width              = RSM_RES;
        texDesc.Height             = RSM_RES;
        texDesc.MipLevels          = 1;
        texDesc.MiscFlags          = NULL;
        texDesc.SampleDesc.Count   = 1;
        texDesc.SampleDesc.Quality = 0;
        texDesc.Usage              = D3D11_USAGE_DEFAULT;*/
        Texture2DDesc texDesc = new Texture2DDesc(Defines.RSM_RES, Defines.RSM_RES,GLenum.GL_DEPTH_COMPONENT32F);
        g_pShadowMapDS = new DepthRT( /*pd3dDevice,*/ texDesc );
        g_pDepthPeelingDS[0] = new DepthRT( /*pd3dDevice,*/ texDesc );
        g_pDepthPeelingDS[1] = new DepthRT( /*pd3dDevice,*/ texDesc );
    }

    void createPropagationAndGeometryVolumes(/*ID3D11Device* pd3dDevice*/)
    {
        if(g_propType == PROP_TYPE.HIERARCHY)
        {
            LPV0Propagate = new LPV_RGB_Hierarchy(/*pd3dDevice*/);
            LPV0Accumulate = new LPV_RGB_Hierarchy(/*pd3dDevice*/);
            GV0 = new LPV_Hierarchy(/*pd3dDevice*/);
            GV0Color = new LPV_Hierarchy(/*pd3dDevice*/);
        }
        else
        {
            LPV0Propagate = new LPV_RGB_Cascade(/*pd3dDevice,*/ g_cascadeScale, g_cascadeTranslate );
            LPV0Accumulate = new LPV_RGB_Cascade(/*pd3dDevice,*/ g_cascadeScale, g_cascadeTranslate );
            GV0 = new LPV_Cascade(/*pd3dDevice,*/ g_cascadeScale, g_cascadeTranslate );
            GV0Color = new LPV_Cascade(/*pd3dDevice,*/ g_cascadeScale, g_cascadeTranslate );
        }

        final int DXGI_FORMAT_R16G16B16A16_FLOAT = GLenum.GL_RGBA16F;
        final int DXGI_FORMAT_R8G8B8A8_UNORM = GLenum.GL_RGBA8;
        LPV0Propagate.Create( g_numHierarchyLevels, /*pd3dDevice,*/ Defines.g_LPVWIDTH, Defines.g_LPVHEIGHT, Defines.g_LPVWIDTH, Defines.g_LPVHEIGHT, Defines.g_LPVDEPTH,
                DXGI_FORMAT_R16G16B16A16_FLOAT , true, true, false, true,1 );

//#ifndef USE_SINGLE_CHANNELS
        LPV0Accumulate.Create( g_numHierarchyLevels, /*pd3dDevice,*/ Defines.g_LPVWIDTH, Defines.g_LPVHEIGHT, Defines.g_LPVWIDTH, Defines.g_LPVHEIGHT, Defines.g_LPVDEPTH,
                DXGI_FORMAT_R16G16B16A16_FLOAT, true, true, false, true, 1);
/*#else
        //reading from a float16 uav is not allowed in d3d11!
        LPV0Accumulate->Create( g_numHierarchyLevels, pd3dDevice, g_LPVWIDTH, g_LPVHEIGHT, g_LPVWIDTH, g_LPVHEIGHT, g_LPVDEPTH, DXGI_FORMAT_R32_FLOAT, true, false, false, true, 4 );
#endif*/

        GV0.Create2DArray(g_numHierarchyLevels, /*pd3dDevice,*/ Defines.g_LPVWIDTH, Defines.g_LPVHEIGHT, Defines.g_LPVDEPTH, DXGI_FORMAT_R16G16B16A16_FLOAT , true );
        GV0Color.Create2DArray(g_numHierarchyLevels, /*pd3dDevice,*/ Defines.g_LPVWIDTH, Defines.g_LPVHEIGHT, Defines.g_LPVDEPTH, DXGI_FORMAT_R8G8B8A8_UNORM , true );

    }

    void incrementPresetCamera()
    {
        if(g_animateLight)
        {

            g_lightPreset = (g_lightPreset+1);
            g_lightPreset = g_lightPreset%NUM_LIGHT_PRESETS;
            g_lightPosDest = g_LightPresetPos[g_lightPreset];
        }
    }

    static float speedDelta = 0.25f;
    static void stepVelocity(Vector3f vel, ReadableVector3f currPos, ReadableVector3f destPos)
    {
        /*D3DXVECTOR3 deltaVel = destPos- currPos;
        const float dist = D3DXVec3Length(&deltaVel);
        D3DXVec3Normalize(&deltaVel, &deltaVel);
        deltaVel *= speedDelta;
        vel = deltaVel;*/

        Vector3f.sub(destPos, currPos, vel);
        vel.normalise();
        vel.scale(speedDelta);
    }

    void updateLight(float dtime)
    {
        if(g_animateLight)
        {
            Vector3f lightPosVel = new Vector3f(0,0,0);
            stepVelocity(lightPosVel, g_lightPos, g_lightPosDest);
//            g_lightPos   += lightPosVel*dtime;
            Vector3f.linear(g_lightPos, lightPosVel, dtime, g_lightPos);
//            g_LightCamera.SetViewParams(g_lightPos,g_center);
            g_LightCamera.setViewAndUpdateCamera(g_lightPos, g_center, Vector3f.Y_AXIS);

            /*D3DXVECTOR3 dist = D3DXVECTOR3(g_lightPosDest-g_lightPos);
            if(D3DXVec3Length(&dist)<0.1)*/
            if(Vector3f.distance(g_lightPosDest, g_lightPos) < 0.1f)
                incrementPresetCamera();
        }
    }

    ////////////////////////////////////////////////////////////////////////////////
//! Updating constant buffers
////////////////////////////////////////////////////////////////////////////////

    private final CB_VS_PER_OBJECT m_VS_Per_Object = new CB_VS_PER_OBJECT();
    private final CB_VS_GLOBAL m_VS_Global = new CB_VS_GLOBAL();
    void UpdateSceneCB( /*ID3D11DeviceContext* pd3dContext,*/ ReadableVector3f lightPos, float lightRadius, float depthBias,
                        boolean bUseSM, Matrix4f WVPMatrix, Matrix4f WVMatrixIT, Matrix4f WMatrix, Matrix4f lightViewProjClip2Tex)
    {
        /*HRESULT hr;
        D3D11_MAPPED_SUBRESOURCE MappedResource;
        V( pd3dContext->Map( g_pcbVSPerObject, 0, D3D11_MAP_WRITE_DISCARD, 0, &MappedResource ) );
        CB_VS_PER_OBJECT* pVSPerObject = ( CB_VS_PER_OBJECT* )MappedResource.pData;
        D3DXMatrixTranspose( &pVSPerObject->m_WorldViewProj, WVPMatrix );
        D3DXMatrixTranspose( &pVSPerObject->m_WorldViewIT, WVMatrixIT );
        D3DXMatrixTranspose( &pVSPerObject->m_World,WMatrix);
        if(lightViewProjClip2Tex)
            D3DXMatrixTranspose( &pVSPerObject->m_LightViewProjClip2Tex,lightViewProjClip2Tex);
        pd3dContext->Unmap( g_pcbVSPerObject, 0 );
        pd3dContext->VSSetConstantBuffers( 0, 1, &g_pcbVSPerObject );*/
        CB_VS_PER_OBJECT pVSPerObject = m_VS_Per_Object;
        pVSPerObject.m_WorldViewProj.load(WVPMatrix);
        pVSPerObject.m_WorldViewIT.load(WVMatrixIT);
        pVSPerObject.m_World.load(WMatrix);
        if(lightViewProjClip2Tex != null){
            pVSPerObject.m_World.load(lightViewProjClip2Tex);
        }

        ByteBuffer buffer = CacheBuffer.wrap(CB_VS_PER_OBJECT.SIZE, pVSPerObject);
        g_pcbVSPerObject.update(0, buffer);
        g_pcbVSPerObject.unbind();
        gl.glBindBufferBase(g_pcbVSPerObject.getTarget(), 0, g_pcbVSPerObject.getBuffer());


        /*V( pd3dContext->Map( g_pcbVSGlobal, 0, D3D11_MAP_WRITE_DISCARD, 0, &MappedResource ) );
        CB_VS_GLOBAL* pVSGlobal = ( CB_VS_GLOBAL* )MappedResource.pData;

        pVSGlobal->g_lightWorldPos = D3DXVECTOR4(lightPos.x,lightPos.y,lightPos.z,1.f / max(0.000001f, lightRadius*lightRadius));
        pVSGlobal->g_depthBiasFromGUI = depthBias;
        pVSGlobal->bUseSM = bUseSM;
        pVSGlobal->g_minCascadeMethod = g_propType==CASCADE ? 1 : 0;
        pVSGlobal->g_numCascadeLevels = g_numHierarchyLevels;

        pd3dContext->Unmap( g_pcbVSGlobal, 0 );
        pd3dContext->VSSetConstantBuffers( 1, 1, &g_pcbVSGlobal );
        pd3dContext->PSSetConstantBuffers( 1, 1, &g_pcbVSGlobal );*/
        CB_VS_GLOBAL pVSGlobal = m_VS_Global;
        pVSGlobal.g_lightWorldPos.set(lightPos.getX(),lightPos.getY(),lightPos.getZ(),1.f / Math.max(0.000001f, lightRadius*lightRadius));
        pVSGlobal.g_depthBiasFromGUI = depthBias;
        pVSGlobal.bUseSM = bUseSM ? 1: 0;
        pVSGlobal.g_minCascadeMethod = g_propType==PROP_TYPE.CASCADE ? 1 : 0;
        pVSGlobal.g_numCascadeLevels = g_numHierarchyLevels;


//        return hr;
    }

    void setLPVScale()
    {
        if(g_propType == PROP_TYPE.CASCADE)
            g_LPVscale = 12.f;
        else
            g_LPVscale = 31.0f;

//    if(g_HUD.GetSlider( IDC_LPVSCALE_SCALE))
//    {
//        WCHAR sz[MAX_PATH];
//        g_HUD.GetSlider( IDC_LPVSCALE_SCALE)->SetValue( (int)g_LPVscale*10 );
//        StringCchPrintf( sz, 100, L"LPV scale: %0.1f", g_LPVscale );
//        g_HUD.GetStatic( IDC_LPVSCALE_STATIC )->SetText( sz );
//    }
    }

    void setFluxAmplifier()
    {
        if(g_propType == PROP_TYPE.CASCADE)
            g_fluxAmplifier = 3.8f;
        else
            g_fluxAmplifier = 3.0f;

        /*if(g_HUD.GetSlider( IDC_FLUX_AMP_SCALE))
        {
            WCHAR sz[MAX_PATH];
            g_HUD.GetSlider( IDC_FLUX_AMP_SCALE)->SetValue((int)(g_fluxAmplifier*100) );
            StringCchPrintf( sz, 100, L"Flux Amplifier: %0.3f", g_fluxAmplifier );
            g_HUD.GetStatic( IDC_FLUX_AMP_STATIC)->SetText(sz);
        }*/
    }

    void setNumIterations()
    {
        if(g_propType == PROP_TYPE.CASCADE)
            g_numPropagationStepsLPV = 8;
        else
            g_numPropagationStepsLPV = 9;

        /*if(g_HUD.GetSlider( IDC_PROPAGATEITERATIONS_SCALE))
        {
            WCHAR sz[MAX_PATH];
            g_HUD.GetSlider( IDC_PROPAGATEITERATIONS_SCALE)->SetValue( g_numPropagationStepsLPV );
            StringCchPrintf( sz, 100, L"LPV iterations: %d", g_numPropagationStepsLPV );
            g_HUD.GetStatic( IDC_PROPAGATEITERATIONS_STATIC)->SetText(sz);
        }*/
    }

    void setLightAndCamera()
    {
        g_vecEye.set(9.6254f, -3.69081f, 2.0234f);
        g_vecAt.set(8.78361f, -3.68715f, 1.48362f);

//        g_Camera.SetScalers(0.005f,3.f);
        g_lightPos.set(-12.7857f, 22.4795f, 7.65355f);
        g_center.set(0.0f,0.0f,0.0f);
        g_mUp.set(1.0f, 0.0f, 0.0f);

        /*g_LightCamera.SetViewParams(g_lightPos,g_center);
        g_LightCamera.SetScalers(0.001f,0.2f);  TODO
        g_LightCamera.SetModelCenter( D3DXVECTOR3(0.0f,0.0f,0.0f) );

        g_LightCamera.SetEnablePositionMovement(true);
        g_LightCamera.SetButtonMasks(NULL, NULL, MOUSE_LEFT_BUTTON );*/

        /*g_Camera.SetViewParams( &g_vecEye, &g_vecAt );
        g_Camera.SetEnablePositionMovement(true);*/

        g_lightPreset = 0;
        g_lightPosDest = g_lightPos; // TODO
    }

    private final CB_SIMPLE_OBJECTS pPSSimple = new CB_SIMPLE_OBJECTS();
    private final CB_RENDER pcbRender = new CB_RENDER();
    void DrawScene(/*ID3D11Device* pd3dDevice,  ID3D11DeviceContext* pd3dContext*/ ){
        /*pd3dContext->OMSetDepthStencilState(g_normalDepthStencilState, 0);*/
        g_normalDepthStencilState.run();

//        D3D11_MAPPED_SUBRESOURCE MappedResource;

        if(bPropTypeChanged)
        {
            CommonUtil.safeRelease(LPV0Propagate);
            CommonUtil.safeRelease(LPV0Accumulate);
            CommonUtil.safeRelease(GV0);
            CommonUtil.safeRelease(GV0Color);
            createPropagationAndGeometryVolumes(/*pd3dDevice*/);
            if(g_propType == PROP_TYPE.CASCADE) g_LPVLevelToInitialize = g_PropLevel;
            else g_LPVLevelToInitialize = Defines.HIERARCHICAL_INIT_LEVEL;
            bPropTypeChanged = false;

            setLPVScale();
            setFluxAmplifier();
            setNumIterations();
        }

        if(g_numTapsChanged) changeSMTaps(/*pd3dContext*/);

        updateLight(/*(1.0f / DXUTGetFPS())*/ getFrameDeltaTime());

        /*ID3D11RenderTargetView* pRTV = DXUTGetD3D11RenderTargetView();
        ID3D11DepthStencilView* pDSV = DXUTGetD3D11DepthStencilView();
        float ClearColorRTV[4] = {0.3f, 0.3f, 0.3f, 1.0f};
        pd3dContext->ClearRenderTargetView( pRTV, ClearColorRTV );
        float ClearColor[4] = {0.0f, 0.0f, 0.0f, 1.0f};
        float ClearColor2[4] = {0.0f, 0.0f, 0.0f, 0.0f};*/
        gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, 0);
        gl.glClearColor(0.3f, 0.3f, 0.3f, 1.0f);
        gl.glClear(GLenum.GL_COLOR_BUFFER_BIT);


        Matrix4f p_mview = g_Camera.getViewMatrix();
        Matrix4f p_cameraViewMatrix = p_mview;

        Matrix4f p_cameraProjectionMatrix = g_Camera.getProjMatrix();
//        D3DXMATRIX VPMatrix = (*p_cameraViewMatrix) * (*p_cameraProjectionMatrix);
        Matrix4f VPMatrix = g_Camera.getViewProjMatrix();

        //set up the shadow matrix for the light
        Matrix4f mShadowMatrix = mShadowMatrix = g_LightCamera.getViewMatrix();

        g_MainMovableMesh.setWorldMatrixTranslate(g_objectTranslate.x,g_objectTranslate.y,g_objectTranslate.z);
        g_MovableBoxMesh.setWorldMatrixTranslate(g_objectTranslate.x,g_objectTranslate.y,g_objectTranslate.z);

        //transforms for the LPV
//        D3DXMATRIX LPVWorldMatrix;

        if(g_resetLPVXform)
        {
            g_camTranslate.set(g_vecEye.x, g_vecEye.y, g_vecEye.z );
            g_camViewVector.set(1, 0, 0);
            g_resetLPVXform = false;
        }
        if(g_movableLPV)
        {
            /*D3DXMATRIX viewInverse;
            D3DXMatrixInverse(&viewInverse, NULL, &*p_cameraViewMatrix);
            g_camViewVector = D3DXVECTOR3(p_cameraViewMatrix->_13, p_cameraViewMatrix->_23, p_cameraViewMatrix->_33);
            g_camTranslate = D3DXVECTOR3(viewInverse._41, viewInverse._42, viewInverse._43 );*/
            g_camViewVector.set(g_Camera.getLookAt());
            g_camTranslate.set(g_Camera.getPosition());
        }

        LPV0Propagate.setLPVTransformsRotatedAndOffset (g_LPVscale, g_camTranslate, p_cameraViewMatrix, g_camViewVector);
        LPV0Accumulate.setLPVTransformsRotatedAndOffset(g_LPVscale, g_camTranslate, p_cameraViewMatrix, g_camViewVector);
        GV0.setLPVTransformsRotatedAndOffset           (g_LPVscale, g_camTranslate, p_cameraViewMatrix, g_camViewVector);
        GV0Color.setLPVTransformsRotatedAndOffset      (g_LPVscale, g_camTranslate, p_cameraViewMatrix, g_camViewVector);

        boolean useFloats = false;
        boolean useFloat4s = false;
        if(LPV0Accumulate.getRed(0).getNumChannels()==1 && LPV0Accumulate.getRed(0).getNumRTs()==4) useFloats = true;
        if(LPV0Accumulate.getRed(0).getNumChannels()>1 && LPV0Accumulate.getRed(0).getNumRTs()==1) useFloat4s = true;

        /*pd3dContext->Map( g_pcbRender, 0, D3D11_MAP_WRITE_DISCARD, 0, &MappedResource );
        CB_RENDER* pcbRender = ( CB_RENDER* )MappedResource.pData;*/
        pcbRender.diffuseScale = g_diffuseInterreflectionScale;
        pcbRender.useDiffuseInterreflection = g_useDiffuseInterreflection?1:0;
        pcbRender.directLight = g_directLight;
        pcbRender.ambientLight = g_ambientLight;
        pcbRender.useFloat4s = useFloat4s?1:0;
        pcbRender.useFloats = useFloats?1:0;
        pcbRender.invSMSize = 1.0f/Defines.RSM_RES;
        pcbRender.normalMapMultiplier = g_normalMapMultiplier;
        pcbRender.useDirectionalDerivativeClamping = g_useDirectionalDerivativeClamping?1:0;
        pcbRender.directionalDampingAmount = g_directionalDampingAmount;
        if(g_propType ==PROP_TYPE.HIERARCHY ||  g_bUseSingleLPV)
        {
            /*D3DXMatrixTranspose( &pcbRender->worldToLPVNormTex,&(LPV0Propagate->m_collection[g_PropLevel]->getWorldToLPVNormTex()));
            D3DXMatrixTranspose( &pcbRender->worldToLPVNormTexRender,&(LPV0Propagate->m_collection[g_PropLevel]->getWorldToLPVNormTexRender()));*/
            pcbRender.worldToLPVNormTex.load(LPV0Propagate.get(g_PropLevel).getWorldToLPVNormTex());
            pcbRender.worldToLPVNormTexRender.load(LPV0Propagate.get(g_PropLevel).getWorldToLPVNormTexRender());
        }
        else
        {
            /*D3DXMatrixTranspose( &pcbRender->worldToLPVNormTex,&(LPV0Propagate->m_collection[0]->getWorldToLPVNormTex()));
            D3DXMatrixTranspose( &pcbRender->worldToLPVNormTexRender,&(LPV0Propagate->m_collection[0]->getWorldToLPVNormTexRender()));*/
            pcbRender.worldToLPVNormTex.load(LPV0Propagate.get(0).getWorldToLPVNormTex());
            pcbRender.worldToLPVNormTexRender.load(LPV0Propagate.get(0).getWorldToLPVNormTexRender());

            if(LPV0Propagate.getNumLevels()>1)
            {
                /*D3DXMatrixTranspose( &pcbRender->worldToLPVNormTex1,&(LPV0Propagate->m_collection[1]->getWorldToLPVNormTex()));
                D3DXMatrixTranspose( &pcbRender->worldToLPVNormTexRender1,&(LPV0Propagate->m_collection[1]->getWorldToLPVNormTexRender()));*/

                pcbRender.worldToLPVNormTex1.load(LPV0Propagate.get(1).getWorldToLPVNormTex());
                pcbRender.worldToLPVNormTexRender1.load(LPV0Propagate.get(1).getWorldToLPVNormTexRender());
            }
            if(LPV0Propagate.getNumLevels()>2)
            {
                /*D3DXMatrixTranspose( &pcbRender->worldToLPVNormTex2,&(LPV0Propagate->m_collection[2]->getWorldToLPVNormTex()));
                D3DXMatrixTranspose( &pcbRender->worldToLPVNormTexRender2,&(LPV0Propagate->m_collection[2]->getWorldToLPVNormTexRender()));*/

                pcbRender.worldToLPVNormTex2.load(LPV0Propagate.get(2).getWorldToLPVNormTex());
                pcbRender.worldToLPVNormTexRender2.load(LPV0Propagate.get(2).getWorldToLPVNormTexRender());
            }
        }

//        pd3dContext->Unmap( g_pcbRender, 0 );
        ByteBuffer bytes = CacheBuffer.wrap(CB_RENDER.SIZE, pcbRender);
        g_pcbRender.update(0, bytes);


        /*D3D11_VIEWPORT viewport;
        UINT nViewports = 1;
        pd3dContext->RSGetViewports(&nViewports, &viewport);
        // IA setup
        UINT Strides[1];
        UINT Offsets[1];
        ID3D11Buffer* pVB[1];
        pVB[0] = g_MainMesh->m_Mesh.GetVB11( 0, 0 );
        Strides[0] = ( UINT )g_MainMesh->m_Mesh.GetVertexStride( 0, 0 );
        Offsets[0] = 0;
        pd3dContext->IASetVertexBuffers( 0, 1, pVB, Strides, Offsets );
        pd3dContext->IASetIndexBuffer( g_MainMesh->m_Mesh.GetIB11( 0 ), g_MainMesh->m_Mesh.GetIBFormat11( 0 ), 0 );
        pd3dContext->IASetInputLayout( g_pMeshLayout );*/
        int VB = g_MainMesh.m_Mesh.getVB10(0,0);
        gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, VB);
        g_pMeshLayout.bind();
        int IB = g_MainMesh.m_Mesh.getIB10( 0 );
        gl.glBindBuffer(GLenum.GL_ELEMENT_ARRAY_BUFFER, IB);
        int iB_Format = g_MainMesh.m_Mesh.getIBFormat10(0);


        //clear the LPVs
        float ClearColorLPV[/*4*/] = {0.0f, 0.0f, 0.0f, 0.0f};

        if(g_propType == PROP_TYPE.HIERARCHY)
        {
            LPV0Propagate.clearRenderTargetView(/*pd3dContext,*/ ClearColorLPV, true, g_LPVLevelToInitialize);
            LPV0Propagate.clearRenderTargetView(/*pd3dContext,*/ ClearColorLPV, false, g_LPVLevelToInitialize);
            LPV0Accumulate.clearRenderTargetView(/*pd3dContext,*/ ClearColorLPV, true, g_LPVLevelToInitialize);
            LPV0Accumulate.clearRenderTargetView(/*pd3dContext,*/ ClearColorLPV, false, g_LPVLevelToInitialize);
        }
        else if( g_propType == PROP_TYPE.CASCADE )
        {
            if(g_bUseSingleLPV)
            {
                LPV0Propagate.clearRenderTargetView(/*pd3dContext,*/ ClearColorLPV, true, g_LPVLevelToInitialize);
                LPV0Propagate.clearRenderTargetView(/*pd3dContext,*/ ClearColorLPV, false, g_LPVLevelToInitialize);
                LPV0Accumulate.clearRenderTargetView(/*pd3dContext,*/ ClearColorLPV, true, g_LPVLevelToInitialize);
                LPV0Accumulate.clearRenderTargetView(/*pd3dContext,*/ ClearColorLPV, false, g_LPVLevelToInitialize);
            }
            else
                for(int level=0; level<LPV0Propagate.getNumLevels(); level++)
                {
                    LPV0Propagate.clearRenderTargetView(/*pd3dContext,*/ ClearColorLPV, true, level);
                    LPV0Propagate.clearRenderTargetView(/*pd3dContext,*/ ClearColorLPV, false, level);
                    LPV0Accumulate.clearRenderTargetView(/*pd3dContext,*/ ClearColorLPV, true, level);
                    LPV0Accumulate.clearRenderTargetView(/*pd3dContext,*/ ClearColorLPV, false, level);
                }
        }


        //render the scene to the shadow map/ RSM and initialize the LPV and GV-----------------

        int numMeshes = 2;
        RenderMesh[] meshes = new RenderMesh[2];
        int m = 0;
        if(g_renderMesh)
            meshes[m++] = g_MainMesh;
        if(g_showMovableMesh)
        {
            if(movableMeshType == MOVABLE_MESH_TYPE.TYPE_BOX)
                meshes[m++] = g_MovableBoxMesh;
            else if(movableMeshType == MOVABLE_MESH_TYPE.TYPE_SPHERE)
                meshes[m++] = g_MainMovableMesh;
        }
        numMeshes = m;


        float BlendFactor[/*4*/] = { 1.0f, 1.0f, 1.0f, 1.0f };

        if(g_renderMesh)
            meshes[0] = g_MainMesh;

        ReadableVector3f lightPos = g_LightCamera.getPosition();
        ReadableVector3f lightAt = g_LightCamera.getLookAt(); // TODO
        ReadableVector3f eyePos = g_Camera.getPosition();
        ReadableVector3f eyeAt = g_Camera.getLookAt();  // TODO

        //render the shadow map for the scene.
        renderShadowMap(/*pd3dContext,*/ g_pSceneShadowMap, g_pSceneShadowMapProj, mShadowMatrix, numMeshes, meshes,
                g_shadowViewportScene, lightPos, g_lightRadius, g_depthBiasFromGUI);


        if(g_useDiffuseInterreflection)
        {
            //clear the Geometry Volumes
            /*float ClearColorGV[4] = {0.0f, 0.0f, 0.0f, 0.0f};
            pd3dContext.ClearRenderTargetView( GV0->getRenderTargetView(g_LPVLevelToInitialize), ClearColorGV );
            pd3dContext.ClearRenderTargetView( GV0Color->getRenderTargetView(g_LPVLevelToInitialize), ClearColorGV );*/
            TextureGL tex = GV0.getRenderTargetView(g_LPVLevelToInitialize, 0);
            gl.glClearTexImage(tex.getTexture(), 0, TextureUtils.measureFormat(tex.getFormat()), TextureUtils.measureDataType(tex.getFormat()), null);
            tex = GV0Color.getRenderTargetView(g_LPVLevelToInitialize, 0);
            gl.glClearTexImage(tex.getTexture(), 0, TextureUtils.measureFormat(tex.getFormat()), TextureUtils.measureDataType(tex.getFormat()), null);

            //initialize the LPV and GV with the RSM data:
            if(g_useRSMForLight)
            {
                if(g_propType==PROP_TYPE.CASCADE && g_useRSMCascade && !g_bUseSingleLPV)
                {
                    //render a separate RSM for each level
                    for(int i=0;i<LPV0Accumulate.getNumLevels();i++)
                    {
                        Matrix4f lightViewMatrix = mShadowMatrix;
                        Matrix4f lightProjMatrix = g_pRSMProjMatrices[Math.min(i,SM_PROJ_MATS_SIZE-1)];

                        //render the main RSM
                        renderRSM(/*pd3dContext,*/ false, g_pRSMColorRT, g_pRSMAlbedoRT, g_pRSMNormalRT, g_pShadowMapDS, g_pShadowMapDS, lightProjMatrix, lightViewMatrix, numMeshes, meshes, g_shadowViewport, lightPos, g_lightRadius, g_depthBiasFromGUI, g_bUseSM);
                        //initialize the LPV
                        initializeLPV(/*pd3dContext,*/ LPV0Accumulate.get(i), LPV0Propagate.get(i), mShadowMatrix, lightProjMatrix, g_pRSMColorRT, g_pRSMNormalRT, g_pShadowMapDS, g_fLightFov, 1, g_lightNear, g_lightFar, g_useDirectionalLight);
                        //initialize the GV ( geometry volume) with the RSM data
                        initializeGV(/*pd3dContext,*/ GV0.getRenderTarget(i), GV0Color.getRenderTarget(i), g_pRSMAlbedoRT, g_pRSMNormalRT, g_pShadowMapDS, g_fLightFov, 1, g_lightNear, g_lightFar, g_useDirectionalLight, lightProjMatrix, lightViewMatrix);
                    }
                }
                else if(g_propType==PROP_TYPE.CASCADE && !g_bUseSingleLPV)
                {
                    Matrix4f lightProjMatrix = g_pRSMProjMatrices[0];
                    Matrix4f lightViewMatrix = mShadowMatrix;

                    //render the main RSM
                    renderRSM(/*pd3dContext,*/ false, g_pRSMColorRT, g_pRSMAlbedoRT, g_pRSMNormalRT, g_pShadowMapDS, g_pShadowMapDS, lightProjMatrix, lightViewMatrix, numMeshes, meshes, g_shadowViewport, lightPos, g_lightRadius, g_depthBiasFromGUI, g_bUseSM);

                    for(int i=0;i<LPV0Accumulate.getNumLevels();i++)
                    {
                        //initialize the LPV
                        initializeLPV(/*pd3dContext,*/ LPV0Accumulate.get(i), LPV0Propagate.get(i), lightViewMatrix, lightProjMatrix, g_pRSMColorRT, g_pRSMNormalRT, g_pShadowMapDS, g_fLightFov, 1, g_lightNear, g_lightFar, g_useDirectionalLight);
                        //initialize the GV ( geometry volume) with the RSM data
                        initializeGV(/*pd3dContext,*/ GV0.getRenderTarget(i), GV0Color.getRenderTarget(i), g_pRSMAlbedoRT, g_pRSMNormalRT, g_pShadowMapDS, g_fLightFov, 1, g_lightNear, g_lightFar, g_useDirectionalLight, lightProjMatrix, *lightViewMatrix);
                    }
                }
                else
                {
                    Matrix4f lightViewMatrix = mShadowMatrix;
                    Matrix4f lightProjMatrix;
                    if(g_propType == PROP_TYPE.HIERARCHY) lightProjMatrix = g_pShadowMapProjMatrixSingle;
                    else lightProjMatrix = g_pRSMProjMatrices[Math.min(g_LPVLevelToInitialize,SM_PROJ_MATS_SIZE-1)];

                    //render the main RSM
                    renderRSM(/*pd3dContext,*/ false, g_pRSMColorRT, g_pRSMAlbedoRT, g_pRSMNormalRT, g_pShadowMapDS, g_pShadowMapDS, lightProjMatrix, lightViewMatrix, numMeshes, meshes, g_shadowViewport, lightPos, g_lightRadius, g_depthBiasFromGUI, g_bUseSM);
                    //initialize the LPV
                    initializeLPV(/*pd3dContext,*/ LPV0Accumulate.get(g_LPVLevelToInitialize), LPV0Propagate.get(g_LPVLevelToInitialize), lightViewMatrix, lightProjMatrix, g_pRSMColorRT, g_pRSMNormalRT, g_pShadowMapDS, g_fLightFov, 1, g_lightNear, g_lightFar, g_useDirectionalLight);
                    //initialize the GV ( geometry volume) with the RSM data
                    initializeGV(/*pd3dContext,*/ GV0.getRenderTarget(g_LPVLevelToInitialize), GV0Color.getRenderTarget(g_LPVLevelToInitialize), g_pRSMAlbedoRT, g_pRSMNormalRT, g_pShadowMapDS, g_fLightFov, 1, g_lightNear, g_lightFar, g_useDirectionalLight, lightProjMatrix, lightViewMatrix);
                }
            }


            if(g_renderMesh)
                meshes[0] = g_MainMeshSimplified;


            //note: the depth peeling code is currently only rendering a single RSM, vs a cascade of them.
            //it is also initializing only one level of the LPV.
            //it would be better (but slower) to call initializeGV at all levels if we are using a CASCADE

            //depth peeling from the POV of the light
            if(g_depthPeelFromLight && (g_useOcclusion || g_useMultipleBounces) )
            {

                for(int depthPeelingPass = 0; depthPeelingPass<g_numDepthPeelingPasses; depthPeelingPass++)
                {
                    Matrix4f lightViewMatrix = mShadowMatrix;
                    Matrix4f lightProjMatrix = g_pShadowMapProjMatrixSingle;
//                    pd3dContext->ClearDepthStencilView( *g_pDepthPeelingDS[depthPeelingPass%2], D3D11_CLEAR_DEPTH, 1.0, 0 );
                    tex = g_pDepthPeelingDS[depthPeelingPass%2].pDSV;
                    gl.glClearTexImage(tex.getTexture(), 0, TextureUtils.measureFormat(tex.getFormat()), TextureUtils.measureDataType(tex.getFormat()), CacheBuffer.wrap(1.0f));
                    //render the RSMs
                    renderRSM(/*pd3dContext,*/ depthPeelingPass>0, g_pRSMColorRT, g_pRSMAlbedoRT, g_pRSMNormalRT, g_pDepthPeelingDS[depthPeelingPass%2],
                            g_pDepthPeelingDS[(depthPeelingPass+1)%2], lightProjMatrix, lightViewMatrix, numMeshes, meshes, g_shadowViewport, lightPos,
                            g_lightRadius, g_depthBiasFromGUI, g_bUseSM);
                    //use RSMs to initialize GV
                    initializeGV(/*pd3dContext,*/ GV0.getRenderTarget(g_LPVLevelToInitialize), GV0Color.getRenderTarget(g_LPVLevelToInitialize),g_pRSMAlbedoRT, g_pRSMNormalRT,
                            g_pDepthPeelingDS[depthPeelingPass%2],g_fLightFov, 1, g_lightNear, g_lightFar, g_useDirectionalLight, lightProjMatrix, lightViewMatrix);
                }
            }

            //depth peeling from the POV of the camera
            if(g_depthPeelFromCamera && (g_useOcclusion || g_useMultipleBounces))
            {
                for(int depthPeelingPass = 0; depthPeelingPass<g_numDepthPeelingPasses; depthPeelingPass++)
                {
//                    pd3dContext.ClearDepthStencilView( *g_pDepthPeelingDS[depthPeelingPass%2], D3D11_CLEAR_DEPTH, 1.0, 0 );
                    tex = g_pDepthPeelingDS[depthPeelingPass%2].pDSV;
                    gl.glClearTexImage(tex.getTexture(), 0, TextureUtils.measureFormat(tex.getFormat()), TextureUtils.measureDataType(tex.getFormat()), CacheBuffer.wrap(1.0f));

                    //render the RSMs
                    renderRSM(/*pd3dContext,*/ depthPeelingPass>0, g_pRSMColorRT, g_pRSMAlbedoRT, g_pRSMNormalRT, g_pDepthPeelingDS[depthPeelingPass%2], g_pDepthPeelingDS[(depthPeelingPass+1)%2], p_cameraProjectionMatrix, p_cameraViewMatrix, numMeshes, meshes, g_shadowViewport,  lightPos, g_lightRadius, g_depthBiasFromGUI, g_bUseSM);
                    //use RSMs to initialize GV
                    float aspsect = (float)getGLContext().width()/getGLContext().height();
                    initializeGV(/*pd3dContext,*/ GV0.getRenderTarget(g_LPVLevelToInitialize), GV0Color.getRenderTarget(g_LPVLevelToInitialize), g_pRSMAlbedoRT, g_pRSMNormalRT,
                            g_pDepthPeelingDS[depthPeelingPass%2], g_fCameraFovy, aspsect, g_cameraNear, g_cameraFar, false, p_cameraProjectionMatrix, p_cameraViewMatrix);
                }
            }

        }


        //restore rasterizer, depth and blend states
        /*pd3dContext->RSSetState(g_pRasterizerStateMainRender);
        pd3dContext->OMSetBlendState(g_pNoBlendBS, BlendFactor, 0xffffffff);
        pd3dContext->OMSetDepthStencilState(g_normalDepthStencilState, 0);*/

        g_pRasterizerStateMainRender.run();
        g_pNoBlendBS.run();
        g_normalDepthStencilState.run();


        //propagate the light --------------------------------------------------------------------
        if(g_useDiffuseInterreflection) //only propagate light if we are actually using diffuse interreflections
        {
            if(g_propType == PROP_TYPE.HIERARCHY)
                invokeHierarchyBasedPropagation(/*pd3dContext,*/ g_bUseSingleLPV, g_numHierarchyLevels, g_numPropagationStepsLPV, g_PropLevel,
                        (LPV_Hierarchy)(GV0),(LPV_Hierarchy)(GV0Color), (LPV_RGB_Hierarchy)(LPV0Accumulate),
                        (LPV_RGB_Hierarchy)(LPV0Propagate));
            else
                invokeCascadeBasedPropagation(/*pd3dContext,*/ g_bUseSingleLPV, g_PropLevel, (LPV_RGB_Cascade)(LPV0Accumulate), (LPV_RGB_Cascade)(LPV0Propagate),
                        (LPV_Cascade)(GV0), (LPV_Cascade)(GV0Color), g_numPropagationStepsLPV);
        }


        //render the scene to the camera---------------------------------------------------

        if(g_renderMesh)
            meshes[0] = g_MainMesh;

        //update the constant buffer for rendering
        if(g_propType == PROP_TYPE.HIERARCHY || g_bUseSingleLPV)
        {
//            pd3dContext->Map( g_pcbRenderLPV, 0, D3D11_MAP_WRITE_DISCARD, 0, &MappedResource );
//            CB_RENDER_LPV* pcbRenderLPV = ( CB_RENDER_LPV* )MappedResource.pData;
            pcbRenderLPV.g_numCols = LPV0Accumulate.getNumCols(g_PropLevel);    //the number of columns in the flattened 2D LPV
            pcbRenderLPV.g_numRows = LPV0Accumulate.getNumRows(g_PropLevel);    //the number of columns in the flattened 2D LPV
            pcbRenderLPV.LPV2DWidth =  LPV0Accumulate.getWidth2D(g_PropLevel);  //the total width of the flattened 2D LPV
            pcbRenderLPV.LPV2DHeight = LPV0Accumulate.getHeight2D(g_PropLevel); //the total height of the flattened 2D LPV
            pcbRenderLPV.LPV3DWidth = LPV0Accumulate.getWidth3D(g_PropLevel);   //the width of the LPV in 3D
            pcbRenderLPV.LPV3DHeight = LPV0Accumulate.getHeight3D(g_PropLevel); //the height of the LPV in 3D
            pcbRenderLPV.LPV3DDepth = LPV0Accumulate.getDepth3D(g_PropLevel);   //the depth of the LPV in 3D
//            pd3dContext->Unmap( g_pcbRenderLPV, 0 );
            bytes = CacheBuffer.wrap(CB_RENDER_LPV.SIZE, pcbRenderLPV);
            g_pcbRenderLPV.update(0, bytes);
        }
        else
        {
            /*pd3dContext->Map( g_pcbRenderLPV, 0, D3D11_MAP_WRITE_DISCARD, 0, &MappedResource );
            CB_RENDER_LPV* pcbRenderLPV = ( CB_RENDER_LPV* )MappedResource.pData;*/
            pcbRenderLPV.g_numCols = LPV0Accumulate.getNumCols(0);    //the number of columns in the flattened 2D LPV
            pcbRenderLPV.g_numRows = LPV0Accumulate.getNumRows(0);    //the number of columns in the flattened 2D LPV
            pcbRenderLPV.LPV2DWidth =  LPV0Accumulate.getWidth2D(0);  //the total width of the flattened 2D LPV
            pcbRenderLPV.LPV2DHeight = LPV0Accumulate.getHeight2D(0); //the total height of the flattened 2D LPV
            pcbRenderLPV.LPV3DWidth = LPV0Accumulate.getWidth3D(0);   //the width of the LPV in 3D
            pcbRenderLPV.LPV3DHeight = LPV0Accumulate.getHeight3D(0); //the height of the LPV in 3D
            pcbRenderLPV.LPV3DDepth = LPV0Accumulate.getDepth3D(0);   //the depth of the LPV in 3D
//            pd3dContext->Unmap( g_pcbRenderLPV, 0 );
            bytes = CacheBuffer.wrap(CB_RENDER_LPV.SIZE, pcbRenderLPV);
            g_pcbRenderLPV.update(0, bytes);

            /*pd3dContext->Map( g_pcbRenderLPV2, 0, D3D11_MAP_WRITE_DISCARD, 0, &MappedResource );
            CB_RENDER_LPV* pcbRenderLPV2 = ( CB_RENDER_LPV* )MappedResource.pData;*/
            pcbRenderLPV.g_numCols = LPV0Accumulate.getNumCols(1);    //the number of columns in the flattened 2D LPV
            pcbRenderLPV.g_numRows = LPV0Accumulate.getNumRows(1);    //the number of columns in the flattened 2D LPV
            pcbRenderLPV.LPV2DWidth =  LPV0Accumulate.getWidth2D(1);  //the total width of the flattened 2D LPV
            pcbRenderLPV.LPV2DHeight = LPV0Accumulate.getHeight2D(1); //the total height of the flattened 2D LPV
            pcbRenderLPV.LPV3DWidth = LPV0Accumulate.getWidth3D(1);   //the width of the LPV in 3D
            pcbRenderLPV.LPV3DHeight = LPV0Accumulate.getHeight3D(1); //the height of the LPV in 3D
            pcbRenderLPV.LPV3DDepth = LPV0Accumulate.getDepth3D(1);   //the depth of the LPV in 3D
//            pd3dContext->Unmap( g_pcbRenderLPV2, 0 );
            bytes = CacheBuffer.wrap(CB_RENDER_LPV.SIZE, pcbRenderLPV);
            g_pcbRenderLPV2.update(0, bytes);
        }

//        pd3dContext->RSSetState(g_pRasterizerStateMainRender);
        g_pRasterizerStateMainRender.run();

//        pd3dContext->IASetInputLayout( g_pMeshLayout );
        g_pMeshLayout.bind();

        // set shader matrices
        /*pd3dContext->VSSetConstantBuffers( 5, 1, &g_pcbRender );
        pd3dContext->PSSetConstantBuffers( 5, 1, &g_pcbRender );
        pd3dContext->PSSetConstantBuffers( 7, 1, &g_pcbRenderLPV );
        pd3dContext->PSSetConstantBuffers( 8, 1, &g_pcbRenderLPV2);*/

        gl.glBindBufferBase(GLenum.GL_UNIFORM_BUFFER, 5, g_pcbRender.getBuffer());
        gl.glBindBufferBase(GLenum.GL_UNIFORM_BUFFER, 7, g_pcbRenderLPV.getBuffer());
        gl.glBindBufferBase(GLenum.GL_UNIFORM_BUFFER, 8, g_pcbRenderLPV2.getBuffer());

        // setup the RT
        /*pd3dContext->OMSetRenderTargets( 1,  &pRTV, pDSV );
        pd3dContext->RSSetViewports(1, &viewport);*/
        gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, 0);
        gl.glViewport(0,0,getGLContext().width(), getGLContext().height());

        // clear color and depth
        /*float ClearColorScene[4] = {0.3f, 0.3f, 0.3f, 1.0f};
        pd3dContext->ClearDepthStencilView( pDSV, D3D11_CLEAR_DEPTH, 1.0, 0 );*/
        gl.glClearColor(0.3f, 0.3f, 0.3f, 1.0f);
        gl.glClearDepthf(1.f);
        gl.glClear(GLenum.GL_COLOR_BUFFER_BIT|GLenum.GL_DEPTH_BUFFER_BIT);

        int srvsUsed = 1;

        //bind the shadow buffer and LPV buffers
        if(LPV0Accumulate.getRed(0).getNumChannels()>1 && LPV0Accumulate->getRed(0)->getNumRTs()==1)
        {
            if(g_bUseSingleLPV || g_propType == HIERARCHY)
            {
                int level = 0;
                if(g_bUseSingleLPV) level = g_PropLevel;

                ID3D11ShaderResourceView* ppSRVs4[4] = { g_pSceneShadowMap->get_pSRV(0), LPV0Accumulate->getRed(level)->get_pSRV(0), LPV0Accumulate->getGreen(level)->get_pSRV(0), LPV0Accumulate->getBlue(level)->get_pSRV(0)  };
                pd3dContext->PSSetShaderResources( 1, 4, ppSRVs4);
                srvsUsed = 4;
            }
            else if(g_propType == CASCADE)
            {
                ID3D11ShaderResourceView* ppSRVs4[7] = { g_pSceneShadowMap->get_pSRV(0), LPV0Accumulate->getRed(0)->get_pSRV(0), LPV0Accumulate->getGreen(0)->get_pSRV(0), LPV0Accumulate->getBlue(0)->get_pSRV(0),
                    LPV0Accumulate->getRed(1)->get_pSRV(0), LPV0Accumulate->getGreen(1)->get_pSRV(0), LPV0Accumulate->getBlue(1)->get_pSRV(0)};
                pd3dContext->PSSetShaderResources( 1, 7, ppSRVs4);
                srvsUsed = 7;
            }
        }
        else if(LPV0Accumulate->getRed(0)->getNumChannels()==1 && LPV0Accumulate->getRed(0)->getNumRTs()==4)
        {
            if(g_bUseSingleLPV || g_propType == HIERARCHY)
            {
                int level = 0;
                if(g_bUseSingleLPV) level = g_PropLevel;
                ID3D11ShaderResourceView* ppSRVs_0[4] = { g_pSceneShadowMap->get_pSRV(0),
                        LPV0Accumulate->getRed(level)->get_pSRV(0), LPV0Accumulate->getGreen(level)->get_pSRV(0), LPV0Accumulate->getBlue(level)->get_pSRV(0)  };
                ID3D11ShaderResourceView* ppSRVs_1[3] = { LPV0Accumulate->getRed(level)->get_pSRV(1), LPV0Accumulate->getGreen(level)->get_pSRV(1), LPV0Accumulate->getBlue(level)->get_pSRV(1)  };
                ID3D11ShaderResourceView* ppSRVs_2[3] = { LPV0Accumulate->getRed(level)->get_pSRV(2), LPV0Accumulate->getGreen(level)->get_pSRV(2), LPV0Accumulate->getBlue(level)->get_pSRV(2)  };
                ID3D11ShaderResourceView* ppSRVs_3[3] = { LPV0Accumulate->getRed(level)->get_pSRV(3), LPV0Accumulate->getGreen(level)->get_pSRV(3), LPV0Accumulate->getBlue(level)->get_pSRV(3)  };
                pd3dContext->PSSetShaderResources(  1, 4, ppSRVs_0);
                pd3dContext->PSSetShaderResources( 12, 3, ppSRVs_1);
                pd3dContext->PSSetShaderResources( 21, 3, ppSRVs_2);
                pd3dContext->PSSetShaderResources( 30, 3, ppSRVs_3);
                srvsUsed = 33;
            }
            else if(g_propType == CASCADE)
            {
                assert(0); //this path is not implemented yet, but needs to be implemented
            }
        }
    else
        assert(0); //this path is not implemented and either we are here by mistake, or we have to implement this path because it is really needed


        //bind the samplers
        ID3D11SamplerState *states[2] = { g_pLinearSampler, g_pComparisonSampler };
        pd3dContext->PSSetSamplers( 0, 2, states );
        ID3D11SamplerState *state[1] = { g_pAnisoSampler };
        pd3dContext->PSSetSamplers( 3, 1, state );

        // set the shaders
        pd3dContext->VSSetShader( g_pVS, NULL, 0 );
        if(useFloat4s)
            pd3dContext->PSSetShader( g_pPS, NULL, 0 );
        else
            pd3dContext->PSSetShader( g_pPS_separateFloatTextures, NULL, 0 );

        for(int i=0; i<numMeshes; i++)
        {
            RenderMesh* mesh = meshes[i];
            //set the matrices
            D3DXMATRIX ViewProjClip2TexLight, WVPMatrixLight, WVMatrixITLight, WVMatrixLight;
            mesh->createMatrices( g_pSceneShadowMapProj, mShadowMatrix, &WVMatrixLight, &WVMatrixITLight, &WVPMatrixLight, &ViewProjClip2TexLight );
            D3DXMATRIX ViewProjClip2TexCamera, WVPMatrixCamera, WVMatrixITCamera, WVMatrixCamera;
            mesh->createMatrices( *p_cameraProjectionMatrix, *p_cameraViewMatrix, &WVMatrixCamera, &WVMatrixITCamera, &WVPMatrixCamera, &ViewProjClip2TexCamera );

            UpdateSceneCB( pd3dContext, *g_LightCamera.GetEyePt(), g_lightRadius, g_depthBiasFromGUI, g_bUseSM, &(WVPMatrixCamera), &(WVMatrixITCamera), &(mesh->m_WMatrix), &(ViewProjClip2TexLight));

            //first render the meshes with no alpha
            pd3dContext->OMSetBlendState(g_pNoBlendBS, BlendFactor, 0xffffffff);
            pd3dContext->Map( g_pcbMeshRenderOptions, 0, D3D11_MAP_WRITE_DISCARD, 0, &MappedResource );
            CB_MESH_RENDER_OPTIONS* pMeshRenderOptions = ( CB_MESH_RENDER_OPTIONS* )MappedResource.pData;
            if(g_useTextureForFinalRender)
                pMeshRenderOptions->useTexture = mesh->m_UseTexture;
            else
                pMeshRenderOptions->useTexture = false;
            pMeshRenderOptions->useAlpha = false;
            pd3dContext->Unmap( g_pcbMeshRenderOptions, 0 );
            pd3dContext->PSSetConstantBuffers( 6, 1, &g_pcbMeshRenderOptions );
            if(g_subsetToRender==-1)
                mesh->m_Mesh.RenderBounded( pd3dContext, D3DXVECTOR3(0,0,0), D3DXVECTOR3(100000,100000,100000), 0, 39, -1, 40, Mesh::NO_ALPHA );
            else
                mesh->m_Mesh.RenderSubsetBounded(0,g_subsetToRender, pd3dContext, D3DXVECTOR3(0,0,0), D3DXVECTOR3(10000,10000,10000), false, 0, 39, -1, 40, Mesh::NO_ALPHA );


            //then render the meshes with alpha
            pd3dContext->OMSetBlendState(g_pAlphaBlendBS, BlendFactor, 0xffffffff);
            pd3dContext->Map( g_pcbMeshRenderOptions, 0, D3D11_MAP_WRITE_DISCARD, 0, &MappedResource );
            pMeshRenderOptions = ( CB_MESH_RENDER_OPTIONS* )MappedResource.pData;
            if(g_useTextureForFinalRender)
                pMeshRenderOptions->useTexture = mesh->m_UseTexture;
            else
                pMeshRenderOptions->useTexture = false;
            pMeshRenderOptions->useAlpha = true;
            pd3dContext->Unmap( g_pcbMeshRenderOptions, 0 );
            pd3dContext->PSSetConstantBuffers( 6, 1, &g_pcbMeshRenderOptions );
            if(g_subsetToRender==-1)
                mesh->m_Mesh.RenderBounded( pd3dContext, D3DXVECTOR3(0,0,0), D3DXVECTOR3(100000,100000,100000), 0, 39, -1, 40, Mesh::WITH_ALPHA );
            else
                mesh->m_Mesh.RenderSubsetBounded(0,g_subsetToRender, pd3dContext, D3DXVECTOR3(0,0,0), D3DXVECTOR3(10000,10000,10000), false, 0, 39, -1, 40, Mesh::WITH_ALPHA );
            pd3dContext->OMSetBlendState(g_pNoBlendBS, BlendFactor, 0xffffffff);
        }

        ID3D11ShaderResourceView** ppSRVsNULL = new ID3D11ShaderResourceView*[srvsUsed];
        for(int i=0; i<srvsUsed; i++) ppSRVsNULL[i]=NULL;
        pd3dContext->PSSetShaderResources( 1, srvsUsed, ppSRVsNULL);
        delete[] ppSRVsNULL;


        //render the box visualizing the LPV
        if(g_bVizLPVBB)
        {
            D3DXVECTOR4 colors[3];
            colors[0] = D3DXVECTOR4(1.0f,0.0f,0.0f,1.0f);
            colors[1] = D3DXVECTOR4(0.0f,1.0f,0.0f,1.0f);
            colors[2] = D3DXVECTOR4(0.0f,0.0f,1.0f,1.0f);

            if(g_propType == HIERARCHY)
                VisualizeBB(pd3dContext, LPV0Propagate->m_collection[0], VPMatrix, colors[0]);
            else
                for(int i=0; i<LPV0Propagate->getNumLevels(); i++)
                    VisualizeBB(pd3dContext, LPV0Propagate->m_collection[i], VPMatrix, colors[min(2,i)]);
        }

        //render the light arrow
        pd3dContext->Map( g_pcbSimple, 0, D3D11_MAP_WRITE_DISCARD, 0, &MappedResource );
        pPSSimple = ( CB_SIMPLE_OBJECTS* )MappedResource.pData;
        //calculate and set the world view projection matrix for transforming the arrow
        D3DXMATRIX objScale, objXForm;
        D3DXMatrixScaling(&objScale,0.06f,0.06f,0.06f);
        D3DXMATRIX mLookAtInv;
        D3DXMatrixInverse(&mLookAtInv, NULL, &mShadowMatrix);
        D3DXMATRIX mWorldS = objScale * mLookAtInv ;
        D3DXMatrixMultiply(&objXForm,&mWorldS,&VPMatrix);
        D3DXMatrixTranspose( &pPSSimple->m_WorldViewProj,&objXForm);
        pPSSimple->m_color = D3DXVECTOR4(1.0f,1.0f,0.0f,1.0f);
        pd3dContext->Unmap( g_pcbSimple, 0 );
        pd3dContext->PSSetConstantBuffers( 4, 1, &g_pcbSimple );
        pd3dContext->VSSetConstantBuffers( 4, 1, &g_pcbSimple );
        pd3dContext->RSSetState(g_pRasterizerStateMainRender);
        pd3dContext->VSSetShader( g_pSimpleVS, NULL, 0 );
        pd3dContext->PSSetShader( g_pSimplePS, NULL, 0 );
        g_MeshArrow.Render( pd3dContext, 0 );


        if(g_bVisualizeLPV3D)
        {
            //render little spheres filling the LPV region showing the value of the LPV at that location in 3D space

            pd3dContext->Map( g_pcbSimple, 0, D3D11_MAP_WRITE_DISCARD, 0, &MappedResource );
            pPSSimple = ( CB_SIMPLE_OBJECTS* )MappedResource.pData;
            D3DXMATRIX wvp;
            D3DXMatrixMultiply(&wvp,&(LPV0Propagate->m_collection[0]->getWorldToLPVBB()),&VPMatrix);
            D3DXMatrixTranspose( &pPSSimple->m_WorldViewProj,&wvp);
            pPSSimple->m_color = D3DXVECTOR4(1.0f,0.0f,0.0f,1.0f);
            pPSSimple->m_sphereScale = D3DXVECTOR4(0.002f * g_LPVscale/23.0f,0.002f * g_LPVscale/23.0f,0.002f * g_LPVscale/23.0f,0.002f * g_LPVscale/23.0f);
            pd3dContext->Unmap( g_pcbSimple, 0 );
            pd3dContext->VSSetConstantBuffers( 4, 1, &g_pcbSimple );

            pd3dContext->VSSetConstantBuffers( 5, 1, &g_pcbRender );
            pd3dContext->PSSetConstantBuffers( 5, 1, &g_pcbRender );
            pd3dContext->PSSetConstantBuffers( 7, 1, &g_pcbRenderLPV ); //note: right now this is not visualizing the right level of the cascade

            if(g_currVizChoice == GV_COLOR )
                pd3dContext->PSSetShaderResources( 11, 1, GV0Color->getShaderResourceViewpp(g_PropLevel) );
            else if(g_currVizChoice == GV )
                pd3dContext->PSSetShaderResources( 11, 1, GV0->getShaderResourceViewpp(g_PropLevel) );
            else if(g_currVizChoice == GREEN_ACCUM_LPV )
                pd3dContext->PSSetShaderResources( 11, 1, LPV0Accumulate->getGreen(g_PropLevel)->get_ppSRV(0) );
    else if(g_currVizChoice == BLUE_ACCUM_LPV )
            pd3dContext->PSSetShaderResources( 11, 1, LPV0Accumulate->getBlue(g_PropLevel)->get_ppSRV(0) );
    else pd3dContext->PSSetShaderResources( 11, 1, LPV0Propagate->getRed(g_PropLevel)->get_ppSRV(0) );


            pd3dContext->RSSetState(g_pRasterizerStateMainRender);
            pd3dContext->VSSetShader( g_pVSVizLPV, NULL, 0 );
            pd3dContext->PSSetShader( g_pPSVizLPV, NULL, 0 );

            ID3D11SamplerState *states[1] = { g_pDefaultSampler };
            pd3dContext->PSSetSamplers( 0, 1, states );

            int xLimit = LPV0Propagate->m_collection[g_PropLevel]->getWidth3D();
            int yLimit = LPV0Propagate->m_collection[g_PropLevel]->getHeight3D();
            int zLimit = LPV0Propagate->m_collection[g_PropLevel]->getDepth3D();

            for(int x=0; x<xLimit; x++)
                for(int y=0; y<yLimit; y++)
                    for(int z=0; z<zLimit; z++)
                    {

                        pd3dContext->Map( g_pcbLPVViz, 0, D3D11_MAP_WRITE_DISCARD, 0, &MappedResource );
                        VIZ_LPV* cbLPVIndex = ( VIZ_LPV* )MappedResource.pData;
                        cbLPVIndex->LPVSpacePos = D3DXVECTOR3((float)x/xLimit,(float)y/yLimit,(float)z/zLimit);
                        pd3dContext->Unmap( g_pcbLPVViz, 0 );
                        pd3dContext->VSSetConstantBuffers( 2, 1, &g_pcbLPVViz );

                        g_LowResMesh.Render( pd3dContext, 0 );
                    }

            ID3D11ShaderResourceView* ppSRVsNULL1[1] = { NULL };
            pd3dContext->PSSetShaderResources( 11, 1, ppSRVsNULL1);

        }

        if(g_bVisualizeSM)
        {
            if(g_currVizChoice == COLOR_RSM )
                visualizeMap(g_pRSMColorRT, pd3dContext, g_pRSMColorRT->getNumChannels() );
            else if(g_currVizChoice == NORMAL_RSM )
                visualizeMap(g_pRSMNormalRT, pd3dContext, g_pRSMNormalRT->getNumChannels() );
            else if(g_currVizChoice == ALBEDO_RSM )
                visualizeMap(g_pRSMAlbedoRT, pd3dContext, g_pRSMAlbedoRT->getNumChannels() );

            else if(g_currVizChoice == RED_LPV )
                visualizeMap(LPV0Propagate->getRed(g_PropLevel), pd3dContext, LPV0Propagate->getRed(g_PropLevel)->getNumChannels() );
        else if(g_currVizChoice == GREEN_ACCUM_LPV )
            visualizeMap(LPV0Accumulate->getGreen(g_PropLevel), pd3dContext, LPV0Accumulate->getGreen(g_PropLevel)->getNumChannels() );
        else if(g_currVizChoice == BLUE_ACCUM_LPV )
            visualizeMap(LPV0Accumulate->getBlue(g_PropLevel), pd3dContext, LPV0Accumulate->getBlue(g_PropLevel)->getNumChannels());

        else if(g_currVizChoice == GV )
            visualizeMap(GV0->getRenderTarget(g_PropLevel),pd3dContext, 1);
        else if(g_currVizChoice == GV_COLOR )
            visualizeMap(GV0Color->getRenderTarget(g_PropLevel),pd3dContext, 1);

        }
    }

    private final CB_RENDER_LPV pcbRenderLPV = new CB_RENDER_LPV();

    void resetSettingValues()
    {
        setLightAndCamera();

        if(g_propType == PROP_TYPE.HIERARCHY ) bPropTypeChanged = true;
        g_propType = PROP_TYPE.CASCADE;
        setLPVScale();
        setFluxAmplifier();
        setNumIterations();

        g_selectedAdditionalOption = ADITIONAL_OPTIONS_SELECT.SIMPLE_LIGHT;
        g_useDiffuseInterreflection = true;
        g_directLightStrength = 1.0f;
        g_diffuseInterreflectionScale = 1.5f;
        g_directLight = 2.22f;
        g_lightRadius = 150.0f;
        g_useBilinearInit = false;
        g_useOcclusion = false;
        g_useMultipleBounces = false;
        g_numDepthPeelingPasses = 1;
        g_reflectedLightAmplifier = 4.8f;
        g_occlusionAmplifier = 0.8f;
        g_movableLPV = true;
        g_resetLPVXform = true;
        g_bUseSingleLPV = false;
        g_useRSMCascade = true;
        g_PropLevel = 0;
        g_VPLDisplacement = 1.f;
        g_bVizLPVBB = false;
        g_bUseSM = true;
        g_renderMesh = true;
        g_useTextureForFinalRender = true;
        g_useTextureForRSMs = true;
        g_showMovableMesh = false;
        g_smTaps = 8;
        g_smFilterSize = 0.8f;
        g_currVizChoice = VIZ_OPTIONS.RED_LPV;
        g_bVisualizeSM = false;
        g_bVisualizeLPV3D = false;
        if(g_useDirectionalLight)
            g_depthBiasFromGUI = 0.0021f;
        else
            g_depthBiasFromGUI = 0.00001f;
        g_useDirectionalDerivativeClamping = false;
        g_directionalDampingAmount = 0.1f;
    }

    void visualizeMap(RenderTarget RT, /*ID3D11DeviceContext* pd3dContext,*/ int numChannels)
    {
        // draw the 2d texture
        /*UINT stride = sizeof( TexPosVertex );
        UINT offset = 0;
        pd3dContext->IASetInputLayout( g_pScreenQuadPosTexIL );
        pd3dContext->IASetVertexBuffers( 0, 1, &g_pVizQuadVB, &stride, &offset );
        pd3dContext->IASetPrimitiveTopology( D3D11_PRIMITIVE_TOPOLOGY_TRIANGLESTRIP );*/
        gl.glBindBuffer(g_pVizQuadVB.getTarget(), g_pVizQuadVB.getBuffer());
        g_pScreenQuadPosTexIL.bind();

        int numRTs = RT.getNumRTs();
        assert(numRTs<=4); //the shaders are not setup for more than this amount of textures
//        ID3D11ShaderResourceView** ppSRV = new ID3D11ShaderResourceView*[numRTs];
        for(int i=0; i<numRTs; i++) {
//            ppSRV[i] = RT -> get_pSRV(i);
            TextureGL src = RT.get_pSRV(i);
            gl.glActiveTexture(GLenum.GL_TEXTURE0 + i);
            gl.glBindTexture(src.getTarget(), src.getTexture());
        }
//        pd3dContext->PSSetShaderResources( 5, numRTs, ppSRV );


       /* ID3D11SamplerState *states[1] = { g_pDefaultSampler };  TODO
        pd3dContext->PSSetSamplers( 0, 1, states );*/

//        D3D11_MAPPED_SUBRESOURCE MappedResource;

        if(!RT.is2DTexture())
        {
//            pd3dContext->VSSetShader( g_pScreenQuadPosTexVS3D, NULL, 0 );
            g_Program.setVS(g_pScreenQuadPosTexVS3D);
            if(numRTs==1)
//                pd3dContext->PSSetShader( g_pScreenQuadDisplayPS3D, NULL, 0 );
                g_Program.setPS(g_pScreenQuadDisplayPS3D);
            else if(numChannels==1)
//                pd3dContext->PSSetShader( g_pScreenQuadDisplayPS3D_floatTextures, NULL, 0 );\
                g_Program.setPS(g_pScreenQuadDisplayPS3D_floatTextures);

            /*pd3dContext->Map( g_pcbSlices3D, 0, D3D11_MAP_WRITE_DISCARD, 0, &MappedResource );
            CB_DRAW_SLICES_3D* pcbSlices3D = ( CB_DRAW_SLICES_3D* )MappedResource.pData;
            pcbSlices3D->width3D = (float)RT->m_width3D;
            pcbSlices3D->height3D = (float)RT->m_height3D;
            pcbSlices3D->depth3D = (float)RT->m_depth3D;
            pd3dContext->Unmap( g_pcbSlices3D, 0 );
            pd3dContext->VSSetConstantBuffers( 0, 1, &g_pcbSlices3D );*/
            FloatBuffer buffer = CacheBuffer.getCachedFloatBuffer(4);
            buffer.put(RT.getWidth());
            buffer.put(RT.getHeight());
            buffer.put(RT.getDepth());
            buffer.put(0).flip();
            g_pcbSlices3D.update(0, buffer);
            gl.glBindBufferBase(GLenum.GL_UNIFORM_BUFFER, 0, g_pcbSlices3D.getBuffer());
            g_grid.DrawSlicesToScreen();
        }
        else
        {
//            pd3dContext->VSSetShader( g_pScreenQuadPosTexVS2D, NULL, 0 );
            g_Program.setVS(g_pScreenQuadPosTexVS2D);
            if(numRTs==1)
//                pd3dContext->PSSetShader( g_pScreenQuadDisplayPS2D, NULL, 0 );
                g_Program.setPS(g_pScreenQuadDisplayPS2D);
            else  if(numChannels==1)
//                pd3dContext->PSSetShader( g_pScreenQuadDisplayPS2D_floatTextures, NULL, 0 );
                g_Program.setPS(g_pScreenQuadDisplayPS2D_floatTextures);

//            pd3dContext->Draw( 4, 0 );
            gl.glDrawArrays(GLenum.GL_TRIANGLE_STRIP, 0, 4);
        }

//        ID3D11ShaderResourceView* ppNullSRV[1] = { NULL };
//        pd3dContext->PSSetShaderResources( 5, 1, ppNullSRV );
    }

    private final CB_SIMPLE_OBJECTS m_SimpleObjects = new CB_SIMPLE_OBJECTS();

    void VisualizeBB(/*ID3D11DeviceContext* pd3dContext,*/ SimpleRT_RGB LPV, Matrix4f VPMatrix, Vector4f color)
    {
        //translate the box to the center and scale it to be 1.0 in size
        //then transform the box by the transform for the LPV
        /*D3D11_MAPPED_SUBRESOURCE MappedResource;
        pd3dContext->Map( g_pcbSimple, 0, D3D11_MAP_WRITE_DISCARD, 0, &MappedResource );
        CB_SIMPLE_OBJECTS* pPSSimple = ( CB_SIMPLE_OBJECTS* )MappedResource.pData;
        D3DXMATRIX objScale, objXForm, objTranslate;
        D3DXMatrixScaling(&objScale,0.5f/g_BoXExtents.x,0.5f/g_BoXExtents.y,0.5f/g_BoXExtents.z);
        D3DXMatrixTranslation(&objTranslate,g_BoxCenter.x,g_BoxCenter.y,g_BoxCenter.z);
        D3DXMatrixMultiply(&objXForm,&objTranslate,&objScale);
        D3DXMatrixMultiply(&objXForm,&objXForm,&(LPV->getWorldToLPVBB()));
        D3DXMatrixMultiply(&objXForm,&objXForm,&VPMatrix);
        D3DXMatrixTranspose( &pPSSimple->m_WorldViewProj,&objXForm);
        pPSSimple->m_color = color;
        pd3dContext->Unmap( g_pcbSimple, 0 );*/

        Matrix4f objXForm = m_SimpleObjects.m_WorldViewProj;
        objXForm.setIdentity();
        objXForm.m00 = 0.5f/g_BoXExtents.x;
        objXForm.m11 = 0.5f/g_BoXExtents.y;
        objXForm.m22 = 0.5f/g_BoXExtents.z;
        objXForm.translate(g_BoxCenter.x,g_BoxCenter.y,g_BoxCenter.z);
        Matrix4f.mul(LPV.getWorldToLPVBB(), objXForm, objXForm);
        Matrix4f.mul(VPMatrix, objXForm, objXForm);
        m_SimpleObjects.m_color.set(color);
        ByteBuffer buffer = CacheBuffer.getCachedByteBuffer(CB_SIMPLE_OBJECTS.SIZE);
        m_SimpleObjects.store(buffer).flip();
        g_pcbSimple.update(0, buffer);
        gl.glBindBufferBase(GLenum.GL_UNIFORM_BUFFER, 0, g_pcbSimple.getBuffer());

        /*pd3dContext->PSSetConstantBuffers( 4, 1, &g_pcbSimple );
        pd3dContext->VSSetConstantBuffers( 4, 1, &g_pcbSimple );
        pd3dContext->RSSetState(pRasterizerStateWireFrame);
        pd3dContext->VSSetShader( g_pSimpleVS, NULL, 0 );
        pd3dContext->PSSetShader( g_pSimplePS, NULL, 0 );*/
        pRasterizerStateWireFrame.run();
        g_Program.setVS(g_pSimpleVS);
        g_Program.setPS(g_pSimplePS);

        g_MeshBox.render(/*pd3dContext,*/ 0, -1, -1);
    }

    @Override
    public void display() {
        DrawScene();
    }

    void updateProjectionMatrices()
    {
        // setup the camera's projection parameters
        g_fCameraFovy        = 55 /** D3DX_PI / 180*/;
        float g_fAspectRatio = (float)getGLContext().width() / getGLContext().height();
        g_cameraNear        = 0.01f;
        g_cameraFar         = 200.0f;

        g_Camera.setProjection ( g_fCameraFovy, g_fAspectRatio, g_cameraNear, g_cameraFar );


        //set the light camera's projection parameters
        g_lightNear = 0.01f;
        g_lightFar = Math.max(100.f, g_lightRadius);

        {
            if(g_useDirectionalLight)
            {
                final float RSM_CASCADE_0_SIZE = 15.f;
                final float RSM_CASCADE_1_SIZE  = 25.f;

//                D3DXMatrixOrthoLH(&g_pSceneShadowMapProj,        20, 20, g_lightNear, g_lightFar);     //matrix for the scene shadow map
//                D3DXMatrixOrthoLH(&g_pShadowMapProjMatrixSingle, 20, 20, g_lightNear, g_lightFar); //matrix to use if not using cascaded RSMs
                Matrix4f.ortho(20, 20, g_lightNear, g_lightFar, g_pSceneShadowMapProj);
                Matrix4f.ortho(20, 20, g_lightNear, g_lightFar, g_pShadowMapProjMatrixSingle);
                //matrices for the cascaded RSM
//                D3DXMatrixOrthoLH(&g_pRSMProjMatrices[0], RSM_CASCADE_0_SIZE, RSM_CASCADE_0_SIZE, g_lightNear, g_lightFar); //first level of the cascade
//                D3DXMatrixOrthoLH(&g_pRSMProjMatrices[1], RSM_CASCADE_1_SIZE, RSM_CASCADE_1_SIZE, g_lightNear, g_lightFar); //second level of the cascade
                Matrix4f.ortho(RSM_CASCADE_0_SIZE, RSM_CASCADE_0_SIZE, g_lightNear, g_lightFar, g_pRSMProjMatrices[0]);
                Matrix4f.ortho(RSM_CASCADE_1_SIZE, RSM_CASCADE_1_SIZE, g_lightNear, g_lightFar, g_pRSMProjMatrices[1]);

                final float fCascadeSize[/*SM_PROJ_MATS_SIZE*/] = { RSM_CASCADE_0_SIZE, RSM_CASCADE_1_SIZE };
                final float fCascadeTexelSizeSnapping[/*SM_PROJ_MATS_SIZE*/] = { RSM_CASCADE_0_SIZE / Defines.RSM_RES * 4.f, RSM_CASCADE_1_SIZE / Defines.RSM_RES * 4.f };

                if(g_movableLPV)
                {
                    // Get light rotation matrix
                    /*D3DXMATRIXA16 mShadowMatrix;
                    mShadowMatrix = *(g_LightCamera.GetViewMatrix());
                    mShadowMatrix._41 = 0.f;
                    mShadowMatrix._42 = 0.f;
                    mShadowMatrix._43 = 0.f;*/
                    final Matrix4f mShadowMatrix = g_LightCamera.getViewMatrix();

                    // get camera position and direction
                    /*const D3DXVECTOR3 vEye = *g_Camera.GetEyePt();
                    D3DXVECTOR3 vDir = (*g_Camera.GetLookAtPt() - *g_Camera.GetEyePt());
                    D3DXVec3Normalize(&vDir, &vDir);*/
                    ReadableVector3f vEye = g_Camera.getPosition();
                    ReadableVector3f vDir = g_Camera.getLookAt();

                    // Move RSM cascades with camera with 4-texel snapping
                    Vector3f[] vEyeCascade = new Vector3f[SM_PROJ_MATS_SIZE];
                    for(int i=0;i<SM_PROJ_MATS_SIZE;++i)
                    {
                        Vector3f vEyeOffsetted = vEyeCascade[i] = new Vector3f();
                        // Shift the center of the RSM for 20% towards the view direction
                        /*D3DXVECTOR3 vEyeOffsetted = vEye + vDir * fCascadeSize[i] * .2f;
                        vEyeOffsetted.z = 0;
                        D3DXVECTOR4 vEye4;
                        D3DXVec3Transform(&vEye4, &vEyeOffsetted, &mShadowMatrix);*/
                        Vector3f.linear(vEye, vDir, fCascadeSize[i] * .2f, vEyeOffsetted);
                        Vector3f vEye4 = Matrix4f.transformNormal(mShadowMatrix, vEyeOffsetted, vEyeOffsetted);

                        // Perform a 4-texels snapping in order to provide coherent movement-independent rasterization
                        vEyeCascade[i].x = (float) (Math.floor(vEye4.x / fCascadeTexelSizeSnapping[i]) * fCascadeTexelSizeSnapping[i]);
                        vEyeCascade[i].y = (float) (Math.floor(vEye4.y / fCascadeTexelSizeSnapping[i]) * fCascadeTexelSizeSnapping[i]);
                        vEyeCascade[i].z = (float) (Math.floor(vEye4.z / fCascadeTexelSizeSnapping[i]) * fCascadeTexelSizeSnapping[i]);
                    }

                    // Translate the projection matrix of each RSM cascade
                    final Matrix4f mxTrans = CacheBuffer.getCachedMatrix();
                    for(int i=0;i<SM_PROJ_MATS_SIZE;++i)
                    {
                        /*D3DXMatrixTranslation(&mxTrans, -vEyeCascade[i].x, -vEyeCascade[i].y, -vEyeCascade[i].z);
                        D3DXMatrixMultiply(&g_pRSMProjMatrices[i], &mxTrans, &g_pRSMProjMatrices[i]);*/
                        mxTrans.setTranslate(-vEyeCascade[i].x, -vEyeCascade[i].y, -vEyeCascade[i].z);
                        Matrix4f.mul(g_pRSMProjMatrices[i], mxTrans, g_pRSMProjMatrices[i]);
                    }
                }
            }
            else
            {
                g_fLightFov = (float) Math.toDegrees(Numeric.PI / 1.4f);
//                D3DXMatrixPerspectiveFovLH( &g_pSceneShadowMapProj, D3DX_PI / 1.4f, 1, g_lightNear, g_lightFar );     //matrix for the scene shadow map
//                D3DXMatrixPerspectiveFovLH( &g_pShadowMapProjMatrixSingle, D3DX_PI / 1.4f, 1, g_lightNear, g_lightFar ); //matrix to use if not using cascaded RSMs
                Matrix4f.perspective(g_fLightFov, 1, g_lightNear, g_lightFar, g_pSceneShadowMapProj);
                Matrix4f.perspective(g_fLightFov, 1, g_lightNear, g_lightFar, g_pShadowMapProjMatrixSingle);


                //matrices for the cascaded RSM
//                D3DXMatrixPerspectiveFovLH( &g_pRSMProjMatrices[0], D3DX_PI / 1.4f, 1, g_lightNear, g_lightFar );//first level of the cascade
//                D3DXMatrixPerspectiveFovLH( &g_pRSMProjMatrices[1], D3DX_PI / 1.4f, 1, g_lightNear, g_lightFar );//second level of the cascade
                Matrix4f.perspective(g_fLightFov, 1, g_lightNear, g_lightFar, g_pRSMProjMatrices[0]);
                g_pRSMProjMatrices[1].load(g_pRSMProjMatrices[0]);
            }
        }
    }

    @Override
    protected void reshape(int width, int height) {
        if(width <= 0 || height <= 0)
            return;

        if(g_pSceneDepth == null || g_pSceneDepth.getWidth() != width || g_pSceneDepth.getHeight() != height){
            CommonUtil.safeRelease(g_pSceneDepth);

            g_pSceneDepth = TextureUtils.createTexture2D(new Texture2DDesc(width, height, GLenum.GL_DEPTH_COMPONENT32F), null);
            g_pSceneDepthRV = g_pSceneDepth;
        }
    }

    void LoadMainMeshes(/*ID3D11Device* pd3dDevice*/) throws IOException {
//        HRESULT hr = S_OK;

        g_MainMesh.m_Mesh.create( /*pd3dDevice,*/ "..\\Media\\sponza\\sponzaNoFlag.sdkmesh", true , null);
        g_MainMeshSimplified.m_Mesh.create("..\\Media\\sponza\\SponzaNoFlag.sdkmesh", true, null ); //can also use SponzaNoFlagSimplified.sdkmesh which is a bit smaller

        g_MainMesh.m_Mesh.LoadNormalmaps(/*pd3dDevice,*/ "diff.dds", "ddn.dds");
        g_MainMesh.m_Mesh.LoadNormalmaps(/*pd3dDevice,*/ ".dds", "_ddn.dds");
        g_MainMesh.m_Mesh.LoadNormalmaps(/*pd3dDevice,*/ "dif.dds", "ddn.dds");
        g_MainMesh.m_Mesh.initializeDefaultNormalmaps(/*pd3dDevice,*/ "defaultNormalTexture.dds");
        g_MainMesh.m_Mesh.initializeAlphaMaskTextures();
        g_MainMesh.m_Mesh.LoadAlphaMasks( /*pd3dDevice,*/ ".dds", "_mask.dds" );
        g_MainMesh.m_Mesh.LoadAlphaMasks( /*pd3dDevice,*/ "_diff.dds", "_mask.dds");

        g_MainMesh.m_UseTexture = true;
        g_MainMeshSimplified.m_UseTexture = true;

        Vector3f meshExtents;
        Vector3f meshCenter;

        meshExtents = g_MainMesh.m_Mesh.getMeshBBoxExtents(0);
        meshCenter = g_MainMesh.m_Mesh.getMeshBBoxCenter(0);
        g_MainMesh.setWorldMatrix(          0.01f,0.01f,0.01f,0,0,0,-meshCenter.x,-meshCenter.y,-meshCenter.z);
        meshExtents = g_MainMeshSimplified.m_Mesh.getMeshBBoxExtents(0);
        meshCenter = g_MainMeshSimplified.m_Mesh.getMeshBBoxCenter(0);
        g_MainMeshSimplified.setWorldMatrix(0.01f,0.01f,0.01f,0,0,0,-meshCenter.x,-meshCenter.y,-meshCenter.z);


        g_MainMovableMesh.m_Mesh.create( "..\\Media\\sponza\\flag.sdkmesh", true, null );
        meshExtents = g_MainMovableMesh.m_Mesh.getMeshBBoxExtents(0);
        meshCenter = g_MainMovableMesh.m_Mesh.getMeshBBoxCenter(0);
        g_MainMovableMesh.setWorldMatrix(0.01f,0.01f,0.01f,0,0,0,-meshCenter.x,-meshCenter.y,-meshCenter.z);
        g_MainMovableMesh.m_Mesh.initializeDefaultNormalmaps("defaultNormalTexture.dds");

        g_MainMesh.m_Mesh.ComputeSubmeshBoundingVolumes();
        g_MainMeshSimplified.m_Mesh.ComputeSubmeshBoundingVolumes();
        g_MainMovableMesh.m_Mesh.ComputeSubmeshBoundingVolumes();

        g_MainMovableMesh.m_UseTexture = false;

//        return hr;
    }

    void renderRSM(/*ID3D11DeviceContext* pd3dContext,*/ boolean depthPeel, SimpleRT pRSMColorRT, SimpleRT pRSMAlbedoRT, SimpleRT pRSMNormalRT, DepthRT pShadowMapDS,
                   DepthRT pShadowTex, Matrix4f projectionMatrix, Matrix4f viewMatrix, int numMeshes, RenderMesh[] meshes, Vector4i shadowViewport,
                   ReadableVector3f lightPos, float lightRadius, float depthBiasFromGUI, boolean bUseSM)
    {
        throw new UnsupportedOperationException();
        /*float BlendFactor[4] = { 1.0f, 1.0f, 1.0f, 1.0f };
        pd3dContext->OMSetBlendState(g_pNoBlendBS, BlendFactor, 0xffffffff);
        pd3dContext->OMSetDepthStencilState(g_normalDepthStencilState, 0);
        pd3dContext->RSSetState(g_pRasterizerStateMainRender);


        D3D11_MAPPED_SUBRESOURCE MappedResource;
        float ClearColor[4] = {0.0f, 0.0f, 0.0f, 0.0f};

        pd3dContext->ClearRenderTargetView( pRSMColorRT->get_pRTV(0), ClearColor );
        pd3dContext->ClearRenderTargetView( pRSMAlbedoRT->get_pRTV(0), ClearColor );
        pd3dContext->ClearRenderTargetView( pRSMNormalRT->get_pRTV(0), ClearColor );
        ID3D11RenderTargetView* pRTVs[3] = { pRSMColorRT->get_pRTV(0), pRSMNormalRT->get_pRTV(0), pRSMAlbedoRT->get_pRTV(0) };

        if(depthPeel)
        {
            ID3D11ShaderResourceView* ppSRV[1] = { pShadowTex->get_pSRV(0) };
            pd3dContext->PSSetShaderResources( 6, 1, ppSRV );

            ID3D11SamplerState *states[1] = { g_pDepthPeelingTexSampler };
            pd3dContext->PSSetSamplers( 2, 1, states );
        }

        pd3dContext->ClearDepthStencilView( *pShadowMapDS, D3D11_CLEAR_DEPTH, 1.0, 0 );

        pd3dContext->OMSetRenderTargets( 3, pRTVs, *pShadowMapDS );
        pd3dContext->RSSetViewports(1, &shadowViewport);

        D3DXMATRIX inverseProjectionMatrix;
        D3DXMatrixInverse( &inverseProjectionMatrix, NULL, projectionMatrix);

        if(depthPeel)
        {
            pd3dContext->VSSetShader( g_pVSRSMDepthPeeling, NULL, 0 );
            pd3dContext->PSSetShader( g_pPSRSMDepthPeel, NULL, 0 );
        }
        else
        {
            pd3dContext->VSSetShader( g_pVSRSM, NULL, 0 );
            pd3dContext->PSSetShader( g_pPSRSM, NULL, 0 );
        }

        //bind the sampler
        ID3D11SamplerState *states[1] = { g_pLinearSampler };
        pd3dContext->PSSetSamplers( 0, 1, states );
        ID3D11SamplerState *stateAniso[1] = { g_pAnisoSampler };
        pd3dContext->PSSetSamplers( 3, 1, stateAniso );


        //render the meshes
        for(int i=0; i<numMeshes; i++)
        {
            RenderMesh* mesh = meshes[i];

            //set the light matrices
            D3DXMATRIX WVMatrix, WVMatrixIT, WVPMatrix, ViewProjClip2Tex;
            mesh->createMatrices( *projectionMatrix, *viewMatrix, &WVMatrix, &WVMatrixIT, &WVPMatrix, &ViewProjClip2Tex );
            UpdateSceneCB( pd3dContext, lightPos, lightRadius, depthBiasFromGUI, bUseSM, &(WVPMatrix), &(WVMatrixIT), &(mesh->m_WMatrix), &(ViewProjClip2Tex) );

            pd3dContext->Map( g_pcbMeshRenderOptions, 0, D3D11_MAP_WRITE_DISCARD, 0, &MappedResource );
            CB_MESH_RENDER_OPTIONS* pMeshRenderOptions = ( CB_MESH_RENDER_OPTIONS* )MappedResource.pData;
            if(g_useTextureForRSMs)
                pMeshRenderOptions->useTexture = mesh->m_UseTexture;
            else
                pMeshRenderOptions->useTexture = false;
            pd3dContext->Unmap( g_pcbMeshRenderOptions, 0 );
            pd3dContext->PSSetConstantBuffers( 6, 1, &g_pcbMeshRenderOptions );

            if(g_subsetToRender==-1)
                mesh->m_Mesh.RenderBounded( pd3dContext, D3DXVECTOR3(0,0,0), D3DXVECTOR3(100000,100000,100000), 0 );
            else
                mesh->m_Mesh.RenderSubsetBounded(0,g_subsetToRender, pd3dContext, D3DXVECTOR3(0,0,0), D3DXVECTOR3(10000,10000,10000), false, 0 );
        }


        ID3D11RenderTargetView* pRTVsNULL3[3] = { NULL, NULL, NULL };
        pd3dContext->OMSetRenderTargets( 3, pRTVsNULL3, NULL );

        ID3D11ShaderResourceView* ppSRVNULL[1] = { NULL };
        pd3dContext->PSSetShaderResources( 6, 1, ppSRVNULL );*/
    }

    //initialize the GV ( geometry volume) with the RSM data
    void initializeGV(/*ID3D11DeviceContext* pd3dContext,*/ SimpleRT GV, SimpleRT GVColor, SimpleRT RSMAlbedoRT, SimpleRT RSMNormalRT, DepthRT shadowMapDS,
                      float fovy, float aspectRatio, float nearPlane, float farPlane, boolean useDirectional, Matrix4f projectionMatrix, Matrix4f viewMatrix)
    {
//        HRESULT hr;
        throw new UnsupportedOperationException();
        /*float BlendFactor[4] = { 1.0f, 1.0f, 1.0f, 1.0f };
        pd3dContext->OMSetBlendState(g_pMaxBlendBS, BlendFactor, 0xffffffff);
        pd3dContext->OMSetDepthStencilState(g_depthStencilStateDisableDepth, 0);

        assert(GV->getNumRTs()==1); // this code cannot deal with more than one texture in each RenderTarget
        ID3D11RenderTargetView* GVRTV = GV->get_pRTV(0);
        ID3D11RenderTargetView* GVColorRTV = GVColor->get_pRTV(0);

        D3DXMATRIX viewToLPVMatrix;
        viewToLPVMatrix = GV->getViewToLPVMatrixGV(viewMatrix);

        //set the constant buffer
        float tanFovyHalf = tan(fovy/2.0f);
        float tanFovxHalf = tan(fovy/2.0f)*aspectRatio;

        D3D11_MAPPED_SUBRESOURCE MappedResource;
        pd3dContext->Map( g_reconPos, 0, D3D11_MAP_WRITE_DISCARD, 0, &MappedResource );
        RECONSTRUCT_POS* pcbTempR = ( RECONSTRUCT_POS* )MappedResource.pData;
        pcbTempR->farClip = farPlane;
        pd3dContext->Unmap( g_reconPos, 0 );
        pd3dContext->VSSetConstantBuffers( 6, 1, &g_reconPos );
        pd3dContext->PSSetConstantBuffers( 6, 1, &g_reconPos );

        D3DXMATRIX mProjInv;
        D3DXMatrixInverse(&mProjInv, NULL, projectionMatrix);
        V(pd3dContext->Map( g_pcbInvProjMatrix, 0, D3D11_MAP_WRITE_DISCARD, 0, &MappedResource ) );
        CB_INVPROJ_MATRIX* pInvProjMat = ( CB_INVPROJ_MATRIX* )MappedResource.pData;
        D3DXMatrixTranspose( &pInvProjMat->m_InverseProjMatrix, &mProjInv );
        pd3dContext->Unmap( g_pcbInvProjMatrix, 0 );
        pd3dContext->VSSetConstantBuffers( 7, 1, &g_pcbInvProjMatrix );

        g_LPVViewport.Width  = (float)GV->getWidth3D();
        g_LPVViewport.Height = (float)GV->getHeight3D();

        //clear and bind the GVs as render targets
        //bind all the GVs as MRT output
        ID3D11RenderTargetView* pRTVGV[2] = { GVRTV, GVColorRTV };
        pd3dContext->OMSetRenderTargets( 2, pRTVGV, NULL );
        pd3dContext->RSSetViewports(1, &g_LPVViewport);

        //bind the RSMs to be read as textures
        ID3D11ShaderResourceView* ppSRVsRSMs2[3] = {RSMAlbedoRT->get_pSRV(0), RSMNormalRT->get_pSRV(0), shadowMapDS->get_pSRV(0)};
        pd3dContext->VSSetShaderResources( 0, 3, ppSRVsRSMs2);

        //set the shaders
        if(g_useBilinearInit && g_bilinearInitGVenabled)
            pd3dContext->VSSetShader( g_pVSInitializeLPV_Bilinear, NULL, 0 );
        else
            pd3dContext->VSSetShader( g_pVSInitializeLPV, NULL, 0 );
        if(g_useBilinearInit && g_bilinearInitGVenabled)
            pd3dContext->GSSetShader( g_pGSInitializeLPV_Bilinear, NULL, 0 );
        else
            pd3dContext->GSSetShader( g_pGSInitializeLPV, NULL, 0 );
        pd3dContext->PSSetShader( g_pPSInitializeGV, NULL, 0 );

        //set the constants
        pd3dContext->Map( g_pcbLPVinitVS, 0, D3D11_MAP_WRITE_DISCARD, 0, &MappedResource );
        CB_LPV_INITIALIZE* pcbLPVinitVS = ( CB_LPV_INITIALIZE* )MappedResource.pData;
        pcbLPVinitVS->RSMWidth = RSMNormalRT->getWidth2D();
        pcbLPVinitVS->RSMHeight = RSMNormalRT->getHeight2D();
        pd3dContext->Unmap( g_pcbLPVinitVS, 0 );
        pd3dContext->VSSetConstantBuffers( 0, 1, &g_pcbLPVinitVS );


        //initialize the constant buffer which changes based on the chosen LPV level
        pd3dContext->Map( g_pcbLPVinitialize_LPVDims, 0, D3D11_MAP_WRITE_DISCARD, 0, &MappedResource );
        CB_LPV_INITIALIZE3* pcbLPVinitVS3 = ( CB_LPV_INITIALIZE3* )MappedResource.pData;
        pcbLPVinitVS3->g_numCols = GV->getNumCols();    //the number of columns in the flattened 2D LPV
        pcbLPVinitVS3->g_numRows = GV->getNumRows();    //the number of columns in the flattened 2D LPV
        pcbLPVinitVS3->LPV2DWidth =  GV->getWidth2D();  //the total width of the flattened 2D LPV
        pcbLPVinitVS3->LPV2DHeight = GV->getHeight2D(); //the total height of the flattened 2D LPV
        pcbLPVinitVS3->LPV3DWidth = GV->getWidth3D();   //the width of the LPV in 3D
        pcbLPVinitVS3->LPV3DHeight = GV->getHeight3D(); //the height of the LPV in 3D
        pcbLPVinitVS3->LPV3DDepth = GV->getDepth3D();   //the depth of the LPV in 3D
        pcbLPVinitVS3->fluxWeight = 4 * tanFovxHalf * tanFovyHalf / (RSM_RES * RSM_RES);
        pcbLPVinitVS3->fluxWeight *= 30.0f; //arbitrary scale
        pcbLPVinitVS3->useFluxWeight = useDirectional? 0 : 1;
        pd3dContext->Unmap( g_pcbLPVinitialize_LPVDims, 0 );

        pd3dContext->VSSetConstantBuffers( 5, 1, &g_pcbLPVinitialize_LPVDims );
        pd3dContext->GSSetConstantBuffers( 5, 1, &g_pcbLPVinitialize_LPVDims );


        pd3dContext->Map( g_pcbLPVinitVS2, 0, D3D11_MAP_WRITE_DISCARD, 0, &MappedResource );
        CB_LPV_INITIALIZE2* pcbLPVinitVS2 = ( CB_LPV_INITIALIZE2* )MappedResource.pData;
        D3DXMatrixTranspose( &pcbLPVinitVS2->g_ViewToLPV, &viewToLPVMatrix);

        D3DXVec3TransformNormal(&pcbLPVinitVS2->lightDirGridSpace, &D3DXVECTOR3(0, 0, 1), &viewToLPVMatrix);
        D3DXVec3Normalize(&pcbLPVinitVS2->lightDirGridSpace, &pcbLPVinitVS2->lightDirGridSpace);

        pcbLPVinitVS2->displacement = g_VPLDisplacement;
        pd3dContext->Unmap( g_pcbLPVinitVS2, 0 );
        pd3dContext->VSSetConstantBuffers( 1, 1, &g_pcbLPVinitVS2 );

        int numPoints = RSMAlbedoRT->getWidth2D()*RSMAlbedoRT->getHeight2D();
        if(g_useBilinearInit && g_bilinearInitGVenabled)
            numPoints *= 8;
        unsigned int stride = 0;
        unsigned int offset = 0;
        ID3D11Buffer* buffer[] = { NULL };
        pd3dContext->IASetVertexBuffers(0, 1, buffer, &stride, &offset);
        pd3dContext->IASetInputLayout(NULL);
        pd3dContext->IASetPrimitiveTopology(D3D11_PRIMITIVE_TOPOLOGY_POINTLIST);
        pd3dContext->Draw(numPoints,0);
        pd3dContext->IASetInputLayout( g_pMeshLayout );

        ID3D11RenderTargetView* pRTVsNULL3[3] = { NULL, NULL, NULL };
        pd3dContext->OMSetRenderTargets( 3, pRTVsNULL3, NULL );
        ID3D11ShaderResourceView* ppSRVsNULL3[3] = { NULL, NULL, NULL };
        pd3dContext->VSSetShaderResources( 0, 3, ppSRVsNULL3);
        ID3D11GeometryShader* NULLGS = NULL;
        pd3dContext->GSSetShader( NULLGS, NULL, 0 );

        pd3dContext->OMSetBlendState(g_pNoBlendBS, BlendFactor, 0xffffffff);
        pd3dContext->OMSetDepthStencilState(g_normalDepthStencilState, 0);*/
    }

    void initializeLPV(/*ID3D11DeviceContext* pd3dContext,*/ SimpleRT_RGB LPVAccumulate, SimpleRT_RGB LPVPropagate,
                       Matrix4f viewMatrix, Matrix4f mProjectionMatrix, SimpleRT RSMColorRT, SimpleRT RSMNormalRT, DepthRT shadowMapDS,
                       float fovy, float aspectRatio, float nearPlane, float farPlane, boolean useDirectional)
    {
        throw new UnsupportedOperationException();
        /*HRESULT hr;

        float BlendFactor[4] = { 1.0f, 1.0f, 1.0f, 1.0f };
        pd3dContext->OMSetBlendState(g_pMaxBlendBS, BlendFactor, 0xffffffff);
        pd3dContext->OMSetDepthStencilState(g_depthStencilStateDisableDepth, 0);

        ID3D11RenderTargetView* pRTVsNULL3[3] = { NULL, NULL, NULL };

        D3DXMATRIX inverseViewToLPVMatrix, viewToLPVMatrix;
        LPVPropagate->getLPVLightViewMatrices(viewMatrix, &viewToLPVMatrix, &inverseViewToLPVMatrix);

        //set the constant buffer
        float tanFovyHalf = tan(fovy/2.0f);
        float tanFovxHalf = tan(fovy/2.0f)*aspectRatio;

        D3D11_MAPPED_SUBRESOURCE MappedResource;
        pd3dContext->Map( g_reconPos, 0, D3D11_MAP_WRITE_DISCARD, 0, &MappedResource );
        RECONSTRUCT_POS* pcbTempR = ( RECONSTRUCT_POS* )MappedResource.pData;
        pcbTempR->farClip = farPlane;
        pd3dContext->Unmap( g_reconPos, 0 );
        pd3dContext->VSSetConstantBuffers( 6, 1, &g_reconPos );
        pd3dContext->PSSetConstantBuffers( 6, 1, &g_reconPos );

        D3DXMATRIX mProjInv;
        D3DXMatrixInverse(&mProjInv, NULL, &mProjectionMatrix);
        V(pd3dContext->Map( g_pcbInvProjMatrix, 0, D3D11_MAP_WRITE_DISCARD, 0, &MappedResource ) );
        CB_INVPROJ_MATRIX* pInvProjMat = ( CB_INVPROJ_MATRIX* )MappedResource.pData;
        D3DXMatrixTranspose( &pInvProjMat->m_InverseProjMatrix, &mProjInv );
        pd3dContext->Unmap( g_pcbInvProjMatrix, 0 );
        pd3dContext->VSSetConstantBuffers( 7, 1, &g_pcbInvProjMatrix );

        g_LPVViewport.Width  = (float)LPVPropagate->getWidth3D();
        g_LPVViewport.Height = (float)LPVPropagate->getHeight3D();

        unsigned int stride = 0;
        unsigned int offset = 0;
        ID3D11Buffer* buffer[] = { NULL };
        ID3D11ShaderResourceView* ppSRVsRSMs[3] = { RSMColorRT->get_pSRV(0), RSMNormalRT->get_pSRV(0), shadowMapDS->get_pSRV(0)};

        CB_LPV_INITIALIZE2* pcbLPVinitVS2;
        int numPoints = RSMColorRT->getWidth2D()*RSMColorRT->getHeight2D();
        if(g_useBilinearInit)
            numPoints *= 8;
        ID3D11GeometryShader* NULLGS = NULL;

        //initialize the LPV with the RSM from the direct light source
        ID3D11RenderTargetView* pRTVsLPV[3] = { LPVPropagate->getRed()->get_pRTV(0), LPVPropagate->getGreen()->get_pRTV(0), LPVPropagate->getBlue()->get_pRTV(0) };
        pd3dContext->OMSetRenderTargets( 3, pRTVsLPV, NULL );
        pd3dContext->RSSetViewports(1, &g_LPVViewport);

        //bind the RSMs to be read as textures
        pd3dContext->VSSetShaderResources( 0, 3, ppSRVsRSMs);

        //set the shaders
        if(g_useBilinearInit)
            pd3dContext->VSSetShader( g_pVSInitializeLPV_Bilinear, NULL, 0 );
        else
            pd3dContext->VSSetShader( g_pVSInitializeLPV, NULL, 0 );
        if(g_useBilinearInit)
            pd3dContext->GSSetShader( g_pGSInitializeLPV_Bilinear, NULL, 0 );
        else
            pd3dContext->GSSetShader( g_pGSInitializeLPV, NULL, 0 );
        pd3dContext->PSSetShader( g_pPSInitializeLPV, NULL, 0 );

        //set the constants
        pd3dContext->Map( g_pcbLPVinitVS, 0, D3D11_MAP_WRITE_DISCARD, 0, &MappedResource );
        CB_LPV_INITIALIZE* pcbLPVinitVS = ( CB_LPV_INITIALIZE* )MappedResource.pData;
        pcbLPVinitVS->RSMWidth = RSMNormalRT->getWidth2D();
        pcbLPVinitVS->RSMHeight = RSMNormalRT->getHeight2D();
        pcbLPVinitVS->lightStrength = g_directLightStrength;
        pd3dContext->Unmap( g_pcbLPVinitVS, 0 );
        pd3dContext->VSSetConstantBuffers( 0, 1, &g_pcbLPVinitVS );
        pd3dContext->PSSetConstantBuffers( 0, 1, &g_pcbLPVinitVS );

        pd3dContext->Map( g_pcbLPVinitialize_LPVDims, 0, D3D11_MAP_WRITE_DISCARD, 0, &MappedResource );
        CB_LPV_INITIALIZE3* pcbLPVinitVS3 = ( CB_LPV_INITIALIZE3* )MappedResource.pData;
        pcbLPVinitVS3->g_numCols = LPVPropagate->getNumCols();    //the number of columns in the flattened 2D LPV
        pcbLPVinitVS3->g_numRows = LPVPropagate->getNumRows();    //the number of columns in the flattened 2D LPV
        pcbLPVinitVS3->LPV2DWidth =  LPVPropagate->getWidth2D();  //the total width of the flattened 2D LPV
        pcbLPVinitVS3->LPV2DHeight = LPVPropagate->getHeight2D(); //the total height of the flattened 2D LPV
        pcbLPVinitVS3->LPV3DWidth = LPVPropagate->getWidth3D();   //the width of the LPV in 3D
        pcbLPVinitVS3->LPV3DHeight = LPVPropagate->getHeight3D(); //the height of the LPV in 3D
        pcbLPVinitVS3->LPV3DDepth = LPVPropagate->getDepth3D();   //the depth of the LPV in 3D
        pcbLPVinitVS3->fluxWeight = 4 * tanFovxHalf * tanFovyHalf / (RSM_RES * RSM_RES);
        pcbLPVinitVS3->fluxWeight *= 30.0f; //arbitrary scale
        pcbLPVinitVS3->useFluxWeight = useDirectional? 0 : 1;
        pd3dContext->Unmap( g_pcbLPVinitialize_LPVDims, 0 );
        pd3dContext->VSSetConstantBuffers( 5, 1, &g_pcbLPVinitialize_LPVDims );
        pd3dContext->GSSetConstantBuffers( 5, 1, &g_pcbLPVinitialize_LPVDims );

        pd3dContext->Map( g_pcbLPVinitVS2, 0, D3D11_MAP_WRITE_DISCARD, 0, &MappedResource );
        pcbLPVinitVS2 = ( CB_LPV_INITIALIZE2* )MappedResource.pData;
        D3DXMatrixTranspose( &pcbLPVinitVS2->g_ViewToLPV, &viewToLPVMatrix);
        D3DXMatrixTranspose( &pcbLPVinitVS2->g_LPVtoView, &inverseViewToLPVMatrix);

        D3DXVec3TransformNormal(&pcbLPVinitVS2->lightDirGridSpace, &D3DXVECTOR3(0, 0, 1), &viewToLPVMatrix);
        D3DXVec3Normalize(&pcbLPVinitVS2->lightDirGridSpace, &pcbLPVinitVS2->lightDirGridSpace);

        pcbLPVinitVS2->displacement = g_VPLDisplacement;
        pd3dContext->Unmap( g_pcbLPVinitVS2, 0 );
        pd3dContext->VSSetConstantBuffers( 1, 1, &g_pcbLPVinitVS2 );

        //render as many points as pixels in the RSM
        //each point will VTFs to determine its position in the grid, its flux and its normal
        //(to determine the point's position in the grid we first have to unproject the point to world space, then transform it to LPV space)
        //the point will then set its output position to write to the correct part of the output 2D texture
        //in the pixel shader the point will write out the SPH coefficients of the clamped cosine lobe
        pd3dContext->IASetVertexBuffers(0, 1, buffer, &stride, &offset);
        pd3dContext->IASetInputLayout(NULL);
        pd3dContext->IASetPrimitiveTopology(D3D11_PRIMITIVE_TOPOLOGY_POINTLIST);
        pd3dContext->Draw(numPoints,0);
        pd3dContext->IASetInputLayout( g_pMeshLayout );

        pd3dContext->OMSetRenderTargets( 3, pRTVsNULL3, NULL );
        ID3D11ShaderResourceView* ppSRVsNULL3[3] = { NULL, NULL, NULL };
        pd3dContext->VSSetShaderResources( 0, 3, ppSRVsNULL3);
        pd3dContext->GSSetShader( NULLGS, NULL, 0 );

        pd3dContext->OMSetBlendState(g_pNoBlendBS, BlendFactor, 0xffffffff);
        pd3dContext->OMSetDepthStencilState(g_normalDepthStencilState, 0);*/
    }

    void invokeHierarchyBasedPropagation(/*ID3D11DeviceContext* pd3dContext,*/ boolean useSingleLPV, int numHierarchyLevels, int numPropagationStepsLPV, int PropLevel,
                                         LPV_Hierarchy GV, LPV_Hierarchy GVColor, LPV_RGB_Hierarchy LPVAccumulate, LPV_RGB_Hierarchy LPVPropagate)
    {
        throw new UnsupportedOperationException();
        /*float ClearColor2[4] = {0.0f, 0.0f, 0.0f, 0.0f};

        if(!useSingleLPV)
        {
            //downsample the geometry volume at the beginning since it does not change
            //here we are downsampling from level 0 to level 1
            for(int i=0; i<numHierarchyLevels-1;i++)
            {
                GV->Downsample(pd3dContext,i,DOWNSAMPLE_MAX);
                GVColor->Downsample(pd3dContext,i,DOWNSAMPLE_MAX);
            }

            PropSpecs propAmounts[MAX_LEVELS];

            if(numHierarchyLevels == 2)
            {
                int individualSteps = numPropagationStepsLPV/3;
                propAmounts[0] = PropSpecs(individualSteps , individualSteps+ (numPropagationStepsLPV-individualSteps*3));
                propAmounts[1] = PropSpecs(individualSteps, 0);
            }
            else if(numHierarchyLevels == 3)
            {
                propAmounts[0] = PropSpecs(numPropagationStepsLPV/5,numPropagationStepsLPV/5);
                propAmounts[1] = PropSpecs(numPropagationStepsLPV/5,numPropagationStepsLPV/5);
                propAmounts[2] = PropSpecs(numPropagationStepsLPV/5,0);
            }
            else
            {
                for(int i=0;i<(numHierarchyLevels-1);i++)
                    propAmounts[i] = PropSpecs(numPropagationStepsLPV/(numHierarchyLevels*2-1),numPropagationStepsLPV/(numHierarchyLevels*2-1));
                propAmounts[numHierarchyLevels-1] = PropSpecs(numPropagationStepsLPV/(numHierarchyLevels*2-1),0);
            }


            propagateLightHierarchy(pd3dContext, LPVAccumulate, LPVPropagate, GV, GVColor, 0, propAmounts, numHierarchyLevels);

        }
        else
        {
            //downsample all the buffers, since we initialized on the finest level (since it looks like i have a bug if I initialize the lower level directly)

            for(int level=0; level<PropLevel; level++)
            {
                LPVPropagate->clearRenderTargetView(pd3dContext,ClearColor2,true, level+1);
                LPVPropagate->clearRenderTargetView(pd3dContext,ClearColor2,false, level+1);
                LPVAccumulate->clearRenderTargetView(pd3dContext,ClearColor2,true, level+1);
                LPVAccumulate->clearRenderTargetView(pd3dContext,ClearColor2,false, level+1);

                LPVPropagate->Downsample(pd3dContext,level,DOWNSAMPLE_AVERAGE);
                LPVAccumulate->Downsample(pd3dContext,level,DOWNSAMPLE_AVERAGE);
                GV->Downsample(pd3dContext,level,DOWNSAMPLE_MAX);
                GVColor->Downsample(pd3dContext,level,DOWNSAMPLE_MAX);
            }

            if(g_usePSPropagation)
            {
                pd3dContext->PSSetConstantBuffers( 0, 1, &g_pcbLPVinitVS );
                pd3dContext->PSSetConstantBuffers( 2, 1, &g_pcbLPVpropagateGather );
                pd3dContext->PSSetConstantBuffers( 6, 1, &g_pcbLPVpropagateGather2 );
            }
            else
            {
                pd3dContext->CSSetConstantBuffers( 0, 1, &g_pcbLPVinitVS );
                pd3dContext->CSSetConstantBuffers( 2, 1, &g_pcbLPVpropagateGather );
                pd3dContext->CSSetConstantBuffers( 6, 1, &g_pcbLPVpropagateGather2 );
            }

            for(int i=0; i< numPropagationStepsLPV; i++)
                propagateLPV(pd3dContext, i, LPVPropagate->m_collection[PropLevel], LPVAccumulate->m_collection[PropLevel], GV->m_collection[PropLevel], GVColor->m_collection[PropLevel] );
        }*/
    }

    void invokeCascadeBasedPropagation(/*ID3D11DeviceContext* pd3dContext,*/ boolean useSingleLPV, int propLevel, LPV_RGB_Cascade LPVAccumulate, LPV_RGB_Cascade LPVPropagate,
                                       LPV_Cascade GV, LPV_Cascade GVColor, int num_iterations)
    {
        if(g_usePSPropagation)
        {
            /*pd3dContext->PSSetConstantBuffers( 2, 1, &g_pcbLPVpropagateGather );
            pd3dContext->PSSetConstantBuffers( 6, 1, &g_pcbLPVpropagateGather2 );*/
            gl.glBindBufferBase(GLenum.GL_UNIFORM_BUFFER,2, g_pcbLPVpropagateGather.getBuffer());
            gl.glBindBufferBase(GLenum.GL_UNIFORM_BUFFER,6, g_pcbLPVpropagateGather2.getBuffer());
        }
        else
        {
            /*pd3dContext->CSSetConstantBuffers( 2, 1, &g_pcbLPVpropagateGather );
            pd3dContext->CSSetConstantBuffers( 6, 1, &g_pcbLPVpropagateGather2 );*/
            gl.glBindBufferBase(GLenum.GL_UNIFORM_BUFFER,2, g_pcbLPVpropagateGather.getBuffer());
            gl.glBindBufferBase(GLenum.GL_UNIFORM_BUFFER,6, g_pcbLPVpropagateGather2.getBuffer());
        }

        if(useSingleLPV)
        {
            for(int iteration=0; iteration<num_iterations; iteration++)
                propagateLPV(/*pd3dContext,*/iteration, LPVPropagate.get(propLevel), LPVAccumulate.get(propLevel), GV.getRenderTarget(propLevel), GVColor.getRenderTarget(propLevel));
            return;
        }

        for(int level=0; level<LPVPropagate.getNumLevels(); level++)
            for(int iteration=0; iteration<num_iterations; iteration++)
                propagateLPV(/*pd3dContext,*/iteration, LPVPropagate.get(level), LPVAccumulate.get(level), GV.getRenderTarget(level), GVColor.getRenderTarget(level));
    }

    void propagateLPV(/*ID3D11DeviceContext* pd3dContext,*/ int iteration, SimpleRT_RGB LPVPropagate, SimpleRT_RGB LPVAccumulate, SimpleRT GV, SimpleRT GVColor )
    {
        throw new UnsupportedOperationException();

        /*if(g_usePSPropagation)
            return propagateLPV_PixelShaderPath(pd3dContext, iteration, LPVPropagate, LPVAccumulate, GV, GVColor );

        bool bAccumulateSeparately = false;
        if(LPVAccumulate->getRedFront()->getNumChannels()==1 && LPVAccumulate->getRedFront()->getNumRTs()==4) bAccumulateSeparately = true;

        D3D11_MAPPED_SUBRESOURCE MappedResource;

        //swap buffers so that the data that we just wrote goes into the back buffer. we will read from the backbuffer and write to the frontbuffer
        LPVPropagate->swapBuffers();

        //clear the front buffer because we are going to be replacing its contents completely with the propagated wavefront from backbuffer
        //we are not doing a clear here since it is faster for my app to just update all the voxels in the shader
        //float ClearColorLPV[4] = {0.0f, 0.0f, 0.0f, 0.0f};
        //LPVPropagate->clearRenderTargetView(pd3dContext, ClearColorLPV, true);

        //make sure that the propagate LPV is actually a float4, since the other paths are not implemented (and were not needed since we dont need to read and write to LPVPropagate)
        assert(LPVPropagate->getRedFront()->getNumRTs()==1);
        assert(GV->getNumRTs()==1);
        assert(GVColor->getNumRTs()==1);


        UINT initCounts = 0;

        //set the shader
        if(g_useOcclusion || g_useMultipleBounces)
            pd3dContext->CSSetShader( g_pCSPropagateLPV, NULL, 0 );
        else
            pd3dContext->CSSetShader( g_pCSPropagateLPVSimple, NULL, 0 );

        //set the unordered views that we are going to be writing to
        ID3D11UnorderedAccessView* ppUAV[3] = { LPVPropagate->getRedFront()->get_pUAV(0), LPVPropagate->getGreenFront()->get_pUAV(0) , LPVPropagate->getBlueFront()->get_pUAV(0)};
        pd3dContext->CSSetUnorderedAccessViews( 0, 3, ppUAV, &initCounts );

        //set the LPV backbuffers as textures to read from
        ID3D11ShaderResourceView* ppSRVsLPV[5] = { LPVPropagate->getRedBack()->get_pSRV(0), LPVPropagate->getGreenBack()->get_pSRV(0), LPVPropagate->getBlueBack()->get_pSRV(0),
            GV->get_pSRV(0), GVColor->get_pSRV(0)};
        pd3dContext->CSSetShaderResources( 3, 5, ppSRVsLPV);

        if(!bAccumulateSeparately)
        {
            LPVAccumulate->swapBuffers();

            //set the textures and UAVs for the accumulation buffer
            ID3D11UnorderedAccessView* ppUAVAccum[3] = { LPVAccumulate->getRedFront()->get_pUAV(0), LPVAccumulate->getGreenFront()->get_pUAV(0) , LPVAccumulate->getBlueFront()->get_pUAV(0)};
            pd3dContext->CSSetUnorderedAccessViews( 3, 3, ppUAVAccum, &initCounts );

            //set the LPV backbuffers as textures to read from
            ID3D11ShaderResourceView* ppSRVsLPVAccum[3] = { LPVAccumulate->getRedBack()->get_pSRV(0), LPVAccumulate->getGreenBack()->get_pSRV(0), LPVAccumulate->getBlueBack()->get_pSRV(0)};
            pd3dContext->CSSetShaderResources( 8, 3, ppSRVsLPVAccum);
        }

        //update constant buffer
        pd3dContext->Map( g_pcbLPVpropagate, 0, D3D11_MAP_WRITE_DISCARD, 0, &MappedResource );
        CB_LPV_PROPAGATE* pcbLPVinitVS3 = ( CB_LPV_PROPAGATE* )MappedResource.pData;
        pcbLPVinitVS3->g_numCols = LPVAccumulate->getNumCols();    //the number of columns in the flattened 2D LPV
        pcbLPVinitVS3->g_numRows = LPVAccumulate->getNumRows();    //the number of columns in the flattened 2D LPV
        pcbLPVinitVS3->LPV2DWidth =  LPVAccumulate->getWidth2D();  //the total width of the flattened 2D LPV
        pcbLPVinitVS3->LPV2DHeight = LPVAccumulate->getHeight2D(); //the total height of the flattened 2D LPV
        pcbLPVinitVS3->LPV3DWidth = LPVAccumulate->getWidth3D();   //the width of the LPV in 3D
        pcbLPVinitVS3->LPV3DHeight = LPVAccumulate->getHeight3D(); //the height of the LPV in 3D
        pcbLPVinitVS3->LPV3DDepth = LPVAccumulate->getDepth3D();   //the depth of the LPV in 3D
        pcbLPVinitVS3->b_accumulate = !bAccumulateSeparately;
        pd3dContext->Unmap( g_pcbLPVpropagate, 0 );
        pd3dContext->CSSetConstantBuffers( 5, 1, &g_pcbLPVpropagate ); //have to update this at each iteration

        if(iteration<2)
        {
            pd3dContext->Map( g_pcbGV, 0, D3D11_MAP_WRITE_DISCARD, 0, &MappedResource );
            CB_GV* pcbGV = ( CB_GV* )MappedResource.pData;

            if(iteration<1 || !g_useOcclusion) pcbGV->useGVOcclusion = 0;
            else pcbGV->useGVOcclusion = 1;

            pcbGV->useMultipleBounces = g_useMultipleBounces;
            pcbGV->fluxAmplifier = g_fluxAmplifier;
            pcbGV->reflectedLightAmplifier = g_reflectedLightAmplifier;
            pcbGV->occlusionAmplifier = g_occlusionAmplifier;

            //in the first step we can optionally copy the propagation LPV data into the accumulation LPV
            if(iteration<1) pcbGV->copyPropToAccum = 1; else pcbGV->copyPropToAccum = 0;

            pd3dContext->Unmap( g_pcbGV, 0 );
            pd3dContext->CSSetConstantBuffers( 3, 1, &g_pcbGV );
        }

        pd3dContext->Dispatch( LPVPropagate->getWidth3D()/X_BLOCK_SIZE,  LPVPropagate->getHeight3D()/Y_BLOCK_SIZE,  LPVPropagate->getDepth3D()/Z_BLOCK_SIZE );

        ID3D11UnorderedAccessView* ppUAVssNULL3[3] = { NULL, NULL, NULL};
        pd3dContext->CSSetUnorderedAccessViews( 0, 3, ppUAVssNULL3, &initCounts );
        ID3D11ShaderResourceView* ppSRVsNULL5[5] = { NULL, NULL, NULL, NULL, NULL };
        pd3dContext->CSSetShaderResources( 3, 5, ppSRVsNULL5);


        if(!bAccumulateSeparately)
        {
            //unset the textures and UAVs for the accumulation buffer
            ID3D11UnorderedAccessView* ppUAVAccumNULL[3] = {NULL, NULL, NULL};
            pd3dContext->CSSetUnorderedAccessViews( 3, 3, ppUAVAccumNULL, &initCounts );

            //set the LPV backbuffers as textures to read from
            ID3D11ShaderResourceView* ppSRVsLPVAccumNULL[3] = { NULL, NULL, NULL};
            pd3dContext->CSSetShaderResources( 8, 3, ppSRVsLPVAccumNULL);
        }
        else
        {
            //after propagation we want to accumulate the propagated results into a separate accumulation buffer
            //note: this code is very specific to either having 3 float4 RGB textures, or 12 float textures
            //if you need more generality you will need to add it to the calculation of batches and accumulateLPV
            int numBatches = 1;
            if(LPVAccumulate->getRedFront()->getNumChannels()==1 && LPVAccumulate->getRedFront()->getNumRTs()==4) numBatches = 2;

            for(int batch=0; batch<numBatches; batch++)
                accumulateLPVs(pd3dContext, batch, LPVPropagate, LPVAccumulate );
        }*/
    }
}
