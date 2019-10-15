package jet.opengl.demos.nvidia.waves.wavework;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;

import jet.opengl.postprocessing.buffer.AttribDesc;
import jet.opengl.postprocessing.common.Disposeable;
import jet.opengl.postprocessing.common.GLCheck;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.shader.Macro;
import jet.opengl.postprocessing.texture.AttachType;
import jet.opengl.postprocessing.texture.RenderTargets;
import jet.opengl.postprocessing.texture.SamplerDesc;
import jet.opengl.postprocessing.texture.SamplerUtils;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.Texture2DDesc;
import jet.opengl.postprocessing.texture.TextureAttachDesc;
import jet.opengl.postprocessing.texture.TextureUtils;
import jet.opengl.postprocessing.util.CacheBuffer;
import jet.opengl.postprocessing.util.CommonUtil;
import jet.opengl.postprocessing.util.LogUtil;

import static jet.opengl.demos.nvidia.waves.wavework.HRESULT.E_FAIL;
import static jet.opengl.demos.nvidia.waves.wavework.HRESULT.S_FALSE;
import static jet.opengl.demos.nvidia.waves.wavework.HRESULT.S_OK;
import static jet.opengl.demos.nvidia.waves.wavework.NVWaveWorks_Mesh.PrimitiveType.PT_TriangleStrip;
import static jet.opengl.demos.nvidia.waves.wavework.nv_water_d3d_api.nv_water_d3d_api_gl2;
import static jet.opengl.demos.nvidia.waves.wavework.nv_water_d3d_api.nv_water_d3d_api_gnm;
import static jet.opengl.demos.nvidia.waves.wavework.nv_water_d3d_api.nv_water_d3d_api_none;
import static jet.opengl.demos.nvidia.waves.wavework.nv_water_d3d_api.nv_water_d3d_api_undefined;
import static jet.opengl.demos.nvidia.waves.wavework.nv_water_simulation_api.nv_water_simulation_api_cpu;
import static jet.opengl.demos.nvidia.waves.wavework.nv_water_simulation_api.nv_water_simulation_api_direct_compute;

/**
 * Created by Administrator on 2017/7/23 0023.
 */

public class GFSDK_WaveWorks_Simulation implements Disposeable{

    private GFSDK_WaveWorks_Detailed_Simulation_Params m_params = new GFSDK_WaveWorks_Detailed_Simulation_Params();
    // 2 in-flight, one usable, one active
    private static final int NumTimerSlots = 4;
    private static final int nvrm_unused = -1;
    private CascadeState[] cascade_states=new CascadeState[GFSDK_WaveWorks_Detailed_Simulation_Params.MaxNumCascades];

    // To preserve SLI scaling, we operate some resources that have inter-frame dependencies on a round-robin basis...
    private int m_num_GPU_slots;		// the number of GPU slots allocated for per-GPU resources (e.g. gradient maps)
    private int m_active_GPU_slot;		// the index of the active GPU within m_num_GPU_slots

    private float m_total_rms;

    private double m_dSimTime;
    private final double[] m_dSimTimeFIFO=new double[GFSDK_WaveWorks.MaxNumGPUs+1];
    private int m_numValidEntriesInSimTimeFIFO;
    private double m_dFoamSimDeltaTime;

    // Some kinds of simulation require a manager to hook simulation-level events
    private NVWaveWorks_FFT_Simulation_Manager m_pSimulationManager;

    // Scheduler to use for CPU work (optional)
    private GFSDK_WaveWorks_CPU_Scheduler_Interface m_pOptionalScheduler;

    // GFX timing services
    private NVWaveWorks_GFX_Timer_Impl m_pGFXTimer;

    // D3D API handling
    private nv_water_d3d_api m_d3dAPI;
    private final UnionGlobal m_d3d = new UnionGlobal();

    private final TimerPool m_gpu_kick_timers = new TimerPool();
    private final TimerPool m_gpu_wait_timers = new TimerPool();

    private boolean m_has_consumed_wait_timer_slot_since_last_kick;
    private GLFuncProvider gl;
    private RenderTargets m_RenderTarget;
    private TextureAttachDesc[] m_AttachDescs = new TextureAttachDesc[1];
    
    private final ps_calcgradient_cbuffer m_calcgradient_cbuffer = new ps_calcgradient_cbuffer();
    private final ps_foamgeneration_cbuffer m_foamgeneration_cbuffer = new ps_foamgeneration_cbuffer();
    private boolean m_printOnce;

    public GFSDK_WaveWorks_Simulation(){
        for(int i = 0; i != GFSDK_WaveWorks_Detailed_Simulation_Params.MaxNumCascades; ++i)
        {
            cascade_states[i] = new CascadeState();
            cascade_states[i].m_pQuadMesh	= null;
            cascade_states[i].m_pFFTSimulation	= null;
            cascade_states[i].m_gradient_map_version = GFSDK_WaveWorks.GFSDK_WaveWorks_InvalidKickID;
//            memset(&cascade_states[i].m_d3d, 0, sizeof(cascade_states[i].m_d3d));
        }

        m_dSimTime = 0.f;
        m_numValidEntriesInSimTimeFIFO = 0;
        m_pSimulationManager = null;
        m_pOptionalScheduler = null;
        m_pGFXTimer = null;

//        memset(&m_params, 0, sizeof(m_params));
//        memset(&m_d3d, 0, sizeof(m_d3d));

        m_d3dAPI = nv_water_d3d_api_undefined;

        m_num_GPU_slots = 1;
        m_active_GPU_slot = 0;

        m_gpu_kick_timers.reset();
        m_gpu_wait_timers.reset();

        m_has_consumed_wait_timer_slot_since_last_kick = false;
    }

    public HRESULT initD3D11(GFSDK_WaveWorks_Detailed_Simulation_Params params, GFSDK_WaveWorks_CPU_Scheduler_Interface pOptionalScheduler/*, ID3D11Device* pD3DDevice*/){
        HRESULT hr;
        gl = GLFuncProviderFactory.getGLFuncProvider();
        if(nv_water_d3d_api.nv_water_d3d_api_d3d11 != m_d3dAPI)
        {
            releaseAll();
        }
//        else if(m_d3d._11.m_pd3d11Device != pD3DDevice)
//        {
//            releaseAll();
//        }

        if(nv_water_d3d_api.nv_water_d3d_api_undefined == m_d3dAPI)
        {
            m_d3dAPI = nv_water_d3d_api.nv_water_d3d_api_d3d11;
//            m_d3d._11.m_pd3d11Device = pD3DDevice;
//            m_d3d._11.m_pd3d11Device->AddRef();

            m_pOptionalScheduler = pOptionalScheduler;
            m_params = params;
            if(m_d3d._11 == null)
                m_d3d._11 = new D3D11GlobalObjects();

            hr = allocateAll();
            if(hr != HRESULT.S_OK)
                return hr;
        }
        else
        {
            hr = reinit(params);
            if(hr != HRESULT.S_OK)
                return hr;
        }
        return HRESULT.S_OK;
    }

    public HRESULT initGnm(GFSDK_WaveWorks_Detailed_Simulation_Params params, GFSDK_WaveWorks_CPU_Scheduler_Interface pOptionalScheduler){
        return HRESULT.E_FAIL;
    }

    public HRESULT initGL2(GFSDK_WaveWorks_Detailed_Simulation_Params params/*, Object pGLContext*/){
        HRESULT hr;
        gl = GLFuncProviderFactory.getGLFuncProvider();
        if(nv_water_d3d_api_gl2 != m_d3dAPI)
        {
            releaseAll();
        }
//        else if(m_d3d._GL2.m_pGLContext != pGLContext)
//        {
//            releaseAll();
//        }

        if(nv_water_d3d_api_undefined == m_d3dAPI)
        {
            m_d3dAPI = nv_water_d3d_api_gl2;
//            m_d3d._GL2.m_pGLContext = pGLContext;
            m_params = params;

            hr = allocateAll();
            if(hr != HRESULT.S_OK)
                return hr;
        }
        else
        {
            hr = reinit(params);
            if(hr != HRESULT.S_OK)
                return hr;
        }
        return HRESULT.S_OK;
    }

    public HRESULT initNoGraphics(GFSDK_WaveWorks_Detailed_Simulation_Params params){
        HRESULT hr;
        if(nv_water_d3d_api_gl2 != m_d3dAPI)
        {
            releaseAll();
        }
//        else if(m_d3d._GL2.m_pGLContext != pGLContext)
//        {
//            releaseAll();
//        }

        if(nv_water_d3d_api_undefined == m_d3dAPI)
        {
            m_d3dAPI = nv_water_d3d_api_none;
//            m_d3d._GL2.m_pGLContext = pGLContext;
            m_params = params;

            hr = allocateAll();
            if(hr != HRESULT.S_OK)
                return hr;
        }
        else
        {
            hr = reinit(params);
            if(hr != HRESULT.S_OK)
                return hr;
        }
        return HRESULT.S_OK;
    }

    public HRESULT reinit(GFSDK_WaveWorks_Detailed_Simulation_Params params){
        HRESULT hr;

        boolean bReinitTextureArrays = false;
        if(params.cascades[GFSDK_WaveWorks_Detailed_Simulation_Params.MaxNumCascades - 1].fft_resolution != m_params.cascades[GFSDK_WaveWorks_Detailed_Simulation_Params.MaxNumCascades - 1].fft_resolution)
        {
            bReinitTextureArrays = true;
        }

        boolean bReinitGradMapSamplers = false;
        if(params.aniso_level != m_params.aniso_level)
        {
            bReinitGradMapSamplers = true;
        }

        boolean bReinitSimManager = false;
        if(params.simulation_api != m_params.simulation_api)
        {
            bReinitSimManager = true;
        }
        else if(nv_water_simulation_api_cpu == params.simulation_api && params.CPU_simulation_threading_model != m_params.CPU_simulation_threading_model)
        {
            bReinitSimManager = true;
        }

        boolean[] bAllocateSim=new boolean[GFSDK_WaveWorks_Detailed_Simulation_Params.MaxNumCascades];
        boolean[] bReleaseSim=new boolean[GFSDK_WaveWorks_Detailed_Simulation_Params.MaxNumCascades];
        boolean[] bReleaseRenderingResources=new boolean[GFSDK_WaveWorks_Detailed_Simulation_Params.MaxNumCascades];
        boolean[] bAllocateRenderingResources=new boolean[GFSDK_WaveWorks_Detailed_Simulation_Params.MaxNumCascades];
        boolean[] bReinitSim=new boolean[GFSDK_WaveWorks_Detailed_Simulation_Params.MaxNumCascades];
        int numReinitSims = 0;
        int numReleaseSims = 0;
        int numAllocSims = 0;

        for(int cascade = 0; cascade != GFSDK_WaveWorks_Detailed_Simulation_Params.MaxNumCascades; ++cascade)
        {
            bAllocateSim[cascade] = false;
            bReleaseSim[cascade] = false;
            bReleaseRenderingResources[cascade] = false;
            bAllocateRenderingResources[cascade] = false;
            bReinitSim[cascade] = false;

            if(cascade < params.num_cascades && cascade >= m_params.num_cascades)
            {
                // Cascade being activated
                bAllocateRenderingResources[cascade] = true;
                bAllocateSim[cascade] = true;
                ++numAllocSims;
            }
            else if(cascade < m_params.num_cascades && cascade >= params.num_cascades)
            {
                // Cascade being deactivated
                bReleaseRenderingResources[cascade] = true;
                bReleaseSim[cascade] = true;
                ++numReleaseSims;
            }
            else if(cascade < params.num_cascades)
            {
                // A kept cascade
                if(bReinitSimManager)
                {
                    // Sim manager will be torn down and re-allocated, cascade needs the same treatment
                    bReleaseSim[cascade] = true;
                    bAllocateSim[cascade] = true;
                    ++numReleaseSims;
                    ++numAllocSims;
                }
                else
                {
                    // Sim manager is not being touched: just prod cascade for an internal re-init
                    bReinitSim[cascade] = true;
                    ++numReinitSims;
                }

                if(params.cascades[cascade].fft_resolution != m_params.cascades[cascade].fft_resolution ||
                        params.num_GPUs != m_params.num_GPUs)	// Need to re-alloc per-GPU resources
                {
                    bReleaseRenderingResources[cascade] = true;
                    bAllocateRenderingResources[cascade] = true;
                }
            }
        }

        m_params = params;

        if(numReinitSims > 0) {
            boolean reinitOnly = false;
            if(0 == numAllocSims && 0 == numReleaseSims && numReinitSims == m_params.num_cascades)
            {
                // This is a pure cascade-level reinit
                reinitOnly = true;
            }
            hr = m_pSimulationManager.beforeReinit(m_params, reinitOnly);
            if(hr != HRESULT.S_OK) return hr;
        }

        for(int cascade = 0; cascade != GFSDK_WaveWorks_Detailed_Simulation_Params.MaxNumCascades; ++cascade)
        {
            if(bReleaseSim[cascade])
            {
                releaseSimulation(cascade);
            }
        }

        if(bReinitSimManager)
        {
            releaseSimulationManager();
            hr = allocateSimulationManager();
            if(hr != HRESULT.S_OK) return hr;
        }

        for(int cascade = 0; cascade != GFSDK_WaveWorks_Detailed_Simulation_Params.MaxNumCascades; ++cascade)
        {
            if(bReleaseRenderingResources[cascade])
            {
                releaseRenderingResources(cascade);
            }

            if(bAllocateRenderingResources[cascade])
            {
                hr = allocateRenderingResources(cascade);
                if(hr != HRESULT.S_OK) return hr;
            }

            if(bAllocateSim[cascade])
            {
                hr = allocateSimulation(cascade);
                if(hr != HRESULT.S_OK) return hr;
            }

            if(bReinitSim[cascade])
            {
                hr = cascade_states[cascade].m_pFFTSimulation.reinit(m_params.cascades[cascade]);
                if(hr != HRESULT.S_OK) return hr;
            }
        }
        updateRMS(m_params);
        if(bReinitGradMapSamplers)
        {
//            V_RETURN(initGradMapSamplers());
            hr = initGradMapSamplers();
            if(hr != HRESULT.S_OK) return hr;
        }

        if(bReinitTextureArrays)
        {
//            V_RETURN(initTextureArrays());
            hr = initTextureArrays();
            if(hr != HRESULT.S_OK) return hr;
        }

        return HRESULT.S_OK;
    }

    public void setSimulationTime(double dAppTime){
        m_dSimTime = dAppTime * (double)m_params.time_scale;

        if(m_numValidEntriesInSimTimeFIFO > 0) {
            assert(m_numValidEntriesInSimTimeFIFO==(m_num_GPU_slots+1));
            for(int i=m_numValidEntriesInSimTimeFIFO-1;i>0;i--) {
                m_dSimTimeFIFO[i] = m_dSimTimeFIFO[i-1];
            }
            m_dSimTimeFIFO[0] = m_dSimTime;
        } else {
            // The FIFO is empty, so this must be first tick - prime it
            m_numValidEntriesInSimTimeFIFO=m_num_GPU_slots+1;
            for(int i = 0; i != m_numValidEntriesInSimTimeFIFO; ++i) {
                m_dSimTimeFIFO[i] = m_dSimTime;
            }
        }

        m_dFoamSimDeltaTime = m_dSimTimeFIFO[0] - m_dSimTimeFIFO[m_num_GPU_slots];
        if(m_dFoamSimDeltaTime <=0 ) m_dFoamSimDeltaTime = 0;
    }

    public float getConservativeMaxDisplacementEstimate(){
        // Based on significant wave height: http://en.wikipedia.org/wiki/Significant_wave_height
        //
        // Significant wave height is said to be 1.4x rms and represents a 1 in 3 event
        // Then, given that wave heights follow a Rayleigh distribution, and based on the form of the CDF,
        // we observe that a wave height of 4x significant should be *very* infrequent (1 in 3^16, approx)
        //
        // Hence, we use 4 x 1.4 x rms, or 6x with rounding up!
        //
        return 6.f * m_total_rms;
    }

    public void updateRMS(GFSDK_WaveWorks_Detailed_Simulation_Params params){
        m_total_rms = 0.f;
        for(int i=0; i<GFSDK_WaveWorks_Detailed_Simulation_Params.MaxNumCascades;i++)
        {
            m_total_rms += Simulation_Util.get_spectrum_rms_sqr(params.cascades[i]);
        }
        m_total_rms = (float) Math.sqrt(m_total_rms);
    }

    public HRESULT kick(long[] pKickID, /*Graphics_Context* pGC,*/ GFSDK_WaveWorks_Savestate pSavestateImpl){
        HRESULT hr;
        // Activate GPU slot for current frame
        // TODO: this assumes that we experience one tick per frame - this is unlikely to hold true in general
        //       the difficulty here is how to reliably detect when work switches to a new GPU
        //       - relying on the Kick() will fail if the developer ever ticks twice in a frame (likely)
        //       - relying on SetRenderState() is even more fragile, because it will fail if the water is rendered twice in a frame (very likely)
        //       - we could probably rely on NVAPI on NVIDIA setups, but what about non-NVIDIA setups?
        //       - so seems like we need to support this in the API
        //
        consumeGPUSlot();

        TimerSlot[] pTimerSlot = new TimerSlot[1];
        if(m_pGFXTimer != null)
        {
            hr = queryAllGfxTimers(m_pGFXTimer); if(hr != HRESULT.S_OK) return hr;
            GLCheck.checkError();
            // Bracket GPU work with a disjoint timer query
            hr = m_pGFXTimer.beginDisjoint();if(hr != HRESULT.S_OK) return hr;
            GLCheck.checkError();
            hr = consumeAvailableTimerSlot(m_pGFXTimer, m_gpu_kick_timers, pTimerSlot);if(hr != HRESULT.S_OK) return hr;
            pTimerSlot[0].m_StartQueryIndex = m_pGFXTimer.issueTimerQuery();
            GLCheck.checkError();

            // This is ensures that wait-timers report zero when the wait API is unused
            // The converse is unnecessary, since the user cannot get useful work done without calling kick()
            if(!m_has_consumed_wait_timer_slot_since_last_kick)
            {
                TimerSlot[] pWaitTimerSlot = new TimerSlot[1];
                hr = consumeAvailableTimerSlot(m_pGFXTimer, m_gpu_wait_timers, pWaitTimerSlot);if(hr != HRESULT.S_OK) return hr;

                // Setting the djqi to an invalid index causes this slot to be handled as a 'dummy' query
                // i.e. no attempt will be made to retrieve real GPU timing data, and the timing values
                // already in the slot (i.e. zero) will be used as the timing data, which is what we want
                if(NVWaveWorks_GFX_Timer_Impl.InvalidQueryIndex != pWaitTimerSlot[0].m_DisjointQueryIndex)
                {
                    m_pGFXTimer.releaseDisjointQuery(pWaitTimerSlot[0].m_DisjointQueryIndex);
                    pWaitTimerSlot[0].m_DisjointQueryIndex = NVWaveWorks_GFX_Timer_Impl.InvalidQueryIndex;
                }
            }
        }

        GLCheck.checkError();
        // Reset for next kick-to-kick interval
        m_has_consumed_wait_timer_slot_since_last_kick = false;

        long[] kickID = null;
        if(pKickID != null)
            kickID = pKickID;
        else
            kickID = new long[1];

        hr = m_pSimulationManager.kick(m_dSimTime,kickID);  if(hr != HRESULT.S_OK) return hr;

        if(m_pGFXTimer != null) {
            pTimerSlot[0].m_StartGFXQueryIndex = m_pGFXTimer.issueTimerQuery();
        }

        hr = updateGradientMaps(pSavestateImpl);  if(hr != HRESULT.S_OK) return hr;

        if(m_pGFXTimer != null)
        {
            pTimerSlot[0].m_StopGFXQueryIndex = m_pGFXTimer.issueTimerQuery();

            pTimerSlot[0].m_StopQueryIndex = m_pGFXTimer.issueTimerQuery();

            hr = m_pGFXTimer.endDisjoint();  if(hr != HRESULT.S_OK) return hr;
        }

//        #if WAVEWORKS_ENABLE_GNM
//        if(nv_water_d3d_api_gnm == m_d3dAPI)
//        {
//            gnmxWrap->popMarker(*gfxContext_gnm);
//        }
//        #endif

//        if(pKickID != null)
//        {
//            pKickID[0] = kickID[0];
//        }

        return HRESULT.S_OK;
    }

    public HRESULT getStats(GFSDK_WaveWorks_Simulation_Stats stats){
        GFSDK_WaveWorks_Simulation_Manager_Timings timings = new GFSDK_WaveWorks_Simulation_Manager_Timings();

        // getting the simulation implementation dependent timings
        m_pSimulationManager.getTimings(timings);

        // putting these to stats
        stats.CPU_main_thread_wait_time = timings.time_wait_for_completion;
        stats.CPU_threads_start_to_finish_time = timings.time_start_to_stop;
        stats.CPU_threads_total_time = timings.time_total;

        // collect GPU times individually from cascade members
        stats.GPU_simulation_time = 0.f;
        stats.GPU_FFT_simulation_time = 0.f;

        NVWaveWorks_FFT_Simulation_Timings cascade_member_timing = new NVWaveWorks_FFT_Simulation_Timings();
        for(int cascade = 0; cascade != m_params.num_cascades; ++cascade)
        {
            cascade_states[cascade].m_pFFTSimulation.getTimings(cascade_member_timing);
            stats.GPU_simulation_time += cascade_member_timing.GPU_simulation_time;
            stats.GPU_FFT_simulation_time += cascade_member_timing.GPU_FFT_simulation_time;
        }

        // we collect GFX GPU time ourself during gradient map calcs
        stats.GPU_gfx_time = m_gpu_kick_timers.m_timer_slots[m_gpu_kick_timers.m_active_timer_slot].m_elapsed_gfx_time + m_gpu_wait_timers.m_timer_slots[m_gpu_wait_timers.m_active_timer_slot].m_elapsed_gfx_time;
        stats.GPU_update_time = m_gpu_kick_timers.m_timer_slots[m_gpu_kick_timers.m_active_timer_slot].m_elapsed_time + m_gpu_wait_timers.m_timer_slots[m_gpu_wait_timers.m_active_timer_slot].m_elapsed_time;

        return HRESULT.S_OK;
    }

    public boolean getStagingCursor(long[] pKickID){return m_pSimulationManager.getStagingCursor(pKickID);}
    public HRESULT advanceStagingCursor(/*Graphics_Context* pGC,*/ boolean block, boolean[] wouldBlock, GFSDK_WaveWorks_Savestate pSavestateImpl){
        HRESULT hr;

        AdvanceCursorResult advance_result = m_pSimulationManager.advanceStagingCursor(block);
        switch(advance_result)
        {
            case AdvanceCursorResult_Succeeded:
                wouldBlock[0] = false;
                break; // result, carry on...
            case AdvanceCursorResult_WouldBlock:
                wouldBlock[0] = true;
                return S_FALSE;
            case AdvanceCursorResult_None:
                wouldBlock[0] = false;
                return S_FALSE;
            case AdvanceCursorResult_Failed:
            default:	// Drop-thru from prior case is intentional
                return E_FAIL;
        }

        TimerSlot[] pTimerSlot = new TimerSlot[1];
        if(m_pGFXTimer != null)
        {
            // Check for completed queries
            hr = queryAllGfxTimers(m_pGFXTimer);  if(hr != HRESULT.S_OK) return hr;

            // Bracket GPU work with a disjoint timer query
            hr = m_pGFXTimer.beginDisjoint(); if(hr != HRESULT.S_OK) return hr;

            hr = consumeAvailableTimerSlot(m_pGFXTimer, m_gpu_wait_timers, pTimerSlot);  if(hr != HRESULT.S_OK) return hr;
            pTimerSlot[0].m_StartQueryIndex = m_pGFXTimer.issueTimerQuery();
            pTimerSlot[0].m_StartGFXQueryIndex = m_pGFXTimer.issueTimerQuery();

            m_has_consumed_wait_timer_slot_since_last_kick = true;
        }

        // If new simulation results have become available, it will be necessary to update the gradient maps
        hr = updateGradientMaps(pSavestateImpl); if(hr != HRESULT.S_OK) return hr;

        if(m_pGFXTimer != null)
        {
            pTimerSlot[0].m_StopGFXQueryIndex = m_pGFXTimer.issueTimerQuery();
            pTimerSlot[0].m_StopQueryIndex = m_pGFXTimer.issueTimerQuery();
            hr = m_pGFXTimer.endDisjoint();  if(hr != HRESULT.S_OK) return hr;
        }

        return S_OK;
    }

    HRESULT waitStagingCursor(){
        WaitCursorResult wait_result = m_pSimulationManager.waitStagingCursor();
        switch(wait_result)
        {
            case WaitCursorResult_Succeeded:
                return HRESULT.S_OK;
            case WaitCursorResult_None:
                return HRESULT.S_FALSE;
            case WaitCursorResult_Failed:
            default:	// Drop-thru from prior case is intentional
                return HRESULT.E_FAIL;
        }
    }

    public boolean getReadbackCursor(long[] pKickID){ return m_pSimulationManager.getReadbackCursor(pKickID);}

    public HRESULT advanceReadbackCursor(boolean block, boolean[] wouldBlock){
        AdvanceCursorResult advance_result = m_pSimulationManager.advanceReadbackCursor(block);
        switch(advance_result)
        {
            case AdvanceCursorResult_Succeeded:
                wouldBlock[0] = false;
                return HRESULT.S_OK;
            case AdvanceCursorResult_WouldBlock:
                wouldBlock[0] = true;
                return HRESULT.S_FALSE;
            case AdvanceCursorResult_None:
                wouldBlock[0] = false;
                return HRESULT.S_FALSE;
            case AdvanceCursorResult_Failed:
            default:	// Drop-thru from prior case is intentional
                return HRESULT.E_FAIL;
        }
    }

    public HRESULT archiveDisplacements(){
        return m_pSimulationManager.archiveDisplacements();
    }

    public HRESULT setRenderState(	//Graphics_Context* pGC,
							Matrix4f matView,
							int[] pShaderInputRegisterMappings , GFSDK_WaveWorks_Savestate pSavestateImpl, GFSDK_WaveWorks_Simulation_GL_Pool pGlPool){
        if(!getStagingCursor(null))
            return HRESULT.E_FAIL;

        switch(m_d3dAPI)
        {
//            #if WAVEWORKS_ENABLE_D3D11
            case nv_water_d3d_api_d3d11:
            {
                return setRenderStateD3D11(matView, pShaderInputRegisterMappings, pSavestateImpl);
            }
//            #endif
//            #if WAVEWORKS_ENABLE_GNM
//            case nv_water_d3d_api_gnm:
//            {
//                return setRenderStateGnm(pGC->gnm(), matView, pShaderInputRegisterMappings, pSavestateImpl);
//            }
//            #endif
//            #if WAVEWORKS_ENABLE_GL
            case nv_water_d3d_api_gl2:
            {
                if(null == pGlPool)
                {
                    LogUtil.e(LogUtil.LogType.DEFAULT, "ERROR: a valid gl pool is required when setting simulation state for gl rendering");
                    return HRESULT.E_FAIL;
                }

                HRESULT hr = setRenderStateGL2(matView, pShaderInputRegisterMappings, pGlPool);
                return hr;
            }
//            #endif
            default:
                return HRESULT.E_FAIL;
        }
    }

