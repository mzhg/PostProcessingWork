package com.nvidia.developer.opengl.ui;

/**
 * This enum defines bit flags for extra/other info about a particular
 * NvUIReaction.
 */
public interface NvReactFlag {

	/** No additional flag data. */
	static final int NONE = 0;
	/**
	 * Flag to notify any UI elements linked to an outside data source
	 * (NvTweakVar or otherwise) that they should update themselves.
	 */
	static final int FORCE_UPDATE = 0x01;
	/**
	 * Flag that UI elements that match this reaction should clear their drawing state (to 'inactive').
	 */
	static final int CLEAR_STATE = 0x02;
}
