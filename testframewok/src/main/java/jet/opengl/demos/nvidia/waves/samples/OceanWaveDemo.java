package jet.opengl.demos.nvidia.waves.samples;

import com.nvidia.developer.opengl.app.NvCameraMotionType;
import com.nvidia.developer.opengl.app.NvSampleApp;
import com.nvidia.developer.opengl.utils.NvImage;
import com.nvidia.developer.opengl.utils.Pool;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;

import java.io.IOException;
import java.nio.IntBuffer;

import jet.opengl.demos.nvidia.water.WaterWaveSimulator;
import jet.opengl.demos.nvidia.waves.GFSDK_WaveWorks_Quadtree;
import jet.opengl.demos.nvidia.waves.GFSDK_WaveWorks_Quadtree_Params;
import jet.opengl.postprocessing.common.GLCheck;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.util.CacheBuffer;
import jet.opengl.postprocessing.util.Numeric;

public final class OceanWaveDemo extends NvSampleApp{
	static final int FRESNEL_TEX_SIZE = 256;
	
	// Ocean simulation variables
	WaterWaveSimulator g_pOceanSimulator = null;
	GFSDK_WaveWorks_Quadtree g_pOceanRender = null;
	boolean g_RenderWireframe = false;
	boolean g_PauseSimulation = false;
	private int g_pSRV_Perlin;
	private int g_pSRV_ReflectCube;
	private int g_pSRV_Fresnel;
	private OceanShadingPorgram g_pOceanSurfaceFX;
	private final GFSDK_WaveWorks_Quadtree_Params g_ocean_quadtree_param = new GFSDK_WaveWorks_Quadtree_Params();
	private final Vector2f g_WindDir  = new Vector2f(0.8f,0.6f);
	// Skybox
//	SkyBoxProgram g_SkyBoxProgram = null;
//	GLVAO g_SkyVAO;
//	int g_pSkyCubeMap = 0;
	
	SkyBoxRender g_SkyBoxRender = null;
	float g_AppTime = 0;
	
	final Matrix4f g_Projection = new Matrix4f();
	final Matrix4f g_FlipMat = new Matrix4f();
	final Vector3f g_EyePostion = new Vector3f();
	final Pool<Matrix4f> g_MatPool = new Pool<>(()->new Matrix4f());

	private GLFuncProvider gl;

