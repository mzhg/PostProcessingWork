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

import java.nio.FloatBuffer;

/**
 * @author foo
 */
public interface ReadableVector extends Readable{
	/**
	 * @return the length of the vector
	 */
	float length();
	/**
	 * @return the length squared of the vector
	 */
	float lengthSquared();
	/**
	 * Store this vector in a FloatBuffer
	 * @param buf The buffer to store it in, at the current position
	 * @return this
	 */
	FloatBuffer store(FloatBuffer buf);
	
	/**
	 * Store this vector in a float array
	 * @param arr The buffer to store it in, at the current position
	 * @param offset The position of the array to write.
	 * @return this
	 */
	float[] store(float[] arr, int offset);
	
	/**
	 * Return the element at the specified location by <i>index</i>.<ul>
	 * <li> index = 0, the x component will be returned.
	 * <li> index = 1, the y component will be returned.
	 * <li> index = 2, the z component will be returned.
	 * <li> index = 3, the w component will be returned.
	 * </ul>
	 * @param index the component correspond index.
	 * @return
	 * @throws IndexOutOfBoundsException if index < 0 or index >= {@link #getCount()}.
	 */
	float get(int index) throws IndexOutOfBoundsException;
	
	/**
	 * Return the component count of the element.
	 */
	int getCount();
	
	boolean isZero();
	
	boolean isOne();
	
	boolean isNaN();
}