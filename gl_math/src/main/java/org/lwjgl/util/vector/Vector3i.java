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

	/**
	 * Subtract a vector from another vector and place the result in a destination
	 * vector.
	 * @param left The LHS vector
	 * @param right The RHS vector
	 * @param dest The destination vector, or null if a new vector is to be created
	 * @return left minus right in dest
	 */
	public static Vector3i sub(int left, Vector3i right, Vector3i dest) {
		if (dest == null)
			return new Vector3i(left - right.x, left - right.y, left - right.z);
		else {
			dest.set(left - right.x, left - right.y, left - right.z);
			return dest;
		}
	}

	/**
	 * Add a vector to another vector and place the result in a destination
	 * vector.
	 * @param left The LHS vector
	 * @param right The RHS vector
	 * @param dest The destination vector, or null if a new vector is to be created
	 * @return the sum of left and right in dest
	 */
	public static Vector3i add(Vector3i left, Vector3i right, Vector3i dest) {
		if (dest == null)
			return new Vector3i(left.x + right.x, left.y + right.y, left.z + right.z);
		else {
			dest.set(left.x + right.x, left.y + right.y, left.z + right.z);
			return dest;
		}
	}

	/**
	 * Subtract a vector from another vector and place the result in a destination
	 * vector.
	 * @param left The LHS vector
	 * @param right The RHS vector
	 * @param dest The destination vector, or null if a new vector is to be created
	 * @return the sum of left and right in dest
	 */
	public static Vector3i sub(Vector3i left, Vector3i right, Vector3i dest) {
		if (dest == null)
			return new Vector3i(left.x - right.x, left.y - right.y, left.z - right.z);
		else {
			dest.set(left.x - right.x, left.y - right.y, left.z - right.z);
			return dest;
		}
	}

	public void setValue(int index, int v) {
		switch (index) {
			case 0: x = v; break;
			case 1: y = v; break;
			case 2: z = v; break;
			default:
				throw new IndexOutOfBoundsException("index = " + index);
		}
	}

	public int get(int index) {
		switch (index) {
			case 0: return x;
			case 1: return y;
			case 2: return z;
			default:
				throw new IndexOutOfBoundsException("index = " + index);
		}
	}
}
