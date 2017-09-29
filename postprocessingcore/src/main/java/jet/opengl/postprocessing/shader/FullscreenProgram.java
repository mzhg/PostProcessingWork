package jet.opengl.postprocessing.shader;

import org.lwjgl.util.vector.Matrix4f;

import java.io.IOException;

import jet.opengl.postprocessing.common.GLCheck;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.util.CacheBuffer;

public class FullscreenProgram extends GLSLProgram {
	
	/** The varying variable between vertex shader and fragment shader. */
	public static final String TEXCOORD0 = "vec2 v_texcoords";

	private final int posMatIndex;
//	private final int texMatIndex;
	private final int texSamplerIndex;

	protected final GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();

	public FullscreenProgram(boolean enablePosTransform){
		try {
			Macro macro = null;
			if(enablePosTransform){
				macro = new Macro("ENABLE_POS_TRANSFORM", 1);
			}
			setAttribBinding(new AttribBinder("In_f4Postion", 0));
			setSourceFromFiles("shader_libs/PostProcessingDefaultScreenSpaceVS.vert", getFragShaderFile(), macro);
		} catch (IOException e) {
			e.printStackTrace();
		}

		gl.glUseProgram(m_program);
		posMatIndex = getUniformLocation("g_PosTransform");
//		texMatIndex = getUniformLocation("g_PosTransform");
		texSamplerIndex = getUniformLocation("g_InputTex");

		applyDefaultUniforms();
		gl.glUseProgram(0);

		GLCheck.checkError("FullscreenProgram::init");
	}

	public FullscreenProgram() { this(false);}
	
	protected String getFragShaderFile() { return "shader_libs/PostProcessingDefaultScreenSpacePS.frag";}
	
	protected void applyDefaultUniforms(){
		if(texSamplerIndex >= 0)
			gl.glUniform1i(texSamplerIndex, 0);
		if(posMatIndex >= 0)
			gl.glUniformMatrix4fv(posMatIndex, false, CacheBuffer.wrap(Matrix4f.IDENTITY));
//		gl.glUniformMatrix4fv(texMatIndex, false, CacheBuffer.wrap(Matrix4f.IDENTITY));
	}
	
	public void applyPositionTransform(Matrix4f mat){ if(posMatIndex >= 0) gl.glUniformMatrix4fv(posMatIndex, false, CacheBuffer.wrap(mat));}
//	public void applyTexcoordTransform(Matrix4f mat){ gl.glUniformMatrix4fv(texMatIndex, false, CacheBuffer.wrap(mat));}
}
