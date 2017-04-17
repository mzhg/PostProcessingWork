package jet.opengl.postprocessing.buffer;

import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;

/**
 * Created by mazhen'gui on 2017/4/15.
 */

public class BufferBinding {
    private BufferGL m_BufferVBO;
    private AttribDesc[] m_AttribDescs;

    public BufferBinding() {
    }

    public BufferBinding(BufferGL bufferVBO, AttribDesc... attribDescs) {
        this.m_BufferVBO = bufferVBO;
        this.m_AttribDescs = attribDescs;
    }

    public BufferGL getBufferVBO() {
        return m_BufferVBO;
    }

    public AttribDesc[] getAttribDescs() {
        return m_AttribDescs;
    }

    public void bind(){
        if(m_BufferVBO != null)
            m_BufferVBO.bind();

        GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
        for(int i = 0; i < m_AttribDescs.length; i++){
            AttribDesc attribDesc = m_AttribDescs[i];
            gl.glEnableVertexAttribArray(attribDesc.index);
            gl.glVertexAttribPointer(attribDesc.index, attribDesc.size, attribDesc.type, attribDesc.normalized, attribDesc.stride, attribDesc.offset);
            gl.glVertexAttribDivisor(attribDesc.index,attribDesc.divisor);
        }
    }

    public void unbind(){
        if(m_BufferVBO != null)
            m_BufferVBO.unbind();

        GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
        for(int i = 0; i < m_AttribDescs.length; i++){
            AttribDesc attribDesc = m_AttribDescs[i];
            gl.glDisableVertexAttribArray(attribDesc.index);
            gl.glVertexAttribDivisor(attribDesc.index,0);
        }
    }
}