    public HRESULT getDisplacements(	Vector2f[] inSamplePoints,
                                Vector4f[] outDisplacements,
                                 int numSamples){
        HRESULT hr;

        // Initialise displacements
//        memset(outDisplacements, 0, numSamples * sizeof(*outDisplacements));
        for(int i = 0; i < numSamples; i++){
            if(outDisplacements[i] != null){
                outDisplacements[i].set(0,0,0,0);
            }else{
                outDisplacements[i] = new Vector4f();
            }
        }

        for(int cascade = 0; cascade != m_params.num_cascades; ++cascade)
        {
            hr = cascade_states[cascade].m_pFFTSimulation.addDisplacements(inSamplePoints,outDisplacements,numSamples);
            if(hr != HRESULT.S_OK)
                return hr;
        }

        return HRESULT.S_OK;
    }

    public HRESULT getArchivedDisplacements(	float coord,
                                         Vector2f[] inSamplePoints,
                                         Vector4f[] outDisplacements,
                                         int numSamples){
        HRESULT hr;

        // Initialise displacements
//        memset(outDisplacements, 0, numSamples * sizeof(*outDisplacements));
        for(int i = 0; i < numSamples; i++){
            if(outDisplacements[i] != null){
                outDisplacements[i].set(0,0,0,0);
            }else{
                outDisplacements[i] = new Vector4f();
            }
        }

        for(int cascade = 0; cascade != m_params.num_cascades; ++cascade)
        {
            hr = cascade_states[cascade].m_pFFTSimulation.addArchivedDisplacements(coord,inSamplePoints,outDisplacements,numSamples);
            if(hr != HRESULT.S_OK)
                return hr;
        }

        return S_OK;
    }

    public static int getShaderInputCountD3D11() { return NumShaderInputsD3D11; }
    public static HRESULT getShaderInputDescD3D11(int inputIndex, GFSDK_WaveWorks_ShaderInput_Desc pDesc){
        if(inputIndex >= NumShaderInputsD3D11)
            return HRESULT.E_FAIL;

//        *pDesc = ShaderInputDescsD3D11[inputIndex];  TODO

        return HRESULT.S_OK;
    }
    public static int getShaderInputCountGnm() { return 0;}
    public static HRESULT getShaderInputDescGnm(int inputIndex, GFSDK_WaveWorks_ShaderInput_Desc pDesc){
        return HRESULT.E_FAIL;
    }
    public static int getShaderInputCountGL2() { return NumShaderInputsGL2;}
    public static int getTextureUnitCountGL2(boolean useTextureArrays) { return useTextureArrays? 2:8;}
    public static HRESULT getShaderInputDescGL2(int inputIndex, GFSDK_WaveWorks_ShaderInput_Desc pDesc){
        if(inputIndex >= NumShaderInputsGL2)
            return HRESULT.E_FAIL;

        pDesc.set(ShaderInputDescsGL2[inputIndex]);

        return HRESULT.S_OK;
    }

    @Override
    public void dispose() {releaseAll();}

    private HRESULT updateGradientMaps(/*Graphics_Context* pGC,*/ GFSDK_WaveWorks_Savestate pSavestateImpl){
        HRESULT result;

        switch(m_d3dAPI)
        {
//            #if WAVEWORKS_ENABLE_D3D11
            case nv_water_d3d_api_d3d11:
                result=updateGradientMapsD3D11( pSavestateImpl);
                break;
//            #endif
//            #if WAVEWORKS_ENABLE_GNM
//            case nv_water_d3d_api_gnm:
//                result=updateGradientMapsGnm(pGC, pSavestateImpl);
//                break;
//            #endif
//            #if WAVEWORKS_ENABLE_GL
            case nv_water_d3d_api_gl2:
                result=updateGradientMapsGL2();
                break;
//            #endif
            case nv_water_d3d_api_none:
                // No graphics, nothing to do
                result=HRESULT.S_OK;
                break;
            default:
                result=HRESULT.E_FAIL;
                break;
        }

        return result;
    }

    private HRESULT updateGradientMapsD3D11(/*Graphics_Context* pGC,*/ GFSDK_WaveWorks_Savestate pSavestateImpl){
        HRESULT hr;

//        ID3D11DeviceContext* pDC_d3d11 = pGC->d3d11();

        // Preserve
        if(pSavestateImpl != null)
        {
//            V_RETURN(pSavestateImpl->PreserveD3D11Viewport(pDC_d3d11));
//            V_RETURN(pSavestateImpl->PreserveD3D11RenderTargets(pDC_d3d11));
//            V_RETURN(pSavestateImpl->PreserveD3D11Shaders(pDC_d3d11));
//            V_RETURN(pSavestateImpl->PreserveD3D11PixelShaderConstantBuffer(pDC_d3d11,0));
//            V_RETURN(pSavestateImpl->PreserveD3D11PixelShaderSampler(pDC_d3d11,0));
//            V_RETURN(pSavestateImpl->PreserveD3D11PixelShaderResource(pDC_d3d11,0));
//            V_RETURN(pSavestateImpl->PreserveD3D11DepthStencil(pDC_d3d11));
//            V_RETURN(pSavestateImpl->PreserveD3D11Blend(pDC_d3d11));
//            V_RETURN(pSavestateImpl->PreserveD3D11Raster(pDC_d3d11));
//
//            for(int cascade = 0; cascade != m_params.num_cascades; ++cascade)
//            {
//                V_RETURN(cascade_states[cascade].m_pQuadMesh->PreserveState(pGC, pSavestateImpl));
//            }
        }

        TextureAttachDesc attachDesc = m_AttachDescs[0];
        attachDesc.index = 0;
        attachDesc.type = AttachType.TEXTURE_2D;
        attachDesc.layer = 0;
        attachDesc.level = 0;

        for(int cascade = 0; cascade != m_params.num_cascades; ++cascade)
        {
            if(cascade_states[cascade].m_gradient_map_version == cascade_states[cascade].m_pFFTSimulation.getDisplacementMapVersion())
                continue;

            // Rendering folding to gradient map //////////////////////////////////

            // Render-targets + viewport
//            pDC_d3d11->OMSetRenderTargets(1, &cascade_states[cascade].m_d3d._11.m_pd3d11GradientRenderTarget[m_active_GPU_slot], NULL);
            m_RenderTarget.bind();
            m_RenderTarget.setRenderTexture(cascade_states[cascade].m_d3d._11.m_pd3d11GradientRenderTarget[m_active_GPU_slot], attachDesc);

            // Clear the gradient map if necessary
//            final float kBlack[] = {0.f,0.f,0.f,0.f};
            if(cascade_states[cascade].m_gradient_map_needs_clear[m_active_GPU_slot]) {
//                pDC_d3d11->ClearRenderTargetView(cascade_states[cascade].m_d3d._11.m_pd3d11GradientRenderTarget[m_active_GPU_slot],kBlack);
                gl.glClearBufferfv(GLenum.GL_COLOR, 0, CacheBuffer.wrap(0.0f, 0.0f, 0.0f, 0.0f));
                cascade_states[cascade].m_gradient_map_needs_clear[m_active_GPU_slot] = false;
            }


            int dmap_dim =m_params.cascades[cascade].fft_resolution;
//            D3D11_VIEWPORT new_vp;
//            new_vp.TopLeftX = 0;
//            new_vp.TopLeftY = 0;
//            new_vp.Width = FLOAT(dmap_dim);
//            new_vp.Height = FLOAT(dmap_dim);
//            new_vp.MinDepth = 0.f;
//            new_vp.MaxDepth = 0.f;
//            UINT num_new_vp = 1;
//            pDC_d3d11->RSSetViewports(num_new_vp, &new_vp);
            gl.glViewport(0,0, dmap_dim, dmap_dim);

            // Shaders
//            pDC_d3d11->VSSetShader(m_d3d._11.m_pd3d11GradCalcVS, NULL, 0);
//            pDC_d3d11->HSSetShader(NULL,NULL,0);
//            pDC_d3d11->DSSetShader(NULL,NULL,0);
//            pDC_d3d11->GSSetShader(NULL,NULL,0);
//            pDC_d3d11->PSSetShader(m_d3d._11.m_pd3d11GradCalcPS, NULL, 0);
            m_d3d._11.m_pd3d11GradCalcProgram.enable();

            // Constants
            {
//                D3D11_CB_Updater<ps_calcgradient_cbuffer> cbu(pDC_d3d11,m_d3d._11.m_pd3d11GradCalcPixelShaderCB);
                m_calcgradient_cbuffer.g_ChoppyScale = m_params.cascades[cascade].choppy_scale * dmap_dim / m_params.cascades[cascade].fft_period;
                if(m_params.cascades[0].fft_period > 1000.0f) m_calcgradient_cbuffer.g_ChoppyScale *= 1.0f + 0.2f * Math.log(m_params.cascades[0].fft_period/1000.0f);
                m_calcgradient_cbuffer.g_GradMap2TexelWSScale = 0.5f*dmap_dim / m_params.cascades[cascade].fft_period ;
                m_calcgradient_cbuffer.g_OneTexel_Left.set(-1.0f/dmap_dim, 0, 0, 0);
                m_calcgradient_cbuffer.g_OneTexel_Right.set( 1.0f/dmap_dim, 0, 0, 0);
                m_calcgradient_cbuffer.g_OneTexel_Back.set( 0,-1.0f/dmap_dim, 0, 0);
                m_calcgradient_cbuffer.g_OneTexel_Front.set( 0, 1.0f/dmap_dim, 0, 0);

                ByteBuffer buf = CacheBuffer.getCachedByteBuffer(ps_calcgradient_cbuffer.SIZE);
                m_calcgradient_cbuffer.store(buf).flip();

                UpdateSubresource(GLenum.GL_UNIFORM_BUFFER, m_d3d._11.m_pd3d11GradCalcPixelShaderCB, buf);
                if(!m_printOnce){
                    System.out.println("cascade: " + cascade);
                    System.out.println(m_calcgradient_cbuffer);
                }
            }
//            pDC_d3d11->PSSetConstantBuffers(0, 1, &m_d3d._11.m_pd3d11GradCalcPixelShaderCB);
            gl.glBindBufferBase(GLenum.GL_UNIFORM_BUFFER, 0, m_d3d._11.m_pd3d11GradCalcPixelShaderCB);

            // Textures/samplers
//            pDC_d3d11->PSSetShaderResources(0, 1, cascade_states[cascade].m_pFFTSimulation->GetDisplacementMapD3D11());
//            pDC_d3d11->PSSetSamplers(0, 1, &m_d3d._11.m_pd3d11PointSampler);
            Texture2D displacementMap = cascade_states[cascade].m_pFFTSimulation.GetDisplacementMapD3D11();
            gl.glActiveTexture(GLenum.GL_TEXTURE0);
            gl.glBindTexture(displacementMap.getTarget(), displacementMap.getTexture());
            gl.glBindSampler(0, m_d3d._11.m_pd3d11PointSampler);

            // Render state
//            pDC_d3d11->OMSetDepthStencilState(m_d3d._11.m_pd3d11NoDepthStencil, 0);
//            pDC_d3d11->OMSetBlendState(m_d3d._11.m_pd3d11CalcGradBlendState, NULL, 0xFFFFFFFF);
//            pDC_d3d11->RSSetState(m_d3d._11.m_pd3d11AlwaysSolidRasterizer);
            m_d3d._11.m_pd3d11NoDepthStencil.run();
            m_d3d._11.m_pd3d11CalcGradBlendState.run();
            m_d3d._11.m_pd3d11AlwaysSolidRasterizer.run();

            // Draw
//            V_RETURN(cascade_states[cascade].m_pQuadMesh->Draw(pGC, NVWaveWorks_Mesh::PT_TriangleStrip, 0, 0, 4, 0, 2, NULL));
            hr = cascade_states[cascade].m_pQuadMesh.Draw(/*pGC,*/ PT_TriangleStrip, 0, 0, 4, 0, 2, null);
            if(hr != HRESULT.S_OK) return hr;

            if(!m_printOnce){
                m_d3d._11.m_pd3d11GradCalcProgram.setName("Grad Calculation");
                m_d3d._11.m_pd3d11GradCalcProgram.printPrograminfo();

                Simulation_Util.saveTextData("Gradient" + cascade + ".txt", cascade_states[cascade].m_d3d._11.m_pd3d11GradientRenderTarget[m_active_GPU_slot]);
            }

            // Accumulating energy in foam energy map //////////////////////////////////

            // Render-targets + viewport
//            pDC_d3d11->OMSetRenderTargets(1, &cascade_states[cascade].m_d3d._11.m_pd3d11FoamEnergyRenderTarget, NULL);
            m_RenderTarget.setRenderTexture(cascade_states[cascade].m_d3d._11.m_pd3d11FoamEnergyRenderTarget, attachDesc);

            // Clear the foam map, to ensure inter-frame deps get broken on multi-GPU
//            pDC_d3d11->ClearRenderTargetView(cascade_states[cascade].m_d3d._11.m_pd3d11FoamEnergyRenderTarget,kBlack);
            gl.glClearBufferfv(GLenum.GL_COLOR, 0, CacheBuffer.wrap(0.f,0.f,0.f,0.f));

            dmap_dim = m_params.cascades[cascade].fft_resolution;
//            new_vp.TopLeftX = 0;
//            new_vp.TopLeftY = 0;
//            new_vp.Width = FLOAT(dmap_dim);
//            new_vp.Height = FLOAT(dmap_dim);
//            new_vp.MinDepth = 0.f;
//            new_vp.MaxDepth = 0.f;
//            num_new_vp = 1;
//            pDC_d3d11->RSSetViewports(num_new_vp, &new_vp);
            gl.glViewport(0,0,dmap_dim, dmap_dim);

            // Shaders
//            pDC_d3d11->VSSetShader(m_d3d._11.m_pd3d11FoamGenVS,NULL,0);
//            pDC_d3d11->HSSetShader(NULL,NULL,0);
//            pDC_d3d11->DSSetShader(NULL,NULL,0);
//            pDC_d3d11->GSSetShader(NULL,NULL,0);
//            pDC_d3d11->PSSetShader(m_d3d._11.m_pd3d11FoamGenPS,NULL,0);
            m_d3d._11.m_pd3d11FoamGenProgram.enable();
            // Constants
            {
//                D3D11_CB_Updater<ps_foamgeneration_cbuffer> cbu(pDC_d3d11,m_d3d._11.m_pd3d11FoamGenPixelShaderCB);
                m_foamgeneration_cbuffer.g_SourceComponents.set(0,0,0.0f,1.0f); // getting component W of grad map as source for energy
                m_foamgeneration_cbuffer.g_UVOffsets.set(0,1.0f,0,0); // blurring by Y
                m_foamgeneration_cbuffer.g_DissipationFactors_Accumulation = m_params.cascades[cascade].foam_generation_amount*(float)m_dFoamSimDeltaTime*50.0f;
                m_foamgeneration_cbuffer.g_DissipationFactors_Fadeout		= (float) Math.pow(m_params.cascades[cascade].foam_falloff_speed,(float)m_dFoamSimDeltaTime*50.0f);
                m_foamgeneration_cbuffer.g_DissipationFactors_BlurExtents	= Math.min(0.5f,m_params.cascades[cascade].foam_dissipation_speed*(float)m_dFoamSimDeltaTime*m_params.cascades[0].fft_period * (1000.0f/m_params.cascades[0].fft_period)/m_params.cascades[cascade].fft_period)/dmap_dim;
                m_foamgeneration_cbuffer.g_FoamGenerationThreshold			= m_params.cascades[cascade].foam_generation_threshold;

                ByteBuffer buf = CacheBuffer.getCachedByteBuffer(ps_foamgeneration_cbuffer.SIZE);
                m_foamgeneration_cbuffer.store(buf).flip();
                UpdateSubresource(GLenum.GL_UNIFORM_BUFFER, m_d3d._11.m_pd3d11FoamGenPixelShaderCB, buf);
            }
//            pDC_d3d11->PSSetConstantBuffers(0, 1, &m_d3d._11.m_pd3d11FoamGenPixelShaderCB);
            gl.glBindBufferBase(GLenum.GL_UNIFORM_BUFFER, 0, m_d3d._11.m_pd3d11FoamGenPixelShaderCB);

            // Textures/samplers
//            pDC_d3d11->PSSetShaderResources(0, 1, &cascade_states[cascade].m_d3d._11.m_pd3d11GradientMap[m_active_GPU_slot]);
//            pDC_d3d11->PSSetSamplers(0, 1, &m_d3d._11.m_pd3d11LinearNoMipSampler);
            Texture2D gradientMap = cascade_states[cascade].m_d3d._11.m_pd3d11GradientMap[m_active_GPU_slot];
            gl.glActiveTexture(GLenum.GL_TEXTURE0);
            gl.glBindTexture(gradientMap.getTarget(), gradientMap.getTexture());
            gl.glBindSampler(0, m_d3d._11.m_pd3d11LinearNoMipSampler);

            // Render state
//            pDC_d3d11->OMSetDepthStencilState(m_d3d._11.m_pd3d11NoDepthStencil, 0);
//            pDC_d3d11->OMSetBlendState(m_d3d._11.m_pd3d11AccumulateFoamBlendState, NULL, 0xFFFFFFFF);
//            pDC_d3d11->RSSetState(m_d3d._11.m_pd3d11AlwaysSolidRasterizer);

            m_d3d._11.m_pd3d11NoDepthStencil.run();
            m_d3d._11.m_pd3d11AccumulateFoamBlendState.run();
            m_d3d._11.m_pd3d11AlwaysSolidRasterizer.run();


            // Draw
            hr = cascade_states[cascade].m_pQuadMesh.Draw(PT_TriangleStrip, 0, 0, 4, 0, 2, null);
            if(hr != HRESULT.S_OK)  return hr;

            if(!m_printOnce){
                m_d3d._11.m_pd3d11FoamGenProgram.setName("Foam Generation BlurY");
                m_d3d._11.m_pd3d11FoamGenProgram.printPrograminfo();

                Simulation_Util.saveTextData("FoamY" + cascade + ".txt", cascade_states[cascade].m_d3d._11.m_pd3d11FoamEnergyRenderTarget);
            }

            // Clear shader resource from inputs
//            ID3D11ShaderResourceView* pNullSRV = NULL;
//            pDC_d3d11->PSSetShaderResources(0, 1, &pNullSRV);

            // Writing back energy to gradient map //////////////////////////////////

            // Render-targets + viewport
//            pDC_d3d11->OMSetRenderTargets(1, &cascade_states[cascade].m_d3d._11.m_pd3d11GradientRenderTarget[m_active_GPU_slot], NULL);
            m_RenderTarget.setRenderTexture(cascade_states[cascade].m_d3d._11.m_pd3d11GradientRenderTarget[m_active_GPU_slot], attachDesc);

            dmap_dim = m_params.cascades[cascade].fft_resolution;
//            new_vp.TopLeftX = 0;
//            new_vp.TopLeftY = 0;
//            new_vp.Width = FLOAT(dmap_dim);
//            new_vp.Height = FLOAT(dmap_dim);
//            new_vp.MinDepth = 0.f;
//            new_vp.MaxDepth = 0.f;
//            num_new_vp = 1;
//            pDC_d3d11->RSSetViewports(num_new_vp, &new_vp);
            gl.glViewport(0,0,dmap_dim, dmap_dim);

            // Shaders
//            pDC_d3d11->VSSetShader(m_d3d._11.m_pd3d11FoamGenVS,NULL,0);
//            pDC_d3d11->HSSetShader(NULL,NULL,0);
//            pDC_d3d11->DSSetShader(NULL,NULL,0);
//            pDC_d3d11->GSSetShader(NULL,NULL,0);
//            pDC_d3d11->PSSetShader(m_d3d._11.m_pd3d11FoamGenPS,NULL,0);
            m_d3d._11.m_pd3d11FoamGenProgram.enable();

            // Constants
            {
//                D3D11_CB_Updater<ps_foamgeneration_cbuffer> cbu(pDC_d3d11,m_d3d._11.m_pd3d11FoamGenPixelShaderCB);
                m_foamgeneration_cbuffer.g_SourceComponents.set(1.0f,0,0,0); // getting component R of energy map as source for energy
                m_foamgeneration_cbuffer.g_UVOffsets.set(1.0f,0,0,0); // blurring by X
                m_foamgeneration_cbuffer.g_DissipationFactors_Accumulation = 0.0f;
                m_foamgeneration_cbuffer.g_DissipationFactors_Fadeout		= 1.0f;
                m_foamgeneration_cbuffer.g_DissipationFactors_BlurExtents	= Math.min(0.5f,m_params.cascades[cascade].foam_dissipation_speed*(float)m_dFoamSimDeltaTime* (1000.0f/m_params.cascades[0].fft_period) * m_params.cascades[0].fft_period/m_params.cascades[cascade].fft_period)/dmap_dim;
                m_foamgeneration_cbuffer.g_FoamGenerationThreshold			= m_params.cascades[cascade].foam_generation_threshold;

                ByteBuffer buf = CacheBuffer.getCachedByteBuffer(ps_foamgeneration_cbuffer.SIZE);
                m_foamgeneration_cbuffer.store(buf).flip();
                UpdateSubresource(GLenum.GL_UNIFORM_BUFFER, m_d3d._11.m_pd3d11FoamGenPixelShaderCB, buf);
            }
//            pDC_d3d11->PSSetConstantBuffers(0, 1, &m_d3d._11.m_pd3d11FoamGenPixelShaderCB);
            gl.glBindBufferBase(GLenum.GL_UNIFORM_BUFFER, 0, m_d3d._11.m_pd3d11FoamGenPixelShaderCB);

            // Textures/samplers
//            pDC_d3d11->PSSetShaderResources(0, 1, &cascade_states[cascade].m_d3d._11.m_pd3d11FoamEnergyMap);
//            pDC_d3d11->PSSetSamplers(0, 1, &m_d3d._11.m_pd3d11LinearNoMipSampler);
            Texture2D pd3d11FoamEnergyMap = cascade_states[cascade].m_d3d._11.m_pd3d11FoamEnergyMap;
            gl.glBindTexture(pd3d11FoamEnergyMap.getTarget(), pd3d11FoamEnergyMap.getTexture());
            gl.glBindSampler(0, m_d3d._11.m_pd3d11LinearNoMipSampler);

            // Render state
//            pDC_d3d11->OMSetDepthStencilState(m_d3d._11.m_pd3d11NoDepthStencil, 0);
//            pDC_d3d11->OMSetBlendState(m_d3d._11.m_pd3d11WriteAccumulatedFoamBlendState, NULL, 0xFFFFFFFF);
//            pDC_d3d11->RSSetState(m_d3d._11.m_pd3d11AlwaysSolidRasterizer);
            m_d3d._11.m_pd3d11NoDepthStencil.run();
            m_d3d._11.m_pd3d11WriteAccumulatedFoamBlendState.run();
            m_d3d._11.m_pd3d11AlwaysSolidRasterizer.run();

            // Draw
            hr = cascade_states[cascade].m_pQuadMesh.Draw(NVWaveWorks_Mesh.PrimitiveType.PT_TriangleStrip, 0, 0, 4, 0, 2, null);

            if(!m_printOnce){
                m_d3d._11.m_pd3d11FoamGenProgram.setName("Foam Generation BlurX");
                m_d3d._11.m_pd3d11FoamGenProgram.printPrograminfo();

                Simulation_Util.saveTextData("FoamX" + cascade + ".txt", cascade_states[cascade].m_d3d._11.m_pd3d11GradientRenderTarget[m_active_GPU_slot]);
            }

            // Generate mips
//            pDC_d3d11->GenerateMips(cascade_states[cascade].m_d3d._11.m_pd3d11GradientMap[m_active_GPU_slot]);
            gl.glGenerateTextureMipmap(cascade_states[cascade].m_d3d._11.m_pd3d11GradientMap[m_active_GPU_slot].getTexture()); // TODO Need check

            cascade_states[cascade].m_gradient_map_version = cascade_states[cascade].m_pFFTSimulation.getDisplacementMapVersion();
        }

        // Clear any lingering displacement map reference
//        ID3D11ShaderResourceView* pNullSRV = NULL;
//        pDC_d3d11->PSSetShaderResources(0, 1, &pNullSRV);
        gl.glColorMask(true, true, true, true);
        m_printOnce = true;
        return HRESULT.S_OK;
    }

    private HRESULT updateGradientMapsGnm(/*Graphics_Context* pGC,*/ GFSDK_WaveWorks_Savestate pSavestateImpl){ return HRESULT.S_FALSE;}

