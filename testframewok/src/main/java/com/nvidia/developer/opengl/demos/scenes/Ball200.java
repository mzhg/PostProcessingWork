package com.nvidia.developer.opengl.demos.scenes;

import com.nvidia.developer.opengl.app.GLEventListener;
import com.nvidia.developer.opengl.app.NvCameraMotionType;
import com.nvidia.developer.opengl.app.NvInputTransformer;
import com.nvidia.developer.opengl.models.AttribFloatArray;
import com.nvidia.developer.opengl.models.DrawMode;
import com.nvidia.developer.opengl.models.GLVAO;
import com.nvidia.developer.opengl.models.Model;
import com.nvidia.developer.opengl.models.QuadricBuilder;
import com.nvidia.developer.opengl.models.QuadricMesh;
import com.nvidia.developer.opengl.models.QuadricPlane;
import com.nvidia.developer.opengl.models.QuadricSphere;
import com.nvidia.developer.opengl.utils.NvStopWatch;
import com.nvidia.developer.opengl.utils.ShadowmapGenerateProgram;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import java.io.IOException;

import jet.opengl.postprocessing.common.GLCheck;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.core.OpenGLProgram;
import jet.opengl.postprocessing.shader.AttribBinder;
import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.Texture2DDesc;
import jet.opengl.postprocessing.texture.TextureUtils;
import jet.opengl.postprocessing.util.CacheBuffer;
import jet.opengl.postprocessing.util.Numeric;

@SuppressWarnings("unused")
public class Ball200 implements GLEventListener {

	private static final int SHADOW_MAP_SIZE = 1024;
	private static final int NUM_SPHERES = 200;
	private static final String SHADER_PATH = "Scenes\\Ball200\\shaders\\";
	private static final String TEXTURES_PATH = "Scenes\\Ball200\\textures\\";
	
	private static final AttribBinder POSITION = new AttribBinder("gxl3d_Position", 0);
	private static final AttribBinder NORMAL = new AttribBinder("gxl3d_Normal", 1);
	private static final AttribBinder TEXCOORD0 = new AttribBinder("gxl3d_TexCoord0", 2);
	private static final AttribBinder INSTANCE_POSITION = new AttribBinder("gxl3d_Instance_Position", 3);
	private static final AttribBinder INSTANCE_ROTATION = new AttribBinder("gxl3d_Instance_Rotation", 4);
	
	private static final AttribBinder TEXCOORD1 = new AttribBinder("gxl3d_TexCoord1", 5);
	private static final AttribBinder COLOR = new AttribBinder("gxl3d_Color", 6);
	private static final AttribBinder TANGENT = new AttribBinder("gxl3d_Tangent", 7);
	
	private NvInputTransformer m_Transformer;
	
	// depth render target
	private int depth_rt;
	private Texture2D depth_tex;
	
	// scene render target
	private int scene_rt;
	private Texture2D scene_color_tex;
	private Texture2D scene_depth_tex;
	
	private Texture2D ground_tex_diffuse;
	private Texture2D sphere_tex_diffuse;
	
	private PhongProgram phong_prog;
	private ColorProgram color_prog;
	private ShadowmapGenerateProgram zpass_prog;
	private ShadowMappingPassProg shadow_mapping_prog;
	private ZpassInstanceProgram zpass_prog_gi;
	private ShadowMappingPassInstanceProg shadow_mapping_prog_gi;
	
//	private FrustumeRender frustumeRender;
//	private final FrustumeRender.Params frustumeParams = new FrustumeRender.Params();

	private GLVAO sphereVAO;
	private GLVAO planeVAO;
	
	private final Matrix4f m_LightProj = new Matrix4f();
	private final Matrix4f m_LightView = new Matrix4f();
	private final Matrix4f m_LightMVP  = new Matrix4f();
	private final Matrix4f m_LightShift  = new Matrix4f();
	private final Matrix4f m_LightTexture = new Matrix4f();
	private final Matrix4f m_Proj = new Matrix4f();
	private final Matrix4f m_View = new Matrix4f();
	private final Matrix4f m_MVP = new Matrix4f();
	private final Vector3f m_CameraPos = new Vector3f();
	private final Vector3f m_ViewDir   = new Vector3f();
	private final Vector3f m_LightPosition = new Vector3f(-20, 60,20);
	
