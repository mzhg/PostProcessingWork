package jet.opengl.desktop.jogl;

import com.nvidia.developer.opengl.app.KeyEventListener;
import com.nvidia.developer.opengl.app.NvInputDeviceType;
import com.nvidia.developer.opengl.app.NvKey;
import com.nvidia.developer.opengl.app.NvKeyActionType;
import com.nvidia.developer.opengl.app.NvPointerActionType;
import com.nvidia.developer.opengl.app.NvPointerEvent;
import com.nvidia.developer.opengl.app.TouchEventListener;
import com.nvidia.developer.opengl.app.WindowEventListener;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import jet.opengl.postprocessing.util.LogUtil;
import jet.opengl.postprocessing.util.Pool;

/**
 * Created by mazhen'gui on 2017/10/12.
 */
@Deprecated
final class JoglInputAdapter implements MouseMotionListener, MouseListener, KeyListener, NvKey {
    private static final HashMap<Integer, Integer> sKeyCodeRemap = new HashMap<Integer, Integer>(128);
    static{
        sKeyCodeRemap.put(KeyEvent.VK_ESCAPE, K_ESC);
        sKeyCodeRemap.put(KeyEvent.VK_F1, K_F1);
        sKeyCodeRemap.put(KeyEvent.VK_F2, K_F2);
        sKeyCodeRemap.put(KeyEvent.VK_F3, K_F3);
        sKeyCodeRemap.put(KeyEvent.VK_F4, K_F4);
        sKeyCodeRemap.put(KeyEvent.VK_F5, K_F5);
        sKeyCodeRemap.put(KeyEvent.VK_F6, K_F6);
        sKeyCodeRemap.put(KeyEvent.VK_F7, K_F7);
        sKeyCodeRemap.put(KeyEvent.VK_F8, K_F8);
        sKeyCodeRemap.put(KeyEvent.VK_F9, K_F9);
        sKeyCodeRemap.put(KeyEvent.VK_F10, K_F10);
        sKeyCodeRemap.put(KeyEvent.VK_F11, K_F11);
        sKeyCodeRemap.put(KeyEvent.VK_F12, K_F12);
        sKeyCodeRemap.put(KeyEvent.VK_PRINTSCREEN, K_PRINT_SCREEN);
        sKeyCodeRemap.put(KeyEvent.VK_SCROLL_LOCK, K_SCROLL_LOCK);
        sKeyCodeRemap.put(KeyEvent.VK_PAUSE, K_PAUSE);
        sKeyCodeRemap.put(KeyEvent.VK_INSERT, K_INSERT);
        sKeyCodeRemap.put(KeyEvent.VK_DELETE, K_DELETE);
        sKeyCodeRemap.put(KeyEvent.VK_HOME, K_HOME);
        sKeyCodeRemap.put(KeyEvent.VK_END, K_END);
        sKeyCodeRemap.put(KeyEvent.VK_PAGE_UP, K_PAGE_UP);
        sKeyCodeRemap.put(KeyEvent.VK_PAGE_DOWN, K_PAGE_DOWN);
        sKeyCodeRemap.put(KeyEvent.VK_UP, K_ARROW_UP);
        sKeyCodeRemap.put(KeyEvent.VK_DOWN, K_ARROW_DOWN);
        sKeyCodeRemap.put(KeyEvent.VK_LEFT, K_ARROW_LEFT);
        sKeyCodeRemap.put(KeyEvent.VK_RIGHT, K_ARROW_RIGHT);
//		sKeyCodeRemap.put(KeyEvent.VK_ACCENT_GRAVE, K_ACCENT_GRAVE);
        sKeyCodeRemap.put(KeyEvent.VK_0, K_0);
        sKeyCodeRemap.put(KeyEvent.VK_1, K_1);
        sKeyCodeRemap.put(KeyEvent.VK_2, K_2);
        sKeyCodeRemap.put(KeyEvent.VK_3, K_3);
        sKeyCodeRemap.put(KeyEvent.VK_4, K_4);
        sKeyCodeRemap.put(KeyEvent.VK_5, K_5);
        sKeyCodeRemap.put(KeyEvent.VK_6, K_6);
        sKeyCodeRemap.put(KeyEvent.VK_7, K_7);
        sKeyCodeRemap.put(KeyEvent.VK_8, K_8);
        sKeyCodeRemap.put(KeyEvent.VK_9, K_9);
        sKeyCodeRemap.put(KeyEvent.VK_MINUS, K_MINUS);
        sKeyCodeRemap.put(KeyEvent.VK_EQUALS, K_EQUAL);
        sKeyCodeRemap.put(KeyEvent.VK_BACK_SLASH, K_BACKSPACE);
        sKeyCodeRemap.put(KeyEvent.VK_TAB, K_TAB);
        sKeyCodeRemap.put(KeyEvent.VK_Q, K_Q);
        sKeyCodeRemap.put(KeyEvent.VK_W, K_W);
        sKeyCodeRemap.put(KeyEvent.VK_E, K_E);
        sKeyCodeRemap.put(KeyEvent.VK_R, K_R);
        sKeyCodeRemap.put(KeyEvent.VK_T, K_T);
        sKeyCodeRemap.put(KeyEvent.VK_Y, K_Y);
        sKeyCodeRemap.put(KeyEvent.VK_U, K_U);
        sKeyCodeRemap.put(KeyEvent.VK_I, K_I);
        sKeyCodeRemap.put(KeyEvent.VK_O, K_O);
        sKeyCodeRemap.put(KeyEvent.VK_P, K_P);
        sKeyCodeRemap.put(KeyEvent.VK_BRACELEFT, K_LBRACKET);
        sKeyCodeRemap.put(KeyEvent.VK_BRACERIGHT, K_RBRACKET);
        sKeyCodeRemap.put(KeyEvent.VK_BACK_SLASH, K_BACKSLASH);
        sKeyCodeRemap.put(KeyEvent.VK_CAPS_LOCK, K_CAPS_LOCK);
        sKeyCodeRemap.put(KeyEvent.VK_A, K_A);
        sKeyCodeRemap.put(KeyEvent.VK_S, K_S);
        sKeyCodeRemap.put(KeyEvent.VK_D, K_D);
        sKeyCodeRemap.put(KeyEvent.VK_F, K_F);
        sKeyCodeRemap.put(KeyEvent.VK_G, K_G);
        sKeyCodeRemap.put(KeyEvent.VK_H, K_H);
        sKeyCodeRemap.put(KeyEvent.VK_J, K_J);
        sKeyCodeRemap.put(KeyEvent.VK_K, K_K);
        sKeyCodeRemap.put(KeyEvent.VK_L, K_L);
        sKeyCodeRemap.put(KeyEvent.VK_SEMICOLON, K_SEMICOLON);
//        sKeyCodeRemap.put(KeyEvent.VK_, K_APOSTROPHE);
        sKeyCodeRemap.put(KeyEvent.VK_ENTER, K_ENTER);
        sKeyCodeRemap.put(KeyEvent.VK_SHIFT, K_SHIFT_LEFT);
        sKeyCodeRemap.put(KeyEvent.VK_Z, K_Z);
        sKeyCodeRemap.put(KeyEvent.VK_X, K_X);
        sKeyCodeRemap.put(KeyEvent.VK_C, K_C);
        sKeyCodeRemap.put(KeyEvent.VK_V, K_V);
        sKeyCodeRemap.put(KeyEvent.VK_B, K_B);
        sKeyCodeRemap.put(KeyEvent.VK_N, K_N);
        sKeyCodeRemap.put(KeyEvent.VK_M, K_M);
        sKeyCodeRemap.put(KeyEvent.VK_COMMA, K_COMMA);
        sKeyCodeRemap.put(KeyEvent.VK_PERIOD, K_PERIOD);
        sKeyCodeRemap.put(KeyEvent.VK_SLASH, K_SLASH);
        sKeyCodeRemap.put(KeyEvent.VK_SHIFT, K_SHIFT_RIGHT);
        sKeyCodeRemap.put(KeyEvent.VK_CONTROL, K_CTRL_LEFT);
        sKeyCodeRemap.put(KeyEvent.VK_WINDOWS, K_WINDOWS_LEFT);
        sKeyCodeRemap.put(KeyEvent.VK_ALT, K_ALT_LEFT);
        sKeyCodeRemap.put(KeyEvent.VK_SPACE, K_SPACE);
        sKeyCodeRemap.put(KeyEvent.VK_ALT, K_ALT_RIGHT);
        sKeyCodeRemap.put(KeyEvent.VK_WINDOWS, K_WINDOWS_RIGHT);
//		sKeyCodeRemap.put(KeyEvent.VK_, K_CONTEXT); TODO
        sKeyCodeRemap.put(KeyEvent.VK_CONTROL, K_CTRL_RIGHT);
        sKeyCodeRemap.put(KeyEvent.VK_NUM_LOCK, K_NUMLOCK);
        sKeyCodeRemap.put(KeyEvent.VK_DIVIDE, K_KP_DIVIDE);
        sKeyCodeRemap.put(KeyEvent.VK_MULTIPLY, K_KP_MULTIPLY);
        sKeyCodeRemap.put(KeyEvent.VK_SUBTRACT, K_KP_SUBTRACT);
        sKeyCodeRemap.put(KeyEvent.VK_ADD, K_KP_ADD);
        sKeyCodeRemap.put(KeyEvent.VK_ENTER, K_KP_ENTER);
        sKeyCodeRemap.put(KeyEvent.VK_DECIMAL, K_KP_DECIMAL);
        sKeyCodeRemap.put(KeyEvent.VK_0, K_KP_0);
        sKeyCodeRemap.put(KeyEvent.VK_1, K_KP_1);
        sKeyCodeRemap.put(KeyEvent.VK_2, K_KP_2);
        sKeyCodeRemap.put(KeyEvent.VK_3, K_KP_3);
        sKeyCodeRemap.put(KeyEvent.VK_4, K_KP_4);
        sKeyCodeRemap.put(KeyEvent.VK_5, K_KP_5);
        sKeyCodeRemap.put(KeyEvent.VK_6, K_KP_6);
        sKeyCodeRemap.put(KeyEvent.VK_7, K_KP_7);
        sKeyCodeRemap.put(KeyEvent.VK_8, K_KP_8);
        sKeyCodeRemap.put(KeyEvent.VK_9, K_KP_9);
    }

