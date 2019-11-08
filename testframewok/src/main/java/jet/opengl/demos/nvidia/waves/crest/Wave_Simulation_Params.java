package jet.opengl.demos.nvidia.waves.crest;

import org.lwjgl.util.vector.Vector2f;

import jet.opengl.postprocessing.common.GLenum;

/**
 * Created by Administrator on 2019/10/29.
 */

public class Wave_Simulation_Params {
    /** Global scale factor for simulated wave amplitude*/
    public float wave_amplitude = 1;
    /** The direction of the wind inducing the waves. x along the x-axis, y along the z-axis in the right hand coordinate */
    public final Vector2f wind_dir=new Vector2f(0.8f, 0.6f);
    /**The speed of the wind inducing the waves. If GFSDK_WaveWorks_Simulation_Settings.UseBeaufortScale is set, this is
     interpreted as a Beaufort scale value. Otherwise, it is interpreted as metres per second*/
    public float wind_speed = 10;

    /** Length of area that wind excites waves. Applies only to JONSWAP*/
    public float fetch = 500_000f;

    /** waveDirectionVariance.<p> The degree to which waves appear to move in the wind direction (vs. standing waves), in the [0,1] range */
    public float wind_dependency = 0.5f;
    /**In addition to height displacements, the simulation also applies lateral displacements. This controls the non-linearity
      and therefore 'choppiness' in the resulting wave shapes. Should normally be set in the [0,1] range. */
    public float choppy_scale = 0.5f;
    /**
     * _smallWavelengthMultiplier<p>
     * The simulation spectrum is low-pass filtered to eliminate wavelengths that could end up under-sampled, this controls
     * how much of the frequency range is considered 'high frequency' (i.e. small wave).
     */
    public float small_wave_fraction = 1f;

    /** The global time multiplier */
    public float time_scale = 1;

    /** How many wave components to generate in each octave.*/
    public int components_per_octave = 8;

    /** Make waves converge towards a point. */
    public boolean direct_towards_Point = false;
    /** Target point XZ to converge to.*/
    public final Vector2f point_positionXZ = new Vector2f();
    /** Inner and outer radii. Influence at full strength at inner radius, fades off at outer radius.*/
    public final Vector2f point_radii = new Vector2f(100f, 200f);

    /**
     * Ping pong between render targets to do the combine. Disabling this uses a compute shader instead which doesn't need
     * to copy back and forth between targets, but has dodgy historical support as pre-DX11.3 hardware may not support typed UAV loads.
     */
    public boolean shape_combine_pass_pingpong;

    /** Used for the wave animation. */
    public int random_seed;

    /** Rang [0,1] */
    public float gertner_weight = 1f;

    public boolean evaluate_spectrum_runtime = true;

    public float min_gridsize = 0f;
    //        [Range(0f, 32f), Tooltip("NOT CURRENTLY WORKING. The wave sim will not run if the simulation grid is bigger in resolution than this size. Zero means no constraint/unlimited resolutions. Useful to limit sim range for performance."),
//    HideInInspector]
    public float max_gridsize = 0f;

    //        [Header("Stability")]
//            [Range(0f, 1f), Tooltip("How much energy is dissipated each frame. Helps sim stability, but limits how far ripples will propagate. Set this as large as possible/acceptable.")]
    public float damping = 0.25f;
    //        [Range(0.1f, 3f), Tooltip("Stability measurement. Lower values means more stable sim, at the cost of more computation. This value should be set as large as possible until sim instabilities/flickering begin to appear.")]
    public float courant_number = 1f;
    //        [Range(1, 8), Tooltip("How many simulation substeps are allowed per frame. Run at target framerate with the OceanDebugGUI visible to see how many substeps are being done when the camera is close to the water, and set the limit to this value. If the max substeps is set lower than this value, the detailed/high frequency waves will propagate slower than they would in reality. For many applications this may not be an issue.")]
    public int max_simsteps_perframe = 3;

    //        [Header("Displacement Generation")]
//            [Range(0f, 20f), Tooltip("Induce horizontal displacements to sharpen simulated waves.")]
    public float horiz_displace = 3f;
    //        [Range(0f, 1f), Tooltip("Clamp displacement to help prevent self-intersection in steep waves. Zero means unclamped.")]
    public float displace_clamp = 0.3f;


    //        [Range(0f, 3f), Tooltip("Foam will be generated in water shallower than this depth.")]
    public float shoreline_foam_maxdepth = 0.65f;
    //        [Range(0f, 5f), Tooltip("Scales intensity of foam generated in shallow water.")]
    public float shoreline_foam_strength = 2f;
    //        [Tooltip("The rendertexture format to use for the foam simulation")]
    public int foam_texture_format = GLenum.GL_R16F;

    /**Scales intensity of foam generated from waves(0,5f).*/
    public float foam_strength = 1f;

    /** Speed at which foam fades/dissipates. (0,20f)*/
    public float foam_fade_rate = 0.8f;
    /** How much of the waves generate foam. (0, 1f)*/
    public float foam_coverage = 0.8f;

    /** Multiplier for physics gravity. More gravity means faster waves.*/
    public float gravityMultiplier = 1;

    //    [Range(0f, 32f), Tooltip("Jitter diameter for soft shadows, controls softness of this shadowing component.")]
    public float jitter_diameter_soft = 15f;

