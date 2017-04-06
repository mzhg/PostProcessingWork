package jet.opengl.desktop.lwjgl;

import org.lwjgl.glfw.GLFW;

public class GLContextConfig {

	public static final int API_OPENGL = GLFW.GLFW_OPENGL_API;
	public static final int API_OPENGL_ES = GLFW.GLFW_OPENGL_ES_API;
	
	public static final int PROFILE_ANY = GLFW.GLFW_OPENGL_ANY_PROFILE;
	public static final int PROFILE_CORE = GLFW.GLFW_OPENGL_CORE_PROFILE;
	public static final int PROFILE_COMPAT = GLFW.GLFW_OPENGL_COMPAT_PROFILE;
	
	public static final int RED_BITS = 8;
	public static final int GREEN_BITS = 8;
	public static final int BLUE_BITS = 8;
	public static final int ALPHA_BITS = 8;
	public static final int DEPTH_BITS = 24;
	public static final int STENCIL_BITS = 8;
	
	public int redBits = 8;
	public int greenBits = 8;
	public int blueBits = 8;
	public int alphaBits = 0;
	
	public int depthBits = 8;
	public int stencilBits = 0;
	public int auxBuffers = 0;
	public int multiSamplers = 0;
	
	// Only for OpenGL ES content
	public int esMajor =2;
	public int esMinor =0;
	
	public boolean stereo;
	public boolean sRGBCapable;
	
	public int clientAPI = API_OPENGL;
	public boolean debugContext = true;
	public int glProfile;
	
	public GLContextConfig set(GLContextConfig o){
		if(this == o) return this;
		
		redBits = o.redBits;
		greenBits = o.greenBits;
		blueBits = o.blueBits;
		alphaBits = o.alphaBits;
		
		depthBits = o.depthBits;
		stencilBits = o.stencilBits;
		auxBuffers = o.auxBuffers;
		multiSamplers = o.multiSamplers;
		
		stereo = o.stereo;
		sRGBCapable = o.sRGBCapable;
		clientAPI = o.clientAPI;
		debugContext = o.debugContext;
		glProfile = o.glProfile;
		return this;
	}
	
	public GLContextConfig() {
	}
}
