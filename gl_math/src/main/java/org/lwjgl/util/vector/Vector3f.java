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
 * Holds a 3-tuple vector.
 *
 * @author cix_foo <cix_foo@users.sourceforge.net>
 * @version $Revision$
 * $Id$
 */

public class Vector3f extends Vector implements Serializable, ReadableVector3f, WritableVector3f, Cloneable {

	private static final long serialVersionUID = 3977120849790213774L;
	
	/** (x, y, z) = (1, 0, 0) */
	public static final ReadableVector3f X_AXIS = new Vector3f(1, 0, 0);
	/** (x, y, z) = (0, 1, 0) */
	public static final ReadableVector3f Y_AXIS = new Vector3f(0, 1, 0);
	/** (x, y, z) = (0, 0, 1) */
	public static final ReadableVector3f Z_AXIS = new Vector3f(0, 0, 1);
	
	/** (x, y, z) = (-1, 0, 0) */
	public static final ReadableVector3f X_AXIS_NEG = new Vector3f(-1, 0, 0);
	/** (x, y, z) = (0, -1, 0) */
	public static final ReadableVector3f Y_AXIS_NEG = new Vector3f(0, -1, 0);
	/** (x, y, z) = (0, 0, -1) */
	public static final ReadableVector3f Z_AXIS_NEG = new Vector3f(0, 0, -1);
	
	/** (x, y, z) = (0, 0, 0) */
	public static final ReadableVector3f ZERO = new Vector3f(0, 0, 0);
	
	/** (x, y, z) = (1, 1, 1) */
	public static final ReadableVector3f ONE = new Vector3f(1, 1, 1);
	
	public float x, y, z;
	
	/** The size in bytes of the Vector3f */
	public static final int SIZE = 12;

	/**
	 * Constructor for Vector3f.
	 */
	public Vector3f() {
		super();
	}

	/**
	 * Constructor
	 */
	public Vector3f(ReadableVector3f src) {
		set(src);
	}

	/**
	 * Constructor
	 */
	public Vector3f(float x, float y, float z) {
		set(x, y, z);
	}