	private NvStopWatch m_Timer;
	private boolean m_bPrintProgram = true;
	private GLFuncProvider gl;
	
	public Ball200(NvInputTransformer transformer){
		m_Transformer = transformer;
		m_Transformer.setMotionMode(NvCameraMotionType.FIRST_PERSON);
		m_Transformer.setTranslation(0, -10, -45);
		
		m_Timer = new NvStopWatch();
//		m_LightShift.m00 = m_LightShift.m11 = m_LightShift.m22 = 0.5f;
//		m_LightShift.m30 = m_LightShift.m31 = m_LightShift.m32 = 0.5f;
	}
	
	@Override
	public void onCreate() {
		gl = GLFuncProviderFactory.getGLFuncProvider();
		{
			// Create the ShadowMap texture and RenderTarget
			depth_rt = gl.glGenFramebuffer();
			Texture2DDesc depthDesc = new Texture2DDesc(SHADOW_MAP_SIZE, SHADOW_MAP_SIZE, GLenum.GL_DEPTH_COMPONENT16);
			depth_tex = TextureUtils.createTexture2D(depthDesc, null);
			depth_tex.setMagFilter(GLenum.GL_LINEAR);
			depth_tex.setMinFilter(GLenum.GL_NEAREST);
			depth_tex.setWrapS(GLenum.GL_CLAMP_TO_BORDER);
			depth_tex.setWrapT(GLenum.GL_CLAMP_TO_BORDER);
			gl.glTextureParameteri(depth_tex.getTexture(), GLenum.GL_TEXTURE_COMPARE_FUNC, GLenum.GL_LESS);
			gl.glTextureParameteri(depth_tex.getTexture(), GLenum.GL_TEXTURE_COMPARE_MODE, GLenum.GL_COMPARE_REF_TO_TEXTURE);
			gl.glTextureParameterfv(depth_tex.getTexture(), GLenum.GL_TEXTURE_BORDER_COLOR, new float[]{1f, 1, 1, 1});

			gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, depth_rt);
			{
				gl.glFramebufferTexture2D(GLenum.GL_FRAMEBUFFER, GLenum.GL_DEPTH_ATTACHMENT, depth_tex.getTarget(), depth_tex.getTexture(), 0);
				gl.glDrawBuffers(GLenum.GL_NONE);
			}
			gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, 0);
			
