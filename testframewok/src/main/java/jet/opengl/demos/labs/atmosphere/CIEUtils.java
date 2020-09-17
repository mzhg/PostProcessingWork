package jet.opengl.demos.labs.atmosphere;

import jet.opengl.postprocessing.util.StackDouble;

public class CIEUtils implements  Constant{
    /**
<p>Finally, we need a utility function to compute the value of the conversion
constants *<code>_RADIANCE_TO_LUMINANCE</code>, used above to convert the
spectral results into luminance values. These are the constants k_r, k_g, k_b
described in Section 14.3 of <a href="https://arxiv.org/pdf/1612.04336.pdf">A
Qualitative and Quantitative Evaluation of 8 Clear Sky Models</a>.

<p>Computing their value requires an integral of a function times a CIE color
matching function. Thus, we first need functions to interpolate an arbitrary
function (specified by some samples), and a CIE color matching function
(specified by tabulated values), at an arbitrary wavelength. This is the purpose
of the following two functions:
*/

    public static final int kLambdaMin = 360;
    public static final int kLambdaMax = 830;

    public static double CieColorMatchingFunctionTableValue(double wavelength, int column) {
        if (wavelength <= kLambdaMin || wavelength >= kLambdaMax) {
            return 0.0;
        }
        double u = (wavelength - kLambdaMin) / 5.0;
        int row =  (int)(Math.floor(u));
        assert(row >= 0 && row + 1 < 95);
        assert(CIE_2_DEG_COLOR_MATCHING_FUNCTIONS[4 * row] <= wavelength &&
                CIE_2_DEG_COLOR_MATCHING_FUNCTIONS[4 * (row + 1)] >= wavelength);
        u -= row;
        return CIE_2_DEG_COLOR_MATCHING_FUNCTIONS[4 * row + column] * (1.0 - u) +
                CIE_2_DEG_COLOR_MATCHING_FUNCTIONS[4 * (row + 1) + column] * u;
    }

    public static double Interpolate(StackDouble wavelengths,
                                     StackDouble wavelength_function,
                                     double wavelength) {
        assert(wavelength_function.size() == wavelengths.size());
        if (wavelength < wavelengths.get(0)) {
            return wavelength_function.get(0);
        }
        for (int i = 0; i < wavelengths.size() - 1; ++i) {
            if (wavelength < wavelengths.get(i + 1)) {
                double u = (wavelength - wavelengths.get(i)) / (wavelengths.get(i + 1) - wavelengths.get(i));
                return wavelength_function.get(i) * (1.0 - u) + wavelength_function.get(i + 1)* u;
            }
        }
        return wavelength_function.get(wavelength_function.size() - 1);
    }

/**
<p>We can then implement a utility function to compute the "spectral radiance to
luminance" conversion constants (see Section 14.3 in <a
href="https://arxiv.org/pdf/1612.04336.pdf">A Qualitative and Quantitative
Evaluation of 8 Clear Sky Models</a> for their definitions):
 The returned constants are in lumen.nm / watt.
*/
    public static void ComputeSpectralRadianceToLuminanceFactors(StackDouble wavelengths, StackDouble solar_irradiance,
                                                                 double lambda_power, double[] outRgb) {
        double k_r = 0.0;
        double k_g = 0.0;
        double k_b = 0.0;
        double solar_r = Interpolate(wavelengths, solar_irradiance, Model.kLambdaR);
        double solar_g = Interpolate(wavelengths, solar_irradiance, Model.kLambdaG);
        double solar_b = Interpolate(wavelengths, solar_irradiance, Model.kLambdaB);
        int dlambda = 1;
        for (int lambda = kLambdaMin; lambda < kLambdaMax; lambda += dlambda) {
            double x_bar = CieColorMatchingFunctionTableValue(lambda, 1);
            double y_bar = CieColorMatchingFunctionTableValue(lambda, 2);
            double z_bar = CieColorMatchingFunctionTableValue(lambda, 3);
            final double[] xyz2srgb = XYZ_TO_SRGB;
            double r_bar = xyz2srgb[0] * x_bar + xyz2srgb[1] * y_bar + xyz2srgb[2] * z_bar;
            double g_bar = xyz2srgb[3] * x_bar + xyz2srgb[4] * y_bar + xyz2srgb[5] * z_bar;
            double b_bar = xyz2srgb[6] * x_bar + xyz2srgb[7] * y_bar + xyz2srgb[8] * z_bar;
            double irradiance = Interpolate(wavelengths, solar_irradiance, lambda);
            k_r += r_bar * irradiance / solar_r * Math.pow(lambda / Model.kLambdaR, lambda_power);
            k_g += g_bar * irradiance / solar_g * Math.pow(lambda / Model.kLambdaG, lambda_power);
            k_b += b_bar * irradiance / solar_b * Math.pow(lambda / Model.kLambdaB, lambda_power);
        }

        k_r *= MAX_LUMINOUS_EFFICACY * dlambda;
        k_g *= MAX_LUMINOUS_EFFICACY * dlambda;
        k_b *= MAX_LUMINOUS_EFFICACY * dlambda;

        outRgb[0] = k_r;
        outRgb[1] = k_g;
        outRgb[2] = k_b;
    }


}
