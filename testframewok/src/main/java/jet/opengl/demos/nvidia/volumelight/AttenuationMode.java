package jet.opengl.demos.nvidia.volumelight;

/** Specifies the type of distance attenuation applied to the light */
public enum AttenuationMode {

	UNKNOWN,
    /**  No attenuation */
	NONE,           //!<
	/** f(x) = 1-(A+Bx+Cx^2) */
	POLYNOMIAL,
	/** f(x) = 1/(A+Bx+Cx^2)+D */
	INV_POLYNOMIAL, 
}
