package com.nvidia.developer.opengl.app;

/**
 * Input handling callback interfaces for internal use.
 * 
 * @author Nvidia 2014-9-12 17:50
 * 
 */
interface NvInputCallbacks {

	/**
	 * Pointer input event.
	 * <p>
	 * Called when any pointer device has changed
	 * 
	 * @param device
	 *            the device generating the event
	 * @param action
	 *            the action represented by the event
	 * @param modifiers
	 *            any modifiers to the event (normally only for mice)
	 * @param count
	 *            the number of points in the #points array parameter
	 * @param points
	 *            an array of the points in the event (normally a single point
	 *            unless the device is a multi-touch screen
	 * @return true if the recipient handled the event, false if the recipient
	 *         wishes the caller to handle the event
	 */
	public boolean pointerInput(NvInputDeviceType device, int action, int modifiers,
								int count, NvPointerEvent[] points);

	/**
	 * Key input event
	 * <p>
	 * Called when a key is pressed, released or held
	 * 
	 * @param code
	 *            the keycode of the event. This is an #NvKey mask
	 * @param action
	 *            the action for the given key
	 * @return true if the recipient handled the event, false if the recipient
	 *         wishes the caller to handle the event. Returning true from a key
	 *         event will generally preclude any #characterInput events coming
	 *         from the action
	 */
	public boolean keyInput(int code, NvKeyActionType action);

	/**
	 * Character input event
	 * <p>
	 * Called when a keypressed, release or hold is unhandled and maps to a
	 * character
	 * 
	 * @param c
	 *            the ASCII character of the event
	 * @return true if the recipient handled the event, false if the recipient
	 *         wishes the caller to handle the event
	 */
	public boolean characterInput(char c);

	/**
	 * Gamepad input event Called when any button or axis value on any active
	 * gamepad changes
	 * 
	 * @param changedPadFlags
	 *            a mask of the changed pad indices. For each gamepad i that has
	 *            changed, bit (1 shift-left i) will be set.
	 * @return true if the recipient handled the event, false if the recipient
	 *         wishes the caller to handle the event
	 */
	public boolean gamepadChanged(int changedPadFlags);
}
