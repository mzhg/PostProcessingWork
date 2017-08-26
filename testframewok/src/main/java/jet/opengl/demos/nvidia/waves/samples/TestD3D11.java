package jet.opengl.demos.nvidia.waves.samples;

import com.nvidia.developer.opengl.app.NvCameraMotionType;
import com.nvidia.developer.opengl.app.NvSampleApp;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import java.io.IOException;

import jet.opengl.demos.nvidia.waves.GFSDK_WaveWorks;
import jet.opengl.demos.nvidia.waves.GFSDK_WaveWorks_Quadtree_Params;
import jet.opengl.demos.nvidia.waves.GFSDK_WaveWorks_Quadtree_Stats;
import jet.opengl.demos.nvidia.waves.GFSDK_WaveWorks_Result;
import jet.opengl.demos.nvidia.waves.GFSDK_WaveWorks_Savestate;
import jet.opengl.demos.nvidia.waves.GFSDK_WaveWorks_Simulation;
import jet.opengl.demos.nvidia.waves.GFSDK_WaveWorks_Simulation_CPU_Threading_Model;
import jet.opengl.demos.nvidia.waves.GFSDK_WaveWorks_Simulation_DetailLevel;
import jet.opengl.demos.nvidia.waves.GFSDK_WaveWorks_Simulation_Params;
import jet.opengl.demos.nvidia.waves.GFSDK_WaveWorks_Simulation_Settings;
import jet.opengl.demos.nvidia.waves.GFSDK_WaveWorks_Simulation_Stats;
import jet.opengl.postprocessing.common.GLCheck;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;

/**
 * Created by mazhen'gui on 2017/8/19.
 */

public class TestD3D11 extends NvSampleApp {

    private static final int ReadbackArchiveSize = 32;
    private static final int ReadbackArchiveInterval = 30;
    private static final int NumMarkersXY = 7, NumMarkers = NumMarkersXY*NumMarkersXY;

    final Vector2f[] g_local_marker_coords = new Vector2f[NumMarkers];
    final Vector2f[] g_remote_marker_coords = new Vector2f[NumMarkers];
    final Vector4f[] g_local_marker_positions = new Vector4f[NumMarkers];
    final Vector4f[] displacements = new Vector4f[NumMarkers];

    private GLFuncProvider gl;
    GFSDK_WaveWorks_Savestate g_hOceanSavestate = null;
    GFSDK_WaveWorks_Simulation g_hOceanSimulation = null;
    final long[] g_LastKickID = {0};
    final long[] g_LastArchivedKickID = {GFSDK_WaveWorks.GFSDK_WaveWorks_InvalidKickID};
    final long[] g_LastReadbackKickID = {GFSDK_WaveWorks.GFSDK_WaveWorks_InvalidKickID};
    int g_RenderLatency = 0;
    int g_ReadbackLatency = 0;
    float g_ReadbackCoord = 0.f;
    OceanSurfaceTest			g_pOceanSurf;
    final GFSDK_WaveWorks_Simulation_Params g_ocean_simulation_param = new GFSDK_WaveWorks_Simulation_Params();
    final GFSDK_WaveWorks_Simulation_Settings g_ocean_simulation_settings = new GFSDK_WaveWorks_Simulation_Settings();
    final Matrix4f g_FlipMat = new Matrix4f();
    final Matrix4f g_Projection = new Matrix4f();
    final Matrix4f g_ModelView = new Matrix4f();
    final IsParameters m_params = new IsParameters();

    final OceanSurfaceParameters			g_ocean_surface_param = new OceanSurfaceParameters();
    final GFSDK_WaveWorks_Quadtree_Params g_ocean_param_quadtree = new GFSDK_WaveWorks_Quadtree_Params();
    final GFSDK_WaveWorks_Quadtree_Stats g_ocean_stats_quadtree = new GFSDK_WaveWorks_Quadtree_Stats();
    final GFSDK_WaveWorks_Simulation_Stats g_ocean_stats_simulation = new GFSDK_WaveWorks_Simulation_Stats();
    final GFSDK_WaveWorks_Simulation_Stats    g_ocean_stats_simulation_filtered = new GFSDK_WaveWorks_Simulation_Stats();
    int   g_max_detail_level;

    boolean g_RenderWireframe = false;
    boolean g_RenderWater = true;
    boolean g_SimulateWater = true;
    boolean g_ForceKick = false;
    boolean g_DebugCam = false;
    boolean g_bShowRemoteMarkers = false;

    private static final int
            SynchronizationMode_None = 0,
            SynchronizationMode_RenderOnly=1,
            SynchronizationMode_Readback=2,
            Num_SynchronizationModes=3;
    int g_bSyncMode = SynchronizationMode_None;

