package jet.opengl.postprocessing.texture;

import java.nio.FloatBuffer;
import java.util.HashMap;

import jet.opengl.postprocessing.common.GLAPIVersion;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.util.CacheBuffer;
import jet.opengl.postprocessing.util.LogUtil;
import jet.opengl.postprocessing.util.Numeric;

public final class SamplerUtils {

	private static final HashMap<SamplerDesc, Integer> sampler_caches = new HashMap<SamplerDesc, Integer>();

	private static final SamplerDesc g_DefaultSampler = new SamplerDesc();
	private static final SamplerDesc g_DepthComparisonSampler = new SamplerDesc();

	static {
		g_DepthComparisonSampler.minFilter = GLenum.GL_NEAREST;
		g_DepthComparisonSampler.compareFunc = GLenum.GL_LESS;
		g_DepthComparisonSampler.compareMode = GLenum.GL_COMPARE_REF_TO_TEXTURE;
	}

	private static final int UNKOWN = 0;
	private static final int ENABLE = 1;
	private static final int DISABLE = 2;

	private static int g_SampleState = UNKOWN;

	public static boolean isSamplerSupport(){
		if(g_SampleState == UNKOWN){
			GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
			GLAPIVersion version = gl.getGLAPIVersion();
			if((version.ES && version.major >= 3) || gl.isSupportExt("GL_ARB_sampler_objects")){
				g_SampleState = ENABLE;
			}else{
				g_SampleState = DISABLE;
			}
		}

		return (g_SampleState == ENABLE);
	}
	
	public static void releaseCaches(){
		GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
		gl.glDeleteSamplers(sampler_caches.values());
		
		sampler_caches.clear();
	}

	public static int getDefaultSampler(){
		return createSampler(g_DefaultSampler);
	}

	public static int getDepthComparisonSampler(){
		return createSampler(g_DepthComparisonSampler);
	}
	
	public static int createSampler(SamplerDesc sampler){
		if(!isSamplerSupport()){
			LogUtil.i(LogUtil.LogType.DEFAULT, "Unsupport the sampler object");
			return 0;
		}
		
		Integer s = sampler_caches.get(sampler);
		if(s != null)
			return s;
		else{
			s = _createSampler(sampler);
			sampler_caches.put(new SamplerDesc(sampler), s);
			return s;
		}
	}
	
	private static int _createSampler(SamplerDesc sampler){
		if(!isSamplerSupport()){
			LogUtil.i(LogUtil.LogType.DEFAULT, "Unsupport the sampler object");
			return 0;
		}

		GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
		int obj = gl.glGenSampler();
		gl.glSamplerParameteri(obj, GLenum.GL_TEXTURE_MIN_FILTER, sampler.minFilter);
		gl.glSamplerParameteri(obj, GLenum.GL_TEXTURE_MAG_FILTER, sampler.magFilter);
		gl.glSamplerParameteri(obj, GLenum.GL_TEXTURE_WRAP_S, sampler.wrapS);
		gl.glSamplerParameteri(obj, GLenum.GL_TEXTURE_WRAP_T, sampler.wrapT);
		if(sampler.wrapR != 0)
			gl.glSamplerParameteri(obj, GLenum.GL_TEXTURE_WRAP_R, sampler.wrapR);
		if(sampler.borderColor != 0){
			float r = Numeric.getRedFromRGBf(sampler.borderColor);
			float g = Numeric.getGreenf(sampler.borderColor);
			float b = Numeric.getBlueFromRGBf(sampler.borderColor);
			float a = Numeric.getAlphaf(sampler.borderColor);
			
			FloatBuffer buf = CacheBuffer.getCachedFloatBuffer(4);
			buf.put(r).put(g).put(b).put(a).flip();
			gl.glSamplerParameterfv(obj, GLenum.GL_TEXTURE_BORDER_COLOR, buf);
//			LogUtil.i(LogUtil.LogType.DEFAULT, "Border Color =(" + r + ", " + g + ", " + b + ", " + a + ")");
		}
		
		if(sampler.anisotropic > 0 && !gl.getGLAPIVersion().ES){
			int largest = gl.glGetInteger(GLenum.GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT);
			gl.glSamplerParameteri(obj, GLenum.GL_TEXTURE_MAX_ANISOTROPY_EXT, Math.min(sampler.anisotropic, largest));
		}
		
		if(sampler.compareFunc != 0){
			gl.glSamplerParameteri(obj, GLenum.GL_TEXTURE_COMPARE_FUNC, sampler.compareFunc);
		}
		
		if(sampler.compareMode != 0){
			gl.glSamplerParameteri(obj, GLenum.GL_TEXTURE_COMPARE_MODE, sampler.compareMode);
		}
		
		return obj;
	}

