package com.nvidia.developer.opengl.app;

public interface GLEventListener {

	void onCreate();
	
	void onResize(int width, int height);
	
	void draw();
	
	void onDestroy();
}
