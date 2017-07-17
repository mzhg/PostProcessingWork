package jet.opengl.postprocessing.util;

import java.util.Arrays;

public class StackInt {

	private int[] items;
	private transient int size;

	public StackInt() {
		items = Numeric.EMPTY_INT;
	}

	public StackInt(int capacity) {
		capacity = capacity < 1 ? 1 : capacity;
		items = new int[capacity];
	}

	public void push(int item) {
		if (size == items.length)
			items = Arrays.copyOf(items, Math.max(16, items.length * 2));

		items[size++] = item;
	}

	public void addAll(StackInt ints) {
		for (int i = 0; i < ints.size; i++) {
			push(ints.get(i));
		}
	}
	
	public void copyFrom(StackInt ints) {
		size = 0;
		addAll(ints);
	}

	public boolean isEmpty() {
		return size == 0;
	}

	public int pop() {
		if (isEmpty())
			throw new NullPointerException("stack is empty!");

		return items[--size];
	}

	public int size() {
		return size;
	}

	public int[] getData() {
		return items;
	}

	public void set(int index, int value) {
		if (index >= size)
			throw new IndexOutOfBoundsException();

		items[index] = value;
	}

	public void clear() {
		size = 0;
		if (items.length >= 256) {
			items = Numeric.EMPTY_INT;
		}
	}

	public int[] toArray() {
		int[] tmp = new int[size];
		System.arraycopy(items, 0, tmp, 0, size);
		return tmp;
	}

	public int get(int index) {
		return items[index];
	}

	public void resize(int newSize) {
		if (newSize < 0)
			throw new IllegalArgumentException("newSize < 0. newSize = "
					+ newSize);
		if (newSize <= items.length)
			size = newSize;
		else {
			items = Arrays.copyOf(items, newSize);
			size = newSize;
		}
	}
	
	public void reserve(int size){
		if(size >= items.length){
			items = Arrays.copyOf(items, size);
		}
	}
	
	public void fill(int offset, int len, int value){
		if(len < 0)
			throw new IllegalArgumentException("len < 0. len = " + len);
		if(offset + len - 1 >= size)
			throw new IndexOutOfBoundsException();
		
		for(int i = offset; i < offset + len;i++){
			items[i] = value;
		}
	}

	public int peer() {
		if (isEmpty())
			throw new NullPointerException("stack is empty!");

		return items[size - 1];
	}

	public StackInt copy() {
		StackInt sf = new StackInt(size);
		System.arraycopy(items, 0, sf.items, 0, size);
		return sf;
	}
}
