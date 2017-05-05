package com.nvidia.developer.opengl.models;

import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

public class QuadricSphere implements QuadricGenerator{

	private float radius = 1.0f;
	
	public QuadricSphere() {}
	
	public QuadricSphere(float radius) {
		this.radius = radius;
	}

	@Override
	public void genVertex(float s, float t, Vector3f position, Vector3f normal, Vector2f texCoord, Vector4f color) {
		double theta = (Math.PI * 2.0 * s);
		double fai   = (Math.PI * t);
		
		float x = (float) (Math.sin(fai) * Math.cos(theta));
		float y = (float) (Math.sin(fai) * Math.sin(theta));
		float z = (float) Math.cos(fai);
		position.set(x * radius, y * radius, z * radius);
		if(normal != null)
			normal.set(x,y,z);
		if(texCoord != null)
			texCoord.set(s, t);
		// white color
		if(color != null)
			color.set(1, 1, 1, 1);
		
	}

	public float getRadius() {
		return radius;
	}

	public void setRadius(float radius) {
		this.radius = radius;
	}

}