	/**
	 * Apply the defualt sampler to a cube map texture which binding.
	 * @param mipmap Indicate whether use mipmap on the min filter.
     */
	public static void applyCubemapSampler(boolean mipmap){
		final GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();

		int target = GLenum.GL_TEXTURE_CUBE_MAP;
		int minFilter = mipmap ? GLenum.GL_LINEAR_MIPMAP_LINEAR : GLenum.GL_LINEAR;
		int magFilter = GLenum.GL_LINEAR;
		int wrap = GLenum.GL_CLAMP_TO_EDGE;

		gl.glTexParameteri(target, GLenum.GL_TEXTURE_MIN_FILTER, minFilter);
		gl.glTexParameteri(target, GLenum.GL_TEXTURE_MAG_FILTER, magFilter);
		gl.glTexParameteri(target, GLenum.GL_TEXTURE_WRAP_S, wrap);
		gl.glTexParameteri(target, GLenum.GL_TEXTURE_WRAP_T, wrap);
		gl.glTexParameteri(target, GLenum.GL_TEXTURE_WRAP_R, wrap);
	}

	// The default setting apply to a texture2D image.
	public static void applyTexture2DLinearClampSampler(boolean mipmap){
		applyTexture2DSampler(GLenum.GL_TEXTURE_2D, mipmap ? GLenum.GL_LINEAR_MIPMAP_LINEAR : GLenum.GL_LINEAR, GLenum.GL_LINEAR, GLenum.GL_CLAMP_TO_EDGE, GLenum.GL_CLAMP_TO_EDGE);
	}
	
	public static void applyTexture2DSampler(int target, int minFilter, int magFilter, int wrapS, int wrapT){
		final GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();

		gl.glTexParameteri(target, GLenum.GL_TEXTURE_MIN_FILTER, minFilter);
		gl.glTexParameteri(target, GLenum.GL_TEXTURE_MAG_FILTER, magFilter);
		gl.glTexParameteri(target, GLenum.GL_TEXTURE_WRAP_S, wrapS);
		gl.glTexParameteri(target, GLenum.GL_TEXTURE_WRAP_T, wrapT);
	}

	/**
	 * Apply the given sampler to the current binding texture.
	 * @param target The texture target. e.g: GL_TEXTURE2D, GL_TEXTURE2D_ARRAY...
	 * @param desc  The sampler which will be apllying to.
     */
	public static void applySampler(int target, SamplerDesc desc){
		final GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();

		gl.glTexParameteri(target, GLenum.GL_TEXTURE_MIN_FILTER, desc.minFilter);
		gl.glTexParameteri(target, GLenum.GL_TEXTURE_MAG_FILTER, desc.magFilter);
		gl.glTexParameteri(target, GLenum.GL_TEXTURE_WRAP_S, desc.wrapS);
		gl.glTexParameteri(target, GLenum.GL_TEXTURE_WRAP_T, desc.wrapT);
		if((target == GLenum.GL_TEXTURE_3D || target == GLenum.GL_TEXTURE_CUBE_MAP) && desc.wrapR != 0)
			gl.glTexParameteri(target, GLenum.GL_TEXTURE_WRAP_R, desc.wrapR);
		
		float r = Numeric.getRedFromRGBf(desc.borderColor);
		float g = Numeric.getGreenf(desc.borderColor);
		float b = Numeric.getBlueFromRGBf(desc.borderColor);
		float a = Numeric.getAlphaf(desc.borderColor);
		
		FloatBuffer buf = CacheBuffer.getCachedFloatBuffer(4);
		buf.put(r).put(g).put(b).put(a).flip();
		gl.glTexParameterfv(target, GLenum.GL_TEXTURE_BORDER_COLOR, buf);
		
		int largest = gl.glGetInteger(GLenum.GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT);
		gl.glTexParameteri(target, GLenum.GL_TEXTURE_MAX_ANISOTROPY_EXT, Math.min(Math.max(0, desc.anisotropic), largest));

		gl.glTexParameteri(target, GLenum.GL_TEXTURE_COMPARE_FUNC, desc.compareFunc);
		gl.glTexParameteri(target, GLenum.GL_TEXTURE_COMPARE_MODE, desc.compareMode);
	}

	/**
	 * Apply the depth comparsion filter to the current binding texture.
	 * @param mipmap
     */
	public static void applyDepthTexture2DSampler(boolean mipmap){
		final GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();

		applyTexture2DSampler(GLenum.GL_TEXTURE_2D, mipmap ? GLenum.GL_NEAREST_MIPMAP_LINEAR : GLenum.GL_NEAREST, GLenum.GL_LINEAR, GLenum.GL_CLAMP_TO_EDGE, GLenum.GL_CLAMP_TO_EDGE);
		gl.glTexParameteri(GLenum.GL_TEXTURE_2D, GLenum.GL_TEXTURE_COMPARE_FUNC, GLenum.GL_LESS);
		gl.glTexParameteri(GLenum.GL_TEXTURE_2D, GLenum.GL_TEXTURE_COMPARE_MODE, GLenum.GL_COMPARE_REF_TO_TEXTURE);
	}
}
