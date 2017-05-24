package jet.opengl.render.volumeline;

import org.lwjgl.util.vector.Matrix4f;

import java.io.IOException;

import jet.opengl.postprocessing.buffer.AttribDesc;
import jet.opengl.postprocessing.buffer.BufferBinding;
import jet.opengl.postprocessing.buffer.BufferGL;
import jet.opengl.postprocessing.buffer.VertexArrayObject;
import jet.opengl.postprocessing.common.DepthStencilState;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLStateTracker;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.util.CacheBuffer;

// Two bug: 
// 1, This program can't work on the driver which doesn't support the VBO and Primitive_Restart.
// 2, The draw() method can't work along so far.
final class VertexLinesRenderer extends VolumeLinesRenderer{
	
	private static final int END_OF_PRIMITIVE_ID = 999999;
	
	// the program
	VertexLineRendererProgram m_program;
	
	// OpenGL Buffer Object
	BufferGL linesEABO;
	BufferGL vertex0VBO;
	BufferGL vertex1VBO;
	BufferGL offsetDirUvVBO;
	VertexArrayObject lineVA;
	
	final int maxLineCount;
	final float[] vertex0;
	final float[] vertex1;
	
	int lineCount;
	boolean locked;
	final UniformData m_UniformData = new UniformData();

	DepthStencilState m_dsstate;

	public VertexLinesRenderer() throws IOException {
		this(1024);
	}

