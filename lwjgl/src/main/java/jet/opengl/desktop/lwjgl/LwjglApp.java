package jet.opengl.desktop.lwjgl;

import com.nvidia.developer.opengl.app.GLEventListener;
import com.nvidia.developer.opengl.app.NvEGLConfiguration;
import com.nvidia.developer.opengl.app.NvGLAppContext;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWCharModsCallback;
import org.lwjgl.glfw.GLFWCursorEnterCallback;
import org.lwjgl.glfw.GLFWCursorPosCallback;
import org.lwjgl.glfw.GLFWFramebufferSizeCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWMouseButtonCallback;
import org.lwjgl.glfw.GLFWScrollCallback;
import org.lwjgl.glfw.GLFWWindowCloseCallback;
import org.lwjgl.glfw.GLFWWindowFocusCallback;
import org.lwjgl.glfw.GLFWWindowIconifyCallback;
import org.lwjgl.glfw.GLFWWindowPosCallback;
import org.lwjgl.glfw.GLFWWindowSizeCallback;
import org.lwjgl.opengl.GL;

import java.util.ArrayList;
import java.util.List;

import jet.opengl.postprocessing.common.GLAPI;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;

public class LwjglApp implements NvGLAppContext{

	private static final int DEFAULT_WIDTH = 1280;
	private static final int DEFAULT_HEIGHT = 720;
	
//	private MultiWindowCallBacks callBacks = new MultiWindowCallBacks();
	private List<GLFWListener> windowCallbacks = new ArrayList<GLFWListener>();
	private List<GLEventListener> glListeners = new ArrayList<GLEventListener>();  // TODO has lots of bug.
	private SafeCollection<Runnable> tasks = new SafeCollection<>(new ArrayList<Runnable>());
	
	/** The window handler. */
	private long window;
	private CharSequence title;
	protected int loopCount; //only for debug
	private transient boolean running;
	
	private final GLContextConfig config = new GLContextConfig();
	
	private Monitor monitor;
	private boolean fullScreenMode;
	
	// the dimension of the window
	private int width = DEFAULT_WIDTH, height = DEFAULT_HEIGHT;
	private boolean resizeable = false;
	private float fps;
	private float desirFPS;
	private long delayTime, lastTime; // in millions
	private int fpsCount;
	private long startFPSTime;
	private boolean showFPS;
	private boolean glfwInited = false;
    private boolean windowCreated = false;
    private boolean needCreateWindow = false;
    
    // keep the reference of callbacks. To prevent the GC to free the callbacks momery.
    private final Object[] callbackRefs = new Object[12];
	
	public LwjglApp() {
	}
	
	public LwjglApp(CharSequence title) {
		this.title = title;
	}
	
	public void setTitle(CharSequence title){
		if(title == null)
			title = "";
		
		this.title = title;
		if(window != 0 && !showFPS){
			GLFW.glfwSetWindowTitle(window, title);
		}else{
			// nothing need to do.
		}
	}
	
	public CharSequence getTitle() {return title == null ? "" : title;}
	
    public long getWindow() {return window; }
    
    public void setGLContextConfig(GLContextConfig config){
    }
    
    public boolean setSwapInterval(int interval){
    	if(window != 0){
    		GLFW.glfwSwapInterval(interval);
			return true;
    	}

		return false;
    }

	@Override
	public int width() {
		return getWidth();
	}

	@Override
	public int height() {
		return getHeight();
	}

	@Override
	public NvEGLConfiguration getConfiguration() {
		return null;
	}

	@Override
	public void requestExit() {
		exit();
	}

	@Override
	public void setAppTitle(String title) {
		setTitle(title);
	}

	@Override
	public void showDialog(String msg, String errorStr) {

	}

	@Override
	public String getAppTitle() {
		return getTitle().toString();
	}

	/**
     * Return the reference of the GLContextConfig
     * @return
     */
    public GLContextConfig getGLContextConfig(){
    	return config;
    }
    
    public boolean isRunning() { return running; }

