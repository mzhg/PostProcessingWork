package jet.opengl.android.common;

public class GLConfiguration {
	
	public static final int GLES2 = 1;
	public static final int GLES3_0 = 2;
	public static final int GLES3_1 = 3;
	public static final int GLES3_2 = 4;

	public int redBits = 8;
	public int greenBits = 8;
	public int blueBits = 8;
	public int alphaBits = 0;
	
	public int depthBits = 8;
	public int stencilBits = 0;
	
	public int version = 0;
	
	public boolean checkGLError = true;
	public boolean logGLCallInfo = false;
	
	public boolean continueRender = true;
	
	public boolean isGLES2(){
		return version >= GLES2;
	}
	
	@Override
	public String toString() {
		return "redBits:" + redBits + "\n"
			  +"greenBits: " + greenBits +"\n"
			  +"blueBits: " + blueBits + "\n"
			  +"alphaBits: " + alphaBits + "\n"
			  +"depthBits: " + depthBits + "\n"
			  +"stencilBits: " + stencilBits;
 	}
}
