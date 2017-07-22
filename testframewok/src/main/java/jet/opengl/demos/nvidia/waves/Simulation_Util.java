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
}
