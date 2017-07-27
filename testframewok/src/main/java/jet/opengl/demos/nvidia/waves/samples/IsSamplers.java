package jet.opengl.demos.nvidia.waves.samples;

import jet.opengl.postprocessing.common.GLCheck;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.util.CachaRes;
import jet.opengl.postprocessing.util.CacheBuffer;

final class IsSamplers {

	static int g_SamplerLinearWrap;
	static int g_SamplerLinearMipmapWrap;
	static int g_SamplerLinearClamp;
	static int g_SamplerAnisotropicWrap;
	static int g_SamplerDepthAnisotropic;

	@CachaRes
	static void createSamplers(){
		final GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
		g_SamplerLinearWrap = gl.glGenSampler();
		gl.glSamplerParameteri(g_SamplerLinearWrap, GLenum.GL_TEXTURE_MIN_FILTER, GLenum.GL_LINEAR);
		gl.glSamplerParameteri(g_SamplerLinearWrap, GLenum.GL_TEXTURE_MAG_FILTER, GLenum.GL_LINEAR);
		gl.glSamplerParameteri(g_SamplerLinearWrap, GLenum.GL_TEXTURE_WRAP_S, GLenum.GL_REPEAT);
		gl.glSamplerParameteri(g_SamplerLinearWrap, GLenum.GL_TEXTURE_WRAP_T, GLenum.GL_REPEAT);
		
		g_SamplerLinearMipmapWrap = gl.glGenSampler();
		gl.glSamplerParameteri(g_SamplerLinearMipmapWrap, GLenum.GL_TEXTURE_MIN_FILTER, GLenum.GL_LINEAR_MIPMAP_LINEAR);
		gl.glSamplerParameteri(g_SamplerLinearMipmapWrap, GLenum.GL_TEXTURE_MAG_FILTER, GLenum.GL_LINEAR);
		gl.glSamplerParameteri(g_SamplerLinearMipmapWrap, GLenum.GL_TEXTURE_WRAP_S, GLenum.GL_REPEAT);
		gl.glSamplerParameteri(g_SamplerLinearMipmapWrap, GLenum.GL_TEXTURE_WRAP_T, GLenum.GL_REPEAT);
		
		g_SamplerLinearClamp =gl.glGenSampler();
		gl.glSamplerParameteri(g_SamplerLinearClamp, GLenum.GL_TEXTURE_MIN_FILTER, GLenum.GL_LINEAR);
		gl.glSamplerParameteri(g_SamplerLinearClamp, GLenum.GL_TEXTURE_MAG_FILTER, GLenum.GL_LINEAR);
		gl.glSamplerParameteri(g_SamplerLinearClamp, GLenum.GL_TEXTURE_WRAP_S, GLenum.GL_CLAMP_TO_EDGE);
		gl.glSamplerParameteri(g_SamplerLinearClamp, GLenum.GL_TEXTURE_WRAP_T, GLenum.GL_CLAMP_TO_EDGE);
		
		g_SamplerAnisotropicWrap = gl.glGenSampler();
		int largest = gl.glGetInteger(GLenum.GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT);
		gl.glSamplerParameteri(g_SamplerAnisotropicWrap, GLenum.GL_TEXTURE_MAX_ANISOTROPY_EXT, Math.min(16, largest));
		gl.glSamplerParameteri(g_SamplerAnisotropicWrap, GLenum.GL_TEXTURE_WRAP_S, GLenum.GL_REPEAT);
		gl.glSamplerParameteri(g_SamplerAnisotropicWrap, GLenum.GL_TEXTURE_WRAP_T, GLenum.GL_REPEAT);
		
		g_SamplerDepthAnisotropic = gl.glGenSampler();
		gl.glSamplerParameteri(g_SamplerDepthAnisotropic, GLenum.GL_TEXTURE_MAX_ANISOTROPY_EXT, Math.min(16, largest));
		gl.glSamplerParameteri(g_SamplerDepthAnisotropic, GLenum.GL_TEXTURE_MAG_FILTER, GLenum.GL_NEAREST);
		gl.glSamplerParameteri(g_SamplerDepthAnisotropic, GLenum.GL_TEXTURE_MIN_FILTER, GLenum.GL_NEAREST);
		gl.glSamplerParameteri(g_SamplerDepthAnisotropic, GLenum.GL_TEXTURE_WRAP_S, GLenum.GL_CLAMP_TO_BORDER);
		gl.glSamplerParameteri(g_SamplerDepthAnisotropic, GLenum.GL_TEXTURE_WRAP_T, GLenum.GL_CLAMP_TO_BORDER);
		gl.glSamplerParameteri(g_SamplerDepthAnisotropic, GLenum.GL_TEXTURE_COMPARE_FUNC, GLenum.GL_LESS);
		gl.glSamplerParameteri(g_SamplerDepthAnisotropic, GLenum.GL_TEXTURE_COMPARE_MODE, GLenum.GL_COMPARE_REF_TO_TEXTURE);
		gl.glSamplerParameterfv(g_SamplerDepthAnisotropic, GLenum.GL_TEXTURE_BORDER_COLOR, CacheBuffer.wrap(1.f,1,1,1));
		
		GLCheck.checkError();
	}
}
