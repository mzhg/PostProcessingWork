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
 * Holds a 4x4 float matrix.
 *
 * @author foo
 */
public class Matrix4f extends Matrix implements Serializable {

	private static final long serialVersionUID = 20171118110230L;
	
	public static final Matrix4f IDENTITY = new Matrix4f();
	
	/** The size in bytes of the Matrix4f */
	public static final int SIZE = 64;

	public float m00, m01, m02, m03, m10, m11, m12, m13, m20, m21, m22, m23, m30, m31, m32, m33;
	
	public Matrix4f(float m00, float m01, float m02, float m03, float m10, float m11, float m12, float m13, float m20,
			float m21, float m22, float m23, float m30, float m31, float m32, float m33) {
		this.m00 = m00;
		this.m01 = m01;
		this.m02 = m02;
		this.m03 = m03;
		this.m10 = m10;
		this.m11 = m11;
		this.m12 = m12;
		this.m13 = m13;
		this.m20 = m20;
		this.m21 = m21;
		this.m22 = m22;
		this.m23 = m23;
		this.m30 = m30;
		this.m31 = m31;
		this.m32 = m32;
		this.m33 = m33;
	}

	/**
	 * Construct a new matrix, initialized to the identity.
	 */
	public Matrix4f() {
		super();
		setIdentity();
	}

	public Matrix4f(final Matrix4f src) {
		super();
		load(src);
	}
	
	public void set(float m00, float m01, float m02, float m03, float m10, float m11, float m12, float m13, float m20,
			float m21, float m22, float m23, float m30, float m31, float m32, float m33) {
		this.m00 = m00;
		this.m01 = m01;
		this.m02 = m02;
		this.m03 = m03;
		this.m10 = m10;
		this.m11 = m11;
		this.m12 = m12;
		this.m13 = m13;
		this.m20 = m20;
		this.m21 = m21;
		this.m22 = m22;
		this.m23 = m23;
		this.m30 = m30;
		this.m31 = m31;
		this.m32 = m32;
		this.m33 = m33;
	}

	/**
	 * Returns a string representation of this matrix
	 */
	public String toString() {
		StringBuilder buf = new StringBuilder(128);  // at least 64 for identity matrix.
		buf.append(m00).append(' ').append(m10).append(' ').append(m20).append(' ').append(m30).append('\n');
		buf.append(m01).append(' ').append(m11).append(' ').append(m21).append(' ').append(m31).append('\n');
		buf.append(m02).append(' ').append(m12).append(' ').append(m22).append(' ').append(m32).append('\n');
		buf.append(m03).append(' ').append(m13).append(' ').append(m23).append(' ').append(m33).append('\n');
		return buf.toString();
	}

	/**
	 * Set this matrix to be the identity matrix.
	 * @return this
	 */
	public Matrix4f setIdentity() {
		return setIdentity(this);
	}
	
	public Matrix4f setTranslate(ReadableVector3f v){
		return setTranslate(v.getX(), v.getY(), v.getZ());
	}
	
	public Matrix4f setTranslate(float x, float y, float z){
		m00 = 1.0f; m10 = 0; m20 = 0; m30 = x;
		m01 = 0.0f; m11 = 1; m21 = 0; m31 = y;
		m02 = 0.0f; m12 = 0; m22 = 1; m32 = z;
		m03 = 0.0f; m13 = 0; m23 = 0; m33 = 1;
		return this;
	}

	/**
	 * Set the given matrix to be the identity matrix.
	 * @param m The matrix to set to the identity
	 * @return m
	 */
	public static Matrix4f setIdentity(Matrix4f m) {
		m.m00 = 1.0f;
		m.m01 = 0.0f;
		m.m02 = 0.0f;
		m.m03 = 0.0f;
		m.m10 = 0.0f;
		m.m11 = 1.0f;
		m.m12 = 0.0f;
		m.m13 = 0.0f;
		m.m20 = 0.0f;
		m.m21 = 0.0f;
		m.m22 = 1.0f;
		m.m23 = 0.0f;
		m.m30 = 0.0f;
		m.m31 = 0.0f;
		m.m32 = 0.0f;
		m.m33 = 1.0f;

		return m;
	}

	/**
	 * Set this matrix to 0.
	 * @return this
	 */
	public Matrix4f setZero() {
		return setZero(this);
	}

	/**
	 * Set the given matrix to 0.
	 * @param m The matrix to set to 0
	 * @return m
	 */
	public static Matrix4f setZero(Matrix4f m) {
		m.m00 = 0.0f;
		m.m01 = 0.0f;
		m.m02 = 0.0f;
		m.m03 = 0.0f;
		m.m10 = 0.0f;
		m.m11 = 0.0f;
		m.m12 = 0.0f;
		m.m13 = 0.0f;
		m.m20 = 0.0f;
		m.m21 = 0.0f;
		m.m22 = 0.0f;
		m.m23 = 0.0f;
		m.m30 = 0.0f;
		m.m31 = 0.0f;
		m.m32 = 0.0f;
		m.m33 = 0.0f;

		return m;
	}

	/**
	 * Load from another matrix4f
	 * @param src The source matrix
	 * @return this
	 */
	public Matrix4f load(Matrix4f src) {
		return load(src, this);
	}
	
	/**
	 * Load left-upper 3x3 values from a matrix3f, leave the remain values unchanged.
	 */
	public Matrix4f load(Matrix3f src){
		m00 = src.m00;
		m01 = src.m01;
		m02 = src.m02;
		m10 = src.m10;
		m11 = src.m11;
		m12 = src.m12;
		m20 = src.m20;
		m21 = src.m21;
		m22 = src.m22;
		
		return this;
	}

	/**
	 * Copy the source matrix to the destination matrix
	 * @param src The source matrix
	 * @param dest The destination matrix, or null of a new one is to be created
	 * @return The copied matrix
	 */
	public static Matrix4f load(Matrix4f src, Matrix4f dest) {
		if (dest == null)
			dest = new Matrix4f();

		if(src == dest)return src;

		dest.m00 = src.m00;
		dest.m01 = src.m01;
		dest.m02 = src.m02;
		dest.m03 = src.m03;
		dest.m10 = src.m10;
		dest.m11 = src.m11;
		dest.m12 = src.m12;
		dest.m13 = src.m13;
		dest.m20 = src.m20;
		dest.m21 = src.m21;
		dest.m22 = src.m22;
		dest.m23 = src.m23;
		dest.m30 = src.m30;
		dest.m31 = src.m31;
		dest.m32 = src.m32;
		dest.m33 = src.m33;

		return dest;
	}
	
	/**
	 * Load from a float buffer. The buffer stores the matrix in column major
	 * (OpenGL) order.
	 *
	 * @param buf A float buffer to read from
	 * @return this
	 */
	public Matrix4f load(FloatBuffer buf) {

		m00 = buf.get();
		m01 = buf.get();
		m02 = buf.get();
		m03 = buf.get();
		m10 = buf.get();
		m11 = buf.get();
		m12 = buf.get();
		m13 = buf.get();
		m20 = buf.get();
		m21 = buf.get();
		m22 = buf.get();
		m23 = buf.get();
		m30 = buf.get();
		m31 = buf.get();
		m32 = buf.get();
		m33 = buf.get();

		return this;
	}
	
	/**
	 * Load from a float buffer. The buffer stores the matrix in column major
	 * (OpenGL) order.
	 *
	 * @param buf A float buffer to read from
	 * @return this
	 */
	public Matrix4f load(ByteBuffer buf) {

		m00 = buf.getFloat();
		m01 = buf.getFloat();
		m02 = buf.getFloat();
		m03 = buf.getFloat();
		m10 = buf.getFloat();
		m11 = buf.getFloat();
		m12 = buf.getFloat();
		m13 = buf.getFloat();
		m20 = buf.getFloat();
		m21 = buf.getFloat();
		m22 = buf.getFloat();
		m23 = buf.getFloat();
		m30 = buf.getFloat();
		m31 = buf.getFloat();
		m32 = buf.getFloat();
		m33 = buf.getFloat();

		return this;
	}

	/**
	 * Load from a float buffer. The buffer stores the matrix in row major
	 * (maths) order.
	 *
	 * @param buf A float buffer to read from
	 * @return this
	 */
	public Matrix4f loadTranspose(FloatBuffer buf) {

		m00 = buf.get();
		m10 = buf.get();
		m20 = buf.get();
		m30 = buf.get();
		m01 = buf.get();
		m11 = buf.get();
		m21 = buf.get();
		m31 = buf.get();
		m02 = buf.get();
		m12 = buf.get();
		m22 = buf.get();
		m32 = buf.get();
		m03 = buf.get();
		m13 = buf.get();
		m23 = buf.get();
		m33 = buf.get();

		return this;
	}

	/**
	 * Load from a float buffer. The buffer stores the matrix in row major
	 * (maths) order.
	 *
	 * @param buf A float buffer to read from
	 * @return this
	 */
	public Matrix4f loadTranspose(ByteBuffer buf) {

		m00 = buf.getFloat();
		m10 = buf.getFloat();
		m20 = buf.getFloat();
		m30 = buf.getFloat();
		m01 = buf.getFloat();
		m11 = buf.getFloat();
		m21 = buf.getFloat();
		m31 = buf.getFloat();
		m02 = buf.getFloat();
		m12 = buf.getFloat();
		m22 = buf.getFloat();
		m32 = buf.getFloat();
		m03 = buf.getFloat();
		m13 = buf.getFloat();
		m23 = buf.getFloat();
		m33 = buf.getFloat();

		return this;
	}
	
	/**
	 * Store this matrix in a float buffer. The matrix is stored in column
	 * major (openGL) order.
	 * @param buf The buffer to store this matrix in
	 */
	public FloatBuffer store(FloatBuffer buf) {
		buf.put(m00);
		buf.put(m01);
		buf.put(m02);
		buf.put(m03);
		buf.put(m10);
		buf.put(m11);
		buf.put(m12);
		buf.put(m13);
		buf.put(m20);
		buf.put(m21);
		buf.put(m22);
		buf.put(m23);
		buf.put(m30);
		buf.put(m31);
		buf.put(m32);
		buf.put(m33);
		return buf;
	}
	
	/**
	 * Store this matrix in a float buffer. The matrix is stored in column
	 * major (openGL) order.
	 * @param buf The buffer to store this matrix in
	 */
	public ByteBuffer store(ByteBuffer buf){
		buf.putFloat(m00);
		buf.putFloat(m01);
		buf.putFloat(m02);
		buf.putFloat(m03);
		buf.putFloat(m10);
		buf.putFloat(m11);
		buf.putFloat(m12);
		buf.putFloat(m13);
		buf.putFloat(m20);
		buf.putFloat(m21);
		buf.putFloat(m22);
		buf.putFloat(m23);
		buf.putFloat(m30);
		buf.putFloat(m31);
		buf.putFloat(m32);
		buf.putFloat(m33);
		return buf;
	}
	
	public Matrix3f store(Matrix3f dest){
		if(dest == null)
			dest = new Matrix3f();
		
		dest.m00 = m00;
		dest.m01 = m01;
		dest.m02 = m02;
		dest.m10 = m10;
		dest.m11 = m11;
		dest.m12 = m12;
		dest.m20 = m20;
		dest.m21 = m21;
		dest.m22 = m22;
		
		return dest;
	}

	/**
	 * Store this matrix in a float buffer. The matrix is stored in row
	 * major (maths) order.
	 * @param buf The buffer to store this matrix in
	 */
	public FloatBuffer storeTranspose(FloatBuffer buf) {
		buf.put(m00);
		buf.put(m10);
		buf.put(m20);
		buf.put(m30);
		buf.put(m01);
		buf.put(m11);
		buf.put(m21);
		buf.put(m31);
		buf.put(m02);
		buf.put(m12);
		buf.put(m22);
		buf.put(m32);
		buf.put(m03);
		buf.put(m13);
		buf.put(m23);
		buf.put(m33);
		return buf;
	}
	
