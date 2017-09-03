package assimp.importer.mdl;

/** 16 bit 565 BGR color. */
public class RGB565 {

	public static final int RED_MASK = Integer.parseInt("1111100000000000", 2);
	public static final int GREEN_MASK = Integer.parseInt("11111100000", 2);
	public static final int BLUE_MASK = Integer.parseInt( "11111", 2);
	public short u;
	
	public RGB565() {
	}

	public RGB565(short u) {
		this.u = u;
	}
	
	public RGB565(RGB565 c){
		u = c.u;
	}
	
	public void set(short u) {
		this.u = u;
	}
	
	public void set(RGB565 c){
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
