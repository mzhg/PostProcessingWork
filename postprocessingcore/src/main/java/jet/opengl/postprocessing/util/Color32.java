package jet.opengl.postprocessing.util;

/** 32 bit color stored as RGBA */
final class Color32 {

	public int u;
	
	/**
	 * every value must be clamp to [0, 255].
	 * @param r
	 * @param g
	 * @param b
	 * @param a
	 */
	public void set(int r, int g, int b, int a){
		u = Numeric.makeRGBA(r, g, b, a);
	}
	
	/**
	 * Get the red component of the color32
	 * @return the red component, range from 0 to 255.
	 */
	public int getR(){
		return u & 0xFF;
	}
	
	/**
	 * Get the green component of the color32
	 * @return the green component, range from 0 to 255.
	 */
	public int getG(){
		return (u >> 8) & 0xFF;
	}
	
	/**
	 * Get the blue component of the color32
	 * @return the blue component, range from 0 to 255.
	 */
	public int getB(){
		return (u >> 16) & 0xFF;
	}
	
	/**
	 * Get the alpha component of the color32
	 * @return the alpha component, range from 0 to 255.
	 */
	public int getA(){
		return (u >> 24) & 0xFF;
	}
}
