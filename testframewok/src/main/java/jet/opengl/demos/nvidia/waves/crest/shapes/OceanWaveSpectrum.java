package jet.opengl.demos.nvidia.waves.crest.shapes;

import org.lwjgl.util.vector.ReadableVector2f;
import org.lwjgl.util.vector.Vector2f;

import jet.opengl.postprocessing.util.LogUtil;
import jet.opengl.postprocessing.util.Numeric;

/** Ocean shape representation - power values for each octave of wave components. */
public final class OceanWaveSpectrum {
    public final int NUM_OCTAVES = 14;
    public static final float SMALLEST_WL_POW_2 = -4f;

//        [HideInInspector]
    public float _windSpeed = 10f;

//        [HideInInspector]
    public float _fetch = 500000f;

    public static final float MIN_POWER_LOG = -6f;
    public static final float MAX_POWER_LOG = 5f;

//        [Tooltip("Variance of wave directions, in degrees"), Range(0f, 180f)]
    public float _waveDirectionVariance = 90f;

//        [Tooltip("More gravity means faster waves."), Range(0f, 25f)]
    public float _gravityScale = 1f;

//        [HideInInspector]
    public float _smallWavelengthMultiplier = 1f;

//        [Tooltip("Multiplier"), Range(0f, 10f), SerializeField]
    float _multiplier = 1f;

//        [SerializeField]
    float[] _powerLog = new float[/*NUM_OCTAVES*/]
    { -6f, -6f, -6f, -4.0088496f, -3.4452133f, -2.6996124f, -2.615044f, -1.2080691f, -0.53905386f, 0.27448857f, 0.53627354f, 1.0282621f, 1.4403292f, -6f };

//        [SerializeField]
    boolean[] _powerDisabled = new boolean[NUM_OCTAVES];

//        [HideInInspector]
    public float[] _chopScales = new float[/*NUM_OCTAVES*/]
    { 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f };

//        [HideInInspector]
    public float[] _gravityScales = new float[/*NUM_OCTAVES*/]
    { 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f };

//        [Tooltip("Scales horizontal displacement"), Range(0f, 2f)]
    public float _chop = 1f;

    public boolean _showAdvancedControls = false;

    public static float SmallWavelength(float octaveIndex) { return (float)Math.pow(2f, SMALLEST_WL_POW_2 + octaveIndex); }

    public float GetAmplitude(float wavelength, float componentsPerOctave)
    {
        // Always take random value so that sequence remains deterministic even if this function early outs
        float rand0 = Numeric.random();

        assert (wavelength > 0f) : "OceanWaveSpectrum: Wavelength must be >= 0f";

        float wl_pow2 = (float) (Math.log(wavelength) / Math.log(2f));
        wl_pow2 = Numeric.clamp(wl_pow2, SMALLEST_WL_POW_2, SMALLEST_WL_POW_2 + NUM_OCTAVES - 1f);

        float lower = (float)Math.pow(2f, Math.floor(wl_pow2));

        int index = (int)(wl_pow2 - SMALLEST_WL_POW_2);

        if(_powerLog.length < NUM_OCTAVES)
        {
            LogUtil.w(LogUtil.LogType.DEFAULT, "Wave spectrum {name} is out of date, please open this asset and resave in editor.");
        }

        if (index >= _powerLog.length)
        {
            assert (index < _powerLog.length) : "OceanWaveSpectrum: index {index} is out of range.";
            return 0f;
        }

        if (_powerDisabled[index])
        {
            return 0f;
        }

        // The amplitude calculation follows this nice paper from Frechot:
        // https://hal.archives-ouvertes.fr/file/index/docid/307938/filename/frechot_realistic_simulation_of_ocean_surface_using_wave_spectra.pdf
        double wl_lo = Math.pow(2f, Math.floor(wl_pow2));
        double k_lo = 2f * Math.PI / wl_lo;
        double omega_lo = k_lo * ComputeWaveSpeed(wl_lo, 1);
        double wl_hi = 2f * wl_lo;
        double k_hi = 2f * Math.PI / wl_hi;
        double omega_hi = k_hi * ComputeWaveSpeed(wl_hi, 1);

        double domega = (omega_lo - omega_hi) / componentsPerOctave;

        // Alpha used to interpolate between power values
        double alpha = (wavelength - lower) / lower;

        // Power
        double pow = Numeric.mix(_powerLog[index], _powerLog[Math.min(index + 1, _powerLog.length - 1)], alpha);

        double a_2 = 2f * Math.pow(10f, pow) * domega;

        // Amplitude
        double a = Math.sqrt(a_2);

        return (float) (a * rand0 * _multiplier);
    }

