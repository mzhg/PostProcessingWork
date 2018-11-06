package jet.opengl.postprocessing.util;

import org.lwjgl.util.vector.ReadableVector2f;
import org.lwjgl.util.vector.ReadableVector3f;
import org.lwjgl.util.vector.ReadableVector4f;

import java.util.Arrays;

public class StackDouble {

	private double[] items;
	private transient int size;

	public StackDouble() {
		items = Numeric.EMPTY_DOUBLE;
	}

	public StackDouble(int capacity) {
		capacity = capacity < 1 ? 1 : capacity;
		items = new double[capacity];
	}

	public double set(int index, double value) {
		if (index >= size)
			throw new IndexOutOfBoundsException("index = " + index + ", size = " + size);

		double old = items[index];
		items[index] = value;

		return old;
	}
	
	public void addAll(StackDouble ints){
		for(int i = 0; i < ints.size;i++){
			push(ints.get(i));
		}
	}
	
	public void copyFrom(StackDouble ints){
		size = 0;
		addAll(ints);
	}

	public void plus(int index, double value) {
		if (index < 0 || index >= size)
			throw new IndexOutOfBoundsException();

		items[index] += value;
	}

	public void push(double item) {
		if (size == items.length)
			items = Arrays.copyOf(items, Math.max(16, items.length * 2));

		items[size++] = item;
	}

	public void push(ReadableVector2f v){
		push(v.getX());
		push(v.getY());
	}

	public void push(ReadableVector3f v){
		push(v.getX());
		push(v.getY());
		push(v.getZ());
	}

	public void push(ReadableVector4f v){
		push(v.getX());
		push(v.getY());
		push(v.getZ());
		push(v.getW());
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
			items = Numeric.EMPTY_DOUBLE;
		}
	}

	public double pop() {
		if (isEmpty())
			throw new NullPointerException("stack is empty!");

		return items[--size];
	}

	public double[] getData() {
		return items;
	}

	public double[] toArray() {
		double[] tmp = new double[size];
		System.arraycopy(items, 0, tmp, 0, size);
		return tmp;
	}

	public double get(int index) {
		return items[index];
	}

	public double peer() {
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
			items = new double[size];
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
	
	public StackDouble copy(){
		StackDouble sf = new StackDouble(size);
		System.arraycopy(items, 0, sf.items, 0, size);
		return sf;
	}

	public void trim(){
		if(size < items.length){
			items = Arrays.copyOf(items, size);
		}
	}
}