    //        [Range(0f, 1f), Tooltip("Current frame weight for accumulation over frames for soft shadows. Roughly means 'responsiveness' for soft shadows.")]
    public float current_frameweight_soft = 0.03f;

    //        [Range(0f, 32f), Tooltip("Jitter diameter for hard shadows, controls softness of this shadowing component.")]
    public float jitter_diameter_hard = 0.6f;

    //        [Range(0f, 1f), Tooltip("Current frame weight for accumulation over frames for hard shadows. Roughly means 'responsiveness' for hard shadows.")]
    public float ccurrent_frameWeight_hard = 0.15f;

    //        [Tooltip("Whether to disable the null light warning, use this if you assign it dynamically and expect it to be null at points")]
//    public boolean allowNullLight = false;

    /** Water depth information used for shallow water, shoreline foam, wave attenuation, among others*/
    public boolean create_seafloordepth = true;

    //        [Tooltip("Simulation of foam created in choppy water and dissipating over time."), SerializeField]
    public boolean create_foam = true;

    //        [Tooltip("Dynamic waves generated from interactions with objects such as boats."), SerializeField]
    public boolean create_dynamic_wave = false;

    //        [Tooltip("Horizontal motion of water body, akin to water currents."), SerializeField]
    public boolean create_flow = false;
//    public SimSettingsFlow _simSettingsFlow;

    //        [Tooltip("Shadow information used for lighting water."), SerializeField]
    public boolean CreateShadowData = false;

    /** Which octave to render into, for example set this to 2 to use render into the 2m-4m octave. These refer to the same octaves as the wave spectrum editor. Set this value to 0 to render into all LODs.*/
    public float octave_wave_length = 0f;

    /** Inform ocean how much this input will displace the ocean surface vertically. This is used to set bounding box heights for the ocean tiles.*/
    public float max_displacement_Vertical;

    /** Inform ocean how much this input will displace the ocean surface horizontally. This is used to set bounding box widths for the ocean tiles.*/
    public float max_displacement_horizontal;

    /** Use the bounding box of an attached renderer component to determine the max vertical displacement.*/
    public boolean report_bounds_to_system = true;

    /** How much waves are dampened in shallow water. (0,1f)*/
    public float attenuation_in_shallows = 0.95f;

    ////////// -------------------------------- Readback configs ------------------------------
    /** Minimum floating object width. The larger the objects that will float, the lower the resolution of the read data. If an object is small, the highest resolution LODs will be sample for physics. This is an optimisation. Set to 0 to disable this optimisation and always copy high res data.*/
    public float min_object_width = 3;

    /** Similar to the minimum width, but this setting will exclude the larger LODs from being copied. Set to 0 to disable this optimisation and always copy low res data. */
    public float max_object_width = 500;

    public Wave_Simulation_Params() {}

    public Wave_Simulation_Params(Wave_Simulation_Params o) {
        set(o);
    }

    public void set(Wave_Simulation_Params o){
        wave_amplitude = o.wave_amplitude;
        wind_dir.set(o.wind_dir);
        wind_speed = o.wind_speed;
        fetch = o.fetch;
        wind_dependency = o.wind_dependency;
        choppy_scale = o.choppy_scale;
        small_wave_fraction = o.small_wave_fraction;
        time_scale = o.time_scale;
        components_per_octave = o.components_per_octave;
        direct_towards_Point = o.direct_towards_Point;
        point_positionXZ.set(o.point_positionXZ);
        point_radii.set(o.point_radii);
        shape_combine_pass_pingpong = o.shape_combine_pass_pingpong;
        random_seed = o.random_seed;
        gertner_weight = o.gertner_weight;
        evaluate_spectrum_runtime = o.evaluate_spectrum_runtime;
        min_gridsize = o.min_gridsize;
        max_gridsize = o.max_gridsize;
        damping = o.damping;
        courant_number = o.courant_number;
        max_simsteps_perframe = o.max_simsteps_perframe;
        horiz_displace = o.horiz_displace;
        displace_clamp = o.displace_clamp;
        shoreline_foam_maxdepth = o.shoreline_foam_maxdepth;
        shoreline_foam_strength = o.shoreline_foam_strength;
        foam_texture_format = o.foam_texture_format;
        foam_strength = o.foam_strength;
        foam_fade_rate = o.foam_fade_rate;
        foam_coverage = o.foam_coverage;
        gravityMultiplier = o.gravityMultiplier;
        jitter_diameter_soft = o.jitter_diameter_soft;
        current_frameweight_soft = o.current_frameweight_soft;
        jitter_diameter_hard = o.jitter_diameter_hard;
        ccurrent_frameWeight_hard = o.ccurrent_frameWeight_hard;
        create_seafloordepth = o.create_seafloordepth;
        create_foam = o.create_foam;
        create_dynamic_wave = o.create_dynamic_wave;
        create_flow = o.create_flow;
        CreateShadowData = o.CreateShadowData;
        octave_wave_length = o.octave_wave_length;
        max_displacement_Vertical = o.max_displacement_Vertical;
        max_displacement_horizontal = o.max_displacement_horizontal;
        report_bounds_to_system = o.report_bounds_to_system;
        min_object_width = o.min_object_width;
        max_object_width = o.max_object_width;
    }
}