    public static double ComputeWaveSpeed(double wavelength, double gravityMultiplier /*= 1f*/)
    {
        // wave speed of deep sea ocean waves: https://en.wikipedia.org/wiki/Wind_wave
        // https://en.wikipedia.org/wiki/Dispersion_(water_waves)#Wave_propagation_and_dispersion
        double g = /*Math.abs(Physics.gravity.y)*/9.8 * gravityMultiplier;
        double k = 2f * Math.PI / wavelength;
        //float h = max(depth, 0.01);
        //float cp = sqrt(abs(tanh_clamped(h * k)) * g / k);
        double cp = Math.sqrt(g / k);
        return cp;
    }

    /** Samples spectrum to generate wave data. Wavelengths will be in ascending order. */
    public void GenerateWaveData(int componentsPerOctave, float[] wavelengths, float[] anglesDeg)
    {
        int totalComponents = NUM_OCTAVES * componentsPerOctave;

        if (wavelengths == null || wavelengths.length != totalComponents) wavelengths = new float[totalComponents];
        if (anglesDeg == null || anglesDeg.length != totalComponents) anglesDeg = new float[totalComponents];

        double minWavelength = Math.pow(2f, SMALLEST_WL_POW_2);
        double invComponentsPerOctave = 1f / componentsPerOctave;

        for (int octave = 0; octave < NUM_OCTAVES; octave++)
        {
            for (int i = 0; i < componentsPerOctave; i++)
            {
                int index = octave * componentsPerOctave + i;

                // stratified random sampling - should give a better range of wavelengths, and also means i can generated the
                // wavelengths in sorted order!
                double minWavelengthi = minWavelength + invComponentsPerOctave * minWavelength * i;
                double maxWavelengthi = Math.min(minWavelengthi + invComponentsPerOctave * minWavelength, 2f * minWavelength);
                wavelengths[index] = (float) Numeric.mix(minWavelengthi, maxWavelengthi, Numeric.random());

                double rnd = (i + Math.random()) * invComponentsPerOctave;
                anglesDeg[index] = (float) ((2f * rnd - 1f) * _waveDirectionVariance);
            }

            minWavelength *= 2f;
        }
    }

    public void ApplyPhillipsSpectrum(float windSpeed, float smallWavelengthMultiplier)
    {
        // Angles should usually be relative to wind direction, so setting wind direction to angle=0 should be ok.
        ReadableVector2f windDir = Vector2f.X_AXIS;

        for (int octave = 0; octave < NUM_OCTAVES; octave++)
        {
            // Shift wavelengths based on a magic number of this spectrum which seems to give small waves.
            float wl = SmallWavelength(octave) * smallWavelengthMultiplier * 1.5f;

            double pow = PhillipsSpectrum(windSpeed, windDir, /*Math.abs(Physics.gravity.y)*/9.8f, wl, 0f);
            // we store power on logarithmic scale. this does not include 0, we represent 0 as min value
            pow = Math.max(pow, Math.pow(10f, MIN_POWER_LOG));
            _powerLog[octave] = (float) Math.log10(pow);
        }
    }

    public void ApplyPiersonMoskowitzSpectrum(float windSpeed, float smallWavelengthMultiplier)
    {
        for (int octave = 0; octave < NUM_OCTAVES; octave++)
        {
            // Shift wavelengths based on a magic number of this spectrum which seems to give small waves.
            float wl = SmallWavelength(octave) * smallWavelengthMultiplier * 9f;

            double pow = PiersonMoskowitzSpectrum(/*Math.abs(Physics.gravity.y)*/9.8f, windSpeed, wl);
            // we store power on logarithmic scale. this does not include 0, we represent 0 as min value
            pow = Math.max(pow, Math.pow(10f, MIN_POWER_LOG));
            _powerLog[octave] = (float) Math.log10(pow);
        }
    }

