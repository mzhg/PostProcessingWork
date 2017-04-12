package jet.opengl.desktop.lwjgl;

import com.nvidia.developer.opengl.app.KeyEventListener;
import com.nvidia.developer.opengl.app.NvInputDeviceType;
import com.nvidia.developer.opengl.app.NvKey;
import com.nvidia.developer.opengl.app.NvPointerEvent;
import com.nvidia.developer.opengl.app.TouchEventListener;
import com.nvidia.developer.opengl.app.WindowEventListener;

import org.lwjgl.glfw.GLFW;

import java.util.HashMap;

public class InputAdapter implements GLFWListener, NvKey {
	private static final HashMap<Integer, Integer> sKeyCodeRemap = new HashMap<Integer, Integer>(128);
	static{
		sKeyCodeRemap.put(GLFW.GLFW_KEY_ESCAPE, K_ESC);
		sKeyCodeRemap.put(GLFW.GLFW_KEY_F1, K_F1);
		sKeyCodeRemap.put(GLFW.GLFW_KEY_F2, K_F2);
		sKeyCodeRemap.put(GLFW.GLFW_KEY_F3, K_F3);
		sKeyCodeRemap.put(GLFW.GLFW_KEY_F4, K_F4);
		sKeyCodeRemap.put(GLFW.GLFW_KEY_F5, K_F5);
		sKeyCodeRemap.put(GLFW.GLFW_KEY_F6, K_F6);
		sKeyCodeRemap.put(GLFW.GLFW_KEY_F7, K_F7);
		sKeyCodeRemap.put(GLFW.GLFW_KEY_F8, K_F8);
		sKeyCodeRemap.put(GLFW.GLFW_KEY_F9, K_F9);
		sKeyCodeRemap.put(GLFW.GLFW_KEY_F10, K_F10);
		sKeyCodeRemap.put(GLFW.GLFW_KEY_F11, K_F11);
		sKeyCodeRemap.put(GLFW.GLFW_KEY_F12, K_F12);
		sKeyCodeRemap.put(GLFW.GLFW_KEY_PRINT_SCREEN, K_PRINT_SCREEN);
		sKeyCodeRemap.put(GLFW.GLFW_KEY_SCROLL_LOCK, K_SCROLL_LOCK);
		sKeyCodeRemap.put(GLFW.GLFW_KEY_PAUSE, K_PAUSE);
		sKeyCodeRemap.put(GLFW.GLFW_KEY_INSERT, K_INSERT);
		sKeyCodeRemap.put(GLFW.GLFW_KEY_DELETE, K_DELETE);
		sKeyCodeRemap.put(GLFW.GLFW_KEY_HOME, K_HOME);
		sKeyCodeRemap.put(GLFW.GLFW_KEY_END, K_END);
		sKeyCodeRemap.put(GLFW.GLFW_KEY_PAGE_UP, K_PAGE_UP);
		sKeyCodeRemap.put(GLFW.GLFW_KEY_PAGE_DOWN, K_PAGE_DOWN);
		sKeyCodeRemap.put(GLFW.GLFW_KEY_UP, K_ARROW_UP);
		sKeyCodeRemap.put(GLFW.GLFW_KEY_DOWN, K_ARROW_DOWN);
		sKeyCodeRemap.put(GLFW.GLFW_KEY_LEFT, K_ARROW_LEFT);
		sKeyCodeRemap.put(GLFW.GLFW_KEY_RIGHT, K_ARROW_RIGHT);
//		sKeyCodeRemap.put(GLFW.GLFW_KEY_ACCENT_GRAVE, K_ACCENT_GRAVE);
		sKeyCodeRemap.put(GLFW.GLFW_KEY_0, K_0);
		sKeyCodeRemap.put(GLFW.GLFW_KEY_1, K_1);
		sKeyCodeRemap.put(GLFW.GLFW_KEY_2, K_2);
		sKeyCodeRemap.put(GLFW.GLFW_KEY_3, K_3);
		sKeyCodeRemap.put(GLFW.GLFW_KEY_4, K_4);
		sKeyCodeRemap.put(GLFW.GLFW_KEY_5, K_5);
		sKeyCodeRemap.put(GLFW.GLFW_KEY_6, K_6);
		sKeyCodeRemap.put(GLFW.GLFW_KEY_7, K_7);
		sKeyCodeRemap.put(GLFW.GLFW_KEY_8, K_8);
		sKeyCodeRemap.put(GLFW.GLFW_KEY_9, K_9);
		sKeyCodeRemap.put(GLFW.GLFW_KEY_MINUS, K_MINUS);
		sKeyCodeRemap.put(GLFW.GLFW_KEY_EQUAL, K_EQUAL);
		sKeyCodeRemap.put(GLFW.GLFW_KEY_BACKSLASH, K_BACKSPACE);
		sKeyCodeRemap.put(GLFW.GLFW_KEY_TAB, K_TAB);
		sKeyCodeRemap.put(GLFW.GLFW_KEY_Q, K_Q);
		sKeyCodeRemap.put(GLFW.GLFW_KEY_W, K_W);
		sKeyCodeRemap.put(GLFW.GLFW_KEY_E, K_E);
		sKeyCodeRemap.put(GLFW.GLFW_KEY_R, K_R);
		sKeyCodeRemap.put(GLFW.GLFW_KEY_T, K_T);
		sKeyCodeRemap.put(GLFW.GLFW_KEY_Y, K_Y);
		sKeyCodeRemap.put(GLFW.GLFW_KEY_U, K_U);
		sKeyCodeRemap.put(GLFW.GLFW_KEY_I, K_I);
		sKeyCodeRemap.put(GLFW.GLFW_KEY_O, K_O);
		sKeyCodeRemap.put(GLFW.GLFW_KEY_P, K_P);
		sKeyCodeRemap.put(GLFW.GLFW_KEY_LEFT_BRACKET, K_LBRACKET);
		sKeyCodeRemap.put(GLFW.GLFW_KEY_RIGHT_BRACKET, K_RBRACKET);
		sKeyCodeRemap.put(GLFW.GLFW_KEY_BACKSLASH, K_BACKSLASH);
		sKeyCodeRemap.put(GLFW.GLFW_KEY_CAPS_LOCK, K_CAPS_LOCK);
		sKeyCodeRemap.put(GLFW.GLFW_KEY_A, K_A);
		sKeyCodeRemap.put(GLFW.GLFW_KEY_S, K_S);
		sKeyCodeRemap.put(GLFW.GLFW_KEY_D, K_D);
		sKeyCodeRemap.put(GLFW.GLFW_KEY_F, K_F);
		sKeyCodeRemap.put(GLFW.GLFW_KEY_G, K_G);
		sKeyCodeRemap.put(GLFW.GLFW_KEY_H, K_H);
		sKeyCodeRemap.put(GLFW.GLFW_KEY_J, K_J);
		sKeyCodeRemap.put(GLFW.GLFW_KEY_K, K_K);
		sKeyCodeRemap.put(GLFW.GLFW_KEY_L, K_L);
		sKeyCodeRemap.put(GLFW.GLFW_KEY_SEMICOLON, K_SEMICOLON);
		sKeyCodeRemap.put(GLFW.GLFW_KEY_APOSTROPHE, K_APOSTROPHE);
		sKeyCodeRemap.put(GLFW.GLFW_KEY_ENTER, K_ENTER);
		sKeyCodeRemap.put(GLFW.GLFW_KEY_LEFT_SHIFT, K_SHIFT_LEFT);
		sKeyCodeRemap.put(GLFW.GLFW_KEY_Z, K_Z);
		sKeyCodeRemap.put(GLFW.GLFW_KEY_X, K_X);
		sKeyCodeRemap.put(GLFW.GLFW_KEY_C, K_C);
		sKeyCodeRemap.put(GLFW.GLFW_KEY_V, K_V);
		sKeyCodeRemap.put(GLFW.GLFW_KEY_B, K_B);
		sKeyCodeRemap.put(GLFW.GLFW_KEY_N, K_N);
		sKeyCodeRemap.put(GLFW.GLFW_KEY_M, K_M);
		sKeyCodeRemap.put(GLFW.GLFW_KEY_COMMA, K_COMMA);
		sKeyCodeRemap.put(GLFW.GLFW_KEY_PERIOD, K_PERIOD);
		sKeyCodeRemap.put(GLFW.GLFW_KEY_SLASH, K_SLASH);
		sKeyCodeRemap.put(GLFW.GLFW_KEY_RIGHT_SHIFT, K_SHIFT_RIGHT);
		sKeyCodeRemap.put(GLFW.GLFW_KEY_LEFT_CONTROL, K_CTRL_LEFT);
		sKeyCodeRemap.put(GLFW.GLFW_KEY_LEFT_SUPER, K_WINDOWS_LEFT);
		sKeyCodeRemap.put(GLFW.GLFW_KEY_LEFT_ALT, K_ALT_LEFT);
		sKeyCodeRemap.put(GLFW.GLFW_KEY_SPACE, K_SPACE);
		sKeyCodeRemap.put(GLFW.GLFW_KEY_RIGHT_ALT, K_ALT_RIGHT);
		sKeyCodeRemap.put(GLFW.GLFW_KEY_RIGHT_SUPER, K_WINDOWS_RIGHT);
//		sKeyCodeRemap.put(GLFW.GLFW_KEY_, K_CONTEXT); TODO
		sKeyCodeRemap.put(GLFW.GLFW_KEY_RIGHT_CONTROL, K_CTRL_RIGHT);
		sKeyCodeRemap.put(GLFW.GLFW_KEY_NUM_LOCK, K_NUMLOCK);
		sKeyCodeRemap.put(GLFW.GLFW_KEY_KP_DIVIDE, K_KP_DIVIDE);
		sKeyCodeRemap.put(GLFW.GLFW_KEY_KP_MULTIPLY, K_KP_MULTIPLY);
		sKeyCodeRemap.put(GLFW.GLFW_KEY_KP_SUBTRACT, K_KP_SUBTRACT);
		sKeyCodeRemap.put(GLFW.GLFW_KEY_KP_ADD, K_KP_ADD);
		sKeyCodeRemap.put(GLFW.GLFW_KEY_KP_ENTER, K_KP_ENTER);
		sKeyCodeRemap.put(GLFW.GLFW_KEY_KP_DECIMAL, K_KP_DECIMAL);
		sKeyCodeRemap.put(GLFW.GLFW_KEY_0, K_KP_0);
		sKeyCodeRemap.put(GLFW.GLFW_KEY_1, K_KP_1);
		sKeyCodeRemap.put(GLFW.GLFW_KEY_2, K_KP_2);
		sKeyCodeRemap.put(GLFW.GLFW_KEY_3, K_KP_3);
		sKeyCodeRemap.put(GLFW.GLFW_KEY_4, K_KP_4);
		sKeyCodeRemap.put(GLFW.GLFW_KEY_5, K_KP_5);
		sKeyCodeRemap.put(GLFW.GLFW_KEY_6, K_KP_6);
		sKeyCodeRemap.put(GLFW.GLFW_KEY_7, K_KP_7);
		sKeyCodeRemap.put(GLFW.GLFW_KEY_8, K_KP_8);
		sKeyCodeRemap.put(GLFW.GLFW_KEY_9, K_KP_9);
	}

