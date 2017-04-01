package org.lwjgl.util.vector;

public class Vector2b {

	public byte x,y;
	
	public Vector2b() {
	}

	public Vector2b(byte x, byte y) {
		this.x = x;
		this.y = y;
	}
	
	public Vector2b(Vector2b v) {
		set(v);
	}
	
	public void set(byte x, byte y){
		this.x = x;
		this.y = y;
	}
	
	public void set(Vector2b v){
		x = v.x;
		y = v.y;
	}
}
