package com.nvidia.developer.opengl.ui;

/** This enum defines values for moving focus around with keyboard/DPAD when allowed and supported. */
public interface NvFocusEvent {

	/** Clear current focus chain. */
	static final int FOCUS_CLEAR     =   0x0000;
	/** Movement events. */
	static final int FLAG_MOVE       =   0x10;
	/** Action events. */
	static final int FLAG_ACT        =   0x20;
	/** Move focus to first element on screen. */
	static final int MOVE_FIRST      =   0x01+FLAG_MOVE; 
	/** Move focus up. */
	static final int MOVE_UP         =   0x02+FLAG_MOVE; 
	/** Move focus down. */
	static final int MOVE_DOWN       =   0x03+FLAG_MOVE; 
	/** Move focus left. */
	static final int MOVE_LEFT       =   0x04+FLAG_MOVE;
	/** Move focus right. */
	static final int MOVE_RIGHT      =   0x05+FLAG_MOVE;
	/** Press or toggle current value/button where appropriate. */
	static final int ACT_PRESS       =   0x01+FLAG_ACT;
	/** Increase current value/button where appropriate. */
	static final int ACT_INC         =   0x02+FLAG_ACT;
	/** Decrease current value/button where appropriate. */
	static final int ACT_DEC         =   0x03+FLAG_ACT; 
}
