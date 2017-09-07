package jet.opengl.demos.nvidia.face.sample;

import com.nvidia.developer.opengl.app.NvSampleApp;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import jet.opengl.demos.nvidia.face.libs.GFSDK_FaceWorks;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.util.CommonUtil;

/**
 * Created by mazhen'gui on 2017/9/7.
 */

public class FaceWorkDemo extends NvSampleApp {
    static float	g_normalStrength = 1.0f;
    static float	g_glossSkin = 0.35f;
    static float	g_glossEye = 0.9f;
    static float	g_specReflectanceSkinDefault = 0.05f;
    static float	g_specReflectanceEye = 0.05f;
    static float	g_rgbDeepScatterEye[] = { 1.0f, 0.3f, 0.3f };
    static float	g_irisRadiusSource = 0.200f;	// Radius of iris in iris texture (in UV units)
    static float	g_irisRadiusDest = 0.205f;		// Radius of iris in schlera texture (in UV units)
    static float	g_irisEdgeHardness = 30.0f;		// Controls hardness/softness of iris edge
    static float	g_irisDilation = 0.0f;			// How much the iris is dilated
    static float	g_specReflectanceHair = 0.05f;
    static float	g_glossHair = 0.25f;

    boolean g_ShowHelp = false;
    boolean g_ShowGUI = true;
    boolean g_ShowText = true;
    boolean g_bWireframe = false;
    boolean g_bShowPerf = true;
    boolean g_bCopyPerfToClipboard = false;
    boolean g_bTessellation = true;

    static final Vector4f s_rectViewBuffer = new Vector4f(10, 50, 512, 512);



// Scene resources

    final float					g_FOV = 0.5f;		// vertical fov, radians

    CSceneDigitalIra			g_sceneDigitalIra;
    CSceneTest					g_sceneTest;
    CSceneHand					g_sceneHand;
    CSceneDragon				g_sceneDragon;
    CSceneLPSHead				g_sceneLPSHead;
    CSceneManjaladon			g_sceneManjaladon;
    CSceneWarriorHead			g_sceneWarriorHead;
    CScene  					g_pSceneCur = null;

    CBackground					g_bkgndBlack;
    CBackground					g_bkgndCharcoal;
    CBackground					g_bkgndForest;
    CBackground					g_bkgndNight;
    CBackground					g_bkgndTunnel;
    CBackground 				g_pBkgndCur = null;

    CMesh						g_meshFullscreen;

    Texture2D                   g_pSrvCurvatureLUT = null;
    static final float			g_curvatureRadiusMinLUT = 0.1f;		// cm
    static final float			g_curvatureRadiusMaxLUT = 10.0f;	// cm

    Texture2D	                g_pSrvShadowLUT = null;
    static final float			g_shadowWidthMinLUT = 0.8f;		// cm
    static final float			g_shadowWidthMaxLUT = 10.0f;	// cm

    /*ID3D11DepthStencilState *	g_pDssDepthTest = nullptr;
    ID3D11DepthStencilState *	g_pDssNoDepthTest = nullptr;
    ID3D11DepthStencilState *	g_pDssNoDepthWrite = nullptr;
    ID3D11RasterizerState *		g_pRsSolid = nullptr;
    ID3D11RasterizerState *		g_pRsWireframe = nullptr;
    ID3D11RasterizerState *		g_pRsSolidDoubleSided = nullptr;
    ID3D11BlendState *			g_pBsAlphaBlend = nullptr;*/

    CShadowMap					g_shadowmap;
    CVarShadowMap				g_vsm;

    Texture2D               	g_pRtvNonSrgb = null;

    float						g_radYawDirectionalLight = 0.70f;
    float						g_radPitchDirectionalLight = 0.40f;
    final Vector3f              g_vecDirectionalLight = new Vector3f();
    final Vector3f				g_rgbDirectionalLight = new Vector3f();
    float						g_directionalLightBrightness = 1.0f;

    float						g_vsmBlurRadius = 0.15f;		// One-sigma radius, in cm
    float						g_vsmMinVariance = 1e-4f;
    float						g_shadowSharpening = 10.0f;

    float						g_sssBlurRadius = 0.27f;		// One-sigma radius of widest Gaussian, in cm

    float						g_tessErrPx = 0.5f;				// Target tessellation error, in pixels

