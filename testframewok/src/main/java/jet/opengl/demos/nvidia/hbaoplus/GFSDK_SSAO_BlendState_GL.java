package jet.opengl.demos.nvidia.hbaoplus;

public class GFSDK_SSAO_BlendState_GL {

	/** OVERWRITE_RGB, MULTIPLY_RGB or CUSTOM_BLEND */
	public GFSDK_SSAO_BlendMode mode = GFSDK_SSAO_BlendMode.GFSDK_SSAO_OVERWRITE_RGB;
	/** Relevant only if Mode is CUSTOM_BLEND */
	public final GFSDK_SSAO_CustomBlendState_GL customState = new GFSDK_SSAO_CustomBlendState_GL();
	
	public GFSDK_SSAO_BlendState_GL() {}
	
	public GFSDK_SSAO_BlendState_GL(GFSDK_SSAO_BlendState_GL o) {
		set(o);
	}
	
	public void set(GFSDK_SSAO_BlendState_GL o){
		mode = o.mode;
		customState.set(o.customState);

	}
}