    private HRESULT updateGradientMapsGL2(/*Graphics_Context* pGC*/){
        HRESULT hr;

        // No state preservation in GL

        for(int cascade = 0; cascade != m_params.num_cascades; ++cascade)
        {
            if(cascade_states[cascade].m_gradient_map_version == cascade_states[cascade].m_pFFTSimulation.getDisplacementMapVersion()) continue;

            // Rendering folding to gradient map //////////////////////////////////
            // Set render target
            gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, cascade_states[cascade].m_d3d._GL2.m_GL2GradientFBO[m_active_GPU_slot]);
            final int bufs = GLenum.GL_COLOR_ATTACHMENT0;
            gl.glDrawBuffers(bufs);
            gl.glViewport(0, 0, m_params.cascades[cascade].fft_resolution,m_params.cascades[cascade].fft_resolution);

            // Clear the gradient map if necessary
            if(cascade_states[cascade].m_gradient_map_needs_clear[m_active_GPU_slot])
            {
                gl.glColorMask(true,true,true,true);
                gl.glClearColor(0.0f,0.0f,0.0f,0.0f); 
                gl.glClear(GLenum.GL_COLOR_BUFFER_BIT);
                cascade_states[cascade].m_gradient_map_needs_clear[m_active_GPU_slot] = false;
            }
            // Shaders
            gl.glUseProgram(m_d3d._GL2.m_GradCalcProgram.getProgram());

            // Constants
            int dmap_dim =m_params.cascades[cascade].fft_resolution;

            float choppyScale = m_params.cascades[cascade].choppy_scale * dmap_dim / m_params.cascades[cascade].fft_period;
            if(m_params.cascades[0].fft_period > 1000.0f) choppyScale *= 1.0f + 0.2f * Math.log(m_params.cascades[0].fft_period/1000.0f);
            float g_GradMap2TexelWSScale = 0.5f*dmap_dim / m_params.cascades[cascade].fft_period;

//            gfsdk_float4 scales = gfsdk_make_float4(choppyScale, g_GradMap2TexelWSScale, 0, 0);
            gl.glUniform4f(m_d3d._GL2.m_GradCalcUniformLocation_Scales, choppyScale, g_GradMap2TexelWSScale, 0, 0);

//            gfsdk_float4 oneLeft = gfsdk_make_float4(-1.0f/dmap_dim, 0, 0, 0);
            gl.glUniform4f(m_d3d._GL2.m_GradCalcUniformLocation_OneLeft, -1.0f/dmap_dim, 0, 0, 0);

//            gfsdk_float4 oneRight = gfsdk_make_float4( 1.0f/dmap_dim, 0, 0, 0);
            gl.glUniform4f(m_d3d._GL2.m_GradCalcUniformLocation_OneRight, 1.0f/dmap_dim, 0, 0, 0);

//            gfsdk_float4 oneBack = gfsdk_make_float4( 0,-1.0f/dmap_dim, 0, 0);
            gl.glUniform4f(m_d3d._GL2.m_GradCalcUniformLocation_OneBack, 0,-1.0f/dmap_dim, 0, 0);

//            gfsdk_float4 oneFront = gfsdk_make_float4( 0, 1.0f/dmap_dim, 0, 0);
            gl.glUniform4f(m_d3d._GL2.m_GradCalcUniformLocation_OneFront, 0, 1.0f/dmap_dim, 0, 0);

            // Textures/samplers
            gl.glActiveTexture(GLenum.GL_TEXTURE0 + m_d3d._GL2.m_GradCalcTextureUnit_DisplacementMap);
            gl.glBindTexture(GLenum.GL_TEXTURE_2D, cascade_states[cascade].m_pFFTSimulation.GetDisplacementMapGL2());
            gl.glTexParameteri(GLenum.GL_TEXTURE_2D, GLenum.GL_TEXTURE_MIN_FILTER, GLenum.GL_NEAREST);
            gl.glTexParameteri(GLenum.GL_TEXTURE_2D, GLenum.GL_TEXTURE_MAG_FILTER, GLenum.GL_NEAREST);
            gl.glTexParameteri(GLenum.GL_TEXTURE_2D,GLenum.GL_TEXTURE_WRAP_S, GLenum.GL_REPEAT);
            gl.glTexParameteri(GLenum.GL_TEXTURE_2D,GLenum.GL_TEXTURE_WRAP_T, GLenum.GL_REPEAT);
            gl.glUniform1i(m_d3d._GL2.m_GradCalcTextureBindLocation_DisplacementMap, m_d3d._GL2.m_GradCalcTextureUnit_DisplacementMap); 

            // Render state
            gl.glColorMask(true,true,true,false);
            gl.glDisable(GLenum.GL_DEPTH_TEST);
            gl.glDisable(GLenum.GL_BLEND);
            gl.glDisable(GLenum.GL_STENCIL_TEST);
            gl.glDisable(GLenum.GL_CULL_FACE);

            // Draw
            final int calcGradAttribLocations[] = { m_d3d._GL2.m_GradCalcAttributeLocation_Pos, m_d3d._GL2.m_GradCalcAttributeLocation_TexCoord };
            hr = cascade_states[cascade].m_pQuadMesh.Draw(NVWaveWorks_Mesh.PrimitiveType.PT_TriangleStrip, 0, 0, 4, 0, 2,
                    /*calcGradAttribLocations*/null);  // TODO Note the attribute location.
            if(hr != HRESULT.S_OK)  return hr;
            gl.glBindTexture(GLenum.GL_TEXTURE_2D, 0);

            // Accumulating energy in foam energy map //////////////////////////////////

            // Set targets
            gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, cascade_states[cascade].m_d3d._GL2.m_GL2FoamEnergyFBO);
            gl.glViewport(0, 0, m_params.cascades[cascade].fft_resolution,m_params.cascades[cascade].fft_resolution);

            // Clear the foam map, to ensure inter-frame deps get broken on multi-GPU
            gl.glColorMask(true,true,true,true);
            gl.glClearColor(0.0f,0.0f,0.0f,0.0f); 
            gl.glClear(GLenum.GL_COLOR_BUFFER_BIT);

            // Shaders
            gl.glUseProgram(m_d3d._GL2.m_FoamGenProgram.getProgram());

            // Constants
            float g_DissipationFactorsZ    = m_params.cascades[cascade].foam_generation_amount*(float)m_dFoamSimDeltaTime*50.0f;
            float g_DissipationFactorsY	   = (float) Math.pow(m_params.cascades[cascade].foam_falloff_speed,(float)m_dFoamSimDeltaTime*50.0f);
            float g_DissipationFactorsX    = Math.min(0.5f,m_params.cascades[cascade].foam_dissipation_speed*(float)m_dFoamSimDeltaTime*m_params.cascades[0].fft_period * (1000.0f/m_params.cascades[0].fft_period)/m_params.cascades[cascade].fft_period)/dmap_dim;
            float g_DissipationFactorsW    = m_params.cascades[cascade].foam_generation_threshold;
//            gfsdk_float4 g_SourceComponents = gfsdk_make_float4(0,0,0.0f,1.0f); // getting component W of grad map as source for energy
//            gfsdk_float4 g_UVOffsets = gfsdk_make_float4(0,1.0f,0,0);			 // blurring by Y
            gl.glUniform4f(m_d3d._GL2.m_FoamGenUniformLocation_DissipationFactors, g_DissipationFactorsX, g_DissipationFactorsY, g_DissipationFactorsZ, g_DissipationFactorsW);
            gl.glUniform4f(m_d3d._GL2.m_FoamGenUniformLocation_SourceComponents, 0,0,0.0f,1.0f);
            gl.glUniform4f(m_d3d._GL2.m_FoamGenUniformLocation_UVOffsets, 0,1.0f,0,0);

            // Textures / samplers
            gl.glActiveTexture(GLenum.GL_TEXTURE0 + m_d3d._GL2.m_FoamGenTextureUnit_EnergyMap);
            gl.glBindTexture(GLenum.GL_TEXTURE_2D, cascade_states[cascade].m_d3d._GL2.m_GL2GradientMap[m_active_GPU_slot]);
            gl.glTexParameteri(GLenum.GL_TEXTURE_2D, GLenum.GL_TEXTURE_MIN_FILTER, GLenum.GL_LINEAR);
            gl.glTexParameteri(GLenum.GL_TEXTURE_2D, GLenum.GL_TEXTURE_MAG_FILTER, GLenum.GL_LINEAR);
            gl.glTexParameteri(GLenum.GL_TEXTURE_2D,GLenum.GL_TEXTURE_WRAP_S, GLenum.GL_REPEAT);
            gl.glTexParameteri(GLenum.GL_TEXTURE_2D,GLenum.GL_TEXTURE_WRAP_T, GLenum.GL_REPEAT);
            gl.glUniform1i(m_d3d._GL2.m_FoamGenTextureBindLocation_EnergyMap, m_d3d._GL2.m_FoamGenTextureUnit_EnergyMap); 

            // Render state
            gl.glColorMask(true,true,true,true);
            gl.glDisable(GLenum.GL_DEPTH_TEST);
            gl.glDisable(GLenum.GL_BLEND);
            gl.glDisable(GLenum.GL_STENCIL_TEST);
            gl.glDisable(GLenum.GL_CULL_FACE);

            // Draw
            final int foamGenAttribLocations[] = { m_d3d._GL2.m_FoamGenAttributeLocation_Pos, m_d3d._GL2.m_FoamGenAttributeLocation_TexCoord };
            hr = cascade_states[cascade].m_pQuadMesh.Draw(/*pGC,*/ NVWaveWorks_Mesh.PrimitiveType.PT_TriangleStrip, 0, 0, 4, 0, 2, foamGenAttribLocations);
            if(hr != HRESULT.S_OK)  return hr;
            gl.glBindTexture(GLenum.GL_TEXTURE_2D, 0);

            // Writing back energy to gradient map //////////////////////////////////

            // Set targets
            gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, cascade_states[cascade].m_d3d._GL2.m_GL2GradientFBO[m_active_GPU_slot]);
            gl.glViewport(0, 0, m_params.cascades[cascade].fft_resolution,m_params.cascades[cascade].fft_resolution);

            // Shaders
            gl.glUseProgram(m_d3d._GL2.m_FoamGenProgram.getProgram());

            // Constants
            g_DissipationFactorsZ     = 0;
            g_DissipationFactorsY	  = 1.0f;
            g_DissipationFactorsX 	  = Math.min(0.5f,m_params.cascades[cascade].foam_dissipation_speed*(float)m_dFoamSimDeltaTime*m_params.cascades[0].fft_period * (1000.0f/m_params.cascades[0].fft_period)/m_params.cascades[cascade].fft_period)/dmap_dim;
            g_DissipationFactorsW     = 0;
//            g_SourceComponents = gfsdk_make_float4(1.0f,0,0,0); // getting component R of energy map as source for energy
//            g_UVOffsets = gfsdk_make_float4(1.0f,0,0,0);			 // blurring by Y
            gl.glUniform4f(m_d3d._GL2.m_FoamGenUniformLocation_DissipationFactors, g_DissipationFactorsX, g_DissipationFactorsY, g_DissipationFactorsZ, g_DissipationFactorsW);
            gl.glUniform4f(m_d3d._GL2.m_FoamGenUniformLocation_SourceComponents, 1.0f,0,0,0);
            gl.glUniform4f(m_d3d._GL2.m_FoamGenUniformLocation_UVOffsets, 1.0f,0,0,0);

            // Textures / samplers
            gl.glActiveTexture(GLenum.GL_TEXTURE0 + m_d3d._GL2.m_FoamGenTextureUnit_EnergyMap);
            gl.glBindTexture(GLenum.GL_TEXTURE_2D, cascade_states[cascade].m_d3d._GL2.m_GL2FoamEnergyMap);
            gl.glTexParameteri(GLenum.GL_TEXTURE_2D, GLenum.GL_TEXTURE_MIN_FILTER, GLenum.GL_LINEAR);
            gl.glTexParameteri(GLenum.GL_TEXTURE_2D, GLenum.GL_TEXTURE_MAG_FILTER, GLenum.GL_LINEAR);
            gl.glTexParameteri(GLenum.GL_TEXTURE_2D,GLenum.GL_TEXTURE_WRAP_S, GLenum.GL_REPEAT);
            gl.glTexParameteri(GLenum.GL_TEXTURE_2D,GLenum.GL_TEXTURE_WRAP_T, GLenum.GL_REPEAT);
            gl.glUniform1i(m_d3d._GL2.m_FoamGenTextureBindLocation_EnergyMap, m_d3d._GL2.m_FoamGenTextureUnit_EnergyMap); 

            // Render state
            gl.glColorMask(false, false, false, true);
            gl.glDisable(GLenum.GL_DEPTH_TEST);
            gl.glDisable(GLenum.GL_BLEND);
            gl.glDisable(GLenum.GL_STENCIL_TEST);
            gl.glDisable(GLenum.GL_CULL_FACE);

            // Draw
            hr = cascade_states[cascade].m_pQuadMesh.Draw(NVWaveWorks_Mesh.PrimitiveType.PT_TriangleStrip, 0, 0, 4, 0, 2, foamGenAttribLocations);

            // Enabling writing to all color components of RT
            gl.glColorMask(true, true, true, true);
            gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, 0);

            // building mipmaps for gradient texture if gradient texture arrays are not used
            if(m_params.use_texture_arrays == false)
            {
                gl.glBindTexture(GLenum.GL_TEXTURE_2D, cascade_states[cascade].m_d3d._GL2.m_GL2GradientMap[m_active_GPU_slot]);
                gl.glGenerateMipmap(GLenum.GL_TEXTURE_2D);
            }
            else
            {
                // if texture arrays are used, then mipmaps will be generated for the gradient texture array after blitting to it
            }
            cascade_states[cascade].m_gradient_map_version = cascade_states[cascade].m_pFFTSimulation.getDisplacementMapVersion();

        }

        return HRESULT.S_OK;
    }

    final vs_attr_cbuffer pVSDSCB = new vs_attr_cbuffer();
    final ps_attr_cbuffer pPSCB = new ps_attr_cbuffer();
    private HRESULT setRenderStateD3D11(//	ID3D11DeviceContext* pDC,
									Matrix4f matView,
									int[] pShaderInputRegisterMappings,
                                    GFSDK_WaveWorks_Savestate pSavestateImpl){
        HRESULT hr = HRESULT.S_OK;
        /*
        const UINT rm_vs_buffer = pShaderInputRegisterMappings[ShaderInputD3D11_vs_buffer];
        const UINT rm_vs_g_samplerDisplacementMap0 = pShaderInputRegisterMappings[ShaderInputD3D11_vs_g_samplerDisplacementMap0];
        const UINT rm_vs_g_samplerDisplacementMap1 = pShaderInputRegisterMappings[ShaderInputD3D11_vs_g_samplerDisplacementMap1];
        const UINT rm_vs_g_samplerDisplacementMap2 = pShaderInputRegisterMappings[ShaderInputD3D11_vs_g_samplerDisplacementMap2];
        const UINT rm_vs_g_samplerDisplacementMap3 = pShaderInputRegisterMappings[ShaderInputD3D11_vs_g_samplerDisplacementMap3];
        const UINT rm_vs_g_textureDisplacementMap0 = pShaderInputRegisterMappings[ShaderInputD3D11_vs_g_textureDisplacementMap0];
        const UINT rm_vs_g_textureDisplacementMap1 = pShaderInputRegisterMappings[ShaderInputD3D11_vs_g_textureDisplacementMap1];
        const UINT rm_vs_g_textureDisplacementMap2 = pShaderInputRegisterMappings[ShaderInputD3D11_vs_g_textureDisplacementMap2];
        const UINT rm_vs_g_textureDisplacementMap3 = pShaderInputRegisterMappings[ShaderInputD3D11_vs_g_textureDisplacementMap3];
        const UINT rm_ds_buffer = pShaderInputRegisterMappings[ShaderInputD3D11_ds_buffer];
        const UINT rm_ds_g_samplerDisplacementMap0 = pShaderInputRegisterMappings[ShaderInputD3D11_ds_g_samplerDisplacementMap0];
        const UINT rm_ds_g_samplerDisplacementMap1 = pShaderInputRegisterMappings[ShaderInputD3D11_ds_g_samplerDisplacementMap1];
        const UINT rm_ds_g_samplerDisplacementMap2 = pShaderInputRegisterMappings[ShaderInputD3D11_ds_g_samplerDisplacementMap2];
        const UINT rm_ds_g_samplerDisplacementMap3 = pShaderInputRegisterMappings[ShaderInputD3D11_ds_g_samplerDisplacementMap3];
        const UINT rm_ds_g_textureDisplacementMap0 = pShaderInputRegisterMappings[ShaderInputD3D11_ds_g_textureDisplacementMap0];
        const UINT rm_ds_g_textureDisplacementMap1 = pShaderInputRegisterMappings[ShaderInputD3D11_ds_g_textureDisplacementMap1];
        const UINT rm_ds_g_textureDisplacementMap2 = pShaderInputRegisterMappings[ShaderInputD3D11_ds_g_textureDisplacementMap2];
        const UINT rm_ds_g_textureDisplacementMap3 = pShaderInputRegisterMappings[ShaderInputD3D11_ds_g_textureDisplacementMap3];
        const UINT rm_ps_buffer = pShaderInputRegisterMappings[ShaderInputD3D11_ps_buffer];
        const UINT rm_g_samplerGradientMap0 = pShaderInputRegisterMappings[ShaderInputD3D11_g_samplerGradientMap0];
        const UINT rm_g_samplerGradientMap1 = pShaderInputRegisterMappings[ShaderInputD3D11_g_samplerGradientMap1];
        const UINT rm_g_samplerGradientMap2 = pShaderInputRegisterMappings[ShaderInputD3D11_g_samplerGradientMap2];
        const UINT rm_g_samplerGradientMap3 = pShaderInputRegisterMappings[ShaderInputD3D11_g_samplerGradientMap3];
        const UINT rm_g_textureGradientMap0 = pShaderInputRegisterMappings[ShaderInputD3D11_g_textureGradientMap0];
        const UINT rm_g_textureGradientMap1 = pShaderInputRegisterMappings[ShaderInputD3D11_g_textureGradientMap1];
        const UINT rm_g_textureGradientMap2 = pShaderInputRegisterMappings[ShaderInputD3D11_g_textureGradientMap2];
        const UINT rm_g_textureGradientMap3 = pShaderInputRegisterMappings[ShaderInputD3D11_g_textureGradientMap3];

        // Preserve state as necessary
        if(pSavestateImpl)
        {
            // Samplers/textures

            if(rm_vs_g_samplerDisplacementMap0 != nvrm_unused)
                V_RETURN(pSavestateImpl->PreserveD3D11VertexShaderSampler(pDC, rm_vs_g_samplerDisplacementMap0));
            if(rm_vs_g_samplerDisplacementMap1 != nvrm_unused)
                V_RETURN(pSavestateImpl->PreserveD3D11VertexShaderSampler(pDC, rm_vs_g_samplerDisplacementMap1));
            if(rm_vs_g_samplerDisplacementMap2 != nvrm_unused)
                V_RETURN(pSavestateImpl->PreserveD3D11VertexShaderSampler(pDC, rm_vs_g_samplerDisplacementMap2));
            if(rm_vs_g_samplerDisplacementMap3 != nvrm_unused)
                V_RETURN(pSavestateImpl->PreserveD3D11VertexShaderSampler(pDC, rm_vs_g_samplerDisplacementMap3));

            if(rm_vs_g_textureDisplacementMap0 != nvrm_unused)
                V_RETURN(pSavestateImpl->PreserveD3D11VertexShaderResource(pDC, rm_vs_g_textureDisplacementMap0));
            if(rm_vs_g_textureDisplacementMap1 != nvrm_unused)
                V_RETURN(pSavestateImpl->PreserveD3D11VertexShaderResource(pDC, rm_vs_g_textureDisplacementMap1));
            if(rm_vs_g_textureDisplacementMap2 != nvrm_unused)
                V_RETURN(pSavestateImpl->PreserveD3D11VertexShaderResource(pDC, rm_vs_g_textureDisplacementMap2));
            if(rm_vs_g_textureDisplacementMap3 != nvrm_unused)
                V_RETURN(pSavestateImpl->PreserveD3D11VertexShaderResource(pDC, rm_vs_g_textureDisplacementMap3));

            if(rm_ds_g_samplerDisplacementMap0 != nvrm_unused)
                V_RETURN(pSavestateImpl->PreserveD3D11DomainShaderSampler(pDC, rm_ds_g_samplerDisplacementMap0));
            if(rm_ds_g_samplerDisplacementMap1 != nvrm_unused)
                V_RETURN(pSavestateImpl->PreserveD3D11DomainShaderSampler(pDC, rm_ds_g_samplerDisplacementMap1));
            if(rm_ds_g_samplerDisplacementMap2 != nvrm_unused)
                V_RETURN(pSavestateImpl->PreserveD3D11DomainShaderSampler(pDC, rm_ds_g_samplerDisplacementMap2));
            if(rm_ds_g_samplerDisplacementMap3 != nvrm_unused)
                V_RETURN(pSavestateImpl->PreserveD3D11DomainShaderSampler(pDC, rm_ds_g_samplerDisplacementMap3));

            if(rm_ds_g_textureDisplacementMap0 != nvrm_unused)
                V_RETURN(pSavestateImpl->PreserveD3D11DomainShaderResource(pDC, rm_ds_g_textureDisplacementMap0));
            if(rm_ds_g_textureDisplacementMap1 != nvrm_unused)
                V_RETURN(pSavestateImpl->PreserveD3D11DomainShaderResource(pDC, rm_ds_g_textureDisplacementMap1));
            if(rm_ds_g_textureDisplacementMap2 != nvrm_unused)
                V_RETURN(pSavestateImpl->PreserveD3D11DomainShaderResource(pDC, rm_ds_g_textureDisplacementMap2));
            if(rm_ds_g_textureDisplacementMap3 != nvrm_unused)
                V_RETURN(pSavestateImpl->PreserveD3D11DomainShaderResource(pDC, rm_ds_g_textureDisplacementMap3));

            if(rm_g_samplerGradientMap0 != nvrm_unused)
                V_RETURN(pSavestateImpl->PreserveD3D11PixelShaderSampler(pDC, rm_g_samplerGradientMap0));
            if(rm_g_samplerGradientMap1 != nvrm_unused)
                V_RETURN(pSavestateImpl->PreserveD3D11PixelShaderSampler(pDC, rm_g_samplerGradientMap1));
            if(rm_g_samplerGradientMap2 != nvrm_unused)
                V_RETURN(pSavestateImpl->PreserveD3D11PixelShaderSampler(pDC, rm_g_samplerGradientMap2));
            if(rm_g_samplerGradientMap3 != nvrm_unused)
                V_RETURN(pSavestateImpl->PreserveD3D11PixelShaderSampler(pDC, rm_g_samplerGradientMap3));

            if(rm_g_textureGradientMap0 != nvrm_unused)
                V_RETURN(pSavestateImpl->PreserveD3D11PixelShaderResource(pDC, rm_g_textureGradientMap0));
            if(rm_g_textureGradientMap1 != nvrm_unused)
                V_RETURN(pSavestateImpl->PreserveD3D11PixelShaderResource(pDC, rm_g_textureGradientMap1));
            if(rm_g_textureGradientMap2 != nvrm_unused)
                V_RETURN(pSavestateImpl->PreserveD3D11PixelShaderResource(pDC, rm_g_textureGradientMap2));
            if(rm_g_textureGradientMap3 != nvrm_unused)
                V_RETURN(pSavestateImpl->PreserveD3D11PixelShaderResource(pDC, rm_g_textureGradientMap3));

            // Constants
            if(rm_vs_buffer != nvrm_unused)
                V_RETURN(pSavestateImpl->PreserveD3D11VertexShaderConstantBuffer(pDC, rm_vs_buffer));
            if(rm_ds_buffer != nvrm_unused)
                V_RETURN(pSavestateImpl->PreserveD3D11DomainShaderConstantBuffer(pDC, rm_ds_buffer));
            if(rm_ps_buffer != nvrm_unused)
                V_RETURN(pSavestateImpl->PreserveD3D11PixelShaderConstantBuffer(pDC, rm_ps_buffer));
        }

        // Vertex textures/samplers
        if(rm_vs_g_samplerDisplacementMap0 != nvrm_unused)
            pDC->VSSetSamplers(rm_vs_g_samplerDisplacementMap0, 1, &m_d3d._11.m_pd3d11LinearNoMipSampler);
        if(rm_vs_g_samplerDisplacementMap1 != nvrm_unused)
            pDC->VSSetSamplers(rm_vs_g_samplerDisplacementMap1, 1, &m_d3d._11.m_pd3d11LinearNoMipSampler);
        if(rm_vs_g_samplerDisplacementMap2 != nvrm_unused)
            pDC->VSSetSamplers(rm_vs_g_samplerDisplacementMap2, 1, &m_d3d._11.m_pd3d11LinearNoMipSampler);
        if(rm_vs_g_samplerDisplacementMap3 != nvrm_unused)
            pDC->VSSetSamplers(rm_vs_g_samplerDisplacementMap3, 1, &m_d3d._11.m_pd3d11LinearNoMipSampler);
        //
        if(rm_vs_g_textureDisplacementMap0 != nvrm_unused)
            pDC->VSSetShaderResources(rm_vs_g_textureDisplacementMap0, 1, cascade_states[0].m_pFFTSimulation->GetDisplacementMapD3D11());
        if(rm_vs_g_textureDisplacementMap1 != nvrm_unused)
            pDC->VSSetShaderResources(rm_vs_g_textureDisplacementMap1, 1, cascade_states[1].m_pFFTSimulation->GetDisplacementMapD3D11());
        if(rm_vs_g_textureDisplacementMap2 != nvrm_unused)
            pDC->VSSetShaderResources(rm_vs_g_textureDisplacementMap2, 1, cascade_states[2].m_pFFTSimulation->GetDisplacementMapD3D11());
        if(rm_vs_g_textureDisplacementMap3 != nvrm_unused)
            pDC->VSSetShaderResources(rm_vs_g_textureDisplacementMap3, 1, cascade_states[3].m_pFFTSimulation->GetDisplacementMapD3D11());

        // Domain textures/samplers
        if(rm_ds_g_samplerDisplacementMap0 != nvrm_unused)
            pDC->DSSetSamplers(rm_ds_g_samplerDisplacementMap0, 1, &m_d3d._11.m_pd3d11LinearNoMipSampler);
        if(rm_ds_g_samplerDisplacementMap1 != nvrm_unused)
            pDC->DSSetSamplers(rm_ds_g_samplerDisplacementMap1, 1, &m_d3d._11.m_pd3d11LinearNoMipSampler);
        if(rm_ds_g_samplerDisplacementMap2 != nvrm_unused)
            pDC->DSSetSamplers(rm_ds_g_samplerDisplacementMap2, 1, &m_d3d._11.m_pd3d11LinearNoMipSampler);
        if(rm_ds_g_samplerDisplacementMap3 != nvrm_unused)
            pDC->DSSetSamplers(rm_ds_g_samplerDisplacementMap3, 1, &m_d3d._11.m_pd3d11LinearNoMipSampler);
        //
        if(rm_ds_g_textureDisplacementMap0 != nvrm_unused)
            pDC->DSSetShaderResources(rm_ds_g_textureDisplacementMap0, 1, cascade_states[0].m_pFFTSimulation->GetDisplacementMapD3D11());
        if(rm_ds_g_textureDisplacementMap1 != nvrm_unused)
            pDC->DSSetShaderResources(rm_ds_g_textureDisplacementMap1, 1, cascade_states[1].m_pFFTSimulation->GetDisplacementMapD3D11());
        if(rm_ds_g_textureDisplacementMap2 != nvrm_unused)
            pDC->DSSetShaderResources(rm_ds_g_textureDisplacementMap2, 1, cascade_states[2].m_pFFTSimulation->GetDisplacementMapD3D11());
        if(rm_ds_g_textureDisplacementMap3 != nvrm_unused)
            pDC->DSSetShaderResources(rm_ds_g_textureDisplacementMap3, 1, cascade_states[3].m_pFFTSimulation->GetDisplacementMapD3D11());

        // Pixel textures/samplers
        if(rm_g_samplerGradientMap0 != nvrm_unused)
            pDC->PSSetSamplers(rm_g_samplerGradientMap0, 1, &m_d3d._11.m_pd3d11GradMapSampler);
        if(rm_g_samplerGradientMap1 != nvrm_unused)
            pDC->PSSetSamplers(rm_g_samplerGradientMap1, 1, &m_d3d._11.m_pd3d11GradMapSampler);
        if(rm_g_samplerGradientMap2 != nvrm_unused)
            pDC->PSSetSamplers(rm_g_samplerGradientMap2, 1, &m_d3d._11.m_pd3d11GradMapSampler);
        if(rm_g_samplerGradientMap3 != nvrm_unused)
            pDC->PSSetSamplers(rm_g_samplerGradientMap3, 1, &m_d3d._11.m_pd3d11GradMapSampler);
        //
        if(rm_g_textureGradientMap0 != nvrm_unused)
            pDC->PSSetShaderResources(rm_g_textureGradientMap0, 1, &cascade_states[0].m_d3d._11.m_pd3d11GradientMap[m_active_GPU_slot]);
        if(rm_g_textureGradientMap1 != nvrm_unused)
            pDC->PSSetShaderResources(rm_g_textureGradientMap1, 1, &cascade_states[1].m_d3d._11.m_pd3d11GradientMap[m_active_GPU_slot]);
        if(rm_g_textureGradientMap2 != nvrm_unused)
            pDC->PSSetShaderResources(rm_g_textureGradientMap2, 1, &cascade_states[2].m_d3d._11.m_pd3d11GradientMap[m_active_GPU_slot]);
        if(rm_g_textureGradientMap3 != nvrm_unused)
            pDC->PSSetShaderResources(rm_g_textureGradientMap3, 1, &cascade_states[3].m_d3d._11.m_pd3d11GradientMap[m_active_GPU_slot]);
        */

        // Constants
//        vs_ds_attr_cbuffer* pVSDSCB = NULL;
//        if(rm_ds_buffer != nvrm_unused || rm_vs_buffer != nvrm_unused)
        {
//            pVSDSCB = &VSDSCB;

            pVSDSCB.g_UVScaleCascade0123[0] = 1.0f / m_params.cascades[0].fft_period;
            pVSDSCB.g_UVScaleCascade0123[1] = 1.0f / m_params.cascades[1].fft_period;
            pVSDSCB.g_UVScaleCascade0123[2] = 1.0f / m_params.cascades[2].fft_period;
            pVSDSCB.g_UVScaleCascade0123[3] = 1.0f / m_params.cascades[3].fft_period;
            Matrix4f.decompseRigidMatrix(matView, pVSDSCB.g_WorldEye, null,null);

//            gfsdk_float4x4 inv_mat_view;
//            gfsdk_float4 vec_original = {0,0,0,1};
//            gfsdk_float4 vec_transformed;
//            mat4Inverse(inv_mat_view,matView);
//            vec4Mat4Mul(vec_transformed, vec_original, inv_mat_view);
//            gfsdk_float4 vGlobalEye = vec_transformed;
//
//            pVSDSCB.g_WorldEye[0] = vGlobalEye.x;
//            pVSDSCB->g_WorldEye[1] = vGlobalEye.y;
//            pVSDSCB->g_WorldEye[2] = vGlobalEye.z;

            ByteBuffer bytes = CacheBuffer.getCachedByteBuffer(vs_attr_cbuffer.SIZE);
            pVSDSCB.store(bytes).flip();
            UpdateSubresource(GLenum.GL_UNIFORM_BUFFER, m_d3d._11.m_pd3d11VertexDomainShaderCB, bytes);
            gl.glBindBufferBase(GLenum.GL_UNIFORM_BUFFER, 1, m_d3d._11.m_pd3d11VertexDomainShaderCB);
        }

//        ps_attr_cbuffer PSCB;
        final float texel_len = m_params.cascades[0].fft_period / m_params.cascades[0].fft_resolution;
        final float cascade1Scale = m_params.cascades[0].fft_period/m_params.cascades[1].fft_period;
        final float cascade1UVOffset = 0.f; // half-pixel not required in D3D11
        final float cascade2Scale = m_params.cascades[0].fft_period/m_params.cascades[2].fft_period;
        final float cascade2UVOffset = 0.f; // half-pixel not required in D3D11
        final float cascade3Scale = m_params.cascades[0].fft_period/m_params.cascades[3].fft_period;
        final float cascade3UVOffset = 0.f; // half-pixel not required in D3D11

//        if(rm_ps_buffer != nvrm_unused)
        {
//            pPSCB = &PSCB;
            pPSCB.g_TexelLength_x2_PS = texel_len;
        }

//        if(NULL != pPSCB)
        {
            pPSCB.g_Cascade1Scale_PS = cascade1Scale;
            pPSCB.g_Cascade1UVOffset_PS = cascade1UVOffset;
            pPSCB.g_Cascade2Scale_PS = cascade2Scale;
            pPSCB.g_Cascade2UVOffset_PS = cascade2UVOffset;
            pPSCB.g_Cascade3Scale_PS = cascade3Scale;
            pPSCB.g_Cascade3UVOffset_PS = cascade3UVOffset;
            pPSCB.g_Cascade1TexelScale_PS = (m_params.cascades[0].fft_period * m_params.cascades[1].fft_resolution) / (m_params.cascades[1].fft_period * m_params.cascades[0].fft_resolution);
            pPSCB.g_Cascade2TexelScale_PS = (m_params.cascades[0].fft_period * m_params.cascades[2].fft_resolution) / (m_params.cascades[2].fft_period * m_params.cascades[0].fft_resolution);
            pPSCB.g_Cascade3TexelScale_PS = (m_params.cascades[0].fft_period * m_params.cascades[3].fft_resolution) / (m_params.cascades[3].fft_period * m_params.cascades[0].fft_resolution);

            ByteBuffer bytes = CacheBuffer.getCachedByteBuffer(ps_attr_cbuffer.SIZE);
            pPSCB.store(bytes).flip();
            UpdateSubresource(GLenum.GL_UNIFORM_BUFFER, m_d3d._11.m_pd3d11PixelShaderCB, bytes);
            gl.glBindBufferBase(GLenum.GL_UNIFORM_BUFFER, 4, m_d3d._11.m_pd3d11PixelShaderCB);
        }

//        if(pVSDSCB)
//        {
//            {
//                D3D11_CB_Updater<vs_ds_attr_cbuffer> cb(pDC,m_d3d._11.m_pd3d11VertexDomainShaderCB);
//                cb.cb() = *pVSDSCB;
//            }
//            if(rm_vs_buffer != nvrm_unused)
//                pDC->VSSetConstantBuffers(rm_vs_buffer, 1, &m_d3d._11.m_pd3d11VertexDomainShaderCB);
//            if(rm_ds_buffer != nvrm_unused)
//                pDC->DSSetConstantBuffers(rm_ds_buffer, 1, &m_d3d._11.m_pd3d11VertexDomainShaderCB);
//        }
//        if(pPSCB)
//        {
//            {
//                D3D11_CB_Updater<ps_attr_cbuffer> cb(pDC,m_d3d._11.m_pd3d11PixelShaderCB);
//                cb.cb() = *pPSCB;
//            }
//            pDC->PSSetConstantBuffers(rm_ps_buffer, 1, &m_d3d._11.m_pd3d11PixelShaderCB);
//        }

        bindRenderTexturesD3D11();
        return HRESULT.S_OK;
    }
    private HRESULT setRenderStateGnm(//		sce::Gnmx::LightweightGfxContext* gfxContext,
									Matrix4f matView,
									int[] pShaderInputRegisterMappings,
                                      GFSDK_WaveWorks_Savestate pSavestateImpl
    ){ return HRESULT.S_FALSE;}

    private HRESULT setRenderStateGL2(		Matrix4f matView,
									int[] pShaderInputRegisterMappings, GFSDK_WaveWorks_Simulation_GL_Pool glPool
    ){

        final int rm_g_WorldEye = pShaderInputRegisterMappings[ShaderInputGL2_g_WorldEye];
        final int rm_g_UseTextureArrays = pShaderInputRegisterMappings[ShaderInputGL2_g_UseTextureArrays];
        final int rm_g_UVScaleCascade0123 = pShaderInputRegisterMappings[ShaderInputGL2_g_UVScaleCascade0123];
        final int rm_g_TexelLength_x2_PS = pShaderInputRegisterMappings[ShaderInputGL2_g_TexelLength_x2_PS];
        final int rm_g_Cascade1Scale_PS = pShaderInputRegisterMappings[ShaderInputGL2_g_Cascade1Scale_PS];
        final int rm_g_Cascade1TexelScale_PS = pShaderInputRegisterMappings[ShaderInputGL2_g_Cascade1TexelScale_PS];
        final int rm_g_Cascade1UVOffset_PS = pShaderInputRegisterMappings[ShaderInputGL2_g_Cascade1UVOffset_PS];
        final int rm_g_Cascade2Scale_PS = pShaderInputRegisterMappings[ShaderInputGL2_g_Cascade2Scale_PS];
        final int rm_g_Cascade2TexelScale_PS = pShaderInputRegisterMappings[ShaderInputGL2_g_Cascade2TexelScale_PS];
        final int rm_g_Cascade2UVOffset_PS = pShaderInputRegisterMappings[ShaderInputGL2_g_Cascade2UVOffset_PS];
        final int rm_g_Cascade3Scale_PS = pShaderInputRegisterMappings[ShaderInputGL2_g_Cascade3Scale_PS];
        final int rm_g_Cascade3TexelScale_PS = pShaderInputRegisterMappings[ShaderInputGL2_g_Cascade3TexelScale_PS];
        final int rm_g_Cascade3UVOffset_PS = pShaderInputRegisterMappings[ShaderInputGL2_g_Cascade3UVOffset_PS];

        int tu_GradientMapTextureArray = 0;

        if(m_params.use_texture_arrays)
        {
            tu_GradientMapTextureArray = glPool.Reserved_Texture_Units[1];
        }

        if(m_params.use_texture_arrays)
        {
            int N = m_params.cascades[GFSDK_WaveWorks_Detailed_Simulation_Params.MaxNumCascades-1].fft_resolution;

            // assembling the displacement textures to texture array
            // glBlitFramebuffer does upscale for cascades with smaller fft_resolution
            gl.glBindFramebuffer(GLenum.GL_READ_FRAMEBUFFER, m_d3d._GL2.m_TextureArraysBlittingReadFBO);
            gl.glBindFramebuffer(GLenum.GL_DRAW_FRAMEBUFFER, m_d3d._GL2.m_TextureArraysBlittingDrawFBO);
            gl.glReadBuffer(GLenum.GL_COLOR_ATTACHMENT0);
            final int bufs = GLenum.GL_COLOR_ATTACHMENT0;
            gl.glDrawBuffers(bufs);
            for(int i = 0; i < GFSDK_WaveWorks_Detailed_Simulation_Params.MaxNumCascades; i++)
            {

                gl.glFramebufferTexture2D(GLenum.GL_READ_FRAMEBUFFER, GLenum.GL_COLOR_ATTACHMENT0, GLenum.GL_TEXTURE_2D, cascade_states[i].m_pFFTSimulation.GetDisplacementMapGL2(), 0);
                gl.glFramebufferTextureLayer(GLenum.GL_DRAW_FRAMEBUFFER, GLenum.GL_COLOR_ATTACHMENT0, m_d3d._GL2.m_DisplacementsTextureArray, 0, i);
                gl.glBlitFramebuffer(0, 0, m_params.cascades[i].fft_resolution, m_params.cascades[i].fft_resolution, 0, 0, N, N, GLenum.GL_COLOR_BUFFER_BIT, GLenum.GL_LINEAR);
            }

            // assembling the gradient textures to texture array
            for(int i = 0; i < GFSDK_WaveWorks_Detailed_Simulation_Params.MaxNumCascades; i++)
            {

                gl.glFramebufferTexture2D(GLenum.GL_READ_FRAMEBUFFER, GLenum.GL_COLOR_ATTACHMENT0, GLenum.GL_TEXTURE_2D, cascade_states[i].m_d3d._GL2.m_GL2GradientMap[m_active_GPU_slot], 0);
                gl.glFramebufferTextureLayer(GLenum.GL_DRAW_FRAMEBUFFER, GLenum.GL_COLOR_ATTACHMENT0, m_d3d._GL2.m_GradientsTextureArray, 0, i);
                gl.glBlitFramebuffer(0, 0, m_params.cascades[i].fft_resolution, m_params.cascades[i].fft_resolution, 0, 0, N, N, GLenum.GL_COLOR_BUFFER_BIT, GLenum.GL_LINEAR);
            }
            gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, 0);

            // generating mipmaps for gradient texture array, using gradient texture array texture unit
            gl.glActiveTexture(GLenum.GL_TEXTURE0 + tu_GradientMapTextureArray);
            for(int i=0; i<GFSDK_WaveWorks_Detailed_Simulation_Params.MaxNumCascades;i++)
            {
                gl.glBindTexture(GLenum.GL_TEXTURE_2D_ARRAY, m_d3d._GL2.m_GradientsTextureArray);
                gl.glGenerateMipmap(GLenum.GL_TEXTURE_2D_ARRAY);
                gl.glBindTexture(GLenum.GL_TEXTURE_2D_ARRAY,0);
            }
        }

        bindRenderTexturesGL2(pShaderInputRegisterMappings, glPool);

        // Constants
        Vector4f UVScaleCascade0123 = new Vector4f();
        UVScaleCascade0123.x = 1.0f / m_params.cascades[0].fft_period;
        UVScaleCascade0123.y = 1.0f / m_params.cascades[1].fft_period;
        UVScaleCascade0123.z = 1.0f / m_params.cascades[2].fft_period;
        UVScaleCascade0123.w = 1.0f / m_params.cascades[3].fft_period;

