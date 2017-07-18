package org.lwjgl.util.vector;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public class Vector3i implements Readable, Writable{

	public int x,y,z;

	public Vector3i() {
	}

	public Vector3i(int x, int y, int z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public Vector3i(Vector3i v) {
		set(v);
	}
	
	public void set(int x, int y, int z){
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	public void set(Vector3i v){
		x = v.x;
		y = v.y;
		z = v.z;
	}
	
	public static String toString(Vector3i v){
		StringBuilder sb = new StringBuilder(16);

		sb.append("[");
		sb.append(v.x);
		sb.append(", ");
		sb.append(v.y);
		sb.append(", ");
		sb.append(v.z);
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
		buf.putInt(z);
		return buf;
	}

	@Override
	public Vector3i load(ByteBuffer buf) {
		x = buf.getInt();
		y = buf.getInt();
		z = buf.getInt();
		return this;
	}

	public void load(IntBuffer buf) {
		x = buf.get();
		y = buf.get();
		z = buf.get();
	}
}
