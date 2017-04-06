package com.nvidia.developer.opengl.app;

public interface KeyEventListener {

	public void keyPressed(int keycode, char keychar);
	
	public void keyReleased(int keycode, char keychar);
	
	public void keyTyped(int keycode, char keychar);
}
