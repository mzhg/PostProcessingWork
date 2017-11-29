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
    final Vector3f g_lightPosDest = new Vector3f();
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


    CB_LPV_PROPAGATE_GATHER      g_LPVPropagateGather = null;
    CB_LPV_PROPAGATE_GATHER2     g_LPVPropagateGather2 = null;


    int           g_pDefaultSampler;
    int           g_pComparisonSampler;

    GLSLProgramPipeline g_Program;

    GLFuncProvider gl;

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
        g_lightPosDest.set(g_lightPos);
    }

    void DrawScene(/*ID3D11Device* pd3dDevice,  ID3D11DeviceContext* pd3dContext*/ ){
        // TODO
    }

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
}