    private static final int KEY_NONE = 0;
    private static final int KEY_TYPE = 1;
    private static final int KEY_UP = 2;
    private static final int KEY_DOWN = 3;
    public static final int MAX_EVENT_COUNT = 16;
    private final Object localObj = sKeyCodeRemap;

    private KeyEventListener keyEvent;
    private TouchEventListener mouseEvent;
    private WindowEventListener windowEvent;

    private final NvPointerEvent[] touchPoint = new NvPointerEvent[MAX_EVENT_COUNT];
    private int mouseX, mouseY;
    private int pointCursor;
    private int mouseCursor;

    private final Pool<_KeyEvent> keyEventPool;
    private final List<_KeyEvent> keyEventsBuffer = new ArrayList<_KeyEvent>();
    private final List<_KeyEvent> keyEvents = new ArrayList<_KeyEvent>();
    private final Pool<NvPointerEvent> touchEventPool;
    private final List<NvPointerEvent> touchEvents = new ArrayList<>();
    private final List<NvPointerEvent> touchEventsBuffer = new ArrayList<>();
    private final NvPointerEvent[][] specialEvents = new NvPointerEvent[6][12];
    private int mainCursor;
    private final int[] subCursor = new int[6];
    private final int[] eventType = new int[6];

    public JoglInputAdapter() {
        this(null, null, null);
    }

