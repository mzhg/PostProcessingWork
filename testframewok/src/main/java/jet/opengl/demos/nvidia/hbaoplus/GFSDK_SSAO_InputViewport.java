package jet.opengl.demos.nvidia.hbaoplus;

/**
 * [Optional] Input viewport.<p>
 * Remarks:<ul>
 * <li> The Viewport defines a sub-area of the input & output full-resolution textures to be sourced and rendered to.
 * 	Only the depth pixels within the viewport sub-area contribute to the RenderAO output.
 * <li> The Viewport's MinDepth and MaxDepth values are ignored except if DepthTextureType is HARDWARE_DEPTHS_SUB_RANGE.
 * <li> If Enable is false, a default input viewport is used with:
 *  <pre>
 *  TopLeftX = 0
 *  TopLeftY = 0
 *  Width    = InputDepthTexture.Width
 *  Height   = InputDepthTexture.Height
 *  MinDepth = 0.f
 *  MaxDepth = 1.f
 *  </pre>
 * </ul>
 */
public class GFSDK_SSAO_InputViewport {

	/** To use the provided viewport data (instead of the default viewport) */
	public boolean enable;
	/** X coordinate of the top-left corner of the viewport rectangle, in pixels */
    public int     topLeftX;
    /** Y coordinate of the top-left corner of the viewport rectangle, in pixels */
    public int     topLeftY;
    /** The width of the viewport rectangle, in pixels */
    public int     width;
    /** The height of the viewport rectangle, in pixels */
    public int     height;
    /** The minimum depth for GFSDK_SSAO_HARDWARE_DEPTHS_SUB_RANGE */
    public float   minDepth = 0.0f;
    /** The maximum depth for GFSDK_SSAO_HARDWARE_DEPTHS_SUB_RANGE */
    public float   maxDepth = 1.0f;
}
