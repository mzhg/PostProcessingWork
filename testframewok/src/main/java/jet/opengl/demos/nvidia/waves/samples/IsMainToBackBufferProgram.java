package jet.opengl.demos.nvidia.waves.samples;

import org.lwjgl.opengl.GL20;

import jet.util.opengl.shader.GLSLProgram;

/** No cull face, No depth Test, Binding main_color_framebuffer(texture0) texture with sampler SampleLinearWrap at texture unit0. */
/*public*/ class IsMainToBackBufferProgram extends IsBaseProgram{

	int mainBufferSizeMultiplierIndex;
	float g_MainBufferSizeMultiplier = 1.1f;
	
	public IsMainToBackBufferProgram(String prefix) {
		super(prefix + "MainToBackBuffer.frag", "u_texture");
	}
	
	@Override
	void initShader() {
		mainBufferSizeMultiplierIndex =GLSLProgram.getUniformLocation(programId, "g_MainBufferSizeMultiplier");
		GL20.glUniform1f(mainBufferSizeMultiplierIndex, g_MainBufferSizeMultiplier);
	}
	
	public void applyMainBufferSizeMultiplier(float value){
		if(g_MainBufferSizeMultiplier != value){
			g_MainBufferSizeMultiplier = value;
			GL20.glUniform1f(mainBufferSizeMultiplierIndex, g_MainBufferSizeMultiplier);
		}
	}
}
