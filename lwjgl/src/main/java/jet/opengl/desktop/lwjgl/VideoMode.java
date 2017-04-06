package jet.opengl.desktop.lwjgl;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWVidMode;

public class VideoMode {

	private final GLFWVidMode viemode;
	
	public final int redBits;
	public final int greenBits;
	public final int blueBits;
	public final int refreshRate;
	
	public final int width;
	public final int height;
	
	VideoMode(long monitor) {
		viemode = GLFW.glfwGetVideoMode(monitor);
		
		width = viemode.width();
		height = viemode.height();
		redBits = viemode.redBits();
		greenBits = viemode.greenBits();
		blueBits = viemode.blueBits();
		refreshRate = viemode.refreshRate();
	}

	@Override
	public String toString() {
		return "VideoMode [redBits=" + redBits + ", greenBits=" + greenBits
				+ ", blueBits=" + blueBits + ", refreshRate=" + refreshRate
				+ ", width=" + width + ", height=" + height + "]";
	}
}
