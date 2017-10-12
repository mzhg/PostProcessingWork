package jet.opengl.android.common;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.pm.ConfigurationInfo;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import com.nvidia.developer.opengl.app.NvAppBase;
import com.nvidia.developer.opengl.app.NvEGLConfiguration;
import com.nvidia.developer.opengl.app.NvGLAppContext;
import com.nvidia.developer.opengl.utils.NvGfxAPIVersion;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import jet.opengl.postprocessing.common.GLAPI;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.util.FileUtils;

public abstract class OpenGLBaseActivity extends Activity implements NvGLAppContext {

	private GLSurfaceView m_surfaceView;
	private final GLConfiguration glConfig = new GLConfiguration();
	private final NvEGLConfiguration nvConfig = new NvEGLConfiguration();
	private NvAppBase renderListener;
	
	private int width, height;
	private String title;
	
	@Override
	protected final void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		width = getWindow().getWindowManager().getDefaultDisplay().getWidth();
		height = getWindow().getWindowManager().getDefaultDisplay().getHeight();
		
		onGLConfig(glConfig);
		
		m_surfaceView = new GLSurfaceView(this);
		m_surfaceView.setEGLConfigChooser(glConfig.redBits, glConfig.greenBits, glConfig.blueBits, glConfig.alphaBits, glConfig.depthBits, glConfig.stencilBits);
		
		if(glConfig.version < GLConfiguration.GLES2 || glConfig.version > GLConfiguration.GLES3_2)
			throw new IllegalArgumentException("Invalid OpenGL ES version: " + glConfig.version);
		
		boolean supportGL = true;
		if(glConfig.version > 0){ // NOT the ES1.0
			ActivityManager activityManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
			ConfigurationInfo configurationInfo = activityManager.getDeviceConfigurationInfo();

			supportGL = configurationInfo.reqGlEsVersion >= 0x20000
				|| (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1 
				&& (Build.FINGERPRINT.startsWith("generic")
				|| Build.FINGERPRINT.startsWith("unknow")
				|| Build.MODEL.contains("google_sdk")
				|| Build.MODEL.contains("Emulator") 
				|| Build.MODEL.contains("Android SDK built for x86")));
			
			Log.e("OpenGL ES", "Version: " + Integer.toHexString(configurationInfo.reqGlEsVersion));

			// measure the actual OpenGL ES Version.
			int major = (configurationInfo.reqGlEsVersion >> 16) & 0xFFFF;
			int minor = configurationInfo.reqGlEsVersion & 0xFFFF;

			if(major == 2){
				glConfig.version = GLConfiguration.GLES2;
			}else if(major == 3){
				glConfig.version = (minor == 0)? GLConfiguration.GLES3_0 : GLConfiguration.GLES3_1;
				// Fewer deviece Support the Opengl ES 3.2 so far.
			}
		}
		
		if(supportGL){
			if(glConfig.version > 0){
				m_surfaceView.setEGLContextClientVersion(glConfig.version + 1);
			}
			
			int flags = 0;
			if(glConfig.checkGLError)
				flags |= GLSurfaceView.DEBUG_CHECK_GL_ERROR;
			if(glConfig.logGLCallInfo)
				flags |= GLSurfaceView.DEBUG_LOG_GL_CALLS;
			
//			m_surfaceView.setDebugFlags(flags);
//			if(glConfig.continueRender)
//				m_surfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
//			else
//				m_surfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
			m_surfaceView.setRenderer(glRenderer);
			setContentView(m_surfaceView);

			GLFuncProviderFactory.initlizeGLFuncProvider(GLAPI.ANDROID, null);
			FileUtils.setIntenalFileLoader(new AndroidAssetLoader(getAssets()));

			renderListener = getRenderListener();
			if(renderListener == null)
				throw new NullPointerException("renderListener can't be null!!!");

			renderListener.setGLContext(this);
		}else{
			// alert a dialog to show the error.
			Log.e("OpenGLCreating", "can't create opengl context!");
			finish();
		}
	}

	public GLConfiguration getGLConfig(){
		return glConfig;
	}

	protected abstract NvAppBase getRenderListener();
	
	protected void onGLConfig(GLConfiguration glConfig){
	}
	
	public int getWidth(){
		return width;
	}
	
	public int getHeight(){
		return height;
	}
	
	private final GLSurfaceView.Renderer glRenderer = new GLSurfaceView.Renderer(){ 
		public void onDrawFrame(GL10 arg0) {
			renderListener.draw();
		}
	
		public void onSurfaceChanged(GL10 arg0, int width, int height) {
			renderListener.onResize(width, height);
		}
	
		public void onSurfaceCreated(GL10 arg0, EGLConfig arg1) {
			int[] i = new int[1];
			GLES20.glGetIntegerv(GLES20.GL_RED_BITS, i, 0);
			nvConfig.redBits = glConfig.redBits = i[0];

			GLES20.glGetIntegerv(GLES20.GL_GREEN_BITS, i, 0);
			nvConfig.greenBits = glConfig.greenBits = i[0];

			GLES20.glGetIntegerv(GLES20.GL_BLUE_BITS, i, 0);
			nvConfig.blueBits = glConfig.blueBits = i[0];

			GLES20.glGetIntegerv(GLES20.GL_ALPHA_BITS, i, 0);
			nvConfig.alphaBits = glConfig.alphaBits = i[0];

			GLES20.glGetIntegerv(GLES20.GL_STENCIL_BITS, i, 0);
			nvConfig.stencilBits = glConfig.stencilBits = i[0];

			GLES20.glGetIntegerv(GLES20.GL_DEPTH_BITS, i, 0);
			nvConfig.depthBits = glConfig.depthBits = i[0];

			Log.e("OpenGL ES", glConfig.toString());
			String version = GLES20.glGetString(GLES20.GL_VERSION);
			String vendor = GLES20.glGetString(GLES20.GL_VENDOR);
			String ext = GLES20.glGetString(GLES20.GL_EXTENSIONS);
			nvConfig.apiVer = NvGfxAPIVersion.GLES3_0;
			
			Log.e("OpenGL ES", "Real Version: " + version);
			Log.e("OpenGL ES", "Vendor: " + vendor);
			Log.e("OpenGL ES", "Extensions: " + ext);

			renderListener.onCreate();
		}
	};

	@Override
	public boolean setSwapInterval(int interval) {
		return false;
	}

	@Override
	public int width() {
		return width;
	}

	@Override
	public int height() {
		return height;
	}

	@Override
	public NvEGLConfiguration getConfiguration() {
		return nvConfig;
	}

	@Override
	public void requestExit() {
		finish();
	}

	@Override
	public void setAppTitle(String title) {
		this.title = title;
	}

	@Override
	public void showDialog(String msg, String errorStr) {

	}

	@Override
	public String getAppTitle() {
		return title;
	}
}