			Matrix4f.perspective(100, 1.0f, getShadowNearPlane(), getShadowFarPlane(), m_LightProj);
			Matrix4f.lookAt(50, 50, 50, 0, 0, 0, 0, 1, 0, m_LightView);
			Matrix4f.mul(m_LightProj, m_LightView, m_LightMVP);
		}
		
		{
			// create shaders
			phong_prog = new PhongProgram();
			color_prog = new ColorProgram();
			zpass_prog = new ShadowmapGenerateProgram();
			shadow_mapping_prog = new ShadowMappingPassProg();
			zpass_prog_gi = new ZpassInstanceProgram();
			shadow_mapping_prog_gi = new ShadowMappingPassInstanceProg();
			
			shadow_mapping_prog.enable();
			gl.glUniform1i(gl.glGetUniformLocation(shadow_mapping_prog.getProgram(), "shadow_map"), 0);
			gl.glUniform1i(gl.glGetUniformLocation(shadow_mapping_prog.getProgram(), "tex1"), 1);

			gl.glUniform4f(gl.glGetUniformLocation(shadow_mapping_prog.getProgram(), "light_ambient"), 0.4f, 0.4f, 0.4f, 1);
			gl.glUniform4f(gl.glGetUniformLocation(shadow_mapping_prog.getProgram(), "light_diffuse"), 0.5f, 0.5f, 0.4f, 1.0f);
			gl.glUniform4f(gl.glGetUniformLocation(shadow_mapping_prog.getProgram(), "light_specular"), 3.0f, 2.0f, 1.5f, 1.0f);
			gl.glUniform4f(gl.glGetUniformLocation(shadow_mapping_prog.getProgram(), "material_diffuse"), 1.0f, 1.0f, 1.0f, 1.0f);
			gl.glUniform4f(gl.glGetUniformLocation(shadow_mapping_prog.getProgram(), "material_specular"), 0.9f, 0.9f, 0.9f, 60.0f);
			gl.glUniform1f(gl.glGetUniformLocation(shadow_mapping_prog.getProgram(), "light_inv_radius"), 0.002f);
			
			shadow_mapping_prog_gi.enable();
			gl.glUniform1i(gl.glGetUniformLocation(shadow_mapping_prog_gi.getProgram(), "shadow_map"), 0);
			gl.glUniform1i(gl.glGetUniformLocation(shadow_mapping_prog_gi.getProgram(), "tex1"), 1);

			gl.glUniform4f(gl.glGetUniformLocation(shadow_mapping_prog_gi.getProgram(), "light_ambient"), 0.4f, 0.4f, 0.4f, 1);
			gl.glUniform4f(gl.glGetUniformLocation(shadow_mapping_prog_gi.getProgram(), "light_diffuse"), 0.5f, 0.5f, 0.4f, 1.0f);
			gl.glUniform4f(gl.glGetUniformLocation(shadow_mapping_prog_gi.getProgram(), "light_specular"), 3.0f, 2.0f, 1.5f, 1.0f);
			gl.glUniform4f(gl.glGetUniformLocation(shadow_mapping_prog_gi.getProgram(), "material_diffuse"), 1.0f, 1.0f, 1.0f, 1.0f);
			gl.glUniform4f(gl.glGetUniformLocation(shadow_mapping_prog_gi.getProgram(), "material_specular"), 0.9f, 0.9f, 0.9f, 60.0f);
			gl.glUniform1f(gl.glGetUniformLocation(shadow_mapping_prog_gi.getProgram(), "light_inv_radius"), 0.002f);

			gl.glUseProgram(0);
		}
		
		{
			final float radius = 1.2f;
			// create sphere model
			QuadricBuilder builder = new QuadricBuilder();
			builder.setXSteps(50).setYSteps(50);
			builder.setPostionLocation(POSITION.index);
			builder.setNormalLocation(NORMAL.index);
			builder.setTexCoordLocation(TEXCOORD0.index);
			builder.setDrawMode(DrawMode.FILL);
			builder.setCenterToOrigin(true);
			Model sphere = new QuadricMesh(builder, new QuadricSphere(radius)).getModel();
			
			AttribFloatArray instance_positions = new AttribFloatArray(3, NUM_SPHERES);
			AttribFloatArray instance_orientations = new AttribFloatArray(3, NUM_SPHERES);
			float max_range = 40;
			for(int i = 0; i < NUM_SPHERES; i++){
				float x = Numeric.random(-max_range, max_range);
				float z = Numeric.random(-max_range, max_range);
				
				while(!checkPosition(instance_positions, i, radius, x, z)){
					x = Numeric.random(-max_range, max_range);
					z = Numeric.random(-max_range, max_range);
				}
				
				instance_positions.add(x, radius - 0.1f, z);
				instance_orientations.add((float)Math.toRadians(90), 0, 0);
			}
			
			instance_positions.setDivisor(1);
			instance_orientations.setDivisor(1);
			sphere.addAttrib(instance_positions, INSTANCE_POSITION.index);
			sphere.addAttrib(instance_orientations, INSTANCE_ROTATION.index);
			sphere.enableSperateBuffer();
			
			sphereVAO = sphere.genVAO();
			
			// create the plane VAO
			builder.setXSteps(20).setYSteps(20);
			planeVAO = new QuadricMesh(builder, new QuadricPlane(200, 200)).getModel().genVAO();
		}
		
		{
			// load textures
			try {
				ground_tex_diffuse = TextureUtils.createTexture2DFromFile(TEXTURES_PATH + "plastic_bluethingroove_df_.jpg", false);
				sphere_tex_diffuse = TextureUtils.createTexture2DFromFile(TEXTURES_PATH + "redvelvet.jpg", false);
				sphere_tex_diffuse.setWrapS(GLenum.GL_REPEAT);
				sphere_tex_diffuse.setWrapT(GLenum.GL_REPEAT);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		m_Timer.start();
	}

	@Override
	public void onResize(int width, int height) {
		if(width == 0 || height == 0){
			return;
		}
		
		gl.glViewport(0, 0, width, height);
		
		if(scene_color_tex != null && scene_color_tex.getWidth() == width && scene_color_tex.getHeight() == height ){
			return;
		}
		
		if(scene_color_tex != null){
			scene_color_tex.dispose();
			scene_depth_tex.dispose();
		}
		
		{
			// create scene render target and textures.
			Texture2DDesc colorDesc = new Texture2DDesc(width, height, GLenum.GL_RGBA16F);
			scene_color_tex = TextureUtils.createTexture2D(colorDesc, null);
			scene_color_tex.setMagFilter(GLenum.GL_LINEAR);
			scene_color_tex.setMinFilter(GLenum.GL_LINEAR);
			scene_color_tex.setWrapS(GLenum.GL_CLAMP_TO_EDGE);
			scene_color_tex.setWrapT(GLenum.GL_CLAMP_TO_EDGE);
			Texture2DDesc depthDesc = colorDesc;
			depthDesc.format = GLenum.GL_DEPTH_COMPONENT16;
			scene_depth_tex = TextureUtils.createTexture2D(depthDesc, null);
			scene_depth_tex.setMagFilter(GLenum.GL_NEAREST);
			scene_depth_tex.setMinFilter(GLenum.GL_NEAREST);
			scene_depth_tex.setWrapS(GLenum.GL_CLAMP_TO_EDGE);
			scene_depth_tex.setWrapT(GLenum.GL_CLAMP_TO_EDGE);
			
			if(scene_rt == 0){
				scene_rt = gl.glGenFramebuffer();
			}

			gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, scene_rt);
			{
				gl.glFramebufferTexture2D(GLenum.GL_FRAMEBUFFER, GLenum.GL_COLOR_ATTACHMENT0, scene_color_tex.getTarget(), scene_color_tex.getTexture(), 0);
				gl.glFramebufferTexture2D(GLenum.GL_FRAMEBUFFER, GLenum.GL_DEPTH_ATTACHMENT, scene_depth_tex.getTarget(), scene_depth_tex.getTexture(), 0);
			}
			gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, 0);
		}
		
		Matrix4f.perspective(60, (float)width/height, getSceneNearPlane(), getSceneFarPlane(), m_Proj);
	}
	
	public float getSceneNearPlane() { return 1.0f;}
	public float getSceneFarPlane() { return 1000.0f;}
	public Matrix4f getProjMat()    { return m_Proj;}
	public Matrix4f getViewProjMat(){ return m_MVP;} 
	public void getLightPosition(Vector4f out){out.set(m_LightPosition); out.w = 1;}  

	@Override
	public void draw() {
		m_Transformer.getModelViewMat(m_View);
		Matrix4f.mul(m_Proj, m_View, m_MVP);
		Matrix4f.decompseRigidMatrix(m_View, m_CameraPos, null, null, m_ViewDir);
		m_ViewDir.scale(20);
		
//		drawZPass();
		GLCheck.checkError();
		float light_position_x=m_LightPosition.x;
		float light_position_y=m_LightPosition.y;
		float light_position_z=m_LightPosition.z;
		
		Matrix4f.lookAt(light_position_x, light_position_y, light_position_z, 0, 0, 0, 0, 1, 0, m_LightView);
		Matrix4f.mul(m_LightProj, m_LightView, m_LightMVP);
		Matrix4f.mul(m_LightShift, m_LightMVP, m_LightTexture);
		buildShadowMap();GLCheck.checkError();
		drawIlluminationPass(light_position_x, light_position_y, light_position_z, true);
		GLCheck.checkError();
//		drawFrustume();
		gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, 0);
		
		m_bPrintProgram = true;
		if(true){
			return;
		}
		
		light_position_x = -20.0f;
		light_position_y = 60.0f;
		light_position_z = 20.0f;
		Matrix4f.lookAt(light_position_x, light_position_y, light_position_z, 0, 0, 0, 0, 1, 0, m_LightView);
		Matrix4f.mul(m_LightProj, m_LightView, m_LightMVP);
		Matrix4f.mul(m_LightShift, m_LightMVP, m_LightTexture);
		buildShadowMap();
		drawIlluminationPass(light_position_x, light_position_y, light_position_z, false);
		GLCheck.checkError();
		
		light_position_x = 20.0f;
		light_position_y = 60.0f;
		light_position_z = -20.0f;
		Matrix4f.lookAt(light_position_x, light_position_y, light_position_z, 0, 0, 0, 0, 1, 0, m_LightView);
		Matrix4f.mul(m_LightProj, m_LightView, m_LightMVP);
		Matrix4f.mul(m_LightShift, m_LightMVP, m_LightTexture);
		buildShadowMap();
		drawIlluminationPass(light_position_x, light_position_y, light_position_z, false);
		GLCheck.checkError();
	}
	
	private void drawFrustume(){
//		frustumeParams.frustumeMat.load(m_LightMVP);  TODO
//		frustumeParams.viewProjMat.load(m_MVP);
//		frustumeParams.blendEnabled = true;
//
//		if(frustumeRender == null)
//			frustumeRender = new FrustumeRender();
//		frustumeRender.draw(frustumeParams);
	}
	
	// draw the scene to scene_rt target.
	private void drawZPass(){
		gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, scene_rt);
		gl.glViewport(0, 0, scene_color_tex.getWidth(), scene_color_tex.getHeight());
		gl.glClearColor(0.29f, 0.29f, 0.29f, 1.0f);
		gl.glClearDepthf(1.0f);

		gl.glClear(GLenum.GL_COLOR_BUFFER_BIT|GLenum.GL_DEPTH_BUFFER_BIT);
		gl.glEnable(GLenum.GL_DEPTH_TEST);
		gl.glDepthFunc(GLenum.GL_LESS);
		
		zpass_prog.enable();
