package jet.opengl.postprocessing.texture;

import java.nio.ByteBuffer;

import jet.opengl.postprocessing.common.GLenum;

public class ImageData {

	/** The width of the texture. Default value is -1*/
	public int width = -1;
	/** The height of the texture. Default value is -1*/
	public int height = -1;
	/** The depth of the texture. Default value is -1.*/
	public int depth = -1;
	
	/** The internalFormat the of image present in OpenGL form. */
	public int internalFormat = GLenum.GL_RGBA8;

	public ByteBuffer pixels;

	@Override
	public String toString() {
		return "TextureData [width=" + width + ", height=" + height + ", depth=" + depth
				+ ", internalFormat=" + TextureUtils.getFormatName(internalFormat) + "]";
	}
}