	/**
	 * Store this matrix in a float buffer. The matrix is stored in row
	 * major (maths) order.
	 * @param buf The buffer to store this matrix in
	 */
	public Matrix4f storeTranspose(ByteBuffer buf) {
		buf.putFloat(m00);
		buf.putFloat(m10);
		buf.putFloat(m20);
		buf.putFloat(m30);
		buf.putFloat(m01);
		buf.putFloat(m11);
		buf.putFloat(m21);
		buf.putFloat(m31);
		buf.putFloat(m02);
		buf.putFloat(m12);
		buf.putFloat(m22);
		buf.putFloat(m32);
		buf.putFloat(m03);
		buf.putFloat(m13);
		buf.putFloat(m23);
		buf.putFloat(m33);
		return this;
	}

	/**
	 * Store the rotation portion of this matrix in a float buffer. The matrix is stored in column
	 * major (openGL) order.
	 * @param buf The buffer to store this matrix in
	 */
	public Matrix4f store3f(FloatBuffer buf) {
		buf.put(m00);
		buf.put(m01);
		buf.put(m02);
		buf.put(m10);
		buf.put(m11);
		buf.put(m12);
		buf.put(m20);
		buf.put(m21);
		buf.put(m22);
		return this;
	}
	
	/**
	 * Store the rotation portion of this matrix in a float buffer. The matrix is stored in column
	 * major (openGL) order.
	 * @param buf The buffer to store this matrix in
	 */
	public Matrix4f store3f(ByteBuffer buf) {
		buf.putFloat(m00);
		buf.putFloat(m01);
		buf.putFloat(m02);
		buf.putFloat(m10);
		buf.putFloat(m11);
		buf.putFloat(m12);
		buf.putFloat(m20);
		buf.putFloat(m21);
		buf.putFloat(m22);
		return this;
	}

	/**
	 * Add two matrices together and place the result in a third matrix.
	 * @param left The left source matrix
	 * @param right The right source matrix
	 * @param dest The destination matrix, or null if a new one is to be created
	 * @return the destination matrix
	 */
	public static Matrix4f add(Matrix4f left, Matrix4f right, Matrix4f dest) {
		if (dest == null)
			dest = new Matrix4f();

		dest.m00 = left.m00 + right.m00;
		dest.m01 = left.m01 + right.m01;
		dest.m02 = left.m02 + right.m02;
		dest.m03 = left.m03 + right.m03;
		dest.m10 = left.m10 + right.m10;
		dest.m11 = left.m11 + right.m11;
		dest.m12 = left.m12 + right.m12;
		dest.m13 = left.m13 + right.m13;
		dest.m20 = left.m20 + right.m20;
		dest.m21 = left.m21 + right.m21;
		dest.m22 = left.m22 + right.m22;
		dest.m23 = left.m23 + right.m23;
		dest.m30 = left.m30 + right.m30;
		dest.m31 = left.m31 + right.m31;
		dest.m32 = left.m32 + right.m32;
		dest.m33 = left.m33 + right.m33;

		return dest;
	}

	/**
	 * Subtract the right matrix from the left and place the result in a third matrix.
	 * @param left The left source matrix
	 * @param right The right source matrix
	 * @param dest The destination matrix, or null if a new one is to be created
	 * @return the destination matrix
	 */
	public static Matrix4f sub(Matrix4f left, Matrix4f right, Matrix4f dest) {
		if (dest == null)
			dest = new Matrix4f();

		dest.m00 = left.m00 - right.m00;
		dest.m01 = left.m01 - right.m01;
		dest.m02 = left.m02 - right.m02;
		dest.m03 = left.m03 - right.m03;
		dest.m10 = left.m10 - right.m10;
		dest.m11 = left.m11 - right.m11;
		dest.m12 = left.m12 - right.m12;
		dest.m13 = left.m13 - right.m13;
		dest.m20 = left.m20 - right.m20;
		dest.m21 = left.m21 - right.m21;
		dest.m22 = left.m22 - right.m22;
		dest.m23 = left.m23 - right.m23;
		dest.m30 = left.m30 - right.m30;
		dest.m31 = left.m31 - right.m31;
		dest.m32 = left.m32 - right.m32;
		dest.m33 = left.m33 - right.m33;

		return dest;
	}

	/**
	 * Multiply the right matrix by the left and place the result in a third matrix.
	 * @param left The left source matrix
	 * @param right The right source matrix
	 * @param dest The destination matrix, or null if a new one is to be created
	 * @return the destination matrix
	 */
	public static Matrix4f mul(Matrix4f left, Matrix4f right, Matrix4f dest) {
		if (dest == null)
			dest = new Matrix4f();

		float m00 = left.m00 * right.m00 + left.m10 * right.m01 + left.m20 * right.m02 + left.m30 * right.m03;
		float m01 = left.m01 * right.m00 + left.m11 * right.m01 + left.m21 * right.m02 + left.m31 * right.m03;
		float m02 = left.m02 * right.m00 + left.m12 * right.m01 + left.m22 * right.m02 + left.m32 * right.m03;
		float m03 = left.m03 * right.m00 + left.m13 * right.m01 + left.m23 * right.m02 + left.m33 * right.m03;
		float m10 = left.m00 * right.m10 + left.m10 * right.m11 + left.m20 * right.m12 + left.m30 * right.m13;
		float m11 = left.m01 * right.m10 + left.m11 * right.m11 + left.m21 * right.m12 + left.m31 * right.m13;
		float m12 = left.m02 * right.m10 + left.m12 * right.m11 + left.m22 * right.m12 + left.m32 * right.m13;
		float m13 = left.m03 * right.m10 + left.m13 * right.m11 + left.m23 * right.m12 + left.m33 * right.m13;
		float m20 = left.m00 * right.m20 + left.m10 * right.m21 + left.m20 * right.m22 + left.m30 * right.m23;
		float m21 = left.m01 * right.m20 + left.m11 * right.m21 + left.m21 * right.m22 + left.m31 * right.m23;
		float m22 = left.m02 * right.m20 + left.m12 * right.m21 + left.m22 * right.m22 + left.m32 * right.m23;
		float m23 = left.m03 * right.m20 + left.m13 * right.m21 + left.m23 * right.m22 + left.m33 * right.m23;
		float m30 = left.m00 * right.m30 + left.m10 * right.m31 + left.m20 * right.m32 + left.m30 * right.m33;
		float m31 = left.m01 * right.m30 + left.m11 * right.m31 + left.m21 * right.m32 + left.m31 * right.m33;
		float m32 = left.m02 * right.m30 + left.m12 * right.m31 + left.m22 * right.m32 + left.m32 * right.m33;
		float m33 = left.m03 * right.m30 + left.m13 * right.m31 + left.m23 * right.m32 + left.m33 * right.m33;

		dest.m00 = m00;
		dest.m01 = m01;
		dest.m02 = m02;
		dest.m03 = m03;
		dest.m10 = m10;
		dest.m11 = m11;
		dest.m12 = m12;
		dest.m13 = m13;
		dest.m20 = m20;
		dest.m21 = m21;
		dest.m22 = m22;
		dest.m23 = m23;
		dest.m30 = m30;
		dest.m31 = m31;
		dest.m32 = m32;
		dest.m33 = m33;

		return dest;
	}
	
	/**
	 * Multiply the right matrix by the left and place the result in a third matrix.
	 * @param left The left source matrix
	 * @param right The right source matrix
	 * @param dest The destination matrix, or null if a new one is to be created
	 * @return the destination matrix
	 */
	public static Matrix4f mul(Matrix3f left, Matrix4f right, Matrix4f dest) {
		if (dest == null)
			dest = new Matrix4f();

		float m00 = left.m00 * right.m00 + left.m10 * right.m01 + left.m20 * right.m02;
		float m01 = left.m01 * right.m00 + left.m11 * right.m01 + left.m21 * right.m02;
		float m02 = left.m02 * right.m00 + left.m12 * right.m01 + left.m22 * right.m02;
		float m03 = right.m03;
		float m10 = left.m00 * right.m10 + left.m10 * right.m11 + left.m20 * right.m12;
		float m11 = left.m01 * right.m10 + left.m11 * right.m11 + left.m21 * right.m12;
		float m12 = left.m02 * right.m10 + left.m12 * right.m11 + left.m22 * right.m12;
		float m13 = right.m13;
		float m20 = left.m00 * right.m20 + left.m10 * right.m21 + left.m20 * right.m22;
		float m21 = left.m01 * right.m20 + left.m11 * right.m21 + left.m21 * right.m22;
		float m22 = left.m02 * right.m20 + left.m12 * right.m21 + left.m22 * right.m22;
		float m23 = right.m23;
		float m30 = left.m00 * right.m30 + left.m10 * right.m31 + left.m20 * right.m32;
		float m31 = left.m01 * right.m30 + left.m11 * right.m31 + left.m21 * right.m32;
		float m32 = left.m02 * right.m30 + left.m12 * right.m31 + left.m22 * right.m32;
		float m33 = right.m33;

		dest.m00 = m00;
		dest.m01 = m01;
		dest.m02 = m02;
		dest.m03 = m03;
		dest.m10 = m10;
		dest.m11 = m11;
		dest.m12 = m12;
		dest.m13 = m13;
		dest.m20 = m20;
		dest.m21 = m21;
		dest.m22 = m22;
		dest.m23 = m23;
		dest.m30 = m30;
		dest.m31 = m31;
		dest.m32 = m32;
		dest.m33 = m33;

		return dest;
	}
	
	/**
	 * Multiply the right matrix by the left and place the result in a third matrix.
	 * @param left The left source matrix
	 * @param right The right source matrix
	 * @param dest The destination matrix, or null if a new one is to be created
	 * @return the destination matrix
	 */
	public static Matrix4f mul(Matrix4f left, Matrix3f right, Matrix4f dest) {
		if (dest == null)
			dest = new Matrix4f();

		float m00 = left.m00 * right.m00 + left.m10 * right.m01 + left.m20 * right.m02;
		float m01 = left.m01 * right.m00 + left.m11 * right.m01 + left.m21 * right.m02;
		float m02 = left.m02 * right.m00 + left.m12 * right.m01 + left.m22 * right.m02;
		float m03 = left.m03 * right.m00 + left.m13 * right.m01 + left.m23 * right.m02;
		float m10 = left.m00 * right.m10 + left.m10 * right.m11 + left.m20 * right.m12;
		float m11 = left.m01 * right.m10 + left.m11 * right.m11 + left.m21 * right.m12;
		float m12 = left.m02 * right.m10 + left.m12 * right.m11 + left.m22 * right.m12;
		float m13 = left.m03 * right.m10 + left.m13 * right.m11 + left.m23 * right.m12;
		float m20 = left.m00 * right.m20 + left.m10 * right.m21 + left.m20 * right.m22;
		float m21 = left.m01 * right.m20 + left.m11 * right.m21 + left.m21 * right.m22;
		float m22 = left.m02 * right.m20 + left.m12 * right.m21 + left.m22 * right.m22;
		float m23 = left.m03 * right.m20 + left.m13 * right.m21 + left.m23 * right.m22;
		float m30 = left.m30;
		float m31 = left.m31;
		float m32 = left.m32;
		float m33 = left.m33;

		dest.m00 = m00;
		dest.m01 = m01;
		dest.m02 = m02;
		dest.m03 = m03;
		dest.m10 = m10;
		dest.m11 = m11;
		dest.m12 = m12;
		dest.m13 = m13;
		dest.m20 = m20;
		dest.m21 = m21;
		dest.m22 = m22;
		dest.m23 = m23;
		dest.m30 = m30;
		dest.m31 = m31;
		dest.m32 = m32;
		dest.m33 = m33;

		return dest;
	}

