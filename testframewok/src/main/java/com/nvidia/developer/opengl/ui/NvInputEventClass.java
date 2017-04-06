package com.nvidia.developer.opengl.ui;

/**
 * mouse/stylus (accurate) vs finger (inaccurate).
 *  these are used as array indices + max size!<p>
 *  These are types of input devices generating pointer events.
 *  We might use this information to adjust hit margins and such
 *  based on the average size of the pointer.
 * @author Nvidia 2014-9-2
 *
 */
public interface NvInputEventClass {
	/** No input type specified. */
	static final int NONE = 0;
	/** Mouse input */
	static final int MOUSE = 1;
	/** Touch input */
	static final int TOUCH = 2;
	/** Stylus input */
	static final int STYLUS = 3;
}