    public void ApplyJONSWAPSpectrum(float windSpeed, float fetch, float smallWavelengthMultiplier)
    {
        for (int octave = 0; octave < NUM_OCTAVES; octave++)
        {
            // Shift wavelengths based on a magic number of this spectrum which seems to give small waves.
            float wl = SmallWavelength(octave) * smallWavelengthMultiplier * 9f;

            double pow = JONSWAPSpectrum(/*Math.abs(Physics.gravity.y)*/9.8f, windSpeed, wl, fetch);
            // we store power on logarithmic scale. this does not include 0, we represent 0 as min value
            pow = Math.max(pow, Math.pow(10f, MIN_POWER_LOG));
            _powerLog[octave] = (float) Math.log10(pow);
        }
    }

    static float PhillipsSpectrum(float windSpeed, ReadableVector2f windDir, float gravity, float wavelength, float angle)
    {
        double wavenumber = 2f * Math.PI / wavelength;
        double angle_radians = Math.PI * angle / 180f;
        double kx = Math.cos(angle_radians) * wavenumber;
        double kz = Math.sin(angle_radians) * wavenumber;

        double k2 = kx * kx + kz * kz;

        double windSpeed2 = windSpeed * windSpeed;
        double wx = windDir.getX();
        double wz = windDir.getY();

        double kdotw = (wx * kx + wz * kz);

        double a = 0.0081f; // phillips constant ( https://hal.archives-ouvertes.fr/file/index/docid/307938/filename/frechot_realistic_simulation_of_ocean_surface_using_wave_spectra.pdf )
        double L = windSpeed2 / gravity;

        // http://citeseerx.ist.psu.edu/viewdoc/download?doi=10.1.1.161.9102&rep=rep1&type=pdf
        return (float) (a * kdotw * kdotw * Math.exp(-1f / (k2 * L * L)) / (k2 * k2));
    }

    // base of modern parametric wave spectrum
    static float PhilSpectrum(float gravity, float wavelength)
    {
        float alpha = 0.0081f; // phillips constant ( https://hal.archives-ouvertes.fr/file/index/docid/307938/filename/frechot_realistic_simulation_of_ocean_surface_using_wave_spectra.pdf )
        return PhilSpectrum(gravity, alpha, wavelength);
    }
    // base of modern parametric wave spectrum
    static float PhilSpectrum(float gravity, float alpha, float wavelength)
    {
        //float alpha = 0.0081f; // phillips constant ( https://hal.archives-ouvertes.fr/file/index/docid/307938/filename/frechot_realistic_simulation_of_ocean_surface_using_wave_spectra.pdf )
        double wavenumber = 2f * Math.PI / wavelength;
        double frequency = Math.sqrt(gravity * wavenumber); // deep water - depth > wavelength/2
        return (float) (alpha * gravity * gravity / Math.pow(frequency, 5f));
    }

    static float PiersonMoskowitzSpectrum(float gravity, float windspeed, float wavelength)
    {
        double wavenumber = 2f * Math.PI / wavelength;
        double frequency = Math.sqrt(gravity * wavenumber); // deep water - depth > wavelength/2
        double frequency_peak = 0.855f * gravity / windspeed;
        return (float) (PhilSpectrum(gravity, wavelength) * Math.exp(-Math.pow(frequency_peak / frequency, 4f) * 5f / 4f));
    }
    static float PiersonMoskowitzSpectrum(float gravity, float windspeed, float frequency_peak, float alpha, float wavelength)
    {
        double wavenumber = 2f * Math.PI / wavelength;
        double frequency = Math.sqrt(gravity * wavenumber); // deep water - depth > wavelength/2
        return (float) (PhilSpectrum(gravity, alpha, wavelength) * Math.exp(-Math.pow(frequency_peak / frequency, 4f) * 5f / 4f));
    }

    static float JONSWAPSpectrum(float gravity, float windspeed, float wavelength, float fetch)
    {
        // fetch distance
        double F = fetch;
        float alpha = (float) (0.076f * Math.pow(windspeed * windspeed / (F * gravity), 0.22f));

        double wavenumber = 2f * Math.PI / wavelength;
        double frequency = Math.sqrt(gravity * wavenumber); // deep water - depth > wavelength/2
        float frequency_peak = (float) (22f * Math.pow(gravity * gravity / (windspeed * F), 1f / 3f));
        double sigma = frequency <= frequency_peak ? 0.07f : 0.09f;
        double r = Math.exp(-Math.pow(frequency - frequency_peak, 2f) / (2f * sigma * sigma * frequency_peak * frequency_peak));
        double gamma = 3.3f;

        return (float) (PiersonMoskowitzSpectrum(gravity, windspeed, frequency_peak, alpha, wavelength) * Math.pow(gamma, r));
    }
}
