package jet.opengl.demos.nvidia.waves;

import org.lwjgl.util.vector.Vector2f;

/**
 * Created by mazhen'gui on 2017/7/21.
 */

final class GFSDK_WaveWorks_Detailed_Simulation_Params {
    static final int MaxNumCascades = 4;

    // The simulation params for one of the frequency cascades
    static final class Cascade
    {
        // Dimension of displacement texture (and, therefore, of the corresponding FFT step)
        int fft_resolution;

        // The repeat interval for the fft simulation, in world units
        float fft_period;

        // Simulation properties
        float time_scale;
        float wave_amplitude;
        final Vector2f wind_dir = new Vector2f();
        float wind_speed;
        float wind_dependency;
        float choppy_scale;
        float small_wave_fraction;

        // Should this cascade's displacement data be read back to the CPU?
        boolean readback_displacements;

        // How big to make the readback FIFO?
        int num_readback_FIFO_entries;

        // Window params for setting up this cascade's spectrum, measured in pixels from DC
        float window_in;
        float window_out;

        // the foam related parameters are per-cascade as these might require per-cascade tweaking inside the lib

        // the factor characterizing critical wave amplitude/shape/energy to start generating foam
        float foam_generation_threshold;
        // the amount of foam generated in such areas on each simulation step
        float foam_generation_amount;
        // the speed of foam spatial dissipation
        float foam_dissipation_speed;
        // the speed of foam dissipation over time
        float foam_falloff_speed;

        // whether to allow CUDA timers
        boolean enable_CUDA_timers;
    }

    // A maximum of 4 cascades is supported - the first cascade (cascades[0]) is taken
    // to be the highest spatial size cascade
    int num_cascades;
    Cascade[] cascades = new Cascade[MaxNumCascades];

    // The overall time scale for the simulation (FFT)
    float time_scale;

    // anisotropic degree for sampling of gradient maps
    int aniso_level;

    // # of GPUS (needed for foam simulation)
    int num_GPUs;

//    nv_water_simulation_api simulation_api;

//    GFSDK_WaveWorks_Simulation_CPU_Threading_Model CPU_simulation_threading_model;

    boolean use_texture_arrays;

    boolean enable_gfx_timers;

    boolean enable_CPU_timers;
}
