package com.nvidia.developer.opengl.demos.amdfx.dof;

import com.nvidia.developer.opengl.app.NvSampleApp;
import com.nvidia.developer.opengl.demos.amdfx.common.AMD_Camera;
import com.nvidia.developer.opengl.demos.amdfx.common.AMD_Mesh;
import com.nvidia.developer.opengl.demos.amdfx.common.CFirstPersonCamera;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;
import org.lwjgl.util.vector.Vector4i;

import java.io.IOException;
import java.nio.ByteBuffer;

import jet.opengl.postprocessing.common.GLCheck;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.shader.GLSLUtil;
import jet.opengl.postprocessing.shader.ProgramProperties;
import jet.opengl.postprocessing.shader.ShaderLoader;
import jet.opengl.postprocessing.shader.ShaderProgram;
import jet.opengl.postprocessing.shader.ShaderSourceItem;
import jet.opengl.postprocessing.shader.ShaderType;
import jet.opengl.postprocessing.texture.SamplerDesc;
import jet.opengl.postprocessing.texture.SamplerUtils;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.Texture2DDesc;
import jet.opengl.postprocessing.texture.TextureUtils;
import jet.opengl.postprocessing.util.CacheBuffer;
import jet.opengl.postprocessing.util.CommonUtil;

import static com.nvidia.developer.opengl.demos.amdfx.dof.DepthOfFieldFX.DepthOfFieldFX_Release;

/**
 * Created by mazhen'gui on 2017/6/26.
 */

public class DepthOfFieldFX_Sample extends NvSampleApp {
    final CFirstPersonCamera g_Viewer = new CFirstPersonCamera();
    final AMD_Camera         g_ViewerData = new AMD_Camera();
    final CalcDOFParams      g_DOFParams = new CalcDOFParams();
    CFirstPersonCamera       g_pCurrentCamera = g_Viewer;
    AMD_Camera               g_pCurrentData   = g_ViewerData;

// AMD helper classes defined here
//    static AMD::MagnifyTool g_MagnifyTool;
//    static AMD::HUD         g_HUD;

    // Global boolean for HUD rendering
    boolean g_bRenderHUD              = true;
    boolean g_bShowDOFResult          = false;
    boolean g_bDebugCircleOfConfusion = false;
    boolean g_bSaveScreenShot         = false;


    private enum DepthOfFieldMode
    {
        DOF_Disabled                   /*= 0*/,
        DOF_BoxFastFilterSpread        /*= 1*/,
        DOF_FastFilterSpread           /*= 2*/,
        DOF_QuarterResFastFilterSpread /*= 3*/,
    };

    DepthOfFieldMode g_depthOfFieldMode = DepthOfFieldMode.DOF_FastFilterSpread;

//--------------------------------------------------------------------------------------
// Mesh
//--------------------------------------------------------------------------------------
    AMD_Mesh g_Model;
    final S_MODEL_DESC        g_ModelDesc = new S_MODEL_DESC();

    int            g_framebuffer;
    Texture2D      g_appColorBuffer;
    Texture2D      g_appDepthBuffer;
    Texture2D      g_appDofSurface;
    GLSLProgram    g_d3dFullScreenProgram = null;


//--------------------------------------------------------------------------------------
// D3D11 Model Rendering Interfaces
//--------------------------------------------------------------------------------------
//    ID3D11InputLayout*  g_d3dModelIL = NULL;
//    ID3D11VertexShader* g_d3dModelVS = NULL;
//    ID3D11PixelShader*  g_d3dModelPS = NULL;
    int          g_d3dModelCB = 0;
    GLSLProgram  g_d3dModelProgram = null;

    static  CameraParameters g_defaultCameraParameters[] = {
        new CameraParameters ( 20.2270432f, 4.19414091f, 16.7282600f, 19.4321709f, 4.09884357f, 16.1290131f , 400.0f, 21.67f, 100.0f, 1.4f),
        new CameraParameters ( -14.7709570f, 5.55706882f, -17.5470028f, -14.1790190f, 5.42186546f, -16.7524414f, 218.0f, 23.3f, 100.0f, 1.6f),
        new CameraParameters ( 2.34538126f, -0.0807961449f, -12.6757965f, 2.23687410f, 0.0531809852f, -11.6907701f, 190.0f, 14.61f, 100.0f, 1.8f),
        new CameraParameters ( 25.5143566f, 5.54141998f, -20.4762344f, 24.8163872f, 5.42109346f, -19.7702885f , 133.0f, 34.95f, 50.0f, 1.6f),
        new CameraParameters ( 5.513732f, 0.803944f, -18.025604f, 5.315537f, 0.848312f, -17.046444f, 205.0f, 39.47f, 85.4f, 2.6f ),
        new CameraParameters ( -15.698505f, 6.656400f, -21.832394f, -15.187683f, 6.442449f, -20.999754f, 229.0f, 11.3f, 100.00f, 3.9f ),
        new CameraParameters ( 10.018296f, 0.288034f, -1.364868f, 9.142344f, 0.441804f, -0.907634f, 157.0f, 10.9f, 100.00f, 2.2f ),
        new CameraParameters ( -3.399786f, 0.948747f, -15.984277f, -3.114154f, 1.013084f, -15.028101f, 366.0f, 16.8f, 100.00f, 1.4f ),
        new CameraParameters ( -14.941996f, 4.904000f, -17.381784f, -14.348591f, 4.798616f, -16.583803f, 155.0f, 24.9f, 42.70f, 1.4f ),
    };

    static int g_defaultCameraParameterIndex = 0;

    GLSLProgram g_pCalcCoc  = null;
    GLSLProgram g_pDebugCoc = null;


    int g_d3dCalcDofCb = 0;

    Texture2D g_appCoCTexture;

    static final int MAX_DOF_RADIUS =  64;
    float        g_FocalLength   = 190.0f;  // in mm
    float        g_FocalDistance = 14.61f;  // in meters
    float        g_sensorWidth   = 100.0f;  // in mm
    float        g_fStop         = 1.8f;
    float        g_forceCoc      = 0.0f;
    int          g_maxRadius     = 57;
    int          g_scale_factor  = 30;
    int          g_box_scale_factor = 24;

//--------------------------------------------------------------------------------------
// D3D11 Common Rendering Interfaces
//--------------------------------------------------------------------------------------
    int g_d3dViewerCB = 0;