//        gfsdk_float4x4 inv_mat_view;
//        gfsdk_float4 vec_original = {0,0,0,1};
//        gfsdk_float4 vec_transformed;
//        mat4Inverse(inv_mat_view,matView);
//        vec4Mat4Mul(vec_transformed, vec_original, inv_mat_view);
//        gfsdk_float4 vGlobalEye = vec_transformed;
        Vector3f vGlobalEye = new Vector3f();
        Matrix4f.decompseRigidMatrix(matView, vGlobalEye, null, null, null);

        final float texel_len = m_params.cascades[0].fft_period / m_params.cascades[0].fft_resolution;
        final float cascade1Scale = m_params.cascades[0].fft_period/m_params.cascades[1].fft_period;
        final float cascade1TexelScale = (m_params.cascades[0].fft_period * m_params.cascades[1].fft_resolution) / (m_params.cascades[1].fft_period * m_params.cascades[0].fft_resolution);
        final float cascade1UVOffset = 0;
        final float cascade2Scale = m_params.cascades[0].fft_period/m_params.cascades[2].fft_period;
        final float cascade2TexelScale = (m_params.cascades[0].fft_period * m_params.cascades[2].fft_resolution) / (m_params.cascades[2].fft_period * m_params.cascades[0].fft_resolution);
        final float cascade2UVOffset = 0;
        final float cascade3Scale = m_params.cascades[0].fft_period/m_params.cascades[3].fft_period;
        final float cascade3TexelScale = (m_params.cascades[0].fft_period * m_params.cascades[3].fft_resolution) / (m_params.cascades[3].fft_period * m_params.cascades[0].fft_resolution);
        final float cascade3UVOffset = 0;

        if(rm_g_WorldEye != nvrm_unused)
        {
            gl.glUniform3f(rm_g_WorldEye, vGlobalEye.x, vGlobalEye.y, vGlobalEye.z);
        }
        if(rm_g_UseTextureArrays != nvrm_unused)
        {
            gl.glUniform1f(rm_g_UseTextureArrays, m_params.use_texture_arrays ? 1.0f:0.0f); 
        }
        if(rm_g_UVScaleCascade0123 != nvrm_unused)
        {
            gl.glUniform4f(rm_g_UVScaleCascade0123, UVScaleCascade0123.x, UVScaleCascade0123.y, UVScaleCascade0123.z, UVScaleCascade0123.w);
        }
        if(rm_g_TexelLength_x2_PS != nvrm_unused)
        {
            gl.glUniform1f(rm_g_TexelLength_x2_PS, texel_len); 
        }
        //
        if(rm_g_Cascade1Scale_PS != nvrm_unused)
        {
            gl.glUniform1f(rm_g_Cascade1Scale_PS, cascade1Scale); 
        }
        if(rm_g_Cascade1TexelScale_PS != nvrm_unused)
        {
            gl.glUniform1f(rm_g_Cascade1TexelScale_PS, cascade1TexelScale); 
        }
        if(rm_g_Cascade1UVOffset_PS != nvrm_unused)
        {
            gl.glUniform1f(rm_g_Cascade1UVOffset_PS, cascade1UVOffset); 
        }

        if(rm_g_Cascade2Scale_PS != nvrm_unused)
        {
            gl.glUniform1f(rm_g_Cascade2Scale_PS, cascade2Scale); 
        }
        if(rm_g_Cascade2TexelScale_PS != nvrm_unused)
        {
            gl.glUniform1f(rm_g_Cascade2TexelScale_PS, cascade2TexelScale); 
        }
        if(rm_g_Cascade2UVOffset_PS != nvrm_unused)
        {
            gl.glUniform1f(rm_g_Cascade2UVOffset_PS, cascade2UVOffset); 
        }

        if(rm_g_Cascade3Scale_PS != nvrm_unused)
        {
            gl.glUniform1f(rm_g_Cascade3Scale_PS, cascade3Scale); 
        }
        if(rm_g_Cascade3TexelScale_PS != nvrm_unused)
        {
            gl.glUniform1f(rm_g_Cascade3TexelScale_PS, cascade3TexelScale); 
        }
        if(rm_g_Cascade3UVOffset_PS != nvrm_unused)
        {
            gl.glUniform1f(rm_g_Cascade3UVOffset_PS, cascade3UVOffset); 
        }
        return HRESULT.S_OK;
    }

    private void bindRenderTexturesGL2(int[] pShaderInputRegisterMappings, GFSDK_WaveWorks_Simulation_GL_Pool glPool
                                       ){

        final int rm_g_textureBindLocationDisplacementMap0 = pShaderInputRegisterMappings[ShaderInputGL2_g_textureBindLocationDisplacementMap0];
        final int rm_g_textureBindLocationDisplacementMap1 = pShaderInputRegisterMappings[ShaderInputGL2_g_textureBindLocationDisplacementMap1];
        final int rm_g_textureBindLocationDisplacementMap2 = pShaderInputRegisterMappings[ShaderInputGL2_g_textureBindLocationDisplacementMap2];
        final int rm_g_textureBindLocationDisplacementMap3 = pShaderInputRegisterMappings[ShaderInputGL2_g_textureBindLocationDisplacementMap3];
        final int rm_g_textureBindLocationGradientMap0 = pShaderInputRegisterMappings[ShaderInputGL2_g_textureBindLocationGradientMap0];
        final int rm_g_textureBindLocationGradientMap1 = pShaderInputRegisterMappings[ShaderInputGL2_g_textureBindLocationGradientMap1];
        final int rm_g_textureBindLocationGradientMap2 = pShaderInputRegisterMappings[ShaderInputGL2_g_textureBindLocationGradientMap2];
        final int rm_g_textureBindLocationGradientMap3 = pShaderInputRegisterMappings[ShaderInputGL2_g_textureBindLocationGradientMap3];
        final int rm_g_textureBindLocationDisplacementMapArray = pShaderInputRegisterMappings[ShaderInputGL2_g_textureBindLocationDisplacementMapArray];
        final int rm_g_textureBindLocationGradientMapArray = pShaderInputRegisterMappings[ShaderInputGL2_g_textureBindLocationGradientMapArray];

        int tu_DisplacementMap0 = 0;
        int tu_DisplacementMap1 = 0;
        int tu_DisplacementMap2 = 0;
        int tu_DisplacementMap3 = 0;
        int tu_GradientMap0 = 0;
        int tu_GradientMap1 = 0;
        int tu_GradientMap2 = 0;
        int tu_GradientMap3 = 0;
        int tu_DisplacementMapTextureArray = 0;
        int tu_GradientMapTextureArray = 0;

        if(m_params.use_texture_arrays)
        {
            tu_DisplacementMapTextureArray = glPool.Reserved_Texture_Units[0];
            tu_GradientMapTextureArray = glPool.Reserved_Texture_Units[1];
        }
        else
        {
            tu_DisplacementMap0 = glPool.Reserved_Texture_Units[0];
            tu_DisplacementMap1 = glPool.Reserved_Texture_Units[1];
            tu_DisplacementMap2 = glPool.Reserved_Texture_Units[2];
            tu_DisplacementMap3 = glPool.Reserved_Texture_Units[3];
            tu_GradientMap0 = glPool.Reserved_Texture_Units[4];
            tu_GradientMap1 = glPool.Reserved_Texture_Units[5];
            tu_GradientMap2 = glPool.Reserved_Texture_Units[6];
            tu_GradientMap3 = glPool.Reserved_Texture_Units[7];
        }

        // Textures
        if(m_params.use_texture_arrays)
        {
            if(rm_g_textureBindLocationDisplacementMapArray != nvrm_unused)
            {
                gl.glActiveTexture(GLenum.GL_TEXTURE0 + tu_DisplacementMapTextureArray);
                gl.glBindTexture(GLenum.GL_TEXTURE_2D_ARRAY, m_d3d._GL2.m_DisplacementsTextureArray);

                gl.glTexParameteri(GLenum.GL_TEXTURE_2D_ARRAY, GLenum.GL_TEXTURE_MIN_FILTER, GLenum.GL_LINEAR);
                gl.glTexParameteri(GLenum.GL_TEXTURE_2D_ARRAY, GLenum.GL_TEXTURE_MAG_FILTER, GLenum.GL_LINEAR);
                gl.glTexParameteri(GLenum.GL_TEXTURE_2D_ARRAY,GLenum.GL_TEXTURE_WRAP_S, GLenum.GL_REPEAT);
                gl.glTexParameteri(GLenum.GL_TEXTURE_2D_ARRAY,GLenum.GL_TEXTURE_WRAP_T, GLenum.GL_REPEAT);
                gl.glUniform1i(rm_g_textureBindLocationDisplacementMapArray, tu_DisplacementMapTextureArray);
            }

            if(rm_g_textureBindLocationGradientMapArray != nvrm_unused)
            {
                gl.glActiveTexture(GLenum.GL_TEXTURE0 + tu_GradientMapTextureArray);
                gl.glBindTexture(GLenum.GL_TEXTURE_2D_ARRAY, m_d3d._GL2.m_GradientsTextureArray);
                gl.glTexParameterf(GLenum.GL_TEXTURE_2D_ARRAY, GLenum.GL_TEXTURE_MAX_ANISOTROPY_EXT, m_params.aniso_level);
                gl.glTexParameteri(GLenum.GL_TEXTURE_2D_ARRAY, GLenum.GL_TEXTURE_MIN_FILTER, GLenum.GL_LINEAR_MIPMAP_LINEAR);
                gl.glTexParameteri(GLenum.GL_TEXTURE_2D_ARRAY, GLenum.GL_TEXTURE_MAG_FILTER, GLenum.GL_LINEAR);
                gl.glTexParameteri(GLenum.GL_TEXTURE_2D_ARRAY,GLenum.GL_TEXTURE_WRAP_S, GLenum.GL_REPEAT);
                gl.glTexParameteri(GLenum.GL_TEXTURE_2D_ARRAY,GLenum.GL_TEXTURE_WRAP_T, GLenum.GL_REPEAT);
                gl.glUniform1i(rm_g_textureBindLocationGradientMapArray, tu_GradientMapTextureArray);
            }
        }
        else

        {
            if(rm_g_textureBindLocationDisplacementMap0 != nvrm_unused)
            {
                gl.glActiveTexture(GLenum.GL_TEXTURE0 + tu_DisplacementMap0);
                gl.glBindTexture(GLenum.GL_TEXTURE_2D, cascade_states[0].m_pFFTSimulation.GetDisplacementMapGL2());
                gl.glTexParameteri(GLenum.GL_TEXTURE_2D, GLenum.GL_TEXTURE_MIN_FILTER, GLenum.GL_LINEAR);
                gl.glTexParameteri(GLenum.GL_TEXTURE_2D, GLenum.GL_TEXTURE_MAG_FILTER, GLenum.GL_LINEAR);
                gl.glTexParameteri(GLenum.GL_TEXTURE_2D,GLenum.GL_TEXTURE_WRAP_S, GLenum.GL_REPEAT);
                gl.glTexParameteri(GLenum.GL_TEXTURE_2D,GLenum.GL_TEXTURE_WRAP_T, GLenum.GL_REPEAT);
                gl.glUniform1i(rm_g_textureBindLocationDisplacementMap0, tu_DisplacementMap0);
            }
            if(rm_g_textureBindLocationDisplacementMap1 != nvrm_unused)
            {
                gl.glActiveTexture(GLenum.GL_TEXTURE0 + tu_DisplacementMap1);
                gl.glBindTexture(GLenum.GL_TEXTURE_2D, cascade_states[1].m_pFFTSimulation.GetDisplacementMapGL2());
                gl.glTexParameteri(GLenum.GL_TEXTURE_2D, GLenum.GL_TEXTURE_MIN_FILTER, GLenum.GL_LINEAR);
                gl.glTexParameteri(GLenum.GL_TEXTURE_2D, GLenum.GL_TEXTURE_MAG_FILTER, GLenum.GL_LINEAR);
                gl.glTexParameteri(GLenum.GL_TEXTURE_2D,GLenum.GL_TEXTURE_WRAP_S, GLenum.GL_REPEAT);
                gl.glTexParameteri(GLenum.GL_TEXTURE_2D,GLenum.GL_TEXTURE_WRAP_T, GLenum.GL_REPEAT);
                gl.glUniform1i(rm_g_textureBindLocationDisplacementMap1, tu_DisplacementMap1);
            }
            if(rm_g_textureBindLocationDisplacementMap2 != nvrm_unused)
            {
                gl.glActiveTexture(GLenum.GL_TEXTURE0 + tu_DisplacementMap2);
                gl.glBindTexture(GLenum.GL_TEXTURE_2D, cascade_states[2].m_pFFTSimulation.GetDisplacementMapGL2());
                gl.glTexParameteri(GLenum.GL_TEXTURE_2D, GLenum.GL_TEXTURE_MIN_FILTER, GLenum.GL_LINEAR);
                gl.glTexParameteri(GLenum.GL_TEXTURE_2D, GLenum.GL_TEXTURE_MAG_FILTER, GLenum.GL_LINEAR);
                gl.glTexParameteri(GLenum.GL_TEXTURE_2D,GLenum.GL_TEXTURE_WRAP_S, GLenum.GL_REPEAT);
                gl.glTexParameteri(GLenum.GL_TEXTURE_2D,GLenum.GL_TEXTURE_WRAP_T, GLenum.GL_REPEAT);
                gl.glUniform1i(rm_g_textureBindLocationDisplacementMap2, tu_DisplacementMap2);
            }
            if(rm_g_textureBindLocationDisplacementMap3 != nvrm_unused)
            {
                gl.glActiveTexture(GLenum.GL_TEXTURE0 + tu_DisplacementMap3);
                gl.glBindTexture(GLenum.GL_TEXTURE_2D, cascade_states[3].m_pFFTSimulation.GetDisplacementMapGL2());
                gl.glTexParameteri(GLenum.GL_TEXTURE_2D, GLenum.GL_TEXTURE_MIN_FILTER, GLenum.GL_LINEAR);
                gl.glTexParameteri(GLenum.GL_TEXTURE_2D, GLenum.GL_TEXTURE_MAG_FILTER, GLenum.GL_LINEAR);
                gl.glTexParameteri(GLenum.GL_TEXTURE_2D,GLenum.GL_TEXTURE_WRAP_S, GLenum.GL_REPEAT);
                gl.glTexParameteri(GLenum.GL_TEXTURE_2D,GLenum.GL_TEXTURE_WRAP_T, GLenum.GL_REPEAT);
                gl.glUniform1i(rm_g_textureBindLocationDisplacementMap3, tu_DisplacementMap3);
            }
            //
            if(rm_g_textureBindLocationGradientMap0 != nvrm_unused)
            {
                gl.glActiveTexture(GLenum.GL_TEXTURE0 + tu_GradientMap0);
                gl.glBindTexture(GLenum.GL_TEXTURE_2D, cascade_states[0].m_d3d._GL2.m_GL2GradientMap[m_active_GPU_slot]);
                gl.glTexParameterf(GLenum.GL_TEXTURE_2D, GLenum.GL_TEXTURE_MAX_ANISOTROPY_EXT, m_params.aniso_level);
                gl.glTexParameteri(GLenum.GL_TEXTURE_2D, GLenum.GL_TEXTURE_MIN_FILTER, GLenum.GL_LINEAR_MIPMAP_LINEAR);
                gl.glTexParameteri(GLenum.GL_TEXTURE_2D, GLenum.GL_TEXTURE_MAG_FILTER, GLenum.GL_LINEAR);
                gl.glTexParameteri(GLenum.GL_TEXTURE_2D,GLenum.GL_TEXTURE_WRAP_S, GLenum.GL_REPEAT);
                gl.glTexParameteri(GLenum.GL_TEXTURE_2D,GLenum.GL_TEXTURE_WRAP_T, GLenum.GL_REPEAT);
                gl.glUniform1i(rm_g_textureBindLocationGradientMap0, tu_GradientMap0);
            }
            if(rm_g_textureBindLocationGradientMap1 != nvrm_unused)
            {
                gl.glActiveTexture(GLenum.GL_TEXTURE0 + tu_GradientMap1);
                gl.glBindTexture(GLenum.GL_TEXTURE_2D, cascade_states[1].m_d3d._GL2.m_GL2GradientMap[m_active_GPU_slot]);
                gl.glTexParameterf(GLenum.GL_TEXTURE_2D, GLenum.GL_TEXTURE_MAX_ANISOTROPY_EXT, m_params.aniso_level);
                gl.glTexParameteri(GLenum.GL_TEXTURE_2D, GLenum.GL_TEXTURE_MIN_FILTER, GLenum.GL_LINEAR_MIPMAP_LINEAR);
                gl.glTexParameteri(GLenum.GL_TEXTURE_2D, GLenum.GL_TEXTURE_MAG_FILTER, GLenum.GL_LINEAR);
                gl.glTexParameteri(GLenum.GL_TEXTURE_2D,GLenum.GL_TEXTURE_WRAP_S, GLenum.GL_REPEAT);
                gl.glTexParameteri(GLenum.GL_TEXTURE_2D,GLenum.GL_TEXTURE_WRAP_T, GLenum.GL_REPEAT);
                gl.glUniform1i(rm_g_textureBindLocationGradientMap1, tu_GradientMap1);
            }
            if(rm_g_textureBindLocationGradientMap2 != nvrm_unused)
            {
                gl.glActiveTexture(GLenum.GL_TEXTURE0 + tu_GradientMap2);
                gl.glBindTexture(GLenum.GL_TEXTURE_2D, cascade_states[2].m_d3d._GL2.m_GL2GradientMap[m_active_GPU_slot]);
                gl.glTexParameterf(GLenum.GL_TEXTURE_2D, GLenum.GL_TEXTURE_MAX_ANISOTROPY_EXT, m_params.aniso_level);
                gl.glTexParameteri(GLenum.GL_TEXTURE_2D, GLenum.GL_TEXTURE_MIN_FILTER, GLenum.GL_LINEAR_MIPMAP_LINEAR);
                gl.glTexParameteri(GLenum.GL_TEXTURE_2D, GLenum.GL_TEXTURE_MAG_FILTER, GLenum.GL_LINEAR);
                gl.glTexParameteri(GLenum.GL_TEXTURE_2D,GLenum.GL_TEXTURE_WRAP_S, GLenum.GL_REPEAT);
                gl.glTexParameteri(GLenum.GL_TEXTURE_2D,GLenum.GL_TEXTURE_WRAP_T, GLenum.GL_REPEAT);
                gl.glUniform1i(rm_g_textureBindLocationGradientMap2, tu_GradientMap2);
            }
            if(rm_g_textureBindLocationGradientMap3 != nvrm_unused)
            {
                gl.glActiveTexture(GLenum.GL_TEXTURE0 + tu_GradientMap3);
                gl.glBindTexture(GLenum.GL_TEXTURE_2D, cascade_states[3].m_d3d._GL2.m_GL2GradientMap[m_active_GPU_slot]);
                gl.glTexParameterf(GLenum.GL_TEXTURE_2D, GLenum.GL_TEXTURE_MAX_ANISOTROPY_EXT, m_params.aniso_level);
                gl.glTexParameteri(GLenum.GL_TEXTURE_2D, GLenum.GL_TEXTURE_MIN_FILTER, GLenum.GL_LINEAR_MIPMAP_LINEAR);
                gl.glTexParameteri(GLenum.GL_TEXTURE_2D, GLenum.GL_TEXTURE_MAG_FILTER, GLenum.GL_LINEAR);
                gl.glTexParameteri(GLenum.GL_TEXTURE_2D,GLenum.GL_TEXTURE_WRAP_S, GLenum.GL_REPEAT);
                gl.glTexParameteri(GLenum.GL_TEXTURE_2D,GLenum.GL_TEXTURE_WRAP_T, GLenum.GL_REPEAT);
                gl.glUniform1i(rm_g_textureBindLocationGradientMap3, tu_GradientMap3);
            }
        }
    }

    private void bindRenderTexturesD3D11(){
        int tu_DisplacementMap0 = 0;
        int tu_DisplacementMap1 = 0;
        int tu_DisplacementMap2 = 0;
        int tu_DisplacementMap3 = 0;
        int tu_GradientMap0 = 0;
        int tu_GradientMap1 = 0;
        int tu_GradientMap2 = 0;
        int tu_GradientMap3 = 0;
        int tu_DisplacementMapTextureArray = 0;
        int tu_GradientMapTextureArray = 0;

        if(m_params.use_texture_arrays)
        {
            tu_DisplacementMapTextureArray = 0;
            tu_GradientMapTextureArray = 1;
            throw new IllegalStateException();
        }
        else
        {
            tu_DisplacementMap0 = 0;
            tu_DisplacementMap1 = 1;
            tu_DisplacementMap2 = 2;
            tu_DisplacementMap3 = 3;
            tu_GradientMap0 = 4;
            tu_GradientMap1 = 5;
            tu_GradientMap2 = 6;
            tu_GradientMap3 = 7;
        }

        // Textures
        if(m_params.use_texture_arrays)
        {
//            if(rm_g_textureBindLocationDisplacementMapArray != nvrm_unused)
            {
                gl.glActiveTexture(GLenum.GL_TEXTURE0 + tu_DisplacementMapTextureArray);
                gl.glBindTexture(GLenum.GL_TEXTURE_2D_ARRAY, m_d3d._GL2.m_DisplacementsTextureArray);

                gl.glTexParameteri(GLenum.GL_TEXTURE_2D_ARRAY, GLenum.GL_TEXTURE_MIN_FILTER, GLenum.GL_LINEAR);
                gl.glTexParameteri(GLenum.GL_TEXTURE_2D_ARRAY, GLenum.GL_TEXTURE_MAG_FILTER, GLenum.GL_LINEAR);
                gl.glTexParameteri(GLenum.GL_TEXTURE_2D_ARRAY,GLenum.GL_TEXTURE_WRAP_S, GLenum.GL_REPEAT);
                gl.glTexParameteri(GLenum.GL_TEXTURE_2D_ARRAY,GLenum.GL_TEXTURE_WRAP_T, GLenum.GL_REPEAT);
                gl.glBindSampler(tu_DisplacementMapTextureArray, 0);
            }

//            if(rm_g_textureBindLocationGradientMapArray != nvrm_unused)
            {
                gl.glActiveTexture(GLenum.GL_TEXTURE0 + tu_GradientMapTextureArray);
                gl.glBindTexture(GLenum.GL_TEXTURE_2D_ARRAY, m_d3d._GL2.m_GradientsTextureArray);
                gl.glTexParameterf(GLenum.GL_TEXTURE_2D_ARRAY, GLenum.GL_TEXTURE_MAX_ANISOTROPY_EXT, m_params.aniso_level);
                gl.glTexParameteri(GLenum.GL_TEXTURE_2D_ARRAY, GLenum.GL_TEXTURE_MIN_FILTER, GLenum.GL_LINEAR_MIPMAP_LINEAR);
                gl.glTexParameteri(GLenum.GL_TEXTURE_2D_ARRAY, GLenum.GL_TEXTURE_MAG_FILTER, GLenum.GL_LINEAR);
                gl.glTexParameteri(GLenum.GL_TEXTURE_2D_ARRAY,GLenum.GL_TEXTURE_WRAP_S, GLenum.GL_REPEAT);
                gl.glTexParameteri(GLenum.GL_TEXTURE_2D_ARRAY,GLenum.GL_TEXTURE_WRAP_T, GLenum.GL_REPEAT);
                gl.glBindSampler(tu_GradientMapTextureArray, 0);
            }
        }
        else

        {
            boolean useMultiTex = true;
//            if(rm_g_textureBindLocationDisplacementMap0 != nvrm_unused)
            {
                gl.glActiveTexture(GLenum.GL_TEXTURE0 + tu_DisplacementMap0);
                gl.glBindTexture(GLenum.GL_TEXTURE_2D, cascade_states[0].m_pFFTSimulation.GetDisplacementMapD3D11().getTexture());
                gl.glBindSampler(tu_DisplacementMap0, m_d3d._11.m_pd3d11LinearNoMipSampler);
//                gl.glUniform1i(rm_g_textureBindLocationDisplacementMap0, tu_DisplacementMap0);
            }
//            if(rm_g_textureBindLocationDisplacementMap1 != nvrm_unused)
            {
                gl.glActiveTexture(GLenum.GL_TEXTURE0 + tu_DisplacementMap1);
                gl.glBindTexture(GLenum.GL_TEXTURE_2D, useMultiTex ? cascade_states[1].m_pFFTSimulation.GetDisplacementMapD3D11().getTexture() : 0);
                gl.glBindSampler(tu_DisplacementMap1, m_d3d._11.m_pd3d11LinearNoMipSampler);
//                gl.glUniform1i(rm_g_textureBindLocationDisplacementMap1, tu_DisplacementMap1);
            }
//            if(rm_g_textureBindLocationDisplacementMap2 != nvrm_unused)
            {
                gl.glActiveTexture(GLenum.GL_TEXTURE0 + tu_DisplacementMap2);
                gl.glBindTexture(GLenum.GL_TEXTURE_2D, useMultiTex ? cascade_states[2].m_pFFTSimulation.GetDisplacementMapD3D11().getTexture() : 0);
                gl.glBindSampler(tu_DisplacementMap2, m_d3d._11.m_pd3d11LinearNoMipSampler);
//                gl.glUniform1i(rm_g_textureBindLocationDisplacementMap2, tu_DisplacementMap2);
            }
//            if(rm_g_textureBindLocationDisplacementMap3 != nvrm_unused)
            {
                gl.glActiveTexture(GLenum.GL_TEXTURE0 + tu_DisplacementMap3);
                gl.glBindTexture(GLenum.GL_TEXTURE_2D, useMultiTex ? cascade_states[3].m_pFFTSimulation.GetDisplacementMapD3D11().getTexture() : 0);
                gl.glBindSampler(tu_DisplacementMap3, m_d3d._11.m_pd3d11LinearNoMipSampler);
//                gl.glUniform1i(rm_g_textureBindLocationDisplacementMap3, tu_DisplacementMap3);
            }
            //
//            if(rm_g_textureBindLocationGradientMap0 != nvrm_unused)
            {
                gl.glActiveTexture(GLenum.GL_TEXTURE0 + tu_GradientMap0);
//                gl.glBindTexture(GLenum.GL_TEXTURE_2D, cascade_states[0].m_d3d._GL2.m_GL2GradientMap[m_active_GPU_slot]);
                gl.glBindTexture(GLenum.GL_TEXTURE_2D, cascade_states[0].m_d3d._11.m_pd3d11GradientMap[m_active_GPU_slot].getTexture());
                gl.glBindSampler(tu_GradientMap0, m_d3d._11.m_pd3d11GradMapSampler);
//                gl.glUniform1i(rm_g_textureBindLocationGradientMap0, tu_GradientMap0);
            }
//            if(rm_g_textureBindLocationGradientMap1 != nvrm_unused)
            {
                gl.glActiveTexture(GLenum.GL_TEXTURE0 + tu_GradientMap1);
                gl.glBindTexture(GLenum.GL_TEXTURE_2D, useMultiTex ? cascade_states[1].m_d3d._11.m_pd3d11GradientMap[m_active_GPU_slot].getTexture() : 0);
                gl.glBindSampler(tu_GradientMap1, m_d3d._11.m_pd3d11GradMapSampler);
//                gl.glUniform1i(rm_g_textureBindLocationGradientMap1, tu_GradientMap1);
            }
//            if(rm_g_textureBindLocationGradientMap2 != nvrm_unused)
            {
                gl.glActiveTexture(GLenum.GL_TEXTURE0 + tu_GradientMap2);
                gl.glBindTexture(GLenum.GL_TEXTURE_2D, useMultiTex ? cascade_states[2].m_d3d._11.m_pd3d11GradientMap[m_active_GPU_slot].getTexture() : 0);
                gl.glBindSampler(tu_GradientMap2, m_d3d._11.m_pd3d11GradMapSampler);
//                gl.glUniform1i(rm_g_textureBindLocationGradientMap2, tu_GradientMap2);
            }
//            if(rm_g_textureBindLocationGradientMap3 != nvrm_unused)
            {
                gl.glActiveTexture(GLenum.GL_TEXTURE0 + tu_GradientMap3);
                gl.glBindTexture(GLenum.GL_TEXTURE_2D, useMultiTex ? cascade_states[3].m_d3d._11.m_pd3d11GradientMap[m_active_GPU_slot].getTexture() : 0);
                gl.glBindSampler(tu_GradientMap3, m_d3d._11.m_pd3d11GradMapSampler);
//                gl.glUniform1i(rm_g_textureBindLocationGradientMap3, tu_GradientMap3);
            }
        }
    }

    private void consumeGPUSlot(){
        m_active_GPU_slot = (m_active_GPU_slot+1)%m_num_GPU_slots;
    }

    private int CreateBuffer(int target, int size, int usage){
        int buffer = gl.glGenBuffer();
        gl.glBindBuffer(target, buffer);
        gl.glBufferData(target, size, usage);
        gl.glBindBuffer(target, 0);
        return buffer;
    }

    private void UpdateSubresource(int target, int buffer, Buffer data){
        gl.glBindBuffer(target, buffer);
        gl.glBufferSubData(target, 0, data);
        gl.glBindBuffer(target, 0);
    }

    static final String SHADER_PATH = "shader_libs/WaveWork/";

    private HRESULT initShaders(){
//        #if WAVEWORKS_ENABLE_GRAPHICS
        switch(m_d3dAPI)
        {
//            #if WAVEWORKS_ENABLE_D3D11
            case nv_water_d3d_api_d3d11:
            {
                HRESULT hr;
                CommonUtil.safeRelease(m_d3d._11.m_pd3d11GradCalcProgram);
                CommonUtil.safeRelease(m_d3d._11.m_pd3d11FoamGenProgram);
//                V_RETURN(m_d3d._11.m_pd3d11Device->CreateVertexShader((void*)SM4::CalcGradient::g_vs, sizeof(SM4::CalcGradient::g_vs), NULL, &m_d3d._11.m_pd3d11GradCalcVS));
//                V_RETURN(m_d3d._11.m_pd3d11Device->CreatePixelShader((void*)SM4::CalcGradient::g_ps, sizeof(SM4::CalcGradient::g_ps), NULL, &m_d3d._11.m_pd3d11GradCalcPS));
//                V_RETURN(m_d3d._11.m_pd3d11Device->CreateVertexShader((void*)SM4::FoamGeneration::g_vs, sizeof(SM4::FoamGeneration::g_vs), NULL, &m_d3d._11.m_pd3d11FoamGenVS));
//                V_RETURN(m_d3d._11.m_pd3d11Device->CreatePixelShader((void*)SM4::FoamGeneration::g_ps, sizeof(SM4::FoamGeneration::g_ps), NULL, &m_d3d._11.m_pd3d11FoamGenPS));

                try {
                    m_d3d._11.m_pd3d11GradCalcProgram = GLSLProgram.createFromFiles(SHADER_PATH + "CalcGradientVS.vert", SHADER_PATH + "CalcGradientPS.frag", new Macro("GFSDK_WAVEWORKS_GL4", 1));
                    m_d3d._11.m_pd3d11FoamGenProgram = GLSLProgram.createFromFiles(SHADER_PATH + "CalcGradientVS.vert", SHADER_PATH + "FoamGenerationPS.frag", new Macro("GFSDK_WAVEWORKS_GL4", 1));
                } catch (IOException e) {
                    e.printStackTrace();
                }


//                D3D11_BUFFER_DESC cbDesc;
//                cbDesc.ByteWidth = sizeof(ps_calcgradient_cbuffer);
//                cbDesc.Usage = D3D11_CB_CREATION_USAGE;
//                cbDesc.BindFlags = D3D11_BIND_CONSTANT_BUFFER;
//                cbDesc.CPUAccessFlags = D3D11_CB_CREATION_CPU_ACCESS_FLAGS;
//                cbDesc.MiscFlags = 0;
//                cbDesc.StructureByteStride = 0;
//                V_RETURN(m_d3d._11.m_pd3d11Device->CreateBuffer(&cbDesc, NULL, &m_d3d._11.m_pd3d11GradCalcPixelShaderCB));
                m_d3d._11.m_pd3d11GradCalcPixelShaderCB = CreateBuffer(GLenum.GL_UNIFORM_BUFFER, 5 * Vector4f.SIZE, GLenum.GL_DYNAMIC_READ);

//                cbDesc.ByteWidth = sizeof(ps_foamgeneration_cbuffer);
//                cbDesc.Usage = D3D11_CB_CREATION_USAGE;
//                cbDesc.BindFlags = D3D11_BIND_CONSTANT_BUFFER;
//                cbDesc.CPUAccessFlags = D3D11_CB_CREATION_CPU_ACCESS_FLAGS;
//                cbDesc.MiscFlags = 0;
//                cbDesc.StructureByteStride = 0;
//                V_RETURN(m_d3d._11.m_pd3d11Device->CreateBuffer(&cbDesc, NULL, &m_d3d._11.m_pd3d11FoamGenPixelShaderCB));
                m_d3d._11.m_pd3d11FoamGenPixelShaderCB = CreateBuffer(GLenum.GL_UNIFORM_BUFFER, 3 * Vector4f.SIZE, GLenum.GL_DYNAMIC_READ);

//                cbDesc.ByteWidth = sizeof(ps_attr_cbuffer);
//                V_RETURN(m_d3d._11.m_pd3d11Device->CreateBuffer(&cbDesc, NULL, &m_d3d._11.m_pd3d11PixelShaderCB));
                m_d3d._11.m_pd3d11PixelShaderCB = CreateBuffer(GLenum.GL_UNIFORM_BUFFER, ps_attr_cbuffer.SIZE, GLenum.GL_DYNAMIC_READ);

//                cbDesc.ByteWidth = sizeof(vs_ds_attr_cbuffer);
//                V_RETURN(m_d3d._11.m_pd3d11Device->CreateBuffer(&cbDesc, NULL, &m_d3d._11.m_pd3d11VertexDomainShaderCB));
                m_d3d._11.m_pd3d11VertexDomainShaderCB = CreateBuffer(GLenum.GL_UNIFORM_BUFFER, 2 * Vector4f.SIZE, GLenum.GL_DYNAMIC_READ);

                SamplerDesc pointSamplerDesc = new SamplerDesc();
//                pointSamplerDesc.Filter = D3D11_FILTER_MIN_MAG_MIP_POINT;
                pointSamplerDesc.magFilter = GLenum.GL_NEAREST;
                pointSamplerDesc.minFilter = GLenum.GL_NEAREST_MIPMAP_NEAREST;
                pointSamplerDesc.wrapR = GLenum.GL_REPEAT;
                pointSamplerDesc.wrapS = GLenum.GL_REPEAT;
                pointSamplerDesc.wrapT = GLenum.GL_REPEAT;
//                pointSamplerDesc.MipLODBias = 0.f;
//                pointSamplerDesc.MaxAnisotropy = 0;
//                pointSamplerDesc.ComparisonFunc = D3D11_COMPARISON_NEVER;
//                pointSamplerDesc.BorderColor[0] = 0.f;
//                pointSamplerDesc.BorderColor[1] = 0.f;
//                pointSamplerDesc.BorderColor[2] = 0.f;
//                pointSamplerDesc.BorderColor[3] = 0.f;
//                pointSamplerDesc.MinLOD = 0.f;
//                pointSamplerDesc.MaxLOD = 0.f;	// NB: No mipping, effectively
//                V_RETURN(m_d3d._11.m_pd3d11Device->CreateSamplerState(&pointSamplerDesc, &m_d3d._11.m_pd3d11PointSampler));
                m_d3d._11.m_pd3d11PointSampler = SamplerUtils.createSampler(pointSamplerDesc);

                SamplerDesc linearNoMipSampleDesc = pointSamplerDesc;
//                linearNoMipSampleDesc.Filter = D3D11_FILTER_MIN_MAG_LINEAR_MIP_POINT;
                linearNoMipSampleDesc.magFilter = GLenum.GL_LINEAR;
                linearNoMipSampleDesc.minFilter = GLenum.GL_LINEAR_MIPMAP_NEAREST;
//                V_RETURN(m_d3d._11.m_pd3d11Device->CreateSamplerState(&linearNoMipSampleDesc, &m_d3d._11.m_pd3d11LinearNoMipSampler));
                m_d3d._11.m_pd3d11LinearNoMipSampler = SamplerUtils.createSampler(linearNoMipSampleDesc);

//                const D3D11_DEPTH_STENCILOP_DESC defaultStencilOp = {D3D11_STENCIL_OP_KEEP, D3D11_STENCIL_OP_KEEP, D3D11_STENCIL_OP_KEEP, D3D11_COMPARISON_ALWAYS};
//                D3D11_DEPTH_STENCIL_DESC dsDesc;
//                dsDesc.DepthEnable = FALSE;
//                dsDesc.DepthWriteMask = D3D11_DEPTH_WRITE_MASK_ZERO;
//                dsDesc.DepthFunc = D3D11_COMPARISON_LESS;
//                dsDesc.StencilEnable = FALSE;
//                dsDesc.StencilReadMask = D3D11_DEFAULT_STENCIL_READ_MASK;
//                dsDesc.StencilWriteMask = D3D11_DEFAULT_STENCIL_WRITE_MASK;
//                dsDesc.FrontFace = defaultStencilOp;
//                dsDesc.BackFace = defaultStencilOp;
//                V_RETURN(m_d3d._11.m_pd3d11Device->CreateDepthStencilState(&dsDesc, &m_d3d._11.m_pd3d11NoDepthStencil));
                m_d3d._11.m_pd3d11NoDepthStencil = ()->
                {
                    gl.glDisable(GLenum.GL_DEPTH_TEST);
                    gl.glDisable(GLenum.GL_STENCIL_TEST);
                };

//                D3D11_RASTERIZER_DESC rastDesc;
//                rastDesc.FillMode = D3D11_FILL_SOLID;
//                rastDesc.CullMode = D3D11_CULL_NONE;
//                rastDesc.FrontCounterClockwise = FALSE;
//                rastDesc.DepthBias = 0;
//                rastDesc.DepthBiasClamp = 0.f;
//                rastDesc.SlopeScaledDepthBias = 0.f;
//                rastDesc.DepthClipEnable = FALSE;
//                rastDesc.ScissorEnable = FALSE;
//                rastDesc.MultisampleEnable = FALSE;
//                rastDesc.AntialiasedLineEnable = FALSE;
//                V_RETURN(m_d3d._11.m_pd3d11Device->CreateRasterizerState(&rastDesc, &m_d3d._11.m_pd3d11AlwaysSolidRasterizer));
                m_d3d._11.m_pd3d11AlwaysSolidRasterizer = ()->
                {
                    gl.glDisable(GLenum.GL_CULL_FACE);
                };

//                D3D11_BLEND_DESC blendDesc;
//                blendDesc.AlphaToCoverageEnable = FALSE;
//                blendDesc.RenderTarget[0].BlendEnable = FALSE;
//                blendDesc.RenderTarget[1].BlendEnable = FALSE;
//                blendDesc.RenderTarget[2].BlendEnable = FALSE;
//                blendDesc.RenderTarget[3].BlendEnable = FALSE;
//                blendDesc.RenderTarget[4].BlendEnable = FALSE;
//                blendDesc.RenderTarget[5].BlendEnable = FALSE;
//                blendDesc.RenderTarget[6].BlendEnable = FALSE;
//                blendDesc.RenderTarget[7].BlendEnable = FALSE;
//                blendDesc.RenderTarget[0].RenderTargetWriteMask = D3D11_COLOR_WRITE_ENABLE_RED | D3D11_COLOR_WRITE_ENABLE_GREEN | D3D11_COLOR_WRITE_ENABLE_BLUE;
//                blendDesc.RenderTarget[1].RenderTargetWriteMask = 0x0F;
//                blendDesc.RenderTarget[2].RenderTargetWriteMask = 0x0F;
//                blendDesc.RenderTarget[3].RenderTargetWriteMask = 0x0F;
//                blendDesc.RenderTarget[4].RenderTargetWriteMask = 0x0F;
//                blendDesc.RenderTarget[5].RenderTargetWriteMask = 0x0F;
//                blendDesc.RenderTarget[6].RenderTargetWriteMask = 0x0F;
//                blendDesc.RenderTarget[7].RenderTargetWriteMask = 0x0F;
//                V_RETURN(m_d3d._11.m_pd3d11Device->CreateBlendState(&blendDesc, &m_d3d._11.m_pd3d11CalcGradBlendState));
                m_d3d._11.m_pd3d11CalcGradBlendState = ()->
                {
                    gl.glDisable(GLenum.GL_BLEND);
                    gl.glColorMask(true, true, true, false);
                };

//                blendDesc.RenderTarget[0].RenderTargetWriteMask = D3D11_COLOR_WRITE_ENABLE_ALL;
//                V_RETURN(m_d3d._11.m_pd3d11Device->CreateBlendState(&blendDesc, &m_d3d._11.m_pd3d11AccumulateFoamBlendState));
                m_d3d._11.m_pd3d11AccumulateFoamBlendState =()->
                {
                    gl.glDisable(GLenum.GL_BLEND);
                    gl.glColorMask(true, true, true, true);
                };

//                blendDesc.RenderTarget[0].RenderTargetWriteMask = D3D11_COLOR_WRITE_ENABLE_ALPHA;
//                V_RETURN(m_d3d._11.m_pd3d11Device->CreateBlendState(&blendDesc, &m_d3d._11.m_pd3d11WriteAccumulatedFoamBlendState));
                m_d3d._11.m_pd3d11WriteAccumulatedFoamBlendState = ()->
                {
                    gl.glDisable(GLenum.GL_BLEND);
                    gl.glColorMask(false, false, false, true);
                };

                m_RenderTarget = new RenderTargets();
                m_AttachDescs[0] = new TextureAttachDesc();
            }
            break;
//            #endif
            /*#if WAVEWORKS_ENABLE_GNM
            case nv_water_d3d_api_gnm:
            {
                GFSDK_WaveWorks_GNM_Util::ReleaseVsShader(m_d3d._gnm.m_pGnmGradCalcVS, m_d3d._gnm.m_pGnmGradCalcFS);
                GFSDK_WaveWorks_GNM_Util::ReleaseInputResourceOffsets(m_d3d._gnm.m_pGnmGradCalcVSResourceOffsets);
                GFSDK_WaveWorks_GNM_Util::ReleasePsShader(m_d3d._gnm.m_pGnmGradCalcPS);
                GFSDK_WaveWorks_GNM_Util::ReleaseInputResourceOffsets(m_d3d._gnm.m_pGnmGradCalcPSResourceOffsets);
                GFSDK_WaveWorks_GNM_Util::ReleaseVsShader(m_d3d._gnm.m_pGnmFoamGenVS, m_d3d._gnm.m_pGnmFoamGenFS);
                GFSDK_WaveWorks_GNM_Util::ReleaseInputResourceOffsets(m_d3d._gnm.m_pGnmFoamGenVSResourceOffsets);
                GFSDK_WaveWorks_GNM_Util::ReleasePsShader(m_d3d._gnm.m_pGnmFoamGenPS);
                GFSDK_WaveWorks_GNM_Util::ReleaseInputResourceOffsets(m_d3d._gnm.m_pGnmFoamGenPSResourceOffsets);
                GFSDK_WaveWorks_GNM_Util::ReleaseCsShader(m_d3d._gnm.m_pGnmMipMapGenCS);
                GFSDK_WaveWorks_GNM_Util::ReleaseInputResourceOffsets(m_d3d._gnm.m_pGnmMipMapGenCSResourceOffsets);
                GFSDK_WaveWorks_GNM_Util::ReleaseRenderTargetClearer(m_d3d._gnm.m_pGnmRenderTargetClearer);

                m_d3d._gnm.m_pGnmGradCalcVS = GFSDK_WaveWorks_GNM_Util::CreateVsMakeFetchShader(m_d3d._gnm.m_pGnmGradCalcFS, PSSL::g_NVWaveWorks_CalcGradientVertexShader);
                m_d3d._gnm.m_pGnmGradCalcVSResourceOffsets = GFSDK_WaveWorks_GNM_Util::CreateInputResourceOffsets(Gnm::kShaderStageVs, m_d3d._gnm.m_pGnmGradCalcVS);
                m_d3d._gnm.m_pGnmGradCalcPS = GFSDK_WaveWorks_GNM_Util::CreatePsShader(PSSL::g_NVWaveWorks_CalcGradientPixelShader);
                m_d3d._gnm.m_pGnmGradCalcPSResourceOffsets = GFSDK_WaveWorks_GNM_Util::CreateInputResourceOffsets(Gnm::kShaderStagePs, m_d3d._gnm.m_pGnmGradCalcPS);
                m_d3d._gnm.m_pGnmFoamGenVS = GFSDK_WaveWorks_GNM_Util::CreateVsMakeFetchShader(m_d3d._gnm.m_pGnmFoamGenFS, PSSL::g_NVWaveWorks_FoamGenerationVertexShader);
                m_d3d._gnm.m_pGnmFoamGenVSResourceOffsets = GFSDK_WaveWorks_GNM_Util::CreateInputResourceOffsets(Gnm::kShaderStageVs, m_d3d._gnm.m_pGnmFoamGenVS);
                m_d3d._gnm.m_pGnmFoamGenPS = GFSDK_WaveWorks_GNM_Util::CreatePsShader(PSSL::g_NVWaveWorks_FoamGenerationPixelShader);
                m_d3d._gnm.m_pGnmFoamGenPSResourceOffsets = GFSDK_WaveWorks_GNM_Util::CreateInputResourceOffsets(Gnm::kShaderStagePs, m_d3d._gnm.m_pGnmFoamGenPS);
                m_d3d._gnm.m_pGnmMipMapGenCS = GFSDK_WaveWorks_GNM_Util::CreateCsShader(PSSL::g_NVWaveWorks_MipMapGenerationComputeShader);
                m_d3d._gnm.m_pGnmMipMapGenCSResourceOffsets = GFSDK_WaveWorks_GNM_Util::CreateInputResourceOffsets(Gnm::kShaderStageCs, m_d3d._gnm.m_pGnmMipMapGenCS);
                m_d3d._gnm.m_pGnmRenderTargetClearer = GFSDK_WaveWorks_GNM_Util::CreateRenderTargetClearer();

                void* pixelShaderCB = NVSDK_aligned_malloc(sizeof(ps_attr_cbuffer), Gnm::kAlignmentOfBufferInBytes);
                m_d3d._gnm.m_pGnmPixelShaderCB.initAsConstantBuffer(pixelShaderCB, sizeof(ps_attr_cbuffer));
                m_d3d._gnm.m_pGnmPixelShaderCB.setResourceMemoryType(Gnm::kResourceMemoryTypeRO); // it's a constant buffer, so read-only is OK

                void* vertexShaderCB = NVSDK_aligned_malloc(sizeof(vs_ds_attr_cbuffer), Gnm::kAlignmentOfBufferInBytes);
                m_d3d._gnm.m_pGnmVertexDomainShaderCB.initAsConstantBuffer(vertexShaderCB, sizeof(vs_ds_attr_cbuffer));
                m_d3d._gnm.m_pGnmVertexDomainShaderCB.setResourceMemoryType(Gnm::kResourceMemoryTypeRO); // it's a constant buffer, so read-only is OK

                m_d3d._gnm.m_pGnmPointSampler.init();
                m_d3d._gnm.m_pGnmPointSampler.setMipFilterMode(Gnm::kMipFilterModeNone);
                m_d3d._gnm.m_pGnmPointSampler.setXyFilterMode(Gnm::kFilterModePoint, Gnm::kFilterModePoint);
                m_d3d._gnm.m_pGnmPointSampler.setWrapMode(Gnm::kWrapModeWrap, Gnm::kWrapModeWrap, Gnm::kWrapModeWrap);
                m_d3d._gnm.m_pGnmPointSampler.setDepthCompareFunction(Gnm::kDepthCompareNever);

                m_d3d._gnm.m_pGnmLinearNoMipSampler = m_d3d._gnm.m_pGnmPointSampler;
                m_d3d._gnm.m_pGnmLinearNoMipSampler.setXyFilterMode(Gnm::kFilterModeBilinear, Gnm::kFilterModeBilinear);

                m_d3d._gnm.m_pGnmNoDepthStencil.init();

                m_d3d._gnm.m_pGnmAlwaysSolidRasterizer.init();
                m_d3d._gnm.m_pGnmAlwaysSolidRasterizer.setFrontFace(Gnm::kPrimitiveSetupFrontFaceCw);
                m_d3d._gnm.m_pGnmAlwaysSolidRasterizer.setPolygonMode(Gnm::kPrimitiveSetupPolygonModeFill, Gnm::kPrimitiveSetupPolygonModeFill);

                m_d3d._gnm.m_pGnmCalcGradBlendState.init();
                m_d3d._gnm.m_pGnmAccumulateFoamBlendState.init();
                m_d3d._gnm.m_pGnmWriteAccumulatedFoamBlendState.init();
            }
            break;
            #endif*/
//            #if WAVEWORKS_ENABLE_GL
            case nv_water_d3d_api_gl2:
            {
                if(m_d3d._GL2.m_GradCalcProgram != null) m_d3d._GL2.m_GradCalcProgram.dispose();
                if(m_d3d._GL2.m_FoamGenProgram != null) m_d3d._GL2.m_FoamGenProgram.dispose();
//                m_d3d._GL2.m_GradCalcProgram = loadGLProgram(GL::k_NVWaveWorks_CalcGradientVertexShader,NULL,NULL,NULL,GL::k_NVWaveWorks_CalcGradientFragmentShader);
//                if(m_d3d._GL2.m_GradCalcProgram == 0) return E_FAIL;
                try {
                    // Creating gradient calculation program
                    m_d3d._GL2.m_GradCalcProgram= GLSLProgram.createFromFiles(SHADER_PATH + "CalcGradientVS.vert", SHADER_PATH + "CalcGradientPS.frag");
                    // Creating foam generation program
                    m_d3d._GL2.m_FoamGenProgram = GLSLProgram.createFromFiles(SHADER_PATH + "CalcGradientVS.vert", SHADER_PATH + "FoamGenerationPS.frag");
                } catch (IOException e) {
                    e.printStackTrace();
                }

                // Gradient calculation program binding
                m_d3d._GL2.m_GradCalcUniformLocation_Scales = gl.glGetUniformLocation(m_d3d._GL2.m_GradCalcProgram.getProgram(),"g_Scales");
                m_d3d._GL2.m_GradCalcUniformLocation_OneBack = gl.glGetUniformLocation(m_d3d._GL2.m_GradCalcProgram.getProgram(),"g_OneTexel_Back");
                m_d3d._GL2.m_GradCalcUniformLocation_OneFront = gl.glGetUniformLocation(m_d3d._GL2.m_GradCalcProgram.getProgram(),"g_OneTexel_Front");
                m_d3d._GL2.m_GradCalcUniformLocation_OneLeft = gl.glGetUniformLocation(m_d3d._GL2.m_GradCalcProgram.getProgram(),"g_OneTexel_Left");
                m_d3d._GL2.m_GradCalcUniformLocation_OneRight = gl.glGetUniformLocation(m_d3d._GL2.m_GradCalcProgram.getProgram(),"g_OneTexel_Right");
                m_d3d._GL2.m_GradCalcTextureBindLocation_DisplacementMap = gl.glGetUniformLocation(m_d3d._GL2.m_GradCalcProgram.getProgram(),"g_samplerDisplacementMap");
                m_d3d._GL2.m_GradCalcTextureUnit_DisplacementMap = 0;
                m_d3d._GL2.m_GradCalcAttributeLocation_Pos = gl.glGetAttribLocation(m_d3d._GL2.m_GradCalcProgram.getProgram(), "vInPos");
                m_d3d._GL2.m_GradCalcAttributeLocation_TexCoord = gl.glGetAttribLocation(m_d3d._GL2.m_GradCalcProgram.getProgram(), "vInTexCoord");

                // Foam accumulation program binding
                m_d3d._GL2.m_FoamGenUniformLocation_DissipationFactors = gl.glGetUniformLocation(m_d3d._GL2.m_FoamGenProgram.getProgram(),"g_DissipationFactors");
                m_d3d._GL2.m_FoamGenUniformLocation_SourceComponents = gl.glGetUniformLocation(m_d3d._GL2.m_FoamGenProgram.getProgram(),"g_SourceComponents");
                m_d3d._GL2.m_FoamGenUniformLocation_UVOffsets = gl.glGetUniformLocation(m_d3d._GL2.m_FoamGenProgram.getProgram(),"g_UVOffsets");
                m_d3d._GL2.m_FoamGenTextureBindLocation_EnergyMap = gl.glGetUniformLocation(m_d3d._GL2.m_FoamGenProgram.getProgram(),"g_samplerEnergyMap");
                m_d3d._GL2.m_FoamGenTextureUnit_EnergyMap = 0;
                m_d3d._GL2.m_FoamGenAttributeLocation_Pos = gl.glGetAttribLocation(m_d3d._GL2.m_FoamGenProgram.getProgram(), "vInPos");
                m_d3d._GL2.m_FoamGenAttributeLocation_TexCoord = gl.glGetAttribLocation(m_d3d._GL2.m_FoamGenProgram.getProgram(), "vInTexCoord");
            }
            break;
//            #endif
            case nv_water_d3d_api_none:
                break;
            default:
                // Unexpected API
                return HRESULT.E_FAIL;
        }

//        #endif // WAVEWORKS_ENABLE_GRAPHICS

        return HRESULT.S_OK;
    }

    private HRESULT initGradMapSamplers(){
//        #if WAVEWORKS_ENABLE_GRAPHICS
        switch(m_d3dAPI)
        {
//            #if WAVEWORKS_ENABLE_D3D11
            case nv_water_d3d_api_d3d11:
            {
                HRESULT hr;
                if(m_d3d._11.m_pd3d11GradMapSampler != 0 && gl.glIsSampler(m_d3d._11.m_pd3d11GradMapSampler))
                    return HRESULT.S_OK;

                SamplerDesc anisoSamplerDesc = new SamplerDesc();
//                anisoSamplerDesc.Filter = m_params.aniso_level > 1 ? D3D11_FILTER_ANISOTROPIC : D3D11_FILTER_MIN_MAG_MIP_LINEAR;
                anisoSamplerDesc.wrapR = GLenum.GL_REPEAT;
                anisoSamplerDesc.wrapS = GLenum.GL_REPEAT;
                anisoSamplerDesc.wrapT = GLenum.GL_REPEAT;
                anisoSamplerDesc.minFilter = GLenum.GL_LINEAR_MIPMAP_LINEAR;
                anisoSamplerDesc.magFilter = GLenum.GL_LINEAR;
//                anisoSamplerDesc.MipLODBias = 0.f;
                anisoSamplerDesc.anisotropic = m_params.aniso_level;
//                anisoSamplerDesc.ComparisonFunc = D3D11_COMPARISON_NEVER;
//                anisoSamplerDesc.BorderColor[0] = 0.f;
//                anisoSamplerDesc.BorderColor[1] = 0.f;
//                anisoSamplerDesc.BorderColor[2] = 0.f;
//                anisoSamplerDesc.BorderColor[3] = 0.f;
//                anisoSamplerDesc.MinLOD = 0.f;
//                anisoSamplerDesc.MaxLOD = FLT_MAX;
//                V_RETURN(m_d3d._11.m_pd3d11Device->CreateSamplerState(&anisoSamplerDesc, &m_d3d._11.m_pd3d11GradMapSampler));
                m_d3d._11.m_pd3d11GradMapSampler = SamplerUtils.createSampler(anisoSamplerDesc);

//                #ifdef TARGET_PLATFORM_XBONE
//                ID3D11DeviceX* pD3DDevX = NULL;
//                hr = m_d3d._11.m_pd3d11Device->QueryInterface(IID_ID3D11DeviceX,(void**)&pD3DDevX);
//
//                if(SUCCEEDED(hr))
//                {
//                    // True fact: the Xbone docs recommends doing it this way... (!)
//                    //
//                    // "The easiest way to determine how to fill in all of the many confusing fields of D3D11X_SAMPLER_DESC
//                    // is to use CreateSamplerState to create the closest Direct3D equivalent, call GetDescX to get back the
//                    // corresponding D3D11X_SAMPLER_DESC structure, override the appropriate fields, and then call CreateSamplerStateX.
//                    //
//                    D3D11X_SAMPLER_DESC anisoSamplerDescX;
//                    m_d3d._11.m_pd3d11GradMapSampler->GetDescX(&anisoSamplerDescX);
//                    anisoSamplerDescX.PerfMip = 10;	// Determined empirically at this stage
//                    SAFE_RELEASE(m_d3d._11.m_pd3d11GradMapSampler);
//                    V_RETURN(pD3DDevX->CreateSamplerStateX(&anisoSamplerDescX, &m_d3d._11.m_pd3d11GradMapSampler));
//                    SAFE_RELEASE(pD3DDevX);
//                }
//                #endif // TARGET_PLATFORM_XBONE

            }
            break;
//            #endif
//            #if WAVEWORKS_ENABLE_GNM
//            case nv_water_d3d_api_gnm:
//            {
//                m_d3d._gnm.m_pGnmGradMapSampler.init();
//                m_d3d._gnm.m_pGnmGradMapSampler.setMipFilterMode(Gnm::kMipFilterModeLinear);
//                Gnm::FilterMode filterMode = m_params.aniso_level > 1 ? Gnm::kFilterModeAnisoBilinear : Gnm::kFilterModeBilinear;
//                m_d3d._gnm.m_pGnmGradMapSampler.setXyFilterMode(filterMode, filterMode);
//                m_d3d._gnm.m_pGnmGradMapSampler.setWrapMode(Gnm::kWrapModeWrap, Gnm::kWrapModeWrap, Gnm::kWrapModeWrap);
//                int ratio = 0;
//                for(int level = m_params.aniso_level; level > 1 && ratio < Gnm::kAnisotropyRatio16; level >>= 1)
//                    ++ratio;
//                m_d3d._gnm.m_pGnmGradMapSampler.setAnisotropyRatio(Gnm::AnisotropyRatio(ratio));
//            }
//            break;
//            #endif
//            #if WAVEWORKS_ENABLE_GL
            case nv_water_d3d_api_gl2:
            {
                // nothing to do here
            }
            break;
//            #endif
            case nv_water_d3d_api_none:
                break;
            default:
                // Unexpected API
                return HRESULT.E_FAIL;
        }

//        #endif // WAVEWORKS_ENABLE_GRAPHICS

        return HRESULT.S_OK;
    }

    private HRESULT initTextureArrays(){
//        #if WAVEWORKS_ENABLE_GRAPHICS
        switch(m_d3dAPI)
        {
//            #if WAVEWORKS_ENABLE_D3D11
            case nv_water_d3d_api_d3d11:
                break;
//            #endif
//            #if WAVEWORKS_ENABLE_GNM
            case nv_water_d3d_api_gnm:
                break;
//            #endif
//            #if WAVEWORKS_ENABLE_GL
            case nv_water_d3d_api_gl2:
            {
                // last cascade is the closest cascade and it has the highest fft resolution
                int N = m_params.cascades[GFSDK_WaveWorks_Detailed_Simulation_Params.MaxNumCascades-1].fft_resolution;

                // using the right texture format to avoid implicit format conversion (half float <-> float) when filling the texture arrays
                int displacement_texture_array_format = (m_params.simulation_api == nv_water_simulation_api_cpu) ? GLenum.GL_RGBA16F : GLenum.GL_RGBA32F;
                int displacement_texture_array_type = (m_params.simulation_api == nv_water_simulation_api_cpu) ? GLenum.GL_HALF_FLOAT : GLenum.GL_FLOAT;

                // creating displacement texture array
                if(m_d3d._GL2.m_DisplacementsTextureArray == 0) m_d3d._GL2.m_DisplacementsTextureArray = gl.glGenTexture();
                gl.glBindTexture(GLenum.GL_TEXTURE_2D_ARRAY, m_d3d._GL2.m_DisplacementsTextureArray);
                gl.glTexParameteri(GLenum.GL_TEXTURE_2D_ARRAY, GLenum.GL_TEXTURE_MIN_FILTER, GLenum.GL_LINEAR);
                gl.glTexParameteri(GLenum.GL_TEXTURE_2D_ARRAY, GLenum.GL_TEXTURE_MAG_FILTER, GLenum.GL_LINEAR);
                gl.glTexImage3D(GLenum.GL_TEXTURE_2D_ARRAY, 0, displacement_texture_array_format, N, N, 4, 0, GLenum.GL_RGBA, displacement_texture_array_type, null);
                gl.glBindTexture(GLenum.GL_TEXTURE_2D_ARRAY, 0);

                // creating gradients texture array
                if(m_d3d._GL2.m_GradientsTextureArray == 0) m_d3d._GL2.m_GradientsTextureArray = gl.glGenTexture();
                gl.glBindTexture(GLenum.GL_TEXTURE_2D_ARRAY, m_d3d._GL2.m_GradientsTextureArray);
                gl.glTexParameteri(GLenum.GL_TEXTURE_2D_ARRAY, GLenum.GL_TEXTURE_MIN_FILTER, GLenum.GL_LINEAR_MIPMAP_LINEAR);
                gl.glTexParameteri(GLenum.GL_TEXTURE_2D_ARRAY, GLenum.GL_TEXTURE_MAG_FILTER, GLenum.GL_LINEAR);
                gl.glTexImage3D(GLenum.GL_TEXTURE_2D_ARRAY, 0, GLenum.GL_RGBA16F, N, N, 4, 0, GLenum.GL_RGBA, GLenum.GL_HALF_FLOAT, null);
                gl.glGenerateMipmap(GLenum.GL_TEXTURE_2D_ARRAY);  // allocating memory for mipmaps of gradient texture array
                gl.glBindTexture(GLenum.GL_TEXTURE_2D_ARRAY, 0);

                // creating FBOs used to blit from separate displacement/gradient textures to displacement/gradient texture arrays
                if(m_d3d._GL2.m_TextureArraysBlittingDrawFBO == 0) m_d3d._GL2.m_TextureArraysBlittingDrawFBO = gl.glGenFramebuffer();
                if(m_d3d._GL2.m_TextureArraysBlittingReadFBO == 0) m_d3d._GL2.m_TextureArraysBlittingReadFBO = gl.glGenFramebuffer();
            }
            break;
//            #endif
            case nv_water_d3d_api_none:
                break;
            default:
                // Unexpected API
                return HRESULT.E_FAIL;
        }

//        #endif // WAVEWORKS_ENABLE_GRAPHICS
        return HRESULT.S_OK;
    }

    private HRESULT initQuadMesh(int cascade){
        if(nv_water_d3d_api_none == m_d3dAPI)
            return S_OK;						// No GFX, no timers

//        #if WAVEWORKS_ENABLE_GRAPHICS
        CommonUtil.safeRelease(cascade_states[cascade].m_pQuadMesh);

        // Vertices
        float tex_adjust = 0.f;

//        float vertices[] = {-1.0f,  1.0f, 0,	tex_adjust,      tex_adjust,
//                -1.0f, -1.0f, 0,	tex_adjust,      tex_adjust+1.0f,
//                1.0f,  1.0f, 0,	tex_adjust+1.0f, tex_adjust,
//                1.0f, -1.0f, 0,	tex_adjust+1.0f, tex_adjust+1.0f};

//        #if WAVEWORKS_ENABLE_GL
        // GL has different viewport origin(0,0) compared to DX, so flipping texcoords
        float verticesGL[]= {-1.0f,  1.0f, 0,	tex_adjust,      tex_adjust+1.0f,
                -1.0f, -1.0f, 0,	tex_adjust,      tex_adjust,
                1.0f,  1.0f, 0,	tex_adjust+1.0f, tex_adjust+1.0f,
                1.0f, -1.0f, 0,	tex_adjust+1.0f, tex_adjust};
//        #endif // WAVEWORKS_ENABLE_GL

        final int VertexStride = 20;

        // Indices
        final int indices[] = {0, 1, 2, 3};

        // Init mesh
        switch(m_d3dAPI)
        {
//            #if WAVEWORKS_ENABLE_D3D11
            /*case nv_water_d3d_api_d3d11:
            {
                HRESULT hr;

                const D3D11_INPUT_ELEMENT_DESC quad_layout[] = {
                    { "POSITION", 0, DXGI_FORMAT_R32G32B32_FLOAT, 0, 0, D3D11_INPUT_PER_VERTEX_DATA, 0 },
                    { "TEXCOORD", 0, DXGI_FORMAT_R32G32_FLOAT, 0, 12, D3D11_INPUT_PER_VERTEX_DATA, 0 },
            };
                const UINT num_layout_elements = sizeof(quad_layout)/sizeof(quad_layout[0]);

                V_RETURN(NVWaveWorks_Mesh::CreateD3D11(	m_d3d._11.m_pd3d11Device,
                    quad_layout, num_layout_elements,
                    SM4::CalcGradient::g_vs, sizeof(SM4::CalcGradient::g_vs),
                VertexStride, vertices, 4, indices, 4,
                &cascade_states[cascade].m_pQuadMesh
                ));
            }
            break;
            #endif
            #if WAVEWORKS_ENABLE_GNM
            case nv_water_d3d_api_gnm:
            {
                NVWaveWorks_Mesh::CreateGnm(VertexStride, vertices, 4, indices, 4,
                &cascade_states[cascade].m_pQuadMesh
                );
            }
            break;
            #endif
            #if WAVEWORKS_ENABLE_GL*/
            case nv_water_d3d_api_gl2:
            case nv_water_d3d_api_d3d11:
            {
                HRESULT hr;

                final AttribDesc attribute_descs[] =
                {
                    new AttribDesc(0,3, GLenum.GL_FLOAT, false, VertexStride, 0),			// Pos
                    new AttribDesc(1,2, GLenum.GL_FLOAT, false, VertexStride, 3*4),			// TexCoord
                };

                NVWaveWorks_Mesh[] out = new NVWaveWorks_Mesh[1];

                if(m_d3dAPI == nv_water_d3d_api_gl2) {
                    hr = NVWaveWorks_Mesh.CreateGL2(attribute_descs,
                            attribute_descs.length,
                            VertexStride, CacheBuffer.wrap(verticesGL), 4,
                            indices, 4,
                            out
                    );
                }else{
                    hr = NVWaveWorks_Mesh.CreateD3D11(attribute_descs,
                            attribute_descs.length,
                            VertexStride, CacheBuffer.wrap(verticesGL), 4,
                            indices, 4,
                            out);
                }

                if(hr != HRESULT.S_OK) return hr;

                cascade_states[cascade].m_pQuadMesh = out[0];
            }
            break;
            case nv_water_d3d_api_none:
                break;
            default:
                // Unexpected API
                return HRESULT.E_FAIL;
        }
//        #endif // WAVEWORKS_ENABLE_GRAPHICS

        return HRESULT.S_OK;
    }

    private HRESULT allocateAll(){
        HRESULT hr;

        hr = initShaders(); if(hr != HRESULT.S_OK) return hr;
        hr = initGradMapSamplers(); if(hr != HRESULT.S_OK) return hr;
        if(m_params.use_texture_arrays)
        {
            hr = initTextureArrays(); if(hr != HRESULT.S_OK) return hr;
        }

        hr = allocateSimulationManager(); if(hr != HRESULT.S_OK) return hr;
        hr = allocateGFXTimer(); if(hr != HRESULT.S_OK) return hr;

        for(int cascade = 0; cascade != m_params.num_cascades; ++cascade)
        {
            hr = allocateRenderingResources(cascade); if(hr != HRESULT.S_OK) return hr;
            hr = allocateSimulation(cascade); if(hr != HRESULT.S_OK) return hr;
        }

        updateRMS(m_params);

        return HRESULT.S_OK;
    }

    private void releaseAll(){
        if(nv_water_d3d_api.nv_water_d3d_api_undefined == m_d3dAPI)
            return;

        for(int cascade = 0; cascade != m_params.num_cascades; ++cascade)
        {
            releaseRenderingResources(cascade);
            releaseSimulation(cascade);
        }

        releaseGFXTimer();
        releaseSimulationManager();

        m_pOptionalScheduler = null;

        switch(m_d3dAPI)
        {
            case nv_water_d3d_api_d3d11:
            {
                CommonUtil.safeRelease(m_d3d._11.m_pd3d11GradCalcProgram);
//                SAFE_RELEASE(m_d3d._11.m_pd3d11GradCalcPS);
                gl.glDeleteBuffer(m_d3d._11.m_pd3d11GradCalcPixelShaderCB);

                CommonUtil.safeRelease(m_d3d._11.m_pd3d11FoamGenProgram);
//                SAFE_RELEASE(m_d3d._11.m_pd3d11FoamGenPS);
                gl.glDeleteBuffer(m_d3d._11.m_pd3d11FoamGenPixelShaderCB);
//                SAFE_RELEASE(m_d3d._11.m_pd3d11PointSampler);
//                SAFE_RELEASE(m_d3d._11.m_pd3d11NoDepthStencil);
//                SAFE_RELEASE(m_d3d._11.m_pd3d11AlwaysSolidRasterizer);
//                SAFE_RELEASE(m_d3d._11.m_pd3d11CalcGradBlendState);
//                SAFE_RELEASE(m_d3d._11.m_pd3d11AccumulateFoamBlendState);
//                SAFE_RELEASE(m_d3d._11.m_pd3d11WriteAccumulatedFoamBlendState);
//                SAFE_RELEASE(m_d3d._11.m_pd3d11LinearNoMipSampler);
//                SAFE_RELEASE(m_d3d._11.m_pd3d11GradMapSampler);
                gl.glDeleteBuffer(m_d3d._11.m_pd3d11PixelShaderCB);
                gl.glDeleteBuffer(m_d3d._11.m_pd3d11VertexDomainShaderCB);
//                SAFE_RELEASE(m_d3d._11.m_pd3d11Device);
                m_d3dAPI = nv_water_d3d_api.nv_water_d3d_api_undefined;
            }
            break;
//            #if WAVEWORKS_ENABLE_GNM
//            case nv_water_d3d_api_gnm:
//            {
//                GFSDK_WaveWorks_GNM_Util::ReleaseVsShader(m_d3d._gnm.m_pGnmGradCalcVS, m_d3d._gnm.m_pGnmGradCalcFS);
//                GFSDK_WaveWorks_GNM_Util::ReleaseInputResourceOffsets(m_d3d._gnm.m_pGnmGradCalcVSResourceOffsets);
//                GFSDK_WaveWorks_GNM_Util::ReleasePsShader(m_d3d._gnm.m_pGnmGradCalcPS);
//                GFSDK_WaveWorks_GNM_Util::ReleaseInputResourceOffsets(m_d3d._gnm.m_pGnmGradCalcPSResourceOffsets);
//                GFSDK_WaveWorks_GNM_Util::ReleaseVsShader(m_d3d._gnm.m_pGnmFoamGenVS, m_d3d._gnm.m_pGnmFoamGenFS);
//                GFSDK_WaveWorks_GNM_Util::ReleaseInputResourceOffsets(m_d3d._gnm.m_pGnmFoamGenVSResourceOffsets);
//                GFSDK_WaveWorks_GNM_Util::ReleasePsShader(m_d3d._gnm.m_pGnmFoamGenPS);
//                GFSDK_WaveWorks_GNM_Util::ReleaseInputResourceOffsets(m_d3d._gnm.m_pGnmFoamGenPSResourceOffsets);
//                GFSDK_WaveWorks_GNM_Util::ReleaseCsShader(m_d3d._gnm.m_pGnmMipMapGenCS);
//                GFSDK_WaveWorks_GNM_Util::ReleaseInputResourceOffsets(m_d3d._gnm.m_pGnmMipMapGenCSResourceOffsets);
//                GFSDK_WaveWorks_GNM_Util::ReleaseRenderTargetClearer(m_d3d._gnm.m_pGnmRenderTargetClearer);
//
//                NVSDK_free(m_d3d._gnm.m_pGnmPixelShaderCB.getBaseAddress());
//                NVSDK_free(m_d3d._gnm.m_pGnmVertexDomainShaderCB.getBaseAddress());
//
//                m_d3dAPI = nv_water_d3d_api_undefined;
//            }
//            break;
//            #endif
            case nv_water_d3d_api_gl2:
            {
                if(m_d3d._GL2.m_GradCalcProgram != null) {CommonUtil.safeRelease(m_d3d._GL2.m_GradCalcProgram);  m_d3d._GL2.m_GradCalcProgram= null;}
                if(m_d3d._GL2.m_FoamGenProgram != null) {CommonUtil.safeRelease(m_d3d._GL2.m_FoamGenProgram);  m_d3d._GL2.m_FoamGenProgram = null;}
                if(m_d3d._GL2.m_DisplacementsTextureArray != 0) gl.glDeleteTexture(m_d3d._GL2.m_DisplacementsTextureArray);
                if(m_d3d._GL2.m_GradientsTextureArray != 0) gl.glDeleteTexture(m_d3d._GL2.m_GradientsTextureArray);
                if(m_d3d._GL2.m_TextureArraysBlittingDrawFBO != 0) gl.glDeleteFramebuffer(m_d3d._GL2.m_TextureArraysBlittingDrawFBO);
                if(m_d3d._GL2.m_TextureArraysBlittingReadFBO != 0) gl.glDeleteFramebuffer(m_d3d._GL2.m_TextureArraysBlittingReadFBO);
                m_d3dAPI = nv_water_d3d_api_undefined;
            }
            break;
            case nv_water_d3d_api_none:
            {
                m_d3dAPI = nv_water_d3d_api_undefined;
            }
            break;
            default:
                break;
        }
    }

    private void releaseRenderingResources(int cascade){
        if(cascade_states[cascade].m_pQuadMesh != null){
            cascade_states[cascade].m_pQuadMesh.dispose();
            cascade_states[cascade].m_pQuadMesh = null;
        }

//        #if WAVEWORKS_ENABLE_GRAPHICS
        for(int gpu_slot = 0; gpu_slot != m_num_GPU_slots; ++gpu_slot)
        {
            switch(m_d3dAPI)
            {
//                #if WAVEWORKS_ENABLE_D3D11
                case nv_water_d3d_api_d3d11:
                {
                    CommonUtil.safeRelease(cascade_states[cascade].m_d3d._11.m_pd3d11GradientMap[gpu_slot]);
                    CommonUtil.safeRelease(cascade_states[cascade].m_d3d._11.m_pd3d11GradientRenderTarget[gpu_slot]);

                    cascade_states[cascade].m_d3d._11.m_pd3d11GradientMap[gpu_slot] = null;
                    cascade_states[cascade].m_d3d._11.m_pd3d11GradientRenderTarget[gpu_slot] = null;
                }
                break;
//                #endif
//                #if WAVEWORKS_ENABLE_GNM
//                case nv_water_d3d_api_gnm:
//                {
//                    NVSDK_garlic_free(cascade_states[cascade].m_d3d._gnm.m_gnmGradientMap[gpu_slot].getBaseAddress());
//                }
//                break;
//                #endif
//                #if WAVEWORKS_ENABLE_GL
                case nv_water_d3d_api_gl2:
                {
                    if(cascade_states[cascade].m_d3d._GL2.m_GL2GradientMap[gpu_slot] != 0) gl.glDeleteTexture(cascade_states[cascade].m_d3d._GL2.m_GL2GradientMap[gpu_slot]);
                    if(cascade_states[cascade].m_d3d._GL2.m_GL2GradientFBO[gpu_slot] != 0) gl.glDeleteFramebuffer(cascade_states[cascade].m_d3d._GL2.m_GL2GradientFBO[gpu_slot]);
                }
                break;
//                #endif
                default:
                    break;
            }
        }

        switch(m_d3dAPI)
        {
//            #if WAVEWORKS_ENABLE_D3D11
            case nv_water_d3d_api_d3d11:
            {
                CommonUtil.safeRelease(cascade_states[cascade].m_d3d._11.m_pd3d11FoamEnergyMap);
                CommonUtil.safeRelease(cascade_states[cascade].m_d3d._11.m_pd3d11FoamEnergyRenderTarget);

                cascade_states[cascade].m_d3d._11.m_pd3d11FoamEnergyMap = null;
                cascade_states[cascade].m_d3d._11.m_pd3d11FoamEnergyRenderTarget = null;
            }
            break;
//            #endif
//            #if WAVEWORKS_ENABLE_GNM
//            case nv_water_d3d_api_gnm:
//            {
//                NVSDK_garlic_free(cascade_states[cascade].m_d3d._gnm.m_gnmFoamEnergyMap.getBaseAddress());
//            }
//            break;
//            #endif
//            #if WAVEWORKS_ENABLE_GL
            case nv_water_d3d_api_gl2:
            {
                if(cascade_states[cascade].m_d3d._GL2.m_GL2FoamEnergyMap != 0) gl.glDeleteTexture(cascade_states[cascade].m_d3d._GL2.m_GL2FoamEnergyMap);
                if(cascade_states[cascade].m_d3d._GL2.m_GL2FoamEnergyFBO != 0) gl.glDeleteFramebuffer(cascade_states[cascade].m_d3d._GL2.m_GL2FoamEnergyFBO);
            }
            break;
//            #endif
            default:
                break;
        }
//        #endif // WAVEWORKS_ENABLE_GRAPHICS
    }

    private HRESULT allocateRenderingResources(int cascade){
        HRESULT hr;

        hr = initQuadMesh(cascade); if(hr != HRESULT.S_OK)  return hr;

        m_num_GPU_slots = m_params.num_GPUs;
        m_active_GPU_slot = m_num_GPU_slots-1;	// First tick will tip back to zero
        m_numValidEntriesInSimTimeFIFO = 0;

//        #if WAVEWORKS_ENABLE_GRAPHICS
        int dmap_dim =m_params.cascades[cascade].fft_resolution;

        for(int gpu_slot = 0; gpu_slot != m_num_GPU_slots; ++gpu_slot)
        {
            switch(m_d3dAPI)
            {

//                #if WAVEWORKS_ENABLE_D3D11
                case nv_water_d3d_api_d3d11:
                {
                    Texture2DDesc gradMapTD = new Texture2DDesc();
                    gradMapTD.width = dmap_dim;
                    gradMapTD.height = dmap_dim;
                    gradMapTD.mipLevels = (int) (Math.log(dmap_dim)/Math.log(2));
                    gradMapTD.arraySize = 1;
                    gradMapTD.format = GLenum.GL_RGBA16F; //  DXGI_FORMAT_R16G16B16A16_FLOAT;
//                    gradMapTD.SampleDesc = kNoSample;
//                    gradMapTD.Usage = D3D11_USAGE_DEFAULT;
//                    gradMapTD.BindFlags = D3D11_BIND_SHADER_RESOURCE | D3D11_BIND_RENDER_TARGET;
//                    gradMapTD.CPUAccessFlags = 0;
//                    gradMapTD.MiscFlags = D3D11_RESOURCE_MISC_GENERATE_MIPS;

//                    ID3D11Texture2D* pD3D11Texture = NULL;
//                    V_RETURN(m_d3d._11.m_pd3d11Device->CreateTexture2D(&gradMapTD, NULL, &pD3D11Texture));
//                    V_RETURN(m_d3d._11.m_pd3d11Device->CreateShaderResourceView(pD3D11Texture, NULL, &cascade_states[cascade].m_d3d._11.m_pd3d11GradientMap[gpu_slot]));
//                    V_RETURN(m_d3d._11.m_pd3d11Device->CreateRenderTargetView(pD3D11Texture, NULL, &cascade_states[cascade].m_d3d._11.m_pd3d11GradientRenderTarget[gpu_slot]));
//                    SAFE_RELEASE(pD3D11Texture);

                    cascade_states[cascade].m_d3d._11.m_pd3d11GradientMap[gpu_slot] = cascade_states[cascade].m_d3d._11.m_pd3d11GradientRenderTarget[gpu_slot] =
                            TextureUtils.createTexture2D(gradMapTD, null);
                }
                break;
//                #endif
//                #if WAVEWORKS_ENABLE_GNM
//                case nv_water_d3d_api_gnm:
//                {
//                    int mips = 1;
//                    for(int pixels = dmap_dim; pixels >>= 1; ++mips)
//                        ;
//
//                    Gnm::DataFormat dataFormat = Gnm::kDataFormatR16G16B16A16Float;
//                    Gnm::TileMode tileMode;
//                    GpuAddress::computeSurfaceTileMode(&tileMode, GpuAddress::kSurfaceTypeRwTextureFlat, dataFormat, 1);
//                    #if 1
//                    Gnm::SizeAlign sizeAlign = cascade_states[cascade].m_d3d._gnm.m_gnmGradientMap[gpu_slot].initAs2d(dmap_dim, dmap_dim, mips, dataFormat, tileMode, SAMPLE_1);
//                    cascade_states[cascade].m_d3d._gnm.m_gnmGradientMap[gpu_slot].setBaseAddress(NVSDK_garlic_malloc(sizeAlign.m_size, sizeAlign.m_align));
//                    cascade_states[cascade].m_d3d._gnm.m_gnmGradientMap[gpu_slot].setResourceMemoryType(Gnm::kResourceMemoryTypeGC);
//                    cascade_states[cascade].m_d3d._gnm.m_gnmGradientRenderTarget[gpu_slot].initFromTexture(&cascade_states[cascade].m_d3d._gnm.m_gnmGradientMap[gpu_slot], 0);

				/* testing...
				struct rgba { uint16_t r, g, b, a; };
				rgba* tmp = (rgba*)NVSDK_aligned_malloc(dmap_dim * dmap_dim * sizeof(rgba), 16);
				Gnm::Texture texture = cascade_states[cascade].m_d3d._gnm.m_gnmGradientMap[gpu_slot];
				for(uint32_t level=0, width = dmap_dim; width > 0; ++level, width >>= 1)
				{
					for(uint32_t j=0; j<width; ++j)
					{
						for(uint32_t i=0; i<width; ++i)
						{
							rgba color = {
								Gnmx::convertF32ToF16(i / (width - 1.0f)),
								Gnmx::convertF32ToF16(j / (width - 1.0f)),
								Gnmx::convertF32ToF16(level * 32.0f),
								Gnmx::convertF32ToF16(1.0f) };
							tmp[j*width + i] = color;
						}
					}
					GpuAddress::TilingParameters tp;
					tp.initFromTexture(&texture, level, 0);
					uint64_t base;
					GpuAddress::computeTextureSurfaceOffsetAndSize(&base, (uint64_t*)0, &texture, level, 0);
					GpuAddress::tileSurface((rgba*)texture.getBaseAddress() + base / sizeof(rgba), tmp, &tp);
				}
				NVSDK_aligned_free(tmp);
				*/

//                    #else // try the other way around....
//                    Gnm::SizeAlign sizeAlign = cascade_states[cascade].m_d3d._gnm.m_gnmGradientRenderTarget[gpu_slot].init(dmap_dim, dmap_dim, 1, dataFormat, tileMode, Gnm::kNumSamples1, Gnm::kNumFragments1, NULL, NULL);
//                    cascade_states[cascade].m_d3d._gnm.m_gnmGradientRenderTarget[gpu_slot].setAddresses(NVSDK_garlic_malloc(sizeAlign.m_size, sizeAlign.m_align), NULL, NULL);
//                    cascade_states[cascade].m_d3d._gnm.m_gnmGradientMap[gpu_slot].initFromRenderTarget(&cascade_states[cascade].m_d3d._gnm.m_gnmGradientRenderTarget[gpu_slot], false);
//                    #endif
//                    // cascade_states[cascade].m_d3d._gnm.m_gnmGradientMap[gpu_slot].setResourceMemoryType(Gnm::kResourceMemoryTypeRO); // we never write to this texture from a shader, so it's OK to mark the texture as read-only.
//                }
//                break;
//                #endif
//                #if WAVEWORKS_ENABLE_GL
                case nv_water_d3d_api_gl2:
                {
                    int framebuffer_binding_result = 0;
                    cascade_states[cascade].m_d3d._GL2.m_GL2GradientMap[gpu_slot] = gl.glGenTexture();
                    gl.glBindTexture(GLenum.GL_TEXTURE_2D, cascade_states[cascade].m_d3d._GL2.m_GL2GradientMap[gpu_slot]);
                    gl.glTexImage2D(GLenum.GL_TEXTURE_2D, 0, GLenum.GL_RGBA32F, dmap_dim, dmap_dim, 0, GLenum.GL_RGBA, GLenum.GL_FLOAT, null);
                    // do not allocate memory for gradient maps' mipmaps if texture arrays for gradient maps are used
                    if(m_params.use_texture_arrays == false)
                    {
                        gl.glGenerateMipmap(GLenum.GL_TEXTURE_2D);
                    }
                    cascade_states[cascade].m_d3d._GL2.m_GL2GradientFBO[gpu_slot] = gl.glGenFramebuffer();
                    gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, cascade_states[cascade].m_d3d._GL2.m_GL2GradientFBO[gpu_slot]);
                    gl.glFramebufferTexture2D(GLenum.GL_FRAMEBUFFER, GLenum.GL_COLOR_ATTACHMENT0, GLenum.GL_TEXTURE_2D, cascade_states[cascade].m_d3d._GL2.m_GL2GradientMap[gpu_slot], 0);
                    framebuffer_binding_result = gl.glCheckFramebufferStatus(GLenum.GL_FRAMEBUFFER);
                    if(framebuffer_binding_result != GLenum.GL_FRAMEBUFFER_COMPLETE) return HRESULT.E_FAIL;
                    gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, 0);
                }
                break;
//                #endif
                case nv_water_d3d_api_none:
                    break;
                default:
                    // Unexpected API
                    return HRESULT.E_FAIL;
            }
            cascade_states[cascade].m_gradient_map_needs_clear[gpu_slot] = true;
        }

        switch(m_d3dAPI)
        {
//            #if WAVEWORKS_ENABLE_D3D11
            case nv_water_d3d_api_d3d11:
            {
                Texture2DDesc foamenergyTD = new Texture2DDesc();
                foamenergyTD.width = dmap_dim;
                foamenergyTD.height = dmap_dim;
                foamenergyTD.mipLevels = 1;
                foamenergyTD.arraySize = 1;
                foamenergyTD.format = GLenum.GL_R16F;
//                foamenergyTD.SampleDesc = kNoSample;
//                foamenergyTD.Usage = D3D11_USAGE_DEFAULT;
//                foamenergyTD.BindFlags = D3D11_BIND_SHADER_RESOURCE | D3D11_BIND_RENDER_TARGET;
//                foamenergyTD.CPUAccessFlags = 0;
//                foamenergyTD.MiscFlags = 0;

//                ID3D11Texture2D* pD3D11FoamEnergyTexture = NULL;
//                V_RETURN(m_d3d._11.m_pd3d11Device->CreateTexture2D(&foamenergyTD, NULL, &pD3D11FoamEnergyTexture));
//                V_RETURN(m_d3d._11.m_pd3d11Device->CreateShaderResourceView(pD3D11FoamEnergyTexture, NULL, &cascade_states[cascade].m_d3d._11.m_pd3d11FoamEnergyMap));
//                V_RETURN(m_d3d._11.m_pd3d11Device->CreateRenderTargetView(pD3D11FoamEnergyTexture, NULL, &cascade_states[cascade].m_d3d._11.m_pd3d11FoamEnergyRenderTarget));
//                SAFE_RELEASE(pD3D11FoamEnergyTexture);

                cascade_states[cascade].m_d3d._11.m_pd3d11FoamEnergyMap = cascade_states[cascade].m_d3d._11.m_pd3d11FoamEnergyRenderTarget =
                        TextureUtils.createTexture2D(foamenergyTD, null);
            }
            break;
            /*#endif
            #if WAVEWORKS_ENABLE_GNM
            case nv_water_d3d_api_gnm:
            {
                Gnm::DataFormat dataFormat = Gnm::kDataFormatR16Float;
                Gnm::TileMode tileMode;
                GpuAddress::computeSurfaceTileMode(&tileMode, GpuAddress::kSurfaceTypeColorTarget, dataFormat, 1);
                #if 1
                Gnm::SizeAlign sizeAlign = cascade_states[cascade].m_d3d._gnm.m_gnmFoamEnergyMap.initAs2d(dmap_dim, dmap_dim, 1, dataFormat, tileMode, SAMPLE_1);
                cascade_states[cascade].m_d3d._gnm.m_gnmFoamEnergyMap.setBaseAddress(NVSDK_garlic_malloc(sizeAlign.m_size, sizeAlign.m_align));
                cascade_states[cascade].m_d3d._gnm.m_gnmFoamEnergyMap.setResourceMemoryType(Gnm::kResourceMemoryTypeGC);
                cascade_states[cascade].m_d3d._gnm.m_gnmFoamEnergyRenderTarget.initFromTexture(&cascade_states[cascade].m_d3d._gnm.m_gnmFoamEnergyMap, SAMPLE_1);
                #else // try the other way around....
                Gnm::SizeAlign sizeAlign = cascade_states[cascade].m_d3d._gnm.m_gnmFoamEnergyRenderTarget.init(dmap_dim, dmap_dim, 1, dataFormat, tileMode, Gnm::kNumSamples1, Gnm::kNumFragments1, NULL, NULL);
                cascade_states[cascade].m_d3d._gnm.m_gnmFoamEnergyRenderTarget.setAddresses(NVSDK_garlic_malloc(sizeAlign.m_size, sizeAlign.m_align), NULL, NULL);
                cascade_states[cascade].m_d3d._gnm.m_gnmFoamEnergyMap.initFromRenderTarget(&cascade_states[cascade].m_d3d._gnm.m_gnmFoamEnergyRenderTarget, false);
                #endif
                cascade_states[cascade].m_d3d._gnm.m_gnmFoamEnergyMap.setResourceMemoryType(Gnm::kResourceMemoryTypeRO); // we never write to this texture from a shader, so it's OK to mark the texture as read-only.
            }
            break;
            #endif
            #if WAVEWORKS_ENABLE_GL*/
            case nv_water_d3d_api_gl2:
            {
                int framebuffer_binding_result = 0;
                cascade_states[cascade].m_d3d._GL2.m_GL2FoamEnergyMap = gl.glGenTexture();
                gl.glBindTexture(GLenum.GL_TEXTURE_2D, cascade_states[cascade].m_d3d._GL2.m_GL2FoamEnergyMap);
                gl.glTexImage2D(GLenum.GL_TEXTURE_2D, 0, GLenum.GL_R32F, dmap_dim, dmap_dim, 0, GLenum.GL_RED, GLenum.GL_FLOAT, null);
                cascade_states[cascade].m_d3d._GL2.m_GL2FoamEnergyFBO = gl.glGenFramebuffer();
                gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, cascade_states[cascade].m_d3d._GL2.m_GL2FoamEnergyFBO);
                gl.glFramebufferTexture2D(GLenum.GL_FRAMEBUFFER, GLenum.GL_COLOR_ATTACHMENT0, GLenum.GL_TEXTURE_2D, cascade_states[cascade].m_d3d._GL2.m_GL2FoamEnergyMap, 0);
                framebuffer_binding_result = gl.glCheckFramebufferStatus(GLenum.GL_FRAMEBUFFER);
                if(framebuffer_binding_result != GLenum.GL_FRAMEBUFFER_COMPLETE) return HRESULT.E_FAIL;
                gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, 0);
            }
            break;
