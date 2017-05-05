package com.nvidia.developer.opengl.models;

import org.lwjgl.util.vector.Vector;

import java.nio.ByteBuffer;
import java.util.Arrays;

import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.util.Numeric;

public class AttribByteArray extends AttribArray{
	byte[] data;
	
	public AttribByteArray(int cmpSize) {
		super(cmpSize);
		data = Numeric.EMPTY_BYTE;
	}
	
	public AttribByteArray(int cmpSize, int capacity){
		super(cmpSize);
		data = new byte[cmpSize * capacity];
	}
	
	public void add(byte x){ add(x, (byte)0, (byte)0, (byte)1);}
	public void add(byte x, byte y){ add(x, y, (byte)0, (byte)1);}
	public void add(byte x, byte y, byte z){ add(x, y, z, (byte)1);}
	
	public void add(byte x, byte y, byte z, byte w){
		checkCapacity(cmpSize);
		
		data[size++] = x;
		if(cmpSize > 1) data[size++] = y;
		if(cmpSize > 2) data[size++] = z;
		if(cmpSize > 3) data[size++] = w;
	}
	
	public void set(int index, byte x){ set(index, x, (byte)0, (byte)0, (byte)1);}
	public void set(int index, byte x, byte y){ set(index, x, y, (byte)0, (byte)1);}
	public void set(int index, byte x, byte y, byte z){ set(index, x, y, z, (byte)1);}
	
	public void set(int index, byte x, byte y, byte z, byte w){
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
	public int getByteSize() { return size;}

	@Override
	public void store(ByteBuffer buf) {
		buf.put(data, 0, size);
	}
	
	public byte get(byte index){
		index *= cmpSize;
		if(index >= size)
			throw new IndexOutOfBoundsException();
		return data[index];
	}
	
	public void get(byte index, Vector dest){
		index *= cmpSize;
		if(index >= size)
			throw new IndexOutOfBoundsException();
		
		for(byte i = 0; i < cmpSize; i++){
			dest.setValue(i, data[index++]);
		}
	}
	
	@Override
	public int getType() { return GLenum.GL_BYTE;}

	@Override
	public void store(int index, ByteBuffer buf) {
		index = index * cmpSize;
		buf.put(data, index, cmpSize);
	}
	
	public byte[] getArray() { return data;}

}
