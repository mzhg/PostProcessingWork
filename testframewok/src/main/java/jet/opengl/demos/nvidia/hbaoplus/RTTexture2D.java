package jet.opengl.demos.nvidia.hbaoplus;

import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;

final class RTTexture2D extends BaseRTTexture{

	private int m_TextureId = 0;
	private int m_FboId = 0;
	private GLFuncProvider gl;
	
	void createOnce(int width, int height, int internalFormat)
    {
        gl = GLFuncProviderFactory.getGLFuncProvider();
        if (m_TextureId == 0)
        {
        	m_TextureId = gl.glGenTexture();
            gl.glBindTexture(GLenum.GL_TEXTURE_2D, m_TextureId);
            gl.glTexImage2D(GLenum.GL_TEXTURE_2D, 0, internalFormat, width, height, 0, getBaseGLFormat(internalFormat), getBaseGLType(internalFormat), null);
            gl.glBindTexture(GLenum.GL_TEXTURE_2D, 0);

//            THROW_IF(GL.glGetError());

            m_FboId = gl.glGenFramebuffer();
            gl.glBindFramebuffer(GLenum.GL_DRAW_FRAMEBUFFER, m_FboId);
            gl.glFramebufferTexture2D(GLenum.GL_FRAMEBUFFER, GLenum.GL_COLOR_ATTACHMENT0, GLenum.GL_TEXTURE_2D, m_TextureId, 0);
            gl.glBindFramebuffer(GLenum.GL_DRAW_FRAMEBUFFER, 0);

//            THROW_IF(GL.glGetError());

            m_AllocatedSizeInBytes = width * height * getFormatSizeInBytes(internalFormat);
        }
    }

    void safeRelease()
    {
        if (m_TextureId != 0)
        {
            gl.glDeleteTexture(m_TextureId);
            m_TextureId = 0;
        }
        if (m_FboId != 0)
        {
            gl.glDeleteFramebuffer(m_FboId);
            m_FboId = 0;
        }
    }

    int getTexture()
    {
//        return GFSDK_SSAO_Texture_GL(GL_TEXTURE_2D, m_TextureId);
    	return m_TextureId;
    }

    int getFramebuffer()
    {
        return m_FboId;
    }
}
