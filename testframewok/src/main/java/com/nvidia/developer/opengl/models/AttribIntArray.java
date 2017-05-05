package com.nvidia.developer.opengl.models;

import org.lwjgl.util.vector.Vector;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Arrays;

import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.util.Numeric;

public class AttribIntArray extends AttribArray{
	int[] data;
	
	public AttribIntArray(int cmpSize) {
		super(cmpSize);
		data = Numeric.EMPTY_INT;
	}
	
	public AttribIntArray(int cmpSize, int capacity){
		super(cmpSize);
		data = new int[cmpSize * capacity];
	}
	
	public void add(int x){ add(x, 0, 0, 1);}
	public void add(int x, int y){ add(x, y, 0, 1);}
	public void add(int x, int y, int z){ add(x, y, z, 1);}
	
	public void add(int x, int y, int z, int w){
		checkCapacity(cmpSize);
		
		data[size++] = x;
		if(cmpSize > 1) data[size++] = y;
		if(cmpSize > 2) data[size++] = z;
		if(cmpSize > 3) data[size++] = w;
	}
	
	public void set(int index, int x){ set(index, x, 0, 0, 1);}
	public void set(int index, int x, int y){ set(index, x, y, 0, 1);}
	public void set(int index, int x, int y, int z){ set(index, x, y, z, 1);}
	
	public void set(int index, int x, int y, int z, int w){
		index *= cmpSize;
		if(index >= size)
			throw new IndexOutOfBoundsException();
		
		data[index++] = x;
		if(cmpSize > 1) data[index++] = y;
		if(cmpSize > 2) data[index++] = z;
		if(cmpSize > 3) data[index++] = w;
	}
	
	private void checkCapacity(int capacity){
		if(data.length - size < capacity){
			int newCapacity = Math.max(64, data.length << 1);
			while(newCapacity < capacity){
				newCapacity <<= 1;
				if(newCapacity < 0){ // overflow.
					newCapacity = capacity;
				}
			}
			data = Arrays.copyOf(data, newCapacity);
		}
	}
	
	@Override
	public void resize(int size) {
		if(size < 0)
			throw new IllegalArgumentException("The newSize less than 0");
		
		ensureCapacity(size);
		this.size = size * cmpSize;
	}
	
	public void ensureCapacity(int capacity){
		capacity *= cmpSize;
		if(data.length < capacity){
			data = Arrays.copyOf(data, capacity);
		}
	}

	@Override
	public int getByteSize() { return size << 2;}

	@Override
	public void store(ByteBuffer buf) {
		if(buf.isDirect() && size > 10){
			IntBuffer fb = buf.asIntBuffer();
			fb.put(data, 0, size);
			buf.position(buf.position() + getByteSize());
		}else{
			for(int i = 0; i < size; i++){
				buf.putFloat(data[i]);
			}
		}
	}
	
	public int get(int index){
		index *= cmpSize;
		if(index >= size)
			throw new IndexOutOfBoundsException();
		return data[index];
	}
	
	public void get(int index, Vector dest){
		index *= cmpSize;
		if(index >= size)
			throw new IndexOutOfBoundsException();
		
		for(int i = 0; i < cmpSize; i++){
			dest.setValue(i, data[index++]);
		}
	}
	
	@Override
	public int getType() { return GLenum.GL_INT;}

	@Override
	public void store(int index, ByteBuffer buf) {
		index = index * cmpSize;
		for(int i = 0;i < cmpSize; i++)
			buf.putInt(data[index + i]);
	}
	
	public int[] getArray() { return data;}

}