    public void start(){
    	running = true;
    	boolean lwjglInited = false;
    	
    	while(running){
    		if(!glfwInited){
//    			GLFW.glfwSetErrorCallback(Callbacks.errorCallbackPrint(System.err));
    			
    			if ( !GLFW.glfwInit())
    	            throw new IllegalStateException("Unable to initialize GLFW");
    			
    			glfwInited = true;
    		}
    		
    		boolean needResize = false;
    		if(needCreateWindow || !windowCreated){ // re-create window.
				if(windowCreated){
					GLFW.glfwDestroyWindow(window);
				}
				
				GLFW.glfwDefaultWindowHints();
				GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, resizeable ? 1 : 0);
				
				GLFW.glfwWindowHint(GLFW.GLFW_RED_BITS, config.redBits);
				GLFW.glfwWindowHint(GLFW.GLFW_GREEN_BITS, config.greenBits);
				GLFW.glfwWindowHint(GLFW.GLFW_BLUE_BITS, config.blueBits);
				GLFW.glfwWindowHint(GLFW.GLFW_ALPHA_BITS, config.alphaBits);
				
				GLFW.glfwWindowHint(GLFW.GLFW_DEPTH_BITS, config.depthBits);
				GLFW.glfwWindowHint(GLFW.GLFW_STENCIL_BITS, config.stencilBits);
				
				GLFW.glfwWindowHint(GLFW.GLFW_AUX_BUFFERS, config.auxBuffers);
				GLFW.glfwWindowHint(GLFW.GLFW_SAMPLES, config.multiSamplers);
				
				GLFW.glfwWindowHint(GLFW.GLFW_STEREO, config.stereo ? 1 : 0);
				GLFW.glfwWindowHint(GLFW.GLFW_SRGB_CAPABLE, config.sRGBCapable ? 1 : 0);
				
				GLFW.glfwWindowHint(GLFW.GLFW_CLIENT_API, config.clientAPI);
				GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_DEBUG_CONTEXT, config.debugContext ? 1 : 0);
				GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, config.glProfile);
				
