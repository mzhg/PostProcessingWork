package jet.opengl.demos.nvidia.waves.wavelets;

import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;
import org.lwjgl.util.vector.VectorInterpolation;

import java.util.List;
import java.util.function.IntConsumer;

/**
 * The class ProfileBuffer is representation of the integral (21) from the
 * paper.<p></p>
 *
 * It provides two functionalities:<br>
 * 1. Function `precompute` precomputes values of the integral for main input
 * valus of `p` (see paper for the meaning of p)<br>
 * 2. Call operator evaluates returns the profile value at a given point.
 * This is done by interpolating from precomputed values.<p>
 *
 * TODO: Add note about making (21) pariodic over precomputed interval.
 */
final class ProfileBuffer {

    float m_period;

//    std::vector<std::array<float, 4>> m_data;
    List<Vector4f> m_data;

    private interface Function{
        Vector4f eval(float x);
    }

    private static Vector4f integrate(int integration_nodes, double x_min, double x_max, Function fun) {

        float dx = (float) ((x_max - x_min) / integration_nodes);
        float x  = (float) (x_min + 0.5f * dx);

        Vector4f result =  fun.eval(x); // the first integration node
        result.scale(dx);
        for (int i = 1; i < integration_nodes; i++) { // proceed with other nodes, notice `int i= 1`
            x += dx;
//            result += dx * fun.eval(x);
            Vector4f.linear(result, fun.eval(x), dx, result);
        }

        return result;
    }

    /**
     * Precomputes profile buffer for the give spectrum.
     *
     * This function numerically precomputes integral (21) from the paper.
     *
     * @param spectrum A function which accepts zeta(=log2(wavelength)) and retuns
     * wave density.
     * @param time Precompute the profile for a given time.
     * @param zeta_min Lower bound of the integral. zeta_min == log2('minimal
     * wavelength')
     * @param zeta_min Upper bound of the integral. zeta_max == log2('maximal
     * wavelength')
     * @param resolution number of nodes for which the .
     * @param periodicity The period of the final function is determined as
     * `periodicity*pow(2,zeta_max)`
     * @param integration_nodes Number of integraion nodes
     */
    void precompute(Spectrum spectrum, float time, float zeta_min,
                    float zeta_max, int resolution /*= 4096*/, int periodicity /*= 2*/,
                    int integration_nodes /*= 100*/) {
//        m_data.resize(resolution);
        m_period = (float) (periodicity * Math.pow(2, zeta_max));

//#pragma omp parallel for
        for (int i = 0; i < resolution; i++) {

            float tau = 6.28318530718f;
            float p   = (i * m_period) / resolution;

            Vector4f data = integrate(integration_nodes, zeta_min, zeta_max, (float zeta)-> {

                float waveLength = (float) Math.pow(2, zeta);
                float waveNumber = tau / waveLength;
                float phase1 =
                        waveNumber * p - dispersionRelation(waveNumber) * time;
                float phase2 = waveNumber * (p - m_period) -
                        dispersionRelation(waveNumber) * time;

                float weight1 = p / m_period;
                float weight2 = 1 - weight1;

                /*return waveLength * spectrum.get(zeta) *
                        (cubic_bump(weight1) * gerstner_wave(phase1, waveNumber) +
                                cubic_bump(weight2) * gerstner_wave(phase2, waveNumber));*/

                Vector4f result = Vector4f.linear(gerstner_wave(phase1, waveNumber), cubic_bump(weight1),
                        gerstner_wave(phase2, waveNumber), cubic_bump(weight2), null);
                result.scale((float) (waveLength * spectrum.get(zeta)));
                return result;
            });

            m_data.add(data);
        }
    }

    private static int pos_modulo(int n, int d) { return (n % d + d) % d; }

    /**
     * Evaluate profile at point p by doing linear interpolation over precomputed
     * data
     * @param p evaluation position, it is usually p=dot(position, wavedirection)
     */
    Vector4f get(float p) {
        final int N = m_data.size();

        /*// Guard from acessing outside of the buffer by wrapping
        auto extended_buffer = [=](int i) -> std::array<float, 4> {
        return m_data[pos_modulo(i, N)];
        };

        // Preform linear interpolation
        auto interpolated_buffer = LinearInterpolation(extended_buffer);

        // rescale `p` to interval [0,1)
        return interpolated_buffer(N * p / m_period);*/

        float x = N * p / m_period;  // todo The p is same as the 'p' defined at the line 76
        int ix = (int)Math.floor(x);
        float wx = x - ix;

        if(wx == 0.0f){
            return m_data.get(pos_modulo(ix, N));
        }else{
            Vector4f v0 = m_data.get(pos_modulo(ix, N));
            Vector4f v1 = m_data.get(pos_modulo(ix+1, N));
            return Vector4f.mix(v0, v1, wx, null);
        }
    }

    /**
     * Dispersion relation in infinite depth -
     * https://en.wikipedia.org/wiki/Dispersion_(water_waves)
     */
    private static float dispersionRelation(float k) {
        final float g = 9.81f;
        return (float) Math.sqrt(k * g);
    }

    /**
     * Gerstner wave - https://en.wikipedia.org/wiki/Trochoidal_wave
     *
     * @return Array of the following values:
    1. horizontal position offset
    2. vertical position offset
    3. position derivative of horizontal offset
    4. position derivative of vertical offset
     */
    private static Vector4f gerstner_wave(float phase /*=knum*x*/, float knum){
        float s = (float) Math.sin(phase);
        float c = (float) Math.cos(phase);
        return new Vector4f(-s, c, -knum * c, -knum * s);
    }

    /** bubic_bump is based on $p_0$ function from
     * https://en.wikipedia.org/wiki/Cubic_Hermite_spline
     */
    private static float cubic_bump(float x) {
        if (Math.abs(x) >= 1)
            return 0.0f;
        else
            return x * x * (2 * Math.abs(x) - 3) + 1;
    }
}
