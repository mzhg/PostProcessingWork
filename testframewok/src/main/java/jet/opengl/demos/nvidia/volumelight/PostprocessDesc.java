package jet.opengl.demos.nvidia.volumelight;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

/** Post-Processing Behavior Description */
public class PostprocessDesc {

	/** Camera view projection without jitter */
	public final Matrix4f mUnjitteredViewProj = new Matrix4f();
	
	/** Weight of pixel history smoothing (0.0 for off) */
	public float fTemporalFactor;
	/** Threshold of frame movement to use temporal history */
	public float fFilterThreshold;
	/** Quality of upsampling to use */
	public UpsampleQuality eUpsampleQuality;
	/** Light to use as "faked" multiscattering */
	public final Vector3f vFogLight = new Vector3f();
	/** strength of faked multiscatter effect */
	public float fMultiscatter;
	/** Apply fogging based on scattering */
	public boolean bDoFog;		
	/** Ignore depth values of (1.0f) for fogging */
	public boolean bIgnoreSkyFog;
	/** Blend factor to use for compositing */
	public float  fBlendfactor;
}
