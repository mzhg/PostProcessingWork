package jet.opengl.demos.nvidia.waves;

/**
 * Created by Administrator on 2017/7/23 0023.
 */

public class GFSDK_WaveWorks_Simulation_Stats {
    // the times spent on particular simulation tasks, measured in milliseconds (1e-3 sec)
    /** CPU time spent by main app thread waiting for CPU FFT simulation results using CPU */
    public float CPU_main_thread_wait_time;
    /** CPU time spent on CPU FFT simulation: time between 1st thread starts work and last thread finishes simulation work */
    public float CPU_threads_start_to_finish_time;
    /** CPU time spent on CPU FFT simulation: sum time spent in threads that perform simulation work */
    public float CPU_threads_total_time;
    /** GPU time spent on GPU simulation */
    public float GPU_simulation_time;
    /** GPU simulation time spent on FFT */
    public float GPU_FFT_simulation_time;
    /** GPU time spent on non-simulation e.g. updating gradient maps */
    public float GPU_gfx_time;
    /** Total GPU time spent on simulation workloads*/
    public float GPU_update_time;
}
