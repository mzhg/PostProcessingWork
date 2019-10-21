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
 * Holds a 2-tuple vector.
 *
 * @author cix_foo <cix_foo@users.sourceforge.net>
 * @version $Revision$
 * $Id$
 */

public class Vector2f extends Vector implements Serializable, ReadableVector2f, WritableVector2f, Cloneable {

	private static final long serialVersionUID = 3587359845319496063L;
	
	/** (x, y) = (1, 0) */
	public static final ReadableVector2f X_AXIS = new Vector2f(1, 0);
	/** (x, y) = (0, 1) */
	public static final ReadableVector2f Y_AXIS = new Vector2f(0, 1);
	
	/** (x, y) = (-1, 0) */
	public static final ReadableVector2f X_AXIS_NEG = new Vector2f(-1, 0);
	/** (x, y) = (0, -1) */
	public static final ReadableVector2f Y_AXIS_NEG = new Vector2f(0, -1);
	/** (x, y) = (0, 0) */
	public static final ReadableVector2f ZERO = new Vector2f(0, 0);
	
	/** The size in bytes of the Vector2f */
	public static final int SIZE = 8;
	
	public float x, y;

	/**
	 * Constructor for Vector3f.
	 */
	public Vector2f() {
		super();
	}

	/**
	 * Constructor
	 */
	public Vector2f(ReadableVector2f src) {
		set(src);
	}

	/**
	 * Constructor
	 */
	public Vector2f(float x, float y) {
		set(x, y);
	}

	/* (non-Javadoc)
	 * @see org.lwjgl.util.vector.WritableVector2f#set(float, float)
	 */
	public void set(float x, float y) {
		this.x = x;
		this.y = y;
	}

	/**
	 * Load from another Vector2f
	 * @param src The source vector
	 * @return this
	 */
	public Vector2f set(ReadableVector2f src) {
		x = src.getX();
		y = src.getY();
		return this;
	}

	/**
	 * @return the length squared of the vector
	 */
	public float lengthSquared() {
		return x * x + y * y;
	}

	/**
	 * Translate a vector
	 * @param x The translation in x
	 * @param y the translation in y
	 * @return this
	 */
	public Vector2f translate(float x, float y) {
		this.x += x;
		this.y += y;
		return this;
	}

	/**
	 * Negate a vector
	 * @return this
	 */
	public Vector2f negate() {
		x = -x;
		y = -y;
		return this;
	}

	/**
	 * Negate a vector and place the result in a destination vector.
	 * @param dest The destination vector or null if a new vector is to be created
	 * @return the negated vector
	 */
	public Vector2f negate(Vector2f dest) {
		if (dest == null)
			dest = new Vector2f();
		dest.x = -x;
		dest.y = -y;
		return dest;
	}


