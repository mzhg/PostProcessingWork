package jet.opengl.demos.nvidia.hbaoplus;

/**
 * When enabled, the screen-space AO kernel radius is:<ul>
 * <li> inversely proportional to ViewDepth for ViewDepth > ForegroundViewDepth
 * <li> uniform in screen-space for ViewDepth <= ForegroundViewDepth
 * </ul>
 */
public class GFSDK_SSAO_ForegroundAO {

	/** Enabling this may have a small performance impact */
	public boolean enable;
	/** View-space depth at which the AO footprint should get clamped */
	public float foreGroundViewDepth;
}
