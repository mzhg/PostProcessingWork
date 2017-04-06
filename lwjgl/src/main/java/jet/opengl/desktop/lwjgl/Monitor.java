package jet.opengl.desktop.lwjgl;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFW;

public class Monitor {

	private static final IntBuffer int1 = BufferUtils.createIntBuffer(1);
	private static final IntBuffer int2 = BufferUtils.createIntBuffer(1);

	private static List<Monitor> monitors = new ArrayList<Monitor>();

	private long handler;

	static long primaryMonitor;

	public final int physicalWidth;
	public final int physicalHeight;
	public final int posX;
	public final int posY;

	public final String name;
	
	private VideoMode videoMode;

	Monitor(long handler) {
		this.handler = handler;

		GLFW.glfwGetMonitorPhysicalSize(handler, int1, int2);

		physicalHeight = int2.get(0);
		physicalWidth = int1.get(0);

		GLFW.glfwGetMonitorPos(handler, int1, int2);

		posX = int1.get(0);
		posY = int1.get(0);

		name = GLFW.glfwGetMonitorName(handler);
	}

	public long getHandler() {
		return handler;
	}
	
	public boolean isPrimary() { return handler == primaryMonitor; }

	/**
	 * Should call glfwInit() first before call this method.
	 * @return
	 */
	public static Monitor[] getAvaiableMonitors() {
		if (monitors.size() == 0) {
			retriveMonitors();
		}
		return monitors.toArray(new Monitor[monitors.size()]);
	}
	
	private static void retriveMonitors(){
		primaryMonitor = GLFW.glfwGetPrimaryMonitor();
		
		PointerBuffer bufs = GLFW.glfwGetMonitors();
		while (bufs.remaining() > 0)
			monitors.add(new Monitor(bufs.get()));
	}
	
	/**
	 * Should call glfwInit() first before call this method.
	 * @return
	 */
	public static Monitor getPrimaryMonitor(){
		if (monitors.size() == 0) {
			retriveMonitors();
		}
		
		for (int i = 0; i < monitors.size(); i++) {
			Monitor m = monitors.get(i);
			if(m.isPrimary())
				return m;
		}
		
		return null;
	}
	
	public VideoMode getVideoMode(){
		if(videoMode == null)
			videoMode = new VideoMode(handler);
		
		return videoMode;
	}
	
	public static void main(String[] args) {
		Monitor[] ma = getAvaiableMonitors();
		
		for (int i = 0; i < ma.length; i++) {
			System.out.println(ma[i].getVideoMode());
		}
	}

	@Override
	public String toString() {
		return "Monitor [handler=" + handler + ", physicalWidth="
				+ physicalWidth + ", physicalHeight=" + physicalHeight
				+ ", posX=" + posX + ", posY=" + posY + ", name=" + name + "]";
	}
}
