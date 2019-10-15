package jet.opengl.demos.nvidia.waves.samples;

import com.nvidia.developer.opengl.app.NvSampleApp;

import org.lwjgl.util.vector.Matrix4f;

import jet.opengl.demos.nvidia.waves.wavework.GFSDK_WaveWorks;
import jet.opengl.demos.nvidia.waves.wavework.GFSDK_WaveWorks_Quadtree_Params;
import jet.opengl.demos.nvidia.waves.wavework.GFSDK_WaveWorks_Quadtree_Stats;
import jet.opengl.demos.nvidia.waves.wavework.GFSDK_WaveWorks_Result;
import jet.opengl.demos.nvidia.waves.wavework.GFSDK_WaveWorks_Simulation;
import jet.opengl.demos.nvidia.waves.wavework.GFSDK_WaveWorks_Simulation_CPU_Threading_Model;
import jet.opengl.demos.nvidia.waves.wavework.GFSDK_WaveWorks_Simulation_DetailLevel;
import jet.opengl.demos.nvidia.waves.wavework.GFSDK_WaveWorks_Simulation_Params;
import jet.opengl.demos.nvidia.waves.wavework.GFSDK_WaveWorks_Simulation_Settings;
import jet.opengl.demos.nvidia.waves.wavework.GFSDK_WaveWorks_Simulation_Stats;
import jet.opengl.postprocessing.util.LogUtil;

/**
 * Created by mazhen'gui on 2017/9/28.
 */

public class SampleGL2 extends NvSampleApp {
    OceanSurfaceGL						g_pOceanSurf = null;
    GFSDK_WaveWorks_Simulation g_hOceanSimulation = null;
    final GFSDK_WaveWorks_Simulation_Params g_ocean_simulation_param = new GFSDK_WaveWorks_Simulation_Params();
    final GFSDK_WaveWorks_Simulation_Settings g_ocean_simulation_settings = new GFSDK_WaveWorks_Simulation_Settings();
    final GFSDK_WaveWorks_Quadtree_Params g_ocean_param_quadtree = new GFSDK_WaveWorks_Quadtree_Params();
    GFSDK_WaveWorks_Quadtree_Stats g_ocean_stats_quadtree;
    GFSDK_WaveWorks_Simulation_Stats g_ocean_stats_simulation;
    GFSDK_WaveWorks_Simulation_Stats    g_ocean_stats_simulation_filtered;
//    GFSDK_WAVEWORKS_GLFunctions         g_GLFunctions;
    int									g_max_detail_level;
    final Matrix4f m_viewNat = new Matrix4f();
    final Matrix4f m_projNat = new Matrix4f();
    /*
    long								g_FPS;
    long								g_Second;
    long								g_FrameNumber;

    HINSTANCE							g_hInstance;				// GL window class instance
    HWND								g_hWnd;						// GL window class handle
    HDC									g_hDC;						// GL window device context handle
    HGLRC								g_hRC;						// GL rendering context
    LRESULT	CALLBACK					WndProc(HWND, UINT, WPARAM, LPARAM);	// forward declaration For WndProc
    bool								g_ExitApp = false;
    bool								g_WindowActive = true;
    MSG									g_Msg;						// Windows message structure
    bool								g_PressedKeys[256];			// Array of pressed keys

    TwBar*								g_pStatsBar;
    TwBar*								g_pControlsBar;*/

    // Controls
    boolean								g_Wireframe = false;
    boolean								g_SimulateWater = true;
    float					total_time;
    float					delta_time;
//    bool								g_ShowStats = false;


