package jet.opengl.demos.nvidia.waves;

import org.lwjgl.util.vector.Vector2f;

import java.util.Random;

import jet.opengl.postprocessing.util.Numeric;

/**
 * Created by mazhen'gui on 2017/7/22.
 */

final class Simulation_Util {

    static final int MAX_NUM_CASCADES = 4;
    static final int MAX_FFT_RESOLUTION = 512;

    static final int gauss_map_resolution	= (MAX_FFT_RESOLUTION);
    static final int gauss_map_size		= ((gauss_map_resolution + 4) * (gauss_map_resolution + 1));
    static final float GRAV_ACCEL =	9.810f;

    static void init_gauss(GFSDK_WaveWorks_Detailed_Simulation_Params.Cascade params, float[] pOutGauss)
    {
        Random random = new Random();
        for (int i = 0; i <= gauss_map_resolution; i++)
        {
            for (int j = 0; j <= gauss_map_resolution; j++)
            {
                final int ix = i * (gauss_map_resolution + 4) + j;

                pOutGauss[2*ix+0] = (float) random.nextGaussian();
                pOutGauss[2*ix+1] = (float) random.nextGaussian();
            }
        }
    }

//    private interface  Functor{
//        void operator(int i, int j, int /* not used nx*/, int /* not used ny*/, Vector2f K);
//    }

    // Template algo for initializaing various aspects of simulation
    private static void for_each_wavevector(GFSDK_WaveWorks_Detailed_Simulation_Params.Cascade params, init_omega_functor functor)
    {
        final int dmap_dim = params.fft_resolution;
        final float fft_period = params.fft_period;
        Vector2f K = new Vector2f();

        for (int i = 0; i <= dmap_dim; i++)
        {
            // ny is y-coord wave number
            final int ny = (-dmap_dim/2 + i);

            // K is wave-vector, range [-|DX/W, |DX/W], [-|DY/H, |DY/H]
            K.y = ny * (2.0f * Numeric.PI / fft_period);

            for (int j = 0; j <= dmap_dim; j++)
            {
                // nx is x-coord wave number
                int nx = (-dmap_dim/2 + j);

                K.x = nx * (2.0f * Numeric.PI / fft_period);

                functor.operator(i,j,nx,ny,K);
            }
        }
    }

    private static final class init_omega_functor {
        int dmap_dim;
        float[] pOutOmega;

        void operator(int i, int j, int nx, int ny, Vector2f K) {
            // The angular frequency is following the dispersion relation:
            //            omega^2 = g*k
            // So the equation of Gerstner wave is:
            //            x = x0 - K/k * A * sin(dot(K, x0) - sqrt(g * k) * t), x is a 2D vector.
            //            z = A * cos(dot(K, x0) - sqrt(g * k) * t)
            // Gerstner wave means: a point on a simple sinusoid wave is doing a uniform circular motion. The
            // center is (x0, y0, z0), the radius is A, and the circle plane is parallel to K.
            pOutOmega[i * (dmap_dim + 4) + j] = (float) Math.sqrt(GRAV_ACCEL * Math.sqrt(K.x * K.x + K.y * K.y));
        }
    }

    static void init_omega(GFSDK_WaveWorks_Detailed_Simulation_Params.Cascade params, float[] pOutOmega)
    {
        init_omega_functor f = new init_omega_functor();
        f.dmap_dim = params.fft_resolution;
        f.pOutOmega = pOutOmega;

        for_each_wavevector(params, f);
    }

    // Upper-bound estimate of integral of Phillips Spectrum power over disc-shaped 2D wave vector space of radius k centred on K = {0,0}
// There is no wind velocity parameter, since the integral is rotationally invariant
//
    private static float UpperBoundPhillipsIntegral(float k, float v, float a, float dir_depend, float small_wave_fraction)
    {
        if(k <= 0.f) return 0.f;

        // largest possible wave from constant wind of velocity v
        float l = v * v / GRAV_ACCEL;

        // integral has analytic form, yay!
        double phillips_integ = 0.5f * Math.PI * a * l * l * Math.exp(-1.f/(k*k*l*l));

        // dir_depend affects half the domain
        phillips_integ *= (1.0f-0.5f*dir_depend);

        // we may safely ignore 'small_wave_fraction' for an upper-bound estimate
        return (float) phillips_integ;
    }

    static float get_spectrum_rms_sqr(GFSDK_WaveWorks_Detailed_Simulation_Params.Cascade params)
    {
        float a = params.wave_amplitude * params.wave_amplitude;
        float v = params.wind_speed;
        float dir_depend = params.wind_dependency;
        float fft_period = params.fft_period;

        float phil_norm = (float) (Math.exp(1)/fft_period);	// This normalization ensures that the simulation is invariant w.r.t. units and/or fft_period
        phil_norm *= phil_norm;					// Use the square as we are accumulating RMS

        // We can compute the integral of Phillips over a disc in wave vector space analytically, and by subtracting one
        // disc from the other we can compute the integral for the ring defined by {params.window_in,params.window_out}
        final float lower_k = params.window_in * 2.f * Numeric.PI / fft_period;
        final float upper_k = params.window_out * 2.f * Numeric.PI / fft_period;
        float rms_est = UpperBoundPhillipsIntegral(upper_k, v, a, dir_depend, params.small_wave_fraction) - UpperBoundPhillipsIntegral(lower_k, v, a, dir_depend, params.small_wave_fraction);

        // Normalize to wave number space
        rms_est *= 0.25f*(fft_period*fft_period)/(Numeric.PI * Numeric.PI);
        rms_est *= phil_norm;

        return rms_est;
    }
}
