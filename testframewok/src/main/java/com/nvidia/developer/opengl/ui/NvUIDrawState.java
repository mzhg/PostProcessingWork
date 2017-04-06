package com.nvidia.developer.opengl.ui;

/**
 * A class that manages all draw-related state.<p>
 * When calling a UI element to Draw itself, an NvUIDrawState object is passed
 * in to give 'annotation' to the drawing process.  This allows us to also add
 * further information to the DrawState, without changing the signature of the
 * Draw method.
 * @author Nvidia 2014-9-2
 *
 */
public class NvUIDrawState {

	/** A UST timestamp for the "time" of a given Draw call.  Currently unused. */
	public long time;
	/** The render-buffer view width. */
	public int  width;
	/** The render-buffer view height. */
	public int   height;
	/** [optional] The current UI's designed width.  Important for auto-scaling and orientation of UI widgets if there is an explicit 'design space' vs current viewport space. */
	public int   designWidth; 
	/** [optional] The current UI's designed height.  Important for auto-scaling and orientation of UI widgets if there is an explicit 'design space' vs current viewport space. */
	public int   designHeight;
	/** [optional] An alpha-fade override for this draw call, to help fading elements over time.  Defaults to 1.0 (opaque). */
    public float alpha = 1.0f;
    /** [optional] A rotation (in degrees) to use for 'aligning' content between the design and the render-buffer.  Defaults to 0.0 (unrotated). */
    public float rotation;
    
    public NvUIDrawState(long time, int width, int height) {
    	this.time = time;
		this.width = width;
		this.height = height;
	}
    
    public NvUIDrawState(NvUIDrawState state) {
    	time = state.time;
    	width = state.width;
    	height = state.height;
    	designWidth = state.designWidth;
    	designHeight = state.designHeight;
    	alpha = state.alpha;
    	rotation = state.rotation;
	}

	public NvUIDrawState(long time, int width, int height, int designWidth,
			int designHeight) {
		this.time = time;
		this.width = width;
		this.height = height;
		this.designWidth = designWidth;
		this.designHeight = designHeight;
	}
}
