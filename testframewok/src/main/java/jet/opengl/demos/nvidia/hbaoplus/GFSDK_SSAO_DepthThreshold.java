package jet.opengl.demos.nvidia.hbaoplus;

public class GFSDK_SSAO_DepthThreshold {

	/** To return white AO for ViewDepths > MaxViewDepth */
	public boolean                 enable = false;
	/** Custom view-depth threshold */
    public float                   maxViewDepth = 0.f;
    /** The higher, the sharper are the AO-to-white transitions */
    public float                   sharpness = 100.f;
}