//		zpass_prog.applyColor(1, 1, 1, 1);
//		zpass_prog.applyMVP(m_MVP);
		zpass_prog.applyMVPMat(m_MVP);
		
		planeVAO.bind();
		planeVAO.draw(DrawMode.FILL.getGLMode());
		planeVAO.unbind();
		
		zpass_prog_gi.enable();
		zpass_prog_gi.applyProj(m_Proj);
		zpass_prog_gi.applyView(m_View);
		sphereVAO.bind();
		sphereVAO.draw(DrawMode.FILL.getGLMode(), NUM_SPHERES);
		sphereVAO.unbind();

		gl.glUseProgram(0);
		gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, 0);
	}
	
	private void buildShadowMap(){
		gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, depth_rt);
		gl.glViewport(0, 0, SHADOW_MAP_SIZE, SHADOW_MAP_SIZE);
		gl.glClearBufferfv(GLenum.GL_DEPTH, 0, CacheBuffer.wrap(1f));
		gl.glEnable(GLenum.GL_DEPTH_TEST);
		gl.glDepthFunc(GLenum.GL_LESS);
		GLCheck.checkError();
		
//		GL11.glPolygonOffset(1.0f, 2.5f);
		zpass_prog.enable();
		zpass_prog.applyMVPMat(m_LightMVP);
		GLCheck.checkError();
		planeVAO.bind();
		planeVAO.draw(DrawMode.FILL.getGLMode());
		planeVAO.unbind();
		GLCheck.checkError();
		if(!m_bPrintProgram){
			printProgram(zpass_prog, "zpass_prog");
		}
		
		zpass_prog_gi.enable();
		zpass_prog_gi.applyProj(m_LightProj);
		zpass_prog_gi.applyView(m_LightView);
		sphereVAO.bind();
		sphereVAO.draw(DrawMode.FILL.getGLMode(), NUM_SPHERES);
		sphereVAO.unbind();
		GLCheck.checkError();
		gl.glUseProgram(0);
		gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, 0);
