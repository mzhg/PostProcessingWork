package jet.opengl.demos.nvidia.waves.samples;

import com.nvidia.developer.opengl.app.NvCameraMotionType;
import com.nvidia.developer.opengl.app.NvSampleApp;
import com.nvidia.developer.opengl.utils.NvImage;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.ReadableVector3f;
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
import jet.opengl.postprocessing.shader.FullscreenProgram;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.TextureUtils;
import jet.opengl.postprocessing.util.CommonUtil;

/**
 * Created by mazhen'gui on 2017/7/27.
 */

public class SampleD3D11 extends NvSampleApp implements Constants{
    private static final int
            SynchronizationMode_None = 0,
            SynchronizationMode_RenderOnly = 1,
            SynchronizationMode_Readback = 2,
            Num_SynchronizationModes = 3;

    private static final int ReadbackArchiveSize = 50;
    private static final int ReadbackArchiveInterval = 5;

    static final int NumMarkersXY = 10;
    static final int NumMarkers = NumMarkersXY*NumMarkersXY;
    //--------------------------------------------------------------------------------------
    // Global variables
    //--------------------------------------------------------------------------------------
    boolean                    g_bShowHelp = false;    // If true, it renders the UI control text
    boolean                    g_bShowUI = true;       // UI can be hidden e.g. for video capture

    GFSDK_WaveWorks_Savestate g_hOceanSavestate = null ;
    GFSDK_WaveWorks_Simulation g_hOceanSimulation = null;

    long[] g_LastKickID = new long[1];
    long[] g_LastArchivedKickID = {GFSDK_WaveWorks.GFSDK_WaveWorks_InvalidKickID};
    long[] g_LastReadbackKickID = {GFSDK_WaveWorks.GFSDK_WaveWorks_InvalidKickID};
    int g_RenderLatency = 0;
    int g_ReadbackLatency = 0;
    float g_ReadbackCoord = 0.f;

    OceanSurface							g_pOceanSurf = null;
    DistanceField					        g_pDistanceField = null;

    final GFSDK_WaveWorks_Simulation_Params g_ocean_simulation_param = new GFSDK_WaveWorks_Simulation_Params();
    final GFSDK_WaveWorks_Simulation_Settings g_ocean_simulation_settings = new GFSDK_WaveWorks_Simulation_Settings();
    final GFSDK_WaveWorks_Simulation_Stats g_ocean_simulation_stats = new GFSDK_WaveWorks_Simulation_Stats();
    final GFSDK_WaveWorks_Simulation_Stats		g_ocean_simulation_stats_filtered = new GFSDK_WaveWorks_Simulation_Stats();

    final GFSDK_WaveWorks_Quadtree_Params g_ocean_quadtree_param = new GFSDK_WaveWorks_Quadtree_Params();
    final GFSDK_WaveWorks_Quadtree_Stats g_ocean_quadtree_stats = new GFSDK_WaveWorks_Quadtree_Stats();
    GFSDK_WaveWorks_Simulation_DetailLevel g_max_detail_level = GFSDK_WaveWorks_Simulation_DetailLevel.Normal;

    final Vector2f g_WindDir = new Vector2f(0.8f, 0.6f);
    boolean g_Wireframe = false;
    boolean g_SimulateWater = true;
    boolean g_ForceKick = false;
    boolean g_QueryStats = false;
    boolean g_enableShoreEffects = true;
    float g_TessellationLOD = 50.0f;
    float g_NearPlane = 1.0f;
    float g_FarPlane = 25000.0f;
    double g_SimulationTime = 0.0;
    float g_FrameTime = 0.0f;

    float g_GerstnerSteepness = 1.0f;
    float g_BaseGerstnerAmplitude = 0.279f;
    float g_BaseGerstnerWavelength = 3.912f;
    float g_BaseGerstnerSpeed = 2.472f;
    float g_GerstnerParallelity = 0.2f;
    float g_ShoreTime = 0.0f;
    int g_bSyncMode = SynchronizationMode_None;

