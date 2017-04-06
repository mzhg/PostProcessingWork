package com.nvidia.developer.opengl.app;

public interface TouchEventListener {

	public static final int BUTTON_LEFT = 1;
	public static final int BUTTON_RIGHT = 2;
	public static final int BUTTON_MIDDLE = 4;
	
	public static final int BUTTON_1 = 1;
	public static final int BUTTON_2 = 2;
	public static final int BUTTON_3 = 4;
	public static final int BUTTON_4 = 8;
	public static final int BUTTON_5 = 16;
	public static final int BUTTON_6 = 32;
	public static final int BUTTON_7 = 64;
	public static final int BUTTON_8 = 128;
	
	public void touchPressed(NvInputDeviceType type, int count, NvPointerEvent[] events);
	
	public void touchReleased(NvInputDeviceType type, int count, NvPointerEvent[] events);
	
	public void touchMoved(NvInputDeviceType type, int count, NvPointerEvent[] events);

	public void scrolled(int wheel);
	
	public default void cursorEnter(boolean entered){
		// nothing need todo here.
	}
}
