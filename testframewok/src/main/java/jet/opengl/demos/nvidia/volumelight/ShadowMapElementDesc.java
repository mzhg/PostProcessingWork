package jet.opengl.demos.nvidia.volumelight;

import org.lwjgl.util.vector.Matrix4f;

/** Describes an individual slice in a shadow map cascade */
public class ShadowMapElementDesc {

	/** View-Proj transform for cascade */
	public final Matrix4f mViewProj = new Matrix4f();
	
	/** X-offset within texture */
	public int uOffsetX;		//!<
	/** Y-offset within texture*/
	public int uOffsetY;		//!<
	/** Footprint width within texture */
	public int uWidth;		//!<
	/** Footprint height within texture */
	public int uHeight;		//!< 
	/** Texture array index for this element (if used) */
	public int mArrayIndex;   //!< 
}
