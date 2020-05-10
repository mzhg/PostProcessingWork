package nv.samples.culling;

import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.GLSLProgram;

class JobIndirectUnordered extends Job {
    GLSLProgram m_program_indirect_compact;
    // 1 indirectSize per object,
    BufferValue  m_bufferObjectIndirects;
    BufferValue  m_bufferIndirectResult;
    // 1 integer
    BufferValue  m_bufferIndirectCounter;

    @Override
    void resultFromBits(BufferValue bufferVisBitsCurrent) {
        GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
        gl.glEnable(GLenum.GL_RASTERIZER_DISCARD);

        gl.glUseProgram(m_program_indirect_compact.getProgram());

        m_bufferIndirectCounter.BindBufferRange(GLenum.GL_ATOMIC_COUNTER_BUFFER, 0);
        m_bufferIndirectCounter.ClearBufferSubData (GLenum.GL_ATOMIC_COUNTER_BUFFER, GLenum.GL_R32UI, GLenum.GL_RED_INTEGER, GLenum.GL_UNSIGNED_INT, null);

        bufferVisBitsCurrent.   BindBufferRange(GLenum.GL_SHADER_STORAGE_BUFFER, 2);
        m_bufferObjectIndirects.BindBufferRange(GLenum.GL_SHADER_STORAGE_BUFFER, 1);
        m_bufferIndirectResult. BindBufferRange(GLenum.GL_SHADER_STORAGE_BUFFER, 0);
        m_bufferIndirectResult. ClearBufferSubData(GLenum.GL_SHADER_STORAGE_BUFFER, GLenum.GL_R32UI, GLenum.GL_RED_INTEGER, GLenum.GL_UNSIGNED_INT, null);

        gl.glMemoryBarrier(GLenum.GL_SHADER_STORAGE_BARRIER_BIT);
        gl.glDrawArrays(GLenum.GL_POINTS,0,m_numObjects);

        gl.glDisable(GLenum.GL_RASTERIZER_DISCARD);
        gl.glBindBufferBase  (GLenum.GL_ATOMIC_COUNTER_BUFFER, 0, 0);
        gl.glBindBufferBase  (GLenum.GL_SHADER_STORAGE_BUFFER, 2, 0);
        gl.glBindBufferBase  (GLenum.GL_SHADER_STORAGE_BUFFER, 1, 0);
        gl.glBindBufferBase  (GLenum.GL_SHADER_STORAGE_BUFFER, 0, 0);
    }
}
