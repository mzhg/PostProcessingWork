package com.nvidia.developer.opengl.models;

import jet.opengl.postprocessing.common.GLenum;

public enum DrawMode {

	FILL(GLenum.GL_TRIANGLES),
	
	LINE(GLenum.GL_LINES),
	
	POINT(GLenum.GL_POINTS);
	
	final int drawMode;
	
	private DrawMode(int drawMode) {
		this.drawMode = drawMode;
	}
	
	/** Get the correspond OpenGL draw command.*/
	public int getGLMode() { return drawMode;}
}
