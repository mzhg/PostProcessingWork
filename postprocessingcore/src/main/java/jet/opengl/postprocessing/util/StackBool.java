package jet.opengl.postprocessing.util;

import java.util.Arrays;

public class StackBool {

	private boolean[] items;
	private transient int size;

	public StackBool() {
		items = Numeric.EMPTY_BOOL;
	}

	public StackBool(int capacity) {
		capacity = capacity < 1 ? 1 : capacity;
		items = new boolean[capacity];
	}

	public void push(boolean item) {
		if (size == items.length)
			items = Arrays.copyOf(items, Math.max(16, items.length * 2));

		items[size++] = item;
	}

	public void addAll(StackBool ints) {
		for (int i = 0; i < ints.size; i++) {
			push(ints.get(i));
		}
	}
	
	public void copyFrom(StackBool ints) {
		size = 0;
		addAll(ints);
	}

	public boolean isEmpty() {
		return size == 0;
	}

	public boolean pop() {
		if (isEmpty())
			throw new NullPointerException("stack is empty!");

		return items[--size];
	}

	public int size() {
		return size;
	}

	public boolean[] getData() {
		return items;
	}

	public void set(int index, boolean value) {
		if (index >= size)
			throw new IndexOutOfBoundsException();

		items[index] = value;
	}

	public void clear() {
		size = 0;
		if (items.length >= 256) {
			items = Numeric.EMPTY_BOOL;
		}
	}

	public boolean[] toArray() {
		boolean[] tmp = new boolean[size];
		System.arraycopy(items, 0, tmp, 0, size);
		return tmp;
	}

	public void trimToSize(){
		if(size < items.length)
			items = Arrays.copyOf(items, size);
	}

	public boolean get(int index) {
		if(index >= size)
			throw new IndexOutOfBoundsException("size is " + size + ", and the index is " + index);
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
	
	public void fill(int offset, int len, boolean value){
		if(len < 0)
			throw new IllegalArgumentException("len < 0. len = " + len);
		if(offset + len - 1 >= size)
			throw new IndexOutOfBoundsException();
		
		for(int i = offset; i < offset + len;i++){
			items[i] = value;
		}
	}

	public boolean peer() {
		if (isEmpty())
			throw new NullPointerException("stack is empty!");

		return items[size - 1];
	}

	public StackBool copy() {
		StackBool sf = new StackBool(size);
		System.arraycopy(items, 0, sf.items, 0, size);
		return sf;
	}

	public void moveTo(StackBool dest){
		if(this == dest)
			return;

		dest.size = size;
		dest.items = items;

		size = 0;
		items = Numeric.EMPTY_BOOL;
	}
}
