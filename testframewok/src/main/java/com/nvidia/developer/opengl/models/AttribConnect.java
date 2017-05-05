package com.nvidia.developer.opengl.models;

import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;

interface AttribConnect {

	void enable(int index, int size, int type, int stride, int offset, int divisor);
	
	void disable(int index);
	
	static final AttribConnect VERTEX_ATTRIB = new AttribConnect() {
		public void enable(int index, int size, int type, int stride, int offset, int divisor) {
			GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
			gl.glVertexAttribPointer(index, size, type, false, stride, offset);
			gl.glEnableVertexAttribArray(index);
			gl.glVertexAttribDivisor(index, divisor);
		}
		
		@Override
		public void disable(int index) {
			GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();

			gl.glDisableVertexAttribArray(index);
			gl.glVertexAttribDivisor(index, 0);
		}
	};
}
