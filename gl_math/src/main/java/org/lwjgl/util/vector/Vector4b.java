package org.lwjgl.util.vector;

public class Vector4b {

	public byte x,y,z,w;
	
	public Vector4b() {
	}

	public Vector4b(byte x, byte y, byte z, byte w) {
		set(x, y, z,w);
	}
	
	public Vector4b(Vector4b v) {
		set(v);
	}
	
	public void set(byte x, byte y, byte z, byte w){
		this.x = x;
		this.y = y;
		this.z = z;
		this.w = w;
	}
	
	public void set(Vector4b v){
		x = v.x;
		y = v.y;
		z = v.z;
		w = v.w;
	}
}
