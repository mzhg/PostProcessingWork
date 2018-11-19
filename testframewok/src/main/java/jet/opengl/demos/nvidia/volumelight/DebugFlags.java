package jet.opengl.demos.nvidia.volumelight;

/** Debug mode constants (bit flags) */
public enum DebugFlags {

	/** No debug visualizations */
	NONE, 
	
	/** Render volume as wireframe */
	WIREFRAME,
	
	/** Don't blend scene into output */
	NO_BLENDING,
	
	/** Only render the lighting. */
	ONLY_LIGHTING,
}
