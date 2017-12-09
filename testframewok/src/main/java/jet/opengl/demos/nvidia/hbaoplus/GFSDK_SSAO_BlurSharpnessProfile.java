package jet.opengl.demos.nvidia.hbaoplus;

/**
 * When enabled, the actual per-pixel blur sharpness value depends on the per-pixel view depth with:<ul>
 * <li> LerpFactor = (PixelViewDepth - ForegroundViewDepth) / (BackgroundViewDepth - ForegroundViewDepth)
 * <li> Sharpness = lerp(Sharpness*ForegroundSharpnessScale, Sharpness, saturate(LerpFactor))
 * </ul>
 */
public class GFSDK_SSAO_BlurSharpnessProfile {

	/** To make the blur sharper in the foreground */
	public boolean                 enable;
	/** Sharpness scale factor for ViewDepths <= ForegroundViewDepth */
    public float                foregroundSharpnessScale = 4.f;
    /** Maximum view depth of the foreground depth range */
    public float                foregroundViewDepth;
    /** Minimum view depth of the background depth range */
    public float                backgroundViewDepth = 1.f;
    
    public GFSDK_SSAO_BlurSharpnessProfile() {}
	
	public GFSDK_SSAO_BlurSharpnessProfile(GFSDK_SSAO_BlurSharpnessProfile o) {
		set(o);
	}
	
	public void set(GFSDK_SSAO_BlurSharpnessProfile o){
		enable = o.enable;
		foregroundSharpnessScale = o.foregroundSharpnessScale;
		foregroundViewDepth = o.foregroundViewDepth;
		backgroundViewDepth = o.backgroundViewDepth;

	}
}