//		GL11.glPolygonOffset(1.0f, 0.0f);
		GLCheck.checkError();
		
		if(!m_bPrintProgram){
			printProgram(zpass_prog_gi, "zpass_prog_gi");
		}
	}
	
	private void drawIlluminationPass(float light_position_x, float light_position_y, float light_position_z, boolean first_pass){
		gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, scene_rt);
		gl.glViewport(0, 0, scene_color_tex.getWidth(), scene_color_tex.getHeight());

		gl.glEnable(GLenum.GL_DEPTH_TEST);
		gl.glDepthFunc(GLenum.GL_LESS);
		
		if(first_pass){
			// Fill the background with given color
			gl.glClearColor(0.29f, 0.29f, 0.29f, 1.0f);
			gl.glClearDepthf(1.0f);

			gl.glClear(GLenum.GL_COLOR_BUFFER_BIT|GLenum.GL_DEPTH_BUFFER_BIT);
		}
		
		shadow_mapping_prog.enable();
		shadow_mapping_prog.applyMVP(m_MVP);
		shadow_mapping_prog.applyModelView(m_View);
		shadow_mapping_prog.applyView(m_View);
		shadow_mapping_prog.applyTextureMat(m_LightTexture);
		
		shadow_mapping_prog.applyLightPosition( light_position_x, light_position_y, light_position_z, 1.0f);
		shadow_mapping_prog.applyUVTiling(1, 1, 0, 0);
		shadow_mapping_prog.applyDiffuse(0.7f, 0.6f, 0.56f, 1.0f);
		shadow_mapping_prog.applySpecular(0.0f, 0.0f, 0.0f, 1.0f);
		
