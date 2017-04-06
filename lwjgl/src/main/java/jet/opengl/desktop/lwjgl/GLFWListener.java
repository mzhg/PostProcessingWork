package jet.opengl.desktop.lwjgl;

import com.nvidia.developer.opengl.app.WindowEventListener;

import org.lwjgl.glfw.GLFW;

public interface GLFWListener extends WindowEventListener {
	
	/**
	 * The key callback.
	 *
	 * @param key    the keyboard key that was pressed or released
	 * @param action the key action. One of:<p/>{@link GLFW#GLFW_PRESS}, {@link GLFW#GLFW_RELEASE}, {@link GLFW#GLFW_REPEAT}
	 * @param mods   bitfield describing which modifiers keys were held down
	 */
	public void key(int key, int scancode, int action, int mods);
	
	
	/**
	 * This Unicode character with modifiers callback. It is called for each input character, regardless of what modifier keys are held down.
	 *
	 * @param codepoint the Unicode code point of the character
	 * @param mods      bitfield describing which modifier keys were held down
	 */
	public void charMods(int codepoint, int mods);
	
	/**
	 * The mouse button callback.
	 *
	 * @param button the mouse button that was pressed or released
	 * @param action the button action. One of:<p/>{@link GLFW#GLFW_PRESS}, {@link GLFW#GLFW_RELEASE}
	 * @param mods   bitfield describing which modifiers keys were held down
	 */
	public void mouseButton(int button, int action, int mods);
	
	/**
	 * The cursor move callback.
	 *
	 * @param xpos   the new x-coordinate, in screen coordinates of the cursor
	 * @param ypos   the new y-coordinate, in screen coordinates of the cursor
	 */
	public void cursorPos(double xpos, double ypos);
	
	/**
	 * The cursor enter callback.
	 *
	 * @param entered true if the cursor enter the window's client area, or false if it left it
	 */
	public void cursorEnter(boolean entered);
	
	/**
	 * The scroll callback.
	 *
	 * @param xoffset the scroll offset along the x-axis
	 * @param yoffset the scroll offset along the y-axis
	 */
	public void scroll(double xoffset, double yoffset);
	
}
