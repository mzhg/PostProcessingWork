package org.lwjgl.util.vector;

import java.nio.IntBuffer;

public class Vector4i {

	public int x,y,z,w;
	
	public Vector4i() {
	}

	public Vector4i(int x, int y,int z, int w) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.w = w;
	}
	
	public Vector4i(Vector4i v) {
		set(v);
	}
	
	public void set(int x, int y,int z, int w){
		this.x = x;
		this.y = y;
		this.z = z;
		this.w = w;
	}
	
	public void set(Vector4i v){
		x = v.x;
		y = v.y;
		z = v.z;
		w = v.w;
	}
	
	public static String toString(Vector4i v){
		StringBuilder sb = new StringBuilder(16);

		sb.append("[");
		sb.append(v.x);
		sb.append(", ");
		sb.append(v.y);
		sb.append(", ");
		sb.append(v.z);
		sb.append(", ");
		sb.append(v.w);
		sb.append(']');
		return sb.toString();
	}

	public String toString() {
		return toString(this);
	}

	public void load(IntBuffer buf) {
		x = buf.get();
		y = buf.get();
		z = buf.get();
		w = buf.get();
	}
}
