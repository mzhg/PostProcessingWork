package jet.opengl.demos.intel.assao;

import com.nvidia.developer.opengl.app.NvSampleApp;

import org.lwjgl.util.vector.Matrix4f;

import jet.opengl.demos.scenes.CubeScene;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.FullscreenProgram;


public class ASSAODemoDebug extends NvSampleApp{
	
	private FullscreenProgram m_screenProgram;
	private final Matrix4f m_ViewProj = new Matrix4f();
	private ASSAO_Effect m_Effect;
	private final ASSAO_InputsOpenGL m_Inputs = new ASSAO_InputsOpenGL();
	private final ASSAO_Settings     m_Settings = new ASSAO_Settings();
	private GLFuncProvider gl;
	private CubeScene m_Scene;

	@Override
	protected void initRendering() {
		ASSAOGL.ASSAO_DEBUG = true;
		getGLContext().setSwapInterval(0);
		gl = GLFuncProviderFactory.getGLFuncProvider();
		m_Scene = new CubeScene(m_transformer);
		m_Scene.onCreate();
		
		m_screenProgram = new FullscreenProgram();
		m_Effect = new ASSAOGL();
		((ASSAOGL)m_Effect).InitializeGL();
	}
	
	@Override
	public void display() {
		m_Scene.draw();

		/*m_Scene.getViewProjMatrix(m_ViewProj);
		m_screenProgram.enable();
		gl.glDisable(GLenum.GL_DEPTH_TEST);
		gl.glBindVertexArray(0);
//		m_SceneColor.bind(0);
		gl.glBindTexture(m_SceneColor.getTarget(), m_SceneColor.getTexture());
		gl.glDrawArrays(GLenum.GL_TRIANGLE_STRIP, 0, 4);
		gl.glBindVertexArray(0);*/
		
		m_Inputs.DepthSRV = m_Scene.getSceneDepth();
		m_Inputs.DrawOpaque = false;
		m_Inputs.MatricesRowMajorOrder = false;
		m_Inputs.NormalSRV = null;
//		m_Scene.getProjMatrix(m_Inputs.ProjectionMatrix);
		m_Inputs.ScissorLeft = 0;
		m_Inputs.ScissorTop = 0;

//		m_frameAttribs.sceneColorTexture = m_Scene.getSceneColor();
//		m_frameAttribs.sceneDepthTexture = m_Scene.getSceneDepth();
//		m_frameAttribs.cameraNear = m_Scene.getSceneNearPlane();
//		m_frameAttribs.cameraFar =  m_Scene.getSceneFarPlane();
//		m_frameAttribs.outputTexture = null;
//		m_frameAttribs.viewport.set(0,0, getGLContext().width(), getGLContext().height());
//		m_frameAttribs.viewMat = m_Scene.getViewMat();
//		m_frameAttribs.projMat = m_Scene.getProjMat();
//		m_frameAttribs.fov =     m_Scene.getFovInRadian();

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
