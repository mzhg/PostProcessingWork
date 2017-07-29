package jet.opengl.demos.nvidia.waves.samples;

import com.nvidia.developer.opengl.app.NvCameraMotionType;
import com.nvidia.developer.opengl.app.NvSampleApp;
import com.nvidia.developer.opengl.utils.Pool;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

import java.io.IOException;

import jet.opengl.postprocessing.common.GLCheck;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.util.Numeric;

public class OceanCSDemo extends NvSampleApp{
	
	// Ocean simulation variables
	OceanSimulator g_pOceanSimulator = null;
	QuadRenderer g_pOceanRender = null;
	boolean g_RenderWireframe = false;
	boolean g_PauseSimulation = false;
	int g_BufferType = 0;
	
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
		getGLContext().setAppTitle("OceanCS");
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

		g_pOceanSimulator = new OceanSimulator("nvidia/WaveWorks/shaders/", ocean_param);
		// Update the simulation for the first time.
		g_pOceanSimulator.updateDisplacementMap(0);

		// Init D3D11 resources for rendering
		g_pOceanRender = new QuadRenderer("nvidia/WaveWorks/shaders/", "nvidia/WaveWorks/textures/", ocean_param, g_MatPool);
//		initRenderResource(ocean_param);
		GLCheck.checkError();
	}
	
	@Override
	public void reshape(int width, int height) {
		gl.glViewport(0, 0, width, height);
		
		Matrix4f.perspective(45, (float)width/height, 100.0f, 200000.0f, g_Projection);
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
		
		Matrix4f.mul(modelView, g_FlipMat, modelView);
		Matrix4f mvp = Matrix4f.mul(g_Projection, modelView, g_MatPool.obtain());
		
		{
			g_SkyBoxRender.setProjectionMatrix(g_Projection);
			g_SkyBoxRender.setRotateMatrix(modelView);
			g_SkyBoxRender.draw();
		}
		
		g_pOceanSimulator.updateDisplacementMap(g_AppTime);
		
		// Ocean rendering
		int tex_displacement = g_pOceanSimulator.getD3D11DisplacementMap();
		int tex_gradient = g_pOceanSimulator.getD3D11GradientMap();
		
		m_transformer.getModelViewMat(modelView);
		Matrix4f.mul(modelView, g_FlipMat, modelView);
		g_pOceanRender.setMatrices(g_Projection, modelView, g_EyePostion, getGLContext().width() * getGLContext().height());
		if(g_RenderWireframe)
			g_pOceanRender.renderWireframe(tex_displacement, g_AppTime);
		else
			g_pOceanRender.renderShaded(tex_displacement, tex_gradient, g_AppTime);
		
		g_MatPool.free(modelView);
		g_MatPool.free(mvp);
	}
	
	@Override
	public void onDestroy() {
		OceanSamplers.destroySamplers();
	}
}
