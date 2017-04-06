package com.nvidia.developer.opengl.app;

/**
 * Automated input-to-camera motion mapping<p>
 * Camera motion mode.
 * @author Nvidia 2014-9-13 12:36
 * Updated at 2017-04-06 15:06:52
 */
public enum NvCameraMotionType {

	/** Camera orbits the world origin */
	ORBITAL,
	/** Camera moves as in a 3D, first-person shooter */
	FIRST_PERSON,
	/** Camera pans and zooms in 2D */
	PAN_ZOOM,
	/** Two independent orbital transforms */
	DUAL_ORBITAL
}
