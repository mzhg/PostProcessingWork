package jet.opengl.demos.nvidia.waves;

import org.lwjgl.util.vector.Vector2f;

/**
 * Created by mazhen'gui on 2017/7/21.
 */

public final class GFSDK_WaveWorks_Detailed_Simulation_Params {
    public static final int MaxNumCascades = 4;

    /** The simulation params for one of the frequency cascades */
    public static final class Cascade
    {
        /** Dimension of displacement texture (and, therefore, of the corresponding FFT step) */
        public int fft_resolution;

        /** The repeat interval for the fft simulation, in world units */
        public float fft_period;

        // Simulation properties
        public float time_scale;
        public float wave_amplitude;
        public final Vector2f wind_dir = new Vector2f();
        public float wind_speed;
        public float wind_dependency;
        public float choppy_scale;
        public float small_wave_fraction;

        // Should this cascade's displacement data be read back to the CPU?
        public boolean readback_displacements;

        // How big to make the readback FIFO?
        public int num_readback_FIFO_entries;

        // Window params for setting up this cascade's spectrum, measured in pixels from DC
        public float window_in;
        public float window_out;

        // the foam related parameters are per-cascade as these might require per-cascade tweaking inside the lib

        // the factor characterizing critical wave amplitude/shape/energy to start generating foam
        public float foam_generation_threshold;
        // the amount of foam generated in such areas on each simulation step
        public float foam_generation_amount;
        // the speed of foam spatial dissipation
        public float foam_dissipation_speed;
        // the speed of foam dissipation over time
        public float foam_falloff_speed;

        // whether to allow CUDA timers
        public boolean enable_CUDA_timers;

        public void set(Cascade o){
            fft_resolution = o.fft_resolution;
            fft_period = o.fft_period;
            time_scale = o.time_scale;
            wave_amplitude = o.wave_amplitude;
            wind_dir.set(o.wind_dir);
            wind_speed = o.wind_speed;
            wind_dependency = o.wind_dependency;
            choppy_scale = o.choppy_scale;
            small_wave_fraction = o.small_wave_fraction;
            readback_displacements = o.readback_displacements;

            num_readback_FIFO_entries = o.num_readback_FIFO_entries;
            window_in = o.window_in;
            window_out = o.window_out;

            foam_generation_threshold = o.foam_generation_threshold;
            foam_generation_amount = o.foam_generation_amount;
            foam_dissipation_speed = o.foam_dissipation_speed;
            foam_falloff_speed = o.foam_falloff_speed;
            enable_CUDA_timers = o.enable_CUDA_timers;
        }
    }

    // A maximum of 4 cascades is supported - the first cascade (cascades[0]) is taken
    // to be the highest spatial size cascade
    public int num_cascades;
    public Cascade[] cascades = new Cascade[MaxNumCascades];

    // The overall time scale for the simulation (FFT)
    public float time_scale;

    // anisotropic degree for sampling of gradient maps
    public int aniso_level;

    // # of GPUS (needed for foam simulation)
    public int num_GPUs;

    public int simulation_api;

    public GFSDK_WaveWorks_Simulation_CPU_Threading_Model CPU_simulation_threading_model;

    public boolean use_texture_arrays;

    public boolean enable_gfx_timers;

    public boolean enable_CPU_timers;

    public GFSDK_WaveWorks_Detailed_Simulation_Params(){
        for(int i = 0; i < cascades.length;i++)
            cascades[i] = new Cascade();
    }
}
