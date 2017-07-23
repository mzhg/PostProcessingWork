package jet.opengl.demos.nvidia.waves;

/**
 * Created by Administrator on 2017/7/23 0023.
 */

final class GFSDK_WaveWorks_Simulation_Manager_Timings {
    // this struct is filled by simulation manager implementation
    float time_start_to_stop;		// time between starting the 1st thread's work and completing the last thread's work
    float time_total;				// sum of all time spent in worker threads doing actual work
    float time_wait_for_completion;	// time spent on waitTasksCompletion
}
