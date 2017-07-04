package jet.opengl.demos.postprocessing.hdr;

import java.nio.Buffer;

import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;

/**
 * Created by mazhen'gui on 2017/3/17.
 */

final class VertexBufferObject {
    private int m_vboId;
    private int m_iboId;
    private GLFuncProvider gl;

    VertexBufferObject(){
        gl = GLFuncProviderFactory.getGLFuncProvider();
    }

    void genVertexData(Buffer data, int length, boolean stream){
        m_vboId = gl.glGenBuffer();
        gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, m_vboId);
        gl.glBufferData(GLenum.GL_ARRAY_BUFFER, data, stream?GLenum.GL_STREAM_DRAW:GLenum.GL_STATIC_DRAW);
        gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, 0);
    }

    void genIndexData(Buffer data, int length, boolean stream){
        m_iboId = gl.glGenBuffer();
        gl.glBindBuffer(GLenum.GL_ELEMENT_ARRAY_BUFFER, m_iboId);
        gl.glBufferData(GLenum.GL_ELEMENT_ARRAY_BUFFER, data, stream?GLenum.GL_STREAM_DRAW:GLenum.GL_STATIC_DRAW);
        gl.glBindBuffer(GLenum.GL_ELEMENT_ARRAY_BUFFER, 0);
    }

    void addAttribute(int attribute, int count, int type, boolean noramlize, int stride, int offset){
        gl.glEnableVertexAttribArray(attribute);
        gl.glVertexAttribPointer(attribute, count, type, noramlize, stride, offset);
    }

    void draw(int type, int count, int data_type, int offset){
        gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, m_vboId);
        gl.glBindBuffer(GLenum.GL_ELEMENT_ARRAY_BUFFER, m_iboId);
        gl.glDrawElements(type, count, data_type, offset);
    }

    int getVBO() { return m_vboId; }
    int getIBO() { return m_iboId; }
    void dispose(){
        gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, 0);
        gl.glBindBuffer(GLenum.GL_ELEMENT_ARRAY_BUFFER, 0);
        gl.glDeleteBuffer(m_vboId);
        gl.glDeleteBuffer(m_iboId);
    }
}
