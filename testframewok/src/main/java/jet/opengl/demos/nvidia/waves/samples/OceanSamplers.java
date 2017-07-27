package jet.opengl.demos.nvidia.waves.samples;

import java.nio.IntBuffer;

import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.util.CacheBuffer;

final class OceanSamplers {

	static int g_SamplerLinearMipmapClamp;
	static int g_SamplerLinearWrap;
	static int m_pPointSamplerState;
	static int g_pHeightSampler;
	static int g_pCubeSampler;
	static int g_pGradientSampler;
	static int g_pPerlinSampler;
	static int g_pFresnelSampler;
	
	static void createSamplers(){
		final GLFuncProvider gl= GLFuncProviderFactory.getGLFuncProvider();
		g_SamplerLinearMipmapClamp = gl.glGenSampler();
		gl.glSamplerParameteri(g_SamplerLinearMipmapClamp, GLenum.GL_TEXTURE_MAG_FILTER, GLenum.GL_LINEAR);
		gl.glSamplerParameteri(g_SamplerLinearMipmapClamp, GLenum.GL_TEXTURE_MIN_FILTER, GLenum.GL_LINEAR_MIPMAP_LINEAR);
		gl.glSamplerParameteri(g_SamplerLinearMipmapClamp, GLenum.GL_TEXTURE_WRAP_S, GLenum.GL_CLAMP_TO_EDGE);
		gl.glSamplerParameteri(g_SamplerLinearMipmapClamp, GLenum.GL_TEXTURE_WRAP_T, GLenum.GL_CLAMP_TO_EDGE);
		
		g_SamplerLinearWrap = gl.glGenSampler();
		gl.glSamplerParameteri(g_SamplerLinearWrap, GLenum.GL_TEXTURE_MAG_FILTER, GLenum.GL_LINEAR);
		gl.glSamplerParameteri(g_SamplerLinearWrap, GLenum.GL_TEXTURE_MIN_FILTER, GLenum.GL_LINEAR);
		gl.glSamplerParameteri(g_SamplerLinearWrap, GLenum.GL_TEXTURE_WRAP_S, GLenum.GL_REPEAT);
		gl.glSamplerParameteri(g_SamplerLinearWrap, GLenum.GL_TEXTURE_WRAP_T, GLenum.GL_REPEAT);
		
		m_pPointSamplerState = gl.glGenSampler();
		gl.glSamplerParameteri(m_pPointSamplerState, GLenum.GL_TEXTURE_MAG_FILTER, GLenum.GL_LINEAR);
		gl.glSamplerParameteri(m_pPointSamplerState, GLenum.GL_TEXTURE_MIN_FILTER, GLenum.GL_LINEAR_MIPMAP_NEAREST);
		gl.glSamplerParameteri(m_pPointSamplerState, GLenum.GL_TEXTURE_WRAP_S, GLenum.GL_REPEAT);
		gl.glSamplerParameteri(m_pPointSamplerState, GLenum.GL_TEXTURE_WRAP_T, GLenum.GL_REPEAT);
		gl.glSamplerParameteri(m_pPointSamplerState, GLenum.GL_TEXTURE_WRAP_R, GLenum.GL_REPEAT);
		
		g_pHeightSampler = m_pPointSamplerState;
		g_pCubeSampler = g_SamplerLinearMipmapClamp;
		
		int largest = gl.glGetInteger(GLenum.GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT);
		g_pGradientSampler = gl.glGenSampler();
		gl.glSamplerParameteri(g_pGradientSampler, GLenum.GL_TEXTURE_MAX_ANISOTROPY_EXT, Math.min(8, largest));
		gl.glSamplerParameteri(g_pGradientSampler, GLenum.GL_TEXTURE_WRAP_S, GLenum.GL_REPEAT);
		gl.glSamplerParameteri(g_pGradientSampler, GLenum.GL_TEXTURE_WRAP_T, GLenum.GL_REPEAT);
		
		g_pPerlinSampler = gl.glGenSampler();
		gl.glSamplerParameteri(g_pPerlinSampler, GLenum.GL_TEXTURE_MAX_ANISOTROPY_EXT, Math.min(4, largest));
		gl.glSamplerParameteri(g_pPerlinSampler, GLenum.GL_TEXTURE_WRAP_S, GLenum.GL_REPEAT);
		gl.glSamplerParameteri(g_pPerlinSampler, GLenum.GL_TEXTURE_WRAP_T, GLenum.GL_REPEAT);
		
		g_pFresnelSampler = gl.glGenSampler();
		gl.glSamplerParameteri(g_pFresnelSampler, GLenum.GL_TEXTURE_MAG_FILTER, GLenum.GL_LINEAR);
		gl.glSamplerParameteri(g_pFresnelSampler, GLenum.GL_TEXTURE_MIN_FILTER, GLenum.GL_LINEAR);
		gl.glSamplerParameteri(g_pFresnelSampler, GLenum.GL_TEXTURE_WRAP_S, GLenum.GL_CLAMP_TO_EDGE);
		gl.glSamplerParameteri(g_pFresnelSampler, GLenum.GL_TEXTURE_WRAP_T, GLenum.GL_CLAMP_TO_EDGE);
	}
	
	static void destroySamplers(){
		IntBuffer buf = CacheBuffer.getCachedIntBuffer(6);
		buf.put(g_SamplerLinearMipmapClamp);
		buf.put(g_SamplerLinearWrap);
		buf.put(m_pPointSamplerState);
		buf.put(g_pGradientSampler);
		buf.put(g_pPerlinSampler);
		buf.put(g_pFresnelSampler);
		buf.flip();
		
		GLFuncProviderFactory.getGLFuncProvider().glDeleteSamplers(buf);
	}
}