    public JoglInputAdapter(KeyEventListener keyEvent, TouchEventListener mouseEvent) {
        this(keyEvent, mouseEvent, null);
    }

    public JoglInputAdapter(KeyEventListener keyEvent, TouchEventListener mouseEvent, WindowEventListener windowEvent) {
        this.keyEvent = keyEvent;
        this.mouseEvent = mouseEvent;
        this.windowEvent = windowEvent;

        for(int i = 0; i < touchPoint.length; i++){
            touchPoint[i] = new NvPointerEvent();
        }

        keyEventPool = new Pool<>(()->new _KeyEvent());
        touchEventPool = new Pool<>(()->new NvPointerEvent());
    }

    static boolean isPrintableKey(int code){
        return false;
    }

    public void pollEvents(){
        List<_KeyEvent> events = getKeyEvents();
        List<NvPointerEvent> pEvents = getTouchEvents();

        synchronized (localObj){
            for(int i = 0; i < events.size(); i++){
                _KeyEvent e = events.get(i);
                int code = e.keyCode;
                char keyChar = e.keyChar;
                final Integer key = sKeyCodeRemap.get(code);
                if(key == null) {
                    continue;
                }

            /*handled = mInputListener.keyInput(code, down ? NvKeyActionType.DOWN : NvKeyActionType.UP);
            if(!handled && down){
                char c = e.keyChar;
                if(c != 0)
                    mInputListener.characterInput(c);
            }*/

                if(keyEvent != null){
                    switch (e.action) {
                        case DOWN:
                            if(!isPrintableKey(code)){
                                keyEvent.keyPressed(key, keyChar);
                            }
                            break;
                        case REPEAT:
                            if(!isPrintableKey(code)){
                                keyEvent.keyTyped(key, keyChar);
                            }
                            break;
                        case UP:
                            if(!isPrintableKey(code)){
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

            if(mouseEvent == null)
                return;

            if(pEvents.size() > 0){
                splitEvents(pEvents);
                for(int i = 0; i <= mainCursor; i++){
//                    mInputListener.pointerInput(NvInputDeviceType.TOUCH, eventType[i], 0, subCursor[i], specialEvents[i]);
                    switch (eventType[i]){
                        case NvPointerActionType.DOWN:
                            mouseEvent.touchPressed(NvInputDeviceType.MOUSE, subCursor[i], specialEvents[i]);
                            break;
                        case NvPointerActionType.UP:
                            mouseEvent.touchReleased(NvInputDeviceType.MOUSE, subCursor[i], specialEvents[i]);
                            break;
                        case NvPointerActionType.MOTION:
                            mouseEvent.touchMoved(NvInputDeviceType.MOUSE, subCursor[i], specialEvents[i]);
                            break;
                    }
                }
            }
        }
    }

    private final void splitEvents(List<NvPointerEvent> pEvents){
        mainCursor = -1;
        Arrays.fill(subCursor, 0);

        int size = pEvents.size();
        int lastType = -1;
        for(int i = 0; i < size; i++){
            NvPointerEvent event = pEvents.get(i);

            if(event.type !=lastType){
                lastType = event.type;
                mainCursor ++;

                int pact = event.type;
                eventType[mainCursor] = pact;
            }

            specialEvents[mainCursor][subCursor[mainCursor] ++] = event;
        }
    }
    
    @Override
    public void keyTyped(KeyEvent e) {
        synchronized (localObj){
            _KeyEvent keyEvent = keyEventPool.obtain();
            keyEvent.keyCode = e.getKeyCode();
            keyEvent.keyChar = e.getKeyChar();
            keyEvent.action = NvKeyActionType.REPEAT;

            /*if(keyCode > 0 && keyCode <127)
                pressedKeys[keyCode] = keyEvent.down;*/

            keyEventsBuffer.add(keyEvent);
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        synchronized (localObj){
            _KeyEvent keyEvent = keyEventPool.obtain();
            keyEvent.keyCode = e.getKeyCode();
            keyEvent.keyChar = e.getKeyChar();
            keyEvent.action = NvKeyActionType.DOWN;

            /*if(keyCode > 0 && keyCode <127)
                pressedKeys[keyCode] = keyEvent.down;*/

            keyEventsBuffer.add(keyEvent);
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        synchronized (localObj){
            _KeyEvent keyEvent = keyEventPool.obtain();
            keyEvent.keyCode = e.getKeyCode();
            keyEvent.keyChar = e.getKeyChar();
            keyEvent.action = NvKeyActionType.UP;

            /*if(keyCode > 0 && keyCode <127)
                pressedKeys[keyCode] = keyEvent.down;*/

            keyEventsBuffer.add(keyEvent);
        }
    }

    @Override
    public void mouseClicked(MouseEvent mouseEvent) {}

    @Override
    public void mousePressed(MouseEvent e) {
        synchronized (localObj){
            NvPointerEvent touchEvent;
            touchEvent = touchEventPool.obtain();
            touchEvent.type = NvPointerActionType.DOWN;
            touchEvent.m_id = 1 << e.getButton();
            touchEvent.m_x = e.getX() /*touchX[pointerId] = (int) (event.getX(pointerIndex) + 0.5f)*/;
            touchEvent.m_y = e.getY() /*touchY[pointerId] = (int) (event.getY(pointerIndex) + 0.5f)*/;
//            isTouched[pointerId] = true;
            touchEventsBuffer.add(touchEvent);

            LogUtil.i(LogUtil.LogType.DEFAULT, "mousePressed: " + e.paramString());
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        synchronized (localObj){
            NvPointerEvent touchEvent;
            touchEvent = touchEventPool.obtain();
            touchEvent.type = NvPointerActionType.UP;
            touchEvent.m_id = 1 << e.getButton();
            touchEvent.m_x = e.getX() /*touchX[pointerId] = (int) (event.getX(pointerIndex) + 0.5f)*/;
            touchEvent.m_y = e.getY() /*touchY[pointerId] = (int) (event.getY(pointerIndex) + 0.5f)*/;
//            isTouched[pointerId] = true;
            touchEventsBuffer.add(touchEvent);

            LogUtil.i(LogUtil.LogType.DEFAULT, "mouseReleased: " + e.paramString());
            LogUtil.i(LogUtil.LogType.DEFAULT, "mouseReleased: " + Thread.currentThread().getName());

        }
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        if(mouseEvent != null){
            mouseEvent.cursorEnter(true);  // TODO It's not thread safe
        }
    }

    @Override
    public void mouseExited(MouseEvent e) {
        if(mouseEvent != null){
            mouseEvent.cursorEnter(false);  // TODO It's not thread safe
        }
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        synchronized (localObj){
            NvPointerEvent touchEvent;
            touchEvent = touchEventPool.obtain();
            touchEvent.type = NvPointerActionType.MOTION;
            touchEvent.m_id = 1<<e.getButton();   // TODO It's not precies.
            touchEvent.m_x = e.getX() /*touchX[pointerId] = (int) (event.getX(pointerIndex) + 0.5f)*/;
            touchEvent.m_y = e.getY() /*touchY[pointerId] = (int) (event.getY(pointerIndex) + 0.5f)*/;
//            isTouched[pointerId] = true;
            touchEventsBuffer.add(touchEvent);
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        synchronized (localObj){
            NvPointerEvent touchEvent;
            touchEvent = touchEventPool.obtain();
            touchEvent.type = NvPointerActionType.MOTION;
            touchEvent.m_id = 0;
            touchEvent.m_x = e.getX() /*touchX[pointerId] = (int) (event.getX(pointerIndex) + 0.5f)*/;
            touchEvent.m_y = e.getY() /*touchY[pointerId] = (int) (event.getY(pointerIndex) + 0.5f)*/;
//            isTouched[pointerId] = true;
            touchEventsBuffer.add(touchEvent);
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

    private static final class _KeyEvent{
        NvKeyActionType action;
        int keyCode;
        char keyChar;
        int modifiers;
    }

    private List<_KeyEvent> getKeyEvents(){
        synchronized (this) {
            int len = keyEvents.size();
            for(int i = 0; i < len; i++)
                keyEventPool.free(keyEvents.get(i));

            keyEvents.clear();
            keyEvents.addAll(keyEventsBuffer);
            keyEventsBuffer.clear();
            return keyEvents;
        }
    }

    List<NvPointerEvent> getTouchEvents(){
        synchronized (this) {
            int len = touchEvents.size();
            for(int i = 0; i < len; i++)
                touchEventPool.free(touchEvents.get(i));

            touchEvents.clear();
            touchEvents.addAll(touchEventsBuffer);
            touchEventsBuffer.clear();
            return touchEvents;
        }
    }


}