    CTerrainOcean g_Terrain;
//    ID3DX11Effect*      g_pEffect       = NULL;
//
    FullscreenProgram g_pLogoTechnique = null;
//    ID3DX11EffectShaderResourceVariable* g_pLogoTextureVariable = NULL;
    Texture2D g_pLogoTex = null;
//    ID3D11Buffer* g_pLogoVB = NULL;
//    ID3D11InputLayout* g_pSkyboxLayout = NULL;



//    D3D11_QUERY_DATA_PIPELINE_STATISTICS g_PipelineQueryData;
    int        g_pPipelineQuery= 0;

// Readbacks and raycasts related globals
//    enum { NumMarkersXY = 10, NumMarkers = NumMarkersXY*NumMarkersXY };

    final Vector2f[] g_readback_marker_coords = new Vector2f[NumMarkers];
    final Vector4f[] g_readback_marker_positions = new Vector4f[NumMarkers];

    final Vector3f[] g_raycast_origins = new Vector3f[NumMarkers];
    final Vector3f[] g_raycast_directions = new Vector3f[NumMarkers];
    final Vector3f[] g_raycast_hitpoints = new Vector3f[NumMarkers];
    final boolean[]	 g_raycast_hittestresults = new boolean[NumMarkers];
    static long g_IntersectRaysPerfCounter, g_IntersectRaysPerfCounterOld, g_IntersectRaysPerfFrequency;
    float		g_IntersectRaysTime;

    final int kSliderRange = 100;
    final float kMinWindDep = 0.f;
    final float kMaxWindDep = 1.f;
    final float kMinTimeScale = 0.25f;
    final float kMaxTimeScale = 1.f;
    final float kMinWindSpeedBeaufort = 2.0f;
    final float kMaxWindSpeedBeaufort = 4.0f;
    boolean m_printOcen;

    final Matrix4f m_proj = new Matrix4f();
    private GLFuncProvider gl;
    private final IsParameters m_params = new IsParameters();

    public SampleD3D11(){
        for(int i = 0; i < NumMarkers; i++){
            g_readback_marker_coords[i] = new Vector2f();
            g_readback_marker_positions[i] = new Vector4f();

            g_raycast_origins[i] = new Vector3f();
            g_raycast_directions[i] = new Vector3f();
            g_raycast_hitpoints[i] = new Vector3f();
        }
    }

