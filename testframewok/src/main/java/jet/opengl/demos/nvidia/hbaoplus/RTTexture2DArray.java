package jet.opengl.demos.nvidia.hbaoplus;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Arrays;

import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.util.CacheBuffer;

final class RTTexture2DArray extends BaseRTTexture{

	private static final int MAX_NUM_MRTS = 8;
	
	private final int array_size;
	private int m_TextureArrayId;
	private int m_LayeredFboId;
	private int[] m_OctaSliceFbo = new int[2];
	private GLFuncProvider gl;
    
	public RTTexture2DArray(int array_size) {
		this.array_size = array_size;
	}
	
	void createOnce(int width, int height, int internalFormat)
    {
        gl = GLFuncProviderFactory.getGLFuncProvider();
        if (m_TextureArrayId == 0)
        {
        	m_TextureArrayId = gl.glGenTexture();
            gl.glBindTexture(GLenum.GL_TEXTURE_2D_ARRAY, m_TextureArrayId);
            gl.glTexImage3D(GLenum.GL_TEXTURE_2D_ARRAY, 0, internalFormat, width, height, array_size, 0, getBaseGLFormat(internalFormat), getBaseGLType(internalFormat), (ByteBuffer)null);
            gl.glBindTexture(GLenum.GL_TEXTURE_2D_ARRAY, 0);

//            THROW_IF(GL.glGetError());

            m_AllocatedSizeInBytes = width * height * array_size * getFormatSizeInBytes(internalFormat);
        }

        if (m_LayeredFboId == 0)
        {
        	m_LayeredFboId = gl.glGenFramebuffer();
            gl.glBindFramebuffer(GLenum.GL_DRAW_FRAMEBUFFER, m_LayeredFboId);
            gl.glFramebufferTexture(GLenum.GL_FRAMEBUFFER, GLenum.GL_COLOR_ATTACHMENT0, m_TextureArrayId, 0);
            gl.glBindFramebuffer(GLenum.GL_DRAW_FRAMEBUFFER, 0);

//            THROW_IF(GL.glGetError());
        }

        if (m_OctaSliceFbo[0] == 0)
        {
//            int[] DrawBuffers[MAX_NUM_MRTS];
        	IntBuffer drawBuffers = CacheBuffer.getCachedIntBuffer(8);
            for (int BufferIndex = 0; BufferIndex < MAX_NUM_MRTS; ++BufferIndex)
            {
//                DrawBuffers[BufferIndex] = GL_COLOR_ATTACHMENT0 + BufferIndex;
            	drawBuffers.put(GLenum.GL_COLOR_ATTACHMENT0 + BufferIndex);
            }
            
            drawBuffers.flip();

//            GL.glGenFramebuffers(2, m_OctaSliceFbo);
            gl.glGenFramebuffers(CacheBuffer.wrap(m_OctaSliceFbo));
            for (int PassIndex = 0; PassIndex < 2; ++PassIndex)
            {
                gl.glBindFramebuffer(GLenum.GL_DRAW_FRAMEBUFFER, m_OctaSliceFbo[PassIndex]);
                for (int RenderTargetId = 0; RenderTargetId < array_size/2; ++RenderTargetId)
                {
                    int LayerIndex = PassIndex * MAX_NUM_MRTS + RenderTargetId;
//                    ASSERT(LayerIndex < ARRAY_SIZE);
                    gl.glFramebufferTextureLayer(GLenum.GL_FRAMEBUFFER, GLenum.GL_COLOR_ATTACHMENT0 + RenderTargetId, m_TextureArrayId, 0, LayerIndex);
                }
//                GL.glDrawBuffers(MAX_NUM_MRTS, DrawBuffers);
                gl.glDrawBuffers(drawBuffers);
                gl.glBindFramebuffer(GLenum.GL_DRAW_FRAMEBUFFER, 0);
            }

//            THROW_IF(GL.glGetError());
        }
    }

    void safeRelease()
    {
        if (m_TextureArrayId != 0)
        {
            gl.glDeleteTexture(m_TextureArrayId);
            m_TextureArrayId = 0;
        }
        if (m_LayeredFboId != 0)
        {
            gl.glDeleteFramebuffer(m_LayeredFboId);
            m_LayeredFboId = 0;
        }
        if (m_OctaSliceFbo[0] != 0)
        {
            gl.glDeleteFramebuffers(/*SIZEOF_ARRAY(m_OctaSliceFbo),*/ CacheBuffer.wrap(m_OctaSliceFbo));
//            ZERO_ARRAY(m_OctaSliceFbo);
            Arrays.fill(m_OctaSliceFbo, 0);
        }
    }

    int getTextureArray()
    {
        return m_TextureArrayId;
    }

    int getLayeredFramebuffer()
    {
        return m_LayeredFboId;
    }

    int getOctaSliceFramebuffer(int PassIndex)
    {
//        ASSERT(PassIndex < SIZEOF_ARRAY(m_OctaSliceFbo));
        return m_OctaSliceFbo[PassIndex];
    }
}
