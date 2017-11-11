package jet.opengl.demos.gpupro.noise;

import com.nvidia.developer.opengl.app.NvSampleApp;
import com.nvidia.developer.opengl.models.DrawMode;
import com.nvidia.developer.opengl.models.GLVAO;
import com.nvidia.developer.opengl.models.ModelGenerator;
import com.nvidia.developer.opengl.models.QuadricBuilder;
import com.nvidia.developer.opengl.models.QuadricMesh;
import com.nvidia.developer.opengl.models.QuadricSphere;
import com.nvidia.developer.opengl.utils.HDRImage;

import org.lwjgl.util.vector.Matrix4f;

import java.io.IOException;

import jet.opengl.demos.postprocessing.hdr.SkyProgram;
import jet.opengl.postprocessing.common.GLCheck;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.AttribBinder;
import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.util.CacheBuffer;

public class ShaderNoise extends NvSampleApp{
	private static final int POSITION_LOC = 0;
	private static final int NORMAL_LOC = 1;
	private static final int TEXTURE_LOC = 2;
	
	ShaderProgram fbmnoise;
	ShaderProgram flownoise2;
	ShaderProgram snoise3;
	ShaderProgram spots;
	ShaderProgram tiles;
	SkyProgram sky_program;
	GLVAO skyVAO;
	
	final Matrix4f model = new Matrix4f();
	final Matrix4f view = new Matrix4f();
	final Matrix4f proj = new Matrix4f();
	final Matrix4f mvp = new Matrix4f();

	int quadBuf;
	int quadVBO;
	private GLVAO m_sphere;
	
	int cubeMap;
	private GLFuncProvider gl;
	private boolean m_printOnce;

	@Override
	protected void initRendering() {
		gl = GLFuncProviderFactory.getGLFuncProvider();
		initPrograms();
		m_transformer.setTranslation(0,0, -10);

		QuadricBuilder builder = new QuadricBuilder();
		builder.setXSteps(20).setYSteps(20);
		builder.setDrawMode(DrawMode.FILL);
		builder.setCenterToOrigin(true);
		builder.setPostionLocation(POSITION_LOC);
		builder.setNormalLocation(NORMAL_LOC);
		builder.setTexCoordLocation(TEXTURE_LOC);

		m_sphere = new QuadricMesh(builder, new QuadricSphere(1)).getModel().genVAO();
//		GL.getCurrent().checkGLError();
		initQuadList();
		
//		GL.getCurrent().checkGLError();
		
		HDRImage image = new HDRImage();
		image.loadHDRIFromFile("HDR\\textures\\rnl_cross_mmp_s.hdr");
		image.convertCrossToCubemap();
		cubeMap = createCubemapTexture(image, GLenum.GL_RGB, true);

		try {
			sky_program = new SkyProgram();
		} catch (IOException e) {
			e.printStackTrace();
		}

		getGLContext().setSwapInterval(0);
	}
	
	private int createCubemapTexture(HDRImage img, int internalformat, boolean filtering)
    {
        int tex = gl.glGenTexture();
		gl.glBindTexture(GLenum.GL_TEXTURE_CUBE_MAP, tex);

		gl.glTexParameteri(GLenum.GL_TEXTURE_CUBE_MAP, GLenum.GL_TEXTURE_MAG_FILTER, filtering ? GLenum.GL_LINEAR : GLenum.GL_NEAREST);
		gl.glTexParameteri(GLenum.GL_TEXTURE_CUBE_MAP, GLenum.GL_TEXTURE_MIN_FILTER, filtering ? GLenum.GL_LINEAR_MIPMAP_LINEAR : GLenum.GL_NEAREST_MIPMAP_NEAREST);
		gl.glTexParameteri(GLenum.GL_TEXTURE_CUBE_MAP, GLenum.GL_TEXTURE_WRAP_S, GLenum.GL_CLAMP_TO_EDGE);
		gl.glTexParameteri(GLenum.GL_TEXTURE_CUBE_MAP, GLenum.GL_TEXTURE_WRAP_T, GLenum.GL_CLAMP_TO_EDGE);

		gl.glPixelStorei(GLenum.GL_UNPACK_ALIGNMENT, 1);
       
    	short[] out = new short[img.getWidth()*img.getHeight()*3];
        for(int i=0; i<6; i++) {
        	HDRImage.fp32toFp16(img.getLevel(0, GLenum.GL_TEXTURE_CUBE_MAP_POSITIVE_X + i), 0, out, 0, img.getWidth(), img.getHeight());
    	    gl.glTexImage2D(GLenum.GL_TEXTURE_CUBE_MAP_POSITIVE_X + i, 0,
					GLenum.GL_RGB16F, img.getWidth(), img.getHeight(), 0,
					GLenum.GL_RGB, GLenum.GL_HALF_FLOAT, CacheBuffer.wrap(out));
        }
		gl.glGenerateMipmap(GLenum.GL_TEXTURE_CUBE_MAP);
		gl.glPixelStorei(GLenum.GL_UNPACK_ALIGNMENT, 4);
        return tex;
    }
	