	private KeyEventListener keyEvent;
	private TouchEventListener mouseEvent;
	private WindowEventListener windowEvent;

	private final NvPointerEvent[] touchPoint = new NvPointerEvent[1];
	private final HashMap<Integer, Integer> keyChars = new HashMap<>();
//	IntIntMap keyChars = new IntIntMap();

	private int currKey;
	private int keyAction;
	private final boolean[] buttonState = new boolean[8];
	private int mouseX, mouseY;
	private int lastX, lastY;
	
	public InputAdapter() {
		this(null, null, null);
	}
	
	public InputAdapter(KeyEventListener keyEvent, TouchEventListener mouseEvent) {
		this(keyEvent, mouseEvent, null);
	}
	
	public InputAdapter(KeyEventListener keyEvent, TouchEventListener mouseEvent, WindowEventListener windowEvent) {
		this.keyEvent = keyEvent;
		this.mouseEvent = mouseEvent;
		this.windowEvent = windowEvent;
		touchPoint[0] = new NvPointerEvent();
	}

	@Override
	public void windowPos(int xpos, int ypos) {
		if(windowEvent != null){
			windowEvent.windowPos(xpos, ypos);
		}
	}

	@Override
	public void windowClose() {
		if(windowEvent != null){
			windowEvent.windowClose();
		}
	}