    int        g_d3dLinearWrapSS           = 0;
    Runnable   g_d3dOpaqueBS               = null;
    Runnable   g_d3dBackCullingSolidRS     = null;
    Runnable   g_d3dNoCullingSolidRS       = null;
    Runnable   g_d3dDepthLessEqualDSS      = null;

    //--------------------------------------------------------------------------------------
// Timing data
//--------------------------------------------------------------------------------------
    float g_SceneRenderingTime = 0.0f;
    float g_DofRenderingTime  = 0.0f;

    //--------------------------------------------------------------------------------------
// Miscellaneous global variables
//--------------------------------------------------------------------------------------
//    boolean        g_bStartWindowed = true;
//    AMD::uint32 g_ScreenWidth    = 1920;
//    AMD::uint32 g_ScreenHeight   = 1080;

//--------------------------------------------------------------------------------------
// DepthOfFieldFX global variables
//--------------------------------------------------------------------------------------
    DepthOfFieldFXDesc g_AMD_DofFX_Desc;
    GLFuncProvider gl;

    @Override
    protected void initRendering() {
        gl = GLFuncProviderFactory.getGLFuncProvider();
        compileShaders();
        createMeshes();
        setupCamera();

        g_d3dLinearWrapSS = SamplerUtils.createSampler(new SamplerDesc());
        g_d3dOpaqueBS = ()-> {gl.glDisable(GLenum.GL_BLEND); };
        g_d3dBackCullingSolidRS = ()->{ gl.glDisable(GLenum.GL_CULL_FACE);};  // TODO
        g_d3dNoCullingSolidRS = ()-> gl.glDisable(GLenum.GL_CULL_FACE);
        g_d3dDepthLessEqualDSS = ()->
        {
            gl.glEnable(GLenum.GL_DEPTH_TEST);
            gl.glDepthFunc(GLenum.GL_LEQUAL);
        };

        GLCheck.checkError();
        try {
            final String shaderPath = "shader_libs/";
            g_d3dFullScreenProgram = GLSLProgram.createFromFiles(shaderPath + "PostProcessingDefaultScreenSpaceVS.vert",
                                                                shaderPath + "PostProcessingDefaultScreenSpacePS.frag");
            g_d3dFullScreenProgram.enable();
            g_d3dFullScreenProgram.setTextureUniform("g_InputTex", 0);
        } catch (IOException e) {
            e.printStackTrace();
        }

        setupDepthOfField();
        g_AMD_DofFX_Desc = new DepthOfFieldFXDesc();
//        g_AMD_DofFX_Desc.m_pDevice        = pd3dDevice;
//        g_AMD_DofFX_Desc.m_pDeviceContext = pd3dContext;
        g_AMD_DofFX_Desc.m_screenSize.x   = getGLContext().width();
        g_AMD_DofFX_Desc.m_screenSize.y   = getGLContext().height();
        DEPTHOFFIELDFX_RETURN_CODE amdResult = DepthOfFieldFX.DepthOfFieldFX_Initialize(g_AMD_DofFX_Desc);
        if (amdResult != DEPTHOFFIELDFX_RETURN_CODE.DEPTHOFFIELDFX_RETURN_CODE_SUCCESS)
        {
            throw new IllegalStateException();
        }

        GLCheck.checkError();
        m_transformer.setTranslation(0,0,-10);
    }

    @Override
    public void display() {
        Vector4i                   pNullSR  = null;
        ShaderProgram              pNullHS  = null;
        ShaderProgram              pNullDS  = null;
        ShaderProgram              pNullGS  = null;
        Texture2D[]                pNullSRV = null;
        Texture2D[]                pNullUAV = null;

//        ID3D11RenderTargetView* pOriginalRTV = NULL;
//        ID3D11DepthStencilView* pOriginalDSV = NULL;

//        const int    nFrameCountMax     = 60;
//        static int   nFrameCount        = 0;
//        static float fTimeSceneRendering = 0.0f;
//        static float fTimeDofRendering  = 0.0f;

        Vector4f light_blue = new Vector4f(0.176f, 0.196f, 0.667f, 0.000f);
//        Vector4f white = new Vector4f(1.000f, 1.000f, 1.000f, 1.000f);

//        TIMER_Reset();

        setCameraProjectionParameters();

        // Store the original render target and depth buffer
//        pd3dContext->OMGetRenderTargets(1, &pOriginalRTV, &pOriginalDSV);

        // Clear the depth stencil & shadow map
//        pd3dContext->ClearRenderTargetView(pOriginalRTV, light_blue.f);
//        pd3dContext->ClearDepthStencilView(pOriginalDSV, D3D11_CLEAR_DEPTH, 1.0f, 0);
//        pd3dContext->ClearRenderTargetView(g_appColorBuffer._rtv, light_blue.f);
//        pd3dContext->ClearDepthStencilView(g_appDepthBuffer._dsv, D3D11_CLEAR_DEPTH, 1.0f, 0);

        setCameraConstantBuffer(/*pd3dContext,*/ g_d3dViewerCB, g_ViewerData, g_Viewer, 1);

//        TIMER_Begin(0, L"Scene Rendering");
        {
            int           pCB[]  = { g_d3dModelCB, g_d3dViewerCB };
            int           pSS[]  = { g_d3dLinearWrapSS };
            Texture2D     pRTV[] = { g_appColorBuffer };

            AMD_Mesh meshes    = g_Model;

            RenderScene(/*pd3dContext,*/ meshes, g_ModelDesc, 1, new Vector4i(0, 0, getGLContext().width(), getGLContext().height()), 1, pNullSR, 0,
                g_d3dBackCullingSolidRS, g_d3dOpaqueBS, null, g_d3dDepthLessEqualDSS, 0, /*g_d3dModelIL*/0, /*g_d3dModelVS*/null, pNullHS, pNullDS,
                pNullGS, g_d3dModelProgram, g_d3dModelCB, pCB, 0, pCB.length, pSS, 0, pSS.length, null, 1, 0, pRTV, pRTV.length, g_appDepthBuffer,
                g_pCurrentCamera);

            if(g_bRenderHUD){
                System.out.println("-------------------------ModelProgram-----------------------");
                ProgramProperties properties = GLSLUtil.getProperties(g_d3dModelProgram.getProgram());
                System.out.println(properties);
            }
        }


//        pd3dContext->OMSetRenderTargets(1, &pOriginalRTV, pOriginalDSV);
        gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, 0);
//        pd3dContext->CSSetShader(g_pCalcCoc, NULL, 0);
//        pd3dContext->CSSetConstantBuffers(0, 1, &g_d3dCalcDofCb);
//        pd3dContext->CSSetUnorderedAccessViews(0, 1, &g_appCoCTexture._uav, NULL);
//        pd3dContext->CSSetShaderResources(0, 1, &g_appDepthBuffer._srv);
        gl.glClearBufferfv(GLenum.GL_COLOR, 0, CacheBuffer.wrap(light_blue));
        gl.glClearBufferfv(GLenum.GL_DEPTH, 0, CacheBuffer.wrap(1.0f));
        g_pCalcCoc.enable();