//            #endif
            case nv_water_d3d_api_none:
                break;
            default:
                // Unexpected API
                return HRESULT.E_FAIL;
        }

//        #endif // WAVEWORKS_ENABLE_GRAPHICS
        cascade_states[cascade].m_gradient_map_version = GFSDK_WaveWorks.GFSDK_WaveWorks_InvalidKickID;

        return HRESULT.S_OK;
    }

    private void releaseSimulation(int cascade){
        m_pSimulationManager.releaseSimulation(cascade_states[cascade].m_pFFTSimulation);
        cascade_states[cascade].m_pFFTSimulation = null;
    }

    private HRESULT allocateSimulation(int cascade){
        NVWaveWorks_FFT_Simulation pFFTSim = m_pSimulationManager!=null ? m_pSimulationManager.createSimulation(m_params.cascades[cascade]) : null;
        cascade_states[cascade].m_pFFTSimulation = pFFTSim;
        if(pFFTSim != null) {
            switch(m_d3dAPI)
            {
//                #if WAVEWORKS_ENABLE_D3D11
                case nv_water_d3d_api_d3d11:
                    return pFFTSim.initD3D11(/*m_d3d._11.m_pd3d11Device*/) ? HRESULT.S_OK : HRESULT.E_FAIL;
//                #endif
//                #if WAVEWORKS_ENABLE_GNM
                case nv_water_d3d_api_gnm:
                    return pFFTSim.initGnm()? HRESULT.S_OK : HRESULT.E_FAIL;
//                #endif
//                #if WAVEWORKS_ENABLE_GL
                case nv_water_d3d_api_gl2:
                    return pFFTSim.initGL2(/*m_d3d._GL2.m_pGLContext*/)? HRESULT.S_OK : HRESULT.E_FAIL;
//                #endif
                case nv_water_d3d_api_none:
                    return pFFTSim.initNoGraphics()? HRESULT.S_OK : HRESULT.E_FAIL;
                default:
                    return HRESULT.E_FAIL;
            }
        } else {
            return HRESULT.E_FAIL;
        }
    }

    private void releaseSimulationManager(){
        if(m_pSimulationManager != null){
            m_pSimulationManager.dispose();
            m_pSimulationManager = null;
        }
    }
    private HRESULT allocateSimulationManager(){
        switch(m_params.simulation_api)
        {
//            #ifdef SUPPORT_CUDA
//            case nv_water_simulation_api_cuda:
//                m_pSimulationManager = new NVWaveWorks_FFT_Simulation_Manager_CUDA_Impl();
//                break;
//            #endif
//            #ifdef SUPPORT_FFTCPU
//            case nv_water_simulation_api_cpu:
//                m_pSimulationManager = new NVWaveWorks_FFT_Simulation_Manager_CPU_Impl(m_params,m_pOptionalScheduler);
//                break;
//            #endif
//            #ifdef SUPPORT_DIRECTCOMPUTE
            case nv_water_simulation_api_direct_compute:
                m_pSimulationManager = new NVWaveWorks_FFT_Simulation_Manager_DirectCompute_Impl();
                break;
//            #endif
            default:
                return HRESULT.E_FAIL;
        }

        if(m_pSimulationManager != null) {
            switch(m_d3dAPI)
            {
//                #if WAVEWORKS_ENABLE_D3D11
                case nv_water_d3d_api_d3d11:
                    return m_pSimulationManager.initD3D11(/*m_d3d._11.m_pd3d11Device*/);
//                #endif
//                #if WAVEWORKS_ENABLE_GNM
//                case nv_water_d3d_api_gnm:
//                    return m_pSimulationManager->initGnm();
//                #endif
//                #if WAVEWORKS_ENABLE_GL
                case nv_water_d3d_api_gl2:
                    return m_pSimulationManager.initGL2(/*m_d3d._GL2.m_pGLContext*/);
//                #endif
                case nv_water_d3d_api_none:
                    return m_pSimulationManager.initNoGraphics();
                default:
                    return HRESULT.E_FAIL;
            }
        } else {
            return HRESULT.E_FAIL;
        }
    }

    private void releaseGFXTimer(){
        if(m_pGFXTimer != null)
        {
            m_pGFXTimer.releaseAll();
            m_pGFXTimer = null;
        }
    }

    private HRESULT allocateGFXTimer(){
        releaseGFXTimer();

        if(!m_params.enable_gfx_timers)
            return HRESULT.S_OK;						// Timers not permitted by settings

        if(nv_water_d3d_api_none == m_d3dAPI)
            return HRESULT.S_OK;						// No GFX, no timers

//        #if WAVEWORKS_ENABLE_GRAPHICS
        if(nv_water_d3d_api_gnm != m_d3dAPI)
        {
            m_pGFXTimer = new NVWaveWorks_GFX_Timer_Impl();
        }

        m_gpu_kick_timers.reset();
        m_gpu_wait_timers.reset();

        switch(m_d3dAPI)
        {
//            #if WAVEWORKS_ENABLE_D3D11
            case nv_water_d3d_api_d3d11:
                return m_pGFXTimer.initD3D11(/*m_d3d._11.m_pd3d11Device*/)? HRESULT.S_OK : HRESULT.E_FAIL;
//            #endif
//            #if WAVEWORKS_ENABLE_GNM
//            case nv_water_d3d_api_gnm:
//                return m_pGFXTimer->initGnm();
//            #endif
//            #if WAVEWORKS_ENABLE_GL
            case nv_water_d3d_api_gl2:
                return m_pGFXTimer.initGL2(/*m_d3d._GL2.m_pGLContext*/)? HRESULT.S_OK : HRESULT.E_FAIL;
//            #endif
            default:
                return E_FAIL;
        }
//        #else// WAVEWORKS_ENABLE_GRAPHICS
//        return E_FAIL;
//        #endif // WAVEWORKS_ENABLE_GRAPHICS
    }

    private HRESULT consumeAvailableTimerSlot(/*Graphics_Context* pGC,*/ NVWaveWorks_GFX_Timer_Impl pGFXTimer, TimerPool pool, TimerSlot[] ppSlot){
        if(pool.m_active_timer_slot == pool.m_end_inflight_timer_slots)
        {
            // No slots available - we must wait for the oldest in-flight timer to complete
            int wait_slot = (pool.m_active_timer_slot + 1) % NumTimerSlots;
//            TimerSlot* pWaitSlot = pool.m_timer_slots + wait_slot;
            TimerSlot pWaitSlot = pool.m_timer_slots[wait_slot];

            if(NVWaveWorks_GFX_Timer_Impl.InvalidQueryIndex != pWaitSlot.m_DisjointQueryIndex)
            {
                long[] t_gfx = new long[1];
                pGFXTimer.waitTimerQueries(pWaitSlot.m_StartGFXQueryIndex, pWaitSlot.m_StopGFXQueryIndex, t_gfx);

                long[] t_update = new long[1];
                pGFXTimer.waitTimerQueries(pWaitSlot.m_StartQueryIndex, pWaitSlot.m_StopQueryIndex, t_update);

                long[] f = new long[1];
                pGFXTimer.waitDisjointQuery(pWaitSlot.m_DisjointQueryIndex, f);
                GLCheck.checkError();
                if(f[0] > 0)
                {
                    pWaitSlot.m_elapsed_gfx_time = 1000.f * (t_gfx[0])/(f[0]);
                    pWaitSlot.m_elapsed_time = 1000.f * (t_update[0])/(f[0]);
                }

                pGFXTimer.releaseDisjointQuery(pWaitSlot.m_DisjointQueryIndex);
                pGFXTimer.releaseTimerQuery(pWaitSlot.m_StartGFXQueryIndex);
                pGFXTimer.releaseTimerQuery(pWaitSlot.m_StopGFXQueryIndex);
                pGFXTimer.releaseTimerQuery(pWaitSlot.m_StartQueryIndex);
                pGFXTimer.releaseTimerQuery(pWaitSlot.m_StopQueryIndex);
                GLCheck.checkError();
            }

            pool.m_active_timer_slot = wait_slot;
        }

        // Consume a slot!
        ppSlot[0] = pool.m_timer_slots[pool.m_end_inflight_timer_slots];
        ppSlot[0].m_elapsed_gfx_time = 0.f;
        ppSlot[0].m_elapsed_time = 0.f;
        ppSlot[0].m_DisjointQueryIndex = pGFXTimer.getCurrentDisjointQuery();
        pool.m_end_inflight_timer_slots = (pool.m_end_inflight_timer_slots + 1) % NumTimerSlots;

        GLCheck.checkError();
        return HRESULT.S_OK;
    }

    private HRESULT queryTimers(/*Graphics_Context* pGC,*/ NVWaveWorks_GFX_Timer_Impl pGFXTimer, TimerPool pool){
        HRESULT hr;

        final int wait_slot = (pool.m_active_timer_slot + 1) % NumTimerSlots;
        long[] tdiff = new long[1];

        // Just consume one timer result per check
        if(wait_slot != pool.m_end_inflight_timer_slots)
        {
//            TimerSlot* pWaitSlot = pool.m_timer_slots + wait_slot;
            TimerSlot pWaitSlot = pool.m_timer_slots[wait_slot];
            if(NVWaveWorks_GFX_Timer_Impl.InvalidQueryIndex != pWaitSlot.m_DisjointQueryIndex)
            {
                long t_gfx;
                hr = pGFXTimer.getTimerQueries(pWaitSlot.m_StartGFXQueryIndex, pWaitSlot.m_StopGFXQueryIndex, tdiff);  t_gfx = tdiff[0];
                if(hr == HRESULT.S_FALSE)
                    return HRESULT.S_OK;

                long t_update;
                hr = pGFXTimer.getTimerQueries(pWaitSlot.m_StartQueryIndex, pWaitSlot.m_StopQueryIndex, tdiff);  t_update = tdiff[0];
                if(hr == HRESULT.S_FALSE)
                    return HRESULT.S_OK;

                long f;
                hr = pGFXTimer.getDisjointQuery(pWaitSlot.m_DisjointQueryIndex, tdiff);  f = tdiff[0];
                if(hr == HRESULT.S_FALSE)
                    return HRESULT.S_OK;

                if(f > 0)
                {
                    pWaitSlot.m_elapsed_gfx_time = 1000.f * t_gfx/f;
                    pWaitSlot.m_elapsed_time = 1000.f * t_update/f;
                }

                pGFXTimer.releaseDisjointQuery(pWaitSlot.m_DisjointQueryIndex);
                pGFXTimer.releaseTimerQuery(pWaitSlot.m_StartGFXQueryIndex);
                pGFXTimer.releaseTimerQuery(pWaitSlot.m_StopGFXQueryIndex);
                pGFXTimer.releaseTimerQuery(pWaitSlot.m_StartQueryIndex);
                pGFXTimer.releaseTimerQuery(pWaitSlot.m_StopQueryIndex);
            }

            pool.m_active_timer_slot = wait_slot;
        }
        GLCheck.checkError();
        return HRESULT.S_OK;
    }

    private HRESULT queryAllGfxTimers(/*Graphics_Context* pGC,*/ NVWaveWorks_GFX_Timer_Impl pGFXTimer){
        HRESULT hr;

        hr = queryTimers(pGFXTimer, m_gpu_kick_timers); if(hr != HRESULT.S_OK) return hr;
        hr = queryTimers(pGFXTimer, m_gpu_wait_timers); if(hr != HRESULT.S_OK) return hr;

        return HRESULT.S_OK;
    }

