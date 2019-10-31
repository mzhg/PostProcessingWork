package jet.opengl.demos.nvidia.waves.crest;

import org.lwjgl.util.vector.Vector2f;

/**
 * Created by Administrator on 2019/10/29.
 */

public class Wave_Simulation_Params {
    /** Global scale factor for simulated wave amplitude*/
    public float wave_amplitude;
    /** The direction of the wind inducing the waves. x along the x-axis, y along the z-axis in the right hand coordinate */
    public final Vector2f wind_dir=new Vector2f();
    /**The speed of the wind inducing the waves. If GFSDK_WaveWorks_Simulation_Settings.UseBeaufortScale is set, this is
     interpreted as a Beaufort scale value. Otherwise, it is interpreted as metres per second*/
    public float wind_speed = 10;

    /** Length of area that wind excites waves. Applies only to JONSWAP*/
    public float fetch = 500000f;

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

    /** Used for the wave animation. */
    public int random_seed;

    /** Rang [0,1] */
    public float gertner_weight = 1f;

    public boolean evaluate_spectrum_runtime = true;

    /** The turbulent energy representing foam and bubbles spread in water starts generating on the tips of the waves if
     Jacobian of wave curvature gets higher than this threshold. The range is [0,1], the typical values are [0.2,0.4] range.*/
    public float foam_generation_threshold;
    /** The amount of turbulent energy injected in areas defined by foam_generation_threshold parameter on each simulation step.
      The range is [0,1], the typical values are [0,0.1] range.*/
    public float foam_generation_amount;
    /** The speed of spatial dissipation of turbulent energy. The range is [0,1], the typical values are in [0.5,1] range. */
    public float foam_dissipation_speed;
    /** In addition to spatial dissipation, the turbulent energy dissolves over time. This parameter sets the speed of
    dissolving over time. The range is [0,1], the typical values are in [0.9,0.99] range. */
    public float foam_falloff_speed;

    /** Multiplier for physics gravity. More gravity means faster waves.*/
    public float gravityMultiplier = 1;

    /** Water depth information used for shallow water, shoreline foam, wave attenuation, among others*/
    public boolean createSeaFloorDepthData = true;

    //        [Tooltip("Simulation of foam created in choppy water and dissipating over time."), SerializeField]
    public boolean CreateFoamSim = true;

    //        [Tooltip("Dynamic waves generated from interactions with objects such as boats."), SerializeField]
    public boolean CreateDynamicWaveSim = false;

    //        [Tooltip("Horizontal motion of water body, akin to water currents."), SerializeField]
    public boolean CreateFlowSim = false;
//    public SimSettingsFlow _simSettingsFlow;

    //        [Tooltip("Shadow information used for lighting water."), SerializeField]
    public boolean CreateShadowData = false;
}
