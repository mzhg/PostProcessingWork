package com.nvidia.developer.opengl.app;

/**
 * Camera transform mode.
 * @author Nvidia 2014-9-13 12:42
 */
public interface NvCameraXformType {

	/** Default transform */
	int MAIN = 0,
	/** Secondary transform */
	SECONDARY = 1;
	/** Number of transforms */
	public static final int COUNT = 2;
}
