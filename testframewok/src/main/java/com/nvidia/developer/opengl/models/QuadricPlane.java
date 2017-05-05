package com.nvidia.developer.opengl.models;

import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

public class QuadricPlane implements QuadricGenerator{

	private float xsize, zsize;
	public QuadricPlane(){
		this(1,1);
	}
	
	public QuadricPlane(float xsize, float zsize){
		this.xsize = xsize;
		this.zsize = zsize;
	}
	
	@Override
	public void genVertex(float x, float y, Vector3f position, Vector3f normal, Vector2f texCoord, Vector4f color) {
		position.set(x * xsize, 0, y * zsize);
		if(normal != null)
			normal.set(0, 1, 0);
		if(texCoord != null)
			texCoord.set(x, y);
	}

}