    @Override
    protected void initRendering() {
        initApp();

        g_pOceanSurf = new OceanSurfaceGL(g_ocean_simulation_settings.use_texture_arrays);
        // initializing WaveWorks
        GFSDK_WaveWorks_Result res = GFSDK_WaveWorks.GFSDK_WaveWorks_InitGL2(GFSDK_WaveWorks.GFSDK_WAVEWORKS_API_GUID);
        if(res == GFSDK_WaveWorks_Result.OK)
        {
            LogUtil.i(LogUtil.LogType.DEFAULT, "GFSDK_WaveWorks_InitGL2: OK");
        }
        else
        {
            LogUtil.e(LogUtil.LogType.DEFAULT, String.format("GFSDK_WaveWorks_InitGL2 ERROR: %i, exiting..\n", res));
            return;
        }

        res = g_pOceanSurf.InitQuadTree(g_ocean_param_quadtree);
        if(res == GFSDK_WaveWorks_Result.OK)
        {
            LogUtil.i(LogUtil.LogType.DEFAULT, "InitQuadTree: OK");
        }
        else
        {
            LogUtil.e(LogUtil.LogType.DEFAULT, String.format("InitQuadTree ERROR: %i, exiting..\n", res));
            return;
        }

        // checking available detail level
        int detail_level = 0;
        for(; detail_level != /*Num_GFSDK_WaveWorks_Simulation_DetailLevels*/3; ++detail_level) {
            if(!GFSDK_WaveWorks.GFSDK_WaveWorks_Simulation_DetailLevelIsSupported_GL2(GFSDK_WaveWorks_Simulation_DetailLevel.values()[detail_level]));
                break;
        }
        if (0 == detail_level)
        {
            LogUtil.e(LogUtil.LogType.DEFAULT, "Fatal Error: No supported detail levels.");
            return;
        }

        g_max_detail_level = detail_level - 1;

        g_hOceanSimulation = GFSDK_WaveWorks.GFSDK_WaveWorks_Simulation_CreateGL2(g_ocean_simulation_settings, g_ocean_simulation_param/*, (void*) g_hRC, &g_hOceanSimulation*/);
        if(g_hOceanSimulation != null)
        {
            LogUtil.i(LogUtil.LogType.DEFAULT, "GFSDK_WaveWorks_Simulation_CreateGL2: OK");
        }
        else
        {
            LogUtil.e(LogUtil.LogType.DEFAULT, String.format("GFSDK_WaveWorks_Simulation_CreateGL2 ERROR: %i, exiting..\n", res));
            return;
        }
        GFSDK_WaveWorks.GFSDK_WaveWorks_Simulation_UpdateProperties(g_hOceanSimulation, g_ocean_simulation_settings, g_ocean_simulation_param);
        GFSDK_WaveWorks.GFSDK_WaveWorks_Quadtree_SetFrustumCullMargin(g_pOceanSurf.hOceanQuadTree, GFSDK_WaveWorks.GFSDK_WaveWorks_Simulation_GetConservativeMaxDisplacementEstimate(g_hOceanSimulation));
    }

    @Override
    public void display() {
        if(g_SimulateWater)
            do {
                GFSDK_WaveWorks.GFSDK_WaveWorks_Simulation_SetTime(g_hOceanSimulation, g_pOceanSurf.total_time);
                GFSDK_WaveWorks.GFSDK_WaveWorks_Simulation_KickGL2(g_hOceanSimulation, null);
            } while(GFSDK_WaveWorks_Result.NONE ==GFSDK_WaveWorks.GFSDK_WaveWorks_Simulation_GetStagingCursor(g_hOceanSimulation,null));

        // getting simulation timings
        GFSDK_WaveWorks.GFSDK_WaveWorks_Simulation_GetStats(g_hOceanSimulation,g_ocean_stats_simulation);

        m_transformer.getModelViewMat(m_viewNat);

        // rendering
        g_pOceanSurf.Render(m_viewNat, m_projNat, g_hOceanSimulation, g_ocean_simulation_settings, g_Wireframe);

        // getting quadtree stats
        GFSDK_WaveWorks.GFSDK_WaveWorks_Quadtree_GetStats(g_pOceanSurf.hOceanQuadTree, g_ocean_stats_quadtree);

        // Drawing AntTweakBars
        /*TwDraw();
        SwapBuffers(g_hDC);
        g_FrameNumber++;*/

        // timing
        /*QueryPerformanceFrequency(&freq);
        QueryPerformanceCounter(&counter);

        g_pOceanSurf->delta_time = (float)(((double)(counter.QuadPart) - (double)(old_counter.QuadPart))/(double)freq.QuadPart);
        g_pOceanSurf->total_time += g_pOceanSurf->delta_time;
        if(g_pOceanSurf->total_time>=36000.0f) g_pOceanSurf->total_time=0;
        old_counter = counter;*/
        delta_time = getFrameDeltaTime();
        total_time += delta_time;

        /*if((long)(floor(g_pOceanSurf->total_time)) > g_Second)
        {
            g_FPS = g_FrameNumber/((long)(floor(g_pOceanSurf->total_time)) - g_Second);
            g_Second = (long)(ceil(g_pOceanSurf->total_time));
            g_FrameNumber = 0;
        }*/

        // exponential filtering for stats
        g_ocean_stats_simulation_filtered.CPU_main_thread_wait_time			= g_ocean_stats_simulation_filtered.CPU_main_thread_wait_time*0.98f + 0.02f*g_ocean_stats_simulation.CPU_main_thread_wait_time;
        g_ocean_stats_simulation_filtered.CPU_threads_start_to_finish_time  = g_ocean_stats_simulation_filtered.CPU_threads_start_to_finish_time*0.98f + 0.02f*g_ocean_stats_simulation.CPU_threads_start_to_finish_time;
        g_ocean_stats_simulation_filtered.CPU_threads_total_time			= g_ocean_stats_simulation_filtered.CPU_threads_total_time*0.98f + 0.02f*g_ocean_stats_simulation.CPU_threads_total_time;
        g_ocean_stats_simulation_filtered.GPU_simulation_time				= g_ocean_stats_simulation_filtered.GPU_simulation_time*0.98f + 0.02f*g_ocean_stats_simulation.GPU_simulation_time;
        g_ocean_stats_simulation_filtered.GPU_FFT_simulation_time			= g_ocean_stats_simulation_filtered.GPU_FFT_simulation_time*0.98f + 0.02f*g_ocean_stats_simulation.GPU_FFT_simulation_time;
        g_ocean_stats_simulation_filtered.GPU_gfx_time						= g_ocean_stats_simulation_filtered.GPU_gfx_time*0.98f + 0.02f*g_ocean_stats_simulation.GPU_gfx_time;
        g_ocean_stats_simulation_filtered.GPU_update_time					= g_ocean_stats_simulation_filtered.GPU_update_time*0.98f + 0.02f*g_ocean_stats_simulation.GPU_update_time;

    }

