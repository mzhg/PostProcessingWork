package jet.opengl.demos.nvidia.hbaoplus;

public enum GFSDK_SSAO_DepthStencilMode {

	/** Composite the AO without any depth-stencil testing */
	GFSDK_SSAO_DISABLED_DEPTH_STENCIL,
	/** Composite the AO with a custom depth-stencil state */
    GFSDK_SSAO_CUSTOM_DEPTH_STENCIL,
}