//    private int compileGLShader(const char *text, GLenum type);
//    private int loadGLProgram(const char* vstext, const char* tetext, const char* tctext,  const char* gstext, const char* fstext);

    // Timer query ring-buffer
    private static final class TimerSlot
    {
        int m_DisjointQueryIndex;
        int m_StartQueryIndex;
        int m_StopQueryIndex;
        int m_StartGFXQueryIndex;
        int m_StopGFXQueryIndex;
        float m_elapsed_time;			// in milli-seconds, as per house style
        float m_elapsed_gfx_time;		// in milli-seconds, as per house style

        void zeros(){
            m_DisjointQueryIndex = 0;
            m_StartQueryIndex = 0;
            m_StopQueryIndex = 0;
            m_StartGFXQueryIndex = 0;
            m_StopGFXQueryIndex = 0;
            m_elapsed_time = 0;
            m_elapsed_gfx_time = 0;
        }
    }

    private static final class TimerPool
    {
        int m_active_timer_slot;			// i.e. not in-flight
        int m_end_inflight_timer_slots;		// the first in-flight slot is always the one after active
        TimerSlot[] m_timer_slots = new TimerSlot[NumTimerSlots];

        TimerPool(){
            for(int i = 0; i < m_timer_slots.length; i++)
                m_timer_slots[i] = new TimerSlot();
        }

        void reset(){
            m_active_timer_slot = 0;
            m_end_inflight_timer_slots = 1;
            for(int i = 0; i < m_timer_slots.length; i++)
                m_timer_slots[i].zeros();
        }
    }

    private static final class D3D11Objects
    {
        Texture2D[] m_pd3d11GradientMap=new Texture2D[GFSDK_WaveWorks.MaxNumGPUs];			// (ABGR16F) - round-robin, to avoid SLI-inteframe dependencies
        Texture2D[] m_pd3d11GradientRenderTarget=new Texture2D[GFSDK_WaveWorks.MaxNumGPUs];	// (ditto)
        Texture2D m_pd3d11FoamEnergyMap;		// (R16F)
        Texture2D   m_pd3d11FoamEnergyRenderTarget;// (ditto)
    };

    private static final class GL2Objects
    {
        int[] m_GL2GradientMap=new int[GFSDK_WaveWorks.MaxNumGPUs];			// (ABGR16F) - round-robin, to avoid SLI-inteframe dependencies
        int[] m_GL2GradientFBO=new int[GFSDK_WaveWorks.MaxNumGPUs];			// (ditto)
        int m_GL2FoamEnergyMap;					// (R16F)
        int m_GL2FoamEnergyFBO;					// (ditto)
    };

    private static final class Union{
        final D3D11Objects _11=new D3D11Objects();
        final GL2Objects _GL2=new GL2Objects();
    }

    private static final class CascadeState{
        NVWaveWorks_Mesh m_pQuadMesh;
        NVWaveWorks_FFT_Simulation m_pFFTSimulation;

        // The kickID that originated the last update to this displacement map, allowing us to track when
        // the map is out of date and needs another update...
        long m_gradient_map_version;

        // Set when the gradient map is newly created and therefore in need of an intitial clear
        boolean[] m_gradient_map_needs_clear=new boolean[GFSDK_WaveWorks.MaxNumGPUs];

        final Union m_d3d = new Union();
    }

    private static final class D3D11GlobalObjects
    {
//        ID3D11Device* m_pd3d11Device;

        // Shaders for grad calc
        GLSLProgram m_pd3d11GradCalcProgram;
//        ID3D11PixelShader* m_pd3d11GradCalcPS;
        int m_pd3d11GradCalcPixelShaderCB;
        int m_pd3d11PointSampler;
        Runnable m_pd3d11NoDepthStencil;
        Runnable m_pd3d11AlwaysSolidRasterizer;
        Runnable m_pd3d11CalcGradBlendState;
        Runnable m_pd3d11AccumulateFoamBlendState;
        Runnable m_pd3d11WriteAccumulatedFoamBlendState;

        // State for main rendering
        int m_pd3d11LinearNoMipSampler;
        int m_pd3d11GradMapSampler;
        int		m_pd3d11PixelShaderCB;
        int		m_pd3d11VertexDomainShaderCB;

        // Shaders for foam generation
        GLSLProgram m_pd3d11FoamGenProgram;
//        ID3D11PixelShader* m_pd3d11FoamGenPS;
        int m_pd3d11FoamGenPixelShaderCB;
    };

    private static final class GL2GlobalObjects
    {
//        void* m_pGLContext;

        // Shaders for grad calc
        GLSLProgram m_GradCalcProgram;
        // Uniform binding points for grad calc shader
        int m_GradCalcUniformLocation_Scales;
        int m_GradCalcUniformLocation_OneLeft;
        int m_GradCalcUniformLocation_OneRight;
        int m_GradCalcUniformLocation_OneBack;
        int m_GradCalcUniformLocation_OneFront;
        int m_GradCalcTextureBindLocation_DisplacementMap;
        int m_GradCalcTextureUnit_DisplacementMap;
        // Vertex attribute locations
        int m_GradCalcAttributeLocation_Pos;
        int m_GradCalcAttributeLocation_TexCoord;

        // Shaders for foam generation
        GLSLProgram m_FoamGenProgram;
        // Uniform binding points for foam generation shader
        int m_FoamGenUniformLocation_DissipationFactors;
        int m_FoamGenUniformLocation_SourceComponents;
        int m_FoamGenUniformLocation_UVOffsets;
        int m_FoamGenTextureBindLocation_EnergyMap;
        int m_FoamGenTextureUnit_EnergyMap;
        // Vertex attribute locations
        int m_FoamGenAttributeLocation_Pos;
        int m_FoamGenAttributeLocation_TexCoord;

        // Texture arrays & FBO needed to blit to those
        int m_DisplacementsTextureArray;
        int m_GradientsTextureArray;
        int m_TextureArraysBlittingReadFBO;
        int m_TextureArraysBlittingDrawFBO;
    }

    private final static class UnionGlobal{
        D3D11GlobalObjects _11;
        GL2GlobalObjects   _GL2;
    }

    private final static class ps_calcgradient_cbuffer{
        static final int SIZE = 5 * Vector4f.SIZE;

        float g_ChoppyScale;
        float g_GradMap2TexelWSScale;
        float pad1;
        float pad2;

        final Vector4f g_OneTexel_Left = new Vector4f();
        final Vector4f g_OneTexel_Right = new Vector4f();
        final Vector4f g_OneTexel_Back = new Vector4f();
        final Vector4f g_OneTexel_Front = new Vector4f();
        
        ByteBuffer store(ByteBuffer buf){
            buf.putFloat(g_ChoppyScale);
            buf.putFloat(g_GradMap2TexelWSScale);
            buf.putFloat(pad1);
            buf.putFloat(pad2);

            g_OneTexel_Left.store(buf);
            g_OneTexel_Right.store(buf);
            g_OneTexel_Back.store(buf);
            g_OneTexel_Front.store(buf);
            
            return buf;
        }

        @Override
        public String toString() {
            final StringBuffer sb = new StringBuffer("ps_calcgradient_cbuffer{\n");
            sb.append("g_ChoppyScale=").append(g_ChoppyScale);
            sb.append("\n g_GradMap2TexelWSScale=").append(g_GradMap2TexelWSScale);
            sb.append("\n g_OneTexel_Left=").append(g_OneTexel_Left);
            sb.append("\n g_OneTexel_Right=").append(g_OneTexel_Right);
            sb.append("\n g_OneTexel_Back=").append(g_OneTexel_Back);
            sb.append("\n g_OneTexel_Front=").append(g_OneTexel_Front);
            sb.append('}');
            return sb.toString();
        }
    }

    private static final class vs_attr_cbuffer
    {
        static final int SIZE = 2 * Vector4f.SIZE;
        final Vector3f g_WorldEye = new Vector3f();
        float pad1;
        float[] g_UVScaleCascade0123 = new float[4];

        ByteBuffer store(ByteBuffer buf){
            g_WorldEye.store(buf);
            buf.putFloat(pad1);

            for(int i = 0; i < g_UVScaleCascade0123.length;i++)
                buf.putFloat(g_UVScaleCascade0123[i]);

            return buf;
        }

    };

    private static final class ps_foamgeneration_cbuffer
    {
        static final int SIZE = 3 * Vector4f.SIZE;
        float g_DissipationFactors_BlurExtents;
        float g_DissipationFactors_Fadeout;
        float g_DissipationFactors_Accumulation;
        float g_FoamGenerationThreshold;
        final Vector4f g_SourceComponents = new Vector4f();
        final Vector4f g_UVOffsets = new Vector4f();

        ByteBuffer store(ByteBuffer buf){
            buf.putFloat(g_DissipationFactors_BlurExtents);
            buf.putFloat(g_DissipationFactors_Fadeout);
            buf.putFloat(g_DissipationFactors_Accumulation);
            buf.putFloat(g_FoamGenerationThreshold);

            g_SourceComponents.store(buf);
            g_UVOffsets.store(buf);

            return buf;
        }
    };

    private static final class ps_attr_cbuffer
    {
        static final int SIZE = 3 * Vector4f.SIZE;

        float g_TexelLength_x2_PS;
        float g_Cascade1Scale_PS;
        float g_Cascade1TexelScale_PS;
        float g_Cascade1UVOffset_PS;

        float g_Cascade2Scale_PS;
        float g_Cascade2TexelScale_PS;
        float g_Cascade2UVOffset_PS;
        float g_Cascade3Scale_PS;

        float g_Cascade3TexelScale_PS;
        float g_Cascade3UVOffset_PS;
        float pad1;
        float pad2;

        ByteBuffer store(ByteBuffer buf){
            buf.putFloat(g_TexelLength_x2_PS);
            buf.putFloat(g_Cascade1Scale_PS);
            buf.putFloat(g_Cascade1TexelScale_PS);
            buf.putFloat(g_Cascade1UVOffset_PS);

            buf.putFloat(g_Cascade2Scale_PS);
            buf.putFloat(g_Cascade2TexelScale_PS);
            buf.putFloat(g_Cascade2UVOffset_PS);
            buf.putFloat(g_Cascade3Scale_PS);

            buf.putFloat(g_Cascade3TexelScale_PS);
            buf.putFloat(g_Cascade3UVOffset_PS);
            buf.putFloat(pad1);
            buf.putFloat(pad2);

            return buf;
        }
    }

    private static final int
        ShaderInputGL2_g_textureBindLocationDisplacementMap0 = 0,
        ShaderInputGL2_g_textureBindLocationDisplacementMap1 = 1,
        ShaderInputGL2_g_textureBindLocationDisplacementMap2 = 2,
        ShaderInputGL2_g_textureBindLocationDisplacementMap3 = 3,
        ShaderInputGL2_g_textureBindLocationGradientMap0 = 4,
        ShaderInputGL2_g_textureBindLocationGradientMap1 = 5,
        ShaderInputGL2_g_textureBindLocationGradientMap2 = 6,
        ShaderInputGL2_g_textureBindLocationGradientMap3 = 7,
        ShaderInputGL2_g_textureBindLocationDisplacementMapArray = 8,
        ShaderInputGL2_g_textureBindLocationGradientMapArray = 9,
        ShaderInputGL2_g_WorldEye = 10,
        ShaderInputGL2_g_UseTextureArrays =11,
        ShaderInputGL2_g_UVScaleCascade0123 =12,
        ShaderInputGL2_g_TexelLength_x2_PS =13,
        ShaderInputGL2_g_Cascade1Scale_PS =14,
        ShaderInputGL2_g_Cascade1TexelScale_PS = 15,
        ShaderInputGL2_g_Cascade1UVOffset_PS = 16,
        ShaderInputGL2_g_Cascade2Scale_PS=17,
        ShaderInputGL2_g_Cascade2TexelScale_PS=18,
        ShaderInputGL2_g_Cascade2UVOffset_PS=19,
        ShaderInputGL2_g_Cascade3Scale_PS=20,
        ShaderInputGL2_g_Cascade3TexelScale_PS=21,
        ShaderInputGL2_g_Cascade3UVOffset_PS=22,
        NumShaderInputsGL2=23;

    private static final int
        ShaderInputD3D11_vs_buffer = 0,
        ShaderInputD3D11_vs_g_samplerDisplacementMap0=1,
        ShaderInputD3D11_vs_g_samplerDisplacementMap1=2,
        ShaderInputD3D11_vs_g_samplerDisplacementMap2=3,
        ShaderInputD3D11_vs_g_samplerDisplacementMap3=4,
        ShaderInputD3D11_vs_g_textureDisplacementMap0=5,
        ShaderInputD3D11_vs_g_textureDisplacementMap1=6,
        ShaderInputD3D11_vs_g_textureDisplacementMap2=7,
        ShaderInputD3D11_vs_g_textureDisplacementMap3=8,
        ShaderInputD3D11_ds_buffer=9,
        ShaderInputD3D11_ds_g_samplerDisplacementMap0=10,
        ShaderInputD3D11_ds_g_samplerDisplacementMap1=11,
        ShaderInputD3D11_ds_g_samplerDisplacementMap2=12,
        ShaderInputD3D11_ds_g_samplerDisplacementMap3=13,
        ShaderInputD3D11_ds_g_textureDisplacementMap0=14,
        ShaderInputD3D11_ds_g_textureDisplacementMap1=15,
        ShaderInputD3D11_ds_g_textureDisplacementMap2=16,
        ShaderInputD3D11_ds_g_textureDisplacementMap3=17,
        ShaderInputD3D11_ps_buffer=18,
        ShaderInputD3D11_g_samplerGradientMap0=19,
        ShaderInputD3D11_g_samplerGradientMap1=20,
        ShaderInputD3D11_g_samplerGradientMap2=21,
        ShaderInputD3D11_g_samplerGradientMap3=22,
        ShaderInputD3D11_g_textureGradientMap0=23,
        ShaderInputD3D11_g_textureGradientMap1=24,
        ShaderInputD3D11_g_textureGradientMap2=25,
        ShaderInputD3D11_g_textureGradientMap3=26,
        NumShaderInputsD3D11=27;

    static final GFSDK_WaveWorks_ShaderInput_Desc ShaderInputDescsGL2[/*NumShaderInputsGL2*/] = {
        new GFSDK_WaveWorks_ShaderInput_Desc( GFSDK_WaveWorks_ShaderInput_Desc.GL_VertexShader_TextureBindLocation, "g_samplerDisplacementMap0", 0 ),
        new GFSDK_WaveWorks_ShaderInput_Desc( GFSDK_WaveWorks_ShaderInput_Desc.GL_VertexShader_TextureBindLocation, "g_samplerDisplacementMap1", 0 ),
        new GFSDK_WaveWorks_ShaderInput_Desc( GFSDK_WaveWorks_ShaderInput_Desc.GL_VertexShader_TextureBindLocation, "g_samplerDisplacementMap2", 0 ),
        new GFSDK_WaveWorks_ShaderInput_Desc( GFSDK_WaveWorks_ShaderInput_Desc.GL_VertexShader_TextureBindLocation, "g_samplerDisplacementMap3", 0 ),
        new GFSDK_WaveWorks_ShaderInput_Desc( GFSDK_WaveWorks_ShaderInput_Desc.GL_FragmentShader_TextureBindLocation, "g_samplerGradientMap0", 0 ),
        new GFSDK_WaveWorks_ShaderInput_Desc( GFSDK_WaveWorks_ShaderInput_Desc.GL_FragmentShader_TextureBindLocation, "g_samplerGradientMap1", 0 ),
        new GFSDK_WaveWorks_ShaderInput_Desc( GFSDK_WaveWorks_ShaderInput_Desc.GL_FragmentShader_TextureBindLocation, "g_samplerGradientMap2", 0 ),
        new GFSDK_WaveWorks_ShaderInput_Desc( GFSDK_WaveWorks_ShaderInput_Desc.GL_FragmentShader_TextureBindLocation, "g_samplerGradientMap3", 0 ),
        new GFSDK_WaveWorks_ShaderInput_Desc( GFSDK_WaveWorks_ShaderInput_Desc.GL_VertexShader_TextureArrayBindLocation, "g_samplerDisplacementMapTextureArray", 0 ),
        new GFSDK_WaveWorks_ShaderInput_Desc( GFSDK_WaveWorks_ShaderInput_Desc.GL_FragmentShader_TextureArrayBindLocation, "g_samplerGradientMapTextureArray", 0 ),
        new GFSDK_WaveWorks_ShaderInput_Desc( GFSDK_WaveWorks_ShaderInput_Desc.GL_VertexShader_UniformLocation, "g_WorldEye", 0 ),
        new GFSDK_WaveWorks_ShaderInput_Desc( GFSDK_WaveWorks_ShaderInput_Desc.GL_VertexShader_UniformLocation, "g_UseTextureArrays", 1 ),
        new GFSDK_WaveWorks_ShaderInput_Desc( GFSDK_WaveWorks_ShaderInput_Desc.GL_VertexShader_UniformLocation, "g_UVScaleCascade0123", 2 ),
        new GFSDK_WaveWorks_ShaderInput_Desc( GFSDK_WaveWorks_ShaderInput_Desc.GL_FragmentShader_UniformLocation, "g_TexelLength_x2_PS", 0 ),
        new GFSDK_WaveWorks_ShaderInput_Desc( GFSDK_WaveWorks_ShaderInput_Desc.GL_FragmentShader_UniformLocation, "g_Cascade1Scale_PS", 1 ),
        new GFSDK_WaveWorks_ShaderInput_Desc( GFSDK_WaveWorks_ShaderInput_Desc.GL_FragmentShader_UniformLocation, "g_Cascade1TexelScale_PS", 2 ),
        new GFSDK_WaveWorks_ShaderInput_Desc( GFSDK_WaveWorks_ShaderInput_Desc.GL_FragmentShader_UniformLocation, "g_Cascade1UVOffset_PS", 3 ),
        new GFSDK_WaveWorks_ShaderInput_Desc( GFSDK_WaveWorks_ShaderInput_Desc.GL_FragmentShader_UniformLocation, "g_Cascade2Scale_PS", 4 ),
        new GFSDK_WaveWorks_ShaderInput_Desc( GFSDK_WaveWorks_ShaderInput_Desc.GL_FragmentShader_UniformLocation, "g_Cascade2TexelScale_PS", 5 ),
        new GFSDK_WaveWorks_ShaderInput_Desc( GFSDK_WaveWorks_ShaderInput_Desc.GL_FragmentShader_UniformLocation, "g_Cascade2UVOffset_PS", 6 ),
        new GFSDK_WaveWorks_ShaderInput_Desc( GFSDK_WaveWorks_ShaderInput_Desc.GL_FragmentShader_UniformLocation, "g_Cascade3Scale_PS", 7 ),
        new GFSDK_WaveWorks_ShaderInput_Desc( GFSDK_WaveWorks_ShaderInput_Desc.GL_FragmentShader_UniformLocation, "g_Cascade3TexelScale_PS", 8 ),
        new GFSDK_WaveWorks_ShaderInput_Desc( GFSDK_WaveWorks_ShaderInput_Desc.GL_FragmentShader_UniformLocation, "g_Cascade3UVOffset_PS", 9 ),
    };
}