    float						g_deepScatterIntensity = 0.5f;	// Multiplier on whole deep scattering result
    float						g_deepScatterRadius = 0.6f;		// One-sigma radius of deep scatter Gaussian, in cm
    float						g_deepScatterNormalOffset = -0.0007f;	// Normal offset for shadow lookup to calculate thickness
    float						g_deepScatterShadowRadius = 1.1f;		// Poisson disk radius, in cm

    float						g_debugSlider0 = 0.0f;
    float						g_debugSlider1 = 0.0f;
    float						g_debugSlider2 = 0.0f;
    float						g_debugSlider3 = 0.0f;

    final CShaderManager g_shdmgr = new CShaderManager();
    final Matrix4f              m_proj = new Matrix4f();

    @Override
    protected void initRendering() {
        // Init FaceWorks
        GFSDK_FaceWorks.GFSDK_FaceWorks_Init();

//        V_RETURN(CreateFullscreenMesh(pDevice, &g_meshFullscreen));  TODO

        // Load scenes
        g_sceneDigitalIra.Init();
        g_sceneTest.Init();
        g_sceneHand.Init();
        g_sceneDragon.Init();
        g_sceneLPSHead.Init();
        g_sceneManjaladon.Init();
        g_sceneWarriorHead.Init();
        g_pSceneCur = g_sceneDigitalIra;

        // Load backgrounds
        g_bkgndBlack.Init("HDREnvironments\\black_cube.dds", "HDREnvironments\\black_cube.dds", "HDREnvironments\\black_cube.dds");
        g_bkgndCharcoal.Init("HDREnvironments\\charcoal_cube.dds", "HDREnvironments\\charcoal_cube.dds", "HDREnvironments\\charcoal_cube.dds");
        g_bkgndForest.Init("HDREnvironments\\forest_env_cubemap.dds", "HDREnvironments\\forest_diffuse_cubemap.dds", "HDREnvironments\\forest_spec_cubemap.dds", 0.25f);
        g_bkgndNight.Init("HDREnvironments\\night_env_cubemap.dds", "HDREnvironments\\night_diffuse_cubemap.dds", "HDREnvironments\\night_spec_cubemap.dds");
        g_bkgndTunnel.Init("HDREnvironments\\tunnel_env_cubemap.dds", "HDREnvironments\\tunnel_diffuse_cubemap.dds", "HDREnvironments\\tunnel_spec_cubemap.dds", 0.5f);
        g_pBkgndCur = g_bkgndCharcoal;

        // Load textures

//        V_RETURN(DXUTFindDXSDKMediaFileCch(strPath, dim(strPath), L"curvatureLUT.bmp"));
//        V_RETURN(LoadTexture(strPath, pDevice, &g_pSrvCurvatureLUT, LT_Linear));
        g_pSrvCurvatureLUT = CScene.loadTexture("curvatureLUT.bmp");

//        V_RETURN(DXUTFindDXSDKMediaFileCch(strPath, dim(strPath), L"shadowLUT.bmp"));
//        V_RETURN(LoadTexture(strPath, pDevice, &g_pSrvShadowLUT, LT_None));
        g_pSrvCurvatureLUT = CScene.loadTexture("shadowLUT.bmp");

        // Load shaders
        g_shdmgr.Init(/*pDevice*/);

        // Create depth-stencil states

        /*D3D11_DEPTH_STENCIL_DESC dssDepthTestDesc =
                {
                        true,							// DepthEnable
                        D3D11_DEPTH_WRITE_MASK_ALL,
                        D3D11_COMPARISON_LESS_EQUAL,
                };
        V_RETURN(pDevice->CreateDepthStencilState(&dssDepthTestDesc, &g_pDssDepthTest));

        D3D11_DEPTH_STENCIL_DESC dssNoDepthTestDesc =
                {
                        false,							// DepthEnable
                };
        V_RETURN(pDevice->CreateDepthStencilState(&dssNoDepthTestDesc, &g_pDssNoDepthTest));

        D3D11_DEPTH_STENCIL_DESC dssNoDepthWriteDesc =
                {
                        true,							// DepthEnable
                        D3D11_DEPTH_WRITE_MASK_ZERO,
                        D3D11_COMPARISON_LESS_EQUAL,
                };
        V_RETURN(pDevice->CreateDepthStencilState(&dssNoDepthWriteDesc, &g_pDssNoDepthWrite));

        // Create rasterizer states

        D3D11_RASTERIZER_DESC rssSolidDesc =
                {
                        D3D11_FILL_SOLID,
                        D3D11_CULL_BACK,
                        true,							// FrontCounterClockwise
                        0, 0, 0,						// depth bias
                        true,							// DepthClipEnable
                        false,							// ScissorEnable
                        true,							// MultisampleEnable
                };
        V_RETURN(pDevice->CreateRasterizerState(&rssSolidDesc, &g_pRsSolid));

        D3D11_RASTERIZER_DESC rssWireframeDesc =
                {
                        D3D11_FILL_WIREFRAME,
                        D3D11_CULL_BACK,
                        true,							// FrontCounterClockwise
                        0, 0, 0,						// depth bias
                        true,							// DepthClipEnable
                        false,							// ScissorEnable
                        true,							// MultisampleEnable
                };
        V_RETURN(pDevice->CreateRasterizerState(&rssWireframeDesc, &g_pRsWireframe));

        D3D11_RASTERIZER_DESC rssSolidDoubleSidedDesc =
                {
                        D3D11_FILL_SOLID,
                        D3D11_CULL_NONE,
                        true,							// FrontCounterClockwise
                        0, 0, 0,						// depth bias
                        true,							// DepthClipEnable
                        false,							// ScissorEnable
                        true,							// MultisampleEnable
                };
        V_RETURN(pDevice->CreateRasterizerState(&rssSolidDoubleSidedDesc, &g_pRsSolidDoubleSided));

        // Initialize the blend state for alpha-blending

        D3D11_BLEND_DESC bsDesc =
                {
                        false, false,
                        {
                                true,
                                D3D11_BLEND_SRC_ALPHA,
                                D3D11_BLEND_INV_SRC_ALPHA,
                                D3D11_BLEND_OP_ADD,
                                D3D11_BLEND_SRC_ALPHA,
                                D3D11_BLEND_INV_SRC_ALPHA,
                                D3D11_BLEND_OP_ADD,
                                D3D11_COLOR_WRITE_ENABLE_ALL,
                        },
                };

        V_RETURN(pDevice->CreateBlendState(&bsDesc, &g_pBsAlphaBlend));*/

        // Create shadow map
        g_shadowmap.Init(1024);
        g_vsm.Init(1024);

        // Set up GPU profiler
//        V_RETURN(g_gpuProfiler.Init(pDevice));

        // Set up directional light
        g_rgbDirectionalLight.set(0.984f, 1.0f, 0.912f);	// Note: linear RGB space
    }

