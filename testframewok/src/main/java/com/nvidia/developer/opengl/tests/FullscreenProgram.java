package com.nvidia.developer.opengl.tests;

import org.lwjgl.util.vector.Matrix4f;

import java.io.IOException;

import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.shader.GLSLUtil;
import jet.opengl.postprocessing.util.CacheBuffer;


public class FullscreenProgram extends GLSLProgram {
	
	/** The varying variable between vertex shader and fragment shader. */
	public static final String TEXCOORD0 = "vec2 v_texcoords";

	private final int posMatIndex;
	private final int texMatIndex;
	private final int texSamplerIndex;
	
	protected final GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();

	public FullscreenProgram() {
		try {
			setSourceFromFiles("shaders/fullscreen.vert", getFragShaderFile());
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		gl.glUseProgram(m_program);
		posMatIndex = GLSLUtil.getUniformLocation(m_program, "positionTransform");
		texMatIndex = GLSLUtil.getUniformLocation(m_program, "texcoordTransform");
		texSamplerIndex = GLSLUtil.getUniformLocation(m_program, "u_texture");
		
		applyDefaultUniforms();
		gl.glUseProgram(0);
	}
	
	protected String getFragShaderFile() { return "shaders/fullscreen.frag";}
	
	protected void applyDefaultUniforms(){
		gl.glUniform1i(texSamplerIndex, 0);
		gl.glUniformMatrix4fv(posMatIndex, false, CacheBuffer.wrap(Matrix4f.IDENTITY));
		gl.glUniformMatrix4fv(texMatIndex, false, CacheBuffer.wrap(Matrix4f.IDENTITY));
	}
	
	public void applyPositionTransform(Matrix4f mat){ gl.glUniformMatrix4fv(posMatIndex, false, CacheBuffer.wrap(mat));}
	public void applyTexcoordTransform(Matrix4f mat){ gl.glUniformMatrix4fv(texMatIndex, false, CacheBuffer.wrap(mat));}
}
