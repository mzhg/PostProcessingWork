package jet.opengl.demos.nvidia.waves.samples;

import java.nio.IntBuffer;

import org.lwjgl.opengl.EXTTextureFilterAnisotropic;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL33;

import jet.util.buffer.GLUtil;

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
		g_SamplerLinearMipmapClamp = GL33.glGenSamplers();
		GL33.glSamplerParameteri(g_SamplerLinearMipmapClamp, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
		GL33.glSamplerParameteri(g_SamplerLinearMipmapClamp, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR_MIPMAP_LINEAR);
		GL33.glSamplerParameteri(g_SamplerLinearMipmapClamp, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
		GL33.glSamplerParameteri(g_SamplerLinearMipmapClamp, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
		
		g_SamplerLinearWrap = GL33.glGenSamplers();
		GL33.glSamplerParameteri(g_SamplerLinearWrap, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
		GL33.glSamplerParameteri(g_SamplerLinearWrap, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
		GL33.glSamplerParameteri(g_SamplerLinearWrap, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
		GL33.glSamplerParameteri(g_SamplerLinearWrap, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT);
		
		m_pPointSamplerState = GL33.glGenSamplers();
		GL33.glSamplerParameteri(m_pPointSamplerState, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
		GL33.glSamplerParameteri(m_pPointSamplerState, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR_MIPMAP_NEAREST);
		GL33.glSamplerParameteri(m_pPointSamplerState, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
		GL33.glSamplerParameteri(m_pPointSamplerState, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT);
		GL33.glSamplerParameteri(m_pPointSamplerState, GL12.GL_TEXTURE_WRAP_R, GL11.GL_REPEAT);
		
		g_pHeightSampler = m_pPointSamplerState;
		g_pCubeSampler = g_SamplerLinearMipmapClamp;
		
		int largest = GL11.glGetInteger(EXTTextureFilterAnisotropic.GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT);
		g_pGradientSampler = GL33.glGenSamplers();
		GL33.glSamplerParameteri(g_pGradientSampler, EXTTextureFilterAnisotropic.GL_TEXTURE_MAX_ANISOTROPY_EXT, Math.min(8, largest));
		GL33.glSamplerParameteri(g_pGradientSampler, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
		GL33.glSamplerParameteri(g_pGradientSampler, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT);
		
		g_pPerlinSampler = GL33.glGenSamplers();
		GL33.glSamplerParameteri(g_pPerlinSampler, EXTTextureFilterAnisotropic.GL_TEXTURE_MAX_ANISOTROPY_EXT, Math.min(4, largest));
		GL33.glSamplerParameteri(g_pPerlinSampler, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
		GL33.glSamplerParameteri(g_pPerlinSampler, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT);
		
		g_pFresnelSampler = GL33.glGenSamplers();
		GL33.glSamplerParameteri(g_pFresnelSampler, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
		GL33.glSamplerParameteri(g_pFresnelSampler, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
		GL33.glSamplerParameteri(g_pFresnelSampler, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
		GL33.glSamplerParameteri(g_pFresnelSampler, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
	}
	
	static void destroySamplers(){
		IntBuffer buf = GLUtil.getCachedIntBuffer(6);
		buf.put(g_SamplerLinearMipmapClamp);
		buf.put(g_SamplerLinearWrap);
		buf.put(m_pPointSamplerState);
		buf.put(g_pGradientSampler);
		buf.put(g_pPerlinSampler);
		buf.put(g_pFresnelSampler);
		buf.flip();
		
		GL33.glDeleteSamplers(buf);
	}
}
