package jet.opengl.demos.nvidia.volumelight;

interface VLConstant {

	/** Maximum number of phase terms in a medium */
	static final int MAX_PHASE_TERMS = 4;
	
	/** Maximum number of sub-elements in a shadow map set */
	static final int MAX_SHADOWMAP_ELEMENTS = 4;
	
	static final int MAX_JITTER_STEPS = 8;

	// These need to match the values in ComputeLightLUT_CS.hlsl
	static final int LIGHT_LUT_DEPTH_RESOLUTION = 128;
	static final int LIGHT_LUT_WDOTV_RESOLUTION = 512;
}
