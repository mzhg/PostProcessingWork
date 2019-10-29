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

import java.nio.ByteBuffer;

/**
 *
 * Quaternions for LWJGL!
 *
 * @author fbi
 * @version $Revision$
 * $Id$
 */

import java.nio.FloatBuffer;

public class Quaternion extends Vector implements ReadableVector4f, WritableVector4f {
	private static final long serialVersionUID = 1L;
	
	/** The size in bytes of the Quaternion */
	public static final int SIZE = 16;

	public float x, y, z, w;

	/**
	 * C'tor. The quaternion will be initialized to the identity.
	 */
	public Quaternion() {
		super();
		setIdentity();
	}

	/**
	 * C'tor
	 *
	 * @param src
	 */
	public Quaternion(ReadableVector4f src) {
		set(src);
	}

	/**
	 * C'tor
	 *
	 */
	public Quaternion(float x, float y, float z, float w) {
		set(x, y, z, w);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.lwjgl.util.vector.WritableVector2f#set(float, float)
	 */
	public void set(float x, float y) {
		this.x = x;
		this.y = y;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.lwjgl.util.vector.WritableVector3f#set(float, float, float)
	 */
	public void set(float x, float y, float z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.lwjgl.util.vector.WritableVector4f#set(float, float, float,
	 *      float)
	 */
	public void set(float x, float y, float z, float w) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.w = w;
	}
	
	public void set(ReadableVector3f src, float w) {
		this.x = src.getX();
		this.y = src.getY();
		this.z = src.getZ();
		this.w = w;
	}

	/**
	 * Load from another Vector4f
	 *
	 * @param src
	 *            The source vector
	 * @return this
	 */
	public Quaternion set(ReadableVector4f src) {
		x = src.getX();
		y = src.getY();
		z = src.getZ();
		w = src.getW();
		return this;
	}

	/**
	 * Set this quaternion to the multiplication identity.
	 * @return this
	 */
	public Quaternion setIdentity() {
		return setIdentity(this);
	}

	/**
	 * Set the given quaternion to the multiplication identity.
	 * @param q The quaternion
	 * @return q
	 */
	public static Quaternion setIdentity(Quaternion q) {
		q.x = 0;
		q.y = 0;
		q.z = 0;
		q.w = 1;
		return q;
	}

	/**
	 * @return the length squared of the quaternion
	 */
	public float lengthSquared() {
		return x * x + y * y + z * z + w * w;
	}

	/**
	 * Normalise the source quaternion and place the result in another quaternion.
	 *
	 * @param src
	 *            The source quaternion
	 * @param dest
	 *            The destination quaternion, or null if a new quaternion is to be
	 *            created
	 * @return The normalised quaternion
	 */
	public static Quaternion normalise(Quaternion src, Quaternion dest) {
		float inv_l = 1f/src.length();

		if (dest == null)
			dest = new Quaternion();

		dest.set(src.x * inv_l, src.y * inv_l, src.z * inv_l, src.w * inv_l);

		return dest;
	}

	/**
	 * Normalise this quaternion and place the result in another quaternion.
	 *
	 * @param dest
	 *            The destination quaternion, or null if a new quaternion is to be
	 *            created
	 * @return the normalised quaternion
	 */
	public Quaternion normalise(Quaternion dest) {
		return normalise(this, dest);
	}

	/**
	 * The dot product of two quaternions
	 *
	 * @param left
	 *            The LHS quat
	 * @param right
	 *            The RHS quat
	 * @return left dot right
	 */
	public static float dot(Quaternion left, Quaternion right) {
		return left.x * right.x + left.y * right.y + left.z * right.z + left.w
				* right.w;
	}

	/**
	 * Calculate the conjugate of this quaternion and put it into the given one
	 *
	 * @param dest
	 *            The quaternion which should be set to the conjugate of this
	 *            quaternion
	 */
	public Quaternion negate(Quaternion dest) {
		return negate(this, dest);
	}

	/**
	 * Calculate the conjugate of this quaternion and put it into the given one
	 *
	 * @param src
	 *            The source quaternion
	 * @param dest
	 *            The quaternion which should be set to the conjugate of this
	 *            quaternion
	 */
	public static Quaternion negate(Quaternion src, Quaternion dest) {
		if (dest == null)
			dest = new Quaternion();

		dest.x = -src.x;
		dest.y = -src.y;
		dest.z = -src.z;
		dest.w = src.w;

		return dest;
	}

	/**
	 * Calculate the conjugate of this quaternion
	 */
	public Vector negate() {
		return negate(this, this);
	}

	/* (non-Javadoc)
	 * @see org.lwjgl.util.vector.Vector#load(java.nio.FloatBuffer)
	 */
	public Quaternion load(FloatBuffer buf) {
		x = buf.get();
		y = buf.get();
		z = buf.get();
		w = buf.get();
		return this;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.lwjgl.vector.Vector#scale(float)
	 */
	public Vector scale(float scale) {
		return scale(scale, this, this);
	}

	/**
	 * Scale the source quaternion by scale and put the result in the destination
	 * @param scale The amount to scale by
	 * @param src The source quaternion
	 * @param dest The destination quaternion, or null if a new quaternion is to be created
	 * @return The scaled quaternion
	 */
	public static Quaternion scale(float scale, Quaternion src, Quaternion dest) {
		if (dest == null)
			dest = new Quaternion();
		dest.x = src.x * scale;
		dest.y = src.y * scale;
		dest.z = src.z * scale;
		dest.w = src.w * scale;
		return dest;
	}

	/* (non-Javadoc)
	 * @see org.lwjgl.util.vector.ReadableVector#store(java.nio.FloatBuffer)
	 */
	public FloatBuffer store(FloatBuffer buf) {
		buf.put(x);
		buf.put(y);
		buf.put(z);
		buf.put(w);

		return buf;
	}
	
	public Quaternion load(float[] arr, int offset){
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
	 *
	 * @param x
	 */
	public final void setX(float x) {
		this.x = x;
	}

	/**
	 * Set Y
	 *
	 * @param y
	 */
	public final void setY(float y) {
		this.y = y;
	}

	/**
	 * Set Z
	 *
	 * @param z
	 */
	public void setZ(float z) {
		this.z = z;
	}

	/*
	 * (Overrides)
	 *
	 * @see org.lwjgl.vector.ReadableVector3f#getZ()
	 */
	public float getZ() {
		return z;
	}

	/**
	 * Set W
	 *
	 * @param w
	 */
	public void setW(float w) {
		this.w = w;
	}

	/*
	 * (Overrides)
	 *
	 * @see org.lwjgl.vector.ReadableVector3f#getW()
	 */
	public float getW() {
		return w;
	}

	public String toString() {
		return "Quaternion: " + x + " " + y + " " + z + " " + w;
	}

	/**
	 * Sets the value of this quaternion to the quaternion product of
	 * quaternions left and right (this = left * right). Note that this is safe
	 * for aliasing (e.g. this can be left or right).
	 *
	 * @param left
	 *            the first quaternion
	 * @param right
	 *            the second quaternion
	 */
	public static Quaternion mul(Quaternion left, Quaternion right,
			Quaternion dest) {
		if (dest == null)
			dest = new Quaternion();
		dest.set(left.x * right.w + left.w * right.x + left.y * right.z
				- left.z * right.y, left.y * right.w + left.w * right.y
				+ left.z * right.x - left.x * right.z, left.z * right.w
				+ left.w * right.z + left.x * right.y - left.y * right.x,
				left.w * right.w - left.x * right.x - left.y * right.y
				- left.z * right.z);
		return dest;
	}

	/**
	 *
	 * Multiplies quaternion left by the inverse of quaternion right and places
	 * the value into this quaternion. The value of both argument quaternions is
	 * preservered (this = left * right^-1).
	 *
	 * @param left
	 *            the left quaternion
	 * @param right
	 *            the right quaternion
	 */
	public static Quaternion mulInverse(Quaternion left, Quaternion right,
			Quaternion dest) {
		float n = right.lengthSquared();
		// zero-div may occur.
		n = (n == 0.0 ? n : 1 / n);
		// store on stack once for aliasing-safty
		if (dest == null)
			dest = new Quaternion();
		dest
			.set((left.x * right.w - left.w * right.x - left.y
						* right.z + left.z * right.y)
					* n, (left.y * right.w - left.w * right.y - left.z
						* right.x + left.x * right.z)
					* n, (left.z * right.w - left.w * right.z - left.x
						* right.y + left.y * right.x)
					* n, (left.w * right.w + left.x * right.x + left.y
						* right.y + left.z * right.z)
					* n);

		return dest;
	}

	/**
	 * Sets the value of this quaternion to the equivalent rotation of the
	 * Axis-Angle argument.
	 *
	 * @param a1
	 *            the axis-angle: (x,y,z) is the axis and w is the angle
	 */
	public final void setFromAxisAngle(ReadableVector4f a1) {
		setFromAxisAngle(a1.getX(), a1.getY(), a1.getZ(), a1.getW());
	}

	public static<T extends WritableVector3f> T getAxisY(Quaternion src, T dest){
		if(dest == null)
			dest = (T) new Vector3f();

		float x = 2.0f * ( src.x * src.y - src.z * src.w );
		float y = 1.0f - 2.0f * ( src.x * src.x + src.z * src.z );
		float z = 2.0f * ( src.y *src.z + src.x *src.w );

		src.set(x,y,z);
		return dest;
	}
	
	/**
	 * Sets the value of this quaternion to the equivalent rotation of the
	 * Axis-Angle argument.
	 *
	 * @param a1
	 *            the axis-angle: (x,y,z) is the axis
	 * @param angle 
	 * 			  the rotate angle, in radiant.
	 */
	public final void setFromAxisAngle(ReadableVector3f a1, float angle) {
		setFromAxisAngle(a1.getX(), a1.getY(), a1.getZ(), angle);
	}
	
	public final void setFromAxisAngle(float axisX, float axisY, float axisZ, float angle){
		x = axisX;
		y = axisY;
		z = axisZ;
		float n = (float) Math.sqrt(x * x + y * y + z * z);
		// zero-div may occur.
		float s = (float) (Math.sin(0.5 * angle) / n);
		x *= s;
		y *= s;
		z *= s;
		w = (float) Math.cos(0.5 * angle);
	}

	/**
	 * Sets the value of this quaternion using the rotational component of the
	 * passed matrix.
	 *
	 * @param m
	 *            The matrix
	 * @return this
	 */
	public final Quaternion setFromMatrix(Matrix4f m) {
		return setFromMat(m.m00, m.m10, m.m20, m.m01, m.m11, m.m21, m.m02,
				m.m12, m.m22);
	}
	
	/**
	 * Invert the <i>src</i> and put the result into the <i>dst</i>
	 * @param src The quaternion which will be inverted.
	 * @param dst Hold the result, will create a new one if it is null.
	 * @return
	 */
	public static final Quaternion invert(Quaternion src, Quaternion dst){
		if(dst == null) dst = new Quaternion();
		
		dst.x = -src.x;
		dst.y = -src.y;
		dst.z = -src.z;
		dst.w = src.w;
		
		return dst;
	}
	
	public void invert(){
		x = -x;
		x = -y;
		x = -z;
	}

	/**
	 * Sets the value of this quaternion using the rotational component of the
	 * passed matrix.
	 *
	 * @param m
	 *            The source matrix
	 */
	public final Quaternion setFromMatrix(Matrix3f m) {
		return setFromMatrix(m, this);
	}

	/**
	 * Sets the value of the source quaternion using the rotational component of the
	 * passed matrix.
	 *
	 * @param m
	 *            The source matrix
	 * @param q
	 *            The destination quaternion, or null if a new quaternion is to be created
	 * @return q
	 */
	public static Quaternion setFromMatrix(Matrix3f m, Quaternion q) {
		return q.setFromMat(m.m00, m.m10, m.m20, m.m01, m.m11, m.m21, m.m02,
				m.m12, m.m22);
	}
	
	public static Vector3f transform(Quaternion left, ReadableVector3f right, Vector3f dst){
		if(dst == null) dst = new Vector3f();
		final float x = left.x;
		final float y = left.y;
		final float z = left.z;
		final float w = left.w;
		
		final float srcx = right.getX();
		final float srcy = right.getY();
		final float srcz = right.getZ();
		
		float v_coef = w * w - x * x - y * y - z * z;                     
        float u_coef = 2.0f * (srcx * x + srcy * y + srcz * z);  
        float c_coef = 2.0f * w;                                       

        final float dstx = v_coef * srcx + u_coef * x + c_coef * (y * srcz - z * srcy);
        final float dsty = v_coef * srcy + u_coef * y + c_coef * (z * srcx - x * srcz);
        final float dstz = v_coef * srcz + u_coef * z + c_coef * (x * srcy - y * srcx);
        
        dst.x = dstx;
        dst.y = dsty;
        dst.z = dstz;
        
        return dst;
	}
	
	public static Vector3f transformInverse(Quaternion left, ReadableVector3f right, Vector3f dst){
		if(dst == null) dst = new Vector3f();
		final float x = -left.x;
		final float y = -left.y;
		final float z = -left.z;
		final float w = left.w;
		
		final float srcx = right.getX();
		final float srcy = right.getY();
		final float srcz = right.getZ();
		
		float v_coef = w * w - x * x - y * y - z * z;                     
        float u_coef = 2.0f * (srcx * x + srcy * y + srcz * z);  
        float c_coef = 2.0f * w;                                       

        final float dstx = v_coef * srcx + u_coef * x + c_coef * (y * srcz - z * srcy);
        final float dsty = v_coef * srcy + u_coef * y + c_coef * (z * srcx - x * srcz);
        final float dstz = v_coef * srcz + u_coef * z + c_coef * (x * srcy - y * srcx);
        
        dst.x = dstx;
        dst.y = dsty;
        dst.z = dstz;
        
        return dst;
	}

	/**
	 * Private method to perform the matrix-to-quaternion conversion
	 */
	private Quaternion setFromMat(float m00, float m01, float m02, float m10,
			float m11, float m12, float m20, float m21, float m22) {

		/*float s;
		float tr = m00 + m11 + m22;
		if (tr >= 0.0) {
			s = (float) Math.sqrt(tr + 1.0);
			w = s * 0.5f;
			s = 0.5f / s;
			x = (m21 - m12) * s;
			y = (m02 - m20) * s;
			z = (m10 - m01) * s;
		} else {
			float max = Math.max(Math.max(m00, m11), m22);
			if (max == m00) {
				s = (float) Math.sqrt(m00 - (m11 + m22) + 1.0);
				x = s * 0.5f;
				s = 0.5f / s;
				y = (m01 + m10) * s;
				z = (m20 + m02) * s;
				w = (m21 - m12) * s;
			} else if (max == m11) {
				s = (float) Math.sqrt(m11 - (m22 + m00) + 1.0);
				y = s * 0.5f;
				s = 0.5f / s;
				z = (m12 + m21) * s;
				x = (m01 + m10) * s;
				w = (m02 - m20) * s;
			} else {
				s = (float) Math.sqrt(m22 - (m00 + m11) + 1.0);
				z = s * 0.5f;
				s = 0.5f / s;
				x = (m20 + m02) * s;
				y = (m12 + m21) * s;
				w = (m10 - m01) * s;
			}
		}
		return this;*/

		fromRotationMat(m00, m01, m02, m10, m11, m12, m20, m21, m22, this);
		return this;
	}

	public static<T extends WritableVector4f> T fromRotationMat(Matrix3f mat,
																WritableVector4f out){
		final Matrix3f m = mat;
		return fromRotationMat(m.m00, m.m10, m.m20, m.m01, m.m11, m.m21, m.m02,
				m.m12, m.m22, out);
	}

	public static<T extends WritableVector4f> T fromRotationMat(Matrix4f mat,
																WritableVector4f out){
		final Matrix4f m = mat;
		return fromRotationMat(m.m00, m.m10, m.m20, m.m01, m.m11, m.m21, m.m02,
				m.m12, m.m22, out);
	}

	public static<T extends WritableVector4f> T fromRotationMat(float m00, float m01, float m02, float m10,
																float m11, float m12, float m20, float m21, float m22,
																WritableVector4f out){
		if(out == null){
			out = new Quaternion();
		}

		float x,y,z,w;
		float s;
		float tr = m00 + m11 + m22;
		if (tr >= 0.0) {
			s = (float) Math.sqrt(tr + 1.0);
			w = s * 0.5f;
			s = 0.5f / s;
			x = (m21 - m12) * s;
			y = (m02 - m20) * s;
			z = (m10 - m01) * s;
		} else {
			float max = Math.max(Math.max(m00, m11), m22);
			if (max == m00) {
				s = (float) Math.sqrt(m00 - (m11 + m22) + 1.0);
				x = s * 0.5f;
				s = 0.5f / s;
				y = (m01 + m10) * s;
				z = (m20 + m02) * s;
				w = (m21 - m12) * s;
			} else if (max == m11) {
				s = (float) Math.sqrt(m11 - (m22 + m00) + 1.0);
				y = s * 0.5f;
				s = 0.5f / s;
				z = (m12 + m21) * s;
				x = (m01 + m10) * s;
				w = (m02 - m20) * s;
			} else {
				s = (float) Math.sqrt(m22 - (m00 + m11) + 1.0);
				z = s * 0.5f;
				s = 0.5f / s;
				x = (m20 + m02) * s;
				y = (m12 + m21) * s;
				w = (m10 - m01) * s;
			}
		}

		out.set(x,y,z,w);
		return (T)out;
	}

	public static Matrix4f toMatrix4f(float x, float y, float z, float w, Matrix4f mat){
		if(mat == null) mat = new Matrix4f();

		final float q0 = w;
		final float q1 = x;
		final float q2 = y;
		final float q3 = z;

		float q00 = q0 * q0;
		float q11 = q1 * q1;
		float q22 = q2 * q2;
		float q33 = q3 * q3;
		// Diagonal elements
		mat.m00 = q00 + q11 - q22 - q33;
		mat.m11 = q00 - q11 + q22 - q33;
		mat.m22 = q00 - q11 - q22 + q33;
		// 0,1 and 1,0 elements
		float q03 = q0 * q3;
		float q12 = q1 * q2;
		mat.m10 = 2.0f * (q12 - q03);
		mat.m01 = 2.0f * (q03 + q12);
		// 0,2 and 2,0 elements
		float q02 = q0 * q2;
		float q13 = q1 * q3;
		mat.m20 = 2.0f * (q02 + q13);
		mat.m02 = 2.0f * (q13 - q02);
		// 1,2 and 2,1 elements
		float q01 = q0 * q1;
		float q23 = q2 * q3;
		mat.m21 = 2.0f * (q23 - q01);
		mat.m12 = 2.0f * (q01 + q23);

		return mat;
	}
	
	/**
	 * Turns this rotation into a 3x3 rotation matrix. NOTE: only mutates the
	 * upper-left 3x3 of the passed Mat4f. Implementation from B. K. P. Horn's
	 * <u>Robot Vision</u> textbook.
	 */
	public Matrix4f toMatrix(Matrix4f mat) {
		return toMatrix4f(x, y, z, w, mat);
	}
	
	public Matrix3f toMatrix(Matrix3f mat) {
		if(mat == null) mat = new Matrix3f();
		
		final float q0 = w;
		final float q1 = x;
		final float q2 = y;
		final float q3 = z;
		
		float q00 = q0 * q0;
		float q11 = q1 * q1;
		float q22 = q2 * q2;
		float q33 = q3 * q3;
		// Diagonal elements
		mat.m00 = q00 + q11 - q22 - q33;
		mat.m11 = q00 - q11 + q22 - q33;
		mat.m22 = q00 - q11 - q22 + q33;
		// 0,1 and 1,0 elements
		float q03 = q0 * q3;
		float q12 = q1 * q2;
		mat.m10 = 2.0f * (q12 - q03);
		mat.m01 = 2.0f * (q03 + q12);
		// 0,2 and 2,0 elements
		float q02 = q0 * q2;
		float q13 = q1 * q3;
		mat.m20 = 2.0f * (q02 + q13);
		mat.m02 = 2.0f * (q13 - q02);
		// 1,2 and 2,1 elements
		float q01 = q0 * q1;
		float q23 = q2 * q3;
		mat.m21 = 2.0f * (q23 - q01);
		mat.m12 = 2.0f * (q01 + q23);
		
		return mat;
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
	
	public static Quaternion slerp(Quaternion r0, Quaternion r1, float alpha, Quaternion dest){
		if(dest == null) dest = new Quaternion();
		
		final float dot = dot(r0, r1);
		float absDot = dot < 0.f ? -dot : dot;

		// Set the first and second scale for the interpolation
		float scale0 = 1 - alpha;
		float scale1 = alpha;

		// Check if the angle between the 2 quaternions was big enough to
		// warrant such calculations
		if ((1 - absDot) > 0.1) {// Get the angle between the 2 quaternions,
			// and then store the sin() of that angle
			final double angle = Math.acos(absDot);
			final double invSinTheta = 1f / Math.sin(angle);

			// Calculate the scale for q1 and q2, according to the angle and
			// it's sine value
			scale0 = (float)(Math.sin((1 - alpha) * angle) * invSinTheta);
			scale1 = (float)(Math.sin((alpha * angle)) * invSinTheta);
		}

		if (dot < 0.f) scale1 = -scale1;

		// Calculate the x, y, z and w values for the quaternion by using a
		// special form of linear interpolation for quaternions.
		dest.x = (scale0 * r0.x) + (scale1 * r1.x);
		dest.y = (scale0 * r0.y) + (scale1 * r1.y);
		dest.z = (scale0 * r0.z) + (scale1 * r1.z);
		dest.w = (scale0 * r0.w) + (scale1 * r1.w);
		
		return dest;
	}

	public void fromToRotation(ReadableVector3f from, ReadableVector3f to)
	{
		Vector3f fromDir = new Vector3f(from);
		Vector3f toDir = new Vector3f(to);

		if (fromDir.isZero() || toDir.isZero()) return;

		//fromDir
		float max = Math.abs(fromDir.getX());
		max = max > Math.abs(fromDir.getY()) ? max : Math.abs(fromDir.getY());
		max = (max > Math.abs(fromDir.getZ())) ? max : Math.abs(fromDir.getZ());

//		fromDir = fromDir / max;
		fromDir.scale(1f/max);

		//toDir
		max = Math.abs(toDir.x);
		max = (max > Math.abs(toDir.y)) ? max : Math.abs(toDir.y);
		max = (max > Math.abs(toDir.z)) ? max : Math.abs(toDir.z);
//		toDir = toDir / max;
		toDir.scale(1f/max);

		float miniThreshold = 0.001f;
		fromDir.x = Math.abs(fromDir.x) <= miniThreshold ? 0 : fromDir.x;
		fromDir.y = Math.abs(fromDir.y) <= miniThreshold ? 0 : fromDir.y;
		fromDir.z = Math.abs(fromDir.z) <= miniThreshold ? 0 : fromDir.z;
		toDir.x = Math.abs(toDir.x) <= miniThreshold ? 0 : toDir.x;
		toDir.y = Math.abs(toDir.y) <= miniThreshold ?0: toDir.y;
		toDir.z = Math.abs(toDir.z) <= miniThreshold ? 0 : toDir.z;

		float fromDirZ = fromDir.z;
		float toDirY = toDir.y;
//		Vector3 mid = (fromDir.normalized + toDir.normalized).normalized;

		fromDir.normalise();
		toDir.normalise();
		Vector3f mid = Vector3f.add(fromDir , toDir, null);
		mid.normalise();
		if (mid.isZero())
		{
			if (fromDir.x != 0 && fromDir.y == 0 && fromDir.z == 0) {
//				return new Quaternion();
				set(0, 1, 0, 0);
				return;
				//Y
			}else if (fromDir.x == 0 && fromDir.y != 0 && fromDir.z == 0) {
				set(1, 0, 0, 0);
				return;
				//Z
			}else if (fromDir.x == 0 && fromDir.y == 0 && fromDir.z != 0) {
				set(1, 0, 0, 0);
				return;
				//X
			}else if (fromDir.x == 0 && fromDir.y != 0 && fromDir.z != 0) {
				set(1, 0, 0, 0);
				return;
				//Y
			}else if (fromDir.x != 0 && fromDir.y == 0 && fromDir.z != 0)
			{
				float X = toDir./*normalized.*/z;
				float Z = fromDir./*normalized.*/x;
				//正负判定
				if (X + Z < 0 || (X + Z == 0 && X < 0)) {
					set(-X, 0, -Z, 0);
					return;
				}else {
					set(X, 0, Z, 0);
					return;
				}
			}
			//Z
			else if (fromDir.x != 0 && fromDir.y != 0 && fromDir.z == 0)
			{
				float X = toDir./*normalized.*/y;
				float Y = fromDir./*normalized.*/x;
				//正负判定
				if (X + Y < 0 || (X + Y == 0 && X < 0)) {
					set(-X, -Y, 0, 0);
					return;
				}else {
					set(X, Y, 0, 0);
					return;
				}
			}
			else
			{
				mid.y = fromDirZ;
				mid.z = toDirY;
//				mid = mid.normalized;
				mid.normalise();
			}
		}

//		q = new Quaternion(-toDir.normalized.x, -toDir.normalized.y, -toDir.normalized.z, 0) * new Quaternion(mid.normalized.x, mid.normalized.y, mid.normalized.z, 0);

		mul(new Quaternion(-toDir.x, -toDir.y, -toDir.z, 0), new Quaternion(mid.x, mid.y, mid.z, 0), this);
	}


	public void setFromEulerAngles(ReadableVector3f euler) {
		setFromEulerAngles(euler.getX(),euler.getY(),euler.getZ());
	}

	public void setFromEulerAngles(float eulerX, float eulerY, float eulerZ) {
		float xDiv2 = eulerX / 2;
		float yDiv2 = eulerY / 2;
		float zDiv2 = eulerZ / 2;

		double cosXDiv2 = Math.cos(xDiv2);
		double sinXDiv2 = Math.sin(xDiv2);
		double cosYDiv2 = Math.cos(yDiv2);
		double sinYDiv2 = Math.sin(yDiv2);

		if (zDiv2 == 0)
		{
			w = (float) (cosXDiv2 * cosYDiv2);
			x = (float) (sinXDiv2 * cosYDiv2);
			y = (float) (cosXDiv2 * sinYDiv2);
			z = (float) (-sinXDiv2 * sinYDiv2);
		}
		else
		{

			double cosZDiv2 = Math.cos(zDiv2);
			double sinZDiv2 = Math.sin(zDiv2);
			w = (float) (cosXDiv2 * cosYDiv2 * cosZDiv2 + sinXDiv2 * sinYDiv2 * sinZDiv2);
			x = (float) (sinXDiv2 * cosYDiv2 * cosZDiv2 + cosXDiv2 * sinYDiv2 * sinZDiv2);
			y = (float) (cosXDiv2 * sinYDiv2 * cosZDiv2 - sinXDiv2 * cosYDiv2 * sinZDiv2);
			z = (float) (cosXDiv2 * cosYDiv2 * sinZDiv2 - sinXDiv2 * sinYDiv2 * cosZDiv2);
		}
	}

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
	
	@Override
	public boolean isNaN() {return x != x || y != y || z != z || w != w;}

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

	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		Quaternion other = (Quaternion) obj;
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
	public ByteBuffer store(ByteBuffer buf) {
		buf.putFloat(x).putFloat(y).putFloat(z).putFloat(w);
		return buf;
	}

	@Override
	public Quaternion load(ByteBuffer buf) {
		x = buf.getFloat();
		y = buf.getFloat();
		z = buf.getFloat();
		w = buf.getFloat();
		return this;
	}
}
