package com.nvidia.developer.opengl.utils;

/** 16 bit 565 BGR color. */
final class Color16 {

	public static final int RED_MASK = Integer.parseInt("1111100000000000", 2);
	public static final int GREEN_MASK = Integer.parseInt("11111100000", 2);
	public static final int BLUE_MASK = Integer.parseInt( "11111", 2);
	public short u;
	
	public Color16() {
	}

	public Color16(short u) {
		this.u = u;
	}
	
	public Color16(Color16 c){
		u = c.u;
	}
	
	public void set(short u) {
		this.u = u;
	}
	
	public void set(Color16 c){
		u = c.u;
	}
	
	public int getB(){
		return u & BLUE_MASK ;
	}
	
	public int getG(){
		return (u & GREEN_MASK) >> 5;
	}
	
	public int getR(){
		return (u & RED_MASK) >> 11;
	}
	
	public int getColor(){
		return u;
	}
}
