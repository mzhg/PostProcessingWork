package com.nvidia.developer.opengl.models;

public class AttribInfo {

	int index = -1;
	int size;
	int type;
	int offset;
	int modified;
	int divisor;
	
	@Override
	public String toString() {
		
		return "AttribInfo [index=" + index + ", size=" + size + ", type=" + type + ", offset=" + offset + ", modified="
				+ modified + ", divisor="+ divisor + "]";
	}
}