	@Override
	public void reshape(int width, int height) {
		if(width <=0 || height <=0)
			return;

		gl.glViewport(0, 0, width, height);
		Matrix4f.perspective(15.0f, (float)width/(float)height, 1.0f, 100.0f, proj);
	}
	
	@Override
	public void display() {
		renderScene();
		m_printOnce = true;
	}

	private void renderScene(){
		gl.glClear(GLenum.GL_COLOR_BUFFER_BIT | GLenum.GL_DEPTH_BUFFER_BIT);
		gl.glClearColor(0.0f, 0.3f, 0.5f, 0.0f);
		gl.glViewport(0,0, getGLContext().width(), getGLContext().height());

//	    final Matrix4f vp = camera.getVP();	    \
		m_transformer.getModelViewMat(view);
		Matrix4f vp = Matrix4f.mul(proj, view, this.mvp);
		float time = getTotalTime();
		view.invert();

		// draw the sky box
		{
			gl.glDisable(GLenum.GL_DEPTH_TEST);
			gl.glDisable(GLenum.GL_CULL_FACE);
			sky_program.enable();
			sky_program.applyProjMat(proj);
			sky_program.applyViewMat(view);
			gl.glActiveTexture(GLenum.GL_TEXTURE0);
			gl.glBindTexture(GLenum.GL_TEXTURE_CUBE_MAP, cubeMap);
			skyVAO.bind();
			skyVAO.draw(GLenum.GL_TRIANGLES);
			skyVAO.unbind();
			gl.glBindTexture(GLenum.GL_TEXTURE_CUBE_MAP, 0);
			gl.glEnable(GLenum.GL_DEPTH_TEST);
			GLCheck.checkError();

			if(!m_printOnce){
				sky_program.setName("Sky Render");
				sky_program.printPrograminfo();
			}
		}

		// draw the plane
		{
			flownoise2.enable();
			flownoise2.setTime(time);
			model.setIdentity();
			model.translate(0.0f, 0.0f, -0.65f);
			model.scale(4, 4, 1);

			Matrix4f mvp = Matrix4f.mul(vp, model, model);
			flownoise2.setMVP(mvp);
//		    plane.setPositionLocation(flownoise2.getPositionAttribute());
//		    plane.draw();

			gl.glBindVertexArray(quadVBO);
			gl.glDrawArrays(GLenum.GL_TRIANGLE_STRIP, 0, 4);
			gl.glBindVertexArray(0);

			if(!m_printOnce){
				flownoise2.setName("FlowNoise");
				flownoise2.printPrograminfo();
			}
		}


		/// Render four spheres, each with their own shader ////
		// first
		{
			snoise3.enable();
			snoise3.setTime(time);
			model.setIdentity();
			model.translate(-1.8f, -0.0f, 0.0f);
			model.scale(0.5f, 0.5f, 0.5f);
			Matrix4f mvp = Matrix4f.mul(vp, model, model);
			snoise3.setMVP(mvp);

//	    	sphere.setPositionLocation(snoise3.getPositionAttribute());
			m_sphere.bind();
			m_sphere.draw(GLenum.GL_TRIANGLES);
			m_sphere.unbind();
		}


		// second
		{
			fbmnoise.enable();
			fbmnoise.setTime(time);
			model.setIdentity();
			model.translate(-0.6f, -0.0f, 0.0f);
			model.scale(0.5f, 0.5f, 0.5f);
			Matrix4f mvp = Matrix4f.mul(vp, model, model);
			fbmnoise.setMVP(mvp);

//	    	sphere.setPositionLocation(fbmnoise.getPositionAttribute());
			m_sphere.bind();
			m_sphere.draw(GLenum.GL_TRIANGLES);
			m_sphere.unbind();
		}

		// third
		{
			spots.enable();
			spots.setTime(time);
			model.setIdentity();
			model.translate(0.6f, -0.0f, 0.0f);
			model.scale(0.5f, 0.5f, 0.5f);
			Matrix4f mvp = Matrix4f.mul(vp, model, model);
			spots.setMVP(mvp);

//	    	sphere.setPositionLocation(spots.getPositionAttribute());
			m_sphere.bind();
			m_sphere.draw(GLenum.GL_TRIANGLES);
			m_sphere.unbind();
		}

		// fourth
		{
			tiles.enable();
			tiles.setTime(time);
			model.setIdentity();
			model.translate(1.8f, -0.0f, 0.0f);
			model.scale(0.5f, 0.5f, 0.5f);
			Matrix4f mvp = Matrix4f.mul(vp, model, model);
			tiles.setMVP(mvp);

//	    	sphere.setPositionLocation(tiles.getPositionAttribute());
			m_sphere.bind();
			m_sphere.draw(GLenum.GL_TRIANGLES);
			m_sphere.unbind();
		}

		GLCheck.checkError();
	}

