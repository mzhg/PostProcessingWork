package com.nvidia.developer.opengl.models.sdkmesh;

import java.io.IOException;
import java.nio.ByteBuffer;

import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.texture.TextureUtils;

public interface SDKmeshCallbacks {

	/**
	 * Create a OpenGL texture object with the given filename.
	 * @param filename The texture file name
	 * @return An OpenGL texture object
	 */
	default int createTextureFromFile(String filename){
		try {
			return TextureUtils.createTexture2DFromFile(filename, true).getTexture();
			// TODO
		} catch (IOException e) {
			e.printStackTrace();
		}

		return 0;
	}
	
	/**
	 * Create an OpenGL Vertex Buffer object.
	 * @param usage The usage of the Buffer.
	 * @param data The buffer data.
	 * @return An OpenGL Vertex Buffer object
	 */
	default int createVertexBuffer(int usage, ByteBuffer data){
		GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
		int buffer = gl.glGenBuffer();
		gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, buffer);
		gl.glBufferData(GLenum.GL_ARRAY_BUFFER, data, usage);
		gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, 0);
		return buffer;
	}
	
	/**
	 * Create an OpenGL Element Buffer object.
	 * @param usage The usage of the Buffer.
	 * @param data The buffer data.
	 * @return An OpenGL Element Buffer object
	 */
	default int createIndexBuffer(int usage, ByteBuffer data){
		GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
		int buffer = gl.glGenBuffer();
		gl.glBindBuffer(GLenum.GL_ELEMENT_ARRAY_BUFFER, buffer);
		gl.glBufferData(GLenum.GL_ELEMENT_ARRAY_BUFFER, data, usage);
		gl.glBindBuffer(GLenum.GL_ELEMENT_ARRAY_BUFFER, 0);
		return buffer;
	}
}
