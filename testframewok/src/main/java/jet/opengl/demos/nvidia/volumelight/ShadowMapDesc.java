package jet.opengl.demos.nvidia.volumelight;

/** Shadow Map Structural Description */
public class ShadowMapDesc {

	/** Shadow map structure type */
	public ShadowMapLayout eType; 
	/** Shadow map texture width */
	public int uWidth; 	 
	/** Shadow map texture height */
	public int uHeight;	 
	/** Number of sub-elements in the shadow map */
	public int uElementCount; //!< 

    /** Individual cascade descriptions */
	public final ShadowMapElementDesc[] elements = new ShadowMapElementDesc[VLConstant.MAX_SHADOWMAP_ELEMENTS];

	public ShadowMapDesc() {
		for(int i = 0; i < elements.length; i++)
			elements[i] = new ShadowMapElementDesc();
	}
}
