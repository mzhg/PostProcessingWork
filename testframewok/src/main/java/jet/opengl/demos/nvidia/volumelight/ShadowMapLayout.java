package jet.opengl.demos.nvidia.volumelight;

/** Specifies the geometric mapping of the shadow map */
public enum ShadowMapLayout {

	/** Simple frustum depth texture */
	SIMPLE,			
	/** Multiple depth views combined into one texture */
	CASCADE_ATLAS,
	/** Multiple depth views as texture array slices*/
	CASCADE_ARRAY,
	/** Depth mapped using paraboloid warping */
    PARABOLOID,
}