	@Override
	public void windowFocus(boolean focused) {
		if(windowEvent != null){
			windowEvent.windowFocus(focused);
		}
	}

	@Override
	public void windowIconify(boolean iconified) {
		if(windowEvent != null){
			windowEvent.windowIconify(iconified);
		}
	}
	
	public static boolean isPrintableKey(int key){
		if(key == -1)  // Unkown key
			return false;
		
		return key < 0x100;
	}

	@Override
	public void key(final int _key, int scancode, int action, int mods) {
		keyAction = action;
		currKey = _key;
//		char keyChar = (char)keyChars.remove(key, 0);
		char keyChar = 0;
		Integer value = keyChars.remove(_key);
		if(value != null){
			keyChar = (char)value.intValue();
		}

		final Integer key = sKeyCodeRemap.get(_key);

		if(key == null) {
			return;
		}

		if(keyEvent != null){
			switch (action) {
			case GLFW.GLFW_PRESS:
				if(!isPrintableKey(_key)){
					keyEvent.keyPressed(key, keyChar);
				}
				break;
			case GLFW.GLFW_REPEAT:
				if(!isPrintableKey(_key)){
					keyEvent.keyTyped(key, keyChar);
				}
				break;
			case GLFW.GLFW_RELEASE:
				if(!isPrintableKey(_key)){
					keyEvent.keyReleased(key, keyChar);
				}else{
					keyEvent.keyReleased(key, keyChar);
				}
				break;
			default:
				break;
			}
		}
	}

