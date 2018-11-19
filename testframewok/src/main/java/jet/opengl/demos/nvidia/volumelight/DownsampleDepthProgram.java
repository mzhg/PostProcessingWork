package jet.opengl.demos.nvidia.volumelight;

import java.lang.reflect.Field;

import jet.opengl.postprocessing.common.GLCheck;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.Macro;
import jet.opengl.postprocessing.util.Pair;

final class DownsampleDepthProgram extends BaseVLProgram{

	private boolean isMSAA;
	public DownsampleDepthProgram(ContextImp_OpenGL context, boolean isMSAA) {
		super(context);
		
		this.isMSAA = isMSAA;
		compileProgram();
		
		int tDepthMapIndex = gl.glGetUniformLocation(m_programId, "tDepthMap");
		gl.glProgramUniform1i(m_programId, tDepthMapIndex, 0);
		GLCheck.checkError();
	}
	
	@Override
	protected Pair<String, Macro[]> getPSShader() {
//		System.out.println("DownsampleDepthProgram: isMSAA = " + isMSAA);
		return new Pair<>("DownsampleDepth_PS.frag", isMSAA ? sampleModeMSAA : sampleModeSingle);
	}
	
	public void enable(int depthTexture, int point) {
		super.enable();

		bindTexture(GLenum.GL_TEXTURE_2D, depthTexture, point, 0);

		/*GL13.glActiveTexture(GL13.GL_TEXTURE0);
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, depthTexture);
		GL33.glBindSampler(0, point);*/
	}
	
	@Override
	public void disable() {
		super.disable();
		
		/*GL13.glActiveTexture(GL13.GL_TEXTURE0);
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
		GL33.glBindSampler(0, 0);*/

		bindTexture(GLenum.GL_TEXTURE_2D, 0, 0, 0);
	}

	@Override
	protected Object getParameter() {
		return "DownsampleDepth_" + (isMSAA ? 2 : 1);
	}

}