	/**
	 * Multiply the right matrix by the left and place the result in a third matrix.
	 * @param left The left source matrix
	 * @param right The right source matrix
	 * @param dest The destination matrix, or null if a new one is to be created
	 * @return the destination matrix
	 */
	public static Matrix3f mul(Matrix4f left, Matrix3f right, Matrix3f dest) {
		if (dest == null)
			dest = new Matrix3f();

		float m00 = left.m00 * right.m00 + left.m10 * right.m01 + left.m20 * right.m02;
		float m01 = left.m01 * right.m00 + left.m11 * right.m01 + left.m21 * right.m02;
		float m02 = left.m02 * right.m00 + left.m12 * right.m01 + left.m22 * right.m02;
//		float m03 = left.m03 * right.m00 + left.m13 * right.m01 + left.m23 * right.m02;
		float m10 = left.m00 * right.m10 + left.m10 * right.m11 + left.m20 * right.m12;
		float m11 = left.m01 * right.m10 + left.m11 * right.m11 + left.m21 * right.m12;
		float m12 = left.m02 * right.m10 + left.m12 * right.m11 + left.m22 * right.m12;
//		float m13 = left.m03 * right.m10 + left.m13 * right.m11 + left.m23 * right.m12;
		float m20 = left.m00 * right.m20 + left.m10 * right.m21 + left.m20 * right.m22;
		float m21 = left.m01 * right.m20 + left.m11 * right.m21 + left.m21 * right.m22;
		float m22 = left.m02 * right.m20 + left.m12 * right.m21 + left.m22 * right.m22;
//		float m23 = left.m03 * right.m20 + left.m13 * right.m21 + left.m23 * right.m22;
//		float m30 = left.m30;
//		float m31 = left.m31;
//		float m32 = left.m32;
//		float m33 = left.m33;

		dest.m00 = m00;
		dest.m01 = m01;
		dest.m02 = m02;
//		dest.m03 = m03;
		dest.m10 = m10;
		dest.m11 = m11;
		dest.m12 = m12;
//		dest.m13 = m13;
		dest.m20 = m20;
		dest.m21 = m21;
		dest.m22 = m22;
//		dest.m23 = m23;
//		dest.m30 = m30;
//		dest.m31 = m31;
//		dest.m32 = m32;
//		dest.m33 = m33;

		return dest;
	}

	/**
	 * Transform a Vector by a matrix and return the result in a destination
	 * vector.
	 * @param left The left matrix
	 * @param right The right vector
	 * @param dest The destination vector, or null if a new one is to be created
	 * @return the destination vector
	 */
	public static Vector4f transform(Matrix4f left, Vector4f right, Vector4f dest) {
		if (dest == null)
			dest = new Vector4f();

		float x = left.m00 * right.x + left.m10 * right.y + left.m20 * right.z + left.m30 * right.w;
		float y = left.m01 * right.x + left.m11 * right.y + left.m21 * right.z + left.m31 * right.w;
		float z = left.m02 * right.x + left.m12 * right.y + left.m22 * right.z + left.m32 * right.w;
		float w = left.m03 * right.x + left.m13 * right.y + left.m23 * right.z + left.m33 * right.w;

		dest.x = x;
		dest.y = y;
		dest.z = z;
		dest.w = w;

		return dest;
	}
	
	/**
	 * Transform a Vector by a matrix and return the result in a destination
	 * vector.
	 * @param left The left matrix
	 * @param right The right vector
	 * @param dest The destination vector, or null if a new one is to be created
	 * @return the destination vector
	 */
	public static Vector3f transformCoord(Matrix4f left, ReadableVector3f right, Vector3f dest) {
		if (dest == null)
			dest = new Vector3f();

		final float rightx = right.getX();
		final float righty = right.getY();
		final float rightz = right.getZ();
		
		float x = left.m00 * rightx + left.m10 * righty + left.m20 * rightz + left.m30;
		float y = left.m01 * rightx + left.m11 * righty + left.m21 * rightz + left.m31;
		float z = left.m02 * rightx + left.m12 * righty + left.m22 * rightz + left.m32;
		float w = left.m03 * rightx + left.m13 * righty + left.m23 * rightz + left.m33;
		if(Math.abs(w) < 1.e-6f){
			w = 1.0f;
		}
		
		dest.set(x/w, y/w, z/w);
		return dest;
	}
	
	/**
	 * Transform a Vector by a matrix and return the result in a destination
	 * vector.
	 * @param left The left matrix
	 * @param right The right vector
	 * @param dest The destination vector, or null if a new one is to be created
	 * @return the destination vector
	 */
	@SuppressWarnings("unchecked")
	public static<T extends WritableVector3f> T transformCoord(Matrix4f left, ReadableVector3f right, WritableVector3f dest) {
		if (dest == null)
			dest = new Vector3f();

		final float rightx = right.getX();
		final float righty = right.getY();
		final float rightz = right.getZ();
		
		float x = left.m00 * rightx + left.m10 * righty + left.m20 * rightz + left.m30;
		float y = left.m01 * rightx + left.m11 * righty + left.m21 * rightz + left.m31;
		float z = left.m02 * rightx + left.m12 * righty + left.m22 * rightz + left.m32;
		float w = left.m03 * rightx + left.m13 * righty + left.m23 * rightz + left.m33;
		if(Math.abs(w) < 1.e-6f){
			w = 1.0f;
		}
		
		dest.set(x/w, y/w, z/w);
		return (T)dest;
	}
	
	/**
	 * Transform a Vector by a matrix and return the result in a destination
	 * vector.
	 * @param left The left matrix
	 * @param right The right vector
	 * @param dest The destination vector, or null if a new one is to be created
	 * @return the destination vector
	 */
	public static Vector3f transformVector(Matrix4f left, ReadableVector3f right, Vector3f dest) {
		if (dest == null)
			dest = new Vector3f();

		final float rightx = right.getX();
		final float righty = right.getY();
		final float rightz = right.getZ();
		
		float x = left.m00 * rightx + left.m10 * righty + left.m20 * rightz + left.m30;
		float y = left.m01 * rightx + left.m11 * righty + left.m21 * rightz + left.m31;
		float z = left.m02 * rightx + left.m12 * righty + left.m22 * rightz + left.m32;
//		float w = left.m03 * rightx + left.m13 * righty + left.m23 * rightz + left.m33;
//		if(Math.abs(w) < 1.e-6f){
//			w = 1.0f;
//		}
//		
//		dest.set(x/w, y/w, z/w);
		dest.set(x, y, z);
		return dest;
	}
	
	/**
	 * Transform a Vector by a matrix and return the result in a destination
	 * vector.
	 * @param left The left matrix
	 * @param right The right vector
	 * @param dest The destination vector, or null if a new one is to be created
	 * @return the destination vector
	 */
	public static WritableVector3f transformVector(Matrix4f left, ReadableVector3f right, WritableVector3f dest) {
		if (dest == null)
			dest = new Vector3f();

		final float rightx = right.getX();
		final float righty = right.getY();
		final float rightz = right.getZ();
		
		float x = left.m00 * rightx + left.m10 * righty + left.m20 * rightz + left.m30;
		float y = left.m01 * rightx + left.m11 * righty + left.m21 * rightz + left.m31;
		float z = left.m02 * rightx + left.m12 * righty + left.m22 * rightz + left.m32;

		dest.set(x, y, z);

		return dest;
	}
	
	/**
	 * Transform a Normal by a matrix and return the result in a destination
	 * vector.
	 * @param left The left matrix
	 * @param right The right vector
	 * @param dest The destination vector, or null if a new one is to be created
	 * @return the destination vector
	 */
	public static Vector3f transformNormal(Matrix4f left, ReadableVector3f right, Vector3f dest) {
		if (dest == null)
			dest = new Vector3f();

		final float rightx = right.getX();
		final float righty = right.getY();
		final float rightz = right.getZ();
		
		float x = left.m00 * rightx + left.m10 * righty + left.m20 * rightz;
		float y = left.m01 * rightx + left.m11 * righty + left.m21 * rightz;
		float z = left.m02 * rightx + left.m12 * righty + left.m22 * rightz;

		dest.set(x, y, z);

		return dest;
	}
	
	/**
	 * Transform a Normal by a matrix and return the result in a destination
	 * vector.
	 * @param left The left matrix
	 * @param right The right vector
	 * @param dest The destination vector, or null if a new one is to be created
	 * @return the destination vector
	 */
	public static WritableVector3f transformNormal(Matrix4f left, ReadableVector3f right, WritableVector3f dest) {
		if (dest == null)
			dest = new Vector3f();

		final float rightx = right.getX();
		final float righty = right.getY();
		final float rightz = right.getZ();
		
		float x = left.m00 * rightx + left.m10 * righty + left.m20 * rightz;
		float y = left.m01 * rightx + left.m11 * righty + left.m21 * rightz;
		float z = left.m02 * rightx + left.m12 * righty + left.m22 * rightz;

		dest.set(x, y, z);

		return dest;
	}

	/**
	 * Transpose this matrix
	 * @return this
	 */
	public Matrix4f transpose() {
		return transpose(this);
	}

	/**
	 * Translate this matrix
	 * @param vec The vector to translate by
	 * @return this
	 */
	public Matrix4f translate(Vector2f vec) {
		return translate(vec, this);
	}

	/**
	 * Translate this matrix
	 * @param vec The vector to translate by
	 * @return this
	 */
	public Matrix4f translate(ReadableVector3f vec) {
		return translate(vec.getX(), vec.getY(), vec.getZ(), this, this);
	}
	
	public Matrix4f translate(float vecx, float vecy, float vecz){
		return translate(vecx, vecy, vecz, this, this);
	}

	/**
	 * Scales this matrix
	 * @param vec The vector to scale by
	 * @return this
	 */
	public Matrix4f scale(ReadableVector3f vec) {
		return scale(vec, this, this);
	}

	/**
	 * Scales the source matrix and put the result in the destination matrix
	 * @param vec The vector to scale by
	 * @param src The source matrix
	 * @param dest The destination matrix, or null if a new matrix is to be created
	 * @return The scaled matrix
	 */
	public static Matrix4f scale(ReadableVector3f vec, Matrix4f src, Matrix4f dest) {
	    return scale(vec.getX(), vec.getY(), vec.getZ(), src, dest);
	}
	
	/**
	 * Scales this matrix
	 * @return this
	 */
	public Matrix4f scale(float scaleX, float scaleY, float scaleZ) {
		return scale(scaleX, scaleY, scaleZ, this, this);
	}
	
	/**
	 * Scales this matrix
	 * @return this
	 */
	public Matrix4f scale(float scaleXYZ){
		return scale(scaleXYZ, scaleXYZ, scaleXYZ, this, this);
	}

	/**
	 * Scales the source matrix and put the result in the destination matrix
	 * @param vecx The x component of vector to scale by
	 * @param vecy The y component of vector to scale by
	 * @param vecz The z component of vector to scale by
	 * @param src The source matrix
	 * @param dest The destination matrix, or null if a new matrix is to be created
	 * @return The scaled matrix
	 */
	public static Matrix4f scale(float vecx, float vecy, float vecz, Matrix4f src, Matrix4f dest) {
		if (dest == null)
			dest = new Matrix4f();
		dest.m00 = src.m00 * vecx;
		dest.m01 = src.m01 * vecx;
		dest.m02 = src.m02 * vecx;
		dest.m03 = src.m03 * vecx;
		dest.m10 = src.m10 * vecy;
		dest.m11 = src.m11 * vecy;
		dest.m12 = src.m12 * vecy;
		dest.m13 = src.m13 * vecy;
		dest.m20 = src.m20 * vecz;
		dest.m21 = src.m21 * vecz;
		dest.m22 = src.m22 * vecz;
		dest.m23 = src.m23 * vecz;
		return dest;
	}

	/**
	 * Rotates the matrix around the given axis the specified angle
	 * @param angle the angle, in radians.
	 * @param axis The vector representing the rotation axis. Must be normalized.
	 * @return this
	 */
	public Matrix4f rotate(float angle, ReadableVector3f axis) {
		return rotate(angle, axis, this);
	}
	
