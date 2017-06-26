package com.nvidia.developer.opengl.models.sdkmesh;

import java.nio.ByteBuffer;

public interface SDKmeshCallbacks {

	/**
	 * Create a OpenGL texture object with the given filename.
	 * @param filename The texture file name
	 * @return An OpenGL texture object
	 */
	default int createTextureFromFile(String filename){
		return 0;
	}
	
	/**
	 * Create an OpenGL Vertex Buffer object.
	 * @param usage The usage of the Buffer.
	 * @param data The buffer data.
	 * @return An OpenGL Vertex Buffer object
	 */
	default int createVertexBuffer(int usage, ByteBuffer data){
		return 0;
	}
	
	/**
	 * Create an OpenGL Element Buffer object.
	 * @param usage The usage of the Buffer.
	 * @param data The buffer data.
	 * @return An OpenGL Element Buffer object
	 */
	default int createIndexBuffer(int usage, ByteBuffer data){
		return 0;
	}
}
