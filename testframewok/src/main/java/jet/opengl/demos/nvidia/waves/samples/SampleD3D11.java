package jet.opengl.demos.nvidia.waves.samples;

import com.nvidia.developer.opengl.app.NvCameraMotionType;
import com.nvidia.developer.opengl.app.NvSampleApp;
import com.nvidia.developer.opengl.utils.NvImage;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import java.io.IOException;

import jet.opengl.demos.nvidia.waves.GFSDK_WaveWorks;
import jet.opengl.demos.nvidia.waves.GFSDK_WaveWorks_Quadtree_Params;
import jet.opengl.demos.nvidia.waves.GFSDK_WaveWorks_Quadtree_Stats;
import jet.opengl.demos.nvidia.waves.GFSDK_WaveWorks_Savestate;
import jet.opengl.demos.nvidia.waves.GFSDK_WaveWorks_Simulation;
import jet.opengl.demos.nvidia.waves.GFSDK_WaveWorks_Simulation_CPU_Threading_Model;
import jet.opengl.demos.nvidia.waves.GFSDK_WaveWorks_Simulation_DetailLevel;
import jet.opengl.demos.nvidia.waves.GFSDK_WaveWorks_Simulation_Params;
import jet.opengl.demos.nvidia.waves.GFSDK_WaveWorks_Simulation_Settings;
import jet.opengl.demos.nvidia.waves.GFSDK_WaveWorks_Simulation_Stats;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.FullscreenProgram;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.TextureUtils;

/**
 * Created by mazhen'gui on 2017/7/27.
 */

public class SampleD3D11 extends NvSampleApp implements Constants{

    private static final int ReadbackArchiveSize = 50;
    private static final int ReadbackArchiveInterval = 5;

    private static final int NumMarkersXY = 10;
    private static final int NumMarkers = NumMarkersXY*NumMarkersXY;
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

    CTerrain2 g_Terrain;
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

    final Matrix4f m_proj = new Matrix4f();
    private GLFuncProvider gl;

    @Override
    protected void initRendering() {
        initApp();

        gl = GLFuncProviderFactory.getGLFuncProvider();
        m_transformer.setTranslation(100.f, -8.0f, -200.f);
        m_transformer.setMotionMode(NvCameraMotionType.FIRST_PERSON);

        // Ocean sim
        GFSDK_WaveWorks.GFSDK_WaveWorks_InitD3D11(GFSDK_WaveWorks.GFSDK_WAVEWORKS_API_GUID);

        g_hOceanSimulation = GFSDK_WaveWorks.GFSDK_WaveWorks_Simulation_CreateD3D11(g_ocean_simulation_settings, g_ocean_simulation_param);
//        g_hOceanSavestate = GFSDK_WaveWorks.GFSDK_WaveWorks_Savestate_CreateD3D11(GFSDK_WaveWorks_StatePreserve_All);
        g_ForceKick = true;

        // Ocean object
        g_pOceanSurf = new OceanSurface();
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

        // Initialize shoreline interaction.
        g_pDistanceField = new DistanceField( g_Terrain );
        g_pDistanceField.Init( /*pd3dDevice*/ );
        g_pOceanSurf.AttachDistanceFieldModule( g_pDistanceField );

        // Initialize terrain
        g_Terrain.Initialize(this/*pd3dDevice,g_pEffect*/);
        g_Terrain.LoadTextures();

        // Creating pipeline query
//        D3D11_QUERY_DESC queryDesc;
//        queryDesc.Query = D3D11_QUERY_PIPELINE_STATISTICS;
//        queryDesc.MiscFlags = 0;
//        pd3dDevice->CreateQuery(&queryDesc, &g_pPipelineQuery);
        g_pPipelineQuery = gl.glGenQuery();
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
    }

    @Override
    protected void reshape(int width, int height) {
        if(width <= 0 || height <=0)
            return;

        float aspectRatio = (float)width/height;
        Matrix4f.perspective(camera_fov, aspectRatio, scene_z_near, scene_z_far, m_proj);

        if(g_Terrain.BackbufferWidth == width && g_Terrain.BackbufferHeight == height)
            return;

        g_Terrain.BackbufferWidth=width;
        g_Terrain.BackbufferHeight=height;
        g_Terrain.ReCreateBuffers();
    }
}
