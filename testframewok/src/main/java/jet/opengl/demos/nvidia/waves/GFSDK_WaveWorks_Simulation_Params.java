package jet.opengl.demos.nvidia.waves;

import org.lwjgl.util.vector.Vector2f;

/**
 * Created by Administrator on 2017/7/23 0023.
 */

public class GFSDK_WaveWorks_Simulation_Params {
    /** Global scale factor for simulated wave amplitude*/
    public float wave_amplitude;
    /** The direction of the wind inducing the waves */
    public final Vector2f wind_dir=new Vector2f();
    /**The speed of the wind inducing the waves. If GFSDK_WaveWorks_Simulation_Settings.UseBeaufortScale is set, this is
     interpreted as a Beaufort scale value. Otherwise, it is interpreted as metres per second*/
    public float wind_speed;
    /** The degree to which waves appear to move in the wind direction (vs. standing waves), in the [0,1] range */
    public float wind_dependency;
    /**In addition to height displacements, the simulation also applies lateral displacements. This controls the non-linearity
      and therefore 'choppiness' in the resulting wave shapes. Should normally be set in the [0,1] range.*/
    public float choppy_scale;
    /**
     * The simulation spectrum is low-pass filtered to eliminate wavelengths that could end up under-sampled, this controls
     * how much of the frequency range is considered 'high frequency' (i.e. small wave).
     */
    public float small_wave_fraction;

    /** The global time multiplier */
    public float time_scale;

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
}
