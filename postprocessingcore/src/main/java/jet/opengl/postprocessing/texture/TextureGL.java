package jet.opengl.postprocessing.texture;

import jet.opengl.postprocessing.common.Disposeable;
import jet.opengl.postprocessing.common.GLAPIVersion;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;

public abstract class TextureGL implements Disposeable {

	int textureID;
	int target;
	
	int format;
    int mipLevels;
	
    TextureGL() {}
    
    public final int getTexture() { return textureID;}
    public abstract int getWidth();
    public int getHeight()  { return 1;}
    public int getDepth()   { return 1;}
    public final int getFormat()  { return format;}
    public int getMipLevels() { return mipLevels;}
    public final int getTarget()  { return target; }
    public int getSampleCount() { return 1;}

	/*
    public void bindImage(int unit, int access, int format){
		GLStateTracker.getInstance().bindImage(unit, textureID, 0, false, 0, access, format);
    }
    
    public void bindImage(int unit, int access){
    	bindImage(unit, access, format);
    }
    
    public void bind(){
    	GLStateTracker.getInstance().bindTexture(0, target, textureID, 0);
    }
    
    public void bind(int unit){ bind(unit, 0);}
    public void bind(int unit, int sampler){
		GLStateTracker.getInstance().bindTexture(0, target, textureID, sampler);
    }
    */
    /*
    public void unbind(){
    	if(bindType == 1){
    		if(bindingUnit >= 0)
    			GL13.glActiveTexture(GL13.GL_TEXTURE0 + bindingUnit);
	    	GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
	    	if(hasSampler){
	    		ARBSamplerObjects.glBindSampler(bindingUnit, 0);
	    	}
    	}else if(bindType == 2){
    		GL42.glBindImageTexture(bindingUnit, 0, 0, false, 0, GL15.GL_READ_ONLY, format);
    	}
    	
    	hasSampler = false;
    	bindingUnit = -1;
    	bindType = 0;
    }*/
    
	@Override
	public void dispose() {
		if(textureID != 0){
			GLFuncProviderFactory.getGLFuncProvider().glDeleteTextures(textureID);
			textureID = 0;
		}
	}
	
	public boolean isValid(){
		return textureID != 0 && GLFuncProviderFactory.getGLFuncProvider().glIsTexture(textureID);
	}
	
	/**
     * Set the minification property for the texture.<P>
     * @param minFilter
     */
    public void setMinFilter(int minFilter){
		GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
		GLAPIVersion version = gl.getGLAPIVersion();

    	if(version.major >= 4 && version.minor >= 5){
    		gl.glTextureParameteri(textureID, GLenum.GL_TEXTURE_MIN_FILTER, minFilter);
    	}else{
//			bind();
			gl.glTexParameteri(target, GLenum.GL_TEXTURE_MIN_FILTER, minFilter);
    	}
    }
    
    /**
     * Set the magnification property for the texture.<P>
     * @param magFilter
     */
    public void setMagFilter(int magFilter){
		GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
		GLAPIVersion version = gl.getGLAPIVersion();

    	if(version.major >= 4 && version.minor >= 5){
			gl.glTextureParameteri(textureID, GLenum.GL_TEXTURE_MAG_FILTER, magFilter);
    	}else{
//			bind();
			gl.glTexParameteri(target, GLenum.GL_TEXTURE_MAG_FILTER, magFilter);
    	}
    }
    
    /**
     * Sets the wrap parameter for texture coordinate s.<p>
     * @param mode
     */
    public void setWrapS(int mode){
		GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
		GLAPIVersion version = gl.getGLAPIVersion();

		if(version.major >= 4 && version.minor >= 5){
			gl.glTextureParameteri(textureID, GLenum.GL_TEXTURE_WRAP_S, mode);
    	}else{
//			bind();
			gl.glTexParameteri(target, GLenum.GL_TEXTURE_WRAP_S, mode);
    	}
    }
    
    /**
     * Sets the wrap parameter for texture coordinate t.<p>
     * @param mode
     */
    public void setWrapT(int mode){
		GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
		GLAPIVersion version = gl.getGLAPIVersion();

		if(version.major >= 4 && version.minor >= 5){
			gl.glTextureParameteri(textureID, GLenum.GL_TEXTURE_WRAP_T, mode);
    	}else{
//			bind();
			gl.glTexParameteri(target, GLenum.GL_TEXTURE_WRAP_T, mode);
    	}
    }
    
    /**
     * Sets the swizzles that will be applied to the r, g, b, and a components of a texel before they are returned to the shader.<p>
     * The way to setting up the property using the appropriate functions available in the hardware drivers.<ul>
     * @param rgba
     */
    public void setSwizzleRGBA(int[] rgba){
		GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
		GLAPIVersion version = gl.getGLAPIVersion();

		if(version.major >= 4 && version.minor >= 5){
			gl.glTextureParameteriv(textureID, GLenum.GL_TEXTURE_SWIZZLE_RGBA, rgba);
    	}else{
//			bind();
			gl.glTexParameteriv(target, GLenum.GL_TEXTURE_SWIZZLE_RGBA, rgba);
    	}
    }
	
}