	public VertexLinesRenderer(int maxLineCount) throws IOException {
		this.maxLineCount = Math.max(1, maxLineCount);
		
		vertex0 = new float[maxLineCount*8*3];
		vertex1 = new float[maxLineCount*8*3];

		m_program = new VertexLineRendererProgram();
		
//		vertex0VBO = GL15.glGenBuffers();
//		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vertex0VBO);
//		GL15.glBufferData(GL15.GL_ARRAY_BUFFER, maxLineCount*3*8*4, GL15.GL_STREAM_DRAW);
//		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
//		vertex1VBO = GL15.glGenBuffers();
//		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vertex1VBO);
//		GL15.glBufferData(GL15.GL_ARRAY_BUFFER, maxLineCount*3*8*4, GL15.GL_STREAM_DRAW);
//		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);

		vertex0VBO = new BufferGL();
		vertex0VBO.initlize(GLenum.GL_ARRAY_BUFFER, maxLineCount*3*8*4, null, GLenum.GL_STATIC_DRAW);
		vertex1VBO = new BufferGL();
		vertex1VBO.initlize(GLenum.GL_ARRAY_BUFFER, maxLineCount*3*8*4, null, GLenum.GL_STATIC_DRAW);
		
		float[] offsetDirUv = new float[maxLineCount * 4 * 8]; // will contains {xy projection space offsets, uv}
		for(int v=0; v<maxLineCount*4*8; v+=4*8)
		{
			offsetDirUv[v   ] = 1.0f;		offsetDirUv[v+1 ] = 1.0f;		offsetDirUv[v+2 ] = 1.0f;		offsetDirUv[v+3 ] = 0.0f;
			offsetDirUv[v+4 ] = 1.0f;		offsetDirUv[v+5 ] =-1.0f;		offsetDirUv[v+6 ] = 1.0f;		offsetDirUv[v+7 ] = 1.0f;
			offsetDirUv[v+8 ] = 0.0f;		offsetDirUv[v+9 ] = 1.0f;		offsetDirUv[v+10] = 0.5f;		offsetDirUv[v+11] = 0.0f;
			offsetDirUv[v+12] = 0.0f;		offsetDirUv[v+13] =-1.0f;		offsetDirUv[v+14] = 0.5f;		offsetDirUv[v+15] = 1.0f;
			offsetDirUv[v+16] = 0.0f;		offsetDirUv[v+17] =-1.0f;		offsetDirUv[v+18] = 0.5f;		offsetDirUv[v+19] = 0.0f;
			offsetDirUv[v+20] = 0.0f;		offsetDirUv[v+21] = 1.0f;		offsetDirUv[v+22] = 0.5f;		offsetDirUv[v+23] = 1.0f;
			offsetDirUv[v+24] = 1.0f;		offsetDirUv[v+25] =-1.0f;		offsetDirUv[v+26] = 0.0f;		offsetDirUv[v+27] = 0.0f;
			offsetDirUv[v+28] = 1.0f;		offsetDirUv[v+29] = 1.0f;		offsetDirUv[v+30] = 0.0f;		offsetDirUv[v+31] = 1.0f;
		}
//		offsetDirUvVBO = GL15.glGenBuffers();
//		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, offsetDirUvVBO);
//		GL15.glBufferData(GL15.GL_ARRAY_BUFFER, GLUtil.wrap(offsetDirUv), GL15.GL_STATIC_DRAW);
//		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
		offsetDirUvVBO = new BufferGL();
		offsetDirUvVBO.initlize(GLenum.GL_ARRAY_BUFFER, offsetDirUv.length*4, CacheBuffer.wrap(offsetDirUv), GLenum.GL_STATIC_DRAW);
		
		//striped triangles for lines rendering with primitive restart
		int[] trisStripElements = new int[maxLineCount*8 + maxLineCount];
//		linesEABO = GL15.glGenBuffers();
//		GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, linesEABO);
		int lineID=0;
		for(int t=0; t<(maxLineCount*8 + maxLineCount); t+=(8+1), lineID+=8)
		{
			trisStripElements[t  ] = lineID;
			trisStripElements[t+1] = lineID+1;
			trisStripElements[t+2] = lineID+2;
			trisStripElements[t+3] = lineID+3;
			trisStripElements[t+4] = lineID+4;
			trisStripElements[t+5] = lineID+5;
			trisStripElements[t+6] = lineID+6;
			trisStripElements[t+7] = lineID+7;
			trisStripElements[t+8] = END_OF_PRIMITIVE_ID;
		}
//		GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, GLUtil.wrap(trisStripElements), GL15.GL_STATIC_DRAW);
//		GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);

		linesEABO = new BufferGL();
		linesEABO.initlize(GLenum.GL_ARRAY_BUFFER, trisStripElements.length*4, CacheBuffer.wrap(trisStripElements), GLenum.GL_STATIC_DRAW);

//		lineVA = ARBVertexArrayObject.glGenVertexArrays();
//
//		ARBVertexArrayObject.glBindVertexArray(lineVA);
//	    {
//			GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vertex0VBO);
//			GL20.glVertexAttribPointer(attr_position, 3, GL11.GL_FLOAT, false, 0, 0);
//			//glBindBuffer(GL_ARRAY_BUFFER, 0);
//			GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vertex1VBO);
//			GL20.glVertexAttribPointer(attr_othervert, 3, GL11.GL_FLOAT, false, 0, 0);
//			//glBindBuffer(GL_ARRAY_BUFFER, 0);
//			GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, offsetDirUvVBO);
//			GL20.glVertexAttribPointer(attr_offsetdir_uv, 4, GL11.GL_FLOAT, false, 0, 0);
//			GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
//
//			GL20.glEnableVertexAttribArray(attr_position);
//			GL20.glEnableVertexAttribArray(attr_othervert);
//			GL20.glEnableVertexAttribArray(attr_offsetdir_uv);
//	    }
//	    ARBVertexArrayObject.glBindVertexArray(0);

		AttribDesc posDesc = new AttribDesc();
		posDesc.index = m_program.attr_position;
		posDesc.size = 3;

		AttribDesc otherDesc = new AttribDesc();
		otherDesc.index = m_program.attr_othervert;
		otherDesc.size = 3;

		AttribDesc dirUvDesc = new AttribDesc();
		dirUvDesc.index = m_program.attr_offsetdir_uv;
		dirUvDesc.size = 4;

		lineVA = new VertexArrayObject();
		lineVA.initlize(new BufferBinding[]{
				new BufferBinding(vertex0VBO, posDesc),
				new BufferBinding(vertex1VBO, otherDesc),
				new BufferBinding(offsetDirUvVBO, dirUvDesc)
		}, linesEABO);

		m_dsstate = new DepthStencilState();
		m_dsstate.depthEnable = true;
	}

