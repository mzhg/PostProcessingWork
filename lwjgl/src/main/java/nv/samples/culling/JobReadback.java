package nv.samples.culling;

import java.nio.ByteBuffer;

import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;

final class JobReadback extends Job {
    // 1 32-bit integer per 32 objects (1 bit per object)
    BufferValue m_bufferVisBitsReadback;
    ByteBuffer m_hostVisBits;

    // Do not use this Job class unless you have to. Persistent
    // mapped buffers are preferred.

    // Copies result into readback buffer
    void resultFromBits( BufferValue bufferVisBitsCurrent ){
        GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();

        int size = /*sizeof(int)*/4 * CullingSystem.minDivide(m_numObjects,32);
        gl.glBindBuffer(GLenum.GL_COPY_READ_BUFFER, bufferVisBitsCurrent.buffer );
        gl.glBindBuffer(GLenum.GL_COPY_WRITE_BUFFER, m_bufferVisBitsReadback.buffer );
        gl.glCopyBufferSubData(GLenum.GL_COPY_READ_BUFFER, GLenum.GL_COPY_WRITE_BUFFER, bufferVisBitsCurrent.offset, m_bufferVisBitsReadback.offset, size);
        gl.glBindBuffer( GLenum.GL_COPY_READ_BUFFER, 0 );
        gl.glBindBuffer( GLenum.GL_COPY_WRITE_BUFFER, 0 );
    }

    // getBufferData into hostVisBits (blocking!)
    void resultClient(){
        GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();

        gl.glBindBuffer(GLenum.GL_COPY_WRITE_BUFFER, m_bufferVisBitsReadback.buffer);
        gl.glGetBufferSubData(GLenum.GL_COPY_WRITE_BUFFER, m_bufferVisBitsReadback.offset, m_bufferVisBitsReadback.size, m_hostVisBits);
        gl.glBindBuffer( GLenum.GL_COPY_WRITE_BUFFER, 0);
    }
}
