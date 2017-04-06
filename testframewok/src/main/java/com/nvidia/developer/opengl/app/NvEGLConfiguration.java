package com.nvidia.developer.opengl.app;


import com.nvidia.developer.opengl.utils.NvGfxAPIVersion;

/** Cross-platform OpenGL Context APIs and information */
public class NvEGLConfiguration {

	/** API and version, defaults GLES2.0 */
	public NvGfxAPIVersion apiVer;
	/** red color channel depth in bits, defaults 8 */
	public int redBits;
	/** green color channel depth in bits, defaults 8 */
	public int greenBits;
	/** blue color channel depth in bits, defaults 8 */
	public int blueBits;
	/** alpha color channel depth in bits, defaults 8 */
	public int alphaBits;
	/** depth color channel depth in bits, defaults 24 */
	public int depthBits;
	/** stencil color channel depth in bits, defaults 0 */
	public int stencilBits;
	/** True will create a debug opengl context, defaults false. */
	public boolean debugContext;
	/** The count of the MSAA samplers. */
	public int multiSamplers = 0;
	
	/**
	 * Inline all-elements constructor.
	 * @param _api the API and version information
	 * @param r the red color depth in bits
	 * @param g the green color depth in bits
	 * @param b the blue color depth in bits
	 * @param a the alpha color depth in bits
	 * @param d the depth buffer depth in bits
	 * @param s the stencil buffer depth in bits
	 */
	public NvEGLConfiguration(NvGfxAPIVersion _api, int r, int g, int b, int a, int d, int s) {
		apiVer = _api;
		redBits = r;
		greenBits = g;
		blueBits = b;
		alphaBits = a;
		depthBits = d;
		stencilBits = s;
	}
	
	public NvEGLConfiguration() {
		this(NvGfxAPIVersion.GLES2, 8, 8, 8, 8, 24, 0);
	}
	
	public NvEGLConfiguration(NvGfxAPIVersion _api) {
		this(_api, 8, 8, 8, 8, 24, 0);
	}
	
	public NvEGLConfiguration(NvGfxAPIVersion _api, int r, int g, int b, int a) {
		this(_api, r, g, b, a, 24, 0);
	}
}
