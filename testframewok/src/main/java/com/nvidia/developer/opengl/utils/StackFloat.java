package com.nvidia.developer.opengl.utils;

import java.util.Arrays;

import jet.opengl.postprocessing.util.Numeric;

public class StackFloat {

	private float[] items;
	private transient int size;

	public StackFloat() {
		items = Numeric.EMPTY_FLOAT;
	}

	public StackFloat(int capacity) {
		capacity = capacity < 1 ? 1 : capacity;
		items = new float[capacity];
	}

	public void set(int index, float value) {
		if (index >= size)
			throw new IndexOutOfBoundsException("index = " + index + ", size = " + size);

		items[index] = value;
	}
	
	public void addAll(StackFloat ints){
		for(int i = 0; i < ints.size;i++){
			push(ints.get(i));
		}
	}
	
	public void copyFrom(StackFloat ints){
		size = 0;
		addAll(ints);
	}

	public void plus(int index, float value) {
		if (index < 0 || index >= size)
			throw new IndexOutOfBoundsException();

		items[index] += value;
	}

	public void push(float item) {
		if (size == items.length)
			items = Arrays.copyOf(items, Math.max(16, items.length * 2));

		items[size++] = item;
	}
	
	public boolean isEmpty() {
		return size == 0;
	}

	public int size() {
		return size;
	}

	public void clear() {
		size = 0;
		if (items.length >= 256) {
			items = Numeric.EMPTY_FLOAT;
		}
	}

	public float pop() {
		if (isEmpty())
			throw new NullPointerException("stack is empty!");

		return items[--size];
	}

	public float[] getData() {
		return items;
	}

	public float[] toArray() {
		float[] tmp = new float[size];
		System.arraycopy(items, 0, tmp, 0, size);
		return tmp;
	}

	public float get(int index) {
		return items[index];
	}

	public float peer() {
		if (isEmpty())
			throw new NullPointerException("stack is empty!");

		return items[size - 1];
	}

	public void reserve(int cap) {
		if (cap > items.length) {
			items = Arrays.copyOf(items, cap);
		}
	}

	public void resize(int size, float value) {
		if (size < 0)
			throw new IllegalArgumentException("size < 0");
		if (size >= items.length)
			items = new float[size];
		Arrays.fill(items, 0, size, value);

		this.size = size;
	}

	public void resize(int size) {
		if (size < 0)
			throw new IllegalArgumentException("size < 0");

		if (size >= items.length)
			items = Arrays.copyOf(items, size);

		this.size = size;
	}
	
	public StackFloat copy(){
		StackFloat sf = new StackFloat(size);
		System.arraycopy(items, 0, sf.items, 0, size);
		return sf;
	}
}
