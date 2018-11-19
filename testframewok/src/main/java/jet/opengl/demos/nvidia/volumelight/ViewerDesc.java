package jet.opengl.demos.nvidia.volumelight;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

public class ViewerDesc {

	/** Camera projection transform */
	public final Matrix4f mProj = new Matrix4f();
	/** Camera view-proj transform */
	public final Matrix4f mViewProj = new Matrix4f();
	/** Camera position in world-space */
	public final Vector3f vEyePosition = new Vector3f();
	/** Viewport Width (may differ from framebuffer) */
	public int uViewportWidth;
	/** Viewport Height (may differ from framebuffer) */
	public int uViewportHeight;
	/** The distance to Camera near, far clip  plane.*/
	public float fZNear, fZFar;
	
	public ViewerDesc() {}
	
	public ViewerDesc(ViewerDesc o) {
		set(o);
	}
	
	public void set(ViewerDesc o){
		mProj.load(o.mProj);
		mViewProj.load(o.mViewProj);
		vEyePosition.set(o.vEyePosition);
		uViewportWidth = o.uViewportWidth;
		uViewportHeight = o.uViewportHeight;
	}
}
