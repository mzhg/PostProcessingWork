package jet.opengl.demos.nvidia.hbaoplus;

public class GFSDK_SSAO_CustomBlendState_GL {

	public int modeRGB;
	public int modeAlpha;
	
	public int srcRGB;
	public int dstRGB;
	public int srcAlpha;
	public int dstAlpha;
	
	public float blendColorR;
	public float blendColorG;
	public float blendColorB;
	public float blendColorA;
	
	public boolean colorMaskR;
	public boolean colorMaskG;
	public boolean colorMaskB;
	public boolean colorMaskA;
	
	public GFSDK_SSAO_CustomBlendState_GL() {}
	
	public GFSDK_SSAO_CustomBlendState_GL(GFSDK_SSAO_CustomBlendState_GL o) {
		set(o);
	}
	
	public void set(GFSDK_SSAO_CustomBlendState_GL o){
		modeRGB = o.modeRGB;
		modeAlpha = o.modeAlpha;
		srcRGB = o.srcRGB;
		dstRGB = o.dstRGB;
		srcAlpha = o.srcAlpha;
		dstAlpha = o.dstAlpha;
		blendColorR = o.blendColorR;
		blendColorG = o.blendColorG;
		blendColorB = o.blendColorB;
		blendColorA = o.blendColorA;
		colorMaskR = o.colorMaskR;
		colorMaskG = o.colorMaskG;
		colorMaskB = o.colorMaskB;
		colorMaskA = o.colorMaskA;
	}
}