	/**
	 * Rotates the matrix around the given axis the specified angle
	 * @param angle the angle, in radians.
	 * @return this
	 */
	public Matrix4f rotate(float angle, float axisx, float axisy, float axisz) {
		return rotate(angle,axisx, axisy, axisz, this, this);
	}

	/**
	 * Rotates the matrix around the given axis the specified angle
	 * @param angle the angle, in radians.
	 * @param axis The vector representing the rotation axis. Must be normalized.
	 * @param dest The matrix to put the result, or null if a new matrix is to be created
	 * @return The rotated matrix
	 */
	public Matrix4f rotate(float angle, ReadableVector3f axis, Matrix4f dest) {
		return rotate(angle, axis, this, dest);
	}
	
	/**
	 * Rotates the matrix around the given axis the specified angle
	 * @param angle the angle, in radians.
	 * @param dest The matrix to put the result, or null if a new matrix is to be created
	 * @return The rotated matrix
	 */
	public Matrix4f rotate(float angle, float axisx, float axisy, float axisz, Matrix4f dest) {
		return rotate(angle, axisx, axisy, axisz, this, dest);
	}

	/**
	 * Rotates the source matrix around the given axis the specified angle and
	 * put the result in the destination matrix.
	 * @param angle the angle, in radians.
	 * @param axis The vector representing the rotation axis. Must be normalized.
	 * @param src The matrix to rotate
	 * @param dest The matrix to put the result, or null if a new matrix is to be created
	 * @return The rotated matrix
	 */
	public static Matrix4f rotate(float angle, ReadableVector3f axis, Matrix4f src, Matrix4f dest) {
		return rotate(angle, axis.getX(), axis.getY(), axis.getZ(), src, dest);
	}

	/**
	 * Rotates the source matrix around the given axis the specified angle and
	 * put the result in the destination matrix.
	 * @param angle the angle, in radians.
	 * @param axisx 
	 * @param axisy 
	 * @param axisz The vector representing the rotation axis. Must be normalized. 
	 * @param src The matrix to rotate
	 * @param dest The matrix to put the result, or null if a new matrix is to be created
	 * @return The rotated matrix
	 */
	public static Matrix4f rotate(float angle, float axisx, float axisy, float axisz, Matrix4f src, Matrix4f dest) {
		if (dest == null)
			dest = new Matrix4f();
		float c = (float) Math.cos(angle);
		float s = (float) Math.sin(angle);
		float oneminusc = 1.0f - c;
		float xy = axisx*axisy;
		float yz = axisy*axisz;
		float xz = axisx*axisz;
		float xs = axisx*s;
		float ys = axisy*s;
		float zs = axisz*s;

		float f00 = axisx*axisx*oneminusc+c;
		float f01 = xy*oneminusc+zs;
		float f02 = xz*oneminusc-ys;
		// n[3] not used
		float f10 = xy*oneminusc-zs;
		float f11 = axisy*axisy*oneminusc+c;
		float f12 = yz*oneminusc+xs;
		// n[7] not used
		float f20 = xz*oneminusc+ys;
		float f21 = yz*oneminusc-xs;
		float f22 = axisz*axisz*oneminusc+c;

		float t00 = src.m00 * f00 + src.m10 * f01 + src.m20 * f02;
		float t01 = src.m01 * f00 + src.m11 * f01 + src.m21 * f02;
		float t02 = src.m02 * f00 + src.m12 * f01 + src.m22 * f02;
		float t03 = src.m03 * f00 + src.m13 * f01 + src.m23 * f02;
		float t10 = src.m00 * f10 + src.m10 * f11 + src.m20 * f12;
		float t11 = src.m01 * f10 + src.m11 * f11 + src.m21 * f12;
		float t12 = src.m02 * f10 + src.m12 * f11 + src.m22 * f12;
		float t13 = src.m03 * f10 + src.m13 * f11 + src.m23 * f12;
		dest.m20 = src.m00 * f20 + src.m10 * f21 + src.m20 * f22;
		dest.m21 = src.m01 * f20 + src.m11 * f21 + src.m21 * f22;
		dest.m22 = src.m02 * f20 + src.m12 * f21 + src.m22 * f22;
		dest.m23 = src.m03 * f20 + src.m13 * f21 + src.m23 * f22;
		dest.m00 = t00;
		dest.m01 = t01;
		dest.m02 = t02;
		dest.m03 = t03;
		dest.m10 = t10;
		dest.m11 = t11;
		dest.m12 = t12;
		dest.m13 = t13;
		return dest;
	}
	/**
	 * Translate this matrix and stash the result in another matrix
	 * @param vec The vector to translate by
	 * @param dest The destination matrix or null if a new matrix is to be created
	 * @return the translated matrix
	 */
	public Matrix4f translate(Vector3f vec, Matrix4f dest) {
		return translate(vec, this, dest);
	}

	/**
	 * Translate the source matrix and stash the result in the destination matrix
	 * @param vec The vector to translate by
	 * @param src The source matrix
	 * @param dest The destination matrix or null if a new matrix is to be created
	 * @return The translated matrix
	 */
	public static Matrix4f translate(ReadableVector3f vec, Matrix4f src, Matrix4f dest) {
		return translate(vec.getX(), vec.getY(), vec.getZ(), src, dest);
	}
	
	/**
	 * Translate the source matrix and stash the result in the destination matrix
	 * @param src The source matrix
	 * @param dest The destination matrix or null if a new matrix is to be created
	 * @return The translated matrix
	 */
	public static Matrix4f translate(float vecx, float vecy, float vecz, Matrix4f src, Matrix4f dest) {
		if (dest == null)
			dest = new Matrix4f();

		dest.m30 += src.m00 * vecx + src.m10 * vecy + src.m20 * vecz;
		dest.m31 += src.m01 * vecx + src.m11 * vecy + src.m21 * vecz;
		dest.m32 += src.m02 * vecx + src.m12 * vecy + src.m22 * vecz;
		dest.m33 += src.m03 * vecx + src.m13 * vecy + src.m23 * vecz;

		return dest;
	}

	/**
	 * Translate this matrix and stash the result in another matrix
	 * @param vec The vector to translate by
	 * @param dest The destination matrix or null if a new matrix is to be created
	 * @return the translated matrix
	 */
	public Matrix4f translate(Vector2f vec, Matrix4f dest) {
		return translate(vec, this, dest);
	}

	/**
	 * Translate the source matrix and stash the result in the destination matrix
	 * @param vec The vector to translate by
	 * @param src The source matrix
	 * @param dest The destination matrix or null if a new matrix is to be created
	 * @return The translated matrix
	 */
	public static Matrix4f translate(Vector2f vec, Matrix4f src, Matrix4f dest) {
		if (dest == null)
			dest = new Matrix4f();

		dest.m30 += src.m00 * vec.x + src.m10 * vec.y;
		dest.m31 += src.m01 * vec.x + src.m11 * vec.y;
		dest.m32 += src.m02 * vec.x + src.m12 * vec.y;
		dest.m33 += src.m03 * vec.x + src.m13 * vec.y;

		return dest;
	}

	/**
	 * Transpose this matrix and place the result in another matrix
	 * @param dest The destination matrix or null if a new matrix is to be created
	 * @return the transposed matrix
	 */
	public Matrix4f transpose(Matrix4f dest) {
		return transpose(this, dest);
	}

	/**
	 * Transpose the source matrix and place the result in the destination matrix
	 * @param src The source matrix
	 * @param dest The destination matrix or null if a new matrix is to be created
	 * @return the transposed matrix
	 */
	public static Matrix4f transpose(Matrix4f src, Matrix4f dest) {
		if (dest == null)
		   dest = new Matrix4f();
		float m00 = src.m00;
		float m01 = src.m10;
		float m02 = src.m20;
		float m03 = src.m30;
		float m10 = src.m01;
		float m11 = src.m11;
		float m12 = src.m21;
		float m13 = src.m31;
		float m20 = src.m02;
		float m21 = src.m12;
		float m22 = src.m22;
		float m23 = src.m32;
		float m30 = src.m03;
		float m31 = src.m13;
		float m32 = src.m23;
		float m33 = src.m33;

		dest.m00 = m00;
		dest.m01 = m01;
		dest.m02 = m02;
		dest.m03 = m03;
		dest.m10 = m10;
		dest.m11 = m11;
		dest.m12 = m12;
		dest.m13 = m13;
		dest.m20 = m20;
		dest.m21 = m21;
		dest.m22 = m22;
		dest.m23 = m23;
		dest.m30 = m30;
		dest.m31 = m31;
		dest.m32 = m32;
		dest.m33 = m33;

		return dest;
	}

	/**
	 * @return the determinant of the matrix
	 */
	public float determinant() {
		float f =
			m00
				* ((m11 * m22 * m33 + m12 * m23 * m31 + m13 * m21 * m32)
					- m13 * m22 * m31
					- m11 * m23 * m32
					- m12 * m21 * m33);
		f -= m01
			* ((m10 * m22 * m33 + m12 * m23 * m30 + m13 * m20 * m32)
				- m13 * m22 * m30
				- m10 * m23 * m32
				- m12 * m20 * m33);
		f += m02
			* ((m10 * m21 * m33 + m11 * m23 * m30 + m13 * m20 * m31)
				- m13 * m21 * m30
				- m10 * m23 * m31
				- m11 * m20 * m33);
		f -= m03
			* ((m10 * m21 * m32 + m11 * m22 * m30 + m12 * m20 * m31)
				- m12 * m21 * m30
				- m10 * m22 * m31
				- m11 * m20 * m32);
		return f;
	}

	/**
	 * Calculate the determinant of a 3x3 matrix
	 * @return result
	 */

	private static float determinant3x3(float t00, float t01, float t02,
				     float t10, float t11, float t12,
				     float t20, float t21, float t22)
	{
		return   t00 * (t11 * t22 - t12 * t21)
		       + t01 * (t12 * t20 - t10 * t22)
		       + t02 * (t10 * t21 - t11 * t20);
	}

	/**
	 * Invert this matrix
	 * @return this if successful, null otherwise
	 */
	public Matrix4f invert() {
		return invert(this, this);
	}

