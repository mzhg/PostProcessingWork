package com.nvidia.developer.opengl.app;

/** Basic single-point pointer event. 
 * @author Nvidia 2014-9-12 17:48*/
public class NvPointerEvent {

	/** x value in pixel-space */
	public float m_x;
	
	/** y value in pixel-space */
	public float m_y;
	/** Unique ID for tracking multiple touches */
	public int m_id;
}
