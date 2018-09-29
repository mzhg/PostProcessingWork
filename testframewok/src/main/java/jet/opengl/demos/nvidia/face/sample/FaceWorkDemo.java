package jet.opengl.demos.nvidia.face.sample;

import com.nvidia.developer.opengl.app.NvSampleApp;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import java.util.ArrayList;
import java.util.List;

import jet.opengl.demos.nvidia.face.libs.GFSDK_FaceWorks;
import jet.opengl.demos.nvidia.face.libs.GFSDK_FaceWorks_DeepScatterConfig;
import jet.opengl.demos.nvidia.face.libs.GFSDK_FaceWorks_ErrorBlob;
import jet.opengl.demos.nvidia.face.libs.GFSDK_FaceWorks_ProjectionType;
import jet.opengl.demos.nvidia.face.libs.GFSDK_FaceWorks_SSSConfig;
import jet.opengl.postprocessing.common.GLCheck;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
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

    static final int
    SHDFEAT_None			= 0x00,
    SHDFEAT_Tessellation	= 0x01,
    SHDFEAT_SSS				= 0x02,
    SHDFEAT_DeepScatter		= 0x04,

    SHDFEAT_PSMask			= 0x06;

    // Rendering methods

    static final int
    RM_None = 0,
    RM_SSS = 1,
    RM_SSSAndDeep = 2,
    RM_ViewCurvature = 3,
    RM_ViewThickness = 4,
    RM_Max = 5;

    int g_renderMethod = RM_SSSAndDeep;

