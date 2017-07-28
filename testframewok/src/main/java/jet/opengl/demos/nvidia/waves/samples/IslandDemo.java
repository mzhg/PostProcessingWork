package jet.opengl.demos.nvidia.waves.samples;

import com.nvidia.developer.opengl.app.NvCameraMotionType;
import com.nvidia.developer.opengl.app.NvSampleApp;
import com.nvidia.developer.opengl.ui.NvTweakBar;
import com.nvidia.developer.opengl.utils.FieldControl;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

import jet.opengl.postprocessing.common.GLCheck;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;

public class IslandDemo extends NvSampleApp{
	Vector3f g_EyePoints[/*6*/]=  {new Vector3f(365.0f,  3.0f, 166.0f),
			new Vector3f(478.0f, -15.0f, 248.0f),
			new Vector3f(430.0f,  3.0f, 249.0f),
			new Vector3f(513.0f, 10.0f, 277.0f),
			new Vector3f(303.0f,  3.0f, 459.0f),
			new Vector3f(20.0f,  12.0f, 477.0f),
	};

	Vector3f g_LookAtPoints[/*6*/]={new Vector3f(330.0f,-11.0f, 259.0f),
			new Vector3f(388.0f,-16.0f, 278.0f),
			new Vector3f(357.0f,-59.0f, 278.0f),
			new Vector3f(438.0f,-12.0f, 289.0f),
			new Vector3f(209.0f,-20.0f, 432.0f),
			new Vector3f(90.0f,  -7.0f, 408.0f),
	};

	final Matrix4f g_CameraProjection = new Matrix4f();
	final Matrix4f g_DepthModifier = new Matrix4f();
	
	IsParameters g_Parameters = new IsParameters();
	CTerrain g_Terrain;
	private GLFuncProvider gl;
	private float m_TotalTime;

	@Override
	public void initUI() {
		NvTweakBar tweakBar=mTweakBar;
		tweakBar.addValue("Show Reflection", new FieldControl(g_Parameters, "g_showReflection", FieldControl.CALL_FIELD), false, 0);
		tweakBar.addValue("Render Water", new FieldControl(g_Parameters, "g_RenderWater", FieldControl.CALL_FIELD), false, 0);
		tweakBar.syncValues();
	}

	@Override
	protected void initRendering() {
		getGLContext().setSwapInterval(0);
		gl= GLFuncProviderFactory.getGLFuncProvider();
		m_transformer.setMotionMode(NvCameraMotionType.FIRST_PERSON);
		m_transformer.setTranslation(-g_EyePoints[1].x, g_EyePoints[1].y, -g_EyePoints[1].z);
		m_transformer.setMaxTranslationVel(20);

		IsSamplers.createSamplers();
		GLCheck.checkError();
		g_Terrain = new CTerrain();
		g_Terrain.onCreate("nvidia/WaveWorks/shaders/", "nvidia/WaveWorks/textures/");
		GLCheck.checkError();

		initConstantData();
	}

	/** Initialize the constant data. */
	void initConstantData(){
		g_Parameters.g_ZNear = CTerrain.scene_z_near;
		g_Parameters.g_ZFar = CTerrain.scene_z_far;
		
		Matrix4f light_lookAt = new Matrix4f();
		Matrix4f light_proj = new Matrix4f();
		
		Vector3f lookAtPoint = new Vector3f(CTerrain.terrain_far_range/2.0f,0.0f,CTerrain.terrain_far_range/2.0f);
		Matrix4f.lookAt(g_Parameters.g_LightPosition, lookAtPoint, Vector3f.Y_AXIS, light_lookAt);
//		Matrix4f.perspective(60, 1.5f, 0.1f, 10000.0f, light_proj);
		float length = g_Parameters.g_LightPosition.length();
		float nr, fr;
//		nr=sqrt(eyePoint.x*eyePoint.x+eyePoint.y*EyePoint.y+EyePoint.z*EyePoint.z)-terrain_far_range*0.7f;
//		fr=sqrt(eyePoint.x*eyePoint.x+eyePoint.y*EyePoint.y+EyePoint.z*EyePoint.z)+terrain_far_range*0.7f;
		nr = length - CTerrain.terrain_far_range*0.7f;
		fr = length + CTerrain.terrain_far_range*0.7f;
		float w = CTerrain.terrain_far_range*1.5f;
		float h = CTerrain.terrain_far_range;
		Matrix4f.ortho(-w/2, w/2, -h/2, h/2, nr,fr, light_proj);
		
		Matrix4f.mul(light_proj, light_lookAt, g_Parameters.g_LightModelViewProjectionMatrix);
//		g_DepthModifier.m22 = 2.0f;  // z-scaler
//		g_DepthModifier.m32 = -1.0f;  // z-offset.
		
		Matrix4f.mul(g_DepthModifier, g_Parameters.g_LightModelViewProjectionMatrix, g_Parameters.g_LightModelViewProjectionMatrix);
		
		Matrix4f shadowMap = g_Parameters.g_DepthModelViewProjectionMatrix;
		shadowMap.m00 = 0.5f;
		shadowMap.m11 = 0.5f;
		shadowMap.m22 = 0.5f;
		shadowMap.m30 = 0.5f;
		shadowMap.m31 = 0.5f;
		shadowMap.m32 = 0.5f;
		Matrix4f.mul(shadowMap, g_Parameters.g_LightModelViewProjectionMatrix, g_Parameters.g_DepthModelViewProjectionMatrix);
	}
	
	void setupCameraViews(){
		Matrix4f modelView = g_Parameters.g_ModelViewMatrix;
		m_transformer.getModelViewMat(modelView);
//		modelView.load(camera.getView());
		Vector3f cameraPosition = g_Parameters.g_CameraPosition;
		Vector3f cameraDirection = g_Parameters.g_CameraDirection;
//		cameraPosition.set(g_EyePoints[0]);
//		cameraDirection.set(g_LookAtPoints[0]);
//		Matrix4f.lookAt(cameraPosition, cameraDirection, Vector3f.Y_AXIS, modelView);
		
		Matrix4f.decompseMatrix(modelView, cameraPosition, cameraDirection, null);
		Vector3f.sub(cameraDirection, cameraPosition, cameraDirection);
		cameraDirection.normalise();
		
		Matrix4f.mul(g_CameraProjection, modelView, g_Parameters.g_ModelViewProjectionMatrix);
		Matrix4f.mul(g_DepthModifier, g_Parameters.g_ModelViewProjectionMatrix, g_Parameters.g_ModelViewProjectionMatrix);
		g_Parameters.g_Projection = g_CameraProjection;
	}

	@Override
	public void display() {
		m_TotalTime += getFrameDeltaTime();
		g_Parameters.g_WaterBumpTexcoordShift.set(m_TotalTime*1.5f,m_TotalTime*0.75f);
		g_Parameters.g_ScreenSizeInv.x = 1.0f/(getGLContext().width() * CTerrain.main_buffer_size_multiplier);
		g_Parameters.g_ScreenSizeInv.y = 1.0f/(getGLContext().height() * CTerrain.main_buffer_size_multiplier);
		setupCameraViews();
//		GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_LINE);
//		g_Parameters.g_Wireframe = true;
		g_Terrain.onDraw(g_Parameters);
	}
	
	@Override
	public void reshape(int width, int height) {
		gl.glViewport(0, 0, width, height);
		g_Terrain.onReshape(width, height);
		
		Matrix4f.perspective(CTerrain.camera_fov/5, (float)width/height, CTerrain.scene_z_near, CTerrain.scene_z_far, g_CameraProjection);
	}
}
