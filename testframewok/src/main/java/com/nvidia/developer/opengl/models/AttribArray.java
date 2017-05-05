package com.nvidia.developer.opengl.models;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class AttribArray extends Attribute{

	private static final AtomicInteger id_generator = new AtomicInteger();
	
	final int cmpSize; // can be 1, 2, 3, 4;
	int size;  // the element count.
	int modified;  // The context data weather changed.
	int divisor;   // for multi-instance
	private final int uniqueID;
	
	public AttribArray(int cmpSize) {
		if(cmpSize < 1 || cmpSize > 4)
			throw new IllegalArgumentException("cmpSize must be 1, 2, 3, or 4.");
		this.cmpSize = cmpSize;
		uniqueID = id_generator.getAndIncrement();
	}
	
	public final int getUniqueID() { return uniqueID;}
	
	public void setDivisor(int divisor) { this.divisor = divisor;}
	public int getDivisor() { return divisor;}
	
	public int getCmpSize() { return cmpSize; };
	/** Get the number of the elements. */
	public int getSize() { return size/cmpSize;}
	
	/** Return the byte size of the <code>AttribArray</code> */
	public abstract int getByteSize();
	/** Put the data into the given <i>buf</i> */
	public abstract void store(ByteBuffer buf);
	
	public boolean isEmpty() { return size == 0;}
	/** Return the type of data correspond to OpenGL.(i.e GL_FLOAT, GL_INT) */
	public abstract int getType();
	/** Store the single element into the <i>buf</i> at the given index specfied by the <i>index</i> */
	public abstract void store(int index, ByteBuffer buf);
	
	public abstract void resize(int size);
	
	/** Increase the value of the {@link #modified} by 1. Sub-class should call this method when the attribute data has changed. */
	protected final void touch() { modified ++;}
	
}
