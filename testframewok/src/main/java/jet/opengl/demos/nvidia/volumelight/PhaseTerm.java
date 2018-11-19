package jet.opengl.demos.nvidia.volumelight;

import org.lwjgl.util.vector.Vector3f;

/** Describes one component of the phase function */
public class PhaseTerm {

	/** Phase function this term uses */
	public PhaseFunctionType ePhaseFunc;
	/** Optical density in [R,G,B] */
	public final Vector3f vDensity = new Vector3f();
	/** Degree/direction of anisotropy (-1, 1) (HG only) */
	public float fEccentricity;
}
