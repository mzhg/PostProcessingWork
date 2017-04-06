package com.nvidia.developer.opengl.ui;

/**
 * A structure holding some 'reaction' to a given user input event.
 * <p>
 * When a UI element handles an input event, it might have a side-effect it
 * wants to occur (an NvUIReaction) in the UI hierarchy/system. This simple
 * structure allows for noting who kicked off the reaction, the 'type' of
 * Reaction (that is usually a unique identifier code), and any secondary state
 * that might be useful to the future-handling element.
 * 
 * @author Nvidia 2014-9-2
 * 
 */
public final class NvUIReaction {

	/**
	 * This is the NvUIElement.uiuid member of the element 'raising' the
	 * reaction.
	 */
	public int uid;
	/** This is the app/element specific 'reaction code'. */
	public int code;
	/**
	 * This is other reaction state, generally used for visual widgets like
	 * buttons to track or pass current draw state.
	 */
	public int state;
	/** Copying the 'cause' of the reaction. If kind is none, it's not input/gesture related. */
	public int causeKind;
	/** For input-related reactions, this could be mouse button, gamepad button, or key identifier. */
	public int causeIndex;
	/** Any particular extra flags about this reaction. */
	public int flags; //NvReactFlag::Enum
	/** This is a floating point value from data-related widgets. */
	public float fval;
	/** This is an integer value from data-related widgets. */
	public int ival;
	
	/** Set all values of NvUIREaction to 0. */
	public void makeZero(){
		uid = 0;
		code = 0;
		state = 0;
		causeKind = 0;
		causeIndex = 0;
		flags = 0;
		fval = 0;
		ival = 0;
	}
}
