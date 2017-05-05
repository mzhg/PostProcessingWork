package com.nvidia.developer.opengl.models;

import org.lwjgl.util.vector.Vector;
import org.lwjgl.util.vector.Vector3f;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.Arrays;

import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.util.Numeric;


public class AttribFloatArray extends AttribArray{
	float[] data;
	
	public AttribFloatArray(int cmpSize) {
		super(cmpSize);
		data = Numeric.EMPTY_FLOAT;
	}
	
	public AttribFloatArray(int cmpSize, int capacity){
		super(cmpSize);
		data = new float[cmpSize * capacity];
	}
	
	public void add(float x){ add(x, 0, 0, 1);}
	public void add(float x, float y){ add(x, y, 0, 1);}
	public void add(float x, float y, float z){ add(x, y, z, 1);}
	
	public void add(float x, float y, float z, float w){
		checkCapacity(cmpSize);
		
		data[size++] = x;
		if(cmpSize > 1) data[size++] = y;
		if(cmpSize > 2) data[size++] = z;
		if(cmpSize > 3) data[size++] = w;
		
		touch();
	}
	
	public void set(int index, float x){ set(index, x, 0, 0, 1);}
	public void set(int index, float x, float y){ set(index, x, y, 0, 1);}
	public void set(int index, float x, float y, float z){ set(index, x, y, z, 1);}
	
	public void set(int index, float x, float y, float z, float w){
		index *= cmpSize;
		if(index >= size)
			throw new IndexOutOfBoundsException();
		
		data[index++] = x;
		if(cmpSize > 1) data[index++] = y;
		if(cmpSize > 2) data[index++] = z;
		if(cmpSize > 3) data[index++] = w;
		
		touch();
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
			FloatBuffer fb = buf.asFloatBuffer();
			fb.put(data, 0, size);
			buf.position(buf.position() + getByteSize());
		}else{
			for(int i = 0; i < size; i++){
				buf.putFloat(data[i]);
			}
		}
	}
	
	public float get(int index){
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
	public int getType() { return GLenum.GL_FLOAT;}

	@Override
	public void store(int index, ByteBuffer buf) {
		index = index * cmpSize;
		for(int i = 0;i < cmpSize; i++)
			buf.putFloat(data[index + i]);
	}
	
	public float[] getArray() { return data;}
	
	public static void main(String[] args) {
		AttribFloatArray array = new AttribFloatArray(3, 2);
		array.add(1, 1, 1);
		array.add(2, 2, 2);
		array.add(3, 3, 3);
		
		System.out.println("size = " + array.getSize());
		System.out.println("bytes = " + array.getByteSize());
		Vector3f v = new Vector3f();
		array.get(0, v); System.out.println(v);
		array.get(1, v); System.out.println(v);
		array.get(2, v); System.out.println(v);
	}

}
