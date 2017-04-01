package org.lwjgl.util.vector;

import java.nio.FloatBuffer;

public interface WritableVector extends Writable{

	/**
	 * Specify the value to the element at the specified index.
	 * @param index
	 * @param v
	 */
	void setValue(int index, float v) throws IndexOutOfBoundsException;
	
	/**
	 * Load this vector from a FloatBuffer
	 * @param buf The buffer to load it from, at the current position
	 * @return this
	 */
	WritableVector load(FloatBuffer buf);
	
	
	
	/**
	 * Load this vector from a float array
	 * @param arr The buffer to load it from, at the current position
	 * @param offset the position of the array to read.
	 * @return this
	 */
	WritableVector load(float[] arr, int offset);
	
	/**
	 * Return the component count of the element.
	 */
	int getCount();
}