// Scene resources

    final float					g_FOV = 0.5f;		// vertical fov, radians

    CSceneDigitalIra			g_sceneDigitalIra = new CSceneDigitalIra();
    CSceneTest					g_sceneTest = new CSceneTest();
    CSceneHand					g_sceneHand = new CSceneHand();
    CSceneDragon				g_sceneDragon = new CSceneDragon();
    CSceneLPSHead				g_sceneLPSHead = new CSceneLPSHead();
    CSceneManjaladon			g_sceneManjaladon = new CSceneManjaladon();
    CSceneWarriorHead			g_sceneWarriorHead = new CSceneWarriorHead();
    CScene  					g_pSceneCur = null;

    CBackground					g_bkgndBlack  = new CBackground();
    CBackground					g_bkgndCharcoal = new CBackground();
    CBackground					g_bkgndForest = new CBackground();
    CBackground					g_bkgndNight = new CBackground();
    CBackground					g_bkgndTunnel = new CBackground();
    CBackground 				g_pBkgndCur = null;

    CMesh						g_meshFullscreen;

    Texture2D                   g_pSrvCurvatureLUT = null;
    static final float			g_curvatureRadiusMinLUT = 0.1f;		// cm
    static final float			g_curvatureRadiusMaxLUT = 10.0f;	// cm

    Texture2D	                g_pSrvShadowLUT = null;
    static final float			g_shadowWidthMinLUT = 0.8f;		// cm
    static final float			g_shadowWidthMaxLUT = 10.0f;	// cm

    Runnable	                g_pDssDepthTest = null;
    Runnable	                g_pDssNoDepthTest = null;
    Runnable	                g_pDssNoDepthWrite = null;
    Runnable		            g_pRsSolid = null;
    Runnable		            g_pRsWireframe = null;
    Runnable		            g_pRsSolidDoubleSided = null;
    Runnable			        g_pBsAlphaBlend = null;

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
    final Matrix4f       m_proj = new Matrix4f();
    final Matrix4f       matClipToWorldAxes = new Matrix4f();  // used for sky rendering
    final CbufFrame      m_bufFrame = new CbufFrame();

    final List<MeshToDraw>   m_meshesToDraw = new ArrayList<>();

    private GLFuncProvider gl;

    @Override
    protected void initRendering() {
        gl = GLFuncProviderFactory.getGLFuncProvider();
        // Init FaceWorks
        GFSDK_FaceWorks.GFSDK_FaceWorks_Init();

//        V_RETURN(CreateFullscreenMesh(pDevice, &g_meshFullscreen));  TODO

        // Load scenes
        g_sceneDigitalIra.Init();
        g_sceneHand.Init();
        g_sceneDragon.Init();
        g_sceneLPSHead.Init();
        g_sceneManjaladon.Init();
        g_sceneWarriorHead.Init();
        setCurrentScene(g_sceneDigitalIra);

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
        GLCheck.checkError();
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
        V_RETURN(pDevice->CreateDepthStencilState(&dssDepthTestDesc, &g_pDssDepthTest));*/
        g_pDssDepthTest = ()->
        {
            gl.glEnable(GLenum.GL_DEPTH_TEST);
            gl.glDepthFunc(GLenum.GL_LEQUAL);
            gl.glDepthMask(true);
        };

        /*D3D11_DEPTH_STENCIL_DESC dssNoDepthTestDesc =
                {
                        false,							// DepthEnable
                };
        V_RETURN(pDevice->CreateDepthStencilState(&dssNoDepthTestDesc, &g_pDssNoDepthTest));*/
        g_pDssNoDepthTest = ()-> gl.glDisable(GLenum.GL_DEPTH_TEST);

        /*D3D11_DEPTH_STENCIL_DESC dssNoDepthWriteDesc =
                {
                        true,							// DepthEnable
                        D3D11_DEPTH_WRITE_MASK_ZERO,
                        D3D11_COMPARISON_LESS_EQUAL,
                };
        V_RETURN(pDevice->CreateDepthStencilState(&dssNoDepthWriteDesc, &g_pDssNoDepthWrite));*/
        g_pDssNoDepthWrite = ()->
        {
            gl.glEnable(GLenum.GL_DEPTH_TEST);
            gl.glDepthFunc(GLenum.GL_LEQUAL);
            gl.glDepthMask(false);
        };

        // Create rasterizer states
        /*D3D11_RASTERIZER_DESC rssSolidDesc =
                {
                        D3D11_FILL_SOLID,
                        D3D11_CULL_BACK,
                        true,							// FrontCounterClockwise
                        0, 0, 0,						// depth bias
                        true,							// DepthClipEnable
                        false,							// ScissorEnable
                        true,							// MultisampleEnable
                };
        V_RETURN(pDevice->CreateRasterizerState(&rssSolidDesc, &g_pRsSolid));*/
        g_pRsSolid =()->
        {
            gl.glPolygonMode(GLenum.GL_FRONT_AND_BACK, GLenum.GL_FILL);
            gl.glCullFace(GLenum.GL_BACK);
            gl.glFrontFace(GLenum.GL_CW);
            gl.glEnable(GLenum.GL_CULL_FACE);
        };

        /*D3D11_RASTERIZER_DESC rssWireframeDesc =
                {
                        D3D11_FILL_WIREFRAME,
                        D3D11_CULL_BACK,
                        true,							// FrontCounterClockwise
                        0, 0, 0,						// depth bias
                        true,							// DepthClipEnable
                        false,							// ScissorEnable
                        true,							// MultisampleEnable
                };
        V_RETURN(pDevice->CreateRasterizerState(&rssWireframeDesc, &g_pRsWireframe));*/
        g_pRsWireframe = ()->
        {
            gl.glPolygonMode(GLenum.GL_FRONT_AND_BACK, GLenum.GL_LINE);
            gl.glCullFace(GLenum.GL_BACK);
            gl.glFrontFace(GLenum.GL_CW);
            gl.glEnable(GLenum.GL_CULL_FACE);
        };

        /*D3D11_RASTERIZER_DESC rssSolidDoubleSidedDesc =
                {
                        D3D11_FILL_SOLID,
                        D3D11_CULL_NONE,
                        true,							// FrontCounterClockwise
                        0, 0, 0,						// depth bias
                        true,							// DepthClipEnable
                        false,							// ScissorEnable
                        true,							// MultisampleEnable
                };
        V_RETURN(pDevice->CreateRasterizerState(&rssSolidDoubleSidedDesc, &g_pRsSolidDoubleSided));*/

        g_pRsSolidDoubleSided = ()->
        {
            gl.glPolygonMode(GLenum.GL_FRONT_AND_BACK, GLenum.GL_FILL);
            gl.glDisable(GLenum.GL_CULL_FACE);
        };

        // Initialize the blend state for alpha-blending

        /*D3D11_BLEND_DESC bsDesc =
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
        g_pBsAlphaBlend = ()->
        {
            gl.glEnable(GLenum.GL_BLEND);
            gl.glBlendFunc(GLenum.GL_SRC_ALPHA, GLenum.GL_ONE_MINUS_SRC_ALPHA);
        };

        // Create shadow map
        g_shadowmap = new CShadowMap();
        g_shadowmap.Init(1024);
        g_vsm = new CVarShadowMap();
        g_vsm.Init(1024);

        // Set up GPU profiler
//        V_RETURN(g_gpuProfiler.Init(pDevice));

        // Set up directional light
        g_rgbDirectionalLight.set(0.984f, 1.0f, 0.912f);	// Note: linear RGB space
    }

    @Override
    public void display() {
//        g_gpuProfiler.BeginFrame(pd3dContext);

        // Set up directional light and shadow map
        g_vecDirectionalLight.set(
                (float)Math.sin(g_radYawDirectionalLight) * (float)Math.cos(g_radPitchDirectionalLight),
                (float)Math.sin(g_radPitchDirectionalLight),
                (float)Math.cos(g_radYawDirectionalLight) * (float)Math.cos(g_radPitchDirectionalLight));


//        XMStoreFloat3(&g_shadowmap.m_vecLight, g_vecDirectionalLight);
        g_shadowmap.m_vecLight.set(g_vecDirectionalLight);

        g_pSceneCur.GetBounds(g_shadowmap.m_posMinScene, g_shadowmap.m_posMaxScene);
        g_shadowmap.UpdateMatrix();

        // Calculate UV space blur radius using shadow map's diameter, to attempt to maintain a
        // constant blur radius in world space.
        g_vsm.m_blurRadius = g_vsmBlurRadius / Math.min(g_shadowmap.m_vecDiam.x, g_shadowmap.m_vecDiam.y);

        // Get the list of stuff to draw for the current scene
        m_meshesToDraw.clear();
        g_pSceneCur.GetMeshesToDraw(m_meshesToDraw);
        int cMeshToDraw = m_meshesToDraw.size();

        // Set up whole-frame textures, constant buffers, etc.
        boolean bDebug = false/*(GetAsyncKeyState(' ') != 0)*/;
        float debug = bDebug ? 1.0f : 0.0f;

        CbufDebug cbDebug = new CbufDebug(debug,
                g_debugSlider0,
                g_debugSlider1,
                g_debugSlider2,
                g_debugSlider3);

        // Get the view and proj matrices from the camera
//        XMMATRIX matView = g_pSceneCur->Camera()->GetViewMatrix();
//        XMMATRIX matProj = g_pSceneCur->Camera()->GetProjMatrix();
        Matrix4f matView = g_pSceneCur.Camera().getViewMatrix();

//        XMFLOAT4X4 matWorldToClip; XMStoreFloat4x4(&matWorldToClip, matView * matProj);
        Matrix4f.mul(m_proj, matView, m_bufFrame.m_matWorldToClip);
//        XMFLOAT4 posCamera; XMStoreFloat4(&posCamera, g_pSceneCur->Camera()->GetEyePt());
        m_bufFrame.m_posCamera.set(g_pSceneCur.Camera().getPosition());

        /*XMMATRIX matViewAxes(
                matView.r[0],
                matView.r[1],
                matView.r[2],
                XMVectorSelect(matView.r[3], g_XMZero, g_XMSelect1110));
        XMMATRIX matViewAxesInverse = XMMatrixInverse(nullptr, matViewAxes);
        XMMATRIX matProjInverse = XMMatrixInverse(nullptr, matProj);
        XMFLOAT4X4 matClipToWorldAxes; XMStoreFloat4x4(&matClipToWorldAxes, matProjInverse * matViewAxesInverse);*/
        matClipToWorldAxes.load(matView);
        matClipToWorldAxes.m30 = matClipToWorldAxes.m31 = matClipToWorldAxes.m32 = 0;  // remove the translate.
        Matrix4f.mul(m_proj, matClipToWorldAxes, matClipToWorldAxes);
        Matrix4f.invert(matClipToWorldAxes, matClipToWorldAxes);

        // Calculate tessellation scale using screen-space error.  The 0.41 is a curve fitting constant.
        // Error goes inversely as the square of the tess factor (for edges that are small wrt curvature),
        // so tess factor goes inversely as the square root of the target error.
        float tessScale = 0.41f / (float)Math.sqrt(g_tessErrPx * 2.0f * Math.tan(0.5 * g_FOV) / /*float(DXUTGetWindowHeight())*/getGLContext().height());

//        XMFLOAT4 vecDirectionalLight; XMStoreFloat4(&vecDirectionalLight, g_vecDirectionalLight);
//        XMFLOAT4 rgbDirectionalLight; XMStoreFloat4(&rgbDirectionalLight, g_directionalLightBrightness * g_rgbDirectionalLight);

        /*CbufFrame cbFrame =
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
        };*/
        m_bufFrame.m_vecDirectionalLight.set(g_vecDirectionalLight);
        Vector3f.scale(g_rgbDirectionalLight, g_directionalLightBrightness, m_bufFrame.m_rgbDirectionalLight);
        m_bufFrame.m_matWorldToUvzwShadow.load(g_shadowmap.m_matWorldToUvzw);
        m_bufFrame.m_matWorldToUvzShadowNormal[0].set(g_shadowmap.m_matWorldToUvzNormal.m00, g_shadowmap.m_matWorldToUvzNormal.m01, g_shadowmap.m_matWorldToUvzNormal.m02);
        m_bufFrame.m_matWorldToUvzShadowNormal[1].set(g_shadowmap.m_matWorldToUvzNormal.m10, g_shadowmap.m_matWorldToUvzNormal.m11, g_shadowmap.m_matWorldToUvzNormal.m12);
        m_bufFrame.m_matWorldToUvzShadowNormal[2].set(g_shadowmap.m_matWorldToUvzNormal.m20, g_shadowmap.m_matWorldToUvzNormal.m21, g_shadowmap.m_matWorldToUvzNormal.m22);
        m_bufFrame.m_vsmMinVariance = g_vsmMinVariance;
        m_bufFrame.m_shadowSharpening = g_shadowSharpening;
        m_bufFrame.m_tessScale = tessScale;
        m_bufFrame.m_deepScatterIntensity = g_deepScatterIntensity;
        m_bufFrame.m_deepScatterNormalOffset = g_deepScatterNormalOffset;
        m_bufFrame.m_exposure = g_pBkgndCur.m_exposure;

        g_shdmgr.InitFrame(
//                pd3dContext,
                cbDebug,
                m_bufFrame,
                g_pBkgndCur.m_pSrvCubeDiff,
                g_pBkgndCur.m_pSrvCubeSpec,
                g_pSrvCurvatureLUT,
                g_pSrvShadowLUT);

        // Clear the shadow map
//        pd3dContext->ClearDepthStencilView(g_shadowmap.m_pDsv, D3D11_CLEAR_DEPTH, 1.0f, 0);

        g_shadowmap.BindRenderTarget(/*pd3dContext*/);
        gl.glClearBufferfi(GLenum.GL_DEPTH, 0, 1, 0);
//        pd3dContext->OMSetDepthStencilState(g_pDssDepthTest, 0);  TODO
//        pd3dContext->RSSetState(g_pRsSolid);                      TODO
        g_pDssDepthTest.run();
        g_pRsSolid.run();


        // Draw shadow map
        g_shdmgr.BindShadow(/*pd3dContext,*/ g_shadowmap.m_matWorldToClip);
        for (int i = 0; i < cMeshToDraw; ++i)
        {
            m_meshesToDraw.get(i).m_pMesh.Draw();
        }

        g_vsm.UpdateFromShadowMap(g_shadowmap);
        g_vsm.GaussianBlur();

//        g_gpuProfiler.Timestamp(pd3dContext, GTS_ShadowMap);

        // Bind the non-SRGB render target view, for rendering with tone mapping
        // (which outputs in SRGB gamma space natively)
        /*V(DXUTSetupD3D11Views(pd3dContext));
        pd3dContext->OMSetRenderTargets(1, &g_pRtvNonSrgb, DXUTGetD3D11DepthStencilView());
        pd3dContext->RSSetState(g_bWireframe ? g_pRsWireframe : g_pRsSolid);

        // Clear the screen
        float rgbaZero[4] = {};
        pd3dContext->ClearRenderTargetView(DXUTGetD3D11RenderTargetView(), rgbaZero);
        pd3dContext->ClearDepthStencilView(DXUTGetD3D11DepthStencilView(), D3D11_CLEAR_DEPTH | D3D11_CLEAR_STENCIL, 1.0f, 0);*/
        gl.glClearColor(0,0,0,0);
        gl.glClearDepthf(1.f);
        gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, 0);
        gl.glViewport(0,0, getGLContext().width(), getGLContext().height());
        gl.glClear(GLenum.GL_COLOR_BUFFER_BIT|GLenum.GL_DEPTH_BUFFER_BIT);

        g_shdmgr.BindShadowTextures(
//                pd3dContext,
                g_shadowmap.m_pSrv,
                g_vsm.m_pSrv);

        int features = 0;
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
                    if (m_meshesToDraw.get(i).m_pMtl.m_shader == SHADER.Skin)
                    {
                        g_shdmgr.BindMaterial(/*pd3dContext, */features, m_meshesToDraw.get(i).m_pMtl);
                        m_meshesToDraw.get(i).m_pMesh.Draw();
                    }
                }