    float g_TessellationLOD = 8.0f;

//    ID3D11ShaderResourceView* g_pSkyCubeMap = NULL;
//    ID3DX11Effect* g_pSkyboxFX = NULL;
//    ID3DX11EffectTechnique* g_pSkyBoxTechnique = NULL;
//    ID3DX11EffectShaderResourceVariable* g_pSkyBoxSkyCubeMapVariable = NULL;
//    ID3DX11EffectVectorVariable* g_pSkyBoxEyePosVariable = NULL;
//    ID3DX11EffectMatrixVariable* g_pSkyBoxMatViewProjVariable = NULL;
//    ID3D11Buffer* g_pSkyBoxVB = NULL;
//    ID3D11InputLayout* g_pSkyboxLayout = NULL;
    SkyBoxRender g_SkyBoxRender = null;

//    ID3D11ShaderResourceView* g_pLogoTex = NULL;
//    ID3D11Buffer* g_pLogoVB = NULL;

// These are borrowed from the effect in g_pOceanSurf
//    ID3DX11EffectTechnique* g_pLogoTechnique = NULL;
//    ID3DX11EffectShaderResourceVariable* g_pLogoTextureVariable = NULL;

//    ID3D11InputLayout* g_pMarkerLayout = NULL;
//    ID3DX11Effect* g_pMarkerFX = NULL;
//    ID3DX11EffectTechnique* g_pMarkerTechnique = NULL;
//    ID3DX11EffectMatrixVariable* g_pMarkerMatViewProjVariable = NULL;
//    ID3DX11EffectVectorVariable* g_pMarkerColor = NULL;
//    ID3D11Buffer* g_pMarkerVB = NULL;
//    ID3D11Buffer* g_pMarkerIB = NULL;

    float g_NearPlane = 1.0f;
    float g_FarPlane = 300000.0f;
    float g_SimulationTime = 0.0f;

    final float kAmplitudeSliderScaleFactor = 3.0f;
    final float kChoppyScaleSliderScaleFactor = 100.0f;
    final float kWindDependencySliderScaleFactor = 100.f;
    final float kTimeScaleSliderScaleFactor = 100.f;
    final float kSkyBlendingSliderScaleFactor = 10.f;
    final float kWindSpeedSliderScaleFactor = 2.5f;
    final float kGeomorphingDegreeSlideScaleFactor = 100.f;
    final float kFoamGenerationThresholdFactor = 100.0f;
    final float kFoamGenerationAmountFactor = 1000.0f;
    final float kFoamDissipationSpeedFactor = 100.0f;
    final float kFoamFadeoutSpeedFactor = 1000.0f;

    final float kWaterScale = 1.f;

    public TestD3D11() {initApp();}

    @Override
    protected void initRendering() {
        getGLContext().setSwapInterval(0);
        getGLContext().setAppTitle("TestD3D11");

        OceanSamplers.createSamplers();
        IsSamplers.createSamplers();

        gl = GLFuncProviderFactory.getGLFuncProvider();
        m_transformer.setTranslation(0.f, -100.534f, 0.f);
        m_transformer.setMotionMode(NvCameraMotionType.FIRST_PERSON);

        // Ocean sim
        GFSDK_WaveWorks.GFSDK_WaveWorks_InitD3D11(GFSDK_WaveWorks.GFSDK_WAVEWORKS_API_GUID);

        g_ocean_simulation_settings.detail_level = GFSDK_WaveWorks_Simulation_DetailLevel.Extreme;
        g_hOceanSimulation = GFSDK_WaveWorks.GFSDK_WaveWorks_Simulation_CreateD3D11(g_ocean_simulation_settings, g_ocean_simulation_param);
//        g_hOceanSavestate = GFSDK_WaveWorks.GFSDK_WaveWorks_Savestate_CreateD3D11(GFSDK_WaveWorks_StatePreserve_All);
        g_ForceKick = true;

        // Ocean object
        g_pOceanSurf = new OceanSurfaceTest();
        g_pOceanSurf.init(g_ocean_surface_param);
        g_pOceanSurf.initQuadTree(g_ocean_param_quadtree);
        GFSDK_WaveWorks.GFSDK_WaveWorks_Simulation_UpdateProperties(g_hOceanSimulation, g_ocean_simulation_settings, g_ocean_simulation_param);
        GFSDK_WaveWorks.GFSDK_WaveWorks_Quadtree_SetFrustumCullMargin(g_pOceanSurf.m_hOceanQuadTree, GFSDK_WaveWorks.GFSDK_WaveWorks_Simulation_GetConservativeMaxDisplacementEstimate(g_hOceanSimulation));

        g_SkyBoxRender = new SkyBoxRender(500);
        try {
            g_SkyBoxRender.loadCubemapFromDDSFile("nvidia/WaveWorks/textures/sky_cube.dds");
            g_SkyBoxRender.setCubemapSampler(OceanSamplers.g_SamplerLinearMipmapClamp);
        } catch (IOException e) {
            e.printStackTrace();
        }

        g_FlipMat.m11 = g_FlipMat.m22 = 0;
        g_FlipMat.m12 = -1;
        g_FlipMat.m21 = 1;

        GLCheck.checkError();
    }