	@Override
	protected void initRendering() {
		gl = GLFuncProviderFactory.getGLFuncProvider();
		// Setup the camera's view parameters
//		D3DXVECTOR3 vecEye(1562.24f, 854.291f, -1224.99f);
//		D3DXVECTOR3 vecAt (1562.91f, 854.113f, -1225.71f);
//		g_Camera.SetViewParams(&vecEye, &vecAt);
		
		getGLContext().setSwapInterval(0);
		getGLContext().setAppTitle("OceanWave");
		m_transformer.setMotionMode(NvCameraMotionType.FIRST_PERSON);
		m_transformer.setTranslation(-1562.24f, -801.291f, 1224.99f);
		m_transformer.setRotationVec(new Vector3f(0.1f, Numeric.PI * 0.7f, 0));
		m_transformer.setMaxTranslationVel(400);
		OceanSamplers.createSamplers();
		GLCheck.checkError();
//		try {
//			g_pSkyCubeMap = NvImage.uploadTextureFromDDSFile(getTextureResourcePath() + "sky_cube.dds");
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
		
		// TODO Don't forget to setup the cube map properties.
//		g_SkyBoxProgram = new SkyBoxProgram(0);
//		Model sky_model = ModelGenerator.genCube(10000, false, false, false);
//		sky_model.bindAttribIndex(0, 0);
//		g_SkyVAO = sky_model.genVAO();
		
		g_SkyBoxRender = new SkyBoxRender(500);
		try {
			g_SkyBoxRender.loadCubemapFromDDSFile("nvidia/WaveWorks/textures/sky_cube.dds");
			g_SkyBoxRender.setCubemapSampler(OceanSamplers.g_SamplerLinearMipmapClamp);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		g_FlipMat.m11 = g_FlipMat.m22 = 0;
		g_FlipMat.m12 = -1;
		g_FlipMat.m21 = 1;
		// Create an OceanSimulator object and D3D11 rendering resources
		createOceanSimAndRender();

		GLCheck.checkError();
	}
	
	
	private void createOceanSimAndRender(){
		// Create ocean simulating object
		// Ocean object
		OceanParameter ocean_param = new OceanParameter();

		// The size of displacement map. In this sample, it's fixed to 512.
		ocean_param.dmap_dim			= 512;
		// The side length (world space) of square patch
		ocean_param.patch_length		= 2000.0f;
		// Adjust this parameter to control the simulation speed
		ocean_param.time_scale			= 0.8f;
		// A scale to control the amplitude. Not the world space height
		ocean_param.wave_amplitude		= 0.35f;
		// 2D wind direction. No need to be normalized
		ocean_param.wind_dir			.set(0.8f, 0.6f);
		// The bigger the wind speed, the larger scale of wave crest.
		// But the wave scale can be no larger than patch_length
		ocean_param.wind_speed			= 600.0f;
		// Damp out the components opposite to wind direction.
		// The smaller the value, the higher wind dependency
		ocean_param.wind_dependency		= 0.07f;
		// Control the scale of horizontal movement. Higher value creates
		// pointy crests.
		ocean_param.choppy_scale		= 1.3f;

		g_ocean_quadtree_param.min_patch_length		= 2000;
		g_ocean_quadtree_param.upper_grid_coverage	= 64.0f;
		g_ocean_quadtree_param.mesh_dim				= 512;
		g_ocean_quadtree_param.sea_level			= 0.f;
		g_ocean_quadtree_param.auto_root_lod		= 8;
		g_ocean_quadtree_param.use_tessellation		= false;
		g_ocean_quadtree_param.tessellation_lod		= 50.0f;
		g_ocean_quadtree_param.geomorphing_degree	= 1.f;
		g_ocean_quadtree_param.enable_CPU_timers	= true;

		g_pOceanSimulator = WaterWaveSimulator.createOceanSimulator(ocean_param);
		// Update the simulation for the first time.
		g_pOceanSimulator.updateSimulation(0);

		// Init D3D11 resources for rendering
		g_pOceanRender = new GFSDK_WaveWorks_Quadtree();
		g_pOceanRender.init(g_ocean_quadtree_param);
//		initRenderResource(ocean_param);
		GLCheck.checkError();

		loadTextures("nvidia/WaveWorks/textures/");

		g_pOceanSurfaceFX = new OceanShadingPorgram("nvidia/WaveWorks/shaders/");

		// Constants
		g_pOceanSurfaceFX.enable();

		// Centre the top-level node on the nearest largest-patch boundary
		final float patch_length = g_ocean_quadtree_param.min_patch_length;
//		final float root_patch_length = patch_length * (float)(0x00000001 << g_ocean_quadtree_param.auto_root_lod);

		// Grid side length * 2
		g_pOceanSurfaceFX.setTexelLength_x2(patch_length / g_ocean_quadtree_param.mesh_dim * 2);
		// Color
		g_pOceanSurfaceFX.setSkyColor(new Vector3f(0.38f, 0.45f, 0.56f));
		g_pOceanSurfaceFX.setWaterbodyColor(new Vector3f(0.07f, 0.15f, 0.2f));
		// Texcoord
		g_pOceanSurfaceFX.setUVScale(1.0f/patch_length);
		g_pOceanSurfaceFX.setUVOffset(0.5f/g_ocean_quadtree_param.mesh_dim);
		// Perlin
		g_pOceanSurfaceFX.setPerlinSize(1.f);
		g_pOceanSurfaceFX.setPerlinAmplitude(new Vector3f(35, 42, 57));
		g_pOceanSurfaceFX.setPerlinGradient(new Vector3f(1.4f, 1.6f, 2.2f));
		g_pOceanSurfaceFX.setPerlinOctave(new Vector3f(1.12f, 0.59f, 0.23f));
		// Multiple reflection workaround
		g_pOceanSurfaceFX.setBendParam(new Vector3f(0.1f, -0.4f, 0.2f));
		// Sun streaks
		g_pOceanSurfaceFX.setSunColor(new Vector3f(1.0f, 1.0f, 0.6f));
		g_pOceanSurfaceFX.setSunDir(new Vector3f(0.936016f, -0.343206f, 0.0780013f));
		g_pOceanSurfaceFX.setShineness(400.0f);

		gl.glUseProgram(0);

	}

	private void createFresnelMap(){
		final int g_SkyBlending = 16;
		IntBuffer buffer = CacheBuffer.getCachedIntBuffer(FRESNEL_TEX_SIZE);
		for(int i = 0; i < FRESNEL_TEX_SIZE; i++){
			float cos_a = (float)i / FRESNEL_TEX_SIZE;
			// Using water's refraction index 1.33
			int fresnel = (int)(Numeric.fresnelTerm(cos_a, 1.33f) * 255);

			int sky_blend = (int)(Math.pow(1.0 / (1 + cos_a), g_SkyBlending) * 255);

			buffer.put((sky_blend << 8) | fresnel);
		}
		buffer.flip();

		g_pSRV_Fresnel = gl.glGenTexture();
		gl.glBindTexture(GLenum.GL_TEXTURE_1D, g_pSRV_Fresnel);
		gl.glTexImage1D(GLenum.GL_TEXTURE_1D, 0, GLenum.GL_RGBA8, FRESNEL_TEX_SIZE, 0, GLenum.GL_RGBA, GLenum.GL_UNSIGNED_BYTE, buffer);
//		GL11.glTexParameteri(GL11.GL_TEXTURE_1D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
//		GL11.glTexParameteri(GL11.GL_TEXTURE_1D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
//		GL11.glTexParameteri(GL11.GL_TEXTURE_1D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
		gl.glBindTexture(GLenum.GL_TEXTURE_1D, 0);
	}

	private void loadTextures(String prefix){
		try {
			g_pSRV_Perlin = NvImage.uploadTextureFromDDSFile(prefix + "perlin_noise.dds");
			g_pSRV_ReflectCube = NvImage.uploadTextureFromDDSFile(prefix + "reflect_cube.dds");
		} catch (IOException e) {
			e.printStackTrace();
		}

		gl.glBindTexture(GLenum.GL_TEXTURE_2D, 0);
		gl.glBindTexture(GLenum.GL_TEXTURE_CUBE_MAP, 0);

		createFresnelMap();
	}
	
	@Override
	public void reshape(int width, int height) {
		gl.glViewport(0, 0, width, height);
		
		Matrix4f.perspective(45, (float)width/height, 10.0f, 2000000.0f, g_Projection);
	}
	
	@Override
	public void display() {
		// Update simulation
		if(!g_PauseSimulation){
			g_AppTime += getFrameDeltaTime();
		}
		
		gl.glClearColor( 0.1f,0.2f,0.4f,0.0f);
		gl.glClear(GLenum.GL_COLOR_BUFFER_BIT | GLenum.GL_DEPTH_BUFFER_BIT);
		gl.glDisable(GLenum.GL_DEPTH_TEST);
		// Sky box rendering
		Matrix4f modelView = g_MatPool.obtain();
		modelView.load(m_transformer.getRotationMat());
		g_EyePostion.set(m_transformer.getTranslationVec());
		g_EyePostion.negate();

		g_FlipMat.m12 =-1;
		Matrix4f.mul(modelView, g_FlipMat, modelView);
		Matrix4f mvp = Matrix4f.mul(g_Projection, modelView, g_MatPool.obtain());
		
		{
			g_SkyBoxRender.setProjectionMatrix(g_Projection);
			g_SkyBoxRender.setRotateMatrix(modelView);
			g_SkyBoxRender.draw();
		}
		
		g_pOceanSimulator.updateSimulation(g_AppTime);
		
		// Ocean rendering
		int tex_displacement = g_pOceanSimulator.getDisplacementMap().getTexture();
		int tex_gradient = g_pOceanSimulator.getGradMap().getTexture();

		renderShaded(tex_displacement, tex_gradient, g_AppTime);

		GLCheck.checkError();
		g_MatPool.free(modelView);
		g_MatPool.free(mvp);
	}

	void renderShaded(int displacemnet_map, int gradient_map, float time){
		final Matrix4f modelView = g_MatPool.obtain();
		final Matrix4f matLocal = g_MatPool.obtain();

		final Vector3f eyePos = new Vector3f();

		m_transformer.getModelViewMat(modelView);
//		g_FlipMat.m12 =1;
		Matrix4f.mul(modelView, g_FlipMat, modelView);

		// VS & PS
		g_pOceanSurfaceFX.enable();
		g_pOceanSurfaceFX.enableOceanSurfPS();

		// Textures
		g_pOceanSurfaceFX.setTexDisplacement(displacemnet_map, OceanSamplers.g_pHeightSampler);
		g_pOceanSurfaceFX.setTexPerlin(g_pSRV_Perlin, OceanSamplers.g_pPerlinSampler);
		g_pOceanSurfaceFX.setTexGradient(gradient_map, OceanSamplers.g_pGradientSampler);
		g_pOceanSurfaceFX.setTexFresnel(g_pSRV_Fresnel, OceanSamplers.g_pFresnelSampler);
		g_pOceanSurfaceFX.setSamplerCube(g_pSRV_ReflectCube, OceanSamplers.g_pCubeSampler);

		final float g_PerlinSpeed = 0.06f;
		float perlin_move_x = -g_WindDir.x * time * g_PerlinSpeed;
		float perlin_move_y = -g_WindDir.y * time * g_PerlinSpeed;
		g_pOceanSurfaceFX.setPerlinMovement(perlin_move_x, perlin_move_y);

		// Uniforms we had set up already
		gl.glEnable(GLenum.GL_DEPTH_TEST);

		g_pOceanRender.renderShaded(modelView, g_Projection, (GFSDK_WaveWorks_Quadtree_Params params, Matrix4f modelMat)->
		{
			final float g_PerlinSize = 1.f;
			// Texcoord for perlin noise
			float uv_base_x = modelMat.m30/params.min_patch_length * g_PerlinSize;
			float uv_base_y = modelMat.m31/params.min_patch_length * g_PerlinSize;
			g_pOceanSurfaceFX.setUVBase(uv_base_x, uv_base_y);

			matLocal.setIdentity();
			matLocal.m00 = modelMat.m00;  // Only the scale part
			matLocal.m11 = modelMat.m11;
			matLocal.m22 = 0;
			g_pOceanSurfaceFX.setMatLocal(matLocal);
			// Construct the matMVP
			// matMVP = Projection * ModelView * MatWorld
			matLocal.setIdentity();
			matLocal.m30 = modelMat.m30;  // only the translation part
			matLocal.m31 = modelMat.m31;
			matLocal.m32 = modelMat.m32;
			Matrix4f.mul(modelView, matLocal, modelMat);
			Matrix4f.decompseRigidMatrix(modelMat, eyePos, null, null);

			Matrix4f.mul(g_Projection, modelMat, modelMat);
			g_pOceanSurfaceFX.setMatWorldViewProj(modelMat);

			// Constant g_PerlinSpeed need to be adjusted mannually
//				D3DXVECTOR2 perlin_move = -g_WindDir * time * g_PerlinSpeed;

			// Eye point
			g_pOceanSurfaceFX.setLocalEye(eyePos);
		});

		g_pOceanSurfaceFX.disable();
		gl.glBindVertexArray(0);
		gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, 0);
		gl.glBindBuffer(GLenum.GL_ELEMENT_ARRAY_BUFFER, 0);
		g_MatPool.free(modelView);
		g_MatPool.free(matLocal);
	}
	
	@Override
	public void onDestroy() {
		OceanSamplers.destroySamplers();
		g_pOceanSimulator.dispose();
	}
}
