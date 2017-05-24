package jet.opengl.render.volumeline;

import org.lwjgl.util.vector.Matrix4f;

import java.io.IOException;

import jet.opengl.postprocessing.buffer.AttribDesc;
import jet.opengl.postprocessing.buffer.BufferBinding;
import jet.opengl.postprocessing.buffer.BufferGL;
import jet.opengl.postprocessing.buffer.VertexArrayObject;
import jet.opengl.postprocessing.common.DepthStencilState;
import jet.opengl.postprocessing.common.GLCheck;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLStateTracker;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.common.RasterizerState;
import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.shader.ShaderLoader;
import jet.opengl.postprocessing.shader.ShaderSourceItem;
import jet.opengl.postprocessing.shader.ShaderType;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.util.CacheBuffer;

final class GeometryLinesRenderer extends VolumeLinesRenderer{

	private static final int LINERENDERER_UNIFORM_ATTR_POSITION     = 0;

	GeometryLinesRendererProgram m_program;
	BufferGL lineVBO;
	VertexArrayObject lineVA;
	
	final int maxLine;
	final float[] lineVertex;
	int lineCount;
	boolean locked;
	DepthStencilState m_dsstate;
	RasterizerState   m_rsstate;

	final UniformData m_uniformData = new UniformData();
	
	public GeometryLinesRenderer()throws IOException  {
		this(1024);
	}
	
	public GeometryLinesRenderer(int maxLine) throws IOException {
		this.maxLine = Math.max(1, maxLine);
		this.lineVertex = new float[maxLine * 6];

		m_program = new GeometryLinesRendererProgram();

//		lineVBO = GL15.glGenBuffers();
//		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, lineVBO);
//		GL15.glBufferData(GL15.GL_ARRAY_BUFFER, maxLine*2*3*4, GL15.GL_STREAM_DRAW);
//		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
//
//		lineVA = GL30.glGenVertexArrays();
//		GL30.glBindVertexArray(lineVA);
//		{
//			GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, lineVBO);
//			GL20.glVertexAttribPointer(LINERENDERER_UNIFORM_ATTR_POSITION, 3, GL11.GL_FLOAT, false, 0, 0);
//			GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
//
//			GL20.glEnableVertexAttribArray(LINERENDERER_UNIFORM_ATTR_POSITION);
//		}
//		GL30.glBindVertexArray(0);

		lineVBO = new BufferGL();
		lineVBO.initlize(GLenum.GL_ARRAY_BUFFER, maxLine*2*3*4, null, GLenum.GL_STREAM_DRAW);

		AttribDesc desc = new AttribDesc();
		desc.index = LINERENDERER_UNIFORM_ATTR_POSITION;
		desc.size = 3;
		BufferBinding binding = new BufferBinding(lineVBO, desc);
		lineVA = new VertexArrayObject();
		lineVA.initlize(new BufferBinding[]{binding}, null);
		if(GLCheck.CHECK)
			GLCheck.checkError();

		m_dsstate = new DepthStencilState();
		m_dsstate.depthEnable = true;

		m_rsstate = new RasterizerState();
		m_rsstate.cullFaceEnable = true;
		m_rsstate.cullMode = GLenum.GL_BACK;
	}

	private final static class UniformData{
		Matrix4f modeView;
		Matrix4f proj;
		Texture2D texture;
		float radius;
	}

	@Override
	public void begin(float radius, Matrix4f mvpMat, Matrix4f mvMat,
					  Matrix4f pMat, Texture2D texture, float screenRatio) {
		if(locked)
			throw new IllegalStateException("begin called twice!");
		
		locked = true;
		lineCount = 0;

		m_uniformData.modeView = mvMat;
		m_uniformData.proj = pMat;
		m_uniformData.radius = radius;
		m_uniformData.texture = texture;
//		GL20.glUseProgram(programid);
//
//		GL20.glUniformMatrix4fv(gpupLine_mvMat, false, GLUtil.wrap(mvMat));
//		GL20.glUniformMatrix4fv(gpupLine_pMat, false, GLUtil.wrap(pMat) );
//		GL20.glUniform1f(gpupLine_radius, radius);
//
//		GL13.glActiveTexture(GL13.GL_TEXTURE0);
//		GL11.glBindTexture(GL11.GL_TEXTURE_2D,texture);
//
//		//enable back face culling to avoid paying for back face shading
//		GL11.glEnable(GL11.GL_CULL_FACE);
//		//glCullFace(GL_FRONT);
//		GL11.glCullFace(GL11.GL_BACK);
//		GL11.glFrontFace(GL11.GL_CCW);
	}

	@Override
	public void line(float x0, float y0, float z0, float x1, float y1, float z1) {
		if(lineCount >= maxLine){
			flush();
		}
		
		int index = lineCount * 6;
		lineVertex[index++] = x0;
		lineVertex[index++] = y0;
		lineVertex[index++] = z0;
		lineVertex[index++] = x1;
		lineVertex[index++] = y1;
		lineVertex[index++] = z1;
		
		lineCount ++;
	}

	@Override
	public void lines(int nbLines, float[] pLines, int offset) {
		while(nbLines > 0){
			int actualLines = Math.min(maxLine - lineCount, nbLines);
			int src_index = offset;
			int dst_index = lineCount * 6;
			System.arraycopy(pLines, src_index, lineVertex, dst_index, actualLines * 6);
			nbLines -= actualLines;
			offset += actualLines * 6;
			lineCount += actualLines;
			if(nbLines > 0){
				flush();  // prepare for next loop
			}
		}
	}

