package jet.opengl.demos.nvidia.hbaoplus;

class OutputInfo {

	int fboId;
	int sampleCount;
	GFSDK_SSAO_BlendState_GL blend;
	
	void init(GFSDK_SSAO_Output_GL output){
		fboId = output.outputFBO;
		sampleCount = 1;
		blend = output.blend;  // TODO refrence copy
	}
}