    @Override
    protected void initRendering() {
        getGLContext().setSwapInterval(0);
        initApp();

        IsSamplers.createSamplers();
        gl = GLFuncProviderFactory.getGLFuncProvider();
        m_transformer.setTranslation(100.f, -8.0f, -200.f);
        m_transformer.setMotionMode(NvCameraMotionType.FIRST_PERSON);

        // Ocean sim
        GFSDK_WaveWorks.GFSDK_WaveWorks_InitD3D11(GFSDK_WaveWorks.GFSDK_WAVEWORKS_API_GUID);

        g_ocean_simulation_settings.detail_level = GFSDK_WaveWorks_Simulation_DetailLevel.Extreme;
        g_hOceanSimulation = GFSDK_WaveWorks.GFSDK_WaveWorks_Simulation_CreateD3D11(g_ocean_simulation_settings, g_ocean_simulation_param);
//        g_hOceanSavestate = GFSDK_WaveWorks.GFSDK_WaveWorks_Savestate_CreateD3D11(GFSDK_WaveWorks_StatePreserve_All);
        g_ForceKick = true;

        // Ocean object
        g_pOceanSurf = new OceanSurface(this);
        g_pOceanSurf.init();
        g_pOceanSurf.initQuadTree(g_ocean_quadtree_param);

        GFSDK_WaveWorks.GFSDK_WaveWorks_Simulation_UpdateProperties(g_hOceanSimulation, g_ocean_simulation_settings, g_ocean_simulation_param);
        GFSDK_WaveWorks.GFSDK_WaveWorks_Quadtree_SetFrustumCullMargin(g_pOceanSurf.m_hOceanQuadTree, GFSDK_WaveWorks.GFSDK_WaveWorks_Simulation_GetConservativeMaxDisplacementEstimate(g_hOceanSimulation));

        // Effect hooks borrowed from ocean object
//        g_pLogoTechnique = g_pOceanSurf->m_pOceanFX->GetTechniqueByName("DisplayLogoTech");
//        g_pLogoTextureVariable = g_pOceanSurf->m_pOceanFX->GetVariableByName("g_LogoTexture")->AsShaderResource();
        g_pLogoTechnique = new FullscreenProgram();

//        ID3D11Resource* pD3D11Resource = NULL;
//        V_RETURN(DXUTFindDXSDKMediaFileCch(path, MAX_PATH, TEXT("nvidia_logo.dds")));
//        V_RETURN(DirectX::CreateDDSTextureFromFile(pd3dDevice, static_cast<const wchar_t *>(path), NULL, &g_pLogoTex));
//        SAFE_RELEASE(pD3D11Resource);
        try {
            int logoTex = NvImage.uploadTextureFromDDSFile("nvidia/WaveWorks/textures/nvidia_logo.dds");
            g_pLogoTex = TextureUtils.createTexture2D(GLenum.GL_TEXTURE_2D, logoTex);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Terrain and sky fx
//        V_RETURN(DXUTFindDXSDKMediaFileCch(path, MAX_PATH, TEXT("sample_d3d11.fxo")));
//        V_RETURN(D3DX11CreateEffectFromFile(path, 0, pd3dDevice, &g_pEffect));

        // Initialize terrain
        g_Terrain = new CTerrainOcean(this);
        g_Terrain.onCreate("nvidia/WaveWorks/shaders/", "nvidia/WaveWorks/textures/");

        // Initialize shoreline interaction.
        g_pDistanceField = new DistanceField( g_Terrain, m_params );
        g_pDistanceField.Init( /*pd3dDevice*/ );
        g_pOceanSurf.AttachDistanceFieldModule( g_pDistanceField );
        g_Terrain.setupDataTexture();
        // Creating pipeline query
//        D3D11_QUERY_DESC queryDesc;
//        queryDesc.Query = D3D11_QUERY_PIPELINE_STATISTICS;
//        queryDesc.MiscFlags = 0;
//        pd3dDevice->CreateQuery(&queryDesc, &g_pPipelineQuery);
        g_pPipelineQuery = gl.glGenQuery();
        GLCheck.checkError();
    }

    @Override
    public void display() {
        float fElapsedTime = getFrameDeltaTime();
        g_SimulationTime += fElapsedTime;
        if(g_SimulateWater)
        {
            g_ShoreTime += fElapsedTime*g_ocean_simulation_param.time_scale;
        }

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
                    while(readbackCursorKickID != g_LastKickID)
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
                    g_LastArchivedKickID = g_LastReadbackKickID;
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
        GFSDK_WaveWorks.GFSDK_WaveWorks_Simulation_GetStats(g_hOceanSimulation,g_ocean_simulation_stats);

        // Performing treadbacks and raycasts
        UpdateReadbackPositions();
        UpdateRaycastPositions();

        // exponential filtering for stats
        g_ocean_simulation_stats_filtered.CPU_main_thread_wait_time			= g_ocean_simulation_stats_filtered.CPU_main_thread_wait_time*0.98f + 0.02f*g_ocean_simulation_stats.CPU_main_thread_wait_time;
        g_ocean_simulation_stats_filtered.CPU_threads_start_to_finish_time  = g_ocean_simulation_stats_filtered.CPU_threads_start_to_finish_time*0.98f + 0.02f*g_ocean_simulation_stats.CPU_threads_start_to_finish_time;
        g_ocean_simulation_stats_filtered.CPU_threads_total_time			= g_ocean_simulation_stats_filtered.CPU_threads_total_time*0.98f + 0.02f*g_ocean_simulation_stats.CPU_threads_total_time;
        g_ocean_simulation_stats_filtered.GPU_simulation_time				= g_ocean_simulation_stats_filtered.GPU_simulation_time*0.98f + 0.02f*g_ocean_simulation_stats.GPU_simulation_time;
        g_ocean_simulation_stats_filtered.GPU_FFT_simulation_time			= g_ocean_simulation_stats_filtered.GPU_FFT_simulation_time*0.98f + 0.02f*g_ocean_simulation_stats.GPU_FFT_simulation_time;
        g_ocean_simulation_stats_filtered.GPU_gfx_time						= g_ocean_simulation_stats_filtered.GPU_gfx_time*0.98f + 0.02f*g_ocean_simulation_stats.GPU_gfx_time;
        g_ocean_simulation_stats_filtered.GPU_update_time					= g_ocean_simulation_stats_filtered.GPU_update_time*0.98f + 0.02f*g_ocean_simulation_stats.GPU_update_time;

        // If the settings dialog is being shown, then
        // render it instead of rendering the app's scene
//        if( g_SettingsDlg.IsActive() )
//        {
//            g_SettingsDlg.OnRender( fElapsedTime );
//            return;
//        }

        g_FrameTime = fElapsedTime;

//        Vector2f ScreenSizeInv=new Vector2f(1.0f / (g_Terrain.BackbufferWidth*main_buffer_size_multiplier), 1.0f / (g_Terrain.BackbufferHeight*main_buffer_size_multiplier));

//        ID3DX11Effect* oceanFX = g_pOceanSurf->m_pOceanFX;

//        oceanFX->GetVariableByName("g_ZNear")->AsScalar()->SetFloat(scene_z_near);  TODO
//        oceanFX->GetVariableByName("g_ZFar")->AsScalar()->SetFloat(scene_z_far);
//        XMFLOAT4 light_pos = XMFLOAT4(140000.0f,65000.0f,40000.0f,0);
//        g_pEffect->GetVariableByName("g_LightPosition")->AsVector()->SetFloatVector((FLOAT*)&light_pos);
//        g_pEffect->GetVariableByName("g_ScreenSizeInv")->AsVector()->SetFloatVector((FLOAT*)&ScreenSizeInv);
//        oceanFX->GetVariableByName("g_ScreenSizeInv")->AsVector()->SetFloatVector((FLOAT*)&ScreenSizeInv);
//        g_pEffect->GetVariableByName("g_DynamicTessFactor")->AsScalar()->SetFloat(g_ocean_quadtree_param.tessellation_lod * 0.25f + 0.1f);

//        g_pOceanSurf->m_pOceanFX->GetVariableByName("g_enableShoreEffects")->AsScalar()->SetFloat(g_enableShoreEffects? 1.0f:0.0f);TODO
//        g_Terrain.pEffect->GetVariableByName("g_enableShoreEffects")->AsScalar()->SetFloat(g_enableShoreEffects? 1.0f:0.0f);TODO

        buildRenderParams();
        g_Terrain.onDraw(m_params);
        g_pDistanceField.GenerateDataTexture( /*pDC*/ );

        //RenderLogo(pDC);

        if(g_bShowUI) {

//            const WCHAR* windSpeedFormatString = g_ocean_simulation_settings.use_Beaufort_scale ? L"Beaufort: %.1f" : L"Wind speed: %.1f";
//            swprintf_s(number_string, 255, windSpeedFormatString, g_ocean_simulation_param.wind_speed);
//            static_being_updated = g_HUD.GetStatic(IDC_WIND_SPEED_SLIDER);
//            static_being_updated->SetText(number_string);
//
//            swprintf_s(number_string, 255, L"Wind dependency: %.2f", g_ocean_simulation_param.wind_dependency);
//            static_being_updated = g_HUD.GetStatic(IDC_WIND_DEPENDENCY_SLIDER);
//            static_being_updated->SetText(number_string);
//
//            swprintf_s(number_string, 255, L"Time scale: %.1f", g_ocean_simulation_param.time_scale);
//            static_being_updated = g_HUD.GetStatic(IDC_TIME_SCALE_SLIDER);
//            static_being_updated->SetText(number_string);
//
//            swprintf_s(number_string, 255, L"Tessellation LOD: %.0f", g_ocean_quadtree_param.tessellation_lod);
//            static_being_updated = g_HUD.GetStatic(IDC_TESSELLATION_LOD_SLIDER);
//            static_being_updated->SetText(number_string);
//
//            g_HUD.OnRender( fElapsedTime );
//            g_SampleUI.OnRender( fElapsedTime );
//            RenderText( fTime );
        }

        m_printOcen = true;
    }

    private void buildRenderParams(){
        m_transformer.getModelViewMat(m_params.g_ModelViewMatrix);
        Matrix4f.decompseRigidMatrix(m_params.g_ModelViewMatrix, m_params.g_CameraPosition, null, null, m_params.g_CameraDirection);
        m_params.g_CameraDirection.scale(-1);

        m_params.g_Projection = m_proj;


        Matrix4f.mul(m_proj, m_params.g_ModelViewMatrix, m_params.g_ModelViewProjectionMatrix);

        m_params.g_Time += getFrameDeltaTime();
        m_params.g_GerstnerSteepness = 1.f;
        m_params.g_BaseGerstnerAmplitude = 0.279f;
        m_params.g_BaseGerstnerWavelength = 3.912f;
        m_params.g_BaseGerstnerSpeed = 2.472f;
        m_params.g_BaseGerstnerParallelness = 0.2f;
        m_params.g_WindDirection.set(0.8f, 0.6f);

    }

    private void initApp(){
        g_ocean_quadtree_param.min_patch_length		= 40.f;
        g_ocean_quadtree_param.upper_grid_coverage	= 64.0f;
        g_ocean_quadtree_param.mesh_dim				= 128;
        g_ocean_quadtree_param.sea_level			= -2.f;
        g_ocean_quadtree_param.auto_root_lod		= 10;
        g_ocean_quadtree_param.use_tessellation		= true;
        g_ocean_quadtree_param.tessellation_lod		= 50.0f;
        g_ocean_quadtree_param.geomorphing_degree	= 1.f;
        g_ocean_quadtree_param.enable_CPU_timers	= true;

        g_ocean_simulation_param.time_scale				= 0.75f;
        g_ocean_simulation_param.wave_amplitude			= 0.8f;
        g_ocean_simulation_param.wind_dir				.set(g_WindDir);
        g_ocean_simulation_param.wind_speed				= 2.52f;
        g_ocean_simulation_param.wind_dependency		= 0.85f;
        g_ocean_simulation_param.choppy_scale			= 1.2f;
        g_ocean_simulation_param.small_wave_fraction	= 0.f;
        g_ocean_simulation_param.foam_dissipation_speed	= 0.6f;
        g_ocean_simulation_param.foam_falloff_speed		= 0.985f;
        g_ocean_simulation_param.foam_generation_amount	= 0.12f;
        g_ocean_simulation_param.foam_generation_threshold = 0.37f;

        g_ocean_simulation_settings.fft_period						= 400.0f;
        g_ocean_simulation_settings.detail_level					= GFSDK_WaveWorks_Simulation_DetailLevel.Normal;
        g_ocean_simulation_settings.readback_displacements			= true;
        g_ocean_simulation_settings.num_readback_FIFO_entries		= ReadbackArchiveSize;
        g_ocean_simulation_settings.aniso_level						= 16;
        g_ocean_simulation_settings.CPU_simulation_threading_model = GFSDK_WaveWorks_Simulation_CPU_Threading_Model.GFSDK_WaveWorks_Simulation_CPU_Threading_Model_Automatic;
        g_ocean_simulation_settings.use_Beaufort_scale				= true;
        g_ocean_simulation_settings.num_GPUs						= 1;
        g_ocean_simulation_settings.enable_CUDA_timers				= true;
        g_ocean_simulation_settings.enable_gfx_timers				= true;
        g_ocean_simulation_settings.enable_CPU_timers				= true;

        for(int i = 0; i < displacements.length;i++)
            displacements[i]=new Vector4f();
    }

    @Override
    protected void reshape(int width, int height) {
        if(width <= 0 || height <=0)
            return;

        float aspectRatio = (float)width/height;
        Matrix4f.perspective(camera_fov/3, aspectRatio, scene_z_near, scene_z_far, m_proj);
        m_params.g_Projection = m_proj;
        m_params.g_ZNear = scene_z_near;
        m_params.g_ZFar = scene_z_far;

//        if(g_Terrain.BackbufferWidth == width && g_Terrain.BackbufferHeight == height)
//            return;
//
//        g_Terrain.BackbufferWidth=width;
//        g_Terrain.BackbufferHeight=height;
//        g_Terrain.ReCreateBuffers();
        g_Terrain.onReshape(width, height);
        GLCheck.checkError();
    }

    private final Vector4f[] displacements = new Vector4f[NumMarkers];

    void UpdateReadbackPositions()
    {
        for(int x = 0; x != NumMarkersXY; ++x)
        {
            for(int y = 0; y != NumMarkersXY; ++y)
            {
                g_readback_marker_coords[y * NumMarkersXY + x].x = 5.0f*x;
                g_readback_marker_coords[y * NumMarkersXY + x].y = 5.0f*y;
            }
        }

        if(g_ocean_simulation_settings.readback_displacements)
        {
//            gfsdk_float4 displacements[NumMarkers];
            if(g_ReadbackCoord >= 1.f)
            {
                final float coord = g_ReadbackCoord - (g_LastReadbackKickID[0]-g_LastArchivedKickID[0]) * 1.f/(ReadbackArchiveInterval + 1);
                GFSDK_WaveWorks.GFSDK_WaveWorks_Simulation_GetArchivedDisplacements(g_hOceanSimulation, coord, g_readback_marker_coords, displacements, NumMarkers);
            }
            else
            {
                GFSDK_WaveWorks.GFSDK_WaveWorks_Simulation_GetDisplacements(g_hOceanSimulation, g_readback_marker_coords, displacements, NumMarkers);
            }

            for(int ix = 0; ix != NumMarkers; ++ix)
            {
                g_readback_marker_positions[ix].x = displacements[ix].x + g_readback_marker_coords[ix].x;
                g_readback_marker_positions[ix].y = displacements[ix].y + g_readback_marker_coords[ix].y;
                g_readback_marker_positions[ix].z = displacements[ix].z + g_ocean_quadtree_param.sea_level;
                g_readback_marker_positions[ix].w = 1.f;
            }
        }
    }

    // Returns true and sets Result to intersection point if intersection is found, or returns false and does not update Result
    // NB: The function does trace the water surface from above or from inside the water volume, but can be easily changed to trace from below water volume
    // NB: y axiz is up
    boolean intersectRayWithOcean(Vector3f Result, ReadableVector3f Position, ReadableVector3f Direction, GFSDK_WaveWorks_Simulation hSim, float sea_level)
    {
        final Vector2f test_point = new Vector2f();					// x,z coordinates of current test point
        final Vector2f old_test_point = new Vector2f();				// x,z coordinates of current test point
        final Vector4f displacements = new Vector4f();				// displacements returned by GFSDK_WaveWorks library
        float t;													// distance traveled along the ray while tracing
        int num_steps = 0;											// number of steps we've done while tracing
        float max_displacement = GFSDK_WaveWorks.GFSDK_WaveWorks_Simulation_GetConservativeMaxDisplacementEstimate(hSim);
        // the maximal possible displacements of ocean surface along y axis,
        // defining volume we have to trace
        final int max_num_successive_steps = 16;					// we limit ourselves on #of successive steps
        final int max_num_binary_steps = 16;						// we limit ourselves on #of binary search steps
        final float t_threshold = 0.05f;							// we stop successive tracing when we don't progress more than 5 cm each step
        final float refinement_threshold_sqr = 0.1f*0.1f;			// we stop refinement step when we don't progress more than 10cm while doing refinement of current water altitude
        final float t_multiplier = 1.8f/(Math.abs(Direction.getY()) + 1.0f);	// we increase step length at steep angles to speed up the tracing,
        // but less than 2 to make sure the process converges
        // and to add some safety to minimize chance of overshooting
        final Vector3f PositionBSStart =new Vector3f();								// Vectors used at binary search step
        final Vector3f PositionBSEnd =new Vector3f();

        // normalizing direction
//        Direction = XMVector3Normalize(Direction);
        ((Vector3f)Direction).normalise();  // TODO

        // checking if ray is outside of ocean surface volume
        if((Position.getY() >= max_displacement + sea_level) && (Direction.getY() >=0)) return false;

        // getting to the top edge of volume where we can start
        if(Position.getY() > max_displacement  + sea_level)
        {
            t = -(Position.getY() - max_displacement - sea_level) / Direction.getY();
//            Position += t*Direction;
            Vector3f.linear(Position, Direction, t, (Vector3f)Position);  // TODO
        }

        // tracing the ocean surface:
        // moving along the ray by distance defined by vertical distance form current test point, increased/decreased by safety multiplier
        // this process will converge despite our assumption on local flatness of the surface because curvature of the surface is smooth
        // NB: this process guarantees we don't shoot through wave tips
        while(true)
        {
            displacements.x = 0;
            displacements.y = 0;
            old_test_point.x = 0;
            old_test_point.y = 0;
            for(int k = 0; k < 4; k++) // few refinement steps to converge at correct intersection point
            {
                // moving back sample points by the displacements read initially,
                // to get a guess on which undisturbed water surface point moved to the actual sample point
                // due to x,y motion of water surface, assuming the x,y disturbances are locally constant
                test_point.x = Position.getX() - displacements.x;
                test_point.y = Position.getZ() - displacements.y;
                GFSDK_WaveWorks.GFSDK_WaveWorks_Simulation_GetDisplacements( hSim, CommonUtil.toArray(test_point), CommonUtil.toArray(displacements), 1 );
                if(refinement_threshold_sqr > (old_test_point.x - test_point.x)*(old_test_point.x - test_point.x) + (old_test_point.y - test_point.y)*(old_test_point.y - test_point.y)) break;
                old_test_point.x = test_point.x;
                old_test_point.y = test_point.y;
            }
            // getting t to travel along the ray
            t = t_multiplier * (Position.getY() - displacements.z - sea_level);

            // traveling along the ray
//            Position += t*Direction;
            Vector3f.linear(Position, Direction, t, (Vector3f)Position);  // TODO

            if(num_steps >= max_num_successive_steps)  break;
            if(t < t_threshold) break;
            ++num_steps;
        }

        // exited the loop, checking if intersection is found
        if(t < t_threshold)
        {
            Result.set(Position);
            return true;
        }

        // if we're looking down and we did not hit water surface, doing binary search to make sure we hit water surface,
        // but there is risk of shooting through wave tips if we are tracing at extremely steep angles
        if(Direction.getY() < 0)
        {
            PositionBSStart.set(Position);

            // getting to the bottom edge of volume where we can start
            t = -(Position.getY() + max_displacement - sea_level) / Direction.getY();
//            PositionBSEnd = Position + t*Direction;
            Vector3f.linear(Position, Direction, t, PositionBSEnd);

            for(int i = 0; i < max_num_binary_steps; i++)
            {
//                Position = (PositionBSStart + PositionBSEnd)*0.5f;
                Vector3f.mix(PositionBSStart, PositionBSEnd, 0.5f, (Vector3f) Position);
                old_test_point.x = 0;
                old_test_point.y = 0;
                for(int k = 0; k < 4; k++)
                {
                    test_point.x = Position.getX() - displacements.x;
                    test_point.y = Position.getZ() - displacements.y;
                    GFSDK_WaveWorks.GFSDK_WaveWorks_Simulation_GetDisplacements( hSim, CommonUtil.toArray(test_point), CommonUtil.toArray(displacements), 1 );
                    if(refinement_threshold_sqr > (old_test_point.x - test_point.x)*(old_test_point.x - test_point.x) + (old_test_point.y - test_point.y)*(old_test_point.y - test_point.y)) break;
                    old_test_point.x = test_point.x;
                    old_test_point.y = test_point.y;
                }
                if(Position.getY() - displacements.z - sea_level > 0)
                {
                    PositionBSStart.set(Position);
                }
                else
                {
                    PositionBSEnd.set(Position);
                }
            }

            Result.set(Position);

            return true;
        }
        return false;
    }

    void UpdateRaycastPositions()
    {
        for(int x = 0; x != NumMarkersXY; ++x)
        {
            for(int y = 0; y != NumMarkersXY; ++y)
            {
                int i = x + y*NumMarkersXY;
                g_raycast_origins[i].set(0, 10, terrain_gridpoints*terrain_geometry_scale);
                g_raycast_directions[i].set(5.0f*(x - NumMarkersXY / 2.0f), -10.0f, 5.0f*(y - NumMarkersXY / 2.0f));
                g_raycast_directions[i].normalise();
            }
        }
        g_IntersectRaysTime = 0.f;
        // Performing water hit test for rays
//        QueryPerformanceFrequency(&g_IntersectRaysPerfFrequency);
//        QueryPerformanceCounter(&g_IntersectRaysPerfCounterOld);

        long time_stamp = System.nanoTime();

        Vector3f position = new Vector3f();
        Vector3f direction = new Vector3f();
        for(int i = 0; i < NumMarkers; i++)
        {
            position.set(g_raycast_origins[i]);
            direction.set(g_raycast_directions[i]);
            g_raycast_hittestresults[i] = intersectRayWithOcean(g_raycast_hitpoints[i], position, direction, g_hOceanSimulation, g_ocean_quadtree_param.sea_level);
        }
        long time_stamp2 = System.nanoTime();
        g_IntersectRaysTime = (time_stamp2 - time_stamp) * 1e-9f;

//        QueryPerformanceCounter(&g_IntersectRaysPerfCounter);
//        g_IntersectRaysTime = (float)(((double)(g_IntersectRaysPerfCounter.QuadPart) - (double)(g_IntersectRaysPerfCounterOld.QuadPart))/(double)g_IntersectRaysPerfFrequency.QuadPart);
    }
}