//		depth_tex.bind(0);
//		ground_tex_diffuse.bind(1);
		gl.glActiveTexture(GLenum.GL_TEXTURE0);
		gl.glBindTexture(depth_tex.getTarget(), depth_tex.getTexture());
		gl.glActiveTexture(GLenum.GL_TEXTURE1);
		gl.glBindTexture(ground_tex_diffuse.getTarget(), ground_tex_diffuse.getTexture());

		planeVAO.bind();
		planeVAO.draw(GLenum.GL_TRIANGLES);
		planeVAO.unbind();
		
		if(!m_bPrintProgram){
			printProgram(shadow_mapping_prog, "shadow_mapping_prog");
		}
		
		shadow_mapping_prog_gi.enable();
		shadow_mapping_prog_gi.applyMVP(m_MVP);
		shadow_mapping_prog_gi.applyModelView(m_View);
		shadow_mapping_prog_gi.applyView(m_View);
		shadow_mapping_prog_gi.applyProj(m_Proj);
		shadow_mapping_prog_gi.applyTextureMat(m_LightTexture);
		
		
		shadow_mapping_prog_gi.applyLightPosition( light_position_x, light_position_y, light_position_z, 1.0f);
		shadow_mapping_prog_gi.applyUVTiling(2, 2, 0, 0);
		shadow_mapping_prog_gi.applyDiffuse(0.7f, 0.6f, 0.56f, 1.0f);
		shadow_mapping_prog_gi.applySpecular(1.0f, 1.0f, 1.0f, 91.0f);
		
