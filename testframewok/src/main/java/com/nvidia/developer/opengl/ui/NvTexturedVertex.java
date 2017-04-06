package com.nvidia.developer.opengl.ui;

import java.nio.FloatBuffer;

/** This is a helper structure for rendering sets of 2D textured vertices. */
class NvTexturedVertex {

	/** 2d vertex coord */
	public float posX, posY;
	/** vertex texturing position */
	public float uvX, uvY;
	
	void store(FloatBuffer buf){
		buf.put(posX);
		buf.put(posY);
		buf.put(uvX);
		buf.put(uvY);
	}
}
