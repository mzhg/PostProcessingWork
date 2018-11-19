package jet.opengl.demos.nvidia.volumelight;

public class ContextDesc {

	/** Width of output/depth surface */
	public int uWidth;
	/** Height of output/depth surface */
	public int uHeight;
	/** Sample rate of output/depth surface */
	public int uSamples;
	
	/** Target resolution of internal buffer */
	public DownsampleMode eDownsampleMode;
	/** Target sample rate of internal buffer */
	public MultisampleMode eInternalSampleMode;
	/** Type of filtering to do on the output */
	public FilterMode eFilterMode;
	
	public ContextDesc() {}
	
	public ContextDesc(ContextDesc o) {
		set(o);
	}
	
	public void set(ContextDesc o){
		uWidth = o.uWidth;
		uHeight = o.uHeight;
		uSamples = o.uSamples;
		eDownsampleMode = o.eDownsampleMode;
		eInternalSampleMode = o.eInternalSampleMode;
		eFilterMode = o.eFilterMode;
	}
}
