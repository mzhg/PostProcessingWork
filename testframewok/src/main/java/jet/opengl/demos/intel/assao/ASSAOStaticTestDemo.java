package jet.opengl.demos.intel.assao;

import com.nvidia.developer.opengl.app.NvSampleApp;

import org.lwjgl.util.vector.Matrix4f;

import java.nio.ByteBuffer;

import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.FullscreenProgram;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.Texture2DDesc;
import jet.opengl.postprocessing.texture.TextureDataDesc;
import jet.opengl.postprocessing.texture.TextureUtils;
import jet.opengl.postprocessing.util.DebugTools;

public class ASSAOStaticTestDemo extends NvSampleApp{
	private static final int g_TestScreenWidth = 1600;
	private static final int g_TestScreenHeight = 900;

	private static final int FORMAT = 5_760_000/(1600 * 900);

	private FullscreenProgram m_screenProgram;
	private final Matrix4f m_ViewProj = new Matrix4f();
	private ASSAO_Effect m_Effect;
	private final ASSAO_InputsOpenGL m_Inputs = new ASSAO_InputsOpenGL();
	private final ASSAO_Settings     m_Settings = new ASSAO_Settings();
	private GLFuncProvider gl;

	private Texture2D m_DepthSRV;
	private Texture2D m_NormalSRV;
	private boolean                         m_adaptive = true;

	@Override
	protected void initRendering() {
		ASSAOGL.ASSAO_DEBUG = true;
		getGLContext().setSwapInterval(0);
		gl = GLFuncProviderFactory.getGLFuncProvider();

		/*ByteBuffer bytes = DebugTools.loadBinary(getFolderName() + "InputData.dat");
		m_Inputs.load(bytes);*/
		Matrix4f.perspectiveLH(45, (float)g_TestScreenWidth/g_TestScreenHeight, 0.1f, 10000.0f, m_Inputs.ProjectionMatrix);
		m_Inputs.ProjectionMatrix.transpose();
		System.out.println(m_Inputs);

		ByteBuffer bytes = DebugTools.loadBinary(getFolderName() + "ASSAO_Settings_Init.dat");
		m_Settings.load(bytes);
		System.out.println(m_Settings);

		m_screenProgram = new FullscreenProgram();
		m_Effect = new ASSAOGL();
		((ASSAOGL)m_Effect).InitializeGL();
		m_DepthSRV = loadTextureFromBinaryFile("DepthSRV.dat", g_TestScreenWidth, g_TestScreenHeight, GLenum.GL_R32F, 1);
		m_NormalSRV = loadTextureFromBinaryFile("DeferredNormalSRV.dat", g_TestScreenWidth, g_TestScreenHeight, GLenum.GL_RGBA8, 1);
	}
	
	@Override
	public void display() {
		m_Inputs.DepthSRV = m_DepthSRV;
		m_Inputs.DrawOpaque = false;
		m_Inputs.MatricesRowMajorOrder = false;
		m_Inputs.NormalSRV = m_NormalSRV;
//		m_Scene.getProjMatrix(m_Inputs.ProjectionMatrix);
		m_Inputs.ScissorLeft = 0;
		m_Inputs.ScissorTop = 0;
		m_Inputs.ScissorRight = g_TestScreenHeight;
		m_Inputs.ScissorBottom = g_TestScreenWidth;
		m_Inputs.ViewportHeight = g_TestScreenHeight;
		m_Inputs.ViewportWidth = g_TestScreenWidth ;

		if(m_adaptive){
			m_Settings.QualityLevel = 3;
		}
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

	}

	private Texture2D loadTextureFromBinaryFile(String filename, int width, int height, int format, int arraySize){
		boolean flip = false;
		Texture2DDesc desc  = new Texture2DDesc(width, height, format);
		desc.arraySize = arraySize;
		String fullFileName = "E:\\textures\\ASSAODX%s\\%s";
		fullFileName = String.format(fullFileName, m_adaptive ? "_ADAPTIVE":"", filename);
		ByteBuffer bytes = DebugTools.loadBinary(fullFileName);

		if(flip){
			TextureUtils.flipY(bytes, height);
		}
		int type = TextureUtils.measureDataType(format);
		format = TextureUtils.measureFormat(format);
		TextureDataDesc data = new TextureDataDesc(format, type, bytes);
		Texture2D texture = TextureUtils.createTexture2D(desc, data);

		return texture;
	}

	private String getFolderName(){
		return "E:\\textures\\ASSAODX" + (m_adaptive ? "_ADAPTIVE\\":"\\");
	}
}
