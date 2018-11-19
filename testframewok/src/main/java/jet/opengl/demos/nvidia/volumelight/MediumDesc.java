package jet.opengl.demos.nvidia.volumelight;

import org.lwjgl.util.vector.Vector3f;

import jet.opengl.postprocessing.util.CommonUtil;

/** Volume Medium Description */
public class MediumDesc {

	/** Absorpsive component of the medium */
	public final Vector3f vAbsorption = new Vector3f();
	/** Number of valid phase terms */
	public int uNumPhaseTerms;
	
	public final PhaseTerm[] phaseTerms = new PhaseTerm[VLConstant.MAX_PHASE_TERMS];
	
	public MediumDesc() {
		for(int i = 0; i < phaseTerms.length;i++)
			phaseTerms[i] = new PhaseTerm();
	}
}