	public Vector3f(float v){
		x = y = z = v;
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

	/**
	 * Load from another Vector3f
	 * @param src The source vector
	 * @return this
	 */
	public Vector3f set(ReadableVector3f src) {
		x = src.getX();
		y = src.getY();
		z = src.getZ();
		return this;
	}

	/**
	 * @return the length squared of the vector
	 */
	public float lengthSquared() {
		return x * x + y * y + z * z;
	}

	/**
	 * Translate a vector
	 * @param x The translation in x
	 * @param y the translation in y
	 * @return this
	 */
	public Vector3f translate(float x, float y, float z) {
		this.x += x;
		this.y += y;
		this.z += z;
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
	@SuppressWarnings("unchecked")
	public static<T extends WritableVector3f> T add(ReadableVector3f left, ReadableVector3f right, T dest) {
		if (dest == null)
			return (T) new Vector3f(left.getX() + right.getX(), left.getY() + right.getY(), left.getZ() + right.getZ());
		else {
			dest.set(left.getX() + right.getX(), left.getY() + right.getY(), left.getZ() + right.getZ());
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
	public static Vector3f add(ReadableVector3f left, ReadableVector3f right, Vector3f dest) {
		if (dest == null)
			return new Vector3f(left.getX() + right.getX(), left.getY() + right.getY(), left.getZ() + right.getZ());
		else {
			dest.set(left.getX() + right.getX(), left.getY() + right.getY(), left.getZ() + right.getZ());
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
	@SuppressWarnings("unchecked")
	public static<T extends WritableVector3f> T sub(ReadableVector3f left, ReadableVector3f right, T dest) {
		if (dest == null)
			return (T)new Vector3f(left.getX() - right.getX(), left.getY() - right.getY(), left.getZ() - right.getZ());
		else {
			dest.set(left.getX() - right.getX(), left.getY() - right.getY(), left.getZ() - right.getZ());
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
	public static Vector3f add(ReadableVector3f left, float right, Vector3f dest) {
		if (dest == null)
			return new Vector3f(left.getX() + right, left.getY() + right, left.getZ() + right);
		else {
			dest.set(left.getX() + right, left.getY() + right, left.getZ() + right);
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
	public static Vector3f sub(ReadableVector3f left, float right, Vector3f dest) {
		if (dest == null)
			return new Vector3f(left.getX() - right, left.getY() - right, left.getZ() - right);
		else {
			dest.set(left.getX() - right, left.getY() - right, left.getZ() - right);
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
	public static Vector3f sub(float left, ReadableVector3f right, Vector3f dest) {
		if (dest == null)
			return new Vector3f(left - right.getX(), left - right.getY(), left - right.getZ());
		else {
			dest.set(left - right.getX(), left - right.getY(), left - right.getZ());
			return dest;
		}
	}
	
	public static Vector3f scale(ReadableVector3f left, ReadableVector3f right, Vector3f dest) {
		if (dest == null)
			return new Vector3f(left.getX() * right.getX(), left.getY() * right.getY(), left.getZ() * right.getZ());
		else {
			dest.set(left.getX() * right.getX(), left.getY() * right.getY(), left.getZ() * right.getZ());
			return dest;
		}
	}

	public static<T extends WritableVector3f> T scale(ReadableVector3f left, ReadableVector3f right, T dest) {
		if (dest == null)
			return (T) new Vector3f(left.getX() * right.getX(), left.getY() * right.getY(), left.getZ() * right.getZ());
		else {
			dest.set(left.getX() * right.getX(), left.getY() * right.getY(), left.getZ() * right.getZ());
			return dest;
		}
	}
	
	public static Vector3f scale(ReadableVector3f left, float right, Vector3f dest) {
		if (dest == null)
			return new Vector3f(left.getX() * right, left.getY() * right, left.getZ() * right);
		else {
			dest.set(left.getX() * right, left.getY() * right, left.getZ() * right);
			return dest;
		}
	}

	public static<T extends WritableVector3f> T scale(ReadableVector3f left, float right, T dest) {
		if (dest == null)
			return (T) new Vector3f(left.getX() * right, left.getY() * right, left.getZ() * right);
		else {
			dest.set(left.getX() * right, left.getY() * right, left.getZ() * right);
			return dest;
		}
	}
	
	public static Vector3f div(float value, ReadableVector3f v, Vector3f dest){
		if (dest == null)
			return new Vector3f(value/v.getX(), value/v.getY(), value/v.getZ());
		else {
			dest.set(value/v.getX(), value/v.getY(), value/v.getZ());
			return dest;
		}
	}
	
	@Override
	public boolean isNaN() {return x != x || y != y || z != z;}

	/**
	 * The cross product of two vectors.
	 *
	 * @param left The LHS vector
	 * @param right The RHS vector
	 * @param dest The destination result, or null if a new vector is to be created
	 * @return left cross right
	 */
	public static Vector3f cross(
			ReadableVector3f left,
			ReadableVector3f right,
			Vector3f dest)
	{

		if (dest == null)
			dest = new Vector3f();

		dest.set(
				left.getY() * right.getZ() - left.getZ() * right.getY(),
				right.getX() * left.getZ() - right.getZ() * left.getX(),
				left.getX() * right.getY() - left.getY() * right.getX()
				);

		return dest;
	}



	/**
	 * Negate a vector
	 * @return this
	 */
	public Vector3f negate() {
		x = -x;
		y = -y;
		z = -z;
		return this;
	}

	/**
	 * Negate a vector and place the result in a destination vector.
	 * @param dest The destination vector or null if a new vector is to be created
	 * @return the negated vector
	 */
	public Vector3f negate(Vector3f dest) {
		if (dest == null)
			dest = new Vector3f();
		dest.x = -x;
		dest.y = -y;
		dest.z = -z;
		return dest;
	}


	/**
	 * Normalise this vector and place the result in another vector.
	 * @param dest The destination vector, or null if a new vector is to be created
	 * @return the normalised vector
	 */
	public Vector3f normalise(Vector3f dest) {
		if(dest == null)
			dest = new Vector3f(x, y, z);
		else
			dest.set(x, y, z);

		dest.normalise();
		return dest;
	}

	/**
	 * The dot product of two vectors is calculated as
	 * v1.x * v2.x + v1.y * v2.y + v1.z * v2.z
	 * @param left The LHS vector
	 * @param right The RHS vector
	 * @return left dot right
	 */
	public static float dot(ReadableVector3f left, ReadableVector3f right) {
		return left.getX() * right.getX() + left.getY() * right.getY() + left.getZ() * right.getZ();
	}

	/**
	 * Calculate the angle between two vectors, in radians
	 * @param a A vector
	 * @param b The other vector
	 * @return the angle between the two vectors, in radians
	 */
	public static float angle(ReadableVector3f a, ReadableVector3f b) {
		float dls = dot(a, b) / (length(a) * length(b));
		if (dls < -1f)
			dls = -1f;
		else if (dls > 1.0f)
			dls = 1.0f;
		return (float)Math.acos(dls);
	}
	
	public static float lengthSquared(ReadableVector3f v){
		float x = v.getX();
		float y = v.getY();
		float z = v.getZ();
		
		return x * x + y * y + z * z;
	}
	
	public static float length(ReadableVector3f v){
		float x = v.getX();
		float y = v.getY();
		float z = v.getZ();
		
		float lenSqure =  x * x + y * y + z * z;
		
		if(lenSqure == 1.0f || lenSqure == 0)
			return lenSqure;
		
		return (float)Math.sqrt(lenSqure);
	}

	/* (non-Javadoc)
	 * @see org.lwjgl.vector.Vector#load(FloatBuffer)
	 */
	public Vector3f load(FloatBuffer buf) {
		x = buf.get();
		y = buf.get();
		z = buf.get();
		return this;
	}

	/* (non-Javadoc)
	 * @see org.lwjgl.vector.Vector#scale(float)
	 */
	public Vector3f scale(float scale) {

		x *= scale;
		y *= scale;
		z *= scale;

		return this;

	}

	/* (non-Javadoc)
	 * @see org.lwjgl.vector.Vector#store(FloatBuffer)
	 */
	public FloatBuffer store(FloatBuffer buf) {

		buf.put(x);
		buf.put(y);
		buf.put(z);

		return buf;
	}
	
	public static String toString(ReadableVector3f v){
		StringBuilder sb = new StringBuilder(16);

		sb.append("[");
		sb.append(v.getX());
		sb.append(", ");
		sb.append(v.getY());
		sb.append(", ");
		sb.append(v.getZ());
		sb.append(']');
		return sb.toString();
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
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
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Float.floatToIntBits(x);
		result = prime * result + Float.floatToIntBits(y);
		result = prime * result + Float.floatToIntBits(z);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		ReadableVector3f other = (ReadableVector3f) obj;
		
		if (Float.floatToIntBits(x) != Float.floatToIntBits(other.getX()))
			return false;
		if (Float.floatToIntBits(y) != Float.floatToIntBits(other.getY()))
			return false;
		if (Float.floatToIntBits(z) != Float.floatToIntBits(other.getZ()))
			return false;
		return true;
	}

	public Vector3f load(float[] arr, int offset){
		x = arr[offset++];
		y = arr[offset++];
		z = arr[offset++];
		return this;	
	}

	public float[] store(float[] arr, int offset){
		arr[offset++] = x;
		arr[offset++] = y;
		arr[offset++] = z;
		return arr;
	}
	
	/**
	 * Compute the projection of a vector p onto a vector q, and the result stored in dest.
	 * @return return the projection of a vector p onto a vector q
	 */
	public static Vector3f projection(ReadableVector3f p, ReadableVector3f q, Vector3f dest){
		if(dest == null) dest = new Vector3f();
		
		float scaler = dot(p, q)/lengthSquared(q);
		dest.set(q).scale(scaler);
		
		return dest;
	}
	
	/**
	 * Compute the projection of a vector p onto a vector q, and the result stored in dest.
	 * @return return the projection of a vector p onto a vector q
	 */
	public static WritableVector3f projection(ReadableVector3f p, ReadableVector3f q, WritableVector3f dest){
		if(dest == null) dest = new Vector3f();
		
		float scaler = dot(p, q)/lengthSquared(q);
		dest.setX(q.getX() * scaler);
		dest.setY(q.getY() * scaler);
		dest.setZ(q.getZ() * scaler);
		
		return dest;
	}
	
	/**
	 * Compute the reflection.
	 * @param l the direction of the incomming light. Must be a normal vector.
	 * @param n the normal vector of the point.
	 * @param r the reflection
	 * @return
	 */
	public static Vector3f reflection(Vector3f l, Vector3f n, Vector3f r){
		float f = 2 * dot(n, l);
		return Vector3f.linear(l, n, -f, r);
		/*r.x = f * n.x - l.x;
		r.y = f * n.y - l.y;
		r.z = f * n.z - l.z;
		
		return r;*/
	}

	/**
	 * Compute the refraction.
	 * @param l
	 * @param n
	 * @param fle
	 * @param r
	 * @return
	 */
	public static Vector3f refraction(Vector3f l, Vector3f n, float fle, Vector3f r){
		if(r == null) r = new Vector3f();
		
		float dot = dot(n,l);
		float f = (float) (fle * dot - Math.sqrt(1 - fle * fle * (1 - dot * dot)));
		
		r.x = f * n.x - fle * l.x;
		r.y = f * n.y - fle * l.y;
		r.z = f * n.z - fle * l.z;
		
		return r;
	}
	
	@Override
	public float get(int index) {
		switch (index) {
		case 0: return x;
		case 1: return y;	
		case 2: return z;	
		default:
			throw new IndexOutOfBoundsException("index = " + index);
		}
	}
	
	@Override
	public int getCount() { return 3; }
	
	@Override
	public void setValue(int index, float v) {
		switch (index) {
		case 0: x = v; break;
		case 1: y = v; break;
		case 2: z = v; break;
		default:
			throw new IndexOutOfBoundsException("index = " + index);
		}
	}
	
	@Override
	public boolean isZero() {
		return x == 0 && y == 0 && z == 0;
	}
	
	/** dest = x * (1.0 - a) + y * a. */
	public static Vector3f mix(ReadableVector3f x, ReadableVector3f y, float a, Vector3f dest){
		if(dest == null) dest = new Vector3f();
		
		dest.x = x.getX() * (1.0f - a) + y.getX() * a;
		dest.y = x.getY() * (1.0f - a) + y.getY() * a;
		dest.z = x.getZ() * (1.0f - a) + y.getZ() * a;
		
		return dest;
	}
	
	public static float distance(ReadableVector3f v1, ReadableVector3f v2){
		return (float) Math.sqrt(distanceSquare(v1, v2));
	}
	
	public static float distanceSquare(ReadableVector3f v1, ReadableVector3f v2){
		float x = v1.getX() - v2.getX();
		float y = v1.getY() - v2.getY();
		float z = v1.getZ() - v2.getZ();
		
		return (x * x + y * y + z * z);
	}
	
	/**
	 * r = a + b * f
	 * @param a
	 * @param b
	 * @param f
	 * @param r
	 * @return r if r is null, a new Vector3f will create.
	 */
	public static Vector3f linear(ReadableVector3f a, ReadableVector3f b, float f, Vector3f r){
		if(r == null)
			r = new Vector3f();
		
		r.x  = a.getX() + b.getX() * f;
		r.y  = a.getY() + b.getY() * f;
		r.z  = a.getZ() + b.getZ() * f;
		
		return r;
	}
	
	/**
	 * r = a * f + b * g
	 * @param a
	 * @param b
	 * @param f
	 * @param r
	 * @return r if r is null, a new Vector3f will create.
	 */
	public static Vector3f linear(ReadableVector3f a, float f, ReadableVector3f b, float g, Vector3f r){
		if(r == null)
			r = new Vector3f();
		
		r.x  = b.getX() * g + a.getX() * f;
		r.y  = b.getY() * g + a.getY() * f;
		r.z  = b.getZ() * g + a.getZ() * f;
		
		return r;
	}
	
	public static float lengthSquare(float x, float y, float z){
		return x * x + y * y + z * z;
	}
	
	public static float length(float x, float y, float z){
		return (float) Math.sqrt(x * x + y * y + z * z);
	}
	
	@SuppressWarnings("unchecked")
	public static<T extends WritableVector3f> T computeNormal(ReadableVector3f v0, ReadableVector3f v1, ReadableVector3f v2, T normal){
		float leftX = v1.getX() - v0.getX();
		float leftY = v1.getY() - v0.getY();
		float leftZ = v1.getZ() - v0.getZ();
		
		float rightX = v2.getX() - v0.getX();
		float rightY = v2.getY() - v0.getY();
		float rightZ = v2.getZ() - v0.getZ();
		
		if(normal == null)
			normal = (T) new Vector3f();
		
		normal.set(
				leftY * rightZ - leftZ * rightY,
				rightX * leftZ - rightZ * leftX,
				leftX * rightY - leftY * rightX
				);
		
		return normal;
	}

	public static<T extends WritableVector3f> T min(ReadableVector3f a, ReadableVector3f b, T result){
		if(result == null)
			result = (T) new Vector3f();

		result.set( Math.min(a.getX(), b.getX()),
					Math.min(a.getY(), b.getY()),
					Math.min(a.getZ(), b.getZ())
				);

		return result;
	}

	public static<T extends WritableVector3f> T max(ReadableVector3f a, ReadableVector3f b, T result){
		if(result == null)
			result = (T) new Vector3f();

		result.set( Math.max(a.getX(), b.getX()),
				Math.max(a.getY(), b.getY()),
				Math.max(a.getZ(), b.getZ())
		);

		return result;
	}
	
	@Override
	public ByteBuffer store(ByteBuffer buf) {
		buf.putFloat(x).putFloat(y).putFloat(z);
		return buf;
	}

	@Override
	public Vector3f load(ByteBuffer buf) {
		x = buf.getFloat();
		y = buf.getFloat();
		z = buf.getFloat();
		return this;
	}

	/**
	 * Set the <code>min</code> to the minimum float values. Set the <code>max</code> to the maximum float values.
	 * @param min The variable which will be set to the minimum float values. Can be null.
	 * @param max The variable which will be set to the maximum float values. Can be null.
	 */
	public static void initAsMinMax(WritableVector3f min, WritableVector3f max){
		if(min!=null) min.set(-Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE);
		if(max!=null) max.set(Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE);
	}

	public static Vector3f saturation(Vector3f inout){
		return saturation(inout, inout);
	}

	public static Vector3f saturation(ReadableVector3f src, Vector3f out){
		if(out == null)
			out = new Vector3f();

		float x = src.getX();
		float y = src.getY();
		float z = src.getZ();

		out.x = (x > 1.f) ? 1.f : ((x < 0.f) ? 0.f : x);
		out.y = (y > 1.f) ? 1.f : ((y < 0.f) ? 0.f : y);
		out.z = (z > 1.f) ? 1.f : ((z < 0.f) ? 0.f : z);
		return out;
	}

	@Override
	public Vector3f clone()  {
		// This is faster than the defualt clone method.
		return new Vector3f(x,y,z);
	}
}