	private void initPrograms() {
		fbmnoise = new ShaderProgram("fbmnoise.vert", "fbmnoise.frag");
		flownoise2 = new ShaderProgram("flownoise2.vert", "flownoise2.frag");
		snoise3 = new ShaderProgram("snoise3.vert", "snoise3.frag");
		spots = new ShaderProgram("spots.vert", "spots.frag");
		tiles = new ShaderProgram("tiles.vert", "tiles.frag");
		
//		GL.getCurrent().checkGLError();
		skyVAO = ModelGenerator.genCube(80, false, false, false).genVAO();
	}
	
	 void initQuadList()
	 {
		 quadVBO = gl.glGenVertexArray();
		 gl.glBindVertexArray(quadVBO);
		 {
			 quadBuf = gl.glGenBuffer();
			 gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, quadBuf);
			 float[] data = {
					 -1.0f, -1.0f, 0.0f,
					 1.0f, -1.0f, 0.0f,
					 -1.0f, 1.0f, 0.0f,
					 1.0f, 1.0f, 0.0f
			 };
			 gl.glBufferData(GLenum.GL_ARRAY_BUFFER, CacheBuffer.wrap(data), GLenum.GL_STATIC_DRAW);
			 gl.glVertexAttribPointer(0, 3, GLenum.GL_FLOAT, false, 0, 0);
			 gl.glEnableVertexAttribArray(0);

			 gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, 0);
		 }
		 gl.glBindVertexArray(0);
	 }

	private final class ShaderProgram extends GLSLProgram{
		int positionAttr;
		int timeUniform;
		int mvpUniform;

		public ShaderProgram(String vertexfile, String fragfile) {
			try {
				String path = "gpupro/ShaderNoise/shaders/";
				setAttribBinding(new AttribBinder("aPosition", POSITION_LOC));
				setSourceFromFiles(path+vertexfile, path+fragfile);
				int programid = getProgram();
				
				positionAttr = gl.glGetAttribLocation(programid, "aPosition");
				timeUniform  = gl.glGetUniformLocation(programid, "time");
				mvpUniform   = gl.glGetUniformLocation(programid, "mvp");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		public int getPositionAttribute() {return positionAttr;}
		public void setTime(float time){
			if(timeUniform != -1)
				gl.glUniform1f(timeUniform, time);
		}
		
		public void setMVP(Matrix4f mat){
			gl.glUniformMatrix4fv(mvpUniform, false, CacheBuffer.wrap(mat));
		}
	}
}
