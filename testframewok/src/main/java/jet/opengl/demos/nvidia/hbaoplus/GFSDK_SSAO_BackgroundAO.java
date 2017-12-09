package jet.opengl.demos.nvidia.hbaoplus;

/**
 * When enabled, the screen-space AO kernel radius is:<ul>
 * <li> inversely proportional to ViewDepth for ViewDepth < BackgroundViewDepth
 * <li> uniform in screen-space for ViewDepth >= BackgroundViewDepth (instead of falling off to zero)
 * </ul>
 */
public class GFSDK_SSAO_BackgroundAO {

	/** Enabling this may have a small performance impact */
	public boolean enable = false;
	/** View-space depth at which the AO footprint should stop falling off with depth */
	public float backgroundViewDepth = 0.f;
}
