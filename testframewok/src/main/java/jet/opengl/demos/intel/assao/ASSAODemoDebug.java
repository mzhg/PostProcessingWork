package jet.opengl.demos.intel.assao;

import com.nvidia.developer.opengl.app.NvSampleApp;
import com.nvidia.developer.opengl.ui.NvTweakEnumi;

import jet.opengl.demos.scenes.CubeScene;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;


public class ASSAODemoDebug extends NvSampleApp{
	private ASSAO_Effect m_Effect;
	private final ASSAO_InputsOpenGL m_Inputs = new ASSAO_InputsOpenGL();
	private final ASSAO_Settings     m_Settings = new ASSAO_Settings();
	private GLFuncProvider gl;
	private CubeScene m_Scene;

	@Override
	public void initUI() {
		// Effect quality; -1 - lowest (low, half res checkerboard), 0 - low, 1 - medium, 2 - high, 3 - very high / adaptive; each quality level is roughly 2x more costly than the previous, except the q3 which is variable but, in general, above q2.
		// ASSAO quality
		NvTweakEnumi assaoQualities[] =
		{
			new NvTweakEnumi( "Lowest", -1 ),
			new NvTweakEnumi( "Low", 0 ),
			new NvTweakEnumi( "Medium", 1 ),
			new NvTweakEnumi( "High", 2 ),
			new NvTweakEnumi( "Adaptive", 3 ),
		};

		mTweakBar.addEnum("ASSAO Quality", createControl("QualityLevel", m_Settings), assaoQualities, 1);
	}

	@Override
	protected void initRendering() {
		ASSAOGL.ASSAO_DEBUG = false;
		getGLContext().setSwapInterval(0);
		gl = GLFuncProviderFactory.getGLFuncProvider();
		m_Scene = new CubeScene(m_transformer);
		m_Scene.onCreate();
		
		m_Effect = new ASSAOGL();
		((ASSAOGL)m_Effect).InitializeGL();

//		m_Settings.QualityLevel = 3;
	}
	
	@Override
	public void display() {
		m_Scene.draw();
		m_Scene.resoveMultisampleTexture();


		m_Inputs.DepthSRV = m_Scene.getSceneDepth();
		m_Inputs.DrawOpaque = false;
		m_Inputs.MatricesRowMajorOrder = false;
		m_Inputs.NormalSRV = null;
//		m_Scene.getProjMatrix(m_Inputs.ProjectionMatrix);
		m_Inputs.ScissorLeft = 0;
		m_Inputs.ScissorTop = 0;


		m_Inputs.ScissorRight = m_Scene.getSceneDepth().getWidth();
		m_Inputs.ScissorBottom = m_Scene.getSceneDepth().getHeight();
		m_Inputs.ViewportHeight = m_Scene.getSceneDepth().getHeight();
		m_Inputs.ViewportWidth = m_Scene.getSceneDepth().getWidth();
		m_Inputs.CameraFar =m_Scene.getSceneFarPlane();
		m_Inputs.CameraNear =m_Scene.getSceneNearPlane();
		
		m_Effect.Draw(m_Settings, m_Inputs);
		gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, 0);
		gl.glDisable(GLenum.GL_BLEND);
		gl.glViewport(0, 0, getGLContext().width(), getGLContext().height());
	}

	@Override
	protected void reshape(int width, int height) {
		if(width ==0 || height == 0)
			return;
		
		gl.glViewport(0, 0, width, height);
		m_Scene.onResize(width, height);

	}
}
