package com.nvidia.developer.opengl.models;

import org.lwjgl.util.vector.Axis;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

public class RevolutionMesh implements QuadricGenerator{
	
	Axis axis = Axis.X;
	Function function;
	
	float scale = 1.0f;
	
	final Vector3f v1 = new Vector3f();
	final Vector3f v2 = new Vector3f();
	
	public RevolutionMesh() {
	}
	
	public RevolutionMesh(Function function, Axis axis) {
		setAxis(axis);
		setFunction(function);
	}
	
	public void setScale(float scale) { this.scale = scale;}
	public float getScale()  { return scale;}
	
	public void setAxis(Axis axis){
		this.axis = axis;
	}
	
	public Axis getAxis() { return axis;}
	
	public void setFunction(Function function){
		this.function = function;
	}
	
	public Function getFunction() { return function;}
	
	@Override
	public void genVertex(float x, float y, Vector3f position, Vector3f normal, Vector2f texCoord, Vector4f color) {
		switch (axis) {
		case X: genRotationX(x, y, position, normal); break;
		case Y: genRotationY(x, y, position, normal); break;
		case Z: genRotationZ(x, y, position, normal);
		default:
			break;
		}
		
		if(texCoord != null)
			texCoord.set(x, y);
		
		if(color != null)
			color.set(1, 1, 1, 1);
	}
	
	void genRotationX(float u, float v, Vector3f position, Vector3f normal){
		u *= scale;
		double theta = Math.PI * 2.0 * v;
		float c = (float) Math.cos(theta);
		float s = (float) Math.sin(theta);
		float x = u;
		float f = function.value(u);
		float y = f * c;
		float z = f * s;
		position.set(x, y, z);
		
		if(normal != null){
			float du = function.deri(u);
			v1.set(1, du * c, du * s);
			v2.set(0, f * (-s), f * c);
			Vector3f.cross(v2, v1, normal);
//			float centerx = u + f * du; // u = px, f = py.
//			normal.set(x - centerx, y, z);
			normal.normalise();
			
//			fixNormalDirection(position, normal, Axis.X);
			
		}
	}
	
	static void fixNormalDirection(Vector3f position, Vector3f normal, Axis aixs){
		float d = - Vector3f.dot(position, normal);
		
		float sum = 0;
		switch (aixs) {
		case X:
			sum = normal.y * position.y + normal.z * position.z;
			break;
		case Y:
			sum = normal.x * position.x + normal.z * position.z;
			break;
		case Z:
			sum = normal.x * position.x + normal.y * position.y;
			break;
		}
		
		if(sum + d> 0.0f){
			normal.negate();
		}
	}
	
	void genRotationY(float u, float v, Vector3f position, Vector3f normal){
		u *= scale;
		double theta = Math.PI * 2.0 * v;
		float c = (float) Math.cos(theta);
		float s = (float) Math.sin(theta);
		float f = function.value(u);
		float x = f * s;
		float y = u;
		float z = f * c;
		position.set(x, y, z);
		
		if(normal != null){
			float du = function.deri(u);
//			v1.set(du * s, 1, du * c);
//			v2.set(f * c, 0, f * (-s));
//			Vector3f.cross(v1, v2, normal);
			float centery = u + f * du; // u = px, f = py.
			normal.set(x, y - centery, z);
			normal.normalise();
			
//			fixNormalDirection(position, normal, Axis.Y);
		}
	}

	void genRotationZ(float u, float v, Vector3f position, Vector3f normal){
		u *= scale;
		double theta = Math.PI * 2.0 * v;
		float c = (float) Math.cos(theta);
		float s = (float) Math.sin(theta);
		float f = function.value(u);
		float x = f * c;
		float y = f * s;
		float z = u;
		position.set(x, y, z);
		
		if(normal != null){
			float du = function.deri(u);
//			v1.set(du * c, du * s, 1);
//			v2.set(f * (-s), f * c, 0);
//			Vector3f.cross(v1, v2, normal);
			float centerz = u + f * du; // u = px, f = py.
			normal.set(x, y, z - centerz);
			normal.normalise();
			
//			fixNormalDirection(position, normal, Axis.Z);
		}
	}

}
