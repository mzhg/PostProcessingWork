package jet.opengl.demos.nvidia.hbaoplus;

public enum GFSDK_SSAO_BlendMode {

	/** Overwrite the destination RGB with the AO, preserving alpha */
	GFSDK_SSAO_OVERWRITE_RGB,
	/** Multiply the AO over the destination RGB, preserving alpha */
    GFSDK_SSAO_MULTIPLY_RGB,
    /** Composite the AO using a custom blend state */
    GFSDK_SSAO_CUSTOM_BLEND,
}
