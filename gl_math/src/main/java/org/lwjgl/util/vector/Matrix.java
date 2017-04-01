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
import java.nio.FloatBuffer;

/**
 *
 * Base class for matrices. When a matrix is constructed it will be the identity
 * matrix unless otherwise stated.
 *
 * @author cix_foo <cix_foo@users.sourceforge.net>
 * @version $Revision$
 * $Id$
 */
public abstract class Matrix implements Serializable, Readable {

	private static final long serialVersionUID = 6364896114057633239L;


	/**
	 * Constructor for Matrix.
	 */
	protected Matrix() {
		super();
	}

	/**
	 * Set this matrix to be the identity matrix.
	 * @return this
	 */
	public abstract Matrix setIdentity();


	/**
	 * Invert this matrix
	 * @return this
	 */
	public abstract Matrix invert();


	/**
	 * Load from a float buffer. The buffer stores the matrix in column major
	 * (OpenGL) order.
	 *
	 * @param buf A float buffer to read from
	 * @return this
	 */
	public abstract Matrix load(FloatBuffer buf);
	
	/**
	 * Load from a float array. The array stores the matrix in column major
	 * (OpenGL) order.
	 *
	 * @param arr A float array to read from
	 * @param offset The position to start read.
	 * @return this
	 */
	public abstract Matrix load(float[] arr, int offset);
	
	/**
	 * Load from a two-dimension float array. The array stores the matrix in column major
	 * (OpenGL) order.
	 *
	 * @param arr A float array to read from
	 * @return this
	 */
	public abstract Matrix load(float[][] arr);


	/**
	 * Load from a float buffer. The buffer stores the matrix in row major
	 * (mathematical) order.
	 *
	 * @param buf A float buffer to read from
	 * @return this
	 */
	public abstract Matrix loadTranspose(FloatBuffer buf);
	
	/**
	 * Load from a float array. The array stores the matrix in row major
	 * (mathematical) order.
	 *
	 * @param arr A float array to read from
	 * @param offset The position to start read.
	 * @return this
	 */
	public abstract Matrix loadTranspose(float[] arr, int offset);
	
	/**
	 * Load from a two-dimension float array. The array stores the matrix in row major
	 * (mathematical) order.
	 *
	 * @param arr A float array to read from
	 * @return this
	 */
	public abstract Matrix loadTranspose(float[][] arr);

	/**
	 * Negate this matrix
	 * @return this
	 */
	public abstract Matrix negate();


	/**
	 * Store this matrix in a float buffer. The matrix is stored in column
	 * major (openGL) order.
	 * @param buf The buffer to store this matrix in
	 * @return The buf
	 */
	public abstract FloatBuffer store(FloatBuffer buf);
	
	/**
	 * Store this matrix in a float array. The matrix is stored in column
	 * major (openGL) order.
	 * @param arr The array to store this matrix in
	 * @param offset The position to start write
	 * @return The arr
	 */
	public abstract float[] store(float[] arr, int offset);

	/**
	 * Store this matrix in a float array. The matrix is stored in column
	 * major (openGL) order.
	 * @param arr The array to store this matrix in
	 * @return The arr
	 */
    public abstract float[][] store(float[][] arr);
    
	/**
	 * Store this matrix in a float buffer. The matrix is stored in row
	 * major (maths) order.
	 * @param buf The buffer to store this matrix in
	 * @return The buf
	 */
	public abstract FloatBuffer storeTranspose(FloatBuffer buf);

	/**
	 * Store this matrix in a float array. The matrix is stored in row
	 * major (maths) order.
	 * @param arr The array to store this matrix in
	 * @param offset The position to start write
	 * @return The arr
	 */
    public abstract float[] storeTranspose(float[] arr, int offset);
    
    /**
	 * Store this matrix in a float array. The matrix is stored in row
	 * major (maths) order.
	 * @param arr The array to store this matrix in
	 * @return this
	 */
    public abstract float[][] storeTranspose(float[][] arr);
	/**
	 * Transpose this matrix
	 * @return this
	 */
	public abstract Matrix transpose();


	/**
	 * Set this matrix to 0.
	 * @return this
	 */
	public abstract Matrix setZero();


	/**
	 * @return the determinant of the matrix
	 */
	public abstract float determinant();

	/**
	 * Return the element at the specified location by <i>index</i> in the column_major form.
	 * Call the method is equal to call the <b>get(int, true)</b>.
	 * @param index
	 * @return
	 * @throws IndexOutOfBoundsException if the <i>index</i> beyond the range of the matrix.
	 * @see #get(int, boolean)
	 */
    public float get(int index){
    	return get(index, true);
    }
    
    /**
     * Return the element at the specified location by <i>index</i> with the specified major by <i>columnMajor</i>
     * @param index
     * @param columnMajor
     * @return
     * @throws IndexOutOfBoundsException if the <i>index</i> beyond the range of the matrix.
     */
    public abstract float get(int index, boolean columnMajor);
    
    public float set(int index, float v){
    	return set(index, v, true);
    }
    
    public abstract float set(int index, float v, boolean columnMajor);
    
    public float get(int col, int row){
    	return get(col, row, true);
    }
    
    public abstract float get(int i, int j, boolean columnMajor);
    
    public float set(int col, int row, float v){
    	return set(col, row, v, false);
    }
    
    public abstract float set(int i, int j, float v, boolean columnMajor);
}