    @Override
    public void display() {
        updateWaveStage();


        // If the settings dialog is being shown, then
        // render it instead of rendering the app's scene
//        if( g_SettingsDlg.IsActive() )
//        {
//            g_SettingsDlg.OnRender( fElapsedTime );
//            return;
//        }

        // Clear the render target and depth stencil
//        float ClearColor[4] = { 0.0f, 0.5f, 0.6f, 0.8f };
//        ID3D11RenderTargetView* pRTV = DXUTGetD3D11RenderTargetView();
//        pDC->ClearRenderTargetView( pRTV, ClearColor );
//        ID3D11DepthStencilView* pDSV = DXUTGetD3D11DepthStencilView();
//        pDC->ClearDepthStencilView( pDSV, D3D11_CLEAR_DEPTH, 1.0, 0 );
        gl.glDisable(GLenum.GL_CULL_FACE);
        gl.glDisable(GLenum.GL_BLEND);
        gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, 0);
        gl.glClearColor(0.0f, 0.5f, 0.6f, 0.8f);
        gl.glClearDepthf(1.0f);
        gl.glClear(GLenum.GL_COLOR_BUFFER_BIT | GLenum.GL_DEPTH_BUFFER_BIT);
        gl.glViewport(0,0, getGLContext().width(), getGLContext().height());
        gl.glColorMask(true, true, true, true);
        //if(g_RenderWater)
        {
            RenderSkybox(/*pDC*/);
        }

//        if(true) return;

//        UpdateMarkers();
        if(g_ocean_simulation_settings.readback_displacements)
            RenderLocalMarkers(/*pDC*/);
        if(g_bShowRemoteMarkers)
            RenderRemoteMarkers(/*pDC*/);

        if(g_RenderWater)
        {
//            const XMMATRIX matView = XMMATRIX(kWaterScale, 0, 0, 0, 0, 0, kWaterScale, 0, 0, kWaterScale, 0, 0, 0, 0, 0, 1) *g_Camera.GetViewMatrix();
//            const XMMATRIX matProj = g_Camera.GetProjMatrix();
            buildRenderParams();
            g_FlipMat.setIdentity();
            g_FlipMat.m11 = g_FlipMat.m22 = 0;
            g_FlipMat.m12 = -kWaterScale;
            g_FlipMat.m21 = kWaterScale;
            g_FlipMat.m00 = kWaterScale;

            Matrix4f.mul(m_params.g_ModelViewMatrix, g_FlipMat,m_params.g_ModelViewMatrix); // Rotate the model view
            Matrix4f.mul(m_params.g_Projection, m_params.g_ModelViewMatrix, m_params.g_ModelViewProjectionMatrix);
            gl.glEnable(GLenum.GL_DEPTH_TEST);

            gl.glActiveTexture(GLenum.GL_TEXTURE15);
            gl.glBindTexture(GLenum.GL_TEXTURE_2D, 0);
            gl.glBindSampler(15, IsSamplers.g_SamplerTrilinearBorder);

            g_pOceanSurf.renderShaded(m_params,g_hOceanSimulation, g_hOceanSavestate, g_DebugCam);
            GLCheck.checkError();

//            if (g_RenderWireframe)
//                g_pOceanSurf.renderWireframe(pDC, matView,matProj,g_hOceanSimulation, g_hOceanSavestate, g_DebugCam);
//            else
//                g_pOceanSurf.renderShaded(pDC, matView,matProj,g_hOceanSimulation, g_hOceanSavestate, g_DebugCam);

            g_pOceanSurf.getQuadTreeStats(g_ocean_stats_quadtree);

            for(int i = 15; i >=0; i--){
                gl.glActiveTexture(GLenum.GL_TEXTURE0 + i);
                gl.glBindTexture(GLenum.GL_TEXTURE_2D, 0);
                gl.glBindSampler(i, 0);
            }

            for(int i = 4; i >=0; i--){
                gl.glBindBufferBase(GLenum.GL_UNIFORM_BUFFER, i, 0);
            }
        }