	private static final class UniformData{
		float radius;
		Matrix4f mvp;
		Texture2D texture;
		float screenRatio;
	}
	
	@Override
	public void begin(float radius, Matrix4f mvpMat, Matrix4f mvMat,
					  Matrix4f pMat, Texture2D texture, float screenRatio) {
		if(locked)
			throw new IllegalStateException("begin called repeated!");
		
		locked = true;
		lineCount = 0;
//		GL20.glUseProgram(programid);
//		GL20.glUniformMatrix4fv(gpupLine_MVP, false, GLUtil.wrap(mvpMat));
//		GL20.glUniform1f(gpupLine_radius, radius);
//		GL20.glUniform1f(gpupLine_invScrRatio, 1.0f/screenRatio);
//
//		GL13.glActiveTexture(GL13.GL_TEXTURE0);
//		GL11.glBindTexture(GL11.GL_TEXTURE_2D,texture);

		m_UniformData.mvp = mvpMat;
		m_UniformData.radius = radius;
		m_UniformData.screenRatio = 1.0f/screenRatio;
		m_UniformData.texture = texture;
	}

	@Override
	public void line(float x0, float y0, float z0, float x1, float y1, float z1) {
		if(lineCount >= maxLineCount){
			flush();
		}
		
		int l = lineCount;
		final int l24 = l*24;
		vertex0[l24   ] = x0;		vertex0[l24+1 ] = y0;		vertex0[l24+2 ] = z0;
		vertex0[l24+3 ] = x0;		vertex0[l24+4 ] = y0;		vertex0[l24+5 ] = z0;
		vertex0[l24+6 ] = x0;		vertex0[l24+7 ] = y0;		vertex0[l24+8 ] = z0;
		vertex0[l24+9 ] = x0;		vertex0[l24+10] = y0;		vertex0[l24+11] = z0;
		
		vertex0[l24+12] = x1;		vertex0[l24+13] = y1;		vertex0[l24+14] = z1;
		vertex0[l24+15] = x1;		vertex0[l24+16] = y1;		vertex0[l24+17] = z1;
		vertex0[l24+18] = x1;		vertex0[l24+19] = y1;		vertex0[l24+20] = z1;
		vertex0[l24+21] = x1;		vertex0[l24+22] = y1;		vertex0[l24+23] = z1;
		
		vertex1[l24   ] = x1;		vertex1[l24+1 ] = y1;		vertex1[l24+2 ] = z1;
		vertex1[l24+3 ] = x1;		vertex1[l24+4 ] = y1;		vertex1[l24+5 ] = z1;
		vertex1[l24+6 ] = x1;		vertex1[l24+7 ] = y1;		vertex1[l24+8 ] = z1;
		vertex1[l24+9 ] = x1;		vertex1[l24+10] = y1;		vertex1[l24+11] = z1;
																						
		vertex1[l24+12] = x0;		vertex1[l24+13] = y0;		vertex1[l24+14] = z0;
		vertex1[l24+15] = x0;		vertex1[l24+16] = y0;		vertex1[l24+17] = z0;
		vertex1[l24+18] = x0;		vertex1[l24+19] = y0;		vertex1[l24+20] = z0;
		vertex1[l24+21] = x0;		vertex1[l24+22] = y0;		vertex1[l24+23] = z0;
		
		lineCount++;
	}

