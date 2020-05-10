package nv.samples.culling;

import java.nio.ByteBuffer;

import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;

final class BufferValue {
    int   buffer;
    int   stride;
    int   offset;
    int   size;

    private GLFuncProvider gl;

    BufferValue(int buffer){
        this(buffer, 0);
    }

    BufferValue(int buffer, int sizei)
    {
        gl = GLFuncProviderFactory.getGLFuncProvider();
        this.buffer = buffer;
        if (sizei > 0) {
            gl.glBindBuffer(GLenum.GL_COPY_READ_BUFFER, buffer );
            /*if (sizeof( GLsizeiptr ) > 4)
                gl.glGetBufferParameteri64v( GL_COPY_READ_BUFFER, GL_BUFFER_SIZE, (GLint64*)&size );
            else
                gl.glGetBufferParameteriv( GL_COPY_READ_BUFFER, GL_BUFFER_SIZE, (GLint*)&size );*/
            size = gl.glGetBufferParameteri( GLenum.GL_COPY_READ_BUFFER, GLenum.GL_BUFFER_SIZE );
            gl.glBindBuffer( GLenum.GL_COPY_READ_BUFFER, 0 );
        }
        else {
            size = sizei;
        }
    }

    BufferValue(){}

    void BindBufferRange(int target, int index) {
        gl.glBindBufferRange(target, index, buffer, offset, size);
    }
    void TexBuffer(int target, int internalformat) {
        gl.glTexBufferRange(target, internalformat, buffer, offset, size);
    }
     void ClearBufferSubData(int target, int internalformat, int format, int type, ByteBuffer data) {
//        gl.glClearBufferSubData(target,internalformat,offset,size,format,type,data);
        gl.glClearNamedBufferSubData(buffer,internalformat,offset,size,format,type,data);
    }
}