        RenderLogo(/*pDC*/);

//        if(!g_pTestParams->ShouldTakeScreenshot())
//        {
//            g_HUD.OnRender( fElapsedTime );
//            g_SampleUI.OnRender( fElapsedTime );
//            RenderText( fTime );
//        }
        GLCheck.checkError();
    }

    void updateWaveState2(){
        g_SimulationTime += getFrameDeltaTime()*5;

        if(g_SimulateWater || g_ForceKick || (GFSDK_WaveWorks_Result.NONE==GFSDK_WaveWorks.GFSDK_WaveWorks_Simulation_GetStagingCursor(g_hOceanSimulation,null)))
        {
            GFSDK_WaveWorks.GFSDK_WaveWorks_Simulation_SetTime(g_hOceanSimulation, g_SimulationTime);  GLCheck.checkError();
            GFSDK_WaveWorks.GFSDK_WaveWorks_Simulation_KickD3D11(g_hOceanSimulation, g_LastKickID, /*pDC,*/ g_hOceanSavestate);

            if(g_bSyncMode >= SynchronizationMode_RenderOnly)
            {
                // Block until the just-submitted kick is ready to render
                long[] stagingCursorKickID = {g_LastKickID[0] - 1};	// Just ensure that the initial value is different from last kick,
                // so that we continue waiting if the staging cursor is empty
                GFSDK_WaveWorks.GFSDK_WaveWorks_Simulation_GetStagingCursor(g_hOceanSimulation, stagingCursorKickID);
                while(stagingCursorKickID[0] != g_LastKickID[0])
                {
                    final boolean doBlock = true;
                    GFSDK_WaveWorks.GFSDK_WaveWorks_Simulation_AdvanceStagingCursorD3D11(g_hOceanSimulation, doBlock, /*pDC,*/ g_hOceanSavestate);
                    GFSDK_WaveWorks.GFSDK_WaveWorks_Simulation_GetStagingCursor(g_hOceanSimulation, stagingCursorKickID);
                }

                if(g_bSyncMode >= SynchronizationMode_Readback && g_ocean_simulation_settings.readback_displacements)
                {
                    long[] readbackCursorKickID = {g_LastKickID[0] - 1};	// Just ensure that the initial value is different from last kick,
                    // so that we continue waiting if the staging cursor is empty
                    while(readbackCursorKickID[0] != g_LastKickID[0])
                    {
                        final boolean doBlock = true;
                        GFSDK_WaveWorks.GFSDK_WaveWorks_Simulation_AdvanceReadbackCursor(g_hOceanSimulation, doBlock);
                        GFSDK_WaveWorks.GFSDK_WaveWorks_Simulation_GetReadbackCursor(g_hOceanSimulation, readbackCursorKickID);
                    }
                }
            }
            else
            {
                // Keep feeding the simulation pipeline until it is full - this loop should skip in all
                // cases except the first iteration, when the simulation pipeline is first 'primed'
                while(GFSDK_WaveWorks_Result.NONE==GFSDK_WaveWorks.GFSDK_WaveWorks_Simulation_GetStagingCursor(g_hOceanSimulation,null))
                {
                    GFSDK_WaveWorks.GFSDK_WaveWorks_Simulation_SetTime(g_hOceanSimulation, g_SimulationTime);
                    GFSDK_WaveWorks.GFSDK_WaveWorks_Simulation_KickD3D11(g_hOceanSimulation, g_LastKickID, /*pDC,*/ g_hOceanSavestate);
                }
            }

//            GFSDK_WaveWorks.GFSDK_WaveWorks_Savestate_RestoreD3D11(g_hOceanSavestate, pDC);
            g_ForceKick = false;

            // Exercise the readback archiving API
            if(GFSDK_WaveWorks_Result.OK == GFSDK_WaveWorks.GFSDK_WaveWorks_Simulation_GetReadbackCursor(g_hOceanSimulation, g_LastReadbackKickID))
            {
                if((g_LastReadbackKickID[0]-g_LastArchivedKickID[0]) > ReadbackArchiveInterval)
                {
                    GFSDK_WaveWorks.GFSDK_WaveWorks_Simulation_ArchiveDisplacements(g_hOceanSimulation);
                    g_LastArchivedKickID[0] = g_LastReadbackKickID[0];
                }
            }
        }

        // deduce the rendering latency of the WaveWorks pipeline
        {
            long[] staging_cursor_kickID = {0};
            GFSDK_WaveWorks.GFSDK_WaveWorks_Simulation_GetStagingCursor(g_hOceanSimulation,staging_cursor_kickID);
            g_RenderLatency = (int)(g_LastKickID[0] - staging_cursor_kickID[0]);
        }

        // likewise with the readback latency
        if(g_ocean_simulation_settings.readback_displacements)
        {
            long[] readback_cursor_kickID = {0};
            if(GFSDK_WaveWorks_Result.OK == GFSDK_WaveWorks.GFSDK_WaveWorks_Simulation_GetReadbackCursor(g_hOceanSimulation,readback_cursor_kickID))
            {
                g_ReadbackLatency = (int)(g_LastKickID[0] - readback_cursor_kickID[0]);
            }
            else
            {
                g_ReadbackLatency = -1;
            }
        }
        else
        {
            g_ReadbackLatency = -1;
        }

        // getting simulation timings
        GFSDK_WaveWorks.GFSDK_WaveWorks_Simulation_GetStats(g_hOceanSimulation,g_ocean_stats_simulation);

        // Performing treadbacks and raycasts
//        UpdateReadbackPositions();
//        UpdateRaycastPositions();

        // exponential filtering for stats
        g_ocean_stats_simulation_filtered.CPU_main_thread_wait_time			= g_ocean_stats_simulation_filtered.CPU_main_thread_wait_time*0.98f + 0.02f*g_ocean_stats_simulation.CPU_main_thread_wait_time;
        g_ocean_stats_simulation_filtered.CPU_threads_start_to_finish_time  = g_ocean_stats_simulation_filtered.CPU_threads_start_to_finish_time*0.98f + 0.02f*g_ocean_stats_simulation.CPU_threads_start_to_finish_time;
        g_ocean_stats_simulation_filtered.CPU_threads_total_time			= g_ocean_stats_simulation_filtered.CPU_threads_total_time*0.98f + 0.02f*g_ocean_stats_simulation.CPU_threads_total_time;
        g_ocean_stats_simulation_filtered.GPU_simulation_time				= g_ocean_stats_simulation_filtered.GPU_simulation_time*0.98f + 0.02f*g_ocean_stats_simulation.GPU_simulation_time;
        g_ocean_stats_simulation_filtered.GPU_FFT_simulation_time			= g_ocean_stats_simulation_filtered.GPU_FFT_simulation_time*0.98f + 0.02f*g_ocean_stats_simulation.GPU_FFT_simulation_time;
        g_ocean_stats_simulation_filtered.GPU_gfx_time						= g_ocean_stats_simulation_filtered.GPU_gfx_time*0.98f + 0.02f*g_ocean_stats_simulation.GPU_gfx_time;
        g_ocean_stats_simulation_filtered.GPU_update_time					= g_ocean_stats_simulation_filtered.GPU_update_time*0.98f + 0.02f*g_ocean_stats_simulation.GPU_update_time;

        GLCheck.checkError();
    }

    void updateWaveStage(){
        g_SimulationTime += getFrameDeltaTime();
        if(g_SimulateWater || g_ForceKick || (GFSDK_WaveWorks_Result.NONE==GFSDK_WaveWorks.GFSDK_WaveWorks_Simulation_GetStagingCursor(g_hOceanSimulation,null)))
        {
            GFSDK_WaveWorks.GFSDK_WaveWorks_Simulation_SetTime(g_hOceanSimulation, g_SimulationTime);
            GFSDK_WaveWorks.GFSDK_WaveWorks_Simulation_KickD3D11(g_hOceanSimulation, g_LastKickID, g_hOceanSavestate);

            if(g_bSyncMode >= SynchronizationMode_RenderOnly)
            {
                // Block until the just-submitted kick is ready to render
                long[] stagingCursorKickID = {g_LastKickID[0] - 1};	// Just ensure that the initial value is different from last kick,
                // so that we continue waiting if the staging cursor is empty
                GFSDK_WaveWorks.GFSDK_WaveWorks_Simulation_GetStagingCursor(g_hOceanSimulation, stagingCursorKickID);
                while(stagingCursorKickID[0] != g_LastKickID[0])
                {
                    final boolean doBlock = true;
                    GFSDK_WaveWorks.GFSDK_WaveWorks_Simulation_AdvanceStagingCursorD3D11(g_hOceanSimulation, doBlock, g_hOceanSavestate);
                    GFSDK_WaveWorks.GFSDK_WaveWorks_Simulation_GetStagingCursor(g_hOceanSimulation, stagingCursorKickID);
                }

                if(g_bSyncMode >= SynchronizationMode_Readback && g_ocean_simulation_settings.readback_displacements)
                {
                    long[] readbackCursorKickID = {g_LastKickID[0] - 1};	// Just ensure that the initial value is different from last kick,
                    // so that we continue waiting if the staging cursor is empty
                    while(readbackCursorKickID[0] != g_LastKickID[0])
                    {
                        final boolean doBlock = true;
                        GFSDK_WaveWorks.GFSDK_WaveWorks_Simulation_AdvanceReadbackCursor(g_hOceanSimulation, doBlock);
                        GFSDK_WaveWorks.GFSDK_WaveWorks_Simulation_GetReadbackCursor(g_hOceanSimulation, readbackCursorKickID);
                    }
                }
            }
            else
            {
                // Keep feeding the simulation pipeline until it is full - this loop should skip in all
                // cases except the first iteration, when the simulation pipeline is first 'primed'
                while(GFSDK_WaveWorks_Result.NONE==GFSDK_WaveWorks.GFSDK_WaveWorks_Simulation_GetStagingCursor(g_hOceanSimulation, null))
                {
                    GFSDK_WaveWorks.GFSDK_WaveWorks_Simulation_SetTime(g_hOceanSimulation, g_SimulationTime);
                    GFSDK_WaveWorks.GFSDK_WaveWorks_Simulation_KickD3D11(g_hOceanSimulation, g_LastKickID, g_hOceanSavestate);
                }
            }

//            GFSDK_WaveWorks.GFSDK_WaveWorks_Savestate_RestoreD3D11(g_hOceanSavestate, pDC);
            g_ForceKick = false;

            // Exercise the readback archiving API
            if(GFSDK_WaveWorks_Result.OK == GFSDK_WaveWorks.GFSDK_WaveWorks_Simulation_GetReadbackCursor(g_hOceanSimulation, g_LastReadbackKickID))
            {
                if((g_LastReadbackKickID[0]-g_LastArchivedKickID[0]) > ReadbackArchiveInterval)
                {
                    GFSDK_WaveWorks.GFSDK_WaveWorks_Simulation_ArchiveDisplacements(g_hOceanSimulation);
                    g_LastArchivedKickID[0] = g_LastReadbackKickID[0];
                }
            }
        }

        // deduce the rendering latency of the WaveWorks pipeline
        {
            long[] staging_cursor_kickID = {0};
            GFSDK_WaveWorks.GFSDK_WaveWorks_Simulation_GetStagingCursor(g_hOceanSimulation,staging_cursor_kickID);
            g_RenderLatency = (int)(g_LastKickID[0] - staging_cursor_kickID[0]);
        }

        // likewise with the readback latency
        if(g_ocean_simulation_settings.readback_displacements)
        {
            long[] readback_cursor_kickID = {0};
            if(GFSDK_WaveWorks_Result.OK == GFSDK_WaveWorks.GFSDK_WaveWorks_Simulation_GetReadbackCursor(g_hOceanSimulation,readback_cursor_kickID))
            {
                g_ReadbackLatency = (int)(g_LastKickID[0] - readback_cursor_kickID[0]);
            }
            else
            {
                g_ReadbackLatency = -1;
            }
        }
        else
        {
            g_ReadbackLatency = -1;
        }

        // getting simulation timings
        GFSDK_WaveWorks.GFSDK_WaveWorks_Simulation_GetStats(g_hOceanSimulation,g_ocean_stats_simulation);

        // exponential filtering for stats
        g_ocean_stats_simulation_filtered.CPU_main_thread_wait_time			= g_ocean_stats_simulation_filtered.CPU_main_thread_wait_time*0.98f + 0.02f*g_ocean_stats_simulation.CPU_main_thread_wait_time;
        g_ocean_stats_simulation_filtered.CPU_threads_start_to_finish_time  = g_ocean_stats_simulation_filtered.CPU_threads_start_to_finish_time*0.98f + 0.02f*g_ocean_stats_simulation.CPU_threads_start_to_finish_time;
        g_ocean_stats_simulation_filtered.CPU_threads_total_time			= g_ocean_stats_simulation_filtered.CPU_threads_total_time*0.98f + 0.02f*g_ocean_stats_simulation.CPU_threads_total_time;
        g_ocean_stats_simulation_filtered.GPU_simulation_time				= g_ocean_stats_simulation_filtered.GPU_simulation_time*0.98f + 0.02f*g_ocean_stats_simulation.GPU_simulation_time;
        g_ocean_stats_simulation_filtered.GPU_FFT_simulation_time			= g_ocean_stats_simulation_filtered.GPU_FFT_simulation_time*0.98f + 0.02f*g_ocean_stats_simulation.GPU_FFT_simulation_time;
        g_ocean_stats_simulation_filtered.GPU_gfx_time						= g_ocean_stats_simulation_filtered.GPU_gfx_time*0.98f + 0.02f*g_ocean_stats_simulation.GPU_gfx_time;
        g_ocean_stats_simulation_filtered.GPU_update_time					= g_ocean_stats_simulation_filtered.GPU_update_time*0.98f + 0.02f*g_ocean_stats_simulation.GPU_update_time;

    }

    void RenderMarkers(Vector4f[] pMarkerPositions, int num_markers, Vector4f color){}
    void RenderLocalMarkers(/*ID3D11DeviceContext* pDC*/)
    {
//        RenderMarkers(pDC, g_local_marker_positions, NumMarkers, XMFLOAT4(1.f, 0.f, 0.f, 1.f));
    }

    void RenderRemoteMarkers(/*ID3D11DeviceContext* pDC*/){}
    void UpdateMarkers(){
        // Find where the camera vector intersects mean sea level
//        XMVECTOR eye_pos = g_Camera.GetEyePt();
//        XMVECTOR lookat_pos = g_Camera.GetLookAtPt();
        final Vector3f eye_pos = new Vector3f();
        final Vector3f lookat_pos = new Vector3f();
        Matrix4f.decompseRigidMatrix(m_params.g_ModelViewMatrix, eye_pos, lookat_pos, null);

        final float intersectionHeight = g_ocean_param_quadtree.sea_level;
        final float lambda = (intersectionHeight - eye_pos.getY())/(lookat_pos.getY() - eye_pos.getY());
//        const XMVECTOR sea_level_pos = (1.f - lambda) * eye_pos + lambda * lookat_pos;
        final Vector3f sea_level_pos = Vector3f.linear(eye_pos, 1.f - lambda, lookat_pos, lambda, null);
//        const XMVECTOR sea_level_xy = XMVectorSet(XMVectorGetX(sea_level_pos), XMVectorGetZ(sea_level_pos), 0, 0);
        final Vector3f sea_level_xy = new Vector3f(sea_level_pos.x, sea_level_pos.z, 0);

        // Update local marker coords, we could need them any time for remote
        for(int x = 0; x != NumMarkersXY; ++x)
        {
            for(int y = 0; y != NumMarkersXY; ++y)
            {
//                XMVECTOR offset = XMVectorSet(2.f * (x - ((NumMarkersXY - 1) / 2)), 2.f * (y - ((NumMarkersXY - 1) / 2)), 0, 0);
//                XMVECTOR newPos = sea_level_xy / kWaterScale + offset;
                float offsetX = 2.f * (x - ((NumMarkersXY - 1) / 2));
                float offsetY = 2.f * (y - ((NumMarkersXY - 1) / 2));
                float newPosX = sea_level_xy.x/kWaterScale + offsetX;
                float newPosY = sea_level_xy.y/kWaterScale + offsetY;

                g_local_marker_coords[y * NumMarkersXY + x].x = newPosX;
                g_local_marker_coords[y * NumMarkersXY + x].x = newPosY;
            }
        }

        // Do local readback, if requested
        if(g_ocean_simulation_settings.readback_displacements)
        {
            if(g_ReadbackCoord >= 1.f)
            {
                final float coord = g_ReadbackCoord - (g_LastReadbackKickID[0]-g_LastArchivedKickID[0]) * 1.f/ReadbackArchiveInterval;
                GFSDK_WaveWorks.GFSDK_WaveWorks_Simulation_GetArchivedDisplacements(g_hOceanSimulation, coord, g_local_marker_coords, displacements, NumMarkers);
            }
            else
            {
                GFSDK_WaveWorks.GFSDK_WaveWorks_Simulation_GetDisplacements(g_hOceanSimulation, g_local_marker_coords, displacements, NumMarkers);
            }

            for(int ix = 0; ix != NumMarkers; ++ix)
            {
                g_local_marker_positions[ix].x = displacements[ix].x + g_local_marker_coords[ix].x;
                g_local_marker_positions[ix].y = displacements[ix].y + g_local_marker_coords[ix].y;
                g_local_marker_positions[ix].z = displacements[ix].z;
                g_local_marker_positions[ix].w = 1.f;
            }
        }

//        if(g_bShowRemoteMarkers && NULL != g_pNetworkClient)
//        {
//            g_pNetworkClient->RequestRemoteMarkerPositions(g_remote_marker_coords,NumMarkers);
//        }
    }

    void RenderSkybox(/*ID3D11DeviceContext* pDC*/) {
        g_FlipMat.setIdentity();
        g_FlipMat.m11 = g_FlipMat.m22 = 0;
        g_FlipMat.m12 = -1;
        g_FlipMat.m21 = 1;
        g_FlipMat.m00 = 1;
        g_ModelView.load(m_transformer.getRotationMat());
        Matrix4f.mul(g_ModelView, g_FlipMat, g_ModelView);
        {
            gl.glDisable(GLenum.GL_DEPTH_TEST);
            g_SkyBoxRender.setProjectionMatrix(g_Projection);
            g_SkyBoxRender.setRotateMatrix(g_ModelView);
            g_SkyBoxRender.draw();
        }
    }

    void RenderLogo(){}

    private void buildRenderParams(){
        m_transformer.getModelViewMat(m_params.g_ModelViewMatrix);
        Matrix4f.decompseRigidMatrix(m_params.g_ModelViewMatrix, m_params.g_CameraPosition, null, null, m_params.g_CameraDirection);
        m_params.g_CameraDirection.scale(-1);
        m_params.g_Projection = g_Projection;
        Matrix4f.mul(g_Projection, m_params.g_ModelViewMatrix, m_params.g_ModelViewProjectionMatrix);

        m_params.g_Time = g_SimulationTime;
        m_params.g_GerstnerSteepness = 1.f;
        m_params.g_BaseGerstnerAmplitude = 0.279f;
        m_params.g_BaseGerstnerWavelength = 3.912f;
        m_params.g_BaseGerstnerSpeed = 2.472f;
        m_params.g_BaseGerstnerParallelness = 0.2f;
        m_params.g_WindDirection.set(0.8f, 0.6f);
    }

    @Override
    protected void reshape(int width, int height) {
        if(width <=0 || height <= 0)
            return;

        Matrix4f.perspective(45, (float)width/height, g_NearPlane, g_FarPlane, g_Projection);
        m_params.g_ZNear = g_NearPlane;
        m_params.g_ZFar = g_FarPlane;
        m_params.g_Projection = g_Projection;

        m_params.g_ScreenSizeInv.x = 1.0f/width;
        m_params.g_ScreenSizeInv.y = 1.0f/height;
    }

    //--------------------------------------------------------------------------------------
