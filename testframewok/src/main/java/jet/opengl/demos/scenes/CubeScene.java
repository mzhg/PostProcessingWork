package jet.opengl.demos.scenes;

import com.nvidia.developer.opengl.app.GLEventListener;
import com.nvidia.developer.opengl.app.NvInputTransformer;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import jet.opengl.postprocessing.common.Disposeable;
import jet.opengl.postprocessing.common.GLCheck;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.Texture2DDesc;
import jet.opengl.postprocessing.texture.TextureUtils;
import jet.opengl.postprocessing.util.CacheBuffer;
import jet.opengl.postprocessing.util.Numeric;

public class CubeScene implements GLEventListener {

	private static final int VERTEX_POS = 0;
	private static final int VERTEX_NORMAL = 1;
	private static final int VERTEX_COLOR = 2;
	private static final int INSTANCE_OFFSET = 3;
	private static final int INSTANCE_SCALE = 4;
	
	private static final int UBO_SCENE = 0;
	
	static final int        grid = 32;
	static final float      globalscale = 16.0f;
	
	private int m_FrameBuffer;
	private Texture2D m_SceneColorTex;
	private Texture2D m_SceneDepthTex;
	private GLSLProgram m_Program;
	private final Buffers m_Buffers = new Buffers();
	private final Projection m_Projection = new Projection();
	private final SceneData m_SceneUbo = new SceneData();
	
	private NvInputTransformer m_Transformer;
	private GLFuncProvider gl;
	private int m_SampleCount = 1; // MSAA count.
	private int m_SampleLastCount = 1; // MSAA count.

	public CubeScene(NvInputTransformer transformer){
		m_Transformer = transformer;
	}
	
	@Override
	public void onCreate() {
		m_Transformer.setTranslationVec(new Vector3f(0.0f, 0.0f, -4.2f));
		m_Transformer.setRotationVec(new Vector3f(Numeric.PI*0.35f, 0.0f, 0.0f));

		System.out.println("CubeScene::OnCreate");
		gl = GLFuncProviderFactory.getGLFuncProvider();

		gl.glEnable(GLenum.GL_CULL_FACE);
		gl.glEnable(GLenum.GL_DEPTH_TEST);
		
		m_FrameBuffer = gl.glGenFramebuffer();
		
		// init scene
		{
			String root = "Scenes/CubeScene/shaders/";
			try {
				m_Program = GLSLProgram.createFromFiles(root + "scene.vert", root + "scene.frag");
			} catch (IOException e) {
				e.printStackTrace();
			}
			initScene();
		}
	}
	
	int cube_instance_count;