        int threadCountX = (getGLContext().width() + 7) / 8;
        int threadCountY = (getGLContext().height() + 7) / 8;
//        pd3dContext->Dispatch(threadCountX, threadCountY, 1);
//        TIMER_End();

        gl.glActiveTexture(GLenum.GL_TEXTURE0);
        gl.glBindTexture(g_appDepthBuffer.getTarget(), g_appDepthBuffer.getTexture());
        gl.glBindImageTexture(0, g_appCoCTexture.getTexture(),0, false, 0, GLenum.GL_READ_WRITE, g_appCoCTexture.getFormat());

        gl.glDispatchCompute(threadCountX, threadCountY, 1);

//        pd3dContext->CSSetUnorderedAccessViews(0, 1, &pNullUAV, NULL);
//        pd3dContext->CSSetShaderResources(0, 1, &pNullSRV);
        gl.glBindTexture(g_appDepthBuffer.getTarget(), 0);
        gl.glBindImageTexture(0, 0,0, false, 0, GLenum.GL_READ_WRITE, g_appCoCTexture.getFormat());

//        TIMER_Begin(0, L"Depth Of Field");

        g_AMD_DofFX_Desc.m_scaleFactor = g_scale_factor;

        switch (g_depthOfFieldMode)
        {
            case DOF_BoxFastFilterSpread:
                g_AMD_DofFX_Desc.m_scaleFactor = g_box_scale_factor;
                DepthOfFieldFX.DepthOfFieldFX_RenderBox(g_AMD_DofFX_Desc);
                break;
            case DOF_FastFilterSpread:
                DepthOfFieldFX.DepthOfFieldFX_Render(g_AMD_DofFX_Desc);
                break;
            case DOF_QuarterResFastFilterSpread:
                DepthOfFieldFX.DepthOfFieldFX_RenderQuarterRes(g_AMD_DofFX_Desc);
                break;
            case DOF_Disabled:
            default:
//                pd3dContext->CopyResource(g_appDofSurface._t2d, g_appColorBuffer._t2d);
                gl.glCopyImageSubData(g_appColorBuffer.getTexture(), g_appColorBuffer.getTarget(), 0, 0,0,0,
                        g_appDofSurface.getTexture(), g_appDofSurface.getTarget(), 0,0,0,0,
                        g_appColorBuffer.getWidth(), g_appColorBuffer.getHeight(), 1);
                break;
        }

//        TIMER_End();

        if (g_bSaveScreenShot == true)
        {
//            DXUTSaveTextureToFile(pd3dContext, g_appDofSurface._t2d, true, L"ScreenShot.dds");
            g_bSaveScreenShot = false;
        }

        if (g_bDebugCircleOfConfusion)
        {
//            pd3dContext->CSSetShader(g_pDebugCoc, NULL, 0);
//            pd3dContext->CSSetShaderResources(0, 1, &g_appCoCTexture._srv);
//            pd3dContext->CSSetUnorderedAccessViews(0, 1, &g_appDofSurface._uav, nullptr);
//            pd3dContext->CSSetConstantBuffers(0, 1, &g_d3dCalcDofCb);
//            pd3dContext->Dispatch((g_ScreenWidth + 7) / 8, (g_ScreenHeight + 7) / 8, 1);
            g_pDebugCoc.enable();
            gl.glActiveTexture(GLenum.GL_TEXTURE0);
            gl.glBindTexture(g_appCoCTexture.getTarget(), g_appCoCTexture.getTexture());
            gl.glBindImageTexture(0, g_appDofSurface.getTexture(), 0, false, 0, GLenum.GL_READ_WRITE, g_appDofSurface.getFormat());
            gl.glDispatchCompute((getGLContext().width() + 7) / 8, (getGLContext().height() + 7) / 8, 1);

//            pd3dContext->CSSetShaderResources(0, 1, &pNullSRV);
//            pd3dContext->CSSetUnorderedAccessViews(0, 1, &pNullUAV, NULL);
            gl.glBindTexture(g_appDepthBuffer.getTarget(), 0);
            gl.glBindImageTexture(0, 0,0, false, 0, GLenum.GL_READ_WRITE, g_appDofSurface.getFormat());
        }

//        pd3dContext->OMSetRenderTargets(1, &pOriginalRTV, pOriginalDSV);
//        pd3dContext->VSSetShader(g_d3dFullScreenVS, NULL, 0);
//        pd3dContext->PSSetShader(g_d3dFullScreenPS, NULL, 0);
//        pd3dContext->PSSetShaderResources(0, 1, g_bShowDOFResult ? &g_appDofSurface._srv : &g_appColorBuffer._srv);
//        pd3dContext->PSSetSamplers(0, 1, &g_d3dLinearWrapSS);
//        pd3dContext->OMSetBlendState(g_d3dOpaqueBS, white.f, 0xf);
//        pd3dContext->OMSetDepthStencilState(g_d3dDepthLessEqualDSS, 0);
//        pd3dContext->RSSetState(g_d3dNoCullingSolidRS);
//        pd3dContext->Draw(6, 0);

        gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, 0);
