/* 
 * Copyright (c) 2002-2008 LWJGL Project
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are 
 * met:
 * 
 * * Redistributions of source code must retain the above copyright 
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'LWJGL' nor the names of 
 *   its contributors may be used to endorse or promote products derived 
 *   from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR 
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, 
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, 
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR 
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING 
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.lwjgl.util.vector;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

/**
 *
 * Holds a 4-tuple vector.
 * 
 * @author cix_foo <cix_foo@users.sourceforge.net>
 * @version $Revision$
 * $Id$
 */

public class Vector4f extends Vector implements Serializable, ReadableVector4f, WritableVector4f {

	private static final long serialVersionUID = 1L;

	public float x, y, z, w;
	
	/** The size in bytes of the Vector4f */
	public static final int SIZE = 16;

	/**
	 * Constructor for Vector4f.
	 */
	public Vector4f() {
		super();
	}
	
	public Vector4f(ReadableVector3f src, float w){
		x = src.getX();
		y = src.getY();
		z = src.getZ();
		this.w = w;
	}

	/**
	 * Constructor
	 */
	public Vector4f(ReadableVector4f src) {
		set(src);
	}

	/**
	 * Constructor
	 */
	public Vector4f(float x, float y, float z, float w) {
		set(x, y, z, w);
	}

	/* (non-Javadoc)
	 * @see org.lwjgl.util.vector.WritableVector2f#set(float, float)
	 */
	public void set(float x, float y) {
		this.x = x;
		this.y = y;
	}

	/* (non-Javadoc)
	 * @see org.lwjgl.util.vector.WritableVector3f#set(float, float, float)
	 */
	public void set(float x, float y, float z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	/* (non-Javadoc)
	 * @see org.lwjgl.util.vector.WritableVector4f#set(float, float, float, float)
	 */
	public void set(float x, float y, float z, float w) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.w = w;
	}

	/**
	 * Load from another Vector4f
	 * @param src The source vector
	 * @return this
	 */
	public Vector4f set(ReadableVector4f src) {
		x = src.getX();
		y = src.getY();
		z = src.getZ();
		w = src.getW();
		return this;
	}
	
	/**
	 * Load from another Vector3f
	 * @param src The source vector
	 * @return this
	 */
	public Vector4f set(ReadableVector3f src) {
		x = src.getX();
		y = src.getY();
		z = src.getZ();
		return this;
	}

	/**
	 * @return the length squared of the vector
	 */
	public float lengthSquared() {
		return x * x + y * y + z * z + w * w;
	}

	/**
	 * Translate a vector
	 * @param x The translation in x
	 * @param y the translation in y
	 * @return this
	 */
	public Vector4f translate(float x, float y, float z, float w) {
		this.x += x;
		this.y += y;
		this.z += z;
		this.w += w;
		return this;
	}

	/**
	 * Add a vector to another vector and place the result in a destination
	 * vector.
	 * @param left The LHS vector
	 * @param right The RHS vector
	 * @param dest The destination vector, or null if a new vector is to be created
	 * @return the sum of left and right in dest
	 */
	public static Vector4f add(Vector4f left, Vector4f right, Vector4f dest) {
		if (dest == null)
			return new Vector4f(left.x + right.x, left.y + right.y, left.z + right.z, left.w + right.w);
		else {
			dest.set(left.x + right.x, left.y + right.y, left.z + right.z, left.w + right.w);
			return dest;
		}
	}