	void initScene(){
		final int LEVELS = 4;
		initCube();
		
//		cubeVAO = ModelGenerator.genCube(1, true, false, false).genVAO();
		//   color  | translate | scale
		//  R,G,B,A |   X,Y,Z   | X,Y,Z 
		cube_instance_count = grid * grid * LEVELS;
		FloatBuffer buf = CacheBuffer.getCachedFloatBuffer(cube_instance_count * (4 + 3 + 3));
		
		final Vector4f color = new Vector4f();
		final Vector2f posxy = new Vector2f();
		final Vector3f pos = new Vector3f();
		final Vector3f size = new Vector3f();
		for (int i = 0; i < grid * grid; i++){
			color.x = Numeric.random(0.75f, 1);
			color.y = Numeric.random(0.75f, 1);
			color.z = Numeric.random(0.75f, 1);
			color.w = 1;
			
	        posxy.set(i % grid, i / grid);
	        
	        float depth = (float) (Math.sin(posxy.x*0.1f) * Math.cos(posxy.y*0.1f) * 2.0f);

	        for (int l = 0; l < LEVELS; l++){
	          pos.set(posxy.x, posxy.y, depth);

	          float scale = globalscale * 0.5f/(grid);
	          if (l != 0){
	            scale *= Math.pow(0.9f,l);
	            scale *= Numeric.random(0.5f, 1.0f);
	          }

//	          vec3 size = vec3(scale);
	          size.set(scale, scale, scale);

	          size.z *= Numeric.random(0.3f, 3);  //frand()*1.0f+1.0f; 
	          if (l != 0){
	            size.z *= Math.pow(0.7f,l);
	          }

	          pos.x -= grid/2;
	          pos.y -= grid/2;
	          pos.scale(globalscale/grid);
	          
	          depth += size.z;

	          pos.z = depth;

	          color.store(buf);
	          pos.store(buf);
	          size.store(buf);
	          
	          depth += size.z;
	        }
		}
		buf.flip();
		
		m_Buffers.instance_vbo = gl.glGenBuffer();
		gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, m_Buffers.instance_vbo);
		gl.glBufferData(GLenum.GL_ARRAY_BUFFER, buf, GLenum.GL_STATIC_DRAW);
		gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, 0);
		
		// build scnene VAO
		m_Buffers.scene_vao = gl.glGenVertexArray();
		gl.glBindVertexArray(m_Buffers.scene_vao);
		{
			gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, m_Buffers.scene_vbo);
			gl.glVertexAttribPointer(VERTEX_POS, 3, GLenum.GL_FLOAT, false, 0, 0);
			gl.glEnableVertexAttribArray(VERTEX_POS);

			gl.glVertexAttribPointer(VERTEX_NORMAL, 3, GLenum.GL_FLOAT, false, 0, 12 * 6 * 4);
			gl.glEnableVertexAttribArray(VERTEX_NORMAL);

			gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, m_Buffers.instance_vbo);
			gl.glVertexAttribPointer(VERTEX_COLOR, 4, GLenum.GL_FLOAT, false, (4 + 3 + 3) * 4, 0);
			gl.glVertexAttribPointer(INSTANCE_OFFSET, 3, GLenum.GL_FLOAT, false, (4 + 3 + 3) * 4, 16);
			gl.glVertexAttribPointer(INSTANCE_SCALE, 3, GLenum.GL_FLOAT, false, (4 + 3 + 3) * 4, 28);

			gl.glEnableVertexAttribArray(VERTEX_COLOR);
			gl.glEnableVertexAttribArray(INSTANCE_OFFSET);
			gl.glEnableVertexAttribArray(INSTANCE_SCALE);

			gl.glVertexAttribDivisor(VERTEX_COLOR, 1);
			gl.glVertexAttribDivisor(INSTANCE_OFFSET, 1);
			gl.glVertexAttribDivisor(INSTANCE_SCALE, 1);

			gl.glBindBuffer(GLenum.GL_ELEMENT_ARRAY_BUFFER, m_Buffers.scene_ibo);
		}
		gl.glBindVertexArray(0);
		gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, 0);
		gl.glBindBuffer(GLenum.GL_ELEMENT_ARRAY_BUFFER, 0);
		
		{  // Scene UBO
			m_Buffers.scene_ubo = gl.glGenBuffer();
			gl.glBindBuffer(GLenum.GL_UNIFORM_BUFFER, m_Buffers.scene_ubo);
			gl.glBufferData(GLenum.GL_UNIFORM_BUFFER, SceneData.SIZE, GLenum.GL_DYNAMIC_DRAW);
			gl.glBindBuffer(GLenum.GL_UNIFORM_BUFFER, 0);
		}
	}
	
	int cube_element_count;
	void initCube(){
		float side2 = 2 * 0.5f;
		float[] v = {
		   // Front
	       -side2, -side2, side2,
	        side2, -side2, side2,
	        side2,  side2, side2,
	       -side2,  side2, side2,
	       // Right
	        side2, -side2, side2,
	        side2, -side2, -side2,
	        side2,  side2, -side2,
	        side2,  side2, side2,
	       // Back
	       -side2, -side2, -side2,
	       -side2,  side2, -side2,
	        side2,  side2, -side2,
	        side2, -side2, -side2,
	       // Left
	       -side2, -side2, side2,
	       -side2,  side2, side2,
	       -side2,  side2, -side2,
	       -side2, -side2, -side2,
	       // Bottom
	       -side2, -side2, side2,
	       -side2, -side2, -side2,
	        side2, -side2, -side2,
	        side2, -side2, side2,
	       // Top
	       -side2,  side2, side2,
	        side2,  side2, side2,
	        side2,  side2, -side2,
	       -side2,  side2, -side2
	    };
		
		/** cube normals */
		final float[] cube_normal = {
		        // Front
		        0.0f, 0.0f, 1.0f,
		        0.0f, 0.0f, 1.0f,
		        0.0f, 0.0f, 1.0f,
		        0.0f, 0.0f, 1.0f,
		        // Right
		        1.0f, 0.0f, 0.0f,
		        1.0f, 0.0f, 0.0f,
		        1.0f, 0.0f, 0.0f,
		        1.0f, 0.0f, 0.0f,
		        // Back
		        0.0f, 0.0f, -1.0f,
		        0.0f, 0.0f, -1.0f,
		        0.0f, 0.0f, -1.0f,
		        0.0f, 0.0f, -1.0f,
		        // Left
		        -1.0f, 0.0f, 0.0f,
		        -1.0f, 0.0f, 0.0f,
		        -1.0f, 0.0f, 0.0f,
		        -1.0f, 0.0f, 0.0f,
		        // Bottom
		        0.0f, -1.0f, 0.0f,
		        0.0f, -1.0f, 0.0f,
		        0.0f, -1.0f, 0.0f,
		        0.0f, -1.0f, 0.0f,
		        // Top
		        0.0f, 1.0f, 0.0f,
		        0.0f, 1.0f, 0.0f,
		        0.0f, 1.0f, 0.0f,
		        0.0f, 1.0f, 0.0f
		    };
		
		/** Cube indices*/
		final byte cube_indices[] = {
		        0,1,2,0,2,3,
		        4,5,6,4,6,7,
		        8,9,10,8,10,11,
		        12,13,14,12,14,15,
		        16,17,18,16,18,19,
		        20,21,22,20,22,23
		    };

		FloatBuffer buf = CacheBuffer.getCachedFloatBuffer(v.length + cube_normal.length);
		buf.put(v).put(cube_normal).flip();
		
		m_Buffers.scene_vbo = gl.glGenBuffer();
		gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, m_Buffers.scene_vbo);
		gl.glBufferData(GLenum.GL_ARRAY_BUFFER, buf, GLenum.GL_STATIC_DRAW);
		
		m_Buffers.scene_ibo = gl.glGenBuffer();
		gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, m_Buffers.scene_ibo);
		gl.glBufferData(GLenum.GL_ARRAY_BUFFER, CacheBuffer.wrap(cube_indices), GLenum.GL_STATIC_DRAW);
		gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, 0);
		
		cube_element_count = cube_indices.length;
	}

	@Override
	public void onResize(int width, int height) {
		if(width == 0 || height == 0)
			return;
		
		gl.glViewport(0, 0, width, height);

		if(m_SceneColorTex == null || m_SceneColorTex.getWidth() != width || m_SceneColorTex.getHeight() != height){
			initFramebuffers(width, height, m_SampleCount);
		}
	}
	public boolean isMultiSample(){ return m_SceneColorTex.getSampleCount() > 1;}

	public void resoveMultisampleTexture(){
		gl.glBindFramebuffer(GLenum.GL_READ_FRAMEBUFFER, m_FrameBuffer);
		gl.glBindFramebuffer(GLenum.GL_DRAW_FRAMEBUFFER, 0);
		gl.glBlitFramebuffer(0,0,m_SceneColorTex.getWidth(),m_SceneColorTex.getHeight(),
				 			 0,0,m_SceneColorTex.getWidth(),m_SceneColorTex.getHeight(),
							 GLenum.GL_COLOR_BUFFER_BIT, GLenum.GL_NEAREST);
		gl.glBindFramebuffer(GLenum.GL_READ_FRAMEBUFFER, 0);
	}

	void initFramebuffers(int width, int height, int sampples){
		if(m_SceneColorTex != null){
			m_SceneColorTex.dispose();
			m_SceneDepthTex.dispose();
		}

		Texture2DDesc desc = new Texture2DDesc();
		desc.width = width;
		desc.height = height;
		desc.format = GLenum.GL_RGBA8;
		desc.mipLevels = 1;
		desc.arraySize = 1;
		desc.sampleCount = sampples;

		m_SceneColorTex = TextureUtils.createTexture2D(desc, null);
		m_SceneColorTex.setName("SceneColor");

		desc.format = GLenum.GL_DEPTH24_STENCIL8;
		m_SceneDepthTex = TextureUtils.createTexture2D(desc, null);
		m_SceneDepthTex.setName("SceneDepth");

		gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, m_FrameBuffer);
		{
			gl.glFramebufferTexture2D(GLenum.GL_FRAMEBUFFER, GLenum.GL_COLOR_ATTACHMENT0, m_SceneColorTex.getTarget(),m_SceneColorTex.getTexture(), 0);
			gl.glFramebufferTexture2D(GLenum.GL_FRAMEBUFFER, GLenum.GL_DEPTH_STENCIL_ATTACHMENT, m_SceneDepthTex.getTarget(),m_SceneDepthTex.getTexture(), 0);
		}
		gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, 0);

		/*
		desc.sampleDesc.count = 1;
		desc.format = GL30.GL_R32F;
		textures.scene_depthlinear = TextureUtils.createTexture2D(desc, null);

		if(!supportDSA){
			textures.scene_depthlinear.bind();
		}

		textures.scene_depthlinear.setWrapS(GL12.GL_CLAMP_TO_EDGE);
		textures.scene_depthlinear.setWrapT(GL12.GL_CLAMP_TO_EDGE);
		textures.scene_depthlinear.setMinFilter(GL11.GL_NEAREST);
		textures.scene_depthlinear.setMagFilter(GL11.GL_NEAREST);

		if(!supportDSA){
			textures.scene_depthlinear.unbind();
		}

		desc.format = GL11.GL_RGBA8;
		textures.scene_viewnormal = TextureUtils.createTexture2D(desc, null);

		if(!supportDSA){
			textures.scene_viewnormal.bind();
		}

		textures.scene_viewnormal.setWrapS(GL12.GL_CLAMP_TO_EDGE);
		textures.scene_viewnormal.setWrapT(GL12.GL_CLAMP_TO_EDGE);
		textures.scene_viewnormal.setMinFilter(GL11.GL_NEAREST);
		textures.scene_viewnormal.setMagFilter(GL11.GL_NEAREST);

		if(!supportDSA){
			textures.scene_viewnormal.unbind();
		}

		int[] swizzle;
		int formatAO;
		int v = USE_AO_SPECIALBLUR;
		if(v != 0){
			formatAO = GL30.GL_RG16F;
			swizzle = new int[]{GL11.GL_RED, GL11.GL_GREEN, GL11.GL_ZERO, GL11.GL_ZERO};
		}else{
			formatAO = GL30.GL_R8;
			swizzle = new int[]{GL11.GL_RED, GL11.GL_RED, GL11.GL_RED, GL11.GL_RED};
		}

		desc.format = formatAO;
		textures.hbao_result = TextureUtils.createTexture2D(desc, null);
		if(!supportDSA){
			textures.hbao_result.bind();
		}
		textures.hbao_result.setSwizzleRGBA(swizzle);
		textures.hbao_result.setWrapS(GL12.GL_CLAMP_TO_EDGE);
		textures.hbao_result.setWrapT(GL12.GL_CLAMP_TO_EDGE);
		if(!supportDSA){
			textures.hbao_result.unbind();
		}

		textures.hbao_blur = TextureUtils.createTexture2D(desc, null);
		if(!supportDSA){
			textures.hbao_blur.bind();
		}
		textures.hbao_blur.setSwizzleRGBA(swizzle);
		textures.hbao_blur.setWrapS(GL12.GL_CLAMP_TO_EDGE);
		textures.hbao_blur.setWrapT(GL12.GL_CLAMP_TO_EDGE);
		if(!supportDSA){
			textures.hbao_blur.unbind();
		}

		// interleaved hbao

		int quarterWidth  = ((width+3)/4);
		int quarterHeight = ((height+3)/4);

		desc.width = quarterWidth;
		desc.height = quarterHeight;
		desc.arraySize = HBAO_RANDOM_ELEMENTS;
		desc.format = GL30.GL_R32F;
		textures.hbao2_deptharray = TextureUtils.createTexture2D(desc, null);
		if(!supportDSA){
			textures.hbao2_deptharray.bind();
		}
		textures.hbao2_deptharray.setSwizzleRGBA(swizzle);
		textures.hbao2_deptharray.setWrapS(GL12.GL_CLAMP_TO_EDGE);
		textures.hbao2_deptharray.setWrapT(GL12.GL_CLAMP_TO_EDGE);
		textures.hbao2_deptharray.setMinFilter(GL11.GL_NEAREST);
		textures.hbao2_deptharray.setMagFilter(GL11.GL_NEAREST);
		if(!supportDSA){
			textures.hbao2_deptharray.unbind();
		}

		for (int i = 0; i < HBAO_RANDOM_ELEMENTS; i++){
			textures.hbao2_depthview[i] = TextureUtils.createTextureView(textures.hbao2_deptharray, GL11.GL_TEXTURE_2D, 0, 1, i, 1);

			if(!supportDSA){
				textures.hbao2_depthview[i].bind();
			}
			textures.hbao2_depthview[i].setSwizzleRGBA(swizzle);
			textures.hbao2_depthview[i].setWrapS(GL12.GL_CLAMP_TO_EDGE);
			textures.hbao2_depthview[i].setWrapT(GL12.GL_CLAMP_TO_EDGE);
			textures.hbao2_depthview[i].setMinFilter(GL11.GL_NEAREST);
			textures.hbao2_depthview[i].setMagFilter(GL11.GL_NEAREST);
			if(!supportDSA){
				textures.hbao2_depthview[i].unbind();
			}
		}

		desc.format = formatAO;
		textures.hbao2_resultarray = TextureUtils.createTexture2D(desc, null);
		if(!supportDSA){
			textures.hbao2_resultarray.bind();
		}
		textures.hbao2_resultarray.setSwizzleRGBA(swizzle);
		textures.hbao2_resultarray.setWrapS(GL12.GL_CLAMP_TO_EDGE);
		textures.hbao2_resultarray.setWrapT(GL12.GL_CLAMP_TO_EDGE);
		textures.hbao2_resultarray.setMinFilter(GL11.GL_NEAREST);
		textures.hbao2_resultarray.setMagFilter(GL11.GL_NEAREST);
		if(!supportDSA){
			textures.hbao2_resultarray.unbind();
		}

		GLError.checkError();
		*/
	}

	@Override
	public void draw() {
		GLCheck.checkError();
		gl.glUseProgram(0);
		int width   = m_Transformer.getScreenWidth();
		int height  = m_Transformer.getScreenHeigth();

		m_Projection.ortho       = /*m_control.m_sceneOrtho*/ false;
		m_Projection.orthoheight = /*m_control.m_sceneOrthoZoom*/1;
		m_Projection.update(width,height);


		gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER,m_FrameBuffer);
	    {
//	      NV_PROFILE_SECTION("Scene");
			gl.glViewport(0, 0, width, height);
			gl.glClearColor(0.2f, 0.2f, 0.2f, 0.0f);
			gl.glClearDepthf(1.0f);
			gl.glClear(GLenum.GL_COLOR_BUFFER_BIT | GLenum.GL_DEPTH_BUFFER_BIT);

			gl.glEnable(GLenum.GL_DEPTH_TEST);

//	      sceneUbo.viewport = uvec2(width,height);
	      m_SceneUbo.viewportX = width;
	      m_SceneUbo.viewportY = height;
	     
//	      nv_math::mat4 view = m_control.m_viewMatrix;
	      final Matrix4f modelView = m_SceneUbo.viewMatrix;
	      m_Transformer.getModelViewMat(modelView);

//	      sceneUbo.viewProjMatrix = projection.matrix * view;
	      Matrix4f.mul(m_Projection.matrix, modelView, m_SceneUbo.viewProjMatrix);
//	      sceneUbo.viewMatrix = view;
//	      sceneUbo.viewMatrixIT = nv_math::transpose(nv_math::invert(view));
	      Matrix4f.invert(modelView, m_SceneUbo.viewMatrixIT);

//	      glUseProgram(progManager.get(programs.draw_scene));
	      
	      m_Program.enable();
//	      m_Program.setVSShader(m_SceneVS);
//	      m_Program.setPSShader(m_ScenePS);

			gl.glBindBufferBase(GLenum.GL_UNIFORM_BUFFER, UBO_SCENE, m_Buffers.scene_ubo);
	      ByteBuffer buf = CacheBuffer.getCachedByteBuffer(SceneData.SIZE);
	      m_SceneUbo.store(buf);
	      buf.flip();
			gl.glBufferSubData(GLenum.GL_UNIFORM_BUFFER,0, buf);

			gl.glBindVertexArray(m_Buffers.scene_vao);
			gl.glDrawElementsInstancedBaseVertex(GLenum.GL_TRIANGLES, cube_element_count, GLenum.GL_UNSIGNED_BYTE, 0, cube_instance_count, 0);
			gl.glBindVertexArray(0);
	    }

		gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER,0);
	}
	
	public void getViewProjMatrix(Matrix4f out){ Matrix4f.mul(m_Projection.matrix, m_SceneUbo.viewMatrix, out);}
	public Texture2D getSceneColor() {return m_SceneColorTex;}
	public Texture2D getSceneDepth() {return m_SceneDepthTex;}
	public Matrix4f getProjMat()    { return m_Projection.matrix;}
	public Matrix4f getViewMat(){ return m_SceneUbo.viewMatrix;}
	public float getSceneNearPlane() { return m_Projection.nearplane;}
	public float getSceneFarPlane() { return m_Projection.farplane;}
	public float getFovInRadian()   { return (float)Math.toRadians(m_Projection.fov);}

	@Override
	public void onDestroy() {
		if(m_FrameBuffer != 0){
			gl.glDeleteFramebuffer(m_FrameBuffer);
			m_FrameBuffer = 0;
		}

		if(m_Program != null){
			m_Program.dispose();
			m_Program = null;
		}

		m_Buffers.dispose();
	}
	
	private final class Buffers implements Disposeable {
		int scene_vbo,
	        scene_ibo,
	        scene_ubo,
	        scene_vao,
	        hbao_ubo,
			instance_vbo;
		
		@Override
		public void dispose() {
			if(scene_vbo != 0){
				gl.glDeleteBuffer(scene_ibo);
				gl.glDeleteBuffer(scene_ibo);
				gl.glDeleteBuffer(scene_ubo);
				gl.glDeleteBuffer(hbao_ubo);
				gl.glDeleteBuffer(instance_vbo);

				gl.glDeleteVertexArray(scene_vao);
			}
			
			scene_vbo = 0;
			scene_ibo = 0;
			scene_ubo = 0;
			hbao_ubo = 0;
			instance_vbo = 0;
			scene_vao = 0;
		}
	}
	
	private final class SceneData{
		static final int SIZE = (16 * 3 + 4) * 4;
		
		final Matrix4f viewProjMatrix = new Matrix4f();
		final Matrix4f viewMatrix     = new Matrix4f();
		final Matrix4f viewMatrixIT   = new Matrix4f();
		
		int viewportX;
		int viewportY;
		int x0,x1;  // pad
		
		void store(ByteBuffer buf){
			viewProjMatrix.store(buf);
			viewMatrix.store(buf);
			viewMatrixIT.store(buf);
			buf.putInt(viewportX);
			buf.putInt(viewportY);
			buf.putInt(x0);
			buf.putInt(x1);
		}
	}
	
	private final class Projection{
		float nearplane = 0.1f;
	    float farplane = 100.0f;
	    float fov = 45.f;
	    float orthoheight = 1.0f;
	    boolean  ortho = false;
	    final Matrix4f matrix = new Matrix4f();
	    
	    void update(int width, int height){
	    	float aspect = (float)width/height;
	    	if(ortho){
	    		Matrix4f.ortho(-orthoheight*0.5f*aspect, orthoheight*0.5f*aspect, -orthoheight*0.5f, orthoheight*0.5f, nearplane, farplane, matrix);
	    	}else{
	    		Matrix4f.perspective(fov, aspect, nearplane, farplane, matrix);
	    	}
	    }
	}

}
