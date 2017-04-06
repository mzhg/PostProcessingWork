package jet.opengl.desktop.lwjgl;

import com.nvidia.developer.opengl.app.GLEventListener;
import com.nvidia.developer.opengl.app.KeyEventListener;
import com.nvidia.developer.opengl.app.NvInputDeviceType;
import com.nvidia.developer.opengl.app.NvPointerEvent;
import com.nvidia.developer.opengl.app.TouchEventListener;
import com.nvidia.developer.opengl.app.WindowEventListener;

final class InputAdapterTest implements GLEventListener, KeyEventListener, TouchEventListener, WindowEventListener {

	public InputAdapterTest() {
		LwjglApp test = new LwjglApp();
		test.registerGLEventListener(this);
		test.registerGLFWListener(new InputAdapter(this, this, this));
		test.start();
	}
	
	public static void main(String[] args) {
		new InputAdapterTest();
	}


	@Override
	public void touchPressed(NvInputDeviceType type, int count, NvPointerEvent[] events) {

	}

	@Override
	public void touchReleased(NvInputDeviceType type, int count, NvPointerEvent[] events) {

	}

	@Override
	public void touchMoved(NvInputDeviceType type, int count, NvPointerEvent[] events) {

	}

	@Override
	public void scrolled(int wheel) {
		System.out.println("scrolled!");
	}

	@Override
	public void keyPressed(int keycode, char keychar) {
		System.out.println(getKeyName(keycode) + ": keyPressed! keycode = " + keycode + ", keychar = " + keychar);
	}

	@Override
	public void keyReleased(int keycode, char keychar) {
		System.out.println(getKeyName(keycode) + ": keyReleased! keycode = " + keycode + ", keychar = " + keychar);
	}

	@Override
	public void keyTyped(int keycode, char keychar) {
		System.out.println(getKeyName(keycode) + ": keyTyped! keycode = " + keycode + ", keychar = " + keychar);
	}

	private String getKeyName(int keycode){
		String name = /*GLFW.glfwGetKeyName(keycode, 0)*/ null;
		return name != null ? name : "";
	}
	
	@Override
	public void onCreate() {
		System.out.println("onCreate");
	}

	@Override
	public void onResize(int width, int height) {
		System.out.println("onResize");
	}

	@Override
	public void draw() {
//		System.out.println("draw");
	}

	@Override
	public void onDestroy() {
		System.out.println("onDestroy");
	}

	@Override
	public void windowPos(int xpos, int ypos) {
//		System.out.println("windowPos: " + xpos + ", " + ypos);
	}

	@Override
	public void windowClose() {
		System.out.println("windowClose");
	}

	@Override
	public void windowFocus(boolean focused) {
		System.out.println("windowFocus: " + focused);
	}

	@Override
	public void windowIconify(boolean iconified) {
		System.out.println("windowIconify: " + iconified);
	}

}
