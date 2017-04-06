package com.nvidia.developer.opengl.app;

/** Pointer input action values. 
 * @author Nvidia 2014-9-12 17:34
 */
public interface NvPointerActionType {

	/** touch or button release */
	public static final int UP = 0;
	/** touch or button press */
	public static final int DOWN = 1;
	/** touch or mouse pointer movement */
	public static final int MOTION = 2;
	/** multitouch additional touch press */
	public static final int EXTRA_DOWN = 4;
	/** multitouch additional touch release */
	public static final int EXTRA_UP = 8;
}
