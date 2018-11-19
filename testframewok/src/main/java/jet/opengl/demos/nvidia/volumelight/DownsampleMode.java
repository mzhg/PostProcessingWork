package jet.opengl.demos.nvidia.volumelight;

/**
 * Specifies the godrays buffer resolution relative to framebuffer
 */
public enum DownsampleMode {

	UNKNOWN,
	/** Same resolution as framebuffer */
	FULL,
	/** Half dimensions of framebuffer (1x downsample) */	
	HALF,
	/** Quarter dimensions of framebuffer (2x downsample) */
	QUARTER,
}
