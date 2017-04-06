package com.nvidia.developer.opengl.ui;

/**
 * Event handling response flags.
 * <p>
 * 
 * All UI objects have the ability/opportunity to handle input events, and must
 * return an NvUIResponse flag for what occurred as a result.
 * <p>
 * 
 * In general, the response notes that an object has handled the event, wants to
 * keep 'interacting' as a result of it, and/or has a NvUIReaction (a side
 * effect) that is triggered based on it.
 */
public interface NvUIEventResponse {

	/** < Flag/mask that we didn't handle the event. */
	public static final int nvuiEventNotHandled = 0x000;
	/** < Flag/mask that we handled the event. */
	public static final int nvuiEventHandled = 0x001;
	/** < Flag/mask that we want hover highlight as a result of the event. */
	public static final int nvuiEventWantsHover = 0x010;
	/** < We handled the event AND want hover highlight. */
	public static final int nvuiEventHandledHover = nvuiEventHandled | nvuiEventWantsHover;
	/** < Flag/mask that we want to keep interacting as a result of the event. */
	public static final int nvuiEventWantsInteract = 0x020;
	/** < We handled the event AND want to keep interacting. */
	public static final int nvuiEventHandledInteract = nvuiEventHandled | nvuiEventWantsInteract;
	/** < Flag/mask that we have a NvUIReaction as a side effect. */
	public static final int nvuiEventHadReaction = 0x100;
	/** < We handled the event AND 'posted' an NvUIReaction. */
	public static final int nvuiEventHandledReaction = nvuiEventHandled | nvuiEventHadReaction;
	/**
	 * < We handled the event AND want to keep interacting AND had anNvUIReaction.
	 */
	public static final int nvuiEventHandledInteractReaction = nvuiEventHandled | nvuiEventWantsInteract | nvuiEventHadReaction;
	/** < Mask to clear a prior Reaction flag from the response. */
	public static final int nvuiEventNoReaction = ~nvuiEventHadReaction;
}
