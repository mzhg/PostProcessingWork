package jet.opengl.demos.nvidia.waves;

/**
 * Controls the threading model when the CPU simulation path is used.<p>
 * Created by mazhen'gui on 2017/7/22.
 */

public enum GFSDK_WaveWorks_Simulation_CPU_Threading_Model {
    GFSDK_WaveWorks_Simulation_CPU_Threading_Model_None,		// Do not use worker threads
    GFSDK_WaveWorks_Simulation_CPU_Threading_Model_Automatic,	// Use an automatically-determined number of worker threads
    GFSDK_WaveWorks_Simulation_CPU_Threading_Model_1,			// Use 1 worker thread
    GFSDK_WaveWorks_Simulation_CPU_Threading_Model_2,			// Use 2 worker threads
    GFSDK_WaveWorks_Simulation_CPU_Threading_Model_3,			// Use 3 worker threads
    // etc...
    // i.e. it's safe to use higher values to represent even larger thread counts
}
