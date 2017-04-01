package org.lwjgl.util.vector;

public class Vector3b {

	public byte x,y,z;
	
	public Vector3b() {
	}

	public Vector3b(byte x, byte y, byte z) {
		set(x, y, z);
	}
	
	public Vector3b(Vector3b v) {
		set(v);
	}
	
	public void set(byte x, byte y, byte z){
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	public void set(Vector3b v){
		x = v.x;
		y = v.y;
		z = v.z;
	}
}
