package jet.opengl.demos.nvidia.illumination;

import com.nvidia.developer.opengl.app.NvSampleApp;
import com.nvidia.developer.opengl.models.sdkmesh.SDKmesh;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.ReadableVector3f;
import org.lwjgl.util.vector.Vector;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4i;

import java.nio.ByteBuffer;

import jet.opengl.demos.intel.cput.ID3D11InputLayout;
import jet.opengl.demos.scene.CameraData;
import jet.opengl.postprocessing.buffer.BufferGL;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.shader.ShaderProgram;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.util.CacheBuffer;

/**
 * Created by Administrator on 2017/11/22 0022.
 */

public class DiffuseGlobalIllumination extends NvSampleApp {
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

    GLFuncProvider gl;

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
}
