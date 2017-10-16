package com.nvidia.developer.opengl.app;

import com.nvidia.developer.opengl.utils.NvGfxAPIVersion;

import java.util.ArrayList;
import java.util.List;

import jet.opengl.postprocessing.common.GLAPIVersion;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.util.LogUtil;

/**
 * A basic app framework, including mainloop, setup and input processing.
 * @author Nvidia 2014-9-13 12:19
 *
 */
public abstract class NvAppBase implements NvInputCallbacks, NvKey, GLEventListener, KeyEventListener, TouchEventListener, WindowEventListener{

	private List<String> commands = new ArrayList<String>();
	
	protected boolean m_requestedExit;
	private NvGLAppContext m_GLContext;

	protected final NvEGLConfiguration config = new NvEGLConfiguration();

	public void setGLContext(NvGLAppContext context) {m_GLContext = context; }
	public NvGLAppContext getGLContext() { return m_GLContext;}

	@Override
	public void onCreate() {
		GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
		GLAPIVersion version = gl.getGLAPIVersion();
		if(m_GLContext == null)
			throw new NullPointerException("GLContext is null");

		config.apiVer = NvGfxAPIVersion.queryVersion(version.ES, version.major, version.minor);
		if(config.apiVer == null){
			LogUtil.e(LogUtil.LogType.NV_FRAMEWROK, "Couldn't found the sutuable version :" + version);
			config.apiVer = NvGfxAPIVersion.GL4;
		}
		
		config.redBits = gl.glGetInteger(GLenum.GL_RED_BIAS);
		config.greenBits = gl.glGetInteger(GLenum.GL_GREEN_BITS);
		config.blueBits = gl.glGetInteger(GLenum.GL_BLUE_BITS);
		config.alphaBits = gl.glGetInteger(GLenum.GL_ALPHA_BITS);
		config.depthBits = gl.glGetInteger(GLenum.GL_DEPTH_BITS);
		config.stencilBits = gl.glGetInteger(GLenum.GL_STENCIL_BITS);
		
//		NvImage.setAPIVersion(getGLContext().getConfiguration().apiVer);
		
		initRendering();
	}
	
	
	
	/**
	 * Initialize rendering.<p>
	 * Called once the GLES context and surface have been created and bound
     * to the main thread.  Called again if the context is lost and must be
     * recreated.
	 */
	protected void initRendering(){
	}
	
	/**
	 * Shutdown rendering.<p>
	 * Called when the GLES context has just been shut down; it indicates that
     * all GL resources in the app's context have been deleted and are invalid, 
     * and will need to be recreated on the next call to initRendering.  This 
     * function should also be used to shut down any secondary threads that 
     * generate GL calls such as buffer mappings.<p>
     *
     * Because the sequence of shutdownRendering/initRendering may be called 
     * without the app being completely shut down (e.g. lost context), the app
     * needs to use this to delete non-GL resources (e.g. system memory related 
     * to 3D resources) and indicate that it needs to reload any GL resources on
     * initRendering
	 */
	protected void shutdownRendering(){}
	
	public void onDestroy() {
		shutdownRendering();
	}
	
	/*
	 * Application animation update.<p>
	 * Called at regular (frame-scale) intervals even if the app is not
     * focused.  Always called directly before a render, but will be called
     * without a render if the app is not focused.  Optional.
	 */
	protected void update(float dt){

	}
	
	/**
	 * Rendering callback.<p>
	 * Called to request the app render a frame at regular intervals when
     * the app is focused or when force by outside events like a resize
	 */
	public void display() {}
	

	/**
	 * Resize callback.<p>
	 * Called when the main rendering window changes dimensions and before
	 * @param width the new window width in pixels
	 * @param height the new window height in pixels
	 */
	protected void reshape(int width, int height){
		
	}
	
	@Override
	public void onResize(int width, int height) {
		reshape(width, height);
	}
	

	/**
	 * GL configuration request callback.<p>
	 * This function passes in the default set of GL configuration requests.<br>
	 * The app can override this function and change the defaults before
     * returning.  These are still requests, and may not be met.  If the
     * platform supports requesting GL options, this function will be called
     * before initGL.  Optional.
	 * @param config the default config to be used is passed in.  If the application
	 *  wishes anything different in the GL configuration, it should change those values before
	 *  returning from the function.  These are merely requests.
	 */
	public void configurationCallback(NvEGLConfiguration config){}
	

	/** return true if the application is in the process of exiting, false if not. */
	public boolean isExiting(){
		return m_requestedExit;
	}
	
	@Override
	public final void keyPressed(int keycode, char keychar) {
		boolean handled = false;
		int nvkey = keycode;
		if (nvkey == K_UNKNOWN)
	        return;
		
        handled = keyInput(nvkey, NvKeyActionType.DOWN);
        
     // Send as a key event; if that is rejected, then send it as a char
        if(!handled){
        	switch (nvkey) {
            case K_ESC:
            	getGLContext().requestExit();
                break;
            default:
                characterInput((char) nvkey);
                return;
            }
        }
	}
	
	@Override
	public final void keyTyped(int keycode, char keychar) {
		boolean handled = false;
		int nvkey = keycode;

		if (nvkey == K_UNKNOWN)
	        return;
		
        handled = keyInput(nvkey, NvKeyActionType.REPEAT);
        
     // Send as a key event; if that is rejected, then send it as a char
        if(!handled){
        	switch (nvkey) {
            case K_ESC:
				getGLContext().requestExit();
                break;
            default:
                characterInput((char) nvkey);
                return;
            }
        }
	}

	@Override
	public final void keyReleased(int keycode, char keychar) {
		int nvkey = keycode;

		if (nvkey == K_UNKNOWN)
	        return;
		
        keyInput(nvkey, NvKeyActionType.UP);
	}
	
	@Override
	public final void touchPressed(NvInputDeviceType type, int count, NvPointerEvent[] pointer_events) {
		pointerInput(type, NvPointerActionType.DOWN, 0, count, pointer_events);
	}
	
	@Override
	public final void touchReleased(NvInputDeviceType type, int count, NvPointerEvent[] pointer_events) {
		pointerInput(type, NvPointerActionType.UP, 0, count, pointer_events);
	}
	
	@Override
	public final void touchMoved(NvInputDeviceType type, int count, NvPointerEvent[] pointer_events) {
		pointerInput(type, NvPointerActionType.MOTION, 0, count, pointer_events);
	}

	@Override
	public boolean pointerInput(NvInputDeviceType device, int action, int modifiers,
			int count, NvPointerEvent[] points) {
		return false;
	}

	@Override
	public boolean keyInput(int code, NvKeyActionType action) {
		return false;
	}

	@Override
	public boolean characterInput(char c) {
		return false;
	}

	@Override
	public boolean gamepadChanged(int changedPadFlags) {
		return false;
	}
	
	public boolean handlePointerInput(NvInputDeviceType device, int action, int modifiers,
			int count, NvPointerEvent[] points) {
		return false;
	}

	public boolean handleKeyInput(int code, NvKeyActionType action) {
		return false;
	}

	public boolean handleCharacterInput(char c) {
		return false;
	}

	@Override
	public void scrolled(int wheel) {
		
	}

	@Override
	public void draw() {}

	@Override
	public void windowPos(int xpos, int ypos) {

	}

	@Override
	public void windowClose() {

	}

	@Override
	public void windowFocus(boolean focused) {

	}

	@Override
	public void windowIconify(boolean iconified) {

	}
}
