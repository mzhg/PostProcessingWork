package com.nvidia.developer.opengl.utils;

import java.util.ArrayList;
import java.util.List;

public class Pool<T> {
	
	private final List<T> freeObjects;
	private final PoolObjectFactory<T> factory;
	private final int maxSize;
	
	public Pool(PoolObjectFactory<T> factory, int maxSize) {
		this.factory = factory;
		this.maxSize = maxSize;
		this.freeObjects = new ArrayList<T>();
	}
	
	public T newObject(){
		return freeObjects.isEmpty() ? factory.createObject() : freeObjects.remove(freeObjects.size() - 1);
	}
	
	public void freeObject(T obj){
		if(freeObjects.size() < maxSize)
			freeObjects.add(obj);
	}

	public interface PoolObjectFactory<T>{
		public T createObject();
	}
}
