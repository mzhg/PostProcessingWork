package jet.opengl.demos.nvidia.waves.samples;

import org.lwjgl.util.vector.Vector2f;

public class OceanParameter {

	/** Must be power of 2. */
	public int dmap_dim;
	/** Typical value is 1000 ~ 2000 */
	public float patch_length;

	/** Adjust the time interval for simulation. */
	public float time_scale;
	// Amplitude for transverse wave. Around 1.0
	public float wave_amplitude;
	// Wind direction. Normalization not required.
	public final Vector2f wind_dir = new Vector2f();
	// Around 100 ~ 1000
	public float wind_speed;
	// This value damps out the waves against the wind direction.
	// Smaller value means higher wind dependency.
	public float wind_dependency;
	// The amplitude for longitudinal wave. Must be positive.
	public float choppy_scale;
}
