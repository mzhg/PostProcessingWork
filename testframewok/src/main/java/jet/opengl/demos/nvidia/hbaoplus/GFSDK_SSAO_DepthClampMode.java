package jet.opengl.demos.nvidia.hbaoplus;

public enum GFSDK_SSAO_DepthClampMode {

	/** Use clamp-to-edge when sampling depth (may cause false occlusion near screen borders) */
	GFSDK_SSAO_CLAMP_TO_EDGE,
	/** Use clamp-to-border when sampling depth (may cause halos near screen borders) */
    GFSDK_SSAO_CLAMP_TO_BORDER,
}
