package com.nvidia.developer.opengl.demos.scenes.outdoor;

import java.io.IOException;

import jet.opengl.postprocessing.shader.GLSLProgram;

final class GenerateNormalMapProgram extends GLSLProgram{

	private int location;
	public GenerateNormalMapProgram(String shaderPath) {
		try {
			setSourceFromFiles(shaderPath + "Quad_VS.vert", shaderPath + "GenerateNormalMapPS.frag");
		} catch (IOException e) {
			e.printStackTrace();
		}

		int textureLoc = gl.glGetUniformLocation(getProgram(), "g_tex2DElevationMap");
		gl.glProgramUniform1i(getProgram(), textureLoc, 0);
		
		location = gl.glGetUniformLocation(getProgram(), "g_NMGenerationAttribs");
	}
	
	public int getNMGenerationAttribs(){
		return location;
	}
}