	/**
	 * Invert the source matrix and put the result in the destination
	 * @param src The source matrix
	 * @param dest The destination matrix, or null if a new matrix is to be created
	 * @return The inverted matrix if successful, null otherwise
	 */
	public static Matrix4f invert(Matrix4f src, Matrix4f dest) {
		float determinant = src.determinant();

		if (determinant != 0) {
			/*
			 * m00 m01 m02 m03
			 * m10 m11 m12 m13
			 * m20 m21 m22 m23
			 * m30 m31 m32 m33
			 */
			if (dest == null)
				dest = new Matrix4f();
			float determinant_inv = 1f/determinant;

			// first row
			float t00 =  determinant3x3(src.m11, src.m12, src.m13, src.m21, src.m22, src.m23, src.m31, src.m32, src.m33);
			float t01 = -determinant3x3(src.m10, src.m12, src.m13, src.m20, src.m22, src.m23, src.m30, src.m32, src.m33);
			float t02 =  determinant3x3(src.m10, src.m11, src.m13, src.m20, src.m21, src.m23, src.m30, src.m31, src.m33);
			float t03 = -determinant3x3(src.m10, src.m11, src.m12, src.m20, src.m21, src.m22, src.m30, src.m31, src.m32);
			// second row
			float t10 = -determinant3x3(src.m01, src.m02, src.m03, src.m21, src.m22, src.m23, src.m31, src.m32, src.m33);
			float t11 =  determinant3x3(src.m00, src.m02, src.m03, src.m20, src.m22, src.m23, src.m30, src.m32, src.m33);
			float t12 = -determinant3x3(src.m00, src.m01, src.m03, src.m20, src.m21, src.m23, src.m30, src.m31, src.m33);
			float t13 =  determinant3x3(src.m00, src.m01, src.m02, src.m20, src.m21, src.m22, src.m30, src.m31, src.m32);
			// third row
			float t20 =  determinant3x3(src.m01, src.m02, src.m03, src.m11, src.m12, src.m13, src.m31, src.m32, src.m33);
			float t21 = -determinant3x3(src.m00, src.m02, src.m03, src.m10, src.m12, src.m13, src.m30, src.m32, src.m33);
			float t22 =  determinant3x3(src.m00, src.m01, src.m03, src.m10, src.m11, src.m13, src.m30, src.m31, src.m33);
			float t23 = -determinant3x3(src.m00, src.m01, src.m02, src.m10, src.m11, src.m12, src.m30, src.m31, src.m32);
			// fourth row
			float t30 = -determinant3x3(src.m01, src.m02, src.m03, src.m11, src.m12, src.m13, src.m21, src.m22, src.m23);
			float t31 =  determinant3x3(src.m00, src.m02, src.m03, src.m10, src.m12, src.m13, src.m20, src.m22, src.m23);
			float t32 = -determinant3x3(src.m00, src.m01, src.m03, src.m10, src.m11, src.m13, src.m20, src.m21, src.m23);
			float t33 =  determinant3x3(src.m00, src.m01, src.m02, src.m10, src.m11, src.m12, src.m20, src.m21, src.m22);

			// transpose and divide by the determinant
			dest.m00 = t00*determinant_inv;
			dest.m11 = t11*determinant_inv;
			dest.m22 = t22*determinant_inv;
			dest.m33 = t33*determinant_inv;
			dest.m01 = t10*determinant_inv;
			dest.m10 = t01*determinant_inv;
			dest.m20 = t02*determinant_inv;
			dest.m02 = t20*determinant_inv;
			dest.m12 = t21*determinant_inv;
			dest.m21 = t12*determinant_inv;
			dest.m03 = t30*determinant_inv;
			dest.m30 = t03*determinant_inv;
			dest.m13 = t31*determinant_inv;
			dest.m31 = t13*determinant_inv;
			dest.m32 = t23*determinant_inv;
			dest.m23 = t32*determinant_inv;
			return dest;
		} else
			return null;
	}

	/**
	 * Negate this matrix
	 * @return this
	 */
	public Matrix4f negate() {
		return negate(this);
	}

	/**
	 * Negate this matrix and place the result in a destination matrix.
	 * @param dest The destination matrix, or null if a new matrix is to be created
	 * @return the negated matrix
	 */
	public Matrix4f negate(Matrix4f dest) {
		return negate(this, dest);
	}

	/**
	 * Negate this matrix and place the result in a destination matrix.
	 * @param src The source matrix
	 * @param dest The destination matrix, or null if a new matrix is to be created
	 * @return The negated matrix
	 */
	public static Matrix4f negate(Matrix4f src, Matrix4f dest) {
		if (dest == null)
			dest = new Matrix4f();

		dest.m00 = -src.m00;
		dest.m01 = -src.m01;
		dest.m02 = -src.m02;
		dest.m03 = -src.m03;
		dest.m10 = -src.m10;
		dest.m11 = -src.m11;
		dest.m12 = -src.m12;
		dest.m13 = -src.m13;
		dest.m20 = -src.m20;
		dest.m21 = -src.m21;
		dest.m22 = -src.m22;
		dest.m23 = -src.m23;
		dest.m30 = -src.m30;
		dest.m31 = -src.m31;
		dest.m32 = -src.m32;
		dest.m33 = -src.m33;

		return dest;
	}
	
	public Matrix4f load(float[] arr, int offset) {
		m00 = arr[offset++];
		m01 = arr[offset++];
		m02 = arr[offset++];
		m03 = arr[offset++];
		m10 = arr[offset++];
		m11 = arr[offset++];
		m12 = arr[offset++];
		m13 = arr[offset++];
		m20 = arr[offset++];
		m21 = arr[offset++];
		m22 = arr[offset++];
		m23 = arr[offset++];
		m30 = arr[offset++];
		m31 = arr[offset++];
		m32 = arr[offset++];
		m33 = arr[offset++];
		return this;
	}
	public Matrix4f load(float[][] arr) {
		m00 = arr[0][0];
		m01 = arr[0][1];
		m02 = arr[0][2];
		m03 = arr[0][3];
		m10 = arr[1][0];
		m11 = arr[1][1];
		m12 = arr[1][2];
		m13 = arr[1][3];
		m20 = arr[2][0];
		m21 = arr[2][1];
		m22 = arr[2][2];
		m23 = arr[2][3];
		m30 = arr[3][0];
		m31 = arr[3][1];
		m32 = arr[3][2];
		m33 = arr[3][3];
		return this;
	}
	public Matrix4f loadTranspose(float[] arr, int offset) {
		m00 = arr[offset++];
		m10 = arr[offset++];
		m20 = arr[offset++];
		m30 = arr[offset++];
		m01 = arr[offset++];
		m11 = arr[offset++];
		m21 = arr[offset++];
		m31 = arr[offset++];
		m02 = arr[offset++];
		m12 = arr[offset++];
		m22 = arr[offset++];
		m32 = arr[offset++];
		m03 = arr[offset++];
		m13 = arr[offset++];
		m23 = arr[offset++];
		m33 = arr[offset++];
		return this;
	}
	public Matrix4f loadTranspose(float[][] arr) {
		m00 = arr[0][0];
		m10 = arr[0][1];
		m20 = arr[0][2];
		m30 = arr[0][3];
		m01 = arr[1][0];
		m11 = arr[1][1];
		m21 = arr[1][2];
		m31 = arr[1][3];
		m02 = arr[2][0];
		m12 = arr[2][1];
		m22 = arr[2][2];
		m32 = arr[2][3];
		m03 = arr[3][0];
		m13 = arr[3][1];
		m23 = arr[3][2];
		m33 = arr[3][3];
		return this;
	}
	public float[] store(float[] arr, int offset) {
		arr[offset++] = m00;
		arr[offset++] = m01;
		arr[offset++] = m02;
		arr[offset++] = m03;
		arr[offset++] = m10;
		arr[offset++] = m11;
		arr[offset++] = m12;
		arr[offset++] = m13;
		arr[offset++] = m20;
		arr[offset++] = m21;
		arr[offset++] = m22;
		arr[offset++] = m23;
		arr[offset++] = m30;
		arr[offset++] = m31;
		arr[offset++] = m32;
		arr[offset++] = m33;
		return arr;
	}
	public float[] storeTranspose(float[] arr, int offset) {
		arr[offset++] = m00;
		arr[offset++] = m10;
		arr[offset++] = m20;
		arr[offset++] = m30;
		arr[offset++] = m01;
		arr[offset++] = m11;
		arr[offset++] = m21;
		arr[offset++] = m31;
		arr[offset++] = m02;
		arr[offset++] = m12;
		arr[offset++] = m22;
		arr[offset++] = m32;
		arr[offset++] = m03;
		arr[offset++] = m13;
		arr[offset++] = m23;
		arr[offset++] = m33;
		return arr;
	}
	public float[][] store(float[][] arr) {
		arr[0][0] = m00;
		arr[0][1] = m01;
		arr[0][2] = m02;
		arr[0][3] = m03;
		arr[1][0] = m10;
		arr[1][1] = m11;
		arr[1][2] = m12;
		arr[1][3] = m13;
		arr[2][0] = m20;
		arr[2][1] = m21;
		arr[2][2] = m22;
		arr[2][3] = m23;
		arr[3][0] = m30;
		arr[3][1] = m31;
		arr[3][2] = m32;
		arr[3][3] = m33;
		return arr;
	}
	public float[][] storeTranspose(float[][] arr) {
		arr[0][0] = m00;
		arr[0][1] = m10;
		arr[0][2] = m20;
		arr[0][3] = m30;
		arr[1][0] = m01;
		arr[1][1] = m11;
		arr[1][2] = m21;
		arr[1][3] = m31;
		arr[2][0] = m02;
		arr[2][1] = m12;
		arr[2][2] = m22;
		arr[2][3] = m32;
		arr[3][0] = m03;
		arr[3][1] = m13;
		arr[3][2] = m23;
		arr[3][3] = m33;
		return arr;
	}
	
	/**
	 * The "eye" values specify a location in 3-space for the viewpoint. The 鈥渃enter鈥�values
	 * must specify a different location so that the view direction is toward the center location. The
	 * 鈥渦p鈥�values specify an upward direction for the y-axis of the viewer. It is not necessary
	 * for the 鈥渦p鈥�vector to be orthogonal to the vector from the eye to the center, but it must not be
	 * parallel to it. The <code>gluLookAt</code> command should be used when the current matrix is the model
	 * view matrix, not the projection matrix. This is because the viewer should always be placed at
	 * the origin in order for OpenGL lighting to work properly.
	 */
	public static final Matrix4f lookAt(ReadableVector3f eye, ReadableVector3f center, ReadableVector3f up, Matrix4f out) {
		return lookAt(eye.getX(), eye.getY(), eye.getZ(), center.getX(), center.getY(), center.getZ(), up.getX(), up.getY(), up.getZ(), out);
	}
	
	/**
	 * The three "eye" values specify a location in 3-space for the viewpoint. The three 鈥渃enter鈥�values
	 * must specify a different location so that the view direction is toward the center location. The
	 * three 鈥渦p鈥�values specify an upward direction for the y-axis of the viewer. It is not necessary
	 * for the 鈥渦p鈥�vector to be orthogonal to the vector from the eye to the center, but it must not be
	 * parallel to it. The <code>gluLookAt</code> command should be used when the current matrix is the model
	 * view matrix, not the projection matrix. This is because the viewer should always be placed at
	 * the origin in order for OpenGL lighting to work properly.
	 *
	 * @param eyex
	 * @param eyey
	 * @param eyez
	 * @param centerx
	 * @param centery
	 * @param centerz
	 * @param upx
	 * @param upy
	 * @param upz
	 * @param out
	 * @return
	 */
	public static final Matrix4f lookAt(float eyex, float eyey, float eyez, float centerx, float centery, float centerz, float upx, float upy, float upz, Matrix4f out){
		if(out == null) out = new Matrix4f();
		else out.setIdentity();
		
		float fx = centerx - eyex;
		float fy = centery - eyey;
		float fz = centerz - eyez;
		
		// normalize f
		double lenSqu = lenSquared(fx, fy, fz);
		if(lenSqu != 1.0 && lenSqu != 0.0){
			double invLen = lenInvert(lenSqu);
			
			fx *= invLen;
			fy *= invLen;
			fz *= invLen;
		}
		
		// normalize up
		lenSqu = lenSquared(upx, upy, upz);
		if(lenSqu != 1.0 && lenSqu != 0.0){
			double invLen = lenInvert(lenSqu);
			
			upx *= invLen;
			upy *= invLen;
			upz *= invLen;
		}
		
		// the cross product of the f and up
		float sx = fy * upz - fz * upy;
		float sy = upx * fz - upz * fx;
		float sz = fx * upy - fy * upx;
		
		// normalize s
		lenSqu = lenSquared(sx, sy, sz);
		if(lenSqu != 1.0 && lenSqu != 0.0){
			double invLen = lenInvert(lenSqu);
			
			sx *= invLen;
			sy *= invLen;
			sz *= invLen;
		}
		
		// the cross product of the s and f
		float ux = sy * fz - sz * fy;
		float uy = fx * sz - fz * sx;
		float uz = sx * fy - sy * fx;
		
		out.m00 = sx;
		out.m10 = sy;
		out.m20 = sz;

		out.m01 = ux;
		out.m11 = uy;
		out.m21 = uz;

		out.m02 = -fx;
		out.m12 = -fy;
		out.m22 = -fz;

		out.translate(-eyex, -eyey, -eyez);
		return out;
	}
	
	public static final Matrix4f lookAtLH(ReadableVector3f eye, ReadableVector3f center, ReadableVector3f up, Matrix4f out) {
		return lookAtLH(eye.getX(), eye.getY(), eye.getZ(), center.getX(), center.getY(), center.getZ(), up.getX(), up.getY(), up.getZ(), out);
	}
	
