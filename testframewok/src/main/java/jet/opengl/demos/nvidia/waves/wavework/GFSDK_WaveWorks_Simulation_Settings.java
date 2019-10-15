package jet.opengl.demos.nvidia.waves.wavework;

/**
 * Created by Administrator on 2017/7/23 0023.
 */

public class GFSDK_WaveWorks_Simulation_Settings {
    /** The detail level of the simulation: this drives the resolution of the FFT and also determines whether the simulation workload is done
        on the GPU or CPU*/
    public GFSDK_WaveWorks_Simulation_DetailLevel detail_level;

    /** The repeat interval for the fft simulation, in world units*/
    public float fft_period;

    /** True if wind_speed in GFSDK_WaveWorks_Simulation_Params should accept Beaufort scale value
    False if wind_speed in GFSDK_WaveWorks_Simulation_Params should accept meters/second*/
    public boolean use_Beaufort_scale;

    /** Should the displacement data be read back to the CPU?*/
    public boolean readback_displacements;

    /** If readback is enabled, displacement data can be kept alive in a FIFO for historical lookups
     e.g. in order to implement predict/correct for a networked application*/
    public int num_readback_FIFO_entries;

    /** Set max aniso degree for sampling of gradient maps*/
    public int aniso_level;

    /** The threading model to use when the CPU simulation path is active
    Can be set to none (meaning: simulation is performed on the calling thread, synchronously), automatic, or even
    an explicitly specified thread count*/
    public GFSDK_WaveWorks_Simulation_CPU_Threading_Model CPU_simulation_threading_model;

    /** Number of GPUs used  */
    public int num_GPUs;

    /** Usage of texture arrays in GL */
    public boolean use_texture_arrays;

    /** Controls whether timer events will be used to gather stats on the CUDA simulation path
    // This can impact negatively on GPU/CPU parallelism, so it is recommended to enable this only when necessary */
    public boolean enable_CUDA_timers;

    /** Controls the use of graphics pipeline timers */
    public boolean enable_gfx_timers;

    /** Controls the use of CPU timers to gather profiling data */
    public boolean enable_CPU_timers;
}
