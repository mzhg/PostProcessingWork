package jet.opengl.demos.nvidia.hbaoplus;

public class GFSDK_SSAO_BlurParameters {

	/** To blur the AO with an edge-preserving blur */
	public boolean                 enable = true;
	/** BLUR_RADIUS_2 or BLUR_RADIUS_4 */
    public GFSDK_SSAO_BlurRadius   radius = GFSDK_SSAO_BlurRadius.GFSDK_SSAO_BLUR_RADIUS_4;
    /** The higher, the more the blur preserves edges // 0.0~16.0 */
    public float                   sharpness = 16.f;
    /** Optional depth-dependent sharpness function */
    public final GFSDK_SSAO_BlurSharpnessProfile SharpnessProfile = new GFSDK_SSAO_BlurSharpnessProfile();
    
    public GFSDK_SSAO_BlurParameters() {}
	
	public GFSDK_SSAO_BlurParameters(GFSDK_SSAO_BlurParameters o) {
		set(o);
	}
	
	public void set(GFSDK_SSAO_BlurParameters o){
		enable = o.enable;
		radius = o.radius;
		sharpness = o.sharpness;
		SharpnessProfile.set(o.SharpnessProfile);

	}
}