	public static final Matrix4f lookAtLH(float eyex, float eyey, float eyez, float centerx, float centery, float centerz, float upx, float upy, float upz, Matrix4f out){
		if(out == null)
			out = new Matrix4f();
		
		float fx = centerx - eyex;
		float fy = centery - eyey;
		float fz = centerz - eyez;
		
		// normalize f
		double lenSqu = lenSquared(fx, fy, fz);
		if(lenSqu != 1.0 && lenSqu != 0.0){
			double invLen = lenInvert(lenSqu);
			
			fx *= invLen;
			fy *= invLen;
			fz *= invLen;
		}
		
		// the cross product of the up and f
		float sx = upy * fz - upz * fy;
		float sy = upz * fx - upx * fz;
		float sz = upx * fy - upy * fx;
		
		// normalize s
		lenSqu = lenSquared(sx, sy, sz);
		if(lenSqu != 1.0 && lenSqu != 0.0){
			double invLen = lenInvert(lenSqu);
			
			sx *= invLen;
			sy *= invLen;
			sz *= invLen;
		}
		
		// the cross product of the f and s
		float ux = fy * sz - fz * sy;
		float uy = fz * sx - fx * sz;
		float uz = fx * sy - fy * sx;
		
		float tx = sx * eyex + sy * eyey + sz * eyez;
		float ty = ux * eyex + uy * eyey + uz * eyez;
		float tz = fx * eyex + fy * eyey + fz * eyez;
		
		out.set(
			sx, ux, fx, 0,
			sy, uy, fy, 0,
			sz, uz, fz, 0,
			-tx, -ty, -tz, 1);
		
		return out;
	}
	
	private static double lenSquared(float x, float y, float z){
		return x * x + y * y + z * z;
	}
	
	private static double lenInvert(double lenSqu){
		double v = 1.0/Math.sqrt(lenSqu);
		if(v != v) return 0.0;
		else return v;
	}
	
	public static final Matrix4f perspectiveLH(float fov, float aspect,float zNear, float zFar, Matrix4f out){
		float r = (float) Math.toRadians(fov / 2);
		float deltaZ = zFar - zNear;
		float s = (float) Math.sin(r);
		float cotangent = 0;

		if (deltaZ == 0 || s == 0 || aspect == 0) {
			return out;
		}

		if (out == null)
			out = new Matrix4f();

		// cos(r) / sin(r) = cot(r)
		cotangent = (float) Math.cos(r) / s;

		out.setIdentity();
		out.m00 = cotangent / aspect;
		out.m11 = cotangent;
		out.m22 = zFar / deltaZ;
		out.m23 = 1;
		out.m32 = -zNear * zFar / deltaZ;
		out.m33 = 0;
		
		return out;
	}

	public static final Matrix4f perspective(float fov, float aspect,float zNear, float zFar, Matrix4f out) {
		/*
		float r = (float) Math.toRadians(fov / 2);
		float deltaZ = zFar - zNear;
		float s = (float) Math.sin(r);
		float cotangent = 0;

		if (deltaZ == 0 || s == 0 || aspect == 0) {
			return out;
		}

		if (out == null)
			out = new Matrix4f();

		// cos(r) / sin(r) = cot(r)
		cotangent = (float) Math.cos(r) / s;

		out.setIdentity();
		out.m00 = cotangent / aspect;
		out.m11 = cotangent;
		out.m22 = -(zFar + zNear) / deltaZ;
		out.m23 = -1;
		out.m32 = -2 * zNear * zFar / deltaZ;
		out.m33 = 0;
		*/
		
		float xmin, xmax, ymin, ymax;
		double r = Math.toRadians(fov / 2);
		ymax = (float) (zNear * Math.tan(r));
		ymin = -ymax;

		xmin = ymin * aspect;
		xmax = ymax * aspect;
		return frustum(xmin, xmax, ymin, ymax, zNear, zFar, out);
	}

	public static final Matrix4f ortho(float width, float height, float near, float far, Matrix4f out) {
		return ortho(-width*0.5f, width*0.5f, -height*0.5f, height*0.5f, near, far, out);
	}
	
	public static final Matrix4f ortho(float left, float right, float bottom,
			float top, float near, float far, Matrix4f out) {
		float tx = -((right + left) / (right - left));
		float ty = -((top + bottom) / (top - bottom));
		float tz = -((far + near) / (far - near));

		if (out == null)
			out = new Matrix4f();
		else
			out.setIdentity();

		out.m00 = 2 / (right - left);
		out.m11 = 2 / (top - bottom);
		out.m22 = -2 / (far - near);
		out.m30 = tx;
		out.m31 = ty;
		out.m32 = tz;

		return out;
	}

	/**
	 * Builds a left-handed perspective projection matrix
	 * @param width  Width of the view volume at the near view-plane.
	 * @param height Height of the view volume at the near view-plane.
	 * @param znear Z-value of the near view-plane.
	 * @param zfar Z-value of the far view-plane.
	 * @param out The result of this operation.
	 * @return
	 */
	public static final Matrix4f frustum(float width, float height, float znear,float zfar, Matrix4f out){
		float ymax = height / 2.0f;
	    float ymin = -ymax;

	    float aspect = width / height;
	    float xmin = ymin * aspect;
	    float xmax = ymax * aspect;
	    
	    return frustum(xmin, xmax, ymin, ymax, znear, zfar, out);
	}
	
	public static final Matrix4f frustum(float left, float right, float bottom,
			float top, float znear, float zfar, Matrix4f out) {
		float n2 = 2 * znear;
		float w = right - left;
		float h = top - bottom;
		float d = zfar - znear;

		// out.set(n2 / w, 0, (right + left) / w, 0,
		// 0, n2 / h, (top + bottom) / h, 0,
		// 0, 0, -(zfar + znear) / d, -(n2 * zfar) / d,
		// 0, 0, -1, 0);

		if (out == null)
			out = new Matrix4f();

		out.m00 = n2 / w;
		out.m01 = 0;
		out.m02 = 0;
		out.m03 = 0;
		out.m10 = 0;
		out.m11 = n2 / h;
		out.m12 = 0;
		out.m13 = 0;
		out.m20 = (right + left) / w;
		out.m21 = (top + bottom) / h;
		out.m22 = -(zfar + znear) / d;
		out.m23 = -1;
		out.m30 = 0;
		out.m31 = 0;
		out.m32 = -(n2 * zfar) / d;
		out.m33 = 0;

		return out;
	}
	
	public static final Vector4f[] extractFrustumPlanes(Matrix4f mat, Vector4f[] dest){
		if(dest == null) dest = new Vector4f[6];
		
		// left
		if(dest[0] == null) dest[0] = new Vector4f();
		dest[0].x = mat.m03 + mat.m00;
		dest[0].y = mat.m13 + mat.m10;
		dest[0].z = mat.m23 + mat.m20;
		dest[0].w = mat.m33 + mat.m30;
		
		// right
		if(dest[1] == null) dest[1] = new Vector4f();
		dest[1].x = mat.m03 - mat.m00;
		dest[1].y = mat.m13 - mat.m10;
		dest[1].z = mat.m23 - mat.m20;
		dest[1].w = mat.m33 - mat.m30;
		
		// bottom
		if(dest[2] == null) dest[2] = new Vector4f();
		dest[2].x = mat.m03 + mat.m01;
		dest[2].y = mat.m13 + mat.m11;
		dest[2].z = mat.m23 + mat.m21;
		dest[2].w = mat.m33 + mat.m31;
		
		// top
		if(dest[3] == null) dest[3] = new Vector4f();
		dest[3].x = mat.m03 - mat.m01;
		dest[3].y = mat.m13 - mat.m11;
		dest[3].z = mat.m23 - mat.m21;
		dest[3].w = mat.m33 - mat.m31;
		
		// near
		if(dest[4] == null) dest[4] = new Vector4f();
		dest[4].x = mat.m03 + mat.m02;
		dest[4].y = mat.m13 + mat.m12;
		dest[4].z = mat.m23 + mat.m22;
		dest[4].w = mat.m33 + mat.m32;
		
		// far
		if(dest[5] == null) dest[5] = new Vector4f();
		dest[5].x = mat.m03 - mat.m02;
		dest[5].y = mat.m13 - mat.m12;
		dest[5].z = mat.m23 - mat.m22;
		dest[5].w = mat.m33 - mat.m32;
		
		// Normalize the plane equations.
		for(int i = 0; i < 6; i++){
			dest[i].normalise();
		}
		
		return dest;
	}
	
	public static final Matrix4f frustumInfinite(float left, float right, float bottom,
			float top, float znear, float zfar, Matrix4f out) {
		float n2 = 2 * znear;
		float w = right - left;
		float h = top - bottom;

		// out.set(n2 / w, 0, (right + left) / w, 0,
		// 0, n2 / h, (top + bottom) / h, 0,
		// 0, 0, -(zfar + znear) / d, -(n2 * zfar) / d,
		// 0, 0, -1, 0);

		if (out == null)
			out = new Matrix4f();

		out.m00 = n2 / w;
		out.m01 = 0;
		out.m02 = 0;
		out.m03 = 0;
		out.m10 = 0;
		out.m11 = n2 / h;
		out.m12 = 0;
		out.m13 = 0;
		out.m20 = (right + left) / w;
		out.m21 = (top + bottom) / h;
		out.m22 = -1;
		out.m23 = -1;
		out.m30 = 0;
		out.m31 = 0;
		out.m32 = -n2;
		out.m33 = 0;

		return out;
	}
	
	/**
	 * The <code>obliqueClipping</code> function modifies the standard OpenGL perspective 
	 * projection matrix so that the near plane of the view frustum is moved to coincide 
	 * with a given arbitrary plane specified by the clipPlane parameter.
     *
	 * @param projection the source projection
	 * @param clipPlane 
	 * @param dest
	 * @return
	 */
	public static Matrix4f obliqueClipping(Matrix4f projection, Vector4f clipPlane, Matrix4f dest){
		if(dest == null) dest = new Matrix4f();
		
		// Calculate the clip-space corner point opposite the clipping plane
		// using Equation (5.64) and transform it into camera space by
		// multiplying it by the inverse of the projection matrix.
		float x = (Math.signum(clipPlane.x) + projection.m20) / projection.m00;
		float y = (Math.signum(clipPlane.y) + projection.m21) / projection.m11;
		float z = -1.0F;
		float w = (1.0F + projection.m22) / projection.m32;

		
		// Calculate the scaled plane vector using Equation (5.68)
		// and replace the third row of the projection matrix.
        
		float dot = clipPlane.x * x + clipPlane.y * y + clipPlane.z * z + clipPlane.w * w;
		x = clipPlane.x * 2.0f/dot;
		y = clipPlane.y * 2.0f/dot;
		z = clipPlane.z * 2.0f/dot;
		w = clipPlane.w * 2.0f/dot;
		
		if(dest != projection) 
			dest.load(projection);
		dest.m02 = x;
		dest.m12 = y;
		dest.m22 = z + 1.0f;
		dest.m32 = w;
		return dest;
	}
	
	public static void decompseMatrix(Matrix4f src, Vector3f origin, Vector3f xAxis, Vector3f yAxis, Vector3f zAxis){
		Matrix4f invert = Matrix4f.invert(src, null);
		if(origin != null){
			origin.set(invert.m30, invert.m31, invert.m32);
		}
		
		if(xAxis != null){
			xAxis.set(invert.m00, invert.m01, invert.m02);
		}
		
		if(yAxis != null){
			yAxis.set(invert.m10, invert.m11, invert.m12);
		}
		
		if(zAxis != null){
			zAxis.set(invert.m20, invert.m21, invert.m22);
		}
	}
	