    @Override
    public void display() {
        /*g_gpuProfiler.BeginFrame(pd3dContext);

        // Set up directional light and shadow map
        g_vecDirectionalLight = XMVectorSet(
                sinf(g_radYawDirectionalLight) * cosf(g_radPitchDirectionalLight),
                sinf(g_radPitchDirectionalLight),
                cosf(g_radYawDirectionalLight) * cosf(g_radPitchDirectionalLight),
                0.0f);


        XMStoreFloat3(&g_shadowmap.m_vecLight, g_vecDirectionalLight);

        g_pSceneCur->GetBounds(&g_shadowmap.m_posMinScene, &g_shadowmap.m_posMaxScene);
        g_shadowmap.UpdateMatrix();

        // Calculate UV space blur radius using shadow map's diameter, to attempt to maintain a
        // constant blur radius in world space.
        g_vsm.m_blurRadius = g_vsmBlurRadius / min(g_shadowmap.m_vecDiam.x, g_shadowmap.m_vecDiam.y);

        // Get the list of stuff to draw for the current scene
        std::vector<MeshToDraw> meshesToDraw;
        g_pSceneCur->GetMeshesToDraw(&meshesToDraw);
        int cMeshToDraw = int(meshesToDraw.size());

        // Set up whole-frame textures, constant buffers, etc.

        bool bDebug = (GetAsyncKeyState(' ') != 0);
        float debug = bDebug ? 1.0f : 0.0f;

        CbufDebug cbDebug =
                {
                        debug,
                        g_debugSlider0,
                        g_debugSlider1,
                        g_debugSlider2,
                        g_debugSlider3,
                };

        // Get the view and proj matrices from the camera
        XMMATRIX matView = g_pSceneCur->Camera()->GetViewMatrix();
        XMMATRIX matProj = g_pSceneCur->Camera()->GetProjMatrix();
        XMFLOAT4X4 matWorldToClip; XMStoreFloat4x4(&matWorldToClip, matView * matProj);
        XMFLOAT4 posCamera; XMStoreFloat4(&posCamera, g_pSceneCur->Camera()->GetEyePt());

        XMMATRIX matViewAxes(
                matView.r[0],
                matView.r[1],
                matView.r[2],
                XMVectorSelect(matView.r[3], g_XMZero, g_XMSelect1110));
        XMMATRIX matViewAxesInverse = XMMatrixInverse(nullptr, matViewAxes);
        XMMATRIX matProjInverse = XMMatrixInverse(nullptr, matProj);
        XMFLOAT4X4 matClipToWorldAxes; XMStoreFloat4x4(&matClipToWorldAxes, matProjInverse * matViewAxesInverse);

        // Calculate tessellation scale using screen-space error.  The 0.41 is a curve fitting constant.
        // Error goes inversely as the square of the tess factor (for edges that are small wrt curvature),
        // so tess factor goes inversely as the square root of the target error.
        float tessScale = 0.41f / sqrtf(g_tessErrPx * 2.0f * tanf(0.5f * g_FOV) / float(DXUTGetWindowHeight()));

        XMFLOAT4 vecDirectionalLight; XMStoreFloat4(&vecDirectionalLight, g_vecDirectionalLight);
        XMFLOAT4 rgbDirectionalLight; XMStoreFloat4(&rgbDirectionalLight, g_directionalLightBrightness * g_rgbDirectionalLight);

        CbufFrame cbFrame =
                {
                        matWorldToClip,
                        posCamera,
                        vecDirectionalLight,
                        rgbDirectionalLight,
                        g_shadowmap.m_matWorldToUvzw,
                        {
                                XMFLOAT4(g_shadowmap.m_matWorldToUvzNormal.m[0]),
                                XMFLOAT4(g_shadowmap.m_matWorldToUvzNormal.m[1]),
                                XMFLOAT4(g_shadowmap.m_matWorldToUvzNormal.m[2]),
                        },
                        g_vsmMinVariance,
                        g_shadowSharpening,
                        tessScale,
                        g_deepScatterIntensity,
                        g_deepScatterNormalOffset,
                        g_pBkgndCur->m_exposure,
                };

        g_shdmgr.InitFrame(
                pd3dContext,
                &cbDebug,
        &cbFrame,
                g_pBkgndCur->m_pSrvCubeDiff,
                g_pBkgndCur->m_pSrvCubeSpec,
                g_pSrvCurvatureLUT,
                g_pSrvShadowLUT);

        // Clear the shadow map
        pd3dContext->ClearDepthStencilView(g_shadowmap.m_pDsv, D3D11_CLEAR_DEPTH, 1.0f, 0);

        g_shadowmap.BindRenderTarget(pd3dContext);
        pd3dContext->OMSetDepthStencilState(g_pDssDepthTest, 0);
        pd3dContext->RSSetState(g_pRsSolid);

        // Draw shadow map

        g_shdmgr.BindShadow(pd3dContext, g_shadowmap.m_matWorldToClip);
        for (int i = 0; i < cMeshToDraw; ++i)
        {
            meshesToDraw[i].m_pMesh->Draw(pd3dContext);
        }

        g_vsm.UpdateFromShadowMap(g_shadowmap, pd3dContext);
        g_vsm.GaussianBlur(pd3dContext);

        g_gpuProfiler.Timestamp(pd3dContext, GTS_ShadowMap);

        // Bind the non-SRGB render target view, for rendering with tone mapping
        // (which outputs in SRGB gamma space natively)
        V(DXUTSetupD3D11Views(pd3dContext));
        pd3dContext->OMSetRenderTargets(1, &g_pRtvNonSrgb, DXUTGetD3D11DepthStencilView());
        pd3dContext->RSSetState(g_bWireframe ? g_pRsWireframe : g_pRsSolid);

        // Clear the screen
        float rgbaZero[4] = {};
        pd3dContext->ClearRenderTargetView(DXUTGetD3D11RenderTargetView(), rgbaZero);
        pd3dContext->ClearDepthStencilView(DXUTGetD3D11DepthStencilView(), D3D11_CLEAR_DEPTH | D3D11_CLEAR_STENCIL, 1.0f, 0);

        g_shdmgr.BindShadowTextures(
                pd3dContext,
                g_shadowmap.m_pSrv,
                g_vsm.m_pSrv);

        SHDFEATURES features = 0;
        if (g_bTessellation)
            features |= SHDFEAT_Tessellation;

        switch (g_renderMethod)
        {
            case RM_None:
            {
                // Note: two loops for skin and eye materials so we can have GPU timestamps around
                // each shader individually. Gack.

                // Draw skin shaders
                for (int i = 0; i < cMeshToDraw; ++i)
                {
                    if (meshesToDraw[i].m_pMtl->m_shader == SHADER_Skin)
                    {
                        g_shdmgr.BindMaterial(pd3dContext, features, meshesToDraw[i].m_pMtl);
                        meshesToDraw[i].m_pMesh->Draw(pd3dContext);
                    }
                }
                g_gpuProfiler.Timestamp(pd3dContext, GTS_Skin);

                // Draw eye shaders
                for (int i = 0; i < cMeshToDraw; ++i)
                {
                    if (meshesToDraw[i].m_pMtl->m_shader == SHADER_Eye)
                    {
                        g_shdmgr.BindMaterial(pd3dContext, features, meshesToDraw[i].m_pMtl);
                        meshesToDraw[i].m_pMesh->Draw(pd3dContext);
                    }
                }
                g_gpuProfiler.Timestamp(pd3dContext, GTS_Eyes);
            }
            break;

            case RM_SSS:
            case RM_SSSAndDeep:
            {
                GFSDK_FaceWorks_SSSConfig sssConfigSkin = {}, sssConfigEye;
                sssConfigSkin.m_diffusionRadius = max(g_sssBlurRadius, 0.01f);
                sssConfigSkin.m_diffusionRadiusLUT = 0.27f;

                sssConfigSkin.m_curvatureRadiusMinLUT = g_curvatureRadiusMinLUT;
                sssConfigSkin.m_curvatureRadiusMaxLUT = g_curvatureRadiusMaxLUT;
                sssConfigSkin.m_shadowWidthMinLUT = g_shadowWidthMinLUT;
                sssConfigSkin.m_shadowWidthMaxLUT = g_shadowWidthMaxLUT;

                // Filter width is ~6 times the Gaussian sigma
                sssConfigSkin.m_shadowFilterWidth = max(6.0f * g_vsmBlurRadius, 0.01f);

                sssConfigEye = sssConfigSkin;

                GFSDK_FaceWorks_DeepScatterConfig deepScatterConfigSkin = {}, deepScatterConfigEye;
                deepScatterConfigSkin.m_radius = max(g_deepScatterRadius, 0.01f);
                deepScatterConfigSkin.m_shadowProjType = GFSDK_FaceWorks_ParallelProjection;
                memcpy(&deepScatterConfigSkin.m_shadowProjMatrix, &g_shadowmap.m_matProj, 16 * sizeof(float));
                deepScatterConfigSkin.m_shadowFilterRadius = g_deepScatterShadowRadius / min(g_shadowmap.m_vecDiam.x, g_shadowmap.m_vecDiam.y);

                deepScatterConfigEye = deepScatterConfigSkin;

                features |= SHDFEAT_SSS;
                if (g_renderMethod == RM_SSSAndDeep)
                    features |= SHDFEAT_DeepScatter;

                GFSDK_FaceWorks_ErrorBlob faceworksErrors = {};

                // Note: two loops for skin and eye materials so we can have GPU timestamps around
                // each shader individually. Gack.

                // Draw skin shaders
                for (int i = 0; i < cMeshToDraw; ++i)
                {
                    if (meshesToDraw[i].m_pMtl->m_shader == SHADER_Skin)
                    {
                        sssConfigSkin.m_normalMapSize = meshesToDraw[i].m_normalMapSize;
                        sssConfigSkin.m_averageUVScale = meshesToDraw[i].m_averageUVScale;
                        NV(GFSDK_FaceWorks_WriteCBDataForSSS(
                                &sssConfigSkin, reinterpret_cast<GFSDK_FaceWorks_CBData *>(&meshesToDraw[i].m_pMtl->m_constants[4]), &faceworksErrors));
                        NV(GFSDK_FaceWorks_WriteCBDataForDeepScatter(
                                &deepScatterConfigSkin, reinterpret_cast<GFSDK_FaceWorks_CBData *>(&meshesToDraw[i].m_pMtl->m_constants[4]), &faceworksErrors));

                        g_shdmgr.BindMaterial(pd3dContext, features, meshesToDraw[i].m_pMtl);
                        meshesToDraw[i].m_pMesh->Draw(pd3dContext);
                    }
                }
                g_gpuProfiler.Timestamp(pd3dContext, GTS_Skin);

                // Draw eye shaders
                for (int i = 0; i < cMeshToDraw; ++i)
                {
                    if (meshesToDraw[i].m_pMtl->m_shader == SHADER_Eye)
                    {
                        sssConfigEye.m_normalMapSize = meshesToDraw[i].m_normalMapSize;
                        sssConfigEye.m_averageUVScale = meshesToDraw[i].m_averageUVScale;
                        NV(GFSDK_FaceWorks_WriteCBDataForSSS(
                                &sssConfigEye, reinterpret_cast<GFSDK_FaceWorks_CBData *>(&meshesToDraw[i].m_pMtl->m_constants[12]), &faceworksErrors));
                        NV(GFSDK_FaceWorks_WriteCBDataForDeepScatter(
                                &deepScatterConfigEye, reinterpret_cast<GFSDK_FaceWorks_CBData *>(&meshesToDraw[i].m_pMtl->m_constants[12]), &faceworksErrors));

                        g_shdmgr.BindMaterial(pd3dContext, features, meshesToDraw[i].m_pMtl);
                        meshesToDraw[i].m_pMesh->Draw(pd3dContext);
                    }
                }
                g_gpuProfiler.Timestamp(pd3dContext, GTS_Eyes);

                if (faceworksErrors.m_msg)
                {
                    #if defined(_DEBUG)
                    wchar_t msg[512];
                    _snwprintf_s(msg, dim(msg),
                            L"FaceWorks rendering error:\n%hs", faceworksErrors.m_msg);
                    DXUTTrace(__FILE__, __LINE__, E_FAIL, msg, true);
                    #endif
                    GFSDK_FaceWorks_FreeErrorBlob(&faceworksErrors);
                }
            }
            break;

            case RM_ViewCurvature:
            {
                // Calculate scale-bias for mapping curvature to LUT coordinate,
                // given the min and max curvature encoded in the LUT.

                float curvatureScale = 1.0f / (1.0f / g_curvatureRadiusMinLUT - 1.0f / g_curvatureRadiusMaxLUT);
                float curvatureBias = 1.0f / (1.0f - g_curvatureRadiusMaxLUT / g_curvatureRadiusMinLUT);

                g_shdmgr.BindCurvature(pd3dContext, curvatureScale, curvatureBias);

                for (int i = 0; i < cMeshToDraw; ++i)
                {
                    meshesToDraw[i].m_pMesh->Draw(pd3dContext);
                }
            }
            break;

            case RM_ViewThickness:
            {
                GFSDK_FaceWorks_DeepScatterConfig deepScatterConfigSkin = {};
                deepScatterConfigSkin.m_radius = max(g_deepScatterRadius, 0.01f);
                deepScatterConfigSkin.m_shadowProjType = GFSDK_FaceWorks_ParallelProjection;
                memcpy(&deepScatterConfigSkin.m_shadowProjMatrix, &g_shadowmap.m_matProj, 16 * sizeof(float));
                deepScatterConfigSkin.m_shadowFilterRadius = g_deepScatterShadowRadius / min(g_shadowmap.m_vecDiam.x, g_shadowmap.m_vecDiam.y);

                GFSDK_FaceWorks_CBData faceworksCBData = {};
                GFSDK_FaceWorks_ErrorBlob faceworksErrors = {};

                NV(GFSDK_FaceWorks_WriteCBDataForDeepScatter(
                        &deepScatterConfigSkin, &faceworksCBData, &faceworksErrors));

                if (faceworksErrors.m_msg)
                {
                    #if defined(_DEBUG)
                    wchar_t msg[512];
                    _snwprintf_s(msg, dim(msg),
                            L"FaceWorks rendering error:\n%hs", faceworksErrors.m_msg);
                    DXUTTrace(__FILE__, __LINE__, E_FAIL, msg, true);
                    #endif
                    GFSDK_FaceWorks_FreeErrorBlob(&faceworksErrors);
                }

                g_shdmgr.BindThickness(pd3dContext, &faceworksCBData);

                for (int i = 0; i < cMeshToDraw; ++i)
                {
                    meshesToDraw[i].m_pMesh->Draw(pd3dContext);
                }
            }
            break;

            default:
                assert(false);
                break;
        }

        g_shdmgr.UnbindTess(pd3dContext);

        // Draw the skybox
        pd3dContext->RSSetState(g_pRsSolid);
        g_shdmgr.BindSkybox(pd3dContext, g_pBkgndCur->m_pSrvCubeEnv, matClipToWorldAxes);
        g_meshFullscreen.Draw(pd3dContext);

        // Draw hair shaders, with alpha blending
        pd3dContext->RSSetState(g_pRsSolidDoubleSided);
        pd3dContext->OMSetDepthStencilState(g_pDssNoDepthWrite, 0);
        pd3dContext->OMSetBlendState(g_pBsAlphaBlend, nullptr, ~0UL);
        for (int i = 0; i < cMeshToDraw; ++i)
        {
            if (meshesToDraw[i].m_pMtl->m_shader == SHADER_Hair)
            {
                g_shdmgr.BindMaterial(pd3dContext, 0, meshesToDraw[i].m_pMtl);
                meshesToDraw[i].m_pMesh->Draw(pd3dContext);
            }
        }
        pd3dContext->RSSetState(g_pRsSolid);
        pd3dContext->OMSetDepthStencilState(g_pDssDepthTest, 0);
        pd3dContext->OMSetBlendState(nullptr, nullptr, ~0UL);

        // Now switch to the SRGB back buffer view, for compositing UI
        V(DXUTSetupD3D11Views(pd3dContext));

        // Show the shadow map if desired

        if (g_viewbuf == VIEWBUF_ShadowMap)
        {
            // Copy red channel to RGB channels
            XMFLOAT4X4 matTransformColor(
                1, 1, 1, 0,
                0, 0, 0, 0,
                0, 0, 0, 0,
                0, 0, 0, 1);

            CopyTexture(
                    pd3dContext, g_shadowmap.m_pSrv,
                    DXUTGetD3D11RenderTargetView(), nullptr,
                    s_rectViewBuffer,
                    &matTransformColor);
            V(DXUTSetupD3D11Views(pd3dContext));
        }

        // Render GUI and text

        if (g_ShowGUI)
        {
            UpdateSliders();
            g_HUD.OnRender(fElapsedTime);
        }

        g_gpuProfiler.WaitForDataAndUpdate(pd3dContext);

        if (g_ShowText)
        {
            RenderText();
        }

        g_gpuProfiler.EndFrame(pd3dContext);*/
    }

