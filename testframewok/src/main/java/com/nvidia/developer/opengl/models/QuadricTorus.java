package com.nvidia.developer.opengl.models;

import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

public class QuadricTorus implements QuadricGenerator{

	private float major = 1.0f;
	private float minor = 0.5f;
	
	private Vector3f dx, dy;
	
	public QuadricTorus() {}
	
	public QuadricTorus(float major, float minor) {
		this.major = major;
		this.minor = minor;
	}

	@Override
	public void genVertex(float x, float y, Vector3f position, Vector3f normal, Vector2f texCoord, Vector4f color) {
		// x -- theta, y = fei
		double two_pi    = Math.PI * 2.0;
		double sin_theta = Math.sin(y * two_pi);
		double sin_fei   = Math.sin(x * two_pi);
		double cos_theta = Math.cos(y * two_pi);
		double cos_fei   = Math.cos(x * two_pi);
		
		position.x = (float) ((major + minor * cos_theta) * cos_fei);
		position.y = (float) ((major + minor * cos_theta) * sin_fei);
		position.z = (float) (minor * sin_theta);
		
		if(texCoord != null)
			texCoord.set(x, y);
		
		if(normal != null){
			if(dx == null){
				dx = new Vector3f();
				dy = new Vector3f();
			}
			
			dx.x = (float) ((0 - minor * sin_theta) * cos_fei);
			dx.y = (float) ((0 - minor * sin_theta) * sin_fei);
			dx.z = (float) (minor * cos_theta);
			
			dy.x = (float) (-(major + minor * cos_theta) * sin_fei);
			dy.y = (float) ((major + minor * cos_theta) * cos_fei);
			dy.z = 0;
			Vector3f.cross(dy, dx, normal);
			
//			float centerx = (float) (major * cos_fei);
//			float centery = (float) (major * sin_fei);
//			normal.set(position.x - centerx, position.y - centery, position.z);
			normal.normalise();
		}
		
		// white color
		if(color != null)
			color.set(1, 1, 1, 1);
	}

	public void setRadius(float majorRadius, float minorRadius){
		major = majorRadius;
		minor = minorRadius;
	}
	
	public float getMajorRadius() { return major;}
	public float getMinorRadius() { return minor;}
}
