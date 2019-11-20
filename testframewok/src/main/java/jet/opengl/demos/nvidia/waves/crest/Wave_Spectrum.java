package jet.opengl.demos.nvidia.waves.crest;

import org.lwjgl.util.vector.ReadableVector2f;
import org.lwjgl.util.vector.Vector2f;

import java.util.Arrays;

import jet.opengl.postprocessing.util.LogUtil;
import jet.opengl.postprocessing.util.Numeric;

final class Wave_Spectrum {

    public final static int NUM_OCTAVES = 14;
    public static final float SMALLEST_WL_POW_2 = -4f;

    public static final float MIN_POWER_LOG = -6f;
    public static final float MAX_POWER_LOG = 5f;

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
    float _gravityScale = 1;

    private Wave_Simulation_Params m_Params;

    Wave_Spectrum(Wave_Simulation_Params params, Wave_Demo_Animation animation){
        m_Params = params;

        switch (animation){
            case Calm:
                setWaveCalm(false, false, false);
                break;
            case  BoatScene:
                setWaveBoatScene(false, false, false);
                break;
            case Dead:
                setWaveDead(false, false, false);
                break;
            case Moderate:
                setWaveModerate(false, false, false);
                break;
            case ModerateSmooth:
                setWaveModerateSmooth(false, false, false);
                break;
            case Whirlpool:
                setWaveWhirlpool(false, false, false);
                break;
            default:
                throw new IllegalArgumentException("Unkown animation type:" + animation);
        }
    }

    public float getAmplitudeScale(){ return m_Params.wave_amplitude;}
    public float waveDirectionVariance(){
        return m_Params.wind_dependency * 180f;
    }

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
        // Equation (18)
        double wl_lo = Math.pow(2f, Math.floor(wl_pow2));
        double k_lo = 2f * Math.PI / wl_lo;
        double omega_lo = k_lo * computeWaveSpeed(wl_lo, 1);
        double wl_hi = 2f * wl_lo;
        double k_hi = 2f * Math.PI / wl_hi;
        double omega_hi = k_hi * computeWaveSpeed(wl_hi, 1);

        double domega = (omega_lo - omega_hi) / componentsPerOctave;

        // Alpha used to interpolate between power values
        double alpha = (wavelength - lower) / lower;

        // Power
        double pow = Numeric.mix(_powerLog[index], _powerLog[Math.min(index + 1, _powerLog.length - 1)], alpha);

        double a_2 = 2f * Math.pow(10f, pow) * domega;

        // Amplitude
        double a = Math.sqrt(a_2);