	@Override
	public void lines(int nbLines, float[] pLines, int offset) {
		int lineIndex = 0;
		while(nbLines > 0){
			int actualLines = Math.min(maxLineCount - lineCount, nbLines);
			
			//transform data into vbo arrays with vertex duplication for extrusion
			for(int l=lineCount;l<actualLines + lineCount;++l)
			{
				final int lA = lineIndex*6 + offset;
				final int lB = lineIndex*6 + 3 + offset;
				final int l24 = l*24;
				vertex0[l24   ] = pLines[lA  ];		vertex0[l24+1 ] = pLines[lA+1];		vertex0[l24+2 ] = pLines[lA+2];
				vertex0[l24+3 ] = pLines[lA  ];		vertex0[l24+4 ] = pLines[lA+1];		vertex0[l24+5 ] = pLines[lA+2];
				vertex0[l24+6 ] = pLines[lA  ];		vertex0[l24+7 ] = pLines[lA+1];		vertex0[l24+8 ] = pLines[lA+2];
				vertex0[l24+9 ] = pLines[lA  ];		vertex0[l24+10] = pLines[lA+1];		vertex0[l24+11] = pLines[lA+2];
				
				vertex0[l24+12] = pLines[lB  ];		vertex0[l24+13] = pLines[lB+1];		vertex0[l24+14] = pLines[lB+2];
				vertex0[l24+15] = pLines[lB  ];		vertex0[l24+16] = pLines[lB+1];		vertex0[l24+17] = pLines[lB+2];
				vertex0[l24+18] = pLines[lB  ];		vertex0[l24+19] = pLines[lB+1];		vertex0[l24+20] = pLines[lB+2];
				vertex0[l24+21] = pLines[lB  ];		vertex0[l24+22] = pLines[lB+1];		vertex0[l24+23] = pLines[lB+2];
				
				vertex1[l24   ] = pLines[lB  ];		vertex1[l24+1 ] = pLines[lB+1];		vertex1[l24+2 ] = pLines[lB+2];
				vertex1[l24+3 ] = pLines[lB  ];		vertex1[l24+4 ] = pLines[lB+1];		vertex1[l24+5 ] = pLines[lB+2];
				vertex1[l24+6 ] = pLines[lB  ];		vertex1[l24+7 ] = pLines[lB+1];		vertex1[l24+8 ] = pLines[lB+2];
				vertex1[l24+9 ] = pLines[lB  ];		vertex1[l24+10] = pLines[lB+1];		vertex1[l24+11] = pLines[lB+2];
																								
				vertex1[l24+12] = pLines[lA  ];		vertex1[l24+13] = pLines[lA+1];		vertex1[l24+14] = pLines[lA+2];
				vertex1[l24+15] = pLines[lA  ];		vertex1[l24+16] = pLines[lA+1];		vertex1[l24+17] = pLines[lA+2];
				vertex1[l24+18] = pLines[lA  ];		vertex1[l24+19] = pLines[lA+1];		vertex1[l24+20] = pLines[lA+2];
				vertex1[l24+21] = pLines[lA  ];		vertex1[l24+22] = pLines[lA+1];		vertex1[l24+23] = pLines[lA+2];
				
				lineIndex++;
			}
			
			lineCount+= actualLines;
			nbLines -= actualLines;
			
			if(nbLines > 0)  
				flush();  // prepare for next loop
		}
	}

	/**
	 * Draw the volume lines to the framebuffer.<p>
	 * Invoke this method will disbale face_culling.
	 */
	@Override
	public void end() {
		if(!locked)
			throw new IllegalStateException("You should call begin first.");
		draw();
		
		locked = false;
//		GL20.glUseProgram(0);
//		GL13.glActiveTexture(GL13.GL_TEXTURE0);
//		GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
	}
	
	void flush(){
		draw();
		lineCount = 0;
	}
	
	@Override
	public void dispose() {
		if(linesEABO != null){
			linesEABO.dispose();
			linesEABO = null;
		}

		if(lineVA != null){
			lineVA.dispose();
			lineVA = null;
		}

		if(vertex0VBO != null){
			vertex0VBO.dispose();
			vertex0VBO = null;
		}

		if(vertex1VBO != null){
			vertex1VBO.dispose();
			vertex1VBO = null;
		}

		if(offsetDirUvVBO != null){
			offsetDirUvVBO.dispose();
			offsetDirUvVBO = null;
		}

		if(m_program != null){
			m_program.dispose();
			m_program = null;
		}
	}

	@Override
	public int getLineCount() { return lineCount;}