// Initialize the app
//--------------------------------------------------------------------------------------
    void initApp()
    {
        for(int i = 0; i < g_local_marker_coords.length; i++) {
            g_local_marker_coords[i] = new Vector2f();
            g_remote_marker_coords[i] = new Vector2f();
            g_local_marker_positions[i] = new Vector4f();
        }

        g_ocean_param_quadtree.min_patch_length		= 40;
        g_ocean_param_quadtree.upper_grid_coverage	= 64.0f;
        g_ocean_param_quadtree.mesh_dim				= 128;
        g_ocean_param_quadtree.sea_level			= 0.0f;
        g_ocean_param_quadtree.auto_root_lod		= 10;
        g_ocean_param_quadtree.tessellation_lod		= 40.0f;
        g_ocean_param_quadtree.geomorphing_degree	= 1.f;
        g_ocean_param_quadtree.use_tessellation     = true;
        g_ocean_param_quadtree.enable_CPU_timers	= true;

        g_ocean_simulation_param.time_scale				= 1.0f;
        g_ocean_simulation_param.wave_amplitude			= 1.0f;
        g_ocean_simulation_param.wind_dir			    .set(-0.8f, -0.6f);
        g_ocean_simulation_param.wind_speed				= 119.0f;
        g_ocean_simulation_param.wind_dependency			= 0.98f;
        g_ocean_simulation_param.choppy_scale				= 1.f;
        g_ocean_simulation_param.small_wave_fraction		= 0.f;
        g_ocean_simulation_param.foam_dissipation_speed	= 0.6f;
        g_ocean_simulation_param.foam_falloff_speed		= 0.985f;
        g_ocean_simulation_param.foam_generation_amount	= 0.12f;
        g_ocean_simulation_param.foam_generation_threshold = 0.37f;

        g_ocean_simulation_settings.fft_period						= 1000.0f;
        g_ocean_simulation_settings.detail_level					= GFSDK_WaveWorks_Simulation_DetailLevel.High;
        g_ocean_simulation_settings.readback_displacements			= true;
        g_ocean_simulation_settings.num_readback_FIFO_entries		= ReadbackArchiveSize;
        g_ocean_simulation_settings.aniso_level						= 4;
        g_ocean_simulation_settings.CPU_simulation_threading_model  = GFSDK_WaveWorks_Simulation_CPU_Threading_Model.GFSDK_WaveWorks_Simulation_CPU_Threading_Model_Automatic;
        g_ocean_simulation_settings.use_Beaufort_scale				= true;
        g_ocean_simulation_settings.num_GPUs						= 1;
        g_ocean_simulation_settings.enable_CUDA_timers				= true;
        g_ocean_simulation_settings.enable_gfx_timers				= true;
        g_ocean_simulation_settings.enable_CPU_timers				= true;

        g_ocean_surface_param.sky_color			.set(0.38f, 0.45f, 0.56f, 0);
        g_ocean_surface_param.waterbody_color   .set(0.07f, 0.15f, 0.2f, 0);
        g_ocean_surface_param.sky_blending		= 100.0f;

//        memset(&g_ocean_stats_simulation_filtered, 0, sizeof(g_ocean_stats_simulation_filtered));

        // Initialize dialogs
//        g_SettingsDlg.Init( &g_DialogResourceManager );
//        g_HUD.Init( &g_DialogResourceManager );
//        g_SampleUI.Init( &g_DialogResourceManager );

//        g_Camera.SetRotateButtons( true, false, false );
//        g_Camera.SetScalers(0.003f, 4000.0f);

//        g_HUD.SetCallback( OnGUIEvent );
//        g_SampleUI.SetCallback( OnGUIEvent );

//        g_pNetworkClient = Client::Create();
//        if(g_pNetworkClient) {
//            UpdateWaveWorksParams(g_ocean_simulation_param,g_pNetworkClient->GetSimulationConfig());
//        }
//
//        AddGUISet();
//        UpdateServerControlledUI();


    }
}
