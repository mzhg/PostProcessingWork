package com.nvidia.developer.opengl.ui;

/** These are the kinds of resulting gestures that we recognize. */
public interface NvGestureKind {
    /// Special bit masks for classification of types of gestures.
	/** For masking off low byte for raw event */
	public static final int MASK_BASE =           0x00FF; 
	/** Event involves device being held 'down' for some period */
	public static final int MASK_HELD =           0x0100;
	/** Event involves a 'release' up of input */
	public static final int MASK_RELEASED =       0x0200; 
	/** Event involves some movement input device */
	public static final int MASK_MOVED =          0x0400;
	/** Event involves multiple inputs, i.e. multi-finger touch */
	public static final int MASK_MULTI =          0x0800;
	/** Hint that event is conditional on a later timeout */
	public static final int MASK_MAYBE =          0x1000;
	/** Base event occurred twice within short period */
	public static final int MASK_REPEAT_DOUBLE =  0x2000;
	/** Base event occurred three times in short period */
	public static final int MASK_REPEAT_TRIPLE =  0x4000;
	/// ---------------------  end  --------------------------
	
	/// The set of recognized gesture values.
	/** Denotes an invalid value */
	public static final int INVALID          = -1;

    /** Base value, no gesture has occurred yet */
	public static final int NONE             = 0;
    /** Denotes a pointer hovering on screen but no press/click yet. */
	public static final int  HOVER            = 1;
    /** Pointer has been pressed down, clicked */
	public static final int PRESS            = 3;
    /** Pointer has been kept down past a particular timeout */
	public static final int HOLD             = 4 | MASK_HELD;
    // !!!!TBD TODO: possible addition, the pointer being kept down even longer */
    // /** Pointer has been kept down past a longer timeout */
    // LONG_HOLD      = 5, 
    /** Pointer has been released, let up */
	public static final int RELEASE          = 6 | MASK_RELEASED;
	/** A short tap occurred, but we haven't confirmed user intent yet due to timeout or new event */
	public static final int MAYBE_TAP        = 8 | MASK_RELEASED | MASK_MAYBE;
	/** A brief tap was confirmed (either because double-tap delay passed, or other event began) */
	public static final int TAP              = 9 | MASK_RELEASED;
    /** Pointer is down and has moved from its starting point enough to intend to 'drag' */
	public static final int DRAG             = 10 | MASK_MOVED;
    /** Pointer was released during accelerating motion vector */
	public static final int FLICK            = 11 | MASK_MOVED | MASK_RELEASED;
	
	/** An extended gesture after a tap, another quick gesture right after. */
	public static final int TAP_PLUS_PRESS   = 12; /* this is an 'extended' gesture, off of prior one. */
    /*
    //!!!!TBD TODO:
    //these "tap-plus" gestures are all TBD.
    TAP_HOLD         = 16 | MASK_HELD,
    TAP_DRAG         = 17 | MASK_MOVED,
 */
    /** Pointer has come back down again within a small delta distance and time of a prior tap. */
	public static final int DOUBLETAP        = TAP | MASK_REPEAT_DOUBLE;
    /* !!!!TBD TODO: a tap with multiple fingers at once */
	public static final int MULTI_TAP        = TAP | MASK_MULTI;
    /* !!!!TBD TODO: a pinch/zoom of two fingers coming together, or spreading apart
     * (note I declared it drag+multi for convenience, it will likely
     * want/need to change it before locking in the specification) */
	public static final int MULTI_ZOOM       = DRAG | MASK_MULTI;
}