				if(config.clientAPI == GLFW.GLFW_OPENGL_ES_API){
					GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, config.esMajor);
					GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, config.esMinor);
				}
				
				if(fullScreenMode){
					if(monitor == null) monitor = Monitor.getPrimaryMonitor();
				}
				
				if(title == null)
					title = getClass().getSimpleName();
				
				// Create the window
		        window = GLFW.glfwCreateWindow(width, height, title, monitor == null ? 0 :monitor.getHandler(), 0);
		        if ( window == 0 )
		            throw new RuntimeException("Failed to create the GLFW window");
		        windowCreated = true;
		        
		        if(!fullScreenMode){
		        	VideoMode video = Monitor.getPrimaryMonitor().getVideoMode();
		        	
		        	// Center our window
			        GLFW.glfwSetWindowPos(
			            window,
			            (video.width - width) / 2,
			            (video.height - height) / 2
			        );
		        }
		        
		     // Make the OpenGL context current
		        GLFW.glfwMakeContextCurrent(window);
		        
		        // Make the window visible
		        GLFW.glfwShowWindow(window);
		        setupCallbacks();
		        
		        needCreateWindow = false;
		        needResize = true;
			}
    		
    		if(!lwjglInited){
    			lwjglInited = true;
    			
    			// This line is critical for LWJGL's interoperation with GLFW's
    	        // OpenGL context, or any context that is managed externally.
    	        // LWJGL detects the context that is current in the current thread,
    	        // creates the ContextCapabilities instance and makes the OpenGL
    	        // bindings available for use.
    	        // GLContext.createFromCurrent();
    			GL.createCapabilities();
				GLFuncProviderFactory.initlizeGLFuncProvider(GLAPI.LWJGL, null);
    	        for(int i = 0; i < glListeners.size(); i++){
    	        	glListeners.get(i).onCreate();
    	        }
    		}
    		
    		if(needResize){
    			needResize = false;
    			for(int i = 0; i < glListeners.size(); i++){
    				if(running)
    					glListeners.get(i).onResize(width, height);
    	        }
    		}
    		
    		block();
    		renderFrame();
        	 
            // Poll for window events. The key callback above will only be
            // invoked during this call.
            GLFW.glfwPollEvents();
            
            if(GLFW.glfwWindowShouldClose(window)){
            	running = false;
            	break;
            }
    	}
    	
    	for(int i = 0; i < glListeners.size(); i++){
        	glListeners.get(i).onDestroy();
        }
        
        // Release window and window callbacks
        GLFW.glfwDestroyWindow(window);
        // Terminate GLFW and release the GLFWerrorfun
    	GLFW.glfwTerminate();
    }
    
    protected void renderFrame(){
    	for(int i = 0; i < glListeners.size(); i++){
    		if(running)
    			glListeners.get(i).draw();
        }
    	
    	if(!tasks.isEmpty()){
	    	for(Runnable r : tasks){
	    		r.run();
	    	}
	    	
	    	tasks.clear();
    	}
    	
    	GLFW.glfwSwapBuffers(window); // swap the color buffers
    }
    
    public void runTask(Runnable r){
    	if(r == null)
    		return;
    	tasks.add(r);
    }
    
    private final void block(){
    	if(showFPS){
    		if(fpsCount == 0){
    			startFPSTime = System.currentTimeMillis();
    		}else /*if(fpsCount == 100)*/{
    			long delay = System.currentTimeMillis() - startFPSTime;
    			if(delay > 1000){
    				float sec = delay * 0.001f;
	    			fps = fpsCount/sec;
//	    			if(fps < 0.5f) fps = 0;
	    			String fps_str = Float.toString(fps);
	    			int index = fps_str.indexOf('.');
	    			if(index > 0){
	    				fps_str = fps_str.substring(0, Math.min(index + 3, fps_str.length()));
	    			}
	    			
	    			GLFW.glfwSetWindowTitle(window, title + ": " + fps_str);
	    			fpsCount = 0;
	    			startFPSTime = System.currentTimeMillis();
    			}
    		}
    		
    		fpsCount++;    		
    	}
    	
    	if(delayTime > 0 ){
    		long currTime = System.currentTimeMillis();
    		long sleepTime = delayTime - (currTime - lastTime);
    		if(sleepTime > 0){
    			try {
					Thread.sleep(sleepTime);
				} catch (InterruptedException e) {
					// Ignore this case
				}
    		}
    		
    		lastTime = System.currentTimeMillis();
    	}
    }
    
	public void registerGLFWListener(GLFWListener listener){
		if(listener != null) windowCallbacks.add(listener);
	}
	
	public void toggleFullScreen(){
		if(!fullScreenMode ||(fullScreenMode && !monitor.isPrimary())){
			fullScreenMode = true;
			monitor = Monitor.getPrimaryMonitor();
			needCreateWindow = true;
		}
	}
	
	public void toggleFullScreen(Monitor target){
		if(target == null){
			toggleFullScreen();
			return;
		}
		
		if(!fullScreenMode ||(fullScreenMode && monitor != target)){
			fullScreenMode = true;
			monitor = target;
			needCreateWindow = true;
		}
	}
	
	public void toggleWindowed(){
		if(fullScreenMode){
			fullScreenMode = false;
			monitor = null;
			needCreateWindow = true;
		}
	}
	
	public boolean isFullScreen(){ return fullScreenMode;}
	
	public boolean isResizeable(){ return resizeable;}
	
	public boolean unregisterGLFWListener(GLFWListener listener){
		return windowCallbacks == null ? false : windowCallbacks.remove(listener);
	}
	
	public void registerGLEventListener(GLEventListener listener){
		if(listener != null) glListeners.add(listener);
	}
	
	public void registerGLEventListener(int index, GLEventListener listener){
		if(listener != null) {
			System.out.println("---------1");
			glListeners.add(index, listener);
			System.out.println("---------2");
		}
	}
	
	public boolean unregisterGLEventListener(GLEventListener listener){
		return listener == null ? false : glListeners.remove(listener);
	}
	
	public int getGLEventListenersCount() { return glListeners.size();}
	
	public final void setWidth(int width){
		if(window == 0)
			this.width = width;
		else
			GLFW.glfwSetWindowSize(window, width, height);
	}
	
	public final void setHeight(int height){
		if(window == 0)
			this.height = height;
		else
			GLFW.glfwSetWindowSize(window, width, height);
	}
	
	public final void setWindowSize(int width, int height){
		if(window == 0){
			this.width = width;
			this.height = height;
		}else{
			GLFW.glfwSetWindowSize(window, width, height);
		}
	}
	
	public void setTile(CharSequence title){
		if(window != 0){
			GLFW.glfwSetWindowTitle(window, title);
		}else{
			this.title = title;
		}
	}
	
	public final int getWidth(){ return width;}
	public final int getHeight(){ return height;}


	public void exit(){
		running = false;
	}
	
	void setupCallbacks(){
		int count = 0;
		GLFW.glfwSetWindowCloseCallback(window, (GLFWWindowCloseCallback) (callbackRefs[count++] = new GLFWWindowCloseCallback() {
			public void invoke(long window) {
				for(int i = 0; i < windowCallbacks.size(); i++)
					windowCallbacks.get(i).windowClose();
			}
		}));
		
		GLFW.glfwSetWindowFocusCallback(window, (GLFWWindowFocusCallback) (callbackRefs[count++] = new GLFWWindowFocusCallback() {
			@Override
			public void invoke(long window, boolean focused) {
				for(int i = 0; i < windowCallbacks.size(); i++)
					windowCallbacks.get(i).windowFocus(focused);
			}

//			public void callback(long args) {}
//			public void close() {}
		}));
		
		GLFW.glfwSetWindowIconifyCallback(window, (GLFWWindowIconifyCallback) (callbackRefs[count++] = new GLFWWindowIconifyCallback() {
			public void invoke(long window, boolean iconified) {
				for(int i = 0; i < windowCallbacks.size(); i++)
					windowCallbacks.get(i).windowIconify(iconified);
			}
		}));
		
		GLFW.glfwSetWindowPosCallback(window, (GLFWWindowPosCallback) (callbackRefs[count++] = new GLFWWindowPosCallback(){
			public void invoke(long window, int xpos, int ypos) {
				for(int i = 0; i < windowCallbacks.size(); i++)
					windowCallbacks.get(i).windowPos(xpos, ypos);
			}
		}));
		
		GLFW.glfwSetWindowRefreshCallback(window, null);
		GLFW.glfwSetCharCallback(window, null);
		
		GLFW.glfwSetCharModsCallback(window, (GLFWCharModsCallback) (callbackRefs[count++] = new GLFWCharModsCallback() {
			public void invoke(long window, int codepoint, int mods) {
				for(int i = 0; i < windowCallbacks.size(); i++)
					windowCallbacks.get(i).charMods(codepoint, mods);
			}
		}));
		
		GLFW.glfwSetCursorEnterCallback(window, (GLFWCursorEnterCallback) (callbackRefs[count++] = new GLFWCursorEnterCallback() {
			public void invoke(long window, boolean entered) {
				for(int i = 0; i < windowCallbacks.size(); i++)
					windowCallbacks.get(i).cursorEnter(entered);
			}
		}));
		
		GLFW.glfwSetCursorPosCallback(window, (GLFWCursorPosCallback) (callbackRefs[count++] = new GLFWCursorPosCallback() {
			public void invoke(long window, double xpos, double ypos) {
				for(int i = 0; i < windowCallbacks.size(); i++)
					windowCallbacks.get(i).cursorPos(xpos, ypos);
			}
		}));
		
		GLFW.glfwSetKeyCallback(window, (GLFWKeyCallback) (callbackRefs[count++] = new GLFWKeyCallback() {
			
			@Override
			public void invoke(long window, int key, int scancode, int action, int mods) {
				for(int i = 0; i < windowCallbacks.size(); i++)
					windowCallbacks.get(i).key(key, scancode, action, mods);
			}
		}));
		
		GLFW.glfwSetMouseButtonCallback(window, (GLFWMouseButtonCallback) (callbackRefs[count++] = new GLFWMouseButtonCallback() {
			public void invoke(long window, int button, int action, int mods) {
				for(int i = 0; i < windowCallbacks.size(); i++)
					windowCallbacks.get(i).mouseButton(button, action, mods);
			}
		}));
		
		GLFW.glfwSetScrollCallback(window, (GLFWScrollCallback) (callbackRefs[count++] = new GLFWScrollCallback() {
			public void invoke(long window, double xoffset, double yoffset) {
				for(int i = 0; i < windowCallbacks.size(); i++)
					windowCallbacks.get(i).scroll(xoffset, yoffset);
			}
		}));
		
		GLFW.glfwSetWindowSizeCallback(window, (GLFWWindowSizeCallback) (callbackRefs[count++] = new GLFWWindowSizeCallback() {
			public void invoke(long window, int width, int height) {
				/*BaseApp.this.width = width;
				BaseApp.this.height = height;
				
//				for(int i = 0; i < windowCallbacks.size(); i++)
//					windowCallbacks.get(i).windowSize(width, height);
				
				for(int i = 0; i < glListeners.size(); i++){
		        	glListeners.get(i).onResize(width, height);
		        }*/
			}
		}));
		
		GLFW.glfwSetFramebufferSizeCallback(window, (GLFWFramebufferSizeCallback) (callbackRefs[count++] = new GLFWFramebufferSizeCallback() {
			public void invoke(long window, int width, int height) {
				LwjglApp.this.width = width;
				LwjglApp.this.height = height;
				
//				for(int i = 0; i < windowCallbacks.size(); i++)
//					windowCallbacks.get(i).windowSize(width, height);
				
				for(int i = 0; i < glListeners.size(); i++){
		        	glListeners.get(i).onResize(width, height);
		        }
			}
		}));
	}

	public void setResizeable(boolean resizeable) {
		needCreateWindow = this.resizeable != resizeable;
		
		this.resizeable = resizeable;
		
		System.out.println("needCreateWindow: " + needCreateWindow );
	}

	public float getDesirFPS() {
		return desirFPS;
	}

	public void setDesirFPS(float desirFPS) {
		this.desirFPS = desirFPS;
		
		if(desirFPS > 0){
			delayTime = (long) (1000.0f/desirFPS);
		}else{
			delayTime = 0;
		}
	}

	public boolean isShowFPS() {
		return showFPS;
	}
	
	public float getCurrentFPS() { return fps; }

	public void setShowFPS(boolean showFPS) {
		if(this.showFPS != showFPS){
			this.showFPS = showFPS;
			if(showFPS) // turn on fps.
				fpsCount = 0;
			else{  // turn off fps
				if(window != 0){
					GLFW.glfwSetWindowTitle(window, title);
				}
			}
		}
	}
}