//		sphere_tex_diffuse.bind(1);
		gl.glBindTexture(sphere_tex_diffuse.getTarget(), sphere_tex_diffuse.getTexture());
		sphereVAO.bind();
		sphereVAO.draw(GLenum.GL_TRIANGLES, NUM_SPHERES);
		sphereVAO.unbind();

		gl.glUseProgram(0);
		
		if(!m_bPrintProgram){
			printProgram(shadow_mapping_prog_gi, "shadow_mapping_prog_gi");
		}
	}

	@Override
	public void onDestroy() {}
	
	public Texture2D getSceneColor(){ return scene_color_tex;}
	public Texture2D getSceneDepth(){ return scene_depth_tex;}
	public Texture2D getShadowMap() { return depth_tex;}
	public float getShadowNearPlane() { return 1.0f;}
	public float getShadowFarPlane()  { return 200.0f;}
	
	private static boolean checkPosition(AttribFloatArray sphere_positions, int spheres_count, float radius, float x, float z){
		Vector3f p = new Vector3f();
		for(int i = 0; i < spheres_count; i++){
			sphere_positions.get(i, p);
			
			float dx = x - p.x;
			float dz = z - p.z;
			float dist = Vector2f.length(dx, dz);
			if(dist < radius * 2.5f){
				return false;
			}
		}
		
		return true;
	}
	
	public static void printProgram(OpenGLProgram program, String debugName){
//		System.out.println("----------------------------"+debugName +"-----------------------------------------" );
//		ProgramProperties props = GLSLUtil.getProperties(program.getProgram());
//		System.out.println(props);
	}

	private final class PhongProgram extends GLSLProgram{
		private final int mvpIndex;
		private final int mvIndex;
		private final int lightPosIndex;
		private final int uvTilingIndex;
		
		public PhongProgram() {
//			compile(SHADER_PATH + "phong_prog.vert", SHADER_PATH + "phong_prog.frag", POSITION, NORMAL, TEXCOORD0);
			setAttribBinding(POSITION, NORMAL, TEXCOORD0);
			try {
				setSourceFromFiles(SHADER_PATH + "phong_prog.vert", SHADER_PATH + "phong_prog.frag");
			} catch (IOException e) {
				e.printStackTrace();
			}

			mvpIndex = getUniformLocation("gxl3d_ModelViewProjectionMatrix");
			mvIndex = getUniformLocation("gxl3d_ModelViewMatrix");
			lightPosIndex = getUniformLocation("light_position");
			uvTilingIndex = getUniformLocation("uv_tiling");
		}
		
		void applyMVP(Matrix4f mat){
			if(mvpIndex >= 0){
				gl.glUniformMatrix4fv(mvpIndex, false, CacheBuffer.wrap(mat));
			}
		}
		
		void applyView(Matrix4f mat){
			if(mvIndex >= 0){
				gl.glUniformMatrix4fv(mvIndex, false, CacheBuffer.wrap(mat));
			}
		}
		
		void applyLightPosition(float x, float y, float z, float w){
			if(lightPosIndex >= 0){
				gl.glUniform4f(lightPosIndex, x, y, z, w);
			}
		}
		
		void applyUVTiling(float x, float y, float z, float w){
			if(uvTilingIndex >= 0){
				gl.glUniform4f(uvTilingIndex, x, y, z, w);
			}
		}
	}
	
	private final class ColorProgram extends GLSLProgram{
		private final int mvpIndex;
		private final int lightPosIndex;
		
		public ColorProgram() {
//			compile(SHADER_PATH + "color_prog.vert", SHADER_PATH + "color_prog.frag", POSITION, COLOR, TEXCOORD0);
			setAttribBinding(POSITION, COLOR, TEXCOORD0);
			try {
				setSourceFromFiles(SHADER_PATH + "color_prog.vert", SHADER_PATH + "color_prog.frag");
			} catch (IOException e) {
				e.printStackTrace();
			}

			mvpIndex = getUniformLocation("gxl3d_ModelViewProjectionMatrix");
			lightPosIndex = getUniformLocation("light_position");
		}
		
		void applyMVP(Matrix4f mat){
			if(mvpIndex >= 0){
				gl.glUniformMatrix4fv(mvpIndex, false, CacheBuffer.wrap(mat));
			}
		}
		
		void applyColor(float x, float y, float z, float w){
			if(lightPosIndex >= 0){
				gl.glUniform4f(lightPosIndex, x, y, z, w);
			}
		}
	}
	
	private class ShadowMappingPassProg extends GLSLProgram{
		private int mvpIndex;
		private int mvIndex;
		private int worldIndex;
		private int textureIndex;
		private int lightPosIndex;
		private int uvTilingIndex;
		
		private int diffuseIndex;
		private int specularIndex;
		
		public ShadowMappingPassProg() {
			init("shadow_mapping_pass_prog.vert", "shadow_mapping_pass_prog.frag", 
					POSITION, NORMAL, TEXCOORD0, TEXCOORD1, TANGENT, COLOR);
		}
		
		void init(String vertFile, String fragFile, AttribBinder ...binders){
//			compile(SHADER_PATH + vertFile, SHADER_PATH + fragFile,binders);
			setAttribBinding(binders);
			try {
				setSourceFromFiles(SHADER_PATH + vertFile, SHADER_PATH + fragFile);
			} catch (IOException e) {
				e.printStackTrace();
			}

			mvpIndex = getUniformLocation("gxl3d_ModelViewProjectionMatrix");
			mvIndex = getUniformLocation("gxl3d_ModelViewMatrix");
			worldIndex = getUniformLocation("gxl3d_ViewMatrix");
			textureIndex = getUniformLocation("gxl3d_TextureMatrix");
			
			lightPosIndex = getUniformLocation("light_position");
			uvTilingIndex = getUniformLocation("uv_tiling");
			diffuseIndex = getUniformLocation("material_diffuse");
			specularIndex = getUniformLocation("material_specular");
		}
		
		void applyMVP(Matrix4f mat){
			if(mvpIndex >= 0){
				gl.glUniformMatrix4fv(mvpIndex, false, CacheBuffer.wrap(mat));
			}
		}
		
		void applyModelView(Matrix4f mat){
			if(mvIndex >= 0){
				gl.glUniformMatrix4fv(mvIndex, false, CacheBuffer.wrap(mat));
			}
		}
		
		void applyView(Matrix4f mat){
			if(worldIndex >= 0){
				gl.glUniformMatrix4fv(worldIndex, false, CacheBuffer.wrap(mat));
			}
		}
		
		void applyTextureMat(Matrix4f mat){
			if(textureIndex >= 0){
				gl.glUniformMatrix4fv(textureIndex, false, CacheBuffer.wrap(mat));
			}
		}
		
		void applyLightPosition(float x, float y, float z, float w){
			if(lightPosIndex >= 0){
				gl.glUniform4f(lightPosIndex, x, y, z, w);
			}
		}
		
		void applyUVTiling(float x, float y, float z, float w){
			if(uvTilingIndex >= 0){
				gl.glUniform4f(uvTilingIndex, x, y, z, w);
			}
		}
		
		void applyDiffuse(float x, float y, float z, float w){
			if(diffuseIndex >= 0){
				gl.glUniform4f(diffuseIndex, x, y, z, w);
			}
		}
		
		void applySpecular(float x, float y, float z, float w){
			if(specularIndex >= 0){
				gl.glUniform4f(specularIndex, x, y, z, w);
			}
		}
	}
	
	private final class ZpassInstanceProgram extends GLSLProgram{
		private final int projIndex;
		private final int viewIndex;
		
		public ZpassInstanceProgram() {
//			compile(SHADER_PATH + "zpass_prog_gi.vert", SHADER_PATH + "color_prog.frag", POSITION,INSTANCE_POSITION, INSTANCE_ROTATION);
			setAttribBinding(POSITION,INSTANCE_POSITION, INSTANCE_ROTATION);
			try {
				setSourceFromFiles(SHADER_PATH + "zpass_prog_gi.vert", SHADER_PATH + "color_prog.frag");
			} catch (IOException e) {
				e.printStackTrace();
			}

			projIndex = getUniformLocation("gxl3d_ProjectionMatrix");
			viewIndex = getUniformLocation("gxl3d_ViewMatrix");
		}
		
		void applyProj(Matrix4f mat){
			if(projIndex >= 0){
				gl.glUniformMatrix4fv(projIndex, false, CacheBuffer.wrap(mat));
			}
		}
		
		void applyView(Matrix4f mat){
			if(viewIndex >= 0){
				gl.glUniformMatrix4fv(viewIndex, false, CacheBuffer.wrap(mat));
			}
		}
	}
	
	private final class ShadowMappingPassInstanceProg extends ShadowMappingPassProg{
		private final int projIndex;
		private final int viewIndex;
		
		public ShadowMappingPassInstanceProg() {
			init("shadow_mapping_prog_gi.vert", "shadow_mapping_prog_gi.frag", 
					POSITION, NORMAL, TEXCOORD0, TEXCOORD1, TANGENT, COLOR,
					INSTANCE_POSITION, INSTANCE_ROTATION
					);
			
			projIndex = getUniformLocation("gxl3d_ProjectionMatrix");
			viewIndex = getUniformLocation("gxl3d_ViewMatrix");
		}
		
		void applyProj(Matrix4f mat){
			if(projIndex >= 0){
				gl.glUniformMatrix4fv(projIndex, false, CacheBuffer.wrap(mat));
			}
		}
		
		void applyView(Matrix4f mat){
			if(viewIndex >= 0){
				gl.glUniformMatrix4fv(viewIndex, false, CacheBuffer.wrap(mat));
			}
		}
	}
}