	/**
	 * Normalise this vector and place the result in another vector.
	 * @param dest The destination vector, or null if a new vector is to be created
	 * @return the normalised vector
	 */
	public Vector2f normalise(Vector2f dest) {
		if(dest == null)
			dest = new Vector2f(x, y);
		else
			dest.set(x, y);
		
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
	public static float dot(ReadableVector2f left, ReadableVector2f right) {
		return left.getX() * right.getX() + left.getY() * right.getY();
	}

	/**
	 * The dot product of two vectors is calculated as
	 * v1.x * v2.y - v2.x * v1.y
	 * @param left The LHS vector
	 * @param right The RHS vector
	 * @return left dot right
	 */
    public static float cross(ReadableVector2f left, ReadableVector2f right){
    	float x1 = left.getX(), y1 = left.getY();  
        float x2 = right.getX(), y2 = right.getY();  
          
        return x1 * y2 - x2 * y1;
    }

	/**
	 * Calculate the angle between two vectors, in radians
	 * @param a A vector
	 * @param b The other vector
	 * @return the angle between the two vectors, in radians
	 */
	public static float angle(ReadableVector2f a, ReadableVector2f b) {
		float dls = dot(a, b) / (length(a) * length(b));
		if (dls < -1f)
			dls = -1f;
		else if (dls > 1.0f)
			dls = 1.0f;
		return (float)Math.acos(dls);
	}
	
	/**
	 * Calculate the length of the vector v.
	 * @param v
	 * @return
	 */
	public static float length(ReadableVector2f v){
		float x = v.getX(), y = v.getY();
		float s = x * x + y * y;
		if(s == 1.0f || s == 0.0f) return s;
		return (float) Math.sqrt(s);
	}
	
	/**
	 * Calculate the length squared of the vector v.
	 * @param v
	 * @return
	 */
	public static float lengthSquared(ReadableVector2f v){
		float x = v.getX(), y = v.getY();
		return x*x + y*y;
	}

	/**
	 * Add a vector to another vector and place the result in a destination
	 * vector.
	 * @param left The LHS vector
	 * @param right The RHS vector
	 * @param dest The destination vector, or null if a new vector is to be created
	 * @return the sum of left and right in dest
	 */
	public static Vector2f add(ReadableVector2f left, ReadableVector2f right, Vector2f dest) {
		if (dest == null)
			return new Vector2f(left.getX() + right.getX(), left.getY() + right.getY());
		else {
			dest.set(left.getX() + right.getX(), left.getY() + right.getY());
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
	public static Vector2f sub(ReadableVector2f left, ReadableVector2f right, Vector2f dest) {
		if (dest == null)
			return new Vector2f(left.getX() - right.getX(), left.getY() - right.getY());
		else {
			dest.set(left.getX() - right.getX(), left.getY() - right.getY());
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
	public static Vector2f add(ReadableVector2f left, float right, Vector2f dest) {
		if (dest == null)
			return new Vector2f(left.getX() + right, left.getY() + right);
		else {
			dest.set(left.getX() + right, left.getY() + right);
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
	public static Vector2f sub(ReadableVector2f left, float right, Vector2f dest) {
		if (dest == null)
			return new Vector2f(left.getX() - right, left.getY() - right);
		else {
			dest.set(left.getX() - right, left.getY() - right);
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
	public static Vector2f sub(float left, ReadableVector2f right, Vector2f dest) {
		if (dest == null)
			return new Vector2f(left - right.getX(), left - right.getY());
		else {
			dest.set(left - right.getX(), left - right.getY());
			return dest;
		}
	}
	
	public static Vector2f scale(ReadableVector2f left, ReadableVector2f right, Vector2f dest) {
		if (dest == null)
			return new Vector2f(left.getX() * right.getX(), left.getY() * right.getY());
		else {
			dest.set(left.getX() * right.getX(), left.getY() * right.getY());
			return dest;
		}
	}
	
	public static Vector2f scale(ReadableVector2f left, float right, Vector2f dest) {
		if (dest == null)
			return new Vector2f(left.getX() * right, left.getY() * right);
		else {
			dest.set(left.getX() * right, left.getY() * right);
			return dest;
		}
	}
	
	public static Vector2f div(float value, ReadableVector2f v, Vector2f dest){
		if (dest == null)
			return new Vector2f(value/v.getX(), value/v.getY());
		else {
			dest.set(value/v.getX(), value/v.getY());
			return dest;
		}
	}
	
	@Override
	public boolean isNaN() {return x != x || y != y;}

	/**
	 * Store this vector in a FloatBuffer
	 * @param buf The buffer to store it in, at the current position
	 * @return this
	 */
	public FloatBuffer store(FloatBuffer buf) {
		buf.put(x);
		buf.put(y);
		return buf;
	}

	/**
	 * Load this vector from a FloatBuffer
	 * @param buf The buffer to load it from, at the current position
	 * @return this
	 */
	public Vector2f load(FloatBuffer buf) {
		x = buf.get();
		y = buf.get();
		return this;
	}
	
	public Vector2f load(float[] arr, int offset){
		x = arr[offset++];
		y = arr[offset++];
		return this;	
	}

	public float[] store(float[] arr, int offset){
		arr[offset++] = x;
		arr[offset++] = y;
		return arr;
	}

	/* (non-Javadoc)
	 * @see org.lwjgl.vector.Vector#scale(float)
	 */
	public Vector2f scale(float scale) {

		x *= scale;
		y *= scale;

		return this;
	}
	
	public static String toString(ReadableVector2f v){
		StringBuilder sb = new StringBuilder(16);

		sb.append("[");
		sb.append(v.getX());
		sb.append(", ");
		sb.append(v.getY());
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
	
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Float.floatToIntBits(x);
		result = prime * result + Float.floatToIntBits(y);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		Vector2f other = (Vector2f) obj;
		if (Float.floatToIntBits(x) != Float.floatToIntBits(other.x))
			return false;
		if (Float.floatToIntBits(y) != Float.floatToIntBits(other.y))
			return false;
		return true;
	}

	/**
	 * Compute the projection of a vector p onto a vector q, and the result stored in dest.
	 * @return return the projection of a vector p onto a vector q
	 */
	public static Vector2f projection(ReadableVector2f p, ReadableVector2f q, Vector2f dest){
		if(dest == null) dest = new Vector2f();
		
		float scaler = dot(p, q)/lengthSquared(q);
		dest.set(q).scale(scaler);
		
		return dest;
	}
	
	/**
	 * Compute the projection of a vector p onto a vector q, and the result stored in dest.
	 * @return return the projection of a vector p onto a vector q
	 */
	public static WritableVector2f projection(ReadableVector2f p, ReadableVector2f q, WritableVector2f dest){
		if(dest == null) dest = new Vector2f();
		
		float scaler = dot(p, q)/lengthSquared(q);
		dest.setX(q.getX() * scaler);
		dest.setY(q.getY() * scaler);
		
		return dest;
	}
	
	/**
	 * Compute the reflection.
	 * @param l the direction of the incomming light. Must be a normal vector.
	 * @param n the normal vector of the point.
	 * @param r the reflection
	 * @return
	 */
	public static Vector2f reflection(Vector2f l, Vector2f n, Vector2f r){
		if(r == null) r = new Vector2f();
		
		float f = 2 * dot(n, l);
		r.x = l.x - f * n.x;
		r.y = l.y - f * n.y;
		
		return r;
	}

	/**
	 * Compute the refraction.
	 * @param l
	 * @param n
	 * @param fle
	 * @param r
	 * @return
	 */
	public static Vector2f refraction(Vector2f l, Vector2f n, float fle, Vector2f r){
		if(r == null) r = new Vector2f();
		
		float dot = dot(n,l);
		float f = (float) (fle * dot - Math.sqrt(1 - fle * fle * (1 - dot * dot)));
		
		r.x = f * n.x - fle * l.x;
		r.y = f * n.y - fle * l.y;
		
		return r;
	}

	@Override
	public float get(int index) {
		switch (index) {
		case 0: return x;
		case 1: return y;	
		default:
			throw new IndexOutOfBoundsException("index = " + index);
		}
	}
	
	@Override
	public int getCount() { return 2; }
	
	@Override
	public void setValue(int index, float v) {
		switch (index) {
		case 0: x = v; break;
		case 1: y = v; break;
		default:
			throw new IndexOutOfBoundsException("index = " + index);
		}
	}
	
	@Override
	public boolean isZero() {
		return x == 0 && y == 0;
	}
	
	/** dest = x * (1.0 - a) + y * a. */
	public static Vector2f mix(ReadableVector2f x, ReadableVector2f y, float a, Vector2f dest){
		if(dest == null) dest = new Vector2f();
		
		dest.x = x.getX() * (1.0f - a) + y.getX() * a;
		dest.y = x.getY() * (1.0f - a) + y.getY() * a;
		
		return dest;
	}
	
	public static float distance(ReadableVector2f v1, ReadableVector2f v2){
		return (float) Math.sqrt(distanceSquare(v1, v2));
	}
	
	public static float distanceSquare(ReadableVector2f v1, ReadableVector2f v2){
		float x = v1.getX() - v2.getX();
		float y = v1.getY() - v2.getY();
		
		return (x * x + y * y);
	}
	
	/**
	 * r = a + b * f
	 * @param a
	 * @param b
	 * @param f
	 * @param r
	 * @return r if r is null, a new Vector3f will create.
	 */
	public static Vector2f linear(ReadableVector2f a, ReadableVector2f b, float f, Vector2f r){
		if(r == null)
			r = new Vector2f();
		
		r.x  = a.getX() + b.getX() * f;
		r.y  = a.getY() + b.getY() * f;
		
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
	public static Vector2f linear(ReadableVector2f a, float f, ReadableVector2f b, float g, Vector2f r){
		if(r == null)
			r = new Vector2f();
		
		r.x  = b.getX() * g + a.getX() * f;
		r.y  = b.getY() * g + a.getY() * f;
		
		return r;
	}

	@Override
	public ByteBuffer store(ByteBuffer buf) {
		buf.putFloat(x).putFloat(y);
		return buf;
	}

	@Override
	public Vector2f load(ByteBuffer buf) {
		x = buf.getFloat();
		y = buf.getFloat();
		return this;
	}
	
	public static float lengthSquare(float x, float y){
		return x * x + y * y;
	}
	
	public static float length(float x, float y){
		return (float) Math.sqrt(x * x + y * y);
	}

	public static<T extends WritableVector2f> T abs(ReadableVector2f src, T dest){
		if(dest==null)
			dest= (T) new Vector2f();

		dest.set(Math.abs(src.getX()), Math.abs(src.getY()));
		return dest;
	}

	@Override
	public Vector2f clone() {
		return new Vector2f(x,y);
	}

	//	public static float
}
