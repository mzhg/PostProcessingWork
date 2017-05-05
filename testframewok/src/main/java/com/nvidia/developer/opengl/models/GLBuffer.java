package com.nvidia.developer.opengl.models;

import java.nio.Buffer;
import java.nio.ByteBuffer;

import jet.opengl.postprocessing.common.GLCheck;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;

/** This class represent a OpenGL buffer object. */
public class GLBuffer {

	int index;  // used for GLVAO.
	int target;
	int usage;
	
	int bufferID;
	long bufferSize;
	
	private ByteBuffer bufferData;
	private final GLFuncProvider gl;
	
	public GLBuffer(int target, int usage) {
		this.target = target;
		this.usage = usage;

		gl = GLFuncProviderFactory.getGLFuncProvider();
	}
	
	public void bind(){
		gl.glBindBuffer(target, bufferID);
	}
	
	public void unbind() { gl.glBindBuffer(target, 0);}
	
	public void load(Buffer buffer){
		boolean binded = false;
		if(bufferID == 0){
			bufferID = gl.glGenBuffer();
			gl.glBindBuffer(target, bufferID);
			binded = true;
		}

		gl.glBufferData(target, buffer, usage);

		if(binded)
			gl.glBindBuffer(target, 0);  // unbind the buffer.

		GLCheck.checkError("GLBuffer::load(Buffer)");
	}
	
	public void load(long size){
		if(bufferID == 0){
			bufferID = gl.glGenBuffer();
			gl.glBindBuffer(target, bufferID);
		}

		bufferSize = size;
		gl.glBufferData(target, (int)size, usage);
		GLCheck.checkError("GLBuffer::load(size)");
	}

	public void update(int offset, int bufferSize, Buffer data){
		gl.glBufferSubData(target, offset, data);
	}
	
	/* Create the mapping buffer for preparing update */
//	public void beginUpdate(int access){
//		bufferData = (ByteBuffer) GLES31.glMapBufferRange(target, 0, (int)bufferSize, access);
//	}
	
	public ByteBuffer getMappingBuffer() { return bufferData;}
	
//	public void finishUpdate(){
//		GLES31.glUnmapBuffer(target);
//	}
	
	public void dispose(){
		gl.glDeleteBuffer(bufferID);
	}
	
	public long getBufferSize() { return bufferSize;}
}