	/** Decompose a transform into the (eye, center, up) form. The up didn't normalize*/
	public static void decompseMatrix(Matrix4f src, Vector3f eye, Vector3f center, Vector3f up){
		Matrix4f invert = Matrix4f.invert(src, null);
		if(eye != null){
			eye.set(invert.m30, invert.m31, invert.m32);
		}
		
		if(center != null){
			center.set(invert.m30 - invert.m20, invert.m31 - invert.m21, invert.m32 - invert.m22);
		}
		
		if(up != null){
			up.set(invert.m10, invert.m11, invert.m12);
		}
	}
	
	public static void decompseRigidMatrix(Matrix4f src, Vector3f eye, Vector3f center, Vector3f up){
		float m10 = src.m01;
		float m11 = src.m11;
		float m12 = src.m21;
		float m20 = src.m02;
		float m21 = src.m12;
		float m22 = src.m22;
		float m30 = -(src.m30 * src.m00) - (src.m31 * src.m01) - (src.m32 * src.m02);
		float m31 = -(src.m30 * src.m10) - (src.m31 * src.m11) - (src.m32 * src.m12);
		float m32 = -(src.m30 * src.m20) - (src.m31 * src.m21) - (src.m32 * src.m22);
		
		if(eye != null){
			eye.set(m30, m31, m32);
		}
		
		if(center != null){
			center.set(m30 - m20, m31 - m21, m32 - m22);
		}
		
		if(up != null){
			up.set(m10, m11, m12);
		}
	}
	
	public static void decompseRigidMatrix(Matrix4f src, Vector3f origin, Vector3f xAxis, Vector3f yAxis, Vector3f zAxis){
		float m00 = src.m00;
		float m01 = src.m10;
		float m02 = src.m20;
		float m10 = src.m01;
		float m11 = src.m11;
		float m12 = src.m21;
		float m20 = src.m02;
		float m21 = src.m12;
		float m22 = src.m22;
		float m30 = -(src.m30 * src.m00) - (src.m31 * src.m01) - (src.m32 * src.m02);
		float m31 = -(src.m30 * src.m10) - (src.m31 * src.m11) - (src.m32 * src.m12);
		float m32 = -(src.m30 * src.m20) - (src.m31 * src.m21) - (src.m32 * src.m22);
		
		if(origin != null){
			origin.set(m30, m31, m32);
		}
		
		if(xAxis != null){
			xAxis.set(m00, m01, m02);
		}
		
		if(yAxis != null){
			yAxis.set(m10, m11, m12);
		}
		
		if(zAxis != null){
			zAxis.set(m20, m21, m22);
		}
	}
	
	/**
	 * Invert the rigid matrix.
	 */
	public static Matrix4f invertRigid(Matrix4f src, Matrix4f dest) {
		if(dest == null)
			dest = new Matrix4f();
		
		float m00 = src.m00;
		float m01 = src.m10;
		float m02 = src.m20;
		float m10 = src.m01;
		float m11 = src.m11;
		float m12 = src.m21;
		float m20 = src.m02;
		float m21 = src.m12;
		float m22 = src.m22;
		float m30 = -(src.m30 * src.m00) - (src.m31 * src.m01) - (src.m32 * src.m02);
		float m31 = -(src.m30 * src.m10) - (src.m31 * src.m11) - (src.m32 * src.m12);
		float m32 = -(src.m30 * src.m20) - (src.m31 * src.m21) - (src.m32 * src.m22);
		float m33 = 1.0f;

		dest.m00 = m00;
		dest.m01 = m01;
		dest.m02 = m02;
		dest.m03 = 0;
		dest.m10 = m10;
		dest.m11 = m11;
		dest.m12 = m12;
		dest.m13 = 0;
		dest.m20 = m20;
		dest.m21 = m21;
		dest.m22 = m22;
		dest.m23 = 0;
		dest.m30 = m30;
		dest.m31 = m31;
		dest.m32 = m32;
		dest.m33 = m33;
		
		return dest;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Float.floatToIntBits(m00);
		result = prime * result + Float.floatToIntBits(m01);
		result = prime * result + Float.floatToIntBits(m02);
		result = prime * result + Float.floatToIntBits(m03);
		result = prime * result + Float.floatToIntBits(m10);
		result = prime * result + Float.floatToIntBits(m11);
		result = prime * result + Float.floatToIntBits(m12);
		result = prime * result + Float.floatToIntBits(m13);
		result = prime * result + Float.floatToIntBits(m20);
		result = prime * result + Float.floatToIntBits(m21);
		result = prime * result + Float.floatToIntBits(m22);
		result = prime * result + Float.floatToIntBits(m23);
		result = prime * result + Float.floatToIntBits(m30);
		result = prime * result + Float.floatToIntBits(m31);
		result = prime * result + Float.floatToIntBits(m32);
		result = prime * result + Float.floatToIntBits(m33);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		Matrix4f other = (Matrix4f) obj;
		if (Float.floatToIntBits(m00) != Float.floatToIntBits(other.m00))
			return false;
		if (Float.floatToIntBits(m01) != Float.floatToIntBits(other.m01))
			return false;
		if (Float.floatToIntBits(m02) != Float.floatToIntBits(other.m02))
			return false;
		if (Float.floatToIntBits(m03) != Float.floatToIntBits(other.m03))
			return false;
		if (Float.floatToIntBits(m10) != Float.floatToIntBits(other.m10))
			return false;
		if (Float.floatToIntBits(m11) != Float.floatToIntBits(other.m11))
			return false;
		if (Float.floatToIntBits(m12) != Float.floatToIntBits(other.m12))
			return false;
		if (Float.floatToIntBits(m13) != Float.floatToIntBits(other.m13))
			return false;
		if (Float.floatToIntBits(m20) != Float.floatToIntBits(other.m20))
			return false;
		if (Float.floatToIntBits(m21) != Float.floatToIntBits(other.m21))
			return false;
		if (Float.floatToIntBits(m22) != Float.floatToIntBits(other.m22))
			return false;
		if (Float.floatToIntBits(m23) != Float.floatToIntBits(other.m23))
			return false;
		if (Float.floatToIntBits(m30) != Float.floatToIntBits(other.m30))
			return false;
		if (Float.floatToIntBits(m31) != Float.floatToIntBits(other.m31))
			return false;
		if (Float.floatToIntBits(m32) != Float.floatToIntBits(other.m32))
			return false;
		if (Float.floatToIntBits(m33) != Float.floatToIntBits(other.m33))
			return false;
		return true;
	}
	
	/** Extract the normal matrix from the modelview matrix.<p>
	 *  More details in the book <p><i>Mathematics for 3D.Game Programming and Computer Graphics Lengyel,.3ed,.Course,.2012</i></p> on the pages 78-80. */
	public static Matrix3f getNormalMatrix(Matrix4f modelView, Matrix3f dest){
		dest = modelView.store(dest);
		dest.invert();
		dest.transpose();
		
		return dest;
	}

	/** Extract the normal matrix from the modelview matrix.<p>
	 *  More details in the book <p><i>Mathematics for 3D.Game Programming and Computer Graphics Lengyel,.3ed,.Course,.2012</i></p> on the pages 78-80. */
	public static Matrix4f getNormalMatrix(Matrix4f modelView, Matrix4f dest){
		if(dest == null)
			dest = new Matrix4f();

		dest.load(modelView);
		dest.m30 = dest.m31 = dest.m32 = 0;
		dest.invert();
		dest.transpose();

		return dest;
	}
	
	/**
	 * Convert a rotation matrix back into a unit rotation vector <i>axis</i> and a rotation angle.
	 * @param rotateMat the source rotation matrix 
	 * @param axis  a unit vector store the rotation axis, if the matrix represent a zero rotation, the axis will not changed.
	 * @return the radiant formed angle of the rotation matrix about the axis. It's rangle in [-pi, pi].
	 */
	public static float getRotatedAxisAndAngle(Matrix4f rotateMat, WritableVector3f axis){
		double alpha = (rotateMat.m00 + rotateMat.m11 + rotateMat.m22 - 1) *0.5f;
		float m21 = rotateMat.m12 - rotateMat.m21;
		float m20 = rotateMat.m20 - rotateMat.m02;
		float m10 = rotateMat.m01 - rotateMat.m10;
		
		if(Math.abs(alpha - 1) < 0.0001){ // the rotated angle tend to be 0.
			float betaSure = m21 * m21 + m20 * m20 + m10 * m10;
			if(betaSure == 0){ // 2sin^2(theta) = 0;
				return 0;
			}else{
				double beta = Math.sqrt(betaSure);
				
				if(axis != null){
					float x = (float) (m21/beta);
					float y = (float) (m20/beta);
					float z = (float) (m10/beta);
					
					axis.set(x, y, z);
				}
				
				return (float) Math.atan2(beta, alpha * 2);
			}
		}else{
			double angle = Math.acos(alpha);
			
			if(axis != null){
				double s2 = 2 * Math.sin(angle);
				
				float x = (float) (m21/s2);
				float y = (float) (m20/s2);
				float z = (float) (m10/s2);
				
				axis.set(x, y, z);
			}
			
			return (float)angle;
		}
		
	}
	
	public float get(int index, boolean columnMajor) {
		switch (index) {
		case 0: return m00;
		case 1: return columnMajor ? m01 : m10;
		case 2: return columnMajor ? m02 : m20;
		case 3: return columnMajor ? m03 : m30;
		case 4: return columnMajor ? m10 : m01;
		case 5: return m11;
		case 6: return columnMajor ? m12 : m21;
		case 7: return columnMajor ? m13 : m31;
		case 8: return columnMajor ? m20 : m02;
		case 9: return columnMajor ? m21 : m12;
		case 10: return m22;
		case 11: return columnMajor ? m23 : m32;
		case 12: return columnMajor ? m30 : m03;
		case 13: return columnMajor ? m31 : m13;
		case 14: return columnMajor ? m32 : m23;
		case 15: return m33;
		default:
			throw new IndexOutOfBoundsException("index = " + index);
		}
	}

	public float set(int index, float v, boolean columnMajor) {
		float t;
		if(columnMajor){
			switch (index) {
			 case 0: t = m00; m00 = v; return t;
			 case 1: t = m01; m01 = v; return t;
			 case 2: t = m02; m02 = v; return t;
			 case 3: t = m03; m03 = v; return t;
			 case 4: t = m10; m10 = v; return t;
			 case 5: t = m11; m11 = v; return t;
			 case 6: t = m12; m12 = v; return t;
			 case 7: t = m13; m13 = v; return t;
			 case 8: t = m20; m20 = v; return t;
			 case 9: t = m21; m21 = v; return t;
			 case 10: t = m22; m22 = v; return t;
			 case 11: t = m23; m23 = v; return t;
			 case 12: t = m30; m30 = v; return t;
			 case 13: t = m31; m31 = v; return t;
			 case 14: t = m32; m32 = v; return t;
			 case 15: t = m33; m33 = v; return t;
			default:
				throw new IndexOutOfBoundsException("index = " + index);
			}
		}else{
			switch (index) {
			 case 0: t = m00; m00 = v; return t;
			 case 1: t = m10; m10 = v; return t;
			 case 2: t = m20; m20 = v; return t;
			 case 3: t = m30; m30 = v; return t;
			 case 4: t = m01; m01 = v; return t;
			 case 5: t = m11; m11 = v; return t;
			 case 6: t = m21; m21 = v; return t;
			 case 7: t = m31; m31 = v; return t;
			 case 8: t = m02; m02 = v; return t;
			 case 9: t = m12; m12 = v; return t;
			 case 10: t = m22; m22 = v; return t;
			 case 11: t = m32; m32 = v; return t;
			 case 12: t = m03; m03 = v; return t;
			 case 13: t = m13; m13 = v; return t;
			 case 14: t = m23; m23 = v; return t;
			 case 15: t = m33; m33 = v; return t;
			default:
				throw new IndexOutOfBoundsException("index = " + index);
			}
		}
	}

