package com.nvidia.developer.opengl.utils;

import org.lwjgl.util.vector.Matrix4f;

import java.io.IOException;

import jet.opengl.postprocessing.shader.AttribBinder;
import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.util.CachaRes;
import jet.opengl.postprocessing.util.CacheBuffer;

public class ShadowmapGenerateProgram extends GLSLProgram{

	private int mvpIndex;
	public ShadowmapGenerateProgram(int posBind) {
		try {
			setAttribBinding(new AttribBinder("a_Position", posBind));
			setSourceFromFiles("shader_libs/shadow_generate.vert", "shader_libs/shadow_generate.frag");
		} catch (IOException e) {
			e.printStackTrace();
		}

		mvpIndex = getUniformLocation("u_ViewProjMatrix");
	}
	
	public ShadowmapGenerateProgram() {
		this(0);
	}

	@CachaRes
	public void applyMVPMat(Matrix4f mat){
		gl.glUniformMatrix4fv(mvpIndex, false, CacheBuffer.wrap(mat!= null ? mat : Matrix4f.IDENTITY));
	}
}
