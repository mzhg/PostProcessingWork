package jet.opengl.demos.nvidia.hbaoplus;

/**
 * Remarks: <ul>
 * <li>The final occlusion is a weighted sum of 2 occlusion contributions. The SmallScaleAO and LargeScaleAO parameters are the weights.
 * <li>Setting the DepthStorage parameter to FP16_VIEW_DEPTHS is fastest but may introduce minor false-occlusion artifacts for large depths.
 * </ul>
 */
public class GFSDK_SSAO_Parameters {

	/** The AO radius in meters */
	public float                radius = 1.f;
	/** To hide low-tessellation artifacts // 0.0~0.5 */
	public float                bias = 0.1f;
	/** Scale factor for the small-scale AO, the greater the darker // 0.0~4.0 */
	public float                smallScaleAO = 1.f;
	/** Scale factor for the large-scale AO, the greater the darker // 0.0~4.0 */
	public float                largeScaleAO = 1.f;
	/** The final AO output is pow(AO, powerExponent) // 1.0~8.0 */
	public float                powerExponent = 2.f;
	/** To limit the occlusion scale in the foreground */
    public final GFSDK_SSAO_ForegroundAO   foregroundAO = new GFSDK_SSAO_ForegroundAO();
    /** To add larger-scale occlusion in the distance */
    public final GFSDK_SSAO_BackgroundAO   backgroundAO = new GFSDK_SSAO_BackgroundAO();
    /** Quality / performance tradeoff */
    public GFSDK_SSAO_DepthStorage    depthStorage = GFSDK_SSAO_DepthStorage.GFSDK_SSAO_FP16_VIEW_DEPTHS;
    /** To hide possible false-occlusion artifacts near screen borders */
    public GFSDK_SSAO_DepthClampMode  depthClampMode = GFSDK_SSAO_DepthClampMode.GFSDK_SSAO_CLAMP_TO_EDGE;
    /** Optional Z threshold, to hide possible depth-precision artifacts */
    public final GFSDK_SSAO_DepthThreshold  depthThreshold = new GFSDK_SSAO_DepthThreshold();
    /** Optional AO blur, to blur the AO before compositing it */
    public final GFSDK_SSAO_BlurParameters  blur = new GFSDK_SSAO_BlurParameters();
}
