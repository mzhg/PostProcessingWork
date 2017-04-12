package com.nvidia.developer.opengl.tests;

import com.nvidia.developer.opengl.app.NvSampleApp;
import com.nvidia.developer.opengl.utils.FieldControl;
import com.nvidia.developer.opengl.utils.NvImage;

import org.lwjgl.util.vector.Matrix4f;

import java.io.IOException;
import java.nio.ByteBuffer;

import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.GLSLUtil;
import jet.opengl.postprocessing.shader.ShaderLoader;
import jet.opengl.postprocessing.shader.ShaderType;

public class ComputeBasicGLSL extends NvSampleApp{

	private static final int WORKGROUP_SIZE = 16;
	private int m_computeProg;
	private NvImage m_sourceImage;
	private int m_sourceTexture;
	private int m_resultTexture;
	private boolean m_enableFilter = true;
	private float m_aspectRatio = 1.0f;
	private FullscreenProgram fullScreenProgram;
	private final Matrix4f modelMatrix = new Matrix4f();
	private GLFuncProvider gl;
	
	@Override
	public void initUI() {
		mTweakBar.addValue("Enable filter", new FieldControl(this, "m_enableFilter", FieldControl.CALL_FIELD), false, 0);
	}

	@Override
	public void initRendering() {
		getGLContext().setSwapInterval(0);
		gl = GLFuncProviderFactory.getGLFuncProvider();
//		if(!requireMinAPIVersion(NvGfxAPIVersion.GL4, true))
//			return;
		
		//init shaders
	    try {
			fullScreenProgram = new FullscreenProgram();
			CharSequence computeSource = ShaderLoader.loadShaderFile("assets\\ComputeBasicGLSL\\shaders\\invert.glsl", false);
			int computeShader = GLSLUtil.compileShaderFromSource(computeSource, ShaderType.COMPUTE, true);
			m_computeProg = GLSLUtil.createProgramFromShaders(computeShader, null);
			gl.glDeleteShader(computeShader);
		} catch (IOException e1) {
			e1.printStackTrace();
		}

	    //load input texture 
	    try {
			m_sourceImage = NvImage.createFromDDSFile("assets\\ComputeBasicGLSL\\textures\\flower1024.dds");
		} catch (IOException e) {
			e.printStackTrace();
		}
	    m_sourceTexture = m_sourceImage.updaloadTexture();
	    
	    gl.glBindTexture(GLenum.GL_TEXTURE_2D, m_sourceTexture);
	    gl.glTexParameteri(GLenum.GL_TEXTURE_2D, GLenum.GL_TEXTURE_MIN_FILTER, GLenum.GL_LINEAR);
	    gl.glTexParameteri(GLenum.GL_TEXTURE_2D, GLenum.GL_TEXTURE_MAG_FILTER, GLenum.GL_LINEAR);
	    gl.glBindTexture(GLenum.GL_TEXTURE_2D, 0);

	    //create output texture with same size and format as input 
	    int w = m_sourceImage.getWidth();
	    int h = m_sourceImage.getHeight();
	    int intFormat = m_sourceImage.getInternalFormat();
	    int format = m_sourceImage.getFormat();
	    int type = m_sourceImage.getType();

	    m_resultTexture = gl.glGenTexture( );
	    gl.glBindTexture(GLenum.GL_TEXTURE_2D, m_resultTexture);
	    gl.glTexImage2D(GLenum.GL_TEXTURE_2D, 0, intFormat, w, h, 0, format, type, (ByteBuffer)null);
	    gl.glTexParameteri(GLenum.GL_TEXTURE_2D, GLenum.GL_TEXTURE_MIN_FILTER, GLenum.GL_LINEAR);
	    gl.glTexParameteri(GLenum.GL_TEXTURE_2D, GLenum.GL_TEXTURE_MAG_FILTER, GLenum.GL_LINEAR);
	    
	    gl.glBindTexture(GLenum.GL_TEXTURE_2D, 0);
	    
//	    setTitle("ComputeGLSL");
	}

	@Override
	public void reshape(int width, int height) {
		gl.glViewport(0, 0, width, height);
		m_aspectRatio = (float)height/(float)width;
		
		System.out.println("ratio = " + m_aspectRatio);
	}
	
	private void runComputeFilter(int inputTex, int outputTex, int width, int height){
		gl.glUseProgram( m_computeProg);

		gl.glBindImageTexture(0, inputTex, 0, false, 0, GLenum.GL_READ_ONLY, GLenum.GL_RGBA8); 
		gl.glBindImageTexture(1, outputTex, 0, false, 0, GLenum.GL_WRITE_ONLY, GLenum.GL_RGBA8);

		gl.glDispatchCompute(width/WORKGROUP_SIZE, height/WORKGROUP_SIZE, 1);

		gl.glMemoryBarrier(GLenum.GL_SHADER_IMAGE_ACCESS_BARRIER_BIT); 
	}
	
	private void drawImage(int texture){
		gl.glClearColor(0.2f, 0.0f, 0.2f, 1.0f);
		gl.glClear(GLenum.GL_COLOR_BUFFER_BIT | GLenum.GL_DEPTH_BUFFER_BIT);

		gl.glActiveTexture(GLenum.GL_TEXTURE0);
	    gl.glBindTexture(GLenum.GL_TEXTURE_2D, texture);

	    // scale the position.x to the apect ratio.
	    modelMatrix.m00 = m_aspectRatio;
	    fullScreenProgram.enable();
	    fullScreenProgram.applyPositionTransform(modelMatrix);
	    gl.glDrawArrays(GLenum.GL_TRIANGLE_STRIP, 0, 4);
	    fullScreenProgram.disable();
	}
	
	@Override
	public void display() {
		if(m_enableFilter){
	        runComputeFilter(m_sourceTexture, m_resultTexture, m_sourceImage.getWidth(), m_sourceImage.getHeight());
	        drawImage(m_resultTexture);
	    }else{
	        drawImage(m_sourceTexture);
	    }
	}
}