	public float get(int i, int j, boolean columnMajor) {
		switch(i){
		case 0: 
			switch(j){
			case 0: return m00;
			case 1: return columnMajor ? m01 : m10;
			case 2: return columnMajor ? m02 : m20;
			case 3: return columnMajor ? m03 : m30;
			default:
				throw new IndexOutOfBoundsException("j = " + j);
			}
		case 1: 
			switch(j){
			case 0: return columnMajor ? m10 : m01;
			case 1: return m11;
			case 2: return columnMajor ? m12 : m21;
			case 3: return columnMajor ? m13 : m31;
			default:
				throw new IndexOutOfBoundsException("j = " + j);
			}
		case 2: 
			switch(j){
			case 0: return columnMajor ? m20 : m02;
			case 1: return columnMajor ? m21 : m12;
			case 2: return m22;
			case 3: return columnMajor ? m23 : m32;
			default:
				throw new IndexOutOfBoundsException("j = " + j);
			}
		case 3: 
			switch(j){
			case 0: return columnMajor ? m30 : m03;
			case 1: return columnMajor ? m31 : m13;
			case 2: return columnMajor ? m32 : m23;
			case 3: return m33;
			default:
				throw new IndexOutOfBoundsException("j = " + j);
			}
		default:
			throw new IndexOutOfBoundsException("i = " + i);
		}
	}

	public float set(int i, int j, float v, boolean columnMajor) {
		float t;
		if(columnMajor){
			switch (i) {
			case 0: 
				switch(j){
				case 0: t = m00; m00 = v; return t;
				case 1: t = m01; m01 = v; return t;
				case 2: t = m02; m02 = v; return t;
				case 3: t = m03; m03 = v; return t;
				default:
					throw new IndexOutOfBoundsException("j = " + j);
				}
			case 1: 
				switch(j){
				case 0: t = m10; m10 = v; return t;
				case 1: t = m11; m11 = v; return t;
				case 2: t = m12; m12 = v; return t;
				case 3: t = m13; m13 = v; return t;
				default:
					throw new IndexOutOfBoundsException("j = " + j);
				}
			case 2: 
				switch(j){
				case 0: t = m20; m20 = v; return t;
				case 1: t = m21; m21 = v; return t;
				case 2: t = m22; m22 = v; return t;
				case 3: t = m23; m23 = v; return t;
				default:
					throw new IndexOutOfBoundsException("j = " + j);
				}
			case 3: 
				switch(j){
				case 0: t = m30; m30 = v; return t;
				case 1: t = m31; m31 = v; return t;
				case 2: t = m32; m32 = v; return t;
				case 3: t = m33; m33 = v; return t;
				default:
					throw new IndexOutOfBoundsException("j = " + j);
				}
			default:
				throw new IndexOutOfBoundsException("i = " + i);
			}
		}else{
			switch (i) {
			case 0: 
				switch(j){
				case 0: t = m00; m00 = v; return t;
				case 1: t = m10; m10 = v; return t;
				case 2: t = m20; m20 = v; return t;
				case 3: t = m30; m30 = v; return t;
				default:
					throw new IndexOutOfBoundsException("j = " + j);
				}
			case 1: 
				switch(j){
				case 0: t = m01; m01 = v; return t;
				case 1: t = m11; m11 = v; return t;
				case 2: t = m21; m21 = v; return t;
				case 3: t = m31; m31 = v; return t;
				default:
					throw new IndexOutOfBoundsException("j = " + j);
				}
			case 2: 
				switch(j){
				case 0: t = m02; m02 = v; return t;
				case 1: t = m12; m12 = v; return t;
				case 2: t = m22; m22 = v; return t;
				case 3: t = m32; m32 = v; return t;
				default:
					throw new IndexOutOfBoundsException("j = " + j);
				}
			case 3: 
				switch(j){
				case 0: t = m03; m03 = v; return t;
				case 1: t = m13; m13 = v; return t;
				case 2: t = m23; m23 = v; return t;
				case 3: t = m33; m33 = v; return t;
				default:
					throw new IndexOutOfBoundsException("j = " + j);
				}
			default:
				throw new IndexOutOfBoundsException("i = " + i);
			}
		}
	}

	public void setColumn(int index, float[] v, int offset){
		setColumn(index, v[offset+0], v[offset+1], v[offset+2], v[offset+3]);
	}
	
	public void setColumn(int index, ReadableVector4f v){
		setColumn(index, v.getX(), v.getY(), v.getZ(), v.getW());
	}
	
	public void setColumn(int index, float x, float y, float z, float w){
		switch (index) {
		case 0: m00 = x; m01 = y; m02 = z; m03 = w; break;
		case 1: m10 = x; m11 = y; m12 = z; m13 = w; break;
		case 2: m20 = x; m21 = y; m22 = z; m23 = w; break;
		case 3: m30 = x; m31 = y; m32 = z; m33 = w; break;
		default:
			throw new IndexOutOfBoundsException("index = " + index);
		}
	}
	
	public WritableVector4f getColumn(int index, WritableVector4f dest){
		if(dest == null) dest = new Vector4f();
		
		switch (index) {
		case 0: dest.set(m00, m01, m02, m03); break;
		case 1: dest.set(m10, m11, m12, m13); break;
		case 2: dest.set(m20, m21, m22, m23); break;
		case 3: dest.set(m30, m31, m32, m33); break;
		default:
			throw new IndexOutOfBoundsException("index = " + index);
		}
		
		return dest;
	}

	public void setRow(int index, float[] v, int offset){
		setRow(index, v[offset+0], v[offset+1], v[offset+2], v[offset+3]);
	}
	
	public void setRow(int index, ReadableVector4f v){
		setRow(index, v.getX(), v.getY(), v.getZ(), v.getW());
	}
	
	public void setRow(int index, float x, float y, float z, float w){
		switch (index) {
		case 0: m00 = x; m10 = y; m20 = z; m30 = w; break;
		case 1: m01 = x; m11 = y; m21 = z; m31 = w; break;
		case 2: m02 = x; m12 = y; m22 = z; m32 = w; break;
		case 3: m03 = x; m13 = y; m23 = z; m33 = w; break;
		default:
			throw new IndexOutOfBoundsException("index = " + index);
		}
	}
	
	public WritableVector4f getRow(int index, WritableVector4f dest){
		if(dest == null) dest = new Vector4f();
		
		switch (index) {
		case 0: dest.set(m00, m10, m20, m30); break;
		case 1: dest.set(m01, m11, m21, m31); break;
		case 2: dest.set(m02, m12, m22, m32); break;
		case 3: dest.set(m03, m13, m23, m33); break;
		default:
			throw new IndexOutOfBoundsException("index = " + index);
		}
		
		return dest;
	}

	public static Vector3f  decomposeRotationYawPitchRoll(Matrix4f mat, /*float & yaw, float & pitch, float & roll*/ Vector3f ypr )
	{
		//pitch = (float)vaMath::ASin( -m[2][1] ); 
		float pitch = (float)Math.asin( -/*m[0][2]*/mat.m20 );
		float roll, yaw;
		float threshold = 0.001f;

		float test = (float)Math.cos( pitch );
		
		if( test > threshold )
		{

			//roll = (float)Math.atan2( m[0][1], m[1][1] ); 
			roll = (float)Math.atan2( /*m[1][2], m[2][2]*/mat.m21, mat.m22 );

			//yaw = (float)Math.atan2( m[2][0], m[2][2] );
			yaw = (float)Math.atan2( /*m[0][1], m[0][0]*/mat.m10, mat.m00 );

		}
		else
		{
			//roll = (float)Math.atan2( -m[1][0], m[0][0] ); 
			roll = (float)Math.atan2( /*-m[2][1], m[1][1]*/-mat.m12, mat.m11 );
			yaw = 0.0f;
		}

		if(ypr != null){
			ypr.set(yaw, pitch, roll);
		}else{
			ypr = new Vector3f(yaw, pitch, roll);
		}

		return ypr;
	}
	
	/**
	 * Rotation matrix creation. From euler angles:<ol>
	 * <li> Yaw around Y axis in radians
	 * <li> Pitch around X axis in radians
	 * <li> Roll around Z axis in radians
	 * </ol>
	 * return the rotation matrix [R] = [Roll].[Pitch].[Yaw]
	 * @param yaw
	 * @param pitch
	 * @param roll
	 * @param result hold the dest matrix4f.
	 * @return
	 */
	public static Matrix4f rotationYawPitchRoll(float yaw, float pitch, float roll, Matrix4f result){
		if(result == null)
			result = new Matrix4f();
		else
			result.setIdentity();
		
//		if(roll != 0)
//			result.rotate(roll, Vector3f.Z_AXIS);
//		
//		if(pitch != 0)
//			result.rotate(pitch, Vector3f.X_AXIS);
//		
//		if(yaw != 0)
//			result.rotate(yaw, Vector3f.Y_AXIS);
		
		double cx = Math.cos(pitch);
		double sx = Math.sin(pitch);
		double cy = Math.cos(yaw);
		double sy = Math.sin(yaw);
		double cz = Math.cos(roll);
		double sz = Math.sin(roll);

		double cxsy = cx * sy;
		double sxsy = sx * sy;

		result.m00  = (float) (cy * cz);
		result.m01  = (float) (-cy * sz);
		result.m02  = (float) -sy;
		result.m10  = (float) (-sxsy * cz + cx * sz);
		result.m11  = (float) (sxsy * sz + cx * cz);
		result.m12  = (float) (-sx * cy);
		result.m20  = (float) (cxsy * cz + sx * sz);
		result.m21  = (float) (-cxsy * sz + sx * cz);
		result.m22 =  (float) (cx * cy);

		result.transpose();
		
		return result;
	}

	public static boolean decompose(Matrix4f transform, WritableVector3f outScale, WritableVector4f outRotation, WritableVector3f outTranslation ){
		// Translation
		/*outTranslation.x = this->m[3][0];
		outTranslation.y = this->m[3][1];
		outTranslation.z = this->m[3][2];*/
		if(outTranslation != null)
			outTranslation.set(transform.m30, transform.m31, transform.m32);

		// Scaling
		float x = transform.m00;
		float y = transform.m01;
		float z = transform.m02;
//		outScale.x = vec.Length( );
		float scaleX = Vector3f.length(x,y,z);
		if(scaleX == 0.0f) return false;
		if(outScale != null) outScale.setX(scaleX);

		x = transform.m10;
		y = transform.m11;
		z = transform.m12;
//		outScale.y = vec.Length( );
		float scaleY = Vector3f.length(x,y,z);
		if(scaleY == 0.0f) return false;
		if(outScale != null) outScale.setY(scaleY);

		x = transform.m20;
		y = transform.m21;
		z = transform.m22;
//		outScale.z = vec.Length( );
		float scaleZ = Vector3f.length(x,y,z);
		if(scaleZ == 0.0f) return false;
		if(outScale != null) outScale.setZ(scaleZ);

		/*normalized.m[0][1] = this->m[0][1] / outScale.x;
		normalized.m[0][2] = this->m[0][2] / outScale.x;
		normalized.m[1][0] = this->m[1][0] / outScale.y;
		normalized.m[1][1] = this->m[1][1] / outScale.y;
		normalized.m[1][2] = this->m[1][2] / outScale.y;
		normalized.m[2][0] = this->m[2][0] / outScale.z;
		normalized.m[2][1] = this->m[2][1] / outScale.z;
		normalized.m[2][2] = this->m[2][2] / outScale.z;*/

		if(outRotation != null) {
			float m00 = transform.m00 / scaleX;
			float m01 = transform.m01 / scaleX;
			float m02 = transform.m02 / scaleX;
			float m10 = transform.m10 / scaleY;
			float m11 = transform.m11 / scaleY;
			float m12 = transform.m12 / scaleY;
			float m20 = transform.m20 / scaleZ;
			float m21 = transform.m21 / scaleZ;
			float m22 = transform.m22 / scaleZ;

			Quaternion.fromRotationMat(m00, m01, m02, m10, m11, m12, m20, m21, m22, outRotation);
		}

		return true;
	}
}