//        gl.glClearBufferfv(GLenum.GL_COLOR, 0, CacheBuffer.wrap(light_blue));
//        gl.glClearBufferfv(GLenum.GL_DEPTH, 0, CacheBuffer.wrap(1.0f));
        g_d3dFullScreenProgram.enable();
        gl.glActiveTexture(GLenum.GL_TEXTURE0);
        if(g_bShowDOFResult){
            gl.glBindTexture(g_appDofSurface.getTarget(), g_appDofSurface.getTexture());
        }else{
            gl.glBindTexture(g_appColorBuffer.getTarget(), g_appColorBuffer.getTexture());
        }
        gl.glBindSampler(0, g_d3dLinearWrapSS);
        g_d3dOpaqueBS.run();
        g_d3dDepthLessEqualDSS.run();
        g_d3dNoCullingSolidRS.run();
        gl.glDrawArrays(GLenum.GL_TRIANGLES, 0, 3);
        gl.glBindSampler(0, 0);


//        SAFE_RELEASE(pOriginalRTV);
//        SAFE_RELEASE(pOriginalDSV);
//
//        float scene = (float)TIMER_GetTime(Gpu, L"Scene Rendering") * 1000.0f;
//        float dof  = (float)TIMER_GetTime(Gpu, L"Depth Of Field") * 1000.0f;
//
//        fTimeSceneRendering += scene;
//        fTimeDofRendering += dof;
//
//        if (g_bRenderHUD)
//        {
//            DXUT_BeginPerfEvent(DXUT_PERFEVENTCOLOR, L"HUD / Stats");
//
//            g_MagnifyTool.Render();
//            g_HUD.OnRender(fElapsedTime);
//            RenderText(g_SceneRenderingTime, scene, g_DofRenderingTime, dof);
//
//            DXUT_EndPerfEvent();
//        }
//
//        if (nFrameCount++ == nFrameCountMax)
//        {
//            g_SceneRenderingTime = fTimeSceneRendering / (float)nFrameCountMax;
//            g_DofRenderingTime   = fTimeDofRendering / (float)nFrameCountMax;
//
//            fTimeSceneRendering = 0.0f;
//            fTimeDofRendering   = 0.0f;
//            nFrameCount         = 0;
//        }

        g_bRenderHUD = false;
    }

    //--------------------------------------------------------------------------------------
// Render the scene (either for the main scene or the shadow map scene)
//--------------------------------------------------------------------------------------
    void RenderScene(//ID3D11DeviceContext* pd3dContext,
                     AMD_Mesh pMesh,
                     S_MODEL_DESC pMeshDesc,
                     int nMeshCount,                        // must be 1
                     Vector4i pVP,                          // ViewPort array
                     int               nVPCount,   // Viewport count
                     Vector4i                   pSR,        // Scissor Rects array
                     int               nSRCount,   // Scissor rect count
                     Runnable     pRS,        // Raster State
                     Runnable          pBS,        // Blend State
                     float[]                     pFactorBS,  // Blend state factor
                     Runnable   pDSS,       // Depth Stencil State
                     int               dssRef,     // Depth stencil state reference value
                     int         pIL,        // Input Layout
                     ShaderProgram pVS,        // Vertex Shader
                     ShaderProgram          pHS,        // Hull Shader
                     ShaderProgram        pDS,        // Domain Shader
                     ShaderProgram      pGS,        // Geometry SHader
                     GLSLProgram         pPS,        // Pixel Shader
                     int              pModelCB,
                     int[]             ppCB,       // Constant Buffer array
                     int               nCBStart,   // First slot to attach constant buffer array
                     int               nCBCount,   // Number of constant buffers in the array
                     int[]       ppSS,       // Sampler State array
                     int               nSSStart,   // First slot to attach sampler state array
                     int               nSSCount,   // Number of sampler states in the array
                     Texture2D[] ppSRV,      // Shader Resource View array
                     int               nSRVStart,  // First slot to attach sr views array
                     int               nSRVCount,  // Number of sr views in the array
                     Texture2D[]   ppRTV,      // Render Target View array
                     int               nRTVCount,  // Number of rt views in the array
                     Texture2D    pDSV,       // Depth Stencil View
                     CFirstPersonCamera        pCamera)
    {
//        ID3D11RenderTargetView* const   pNullRTV[8]   = { 0 };
//        ID3D11ShaderResourceView* const pNullSRV[128] = { 0 };

        // Unbind anything that could be still bound on input or output
        // If this doesn't happen, DX Runtime will spam with warnings
//        pd3dContext->OMSetRenderTargets(AMD_ARRAY_SIZE(pNullRTV), pNullRTV, NULL);
//        pd3dContext->CSSetShaderResources(0, AMD_ARRAY_SIZE(pNullSRV), pNullSRV);
//        pd3dContext->VSSetShaderResources(0, AMD_ARRAY_SIZE(pNullSRV), pNullSRV);
//        pd3dContext->HSSetShaderResources(0, AMD_ARRAY_SIZE(pNullSRV), pNullSRV);
//        pd3dContext->DSSetShaderResources(0, AMD_ARRAY_SIZE(pNullSRV), pNullSRV);
//        pd3dContext->GSSetShaderResources(0, AMD_ARRAY_SIZE(pNullSRV), pNullSRV);
//        pd3dContext->PSSetShaderResources(0, AMD_ARRAY_SIZE(pNullSRV), pNullSRV);

//        pd3dContext->IASetInputLayout(pIL);

//        pd3dContext->VSSetShader(pVS, NULL, 0);
//        pd3dContext->HSSetShader(pHS, NULL, 0);
//        pd3dContext->DSSetShader(pDS, NULL, 0);
//        pd3dContext->GSSetShader(pGS, NULL, 0);
//        pd3dContext->PSSetShader(pPS, NULL, 0);

        gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, g_framebuffer);

        if (nSSCount > 0)
        {
//            pd3dContext->VSSetSamplers(nSSStart, nSSCount, ppSS);
//            pd3dContext->HSSetSamplers(nSSStart, nSSCount, ppSS);
//            pd3dContext->DSSetSamplers(nSSStart, nSSCount, ppSS);
//            pd3dContext->GSSetSamplers(nSSStart, nSSCount, ppSS);
//            pd3dContext->PSSetSamplers(nSSStart, nSSCount, ppSS);
            for(int i = 0; i < nSSCount;i++){
                gl.glBindSampler(nSSStart + i, ppSS[i]);
            }
        }

        if (nSRVCount > 0)
        {
//            pd3dContext->VSSetShaderResources(nSRVStart, nSRVCount, ppSRV);
//            pd3dContext->HSSetShaderResources(nSRVStart, nSRVCount, ppSRV);
//            pd3dContext->DSSetShaderResources(nSRVStart, nSRVCount, ppSRV);
//            pd3dContext->GSSetShaderResources(nSRVStart, nSRVCount, ppSRV);
//            pd3dContext->PSSetShaderResources(nSRVStart, nSRVCount, ppSRV);
            for(int i = 0; i < nSRVCount;i++){
                gl.glActiveTexture(GLenum.GL_TEXTURE0 + i + nSRVStart);
                gl.glBindTexture(ppSRV[i].getTarget(), ppSRV[i].getTexture());
            }
        }

        if (nCBCount > 0)
        {
//            pd3dContext->VSSetConstantBuffers(nCBStart, nCBCount, ppCB);
//            pd3dContext->HSSetConstantBuffers(nCBStart, nCBCount, ppCB);
//            pd3dContext->DSSetConstantBuffers(nCBStart, nCBCount, ppCB);
//            pd3dContext->GSSetConstantBuffers(nCBStart, nCBCount, ppCB);
//            pd3dContext->PSSetConstantBuffers(nCBStart, nCBCount, ppCB);
            for(int i = 0; i < nCBCount; i++){
                gl.glBindBufferBase(GLenum.GL_UNIFORM_BUFFER, nCBStart + i, ppCB[i]);
            }
        }