        return (float) (a * rand0 * getAmplitudeScale());
    }

    public static double computeWaveSpeed(double wavelength, double gravityMultiplier /*= 1f*/)
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

        if (wavelengths == null || wavelengths.length != totalComponents) throw new IllegalArgumentException("Invalid wavelengths");
        if (anglesDeg == null || anglesDeg.length != totalComponents) throw new IllegalArgumentException("Invalid anglesDeg");

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

                double rnd = (i + Numeric.random()) * invComponentsPerOctave;
                anglesDeg[index] = (float) ((2f * rnd - 1f) * waveDirectionVariance());
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

    void setWaveBoatScene(boolean applyPhillipsSpectrum, boolean applyPiersonMoskowitzSpectrum, boolean applyJONSWAPSpectrum){
        m_Params.wind_dependency = 0.5f;
        _gravityScale = 1;
        m_Params.wave_amplitude = 1;

        _powerLog[0] =-5.743933f;
        _powerLog[1] =-5.141876f;
        _powerLog[2] =-4.539827f;
        _powerLog[3] =-3.937812f;
        _powerLog[4] =-3.33593f;
        _powerLog[5] =-2.734585f;
        _powerLog[6] =-2.135383f;
        _powerLog[7] =-1.544757f;
        _powerLog[8] =-0.988432f;
        _powerLog[9] =-0.569311f;
        _powerLog[10] =-0.699007f;
        _powerLog[11] =-3.023971f;
        _powerLog[12] =-6;
        _powerLog[13] =-6;

        for(int i = 0; i < 14; i++){
            _powerDisabled[i] = !(i < 10);
        }

        _chop = 1.69f;

        Arrays.fill(_chopScales, 1);
        Arrays.fill(_gravityScales, 1);
        _gravityScales[10] = 1.5f;

        m_Params.wind_speed = 36/3.6f;
        float _smallWavelengthMultiplier = 1;

        if (applyPhillipsSpectrum)
        {
            ApplyPhillipsSpectrum(m_Params.wind_speed, _smallWavelengthMultiplier);
        }

        if (applyPiersonMoskowitzSpectrum)
        {
            ApplyPiersonMoskowitzSpectrum(m_Params.wind_speed, _smallWavelengthMultiplier);
        }

//        spec._fetch = EditorGUILayout.Slider(s_labelFetch, spec._fetch, 0f, 1000000f);
        m_Params.fetch =500_000f;
        if (applyJONSWAPSpectrum)
        {
            ApplyJONSWAPSpectrum(m_Params.wind_speed, m_Params.fetch, _smallWavelengthMultiplier);
        }
    }

    void setWaveCalm(boolean applyPhillipsSpectrum, boolean applyPiersonMoskowitzSpectrum, boolean applyJONSWAPSpectrum){
        m_Params.wind_dependency = 56f/180f;
        _gravityScale = 3.1f;
        m_Params.wave_amplitude = 1;

        _powerLog[0] =-5.207515f;
        _powerLog[1] =-4.455008f;
        _powerLog[2] =-3.702701f;
        _powerLog[3] =-2.9512f;
        _powerLog[4] =-2.202921f;
        _powerLog[5] =-1.467531f;
        _powerLog[6] =-0.783695f;
        _powerLog[7] =-0.3060761f;
        _powerLog[8] =-0.6533254f;
        _powerLog[9] =-4.300048f;
        _powerLog[10] =-6;
        _powerLog[11] =-6;
        _powerLog[12] =-6;
        _powerLog[13] =-6;

        Arrays.fill(_powerDisabled, false);

        _chop = 1.245f;

        Arrays.fill(_chopScales, 1);
        Arrays.fill(_gravityScales, 1);
        _chopScales[0] = 0.85f;

        m_Params.wind_speed = 36/3.6f;
        float _smallWavelengthMultiplier = 1;

        if (applyPhillipsSpectrum)
        {
            ApplyPhillipsSpectrum(m_Params.wind_speed, _smallWavelengthMultiplier);
        }

        if (applyPiersonMoskowitzSpectrum)
        {
            ApplyPiersonMoskowitzSpectrum(m_Params.wind_speed, _smallWavelengthMultiplier);
        }

        m_Params.fetch =500_000f;
        if (applyJONSWAPSpectrum)
        {
            ApplyJONSWAPSpectrum(m_Params.wind_speed, m_Params.fetch, _smallWavelengthMultiplier);
        }
    }

    void setWaveDead(boolean applyPhillipsSpectrum, boolean applyPiersonMoskowitzSpectrum, boolean applyJONSWAPSpectrum){
        m_Params.wind_dependency = 56f/180f;
        _gravityScale = 1f;
        m_Params.wave_amplitude = 1;

        Arrays.fill(_powerDisabled, true);

        _chop = 1.f;

        Arrays.fill(_chopScales, 1);
        Arrays.fill(_gravityScales, 1);

        m_Params.wind_speed = 36/3.6f;
        float _smallWavelengthMultiplier = 1;

        if (applyPhillipsSpectrum)
        {
            ApplyPhillipsSpectrum(m_Params.wind_speed, _smallWavelengthMultiplier);
        }

        if (applyPiersonMoskowitzSpectrum)
        {
            ApplyPiersonMoskowitzSpectrum(m_Params.wind_speed, _smallWavelengthMultiplier);
        }

        m_Params.fetch =385_738f;
        if (applyJONSWAPSpectrum)
        {
            ApplyJONSWAPSpectrum(m_Params.wind_speed, m_Params.fetch, _smallWavelengthMultiplier);
        }
    }

    void setWaveModerate(boolean applyPhillipsSpectrum, boolean applyPiersonMoskowitzSpectrum, boolean applyJONSWAPSpectrum){
        m_Params.wind_dependency = 90f/180f;
        _gravityScale = 1f;
        m_Params.wave_amplitude = 1;

        _powerLog[0] =-5.743932f;
        _powerLog[1] =-5.141873f;
        _powerLog[2] =-4.539814f;
        _powerLog[3] =-3.93776f;
        _powerLog[4] =-3.335723f;
        _powerLog[5] =-2.733756f;
        _powerLog[6] =-2.132066f;
        _powerLog[7] =-1.531488f;
        _powerLog[8] =-0.9353552f;
        _powerLog[9] =-0.3570041f;
        _powerLog[10] =0.1502203f;
        _powerLog[11] =0.3729381f;
        _powerLog[12] =-0.5423706f;
        _powerLog[13] =-6;

        Arrays.fill(_powerDisabled, false);

        _chop = 1.54f;

        Arrays.fill(_chopScales, 1);
        Arrays.fill(_gravityScales, 1);

        m_Params.wind_speed = 60/3.6f;
        float _smallWavelengthMultiplier = 1;

        if (applyPhillipsSpectrum)
        {
            ApplyPhillipsSpectrum(m_Params.wind_speed, _smallWavelengthMultiplier);
        }

        if (applyPiersonMoskowitzSpectrum)
        {
            ApplyPiersonMoskowitzSpectrum(m_Params.wind_speed, _smallWavelengthMultiplier);
        }

        m_Params.fetch =1_000_000f;
        if (applyJONSWAPSpectrum)
        {
            ApplyJONSWAPSpectrum(m_Params.wind_speed, m_Params.fetch, _smallWavelengthMultiplier);
        }
    }

    void setWaveModerateSmooth(boolean applyPhillipsSpectrum, boolean applyPiersonMoskowitzSpectrum, boolean applyJONSWAPSpectrum){
        m_Params.wind_dependency = 90f/180f;
        _gravityScale = 1f;
        m_Params.wave_amplitude = 1;

        _powerLog[0] =-6;
        _powerLog[1] =-6;
        _powerLog[2] =-6;
        _powerLog[3] =-4.904258f;
        _powerLog[4] =-4.593173f;
        _powerLog[5] =-3.343004f;
        _powerLog[6] =-3.166065f;
        _powerLog[7] =-3.090722f;
        _powerLog[8] =-1.50334f;
        _powerLog[9] =0.2974477f;
        _powerLog[10] =0.5362735f;
        _powerLog[11] =1.028262f;
        _powerLog[12] =2.790329f;
        _powerLog[13] =3;

        Arrays.fill(_powerDisabled, false);

        _chop = 1;

        Arrays.fill(_chopScales, 1);
        Arrays.fill(_gravityScales, 1);

        m_Params.wind_speed = 36/3.6f;
        float _smallWavelengthMultiplier = 1;

        if (applyPhillipsSpectrum)
        {
            ApplyPhillipsSpectrum(m_Params.wind_speed, _smallWavelengthMultiplier);
        }

        if (applyPiersonMoskowitzSpectrum)
        {
            ApplyPiersonMoskowitzSpectrum(m_Params.wind_speed, _smallWavelengthMultiplier);
        }

        m_Params.fetch =500000;
        if (applyJONSWAPSpectrum)
        {
            ApplyJONSWAPSpectrum(m_Params.wind_speed, m_Params.fetch, _smallWavelengthMultiplier);
        }
    }

    void setWaveWhirlpool(boolean applyPhillipsSpectrum, boolean applyPiersonMoskowitzSpectrum, boolean applyJONSWAPSpectrum){
        m_Params.wind_dependency = 90f/180f;
        _gravityScale = 1f;
        m_Params.wave_amplitude = 1;

        _powerLog[0] =-5.207943f;
        _powerLog[1] =-4.456718f;
        _powerLog[2] =-3.709543f;
        _powerLog[3] =-2.978567f;
        _powerLog[4] =-2.312389f;
        _powerLog[5] =-1.905401f;
        _powerLog[6] =-2.535175f;
        _powerLog[7] =-6;
        _powerLog[8] =-6;
        _powerLog[9] =-6;
        _powerLog[10] =-6;
        _powerLog[11] =-6;
        _powerLog[12] =-6;
        _powerLog[13] =-6;

        Arrays.fill(_powerDisabled, false);

        _chop = 1.5f;

        Arrays.fill(_chopScales, 1);
        Arrays.fill(_gravityScales, 1);

        m_Params.wind_speed = 17/3.6f;
        float _smallWavelengthMultiplier = 1;

        if (applyPhillipsSpectrum)
        {
            ApplyPhillipsSpectrum(m_Params.wind_speed, _smallWavelengthMultiplier);
        }

        if (applyPiersonMoskowitzSpectrum)
        {
            ApplyPiersonMoskowitzSpectrum(m_Params.wind_speed, _smallWavelengthMultiplier);
        }

        m_Params.fetch =500000;
        if (applyJONSWAPSpectrum)
        {
            ApplyJONSWAPSpectrum(m_Params.wind_speed, m_Params.fetch, _smallWavelengthMultiplier);
        }
    }
}