//                g_gpuProfiler.Timestamp(pd3dContext, GTS_Skin);

                // Draw eye shaders
                for (int i = 0; i < cMeshToDraw; ++i)
                {
                    if (m_meshesToDraw.get(i).m_pMtl.m_shader == SHADER.Eye)
                    {
                        g_shdmgr.BindMaterial(/*pd3dContext,*/ features, m_meshesToDraw.get(i).m_pMtl);
                        m_meshesToDraw.get(i).m_pMesh.Draw();
                    }
                }
//                g_gpuProfiler.Timestamp(pd3dContext, GTS_Eyes);
            }
            break;

            case RM_SSS:
            case RM_SSSAndDeep:
            {
                GFSDK_FaceWorks_SSSConfig sssConfigSkin =new GFSDK_FaceWorks_SSSConfig(), sssConfigEye = new GFSDK_FaceWorks_SSSConfig();
                sssConfigSkin.m_diffusionRadius = Math.max(g_sssBlurRadius, 0.01f);
                sssConfigSkin.m_diffusionRadiusLUT = 0.27f;

                sssConfigSkin.m_curvatureRadiusMinLUT = g_curvatureRadiusMinLUT;
                sssConfigSkin.m_curvatureRadiusMaxLUT = g_curvatureRadiusMaxLUT;
                sssConfigSkin.m_shadowWidthMinLUT = g_shadowWidthMinLUT;
                sssConfigSkin.m_shadowWidthMaxLUT = g_shadowWidthMaxLUT;

                // Filter width is ~6 times the Gaussian sigma
                sssConfigSkin.m_shadowFilterWidth = Math.max(6.0f * g_vsmBlurRadius, 0.01f);

                sssConfigEye = sssConfigSkin;

                GFSDK_FaceWorks_DeepScatterConfig deepScatterConfigSkin = new GFSDK_FaceWorks_DeepScatterConfig(), deepScatterConfigEye = new GFSDK_FaceWorks_DeepScatterConfig();
                deepScatterConfigSkin.m_radius = Math.max(g_deepScatterRadius, 0.01f);
                deepScatterConfigSkin.m_shadowProjType = GFSDK_FaceWorks_ProjectionType.ParallelProjection;
//                memcpy(&deepScatterConfigSkin.m_shadowProjMatrix, &g_shadowmap.m_matProj, 16 * sizeof(float));
                deepScatterConfigSkin.m_shadowProjMatrix.load(g_shadowmap.m_matProj);
                deepScatterConfigSkin.m_shadowFilterRadius = g_deepScatterShadowRadius / Math.min(g_shadowmap.m_vecDiam.x, g_shadowmap.m_vecDiam.y);

                deepScatterConfigEye = deepScatterConfigSkin;

                features |= SHDFEAT_SSS;
                if (g_renderMethod == RM_SSSAndDeep)
                    features |= SHDFEAT_DeepScatter;

                GFSDK_FaceWorks_ErrorBlob faceworksErrors = new GFSDK_FaceWorks_ErrorBlob();

                // Note: two loops for skin and eye materials so we can have GPU timestamps around
                // each shader individually. Gack.

                // Draw skin shaders
                for (int i = 0; i < cMeshToDraw; ++i)
                {
                    if (m_meshesToDraw.get(i).m_pMtl.m_shader == SHADER.Skin)
                    {
                        sssConfigSkin.m_normalMapSize = m_meshesToDraw.get(i).m_normalMapSize;
                        sssConfigSkin.m_averageUVScale = m_meshesToDraw.get(i).m_averageUVScale;
                        GFSDK_FaceWorks.GFSDK_FaceWorks_WriteCBDataForSSS(
                                sssConfigSkin, /*reinterpret_cast<GFSDK_FaceWorks_CBData *>*/(m_meshesToDraw.get(i).m_pMtl.m_constants[4]), faceworksErrors);
                        GFSDK_FaceWorks.GFSDK_FaceWorks_WriteCBDataForDeepScatter(
                                deepScatterConfigSkin, /*reinterpret_cast<GFSDK_FaceWorks_CBData *>*/(m_meshesToDraw.get(i).m_pMtl.m_constants[4]), faceworksErrors));

                        g_shdmgr.BindMaterial(/*pd3dContext,*/ features, m_meshesToDraw.get(i).m_pMtl);
                        m_meshesToDraw.get(i).m_pMesh->Draw(pd3dContext);
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

        g_gpuProfiler.EndFrame(pd3dContext);
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

    private void setCurrentScene(CScene scene){
        if(g_pSceneCur != scene){
            g_pSceneCur = scene;
            setInputHandler(scene.Camera());
        }
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
