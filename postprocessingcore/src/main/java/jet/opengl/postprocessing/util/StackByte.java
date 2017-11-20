package jet.opengl.postprocessing.util;

import java.util.Arrays;

public class StackByte {

	private byte[] items;
	private transient int size;

	public StackByte() {
		items = Numeric.EMPTY_BYTE;
	}

	public StackByte(int capacity) {
		capacity = capacity < 1 ? 1 : capacity;
		items = new byte[capacity];
	}

	public void push(byte item) {
		if (size == items.length)
			items = Arrays.copyOf(items, Math.max(16, items.length * 2));

		items[size++] = item;
	}

	public void addAll(StackByte ints) {
		for (int i = 0; i < ints.size; i++) {
			push(ints.get(i));
		}
	}
	
	public void copyFrom(StackByte ints) {
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

	public byte[] getData() {
		return items;
	}

	public void set(int index, byte value) {
		if (index >= size)
			throw new IndexOutOfBoundsException();

		items[index] = value;
	}

	public void clear() {
		size = 0;
		if (items.length >= 256) {
			items = Numeric.EMPTY_BYTE;
		}
	}

	public byte[] toArray() {
		byte[] tmp = new byte[size];
		System.arraycopy(items, 0, tmp, 0, size);
		return tmp;
	}

	public byte get(int index) {
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

	public byte incrLeft(int index){
		if(index >= size)
			throw new IndexOutOfBoundsException("size is " + size + ", and the index is " + index);

		return ++items[index];
	}

	public byte incrRight(int index){
		if(index >= size)
			throw new IndexOutOfBoundsException("size is " + size + ", and the index is " + index);

		return items[index]++;
	}
	
	public void reserve(int size){
		if(size >= items.length){
			items = Arrays.copyOf(items, size);
		}
	}
	
	public void fill(int offset, int len, byte value){
		if(len < 0)
			throw new IllegalArgumentException("len < 0. len = " + len);
		if(offset + len - 1 >= size)
			throw new IndexOutOfBoundsException();
		
		for(int i = offset; i < offset + len;i++){
			items[i] = value;
		}
	}

	public void trimToSize(){
		if(size < items.length)
			items = Arrays.copyOf(items, size);
	}

	public byte peer() {
		if (isEmpty())
			throw new NullPointerException("stack is empty!");

		return items[size - 1];
	}

	public StackByte copy() {
		StackByte sf = new StackByte(size);
		System.arraycopy(items, 0, sf.items, 0, size);
		return sf;
	}
}
