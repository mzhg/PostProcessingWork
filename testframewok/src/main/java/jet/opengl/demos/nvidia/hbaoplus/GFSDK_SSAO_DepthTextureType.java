package jet.opengl.demos.nvidia.hbaoplus;

public enum GFSDK_SSAO_DepthTextureType {

	/** Non-linear depths in the range [0.f,1.f] */
	GFSDK_SSAO_HARDWARE_DEPTHS,
	/** Non-linear depths in the range [Viewport.MinDepth,Viewport.MaxDepth] */
    GFSDK_SSAO_HARDWARE_DEPTHS_SUB_RANGE,
    /** Linear depths in the range [ZNear,ZFar] */
    GFSDK_SSAO_VIEW_DEPTHS,
}
