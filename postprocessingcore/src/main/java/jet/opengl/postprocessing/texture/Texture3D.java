package jet.opengl.postprocessing.texture;

import jet.opengl.postprocessing.common.GLAPIVersion;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;

public class Texture3D extends TextureGL{
	
	int width;
	int height;
	int depth;
	
	public Texture3D() {}

	@Override
	public int getWidth() {return width;}
	public int getHeight() { return height;}
	public int getDepth()  { return depth;}
	
	/**
     * Sets the wrap parameter for texture coordinate r.<p>
     * @param mode
     */
    public void setWrapR(int mode){
		GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
		GLAPIVersion version = gl.getGLAPIVersion();

		if(version.major >= 4 && version.minor >= 5){
			gl.glTextureParameteri(textureID, GLenum.GL_TEXTURE_WRAP_R, mode);
    	}else{
			bind();
			gl.glTexParameteri(target, GLenum.GL_TEXTURE_WRAP_R, mode);
    	}
    }
}
