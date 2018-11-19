package jet.opengl.demos.nvidia.volumelight;

/** Phase function to use for this media term */
public enum PhaseFunctionType {

	UNKNOWN,
	/** Isotropic scattering (equivalent to HG with 0 eccentricity, but more efficient) */
	ISOTROPIC,
	/** Rayleigh scattering term (air/small molecules) */
	RAYLEIGH,
	/** Scattering term with variable anisotropy */
	HENYEYGREENSTEIN,
    /** Slightly forward-scattering */
	MIE_HAZY,
	/** Densely forward-scattering */
    MIE_MURKY,
}
