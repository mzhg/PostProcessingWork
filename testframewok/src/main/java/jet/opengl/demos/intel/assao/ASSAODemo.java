package jet.opengl.demos.intel.assao;

import com.nvidia.developer.opengl.app.NvSampleApp;

import jet.opengl.postprocessing.shader.FullscreenProgram;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.Texture2DDesc;
import jet.opengl.postprocessing.texture.TextureUtils;


public class ASSAODemo extends NvSampleApp{
	
	private int m_Framebuffer;
	private Texture2D m_SceneColor;
	private Texture2D m_SceneDepth;
	private FullscreenProgram m_screenProgram;
//	private final Matrix4f m_ViewProj = new Matrix4f();
	private ASSAO_Effect m_Effect;
	private final ASSAO_InputsOpenGL m_Inputs = new ASSAO_InputsOpenGL();
	private final ASSAO_Settings     m_Settings = new ASSAO_Settings();

	@Override
	protected void initRendering() {
		getGLContext().setSwapInterval(0);
		m_Scene = new SSAOCubeScene(m_transformer);
		m_Scene.onCreate();
		
		m_screenProgram = new FullscreenProgram();
		m_Effect = new ASSAOGL();
		((ASSAOGL)m_Effect).InitializeGL();
	}
	
	@Override
	public void display() {
		GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, m_Framebuffer);
		m_Scene.draw();
		GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
		
//		m_Scene.getViewProjMatrix(m_ViewProj);
		m_screenProgram.enable();
		GL11.glDisable(GL11.GL_DEPTH_TEST);
		GL30.glBindVertexArray(0);
		m_SceneColor.bind(0);
		GL11.glDrawArrays(GL11.GL_TRIANGLE_STRIP, 0, 4);
		GL30.glBindVertexArray(0);
		
		m_Inputs.DepthSRV = m_SceneDepth;
		m_Inputs.DrawOpaque = false;
		m_Inputs.MatricesRowMajorOrder = false;
		m_Inputs.NormalSRV = null;
		m_Scene.getProjMatrix(m_Inputs.ProjectionMatrix);
		m_Inputs.ScissorLeft = 0;
		m_Inputs.ScissorTop = 0;
		m_Inputs.ScissorRight = m_SceneDepth.getWidth();
		m_Inputs.ScissorBottom = m_SceneDepth.getHeight();
		m_Inputs.ViewportHeight = m_SceneDepth.getHeight();
		m_Inputs.ViewportWidth = m_SceneDepth.getWidth();
		m_Inputs.CameraFar =m_Scene.getCameraFar();
		m_Inputs.CameraNear =m_Scene.getCameraNear();
		
		m_Effect.Draw(m_Settings, m_Inputs);
		GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
		GL11.glDisable(GL11.GL_BLEND);
		GL11.glViewport(0, 0, m_SceneColor.getWidth(), m_SceneColor.getHeight());
	}

	@Override
	protected void reshape(int width, int height) {
		if(width ==0 || height == 0)
			return;
		
		GL11.glViewport(0, 0, width, height);
		m_Scene.onResize(width, height);
		if(m_Framebuffer == 0){
			m_Framebuffer = GL30.glGenFramebuffers();
		}
		
		if(m_SceneColor != null && (m_SceneColor.getWidth() != width||m_SceneColor.getHeight() != height) ){
			m_SceneColor.dispose();
			m_SceneDepth.dispose();
			
			m_SceneColor = null;
			m_SceneDepth = null;
		}else if(m_SceneColor != null){
			return;
		}
		
		if(m_SceneColor == null){
			Texture2DDesc desc = new Texture2DDesc(width, height, GL11.GL_RGBA8);
			m_SceneColor = TextureUtils.createTexture2D(desc, null);
			m_SceneColor.setMagFilter(GL11.GL_LINEAR);
			m_SceneColor.setMinFilter(GL11.GL_LINEAR);
			
			desc.format = GL14.GL_DEPTH_COMPONENT16;
			m_SceneDepth = TextureUtils.createTexture2D(desc, null);
			m_SceneDepth.setMagFilter(GL11.GL_LINEAR);
			m_SceneDepth.setMinFilter(GL11.GL_NEAREST);
		}
		
		GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, m_Framebuffer);
		{
			GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, m_SceneColor.getTexture(), 0);
			GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, GL11.GL_TEXTURE_2D, m_SceneDepth.getTexture(), 0);
		}
		GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
		GLError.checkError();
	}
}