    @Override
    protected void reshape(int width, int height) {
        if(width <= 0 || height <= 0)
            return;

        Matrix4f.perspective(60, (float)width/height, 0.1f, 10000.0f, m_projNat);
    }

    private void initApp(){
        // Creating OceanSurface instance
        g_ocean_simulation_settings.fft_period						= 1000.0f;
        g_ocean_simulation_settings.detail_level					= GFSDK_WaveWorks_Simulation_DetailLevel.High;
        g_ocean_simulation_settings.readback_displacements			= false;
        g_ocean_simulation_settings.num_readback_FIFO_entries		= 0;
        g_ocean_simulation_settings.aniso_level						= 4;
        g_ocean_simulation_settings.CPU_simulation_threading_model  = GFSDK_WaveWorks_Simulation_CPU_Threading_Model.GFSDK_WaveWorks_Simulation_CPU_Threading_Model_Automatic;
        g_ocean_simulation_settings.use_Beaufort_scale				= true;
        g_ocean_simulation_settings.num_GPUs						= 1;
        g_ocean_simulation_settings.use_texture_arrays				= true;
        g_ocean_simulation_settings.enable_CUDA_timers				= true;

        // initializing QuadTree
        g_ocean_param_quadtree.min_patch_length		= 40.0f;
        g_ocean_param_quadtree.upper_grid_coverage	= 64.0f;
        g_ocean_param_quadtree.mesh_dim				= 128;
        g_ocean_param_quadtree.sea_level			= 0.f;
        g_ocean_param_quadtree.auto_root_lod		= 10;
        g_ocean_param_quadtree.tessellation_lod		= 100.0f;
        g_ocean_param_quadtree.geomorphing_degree	= 1.f;
        g_ocean_param_quadtree.enable_CPU_timers	= true;

        // initializing simulation
        g_ocean_simulation_param.time_scale				= 0.5f;
        g_ocean_simulation_param.wave_amplitude			= 1.0f;
        g_ocean_simulation_param.wind_dir.x				= 0.8f;
        g_ocean_simulation_param.wind_dir.y				= 0.6f;
        g_ocean_simulation_param.wind_speed				= 9.0f;
        g_ocean_simulation_param.wind_dependency		= 0.98f;
        g_ocean_simulation_param.choppy_scale			= 1.f;
        g_ocean_simulation_param.small_wave_fraction	= 0.f;
        g_ocean_simulation_param.foam_dissipation_speed	= 0.6f;
        g_ocean_simulation_param.foam_falloff_speed		= 0.985f;
        g_ocean_simulation_param.foam_generation_amount	= 0.12f;
        g_ocean_simulation_param.foam_generation_threshold = 0.37f;
    }
}
