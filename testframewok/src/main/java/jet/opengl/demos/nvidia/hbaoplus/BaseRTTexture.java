package jet.opengl.demos.nvidia.hbaoplus;

import jet.opengl.postprocessing.common.GLenum;

abstract class BaseRTTexture {

	int m_AllocatedSizeInBytes;
	
	public int getAllocatedSizeInBytes()
    {
        return m_AllocatedSizeInBytes;
    }

    static int getFormatSizeInBytes(int internalFormat)
    {
        switch (internalFormat)
        {
        case GLenum.GL_RG16F:
        case GLenum.GL_R32F:
        case GLenum.GL_RGBA8:
            return 4;
        case GLenum.GL_R16F:
            return 2;
        case GLenum.GL_R8:
            return 1;
        }
//        ASSERT(0);
        return 0;
    }

    static int getBaseGLFormat(int InternalFormat)
    {
        switch(InternalFormat)
        {
        case GLenum.GL_R32F:
        case GLenum.GL_R16F:
        case GLenum.GL_R8:
            return GLenum.GL_RED;
        case GLenum.GL_RG16F:
           return GLenum.GL_RG;
        case GLenum.GL_RGBA8:
            return GLenum.GL_RGBA;
        }
//        ASSERT(0);
        return 0;
    }
    
    static int getBaseGLType(int InternalFormat)
    {
        switch(InternalFormat)
        {
        case GLenum.GL_R32F:
        case GLenum.GL_R16F:
        case GLenum.GL_RG16F:
            return GLenum.GL_FLOAT;
        case GLenum.GL_R8:
        case GLenum.GL_RGBA8:
            return GLenum.GL_UNSIGNED_BYTE;
        }
        return 0;
    }
}