	/**
	 * Subtract a vector from another vector and place the result in a destination
	 * vector.
	 * @param left The LHS vector
	 * @param right The RHS vector
	 * @param dest The destination vector, or null if a new vector is to be created
	 * @return left minus right in dest
	 */
	public static Vector4f sub(Vector4f left, Vector4f right, Vector4f dest) {
		if (dest == null)
			return new Vector4f(left.x - right.x, left.y - right.y, left.z - right.z, left.w - right.w);
		else {
			dest.set(left.x - right.x, left.y - right.y, left.z - right.z, left.w - right.w);
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
	public static Vector4f add(ReadableVector4f left, float right, Vector4f dest) {
		if (dest == null)
			return new Vector4f(left.getX() + right, left.getY() + right, left.getZ() + right, left.getW() + right);
		else {
			dest.set(left.getX() + right, left.getY() + right, left.getZ() + right, left.getW() + right);
			return dest;
		}
	}
	
	/**
	 * Subtract a vector from another vector and place the result in a destination
	 * vector.
	 * @param left The LHS vector
	 * @param right The RHS vector
	 * @param dest The destination vector, or null if a new vector is to be created
	 * @return left minus right in dest
	 */
	public static Vector4f sub(ReadableVector4f left, float right, Vector4f dest) {
		if (dest == null)
			return new Vector4f(left.getX() - right, left.getY() - right, left.getZ() - right, left.getW() - right);
		else {
			dest.set(left.getX() - right, left.getY() - right, left.getZ() - right, left.getW() - right);
			return dest;
		}
	}
	
	/**
	 * Subtract a vector from another vector and place the result in a destination
	 * vector.
	 * @param left The LHS vector
	 * @param right The RHS vector
	 * @param dest The destination vector, or null if a new vector is to be created
	 * @return left minus right in dest
	 */
	public static Vector4f sub(float left, ReadableVector4f right, Vector4f dest) {
		if (dest == null)
			return new Vector4f(left - right.getX(), left - right.getY(), left - right.getZ(), left - right.getW());
		else {
			dest.set(left - right.getX(), left - right.getY(), left - right.getZ(), left - right.getW());
			return dest;
		}
	}
	
	public static Vector4f scale(ReadableVector4f left, ReadableVector4f right, Vector4f dest) {
		if (dest == null)
			return new Vector4f(left.getX() * right.getX(), left.getY() * right.getY(), left.getZ() * right.getZ(), left.getW() * right.getW());
		else {
			dest.set(left.getX() * right.getX(), left.getY() * right.getY(), left.getZ() * right.getZ(), left.getW() * right.getW());
			return dest;
		}
	}
	
	public static Vector4f scale(ReadableVector4f left, float right, Vector4f dest) {
		if (dest == null)
			return new Vector4f(left.getX() * right, left.getY() * right, left.getZ() * right, left.getW() * right);
		else {
			dest.set(left.getX() * right, left.getY() * right, left.getZ() * right, left.getW() * right);
			return dest;
		}
	}
	
	public static Vector4f div(float value, ReadableVector4f v, Vector4f dest){
		if (dest == null)
			return new Vector4f(value/v.getX(), value/v.getY(), value/v.getZ(), value/v.getW());
		else {
			dest.set(value/v.getX(), value/v.getY(), value/v.getZ(), value/v.getW());
			return dest;
		}
	}
	
	@Override
	public boolean isNaN() {return x != x || y != y || z != z || w != w;}


	/**
	 * Negate a vector
	 * @return this
	 */
	public Vector4f negate() {
		x = -x;
		y = -y;
		z = -z;
		w = -w;
		return this;
	}

	/**
	 * Negate a vector and place the result in a destination vector.
	 * @param dest The destination vector or null if a new vector is to be created
	 * @return the negated vector
	 */
	public Vector4f negate(Vector4f dest) {
		if (dest == null)
			dest = new Vector4f();
		dest.x = -x;
		dest.y = -y;
		dest.z = -z;
		dest.w = -w;
		return dest;
	}


	/**
	 * Normalise this vector and place the result in another vector.
	 * @param dest The destination vector, or null if a new vector is to be created
	 * @return the normalised vector
	 */
	public Vector4f normalise(Vector4f dest) {
		float l = length();

		if (dest == null)
			dest = new Vector4f(x / l, y / l, z / l, w / l);
		else
			dest.set(x / l, y / l, z / l, w / l);

		return dest;
	}

	/**
	 * The dot product of two vectors is calculated as
	 * v1.x * v2.x + v1.y * v2.y + v1.z * v2.z + v1.w * v2.w
	 * @param left The LHS vector
	 * @param right The RHS vector
	 * @return left dot right
	 */
	public static float dot(Vector4f left, Vector4f right) {
		return left.x * right.x + left.y * right.y + left.z * right.z + left.w * right.w;
	}

	/**
	 * Calculate the angle between two vectors, in radians
	 * @param a A vector
	 * @param b The other vector
	 * @return the angle between the two vectors, in radians
	 */
	public static float angle(Vector4f a, Vector4f b) {
		float dls = dot(a, b) / (a.length() * b.length());
		if (dls < -1f)
			dls = -1f;
		else if (dls > 1.0f)
			dls = 1.0f;
		return (float)Math.acos(dls);
	}

	/* (non-Javadoc)
	 * @see org.lwjgl.vector.Vector#load(FloatBuffer)
	 */
	public Vector4f load(FloatBuffer buf) {
		x = buf.get();
		y = buf.get();
		z = buf.get();
		w = buf.get();
		return this;
	}

	/* (non-Javadoc)
	 * @see org.lwjgl.vector.Vector#scale(float)
	 */
	public Vector4f scale(float scale) {
		x *= scale;
		y *= scale;
		z *= scale;
		w *= scale;
		return this;
	}

	/* (non-Javadoc)
	 * @see org.lwjgl.vector.Vector#store(FloatBuffer)
	 */
	public FloatBuffer store(FloatBuffer buf) {

		buf.put(x);
		buf.put(y);
		buf.put(z);
		buf.put(w);

		return buf;
	}
	
	public static String toString(ReadableVector4f v){
		StringBuilder sb = new StringBuilder(16);

		sb.append("[");
		sb.append(v.getX());
		sb.append(", ");
		sb.append(v.getY());
		sb.append(", ");
		sb.append(v.getZ());
		sb.append(", ");
		sb.append(v.getW());
		sb.append(']');
		return sb.toString();
	}

	public String toString() {
		return toString(this);
	}

	/**
	 * @return x
	 */
	public final float getX() {
		return x;
	}

	/**
	 * @return y
	 */
	public final float getY() {
		return y;
	}

	/**
	 * Set X
	 * @param x
	 */
	public final void setX(float x) {
		this.x = x;
	}

	/**
	 * Set Y
	 * @param y
	 */
	public final void setY(float y) {
		this.y = y;
	}

	/**
	 * Set Z
	 * @param z
	 */
	public void setZ(float z) {
		this.z = z;
	}


	/* (Overrides)
	 * @see org.lwjgl.vector.ReadableVector3f#getZ()
	 */
	public float getZ() {
		return z;
	}

	/**
	 * Set W
	 * @param w
	 */
	public void setW(float w) {
		this.w = w;
	}

	/* (Overrides)
	 * @see org.lwjgl.vector.ReadableVector3f#getZ()
	 */
	public float getW() {
		return w;
	}
	
	public Vector4f load(float[] arr, int offset){
		x = arr[offset++];
		y = arr[offset++];
		z = arr[offset++];
		w = arr[offset++];
		return this;	
	}

	public float[] store(float[] arr, int offset){
		arr[offset++] = x;
		arr[offset++] = y;
		arr[offset++] = z;
		arr[offset++] = w;
		return arr;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Float.floatToIntBits(w);
		result = prime * result + Float.floatToIntBits(x);
		result = prime * result + Float.floatToIntBits(y);
		result = prime * result + Float.floatToIntBits(z);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		Vector4f other = (Vector4f) obj;
		if (Float.floatToIntBits(w) != Float.floatToIntBits(other.w))
			return false;
		if (Float.floatToIntBits(x) != Float.floatToIntBits(other.x))
			return false;
		if (Float.floatToIntBits(y) != Float.floatToIntBits(other.y))
			return false;
		if (Float.floatToIntBits(z) != Float.floatToIntBits(other.z))
			return false;
		return true;
	}

	@Override
	public float get(int index) {
		switch (index) {
		case 0: return x;
		case 1: return y;	
		case 2: return z;	
		case 3: return w;	
		default:
			throw new IndexOutOfBoundsException("index = " + index);
		}
	}
	
	@Override
	public int getCount() { return 4; }
	
	@Override
	public void setValue(int index, float v) {
		switch (index) {
		case 0: x = v; break;
		case 1: y = v; break;
		case 2: z = v; break;
		case 3: w = v; break;
		default:
			throw new IndexOutOfBoundsException("index = " + index);
		}
	}
	
	@Override
	public boolean isZero() {
		return x == 0 && y == 0 && z == 0 && w == 0;
	}
	
	/** dest = x * (1.0 - a) + y * a. */
	@SuppressWarnings("unchecked")
	public static<T extends WritableVector4f> T mix(ReadableVector4f x, ReadableVector4f y, float a, WritableVector4f dest){
		if(dest == null) dest = new Vector4f();
		
		dest.setX(x.getX() * (1.0f - a) + y.getX() * a);;
		dest.setY(x.getY() * (1.0f - a) + y.getY() * a);;
		dest.setZ(x.getZ() * (1.0f - a) + y.getZ() * a);;
		dest.setW(x.getW() * (1.0f - a) + y.getW() * a);;
		
		return (T)dest;
	}
	
	public static float distance(ReadableVector4f v1, ReadableVector4f v2){
		return (float) Math.sqrt(distanceSquare(v1, v2));
	}
	
	public static float distanceSquare(ReadableVector4f v1, ReadableVector4f v2){
		float x = v1.getX() - v2.getX();
		float y = v1.getY() - v2.getY();
		float z = v1.getZ() - v2.getZ();
		float w = v1.getW() - v2.getW();
		
		return (x * x + y * y + z * z + w * w);
	}
	
	public static float lengthSquare(float x, float y, float z, float w){
		return x * x + y * y + z * z + w * w;
	}
	
	public static float length(float x, float y, float z, float w){
		return (float) Math.sqrt(x * x + y * y + z * z + w * w);
	}
	
	@Override
	public ByteBuffer store(ByteBuffer buf) {
		buf.putFloat(x).putFloat(y).putFloat(z).putFloat(w);
		return buf;
	}

	@Override
	public Vector4f load(ByteBuffer buf) {
		x = buf.getFloat();
		y = buf.getFloat();
		z = buf.getFloat();
		w = buf.getFloat();
		return this;
	}
}