//        pd3dContext->OMSetRenderTargets(nRTVCount, ppRTV, pDSV);  TODO binding buffers
//        pd3dContext->OMSetBlendState(pBS, pFactorBS, 0xf);
//        pd3dContext->OMSetDepthStencilState(pDSS, dssRef);
//        pd3dContext->RSSetState(pRS);
//        pd3dContext->RSSetScissorRects(nSRCount, pSR);
//        pd3dContext->RSSetViewports(nVPCount, pVP);

        if(pBS != null) pBS.run();
        if(pDSS != null) pDSS.run();
        if(pRS != null) pRS.run();
        gl.glViewport(pVP.x, pVP.y, pVP.z, pVP.w);

        GLCheck.checkError();

        for (int mesh = 0; mesh < 1; mesh++)
        {
            SetModelConstantBuffer(pModelCB, pMeshDesc, pCamera);
            pMesh.Render();
        }

        GLCheck.checkError();
    }

    @Override
    protected void reshape(int width, int height) {
        if(width <= 0 || height <= 0)
            return;

        setCameraProjectionParameters();

        // Set the location and size of the AMD standard HUD
//        g_HUD.m_GUI.SetLocation(pSurfaceDesc->Width - AMD::HUD::iDialogWidth, 0);
//        g_HUD.m_GUI.SetSize(AMD::HUD::iDialogWidth, pSurfaceDesc->Height);

        // Magnify tool will capture from the color buffer
//        g_MagnifyTool.OnResizedSwapChain(pd3dDevice, pSwapChain, pSurfaceDesc, pUserContext, pSurfaceDesc->Width - AMD::HUD::iDialogWidth, 0);
//        D3D11_RENDER_TARGET_VIEW_DESC RTDesc;
//        ID3D11Resource*               pTempRTResource;
//        DXUTGetD3D11RenderTargetView()->GetResource(&pTempRTResource);
//        DXUTGetD3D11RenderTargetView()->GetDesc(&RTDesc);
//        g_MagnifyTool.SetSourceResources(pTempRTResource, RTDesc.Format, g_ScreenWidth, g_ScreenHeight, pSurfaceDesc->SampleDesc.Count);
//        g_MagnifyTool.SetPixelRegion(128);
//        g_MagnifyTool.SetScale(5);
//        SAFE_RELEASE(pTempRTResource);

        // AMD HUD hook
//        g_HUD.OnResizedSwapChain(pSurfaceDesc);

        g_AMD_DofFX_Desc.m_screenSize.x = width;
        g_AMD_DofFX_Desc.m_screenSize.y = height;

        if(g_appColorBuffer != null && (g_appColorBuffer.getWidth() == width) && (g_appColorBuffer.getHeight() == height))
            return;

        // App specific resources
        // scene render target
        CommonUtil.safeRelease(g_appColorBuffer);
//        hr = g_appColorBuffer.CreateSurface(pd3dDevice, pSurfaceDesc->Width, pSurfaceDesc->Height, pSurfaceDesc->SampleDesc.Count, 1, 1, DXGI_FORMAT_R8G8B8A8_TYPELESS, DXGI_FORMAT_R8G8B8A8_UNORM_SRGB,
//                DXGI_FORMAT_R8G8B8A8_UNORM_SRGB, DXGI_FORMAT_UNKNOWN, DXGI_FORMAT_UNKNOWN, DXGI_FORMAT_UNKNOWN, D3D11_USAGE_DEFAULT, false, 0, NULL, NULL, 0);
        g_appColorBuffer = TextureUtils.createTexture2D(new Texture2DDesc(width, height, GLenum.GL_RGBA8), null);

        // scene depth buffer
        CommonUtil.safeRelease(g_appDepthBuffer);
//        hr = g_appDepthBuffer.CreateSurface(pd3dDevice, pSurfaceDesc->Width, pSurfaceDesc->Height, pSurfaceDesc->SampleDesc.Count, 1, 1, DXGI_FORMAT_R32_TYPELESS, DXGI_FORMAT_R32_FLOAT,
//                DXGI_FORMAT_UNKNOWN, DXGI_FORMAT_D32_FLOAT, DXGI_FORMAT_UNKNOWN, DXGI_FORMAT_UNKNOWN, D3D11_USAGE_DEFAULT, false, 0, NULL, NULL, 0);
        g_appDepthBuffer = TextureUtils.createTexture2D(new Texture2DDesc(width, height, GLenum.GL_DEPTH_COMPONENT32F), null);

        // circle of confusion target
        CommonUtil.safeRelease(g_appCoCTexture);
//        hr = g_appCoCTexture.CreateSurface(pd3dDevice, pSurfaceDesc->Width, pSurfaceDesc->Height, pSurfaceDesc->SampleDesc.Count, 1, 1, DXGI_FORMAT_R16_FLOAT, DXGI_FORMAT_R16_FLOAT, DXGI_FORMAT_UNKNOWN,
//                DXGI_FORMAT_UNKNOWN, DXGI_FORMAT_R16_FLOAT, DXGI_FORMAT_UNKNOWN, D3D11_USAGE_DEFAULT, false, 0, NULL, NULL, 0);
        g_appCoCTexture = TextureUtils.createTexture2D(new Texture2DDesc(width, height, GLenum.GL_R16F), null);

        // Depth Of Feild Result surface
        CommonUtil.safeRelease(g_appDofSurface);
//        DXGI_FORMAT appDebugFormat = DXGI_FORMAT_R8G8B8A8_UNORM_SRGB;
//        hr = g_appDofSurface.CreateSurface(pd3dDevice, pSurfaceDesc->Width, pSurfaceDesc->Height, pSurfaceDesc->SampleDesc.Count, 1, 1, DXGI_FORMAT_R8G8B8A8_TYPELESS, appDebugFormat, appDebugFormat,
//                DXGI_FORMAT_UNKNOWN, DXGI_FORMAT_R8G8B8A8_UNORM, DXGI_FORMAT_UNKNOWN, D3D11_USAGE_DEFAULT, false, 0, NULL, NULL, 0);
        g_appDofSurface = TextureUtils.createTexture2D(new Texture2DDesc(width, height, GLenum.GL_RGBA8), null);

        if(g_framebuffer == 0){
            g_framebuffer = gl.glGenFramebuffer();
        }

        gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, g_framebuffer);
        gl.glFramebufferTexture2D(GLenum.GL_FRAMEBUFFER, GLenum.GL_COLOR_ATTACHMENT0, g_appColorBuffer.getTarget(), g_appColorBuffer.getTexture(), 0);
        gl.glFramebufferTexture2D(GLenum.GL_FRAMEBUFFER, GLenum.GL_DEPTH_ATTACHMENT, g_appDepthBuffer.getTarget(), g_appDepthBuffer.getTexture(), 0);
        gl.glDrawBuffers(GLenum.GL_COLOR_ATTACHMENT0);
        gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, 0);

        g_AMD_DofFX_Desc.m_pCircleOfConfusionSRV = g_appCoCTexture;
        g_AMD_DofFX_Desc.m_pColorSRV             = g_appColorBuffer;
        g_AMD_DofFX_Desc.m_pResultUAV            = g_appDofSurface;
        g_AMD_DofFX_Desc.m_maxBlurRadius         = g_maxRadius;
        DepthOfFieldFX.DepthOfFieldFX_Resize(g_AMD_DofFX_Desc);
    }

    @Override
    public void onDestroy() {
        DepthOfFieldFX_Release(g_AMD_DofFX_Desc);

        releaseMeshes();
        releaseShaders();

        gl.glDeleteBuffer(g_d3dViewerCB);
        gl.glDeleteBuffer(g_d3dModelCB);
        gl.glDeleteBuffer(g_d3dCalcDofCb);

        g_appColorBuffer.dispose();
        g_appDepthBuffer.dispose();
        g_appCoCTexture.dispose();
        g_appDofSurface.dispose();
    }

    void setCameraConstantBuffer(int pd3dCameraCB, AMD_Camera pCameraDesc, CFirstPersonCamera pCamera, int nCount)
    {
        /*
        if (pd3dContext == NULL)
        {
            OutputDebugString(AMD_FUNCTION_WIDE_NAME L" received a NULL D3D11 Context pointer \n");
            return;
        }
        if (pd3dCameraCB == NULL)
        {
            OutputDebugString(AMD_FUNCTION_WIDE_NAME L" received a NULL D3D11 Constant Buffer pointer \n");
            return;
        }
        */

//        D3D11_MAPPED_SUBRESOURCE MappedResource;

        CFirstPersonCamera  camera     = pCamera;
        AMD_Camera          cameraDesc = pCameraDesc;

        Matrix4f view         = camera.GetViewMatrix();
        Matrix4f proj         = camera.GetProjMatrix();
//        Matrix4f viewproj     = view * proj;
//        Matrix4f view_inv     = XMMatrixInverse(&XMMatrixDeterminant(view), view);
//        Matrix4f proj_inv     = XMMatrixInverse(&XMMatrixDeterminant(proj), proj);
//        Matrix4f viewproj_inv = XMMatrixInverse(&XMMatrixDeterminant(viewproj), viewproj);
        Matrix4f viewproj     = cameraDesc.m_ViewProjection;
        Matrix4f view_inv     = cameraDesc.m_View_Inv;
        Matrix4f proj_inv     = cameraDesc.m_Projection_Inv;
        Matrix4f viewproj_inv = cameraDesc.m_ViewProjection_Inv;

        Matrix4f.mul(proj, view, viewproj);
        Matrix4f.invert(view, view_inv);
        Matrix4f.invert(proj, proj_inv);
        Matrix4f.invert(viewproj, viewproj_inv);

        cameraDesc.m_View               .load(view);
        cameraDesc.m_Projection         .load(proj);
//        cameraDesc.m_View_Inv           = XMMatrixTranspose(view_inv);
//        cameraDesc.m_Projection_Inv     = XMMatrixTranspose(proj_inv);
//        cameraDesc.m_ViewProjection     = XMMatrixTranspose(viewproj);
//        cameraDesc.m_ViewProjection_Inv = XMMatrixTranspose(viewproj_inv);
        cameraDesc.m_Fov                = camera.GetFOV();
        cameraDesc.m_Aspect             = camera.GetAspect();
        cameraDesc.m_NearPlane          = camera.GetNearClip();
        cameraDesc.m_FarPlane           = camera.GetFarClip();

//        memcpy(&cameraDesc.m_Position, &(camera.GetEyePt()), sizeof(cameraDesc.m_Position));
//        memcpy(&cameraDesc.m_Direction, &(XMVector3Normalize(camera.GetLookAtPt() - camera.GetEyePt())), sizeof(cameraDesc.m_Direction));
//        memcpy(&cameraDesc.m_Up, &(camera.GetWorldUp()), sizeof(cameraDesc.m_Position));
        cameraDesc.m_Position.set(camera.GetEyePt());  // TODO This could opmitimze
        Vector3f.sub(camera.GetLookAtPt(), camera.GetEyePt(), cameraDesc.m_Direction);
        cameraDesc.m_Direction.normalise();
        cameraDesc.m_Up.set(camera.GetWorldUp());

//        HRESULT hr = pd3dContext->Map(pd3dCameraCB, 0, D3D11_MAP_WRITE_DISCARD, 0, &MappedResource);
//        if (hr == S_OK && MappedResource.pData)
//        {
//            memcpy(MappedResource.pData, pCameraDesc, sizeof(S_CAMERA_DESC) * nCount);
//            pd3dContext->Unmap(pd3dCameraCB, 0);
//        }
        ByteBuffer data = CacheBuffer.getCachedByteBuffer(AMD_Camera.SIZE);
        cameraDesc.store(data);
        data.flip();
        gl.glBindBuffer(GLenum.GL_UNIFORM_BUFFER, pd3dCameraCB);
        gl.glBufferSubData(GLenum.GL_UNIFORM_BUFFER, 0, data);
        gl.glBindBuffer(GLenum.GL_UNIFORM_BUFFER, 0);

        CalcDOFParams pParams = g_DOFParams;
        pParams.focalLength   = g_FocalLength / 1000.0f;
        pParams.focusDistance = g_FocalDistance;
        pParams.fStop         = g_fStop;
        pParams.ScreenParamsX = getGLContext().width();
        pParams.ScreenParamsY = getGLContext().height();
        pParams.zNear         = g_ViewerData.m_NearPlane;
        pParams.zFar          = g_ViewerData.m_FarPlane;
        pParams.maxRadius     = g_maxRadius;
        pParams.forceCoc      = g_forceCoc;

        data = CacheBuffer.getCachedByteBuffer(CalcDOFParams.SIZE);
        pParams.store(data);
        data.flip();

        gl.glBindBuffer(GLenum.GL_UNIFORM_BUFFER, g_d3dCalcDofCb);
        gl.glBufferSubData(GLenum.GL_UNIFORM_BUFFER, 0, data);
        gl.glBindBuffer(GLenum.GL_UNIFORM_BUFFER, 0);
    }

    void SetModelConstantBuffer(int pd3dModelCB, S_MODEL_DESC pModelDesc, CFirstPersonCamera pCamera)
    {
        /*
        if (pd3dContext == NULL)
        {
            OutputDebugString(AMD_FUNCTION_WIDE_NAME L" received a NULL D3D11 Context pointer \n");
            return;
        }
        if (pd3dModelCB == NULL)
        {
            OutputDebugString(AMD_FUNCTION_WIDE_NAME L" received a NULL D3D11 Constant Buffer pointer \n");
            return;
        }*/

//        D3D11_MAPPED_SUBRESOURCE MappedResource;

        S_MODEL_DESC       modelDesc = pModelDesc;
        CFirstPersonCamera camera    = pCamera;

        Matrix4f world             = modelDesc.m_World;
        Matrix4f view              = camera.GetViewMatrix();
        Matrix4f proj              = camera.GetProjMatrix();
//        Matrix4f viewproj          = view * proj;
//        Matrix4f worldview         = world * view;
//        Matrix4f worldviewproj     = world * viewproj;
//        Matrix4f worldview_inv     = XMMatrixInverse(&XMMatrixDeterminant(worldview), worldview);
//        Matrix4f worldviewproj_inv = XMMatrixInverse(&XMMatrixDeterminant(worldviewproj), worldviewproj);
        Matrix4f viewproj          = modelDesc.m_WorldViewProjection;
        Matrix4f worldview         = modelDesc.m_WorldView;
        Matrix4f worldviewproj     = modelDesc.m_WorldViewProjection;
        Matrix4f worldview_inv     = modelDesc.m_WorldView_Inv;
        Matrix4f worldviewproj_inv = modelDesc.m_WorldViewProjection_Inv;

        Matrix4f.mul(proj, view, viewproj);
        Matrix4f.mul(view, world, worldview);
        Matrix4f.mul(viewproj, world, worldviewproj);
        Matrix4f.invert(worldview, worldview_inv);
        Matrix4f.invert(worldviewproj, worldviewproj_inv);


//        modelDesc.m_WorldView               = XMMatrixTranspose(worldview);
//        modelDesc.m_WorldView_Inv           = XMMatrixTranspose(worldview_inv);
//        modelDesc.m_WorldViewProjection     = XMMatrixTranspose(worldviewproj);
//        modelDesc.m_WorldViewProjection_Inv = XMMatrixTranspose(worldviewproj_inv);

//        HRESULT hr = pd3dContext->Map(pd3dModelCB, 0, D3D11_MAP_WRITE_DISCARD, 0, &MappedResource);
//        if (hr == S_OK && MappedResource.pData)
//        {
//            memcpy(MappedResource.pData, &modelDesc, sizeof(modelDesc));
//            pd3dContext->Unmap(pd3dModelCB, 0);
//        }

        ByteBuffer data = CacheBuffer.getCachedByteBuffer(S_MODEL_DESC.SIZE);
        modelDesc.store(data);
        data.flip();
        gl.glBindBuffer(GLenum.GL_UNIFORM_BUFFER, pd3dModelCB);
        gl.glBufferSubData(GLenum.GL_UNIFORM_BUFFER, 0, data);
        gl.glBindBuffer(GLenum.GL_UNIFORM_BUFFER, 0);
    }

    void setCameraProjectionParameters()
    {
        float fov = (float)  Math.toDegrees(2 * Math.atan(0.5f * g_sensorWidth / g_FocalLength));
        // Setup the camera's projection parameters
        float fAspectRatio = (float)getGLContext().width() / (float)getGLContext().height();
        g_Viewer.SetProjParams(fov, fAspectRatio, 0.1f, 200.0f);
        m_transformer.getModelViewMat(g_Viewer.GetViewMatrix());
        Matrix4f.decompseRigidMatrix(g_Viewer.GetViewMatrix(), g_Viewer.GetEyePt(), g_Viewer.GetLookAtPt(), g_Viewer.GetWorldUp());
    }

    void compileShaders(){
        final String shaderPath = "amdfx/DepthOfFieldFX/shaders/";
        try {
            g_d3dModelProgram = GLSLProgram.createFromFiles(shaderPath + "SampleVS.vert", shaderPath + "SamplePS.frag");
            g_pCalcCoc = create(shaderPath + "CalcDOF.comp");
            g_pDebugCoc = create(shaderPath + "DebugVisDOF.comp");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void releaseShaders(){
        CommonUtil.safeRelease(g_d3dModelProgram);
        CommonUtil.safeRelease(g_pCalcCoc);
        CommonUtil.safeRelease(g_pDebugCoc);
    }

    private static final GLSLProgram create(String filename) throws IOException{
        CharSequence computeSrc = ShaderLoader.loadShaderFile(filename, false);
        ShaderSourceItem cs_item = new ShaderSourceItem(computeSrc, ShaderType.COMPUTE);
        return GLSLProgram.createFromShaderItems(cs_item);
    }

    void createMeshes()
    {

//        int material_size = SDKmeshMaterial.SIZE;  // sizeof(SDKMESH_MATERIAL);
        // Load the meshe
        g_Model = new AMD_Mesh();
        g_Model.Create("amdfx/DepthOfFieldFX/models/Tank", "TankScene.sdkmesh", true);

//        g_ModelDesc.m_World       = XMMatrixScaling(1.0f, 1.0f, 1.0f);
//        g_ModelDesc.m_World_Inv   = XMMatrixInverse(&XMMatrixDeterminant(g_ModelDesc.m_World), g_ModelDesc.m_World);
        g_ModelDesc.m_Position    .set(0.0f, 0.0f, 0.0f, 1.0f);
        g_ModelDesc.m_Orientation .set(0.0f, 1.0f, 0.0f, 0.0f);
        g_ModelDesc.m_Scale       .set(0.001f, 0.001f, 0.001f, 1.0f);
        g_ModelDesc.m_Ambient     .set(0.1f, 0.1f, 0.1f, 1.0f);
        g_ModelDesc.m_Diffuse     .set(1.0f, 1.0f, 1.0f, 1.0f);
        g_ModelDesc.m_Specular    .set(0.5f, 0.5f, 0.0f, 1.0f);


//        D3D11_BUFFER_DESC b1d_desc;
//        b1d_desc.Usage          = D3D11_USAGE_DYNAMIC;
//        b1d_desc.BindFlags      = D3D11_BIND_CONSTANT_BUFFER;
//        b1d_desc.CPUAccessFlags = D3D11_CPU_ACCESS_WRITE;
//        b1d_desc.MiscFlags      = 0;
//        b1d_desc.ByteWidth      = sizeof(S_MODEL_DESC);
//        hr                      = device->CreateBuffer(&b1d_desc, NULL, &g_d3dModelCB);
//        assert(hr == S_OK);
        g_d3dModelCB = gl.glGenBuffer();
        gl.glBindBuffer(GLenum.GL_UNIFORM_BUFFER, g_d3dModelCB);
        gl.glBufferData(GLenum.GL_UNIFORM_BUFFER, S_MODEL_DESC.SIZE, GLenum.GL_STREAM_DRAW);
        gl.glBindBuffer(GLenum.GL_UNIFORM_BUFFER, 0);
    }

    void releaseMeshes()
    {
        g_Model.Release();
    }

    void setupCamera()
    {
        setCameraParameters();

//        D3D11_BUFFER_DESC b1d_desc;
//        b1d_desc.Usage          = D3D11_USAGE_DYNAMIC;
//        b1d_desc.BindFlags      = D3D11_BIND_CONSTANT_BUFFER;
//        b1d_desc.CPUAccessFlags = D3D11_CPU_ACCESS_WRITE;
//        b1d_desc.MiscFlags      = 0;
//        b1d_desc.ByteWidth      = sizeof(S_CAMERA_DESC);
//        hr                      = device->CreateBuffer(&b1d_desc, NULL, &g_d3dViewerCB);
//        assert(hr == S_OK);
        g_d3dViewerCB = gl.glGenBuffer();
        gl.glBindBuffer(GLenum.GL_UNIFORM_BUFFER, g_d3dViewerCB);
        gl.glBufferData(GLenum.GL_UNIFORM_BUFFER, AMD_Camera.SIZE, GLenum.GL_STREAM_DRAW);
        gl.glBindBuffer(GLenum.GL_UNIFORM_BUFFER, 0);

    }

    void setupDepthOfField()
    {

//        if (hr == S_OK)
//        {
//            D3D11_BUFFER_DESC b1d_desc;
//            b1d_desc.Usage          = D3D11_USAGE_DYNAMIC;
//            b1d_desc.BindFlags      = D3D11_BIND_CONSTANT_BUFFER;
//            b1d_desc.CPUAccessFlags = D3D11_CPU_ACCESS_WRITE;
//            b1d_desc.MiscFlags      = 0;
//            b1d_desc.ByteWidth      = sizeof(CalcDOFParams);
//            hr                      = device->CreateBuffer(&b1d_desc, NULL, &g_d3dCalcDofCb);
//        }
//
//        assert(hr == S_OK);
//        return hr;

        g_d3dCalcDofCb = gl.glGenBuffer();
        gl.glBindBuffer(GLenum.GL_UNIFORM_BUFFER, g_d3dCalcDofCb);
        gl.glBufferData(GLenum.GL_UNIFORM_BUFFER, CalcDOFParams.SIZE, GLenum.GL_STREAM_DRAW);
        gl.glBindBuffer(GLenum.GL_UNIFORM_BUFFER, 0);
    }

    void setCameraParameters() {
        final CameraParameters params = g_defaultCameraParameters[g_defaultCameraParameterIndex];
        // Setup the camera's view parameters
//        float4 vecEye(params.vecEye);
//        float4 vecAt(params.vecAt);
//        float4 vecDir = vecAt.v - vecEye.v;
//        vecDir.f[1]   = 0.0f;
//        vecDir.v      = XMVector3Normalize(vecDir.v);

        g_Viewer.SetViewParams(params.vecEye, params.vecAt, Vector3f.Y_AXIS);

        g_FocalLength = params.focalLength;
        g_FocalDistance = params.focalDistance;
        g_sensorWidth = params.sensorWidth;
        g_fStop = params.fStop;


//        g_HUD.m_GUI.GetSlider(IDC_SLIDER_FOCAL_LENGTH)->SetValue((int)g_FocalLength);
//        g_HUD.m_GUI.GetSlider(IDC_SLIDER_FOCAL_DISTANCE)->SetValue((int)(g_FocalDistance * 100.0f));
//        g_HUD.m_GUI.GetSlider(IDC_SLIDER_SENSOR_WIDTH)->SetValue((int)(g_sensorWidth * 10.0f));
//        g_HUD.m_GUI.GetSlider(IDC_SLIDER_FSTOP)->SetValue(int(g_fStop * 10.0f));
    }
}
