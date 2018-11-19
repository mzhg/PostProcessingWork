package jet.opengl.demos.nvidia.volumelight;

/** Quality of upsampling */
public enum UpsampleQuality {

	UNKNOWN,
	/** Point sampling (no filter) */
	POINT,
	/** Bilinear Filtering */
	BILINEAR,
	/** Bilateral Filtering (using depth) */
	BILATERAL,
}
