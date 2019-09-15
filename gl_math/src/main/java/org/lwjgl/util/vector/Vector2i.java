package org.lwjgl.util.vector;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public class Vector2i implements Readable, Writable{

	public int x,y;
	
	public Vector2i() {
	}

	public Vector2i(int x, int y) {
		this.x = x;
		this.y = y;
	}
	
	public Vector2i(Vector2i v) {
		set(v);
	}
	
	public void set(int x, int y){
		this.x = x;
		this.y = y;
	}
	
	public void set(Vector2i v){
		x = v.x;
		y = v.y;
	}

	public int getMin()
	{
		return Math.min(x,y);
	}
	
	public static String toString(Vector2i v){
		StringBuilder sb = new StringBuilder(16);

		sb.append("[");
		sb.append(v.x);
		sb.append(", ");
		sb.append(v.y);
		sb.append(']');
		return sb.toString();
	}

	public String toString() {
		return toString(this);
	}

	@Override
	public ByteBuffer store(ByteBuffer buf){
		buf.putInt(x);
		buf.putInt(y);
		return buf;
	}

	@Override
	public Vector2i load(ByteBuffer buf) {
		x = buf.getInt();
		y = buf.getInt();
		return this;
	}

	public void load(IntBuffer buf) {
		x = buf.get();
		y = buf.get();
	}
}
