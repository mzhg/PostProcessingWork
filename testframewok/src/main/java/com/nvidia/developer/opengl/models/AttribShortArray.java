package com.nvidia.developer.opengl.models;

import org.lwjgl.util.vector.Vector;

import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.util.Arrays;

import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.util.Numeric;

public class AttribShortArray extends AttribArray  implements IndicesAttrib{
	short[] data;

	public AttribShortArray(int cmpSize) {
		super(cmpSize);
		data = Numeric.EMPTY_SHORT;
	}

	public AttribShortArray(int cmpSize, int capacity){
		super(cmpSize);
		data = new short[cmpSize * capacity];
	}

	public void add(short x){ add(x, (short)0, (short)0, (short)1);}
	public void add(short x, short y){ add(x, y, (short)0, (short)1);}
	public void add(short x, short y, short z){ add(x, y, z, (short)1);}
	
	public void add(short x, short y, short z, short w){
		checkCapacity(cmpSize);
		
		data[size++] = x;
		if(cmpSize > 1) data[size++] = y;
		if(cmpSize > 2) data[size++] = z;
		if(cmpSize > 3) data[size++] = w;
	}

	@Override
	public void add(int indice) { add((short)indice, (short)0, (short)0, (short)1);}
	@Override
	public void set(int index, int indice) { set(index, (short)indice, (short)0, (short)0, (short)1);}

	public void set(int index, short x){ set(index, x, (short)0, (short)0, (short)1);}
	public void set(int index, short x, short y){ set(index, x, y, (short)0, (short)1);}
	public void set(int index, short x, short y, short z){ set(index, x, y, z, (short)1);}
	
	public void set(int index, short x, short y, short z, short w){
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
			ShortBuffer fb = buf.asShortBuffer();
			fb.put(data, 0, size);
			buf.position(buf.position() + getByteSize());
		}else{
			for(int i = 0; i < size; i++){
				buf.putShort(data[i]);
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
	public int getType() { return GLenum.GL_SHORT;}

	@Override
	public void store(int index, ByteBuffer buf) {
		index = index * cmpSize;
		for(int i = 0;i < cmpSize; i++)
			buf.putInt(data[index + i]);
	}
	
	public short[] getArray() { return data;}

}
