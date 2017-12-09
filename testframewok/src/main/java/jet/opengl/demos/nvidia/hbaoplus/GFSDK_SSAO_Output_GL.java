package jet.opengl.demos.nvidia.hbaoplus;

public class GFSDK_SSAO_Output_GL {

	/** Output Frame Buffer Object of RenderAO */
	public int outputFBO;
	/** Blend state used when writing the AO to OutputFBO */
	public final GFSDK_SSAO_BlendState_GL blend = new GFSDK_SSAO_BlendState_GL();
}
