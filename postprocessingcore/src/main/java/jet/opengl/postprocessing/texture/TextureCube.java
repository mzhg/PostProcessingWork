package jet.opengl.postprocessing.texture;

import jet.opengl.postprocessing.common.GLAPIVersion;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;

public class TextureCube extends TextureGL {
    int size;

    int arraySize = 1;

    public TextureCube() {
        super("TextureCube");
    }

    public TextureCube(String name) {
        super(name);
    }

    @Override
    public final int getWidth() {
        return size;
    }

    @Override
    public final int getHeight(){ return size;}

    public int getArraySize() { return arraySize;}

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
//			bind();
            gl.glTexParameteri(target, GLenum.GL_TEXTURE_WRAP_R, mode);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(80);
        sb.append(name).append(' ').append('[');
        sb.append("textureID: ").append(textureID).append(',').append(' ');
        sb.append("target: ").append(TextureUtils.getTextureTargetName(target)).append(',').append(' ');
        sb.append("size = ").append(size).append(',').append(' ');
        sb.append("arraysize = ").append(arraySize).append(',').append(' ');
        sb.append("format = ").append(TextureUtils.getFormatName(format)).append(',').append(' ');
        sb.append("mipLevels = ").append(mipLevels).append(',').append(' ');

        return sb.toString();
    }
}
