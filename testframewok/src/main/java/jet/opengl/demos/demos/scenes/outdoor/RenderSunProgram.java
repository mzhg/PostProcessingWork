package jet.opengl.demos.demos.scenes.outdoor;

import java.io.IOException;

import jet.opengl.postprocessing.common.GLCheck;
import jet.opengl.postprocessing.shader.GLSLProgram;

final class RenderSunProgram extends GLSLProgram{

	private int uniformIndex;
	public RenderSunProgram(String shaderPath) {
		try {
			setSourceFromFiles(shaderPath + "Sun_VS.vert", shaderPath + "Sun_PS.frag");
		} catch (IOException e) {
			e.printStackTrace();
		}

		uniformIndex = gl.glGetUniformLocation(getProgram(), "uniformData");
		GLCheck.checkError();
	}
	
	public void setUniform(float x, float y, float z, float w){
		gl.glUniform4f(uniformIndex, x, y, z, w);
	}
}
