package com.nvidia.developer.opengl.ui;

/**
 * Pair of NvTweakCmd and NvTweakVarBase that we can look up as a bound result for some user input.
 * @author Nvidia 2014-9-13 19:13
 *
 */
public class NvTweakBind {

	/** No command specified */
	public static final int NONE = 0;
	/** Reset the variable to initial state */
	public static final int RESET = 1;
	/** Increment the variable */
	public static final int INCREMENT = 2;
	/** Decrement the variable */
	public static final int DECREMENT = 3;
	
	public int mCmd;
	public NvTweakVarBase mVar;
	
	public NvTweakBind() {
	}

	public NvTweakBind(int cmd, NvTweakVarBase var) {
		this.mCmd = cmd;
		this.mVar = var;
	}
}
