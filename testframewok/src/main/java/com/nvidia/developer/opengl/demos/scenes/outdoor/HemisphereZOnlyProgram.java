package com.nvidia.developer.opengl.demos.scenes.outdoor;

import org.lwjgl.util.vector.Matrix4f;

import java.io.IOException;

import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.util.CacheBuffer;

final class HemisphereZOnlyProgram extends GLSLProgram{

	private int worldViewProjIndex;
	public HemisphereZOnlyProgram(String shaderPath) {
		try {
			setSourceFromFiles(shaderPath + "HemisphereZOnly.vert", shaderPath + "DummyPS.frag");
		} catch (IOException e) {
			e.printStackTrace();
		}
		worldViewProjIndex =gl.glGetUniformLocation(getProgram(), "g_WorldViewProj");
	}
	
	public void setWorldViewProjMatrix(Matrix4f mat){
		gl.glProgramUniformMatrix4fv(getProgram(), worldViewProjIndex, false, CacheBuffer.wrap(mat));
	}
}
