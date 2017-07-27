package jet.opengl.demos.nvidia.waves.samples;

/** No cull face, No depth Test, Binding main_color_framebuffer(texture0) texture with sampler SampleLinearWrap at texture unit0. */
/*public*/ class IsMainToBackBufferProgram extends IsBaseProgram{

	int mainBufferSizeMultiplierIndex;
	float g_MainBufferSizeMultiplier = 1.1f;
	
	public IsMainToBackBufferProgram(String prefix) {
		super(prefix + "MainToBackBuffer.frag", "u_texture");

		initShader();
	}
	
	void initShader() {
		mainBufferSizeMultiplierIndex =getUniformLocation("g_MainBufferSizeMultiplier");
		gl.glUniform1f(mainBufferSizeMultiplierIndex, g_MainBufferSizeMultiplier);
	}
	
	public void applyMainBufferSizeMultiplier(float value){
		if(g_MainBufferSizeMultiplier != value){
			g_MainBufferSizeMultiplier = value;
			gl.glUniform1f(mainBufferSizeMultiplierIndex, g_MainBufferSizeMultiplier);
		}
	}
}
