package nv.samples.culling;

import java.nio.ByteBuffer;

import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;

final class JobReadbackPersistent extends Job{
    // 1 32-bit integer per 32 objects (1 bit per object)
    BufferValue m_bufferVisBitsReadback;
    ByteBuffer m_bufferVisBitsMapping;
    ByteBuffer   m_hostVisBits;
    long        m_fence;

    // Copies result into readback buffer and records
    // a fence.
    void resultFromBits(BufferValue bufferVisBitsCurrent){
        GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();

        int size = /*sizeof( int )*/4 * CullingSystem.minDivide( m_numObjects, 32 );
        gl.glCopyNamedBufferSubData( bufferVisBitsCurrent.buffer, m_bufferVisBitsReadback.buffer, bufferVisBitsCurrent.offset, m_bufferVisBitsReadback.offset, size);
        if (m_fence != 0) {
            gl.glDeleteSync( m_fence );
            m_fence = 0;
        }
        m_fence = gl.glFenceSync(/*GL_SYNC_GPU_COMMANDS_COMPLETE, 0*/);
    }

    // waits on fence and copies mapping into hostVisBits
    void resultClient(){
        if (m_fence != 0) {
            GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
//            GLsizeiptr size = sizeof( int ) * minDivide( m_numObjects, 32 );
            int size = /*sizeof( int )*/4 * CullingSystem.minDivide( m_numObjects, 32 );
            // as some samples read-back within same frame (not recommended) we use the flush here, normally one  use it
            gl.glClientWaitSync(m_fence, GLenum.GL_SYNC_FLUSH_COMMANDS_BIT, GLenum.GL_TIMEOUT_IGNORED);
            gl.glDeleteSync(m_fence);
            m_fence = 0;
//            memcpy( m_hostVisBits, ((uint8_t*)m_bufferVisBitsMapping) + m_bufferVisBitsReadback.offset, size );

            int pos = m_bufferVisBitsMapping.position();
            int count = m_bufferVisBitsMapping.limit();

            m_bufferVisBitsMapping.position(m_bufferVisBitsReadback.offset + pos);
            m_bufferVisBitsMapping.limit(m_bufferVisBitsReadback.offset + pos + size);
            m_hostVisBits.put(m_bufferVisBitsMapping);

            m_bufferVisBitsMapping.position(pos).limit(count);

        }
    }
}