    @Override
    protected void reshape(int width, int height) {
        if(width <=0||height<=0)
            return;

        final float NEAR_PLANE = 0.1f;
        final float FAR_PLANE = 1e5f;
        Matrix4f.perspective((float)Math.toDegrees(g_FOV), (float)width/height, NEAR_PLANE,
                FAR_PLANE, m_proj);
    }

    @Override
    public void onDestroy() {
//        g_DialogResourceManager.OnD3D11DestroyDevice();
//        DXUTGetGlobalResourceCache().OnDestroyDevice();
//        SAFE_DELETE(g_pTxtHelper);

        g_sceneDigitalIra.Release();
        g_sceneTest.Release();
        g_sceneHand.Release();
        g_sceneDragon.Release();
        g_sceneLPSHead.Release();
        g_sceneManjaladon.Release();
        g_sceneWarriorHead.Release();

        g_bkgndBlack.Release();
        g_bkgndCharcoal.Release();
        g_bkgndForest.Release();
        g_bkgndNight.Release();
        g_bkgndTunnel.Release();

        g_meshFullscreen.dispose();

        CommonUtil.safeRelease(g_pSrvCurvatureLUT);
        CommonUtil.safeRelease(g_pSrvShadowLUT);

        g_shdmgr.Release();

        /*SAFE_RELEASE(g_pDssDepthTest);
        SAFE_RELEASE(g_pDssNoDepthTest);
        SAFE_RELEASE(g_pDssNoDepthWrite);
        SAFE_RELEASE(g_pRsSolid);
        SAFE_RELEASE(g_pRsWireframe);
        SAFE_RELEASE(g_pRsSolidDoubleSided);
        SAFE_RELEASE(g_pBsAlphaBlend);*/

        g_shadowmap.dispose();
        g_vsm.dispose();

//        g_gpuProfiler.Release();
    }
}