	@Override
	public int getMaxLineCount() {return maxLineCount;}

	void draw() {
		//copy data into vertex buffer object
//		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vertex0VBO);
//		GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER,0,GLUtil.wrap(vertex0, 0, lineCount * 8 * 3));
//		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vertex1VBO);
//		GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER,0,GLUtil.wrap(vertex1, 0, lineCount * 8 * 3));
//		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
//
//		GL11.glDisable(GL11.GL_CULL_FACE);	//no neend to cull, all triangles drawn will be visible
//
//		GL11.glEnable(GL31.GL_PRIMITIVE_RESTART);
//		GL31.glPrimitiveRestartIndex(END_OF_PRIMITIVE_ID);
//
//		GL30.glBindVertexArray(lineVA);
//		GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, linesEABO);
//		GL11.glDrawElements(GL11.GL_TRIANGLE_STRIP, lineCount*(8+1), GL11.GL_UNSIGNED_INT, 0);
//		GL30.glBindVertexArray(0);
//		GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);
//
//		GL11.glDisable(GL31.GL_PRIMITIVE_RESTART);

		// update buffers
		vertex0VBO.update(0, CacheBuffer.wrap(vertex0, 0, lineCount * 8 * 3));
		vertex1VBO.update(0, CacheBuffer.wrap(vertex1, 0, lineCount * 8 * 3));

		GLStateTracker stateTracker = GLStateTracker.getInstance();
		try{
			stateTracker.saveStates();

			stateTracker.setBlendState(null);  // TODO
			stateTracker.setRasterizerState(null);
			stateTracker.setDepthStencilState(m_dsstate);

			stateTracker.setVAO(lineVA);
			stateTracker.setProgram(m_program);
			m_program.setMVP(m_UniformData.mvp);
			m_program.setInvScrRatio(m_UniformData.screenRatio);
			m_program.setRadius(m_UniformData.radius);

			stateTracker.bindTexture(m_UniformData.texture, 0, 0);
			GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
			gl.glEnable(GLenum.GL_PRIMITIVE_RESTART);
			gl.glPrimitiveRestartIndex(END_OF_PRIMITIVE_ID);
			gl.glDrawElements(GLenum.GL_TRIANGLE_STRIP, lineCount*(8+1), GLenum.GL_UNSIGNED_INT, 0);
			gl.glDisable(GLenum.GL_PRIMITIVE_RESTART);
		}finally {
			stateTracker.restoreStates();
			stateTracker.reset();
		}
	}

	private final class VertexLineRendererProgram extends GLSLProgram{
		// attribute location
		int attr_position;
		int attr_othervert;
		int attr_offsetdir_uv;

		// uniform location
		int gpupLine_MVP;
		int gpupLine_radius;
		int gpupLine_invScrRatio;

		VertexLineRendererProgram() throws IOException {
			setSourceFromFiles("shader_libs/VolumeLine/VolumeLine_VertexLineVS.vert", "shader_libs/VolumeLine/VolumeLine_VertexLinePS.frag");
			enable();

			attr_position = getAttribLocation("Position");
			attr_othervert = getAttribLocation("PositionOther");
			attr_offsetdir_uv = getAttribLocation("OffsetUV");

			gpupLine_MVP = getUniformLocation("MVP");
			gpupLine_radius = getUniformLocation("radius");
			gpupLine_invScrRatio = getUniformLocation("invScrRatio");
			setTextureUniform("lineTexture", 0);

//			GL20.glUseProgram(programid);
//			GL20.glUniform1i(gpupLine_lineTexture, 0);
//			GL20.glUseProgram(0);
		}

		void setMVP(Matrix4f mvp){
			gl.glUniformMatrix4fv(gpupLine_MVP, false, CacheBuffer.wrap(mvp));
		}

		void setRadius(float radius) { gl.glUniform1f(gpupLine_radius, radius);}
		void setInvScrRatio(float invScrRatio) { gl.glUniform1f(gpupLine_invScrRatio, invScrRatio);}
	}

}