	@Override
	public void charMods(int codepoint, int mods) {
		if(keyEvent != null){
			Integer key = sKeyCodeRemap.get(currKey);

			if(key == null) {
				return;
			}
			switch (keyAction) {
			case GLFW.GLFW_PRESS:
				keyEvent.keyPressed(key, (char)codepoint);
				break;
			case GLFW.GLFW_REPEAT:
				keyEvent.keyTyped(key, (char)codepoint);
				break;
			default:
				break;
			}
		}

		// Only record the valid
		keyChars.put(currKey, codepoint);
	}

	@Override
	public void mouseButton(int button, int action, int mods) {
		switch (action) {
		case GLFW.GLFW_PRESS:
			buttonState[button] = true;
			if(mouseEvent != null){
				touchPoint[0].m_x = mouseX;
				touchPoint[0].m_y = mouseY;
				touchPoint[0].m_id = 1 << button;

				mouseEvent.touchPressed(NvInputDeviceType.MOUSE, 1,touchPoint);
			}
			break;
		case GLFW.GLFW_RELEASE:
			buttonState[button] = false;
			if(mouseEvent != null){
				touchPoint[0].m_x = mouseX;
				touchPoint[0].m_y = mouseY;
				touchPoint[0].m_id = 1 << button;
				mouseEvent.touchReleased(NvInputDeviceType.MOUSE, 1,touchPoint);
			}
			break;
		default:
			break;
		}
	}

	@Override
	public void cursorPos(double xpos, double ypos) {
		mouseX = (int)xpos;
		mouseY = (int)ypos;
		
		if(mouseEvent != null){
			int bits = 0;
			for(int i = 0; i < buttonState.length; i++){
				if(buttonState[i]){
					bits |= 1<<i;
				}
			}

			touchPoint[0].m_x = mouseX;
			touchPoint[0].m_y = mouseY;
			touchPoint[0].m_id = bits;

			mouseEvent.touchMoved(NvInputDeviceType.MOUSE, 1,touchPoint);
		}
		
		lastX = mouseX;
		lastY = mouseY;
	}

	@Override
	public void cursorEnter(boolean entered) {
		if(mouseEvent != null){
			mouseEvent.cursorEnter(entered);
		}
	}

	@Override
	public void scroll(double xoffset, double yoffset) {
		if(mouseEvent != null){
			mouseEvent.scrolled((int)yoffset);
		}
	}

	public KeyEventListener getKeyEventListener() {
		return keyEvent;
	}

	public void setKeyEventListener(KeyEventListener keyEvent) {
		this.keyEvent = keyEvent;
	}

	public TouchEventListener getMouseEventListener() {
		return mouseEvent;
	}

	public void setMouseEventListener(TouchEventListener mouseEvent) {
		this.mouseEvent = mouseEvent;
	}
	
	public WindowEventListener getWindowEventListener() {
		return windowEvent;
	}

	public void setWindowEventListener(WindowEventListener windowEvent) {
		this.windowEvent = windowEvent;
	}

}
