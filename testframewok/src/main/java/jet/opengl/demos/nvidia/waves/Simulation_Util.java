package jet.opengl.demos.nvidia.waves;

import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector4f;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Random;

import jet.opengl.postprocessing.texture.TextureGL;
import jet.opengl.postprocessing.util.DebugTools;
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

    public static void tieThreadToCore(int core) {
        // Enable this to WAR on systems that have core-sensitive QueryPerformanceFrequency
        // SetThreadAffinityMask( GetCurrentThread(), 1<<core );
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

    private static abstract class  InputPolicy{
        final Vector4f m_data = new Vector4f();
        abstract int stride();
        abstract Vector4f get_float4(ByteBuffer input);
    }

    private static final class Float16InputPolicy extends InputPolicy{
        @Override
        int stride() {return /*Vector4f.SIZE/2*/1; /* TODO */}

        @Override
        Vector4f get_float4(ByteBuffer input) {
            m_data.x = Numeric.convertHFloatToFloat(input.getShort());
            m_data.y = Numeric.convertHFloatToFloat(input.getShort());
            m_data.z = Numeric.convertHFloatToFloat(input.getShort());
            m_data.w = Numeric.convertHFloatToFloat(input.getShort());
            return m_data;
        }
    }

    private interface MultiplierPolicy{
        Vector4f mult(Vector4f val);
    }

    private static final class NoMultiplierPolicy implements MultiplierPolicy{
        @Override
        public Vector4f mult(Vector4f val) { return val;}
    }

    private static final class ParameterizedMultiplierPolicy implements MultiplierPolicy {
        ParameterizedMultiplierPolicy(float m) {m_multiplier = m;}

        public Vector4f mult(Vector4f val) {
            Vector4f.scale(val, m_multiplier, m_data);
            return m_data;
        }

        float m_multiplier;
        final Vector4f m_data = new Vector4f();
    }

    static void add_displacements_float16(GFSDK_WaveWorks_Detailed_Simulation_Params.Cascade params,
                                       ByteBuffer pReadbackData,
                                       int rowPitch,
                                       Vector2f[] inSamplePoints,
                                       Vector4f[] outDisplacements,
                                       int numSamples,
                                       float multiplier
    )
    {
        add_displacements/*<Float16InputPolicy>*/(new Float16InputPolicy(), params,pReadbackData,rowPitch,inSamplePoints,outDisplacements,numSamples,multiplier);
    }

    private static void add_displacements(
                                InputPolicy inputPolicy,
                               GFSDK_WaveWorks_Detailed_Simulation_Params.Cascade params,
                               ByteBuffer pReadbackData,
                               int rowPitch,
                               Vector2f[] inSamplePoints,
                               Vector4f[] outDisplacements,
                               int numSamples,
                               float multiplier
    )
    {
        if(1.f == multiplier)
        {
            // No multiplier required
            add_displacements/*<InputPolicy,NoMultiplierPolicy>*/(inputPolicy, params,pReadbackData,rowPitch,inSamplePoints,outDisplacements,numSamples,new NoMultiplierPolicy());
        }
        else if(0.f != multiplier)
        {
            add_displacements/*<InputPolicy,ParameterizedMultiplierPolicy>*/(inputPolicy,params,pReadbackData,rowPitch,inSamplePoints,outDisplacements,numSamples,
                    new ParameterizedMultiplierPolicy(multiplier));
        }
        else
        {
            // Nothin to add, do nothin
        }
    }

    private static void add_displacements(	InputPolicy inputPolicy, GFSDK_WaveWorks_Detailed_Simulation_Params.Cascade params,
                               ByteBuffer pReadbackData,
                               int rowPitch,
                               Vector2f[] inSamplePoints,
                               Vector4f[] outDisplacements,
                               int numSamples,
                               MultiplierPolicy multiplier
    )
    {
        final int dmap_dim = params.fft_resolution;
        final float f_dmap_dim = dmap_dim;
        final float uv_scale = f_dmap_dim / params.fft_period;
        final Vector2f uv = new Vector2f();
        final Vector2f uv_wrap = new Vector2f();
        final Vector2f uv_round = new Vector2f();
        final Vector2f uv_frac = new Vector2f();
        final Vector4f toadd = new Vector4f();

//        const gfsdk_float2* currSrc = inSamplePoints;
//        gfsdk_float4* currDst = outDisplacements;
        for(int sample = 0; sample != numSamples; ++sample/*, ++currSrc, ++currDst*/)
        {
            // Calculate the UV coords, in texels
            final Vector2f currSrc = inSamplePoints[sample];
            final Vector4f currDst = outDisplacements[sample];
//            const gfsdk_float2 uv = *currSrc * uv_scale - gfsdk_make_float2(0.5f, 0.5f);
            uv.x = currSrc.x * uv_scale - 0.5f;
            uv.y = currSrc.y * uv_scale - 0.5f;

//            gfsdk_float2 uv_wrap = gfsdk_make_float2(fmodf(uv.x,f_dmap_dim),fmodf(uv.y,f_dmap_dim));
            uv_wrap.x = uv.x % f_dmap_dim;
            uv_wrap.y = uv.y % f_dmap_dim;
            if(uv_wrap.x < 0.f)
                uv_wrap.x += f_dmap_dim;
            else if(uv_wrap.x >= f_dmap_dim)
                uv_wrap.x -= f_dmap_dim;
            if(uv_wrap.y < 0.f)
                uv_wrap.y += f_dmap_dim;
            else if(uv_wrap.y >= f_dmap_dim)
                uv_wrap.y -= f_dmap_dim;
//            const gfsdk_float2 uv_round = gfsdk_make_float2(floorf(uv_wrap.x),floorf(uv_wrap.y));
            uv_round.x = (float) Math.floor(uv_wrap.x);
            uv_round.y = (float) Math.floor(uv_wrap.y);
//            const gfsdk_float2 uv_frac = uv_wrap - uv_round;
            Vector2f.sub(uv_wrap, uv_round, uv_frac);

            final int uv_x = ((int)uv_round.x) % dmap_dim;
            final int uv_y = ((int)uv_round.y) % dmap_dim;
            final int uv_x_1 = (uv_x + 1) % dmap_dim;
            final int uv_y_1 = (uv_y + 1) % dmap_dim;

            // Ensure we wrap round during the lerp too
//            const typename InputPolicy::InputType* pTL = reinterpret_cast<const typename InputPolicy::InputType*>(pReadbackData + uv_y * rowPitch);
//            const typename InputPolicy::InputType* pTR = pTL + uv_x_1;
//            pTL += uv_x;
//            const typename InputPolicy::InputType* pBL = reinterpret_cast<const typename InputPolicy::InputType*>(pReadbackData + uv_y_1 * rowPitch);
//            const typename InputPolicy::InputType* pBR = pBL + uv_x_1;
//            pBL += uv_x;

            int pTL = uv_y * rowPitch;
            int pTR = pTL + uv_x_1;
            pTL += uv_x;
            int pBL = uv_y_1 * rowPitch;
            int pBR = pBL + uv_x_1;
            pBL += uv_x;

//            Vector4f toadd = (1.f - uv_frac.x) * (1.f - uv_frac.y) * InputPolicy::get_float4(pTL);
            pReadbackData.position(pTL * inputPolicy.stride());
            Vector4f value = inputPolicy.get_float4(pReadbackData);
            value.scale((1.f - uv_frac.x) * (1.f - uv_frac.y));
            toadd.set(value);

//            toadd             +=        uv_frac.x  * (1.f - uv_frac.y) * InputPolicy::get_float4(pTR);
            pReadbackData.position(pTR * inputPolicy.stride());
            value = inputPolicy.get_float4(pReadbackData);
            value.scale(uv_frac.x  * (1.f - uv_frac.y));
            Vector4f.add(toadd, value, toadd);

//            toadd             += (1.f - uv_frac.x) *        uv_frac.y  * InputPolicy::get_float4(pBL);
            pReadbackData.position(pBL * inputPolicy.stride());
            value = inputPolicy.get_float4(pReadbackData);
            value.scale((1.f - uv_frac.x) *        uv_frac.y);
            Vector4f.add(toadd, value, toadd);

//            toadd             +=        uv_frac.x  *        uv_frac.y  * InputPolicy::get_float4(pBR);
            pReadbackData.position(pBR * inputPolicy.stride());
            value = inputPolicy.get_float4(pReadbackData);
            value.scale(uv_frac.x  *        uv_frac.y);
            Vector4f.add(toadd, value, toadd);

//            *currDst += multiplier.mult(toadd);
            Vector4f.add(toadd, currDst, currDst);
        }
    }

    static final void saveTextData(String filename, TextureGL texture){
        final String filepath = "E:/textures/WaveWorks/";
        try {
            DebugTools.saveTextureAsText(texture.getTarget(), texture.getTexture(), 0, filepath + filename);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static final void saveTextData(String filename, int target, int buffer, int internalformat){
        final String filepath = "E:/textures/WaveWorks/";
        try {
            DebugTools.saveBufferAsText(target, buffer, internalformat, 128, filepath + filename);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static final void saveTextData(String filename, int target, int buffer, Class<?> internalformat){
        final String filepath = "E:/textures/WaveWorks/";
        try {
            DebugTools.saveBufferAsText(target, buffer, internalformat, 128, filepath + filename);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