	@Override
	public void end() {
		if(!locked)
			throw new IllegalStateException("You should call begin first.");
		
		draw();
		
//		GL11.glDisable(GL11.GL_CULL_FACE);
//		GL20.glUseProgram(0);
//		GL13.glActiveTexture(GL13.GL_TEXTURE0);
//		GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
		locked = false;
	}
	
	void flush(){
		draw();
		lineCount = 0;
	}

	@Override
	public void dispose() {
		if(lineVBO != null){
			lineVBO.dispose();
			lineVBO = null;
		}

		if(lineVA != null){
			lineVA.dispose();
			lineVA = null;
		}

		if(m_program != null){
			m_program.dispose();
			m_program = null;
		}
	}

	@Override
	public int getLineCount() { return lineCount;}

	@Override
	public int getMaxLineCount() { return maxLine;}

	void draw() {
//		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, lineVBO);
//		GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER,0, GLUtil.wrap(lineVertex, 0, lineCount*2*3));		//send data to the graphic card
//		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
//
//		GL30.glBindVertexArray(lineVA);
//		GL31.glDrawArraysInstanced(GL11.GL_LINES, 0, lineCount*2, 1);							//render lines as usual and let the shader do the trick
//		GL30.glBindVertexArray(0);

		lineVBO.update(0, CacheBuffer.wrap(lineVertex, 0, lineCount * 2 * 3));
		lineVBO.unbind();

		GLStateTracker stateTracker = GLStateTracker.getInstance();
		try{
			stateTracker.saveStates();
//			m_StateTracker.clearFlags(true);

//            m_StateTracker.setCurrentFramebuffer();
//            m_StateTracker.setCurrentViewport();
			stateTracker.setVAO(lineVA);
			stateTracker.setProgram(m_program);

			m_program.setRadius(m_uniformData.radius);
			m_program.setModeViewMatrix(m_uniformData.modeView);
			m_program.setProjMatrix(m_uniformData.proj);

			stateTracker.bindTexture(m_uniformData.texture, 0,0);
			stateTracker.setRasterizerState(m_rsstate);
			stateTracker.setDepthStencilState(m_dsstate);
			stateTracker.setBlendState(null);

			GLFuncProviderFactory.getGLFuncProvider().glDrawArrays(GLenum.GL_LINES, 0, lineCount*2); //render lines as usual
		}finally {
			stateTracker.restoreStates();
			stateTracker.reset();
		}
	}

	private final class GeometryLinesRendererProgram extends GLSLProgram{
		int gpupLine_mvMat;
		int gpupLine_pMat;
		int gpupLine_radius;

		public GeometryLinesRendererProgram() throws IOException {
			CharSequence vertSrc = ShaderLoader.loadShaderFile("shader_libs/VolumeLine/VolumeLine_GeometryLineVS.vert", false);
			CharSequence geomSrc = ShaderLoader.loadShaderFile("shader_libs/VolumeLine/VolumeLine_GeometryLineGS.geom", false);
			CharSequence fragSrc = ShaderLoader.loadShaderFile("shader_libs/VolumeLine/VolumeLine_GeometryLinePS.frag", false);

			ShaderSourceItem vs_item = new ShaderSourceItem(vertSrc, ShaderType.VERTEX);
			ShaderSourceItem gs_item = new ShaderSourceItem(geomSrc, ShaderType.GEOMETRY);
			ShaderSourceItem ps_item = new ShaderSourceItem(fragSrc, ShaderType.FRAGMENT);

			setSourceFromStrings(vs_item, gs_item, ps_item);

			//specify required parameter by geometry shader
//			ARBGeometryShader4.glProgramParameteriARB(programid,ARBGeometryShader4.GL_GEOMETRY_INPUT_TYPE_ARB, GL11.GL_LINES);
//			ARBGeometryShader4.glProgramParameteriARB(programid,ARBGeometryShader4.GL_GEOMETRY_OUTPUT_TYPE_ARB,GL11.GL_TRIANGLE_STRIP);
			int GS_HardwareLimit_maxVerticesOut = gl.glGetInteger(GLenum.GL_MAX_GEOMETRY_OUTPUT_VERTICES_ARB);
			if(GS_HardwareLimit_maxVerticesOut< 16)
			{
//			std::cout << GS_maxVerticesOut << "max vertices produced specified, but your graphic card is limited to "<< GS_HardwareLimit_maxVerticesOut<<"."<< std::endl;
				System.out.println("16 max vertices produced specified, but your graphic card is limited to " + GS_HardwareLimit_maxVerticesOut + ".");
				gl.glProgramParameteri(getProgram(),GLenum.GL_GEOMETRY_VERTICES_OUT_ARB,GS_HardwareLimit_maxVerticesOut);
			}
			else
				gl.glProgramParameteri(getProgram(),GLenum.GL_GEOMETRY_VERTICES_OUT_ARB,16);
			enable();
			gpupLine_mvMat = getUniformLocation("mvMat");
			gpupLine_pMat = getUniformLocation("pMat");
			gpupLine_radius = getUniformLocation("radius");

			setTextureUniform("gradientTexture", 0);
			if(GLCheck.CHECK)
				GLCheck.checkError("PostProcessingHBAOProgram::init()");
		}

		void setModeViewMatrix(Matrix4f mat) { gl.glUniformMatrix4fv(gpupLine_mvMat, false, CacheBuffer.wrap(mat));}
		void setProjMatrix(Matrix4f proj) { gl.glUniformMatrix4fv(gpupLine_pMat, false, CacheBuffer.wrap(proj));}
		void setRadius(float radius) { gl.glUniform1f(gpupLine_radius, radius);}
	}

}
