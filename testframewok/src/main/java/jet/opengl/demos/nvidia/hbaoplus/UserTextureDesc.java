package jet.opengl.demos.nvidia.hbaoplus;

import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.texture.Texture2D;

final class UserTextureDesc {

	int width;
	int height;
	int sampleCount;
	
	int textureID;
	int target;
	
	GFSDK_SSAO_Status init(Texture2D inputGLTexture){
		if(!hasValidTextureTarget(inputGLTexture)){
			throw new IllegalArgumentException(GFSDK_SSAO_Status.GFSDK_SSAO_GL_INVALID_TEXTURE_TARGET.name());
//			return GFSDK_SSAO_Status.GFSDK_SSAO_GL_INVALID_TEXTURE_TARGET;
		}
		
		if(inputGLTexture.getTexture() == 0){
			throw new IllegalArgumentException(GFSDK_SSAO_Status.GFSDK_SSAO_GL_INVALID_TEXTURE_OBJECT.name());
			// The name space for texture objects is the unsigned integers, with zero reserved by the GL.
//            return GFSDK_SSAO_Status.GFSDK_SSAO_GL_INVALID_TEXTURE_OBJECT;
		}
		
		width = inputGLTexture.getWidth();
		height = inputGLTexture.getHeight();
		textureID = inputGLTexture.getTexture();
		target = inputGLTexture.getTarget();
		sampleCount = inputGLTexture.getSampleCount();
		
		return GFSDK_SSAO_Status.GFSDK_SSAO_OK;
	}
	
	boolean isSet()
    {
        return (textureID != 0);
    }
	
	static boolean hasValidTextureTarget(Texture2D Texture)
	{
       return (Texture.getTarget() == GLenum.GL_TEXTURE_2D || Texture.getTarget() == GLenum.GL_TEXTURE_2D_MULTISAMPLE);
	}
}
