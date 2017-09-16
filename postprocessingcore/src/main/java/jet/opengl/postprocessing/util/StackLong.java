package jet.opengl.postprocessing.util;

import java.util.Arrays;

public class StackLong {

	private long[] items;
	private transient int size;

	public StackLong() {
		items = Numeric.EMPTY_LONG;
	}

	public StackLong(int capacity) {
		capacity = capacity < 1 ? 1 : capacity;
		items = new long[capacity];
	}

	public void push(long item) {
		if (size == items.length)
			items = Arrays.copyOf(items, Math.max(16, items.length * 2));

		items[size++] = item;
	}

	public void addAll(StackLong ints) {
		for (int i = 0; i < ints.size; i++) {
			push(ints.get(i));
		}
	}
	
	public void copyFrom(StackLong ints) {
		size = 0;
		addAll(ints);
	}

	public boolean isEmpty() {
		return size == 0;
	}

	public long pop() {
		if (isEmpty())
			throw new NullPointerException("stack is empty!");

		return items[--size];
	}

	public int size() {
		return size;
	}

	public long[] getData() {
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
			items = Numeric.EMPTY_LONG;
		}
	}

	public long[] toArray() {
		long[] tmp = new long[size];
		System.arraycopy(items, 0, tmp, 0, size);
		return tmp;
	}

	public long get(int index) {
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

	public long incrLeft(int index){
		if(index >= size)
			throw new IndexOutOfBoundsException("size is " + size + ", and the index is " + index);

		return ++items[index];
	}

	public long incrRight(int index){
		if(index >= size)
			throw new IndexOutOfBoundsException("size is " + size + ", and the index is " + index);

		return items[index]++;
	}
	
	public void reserve(int size){
		if(size >= items.length){
			items = Arrays.copyOf(items, size);
		}
	}
	
	public void fill(int offset, int len, long value){
		if(len < 0)
			throw new IllegalArgumentException("len < 0. len = " + len);
		if(offset + len - 1 >= size)
			throw new IndexOutOfBoundsException();
		
		for(int i = offset; i < offset + len;i++){
			items[i] = value;
		}
	}

	public long peer() {
		if (isEmpty())
			throw new NullPointerException("stack is empty!");

		return items[size - 1];
	}

	public StackLong copy() {
		StackLong sf = new StackLong(size);
		System.arraycopy(items, 0, sf.items, 0, size);
		return sf;
	}
}
