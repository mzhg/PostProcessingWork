package com.nvidia.developer.opengl.ui;

public class NvUIRect {
	/** Left/x origin in UI-space */
	public float left;
	/** Top/y origin in UI-space */
	public float top; 
	/** Width in pixels */
	public float width; 
	/** Height in pixels */
	public float height;
	/** FUTURE: z-depth of rect */
	public float zdepth;
	
	/** Default constructor, zeroes all elements. */
	public NvUIRect() {
	}

	/**
	 * Normal use constructor.
	 * @param left left edge position
	 * @param top top edge position
	 * @param width width of rect
	 * @param height height of rect
	 */
	public NvUIRect(float left, float top, float width, float height) {
		this.left = left;
		this.top = top;
		this.width = width;
		this.height = height;
	}
	
	/**
	 * General value setter.
	 * @param l left edge position
	 * @param t top edge position
	 * @param w width of rect
	 * @param h height of rect
	 */
	public void set(float l, float t, float w, float h)
    {
        left = l;
        top = t;
        width = w;
        height = h;
    }
	
	public void set(NvUIRect r){
		left = r.left;
		top = r.top;
		width = r.width;
		height = r.height;
		zdepth = r.zdepth;
	}
	
	/**
	 * Tests whether a given point is inside our rect bounds, with optional
	 * margin for slop-factor with inaccurate tests (i.e., finger-hit-box).
	 * @param x X coordinate of point in UI space
        @param y Y coordinate of point in UI space
        @param mx Margin in pixels in x-axis
        @param my Margin in pixels in y-axis
        @return true if tests as inside rect, false otherwise.
	 */
	public boolean inside(float x, float y, float mx, float my){
        if (x >= left-mx && x <= left+width+mx)
            if (y >= top-my && y <= top+height+my)
                return true;
        return false;
    }
	
	/**
	 * Helper to grow the rectangle by a given width and height, keeping existing 'center'.
	 * This is used to build 'outer' frames, borders, focus rectangles.
	 */
	public void grow(float w, float h){
		// when add gravity, need to adjust +/- here.. todo.
        left -= w*0.5f;
        top -= h*0.5f;
        width += w;
        height += h;
	}
	
	@Override
	public String toString() {
		return "NvUIRect[left, top, width, height] = [" + left + ", " + top + ", " + width + ", " + height + "]";
	}
}
