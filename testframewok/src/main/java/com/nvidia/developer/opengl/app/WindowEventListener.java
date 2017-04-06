package com.nvidia.developer.opengl.app;

public interface WindowEventListener {

	/**
	 * The window move callback.
	 *
	 * @param xpos   the new x-coordinate, in pixels, of the upper-left corner of the client area of the window
	 * @param ypos   the new y-coordinate, in pixels, of the upper-left corner of the client area of the window
	 */
	public void windowPos(int xpos, int ypos);
	
	/**
	 * The window close callback. On Android, this method never called!
	 */
	public void windowClose();
	
	/**
	 * The window focus callback.
	 *
	 * @param focused true if the window was focused, or false if it was defocused
	 */
	public void windowFocus(boolean focused);
	
	/**
	 * The window iconify callback.
	 *
	 * @param iconified true if the window was iconified, or false if it was restored
	 */
	public void windowIconify(boolean iconified);
}
