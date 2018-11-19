package jet.opengl.demos.nvidia.volumelight;

/** Specifies the type of angular falloff to apply to the spotlight */
public enum SpotlightFalloffMode {

	 UNKNOWN,
     /** No falloff (constant brightness across cone cross-section) */
	 NONE,
     /** A_fixed(vL, vP) = (dot(vL, vP) - theta_max)/(1 - theta_max) */
	 FIXED,
	 /** A_custom(vL, vP) = (A_fixed(vL, vP))^n */
     CUSTOM,
}
