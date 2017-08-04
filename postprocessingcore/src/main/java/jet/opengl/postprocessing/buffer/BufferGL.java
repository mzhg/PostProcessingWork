package jet.opengl.postprocessing.buffer;

import java.nio.Buffer;
import java.nio.ByteBuffer;

import jet.opengl.postprocessing.common.Disposeable;
import jet.opengl.postprocessing.common.GLCheck;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.util.BufferUtils;

/**
 * Created by mazhen'gui on 2017/4/15.
 */

public class BufferGL implements Disposeable{
    private int m_target;  // GL Target
    private int m_bufferSize;
    private int m_usage;

    private ByteBuffer m_mapBuffer;
    private int m_bufferID;
    private GLFuncProvider gl;
    private boolean m_bInMapping;

    public void initlize(int target, int size, Buffer data, int usage/*, boolean persistent*/){
        gl = GLFuncProviderFactory.getGLFuncProvider();
//        GLAPIVersion version = gl.getGLAPIVersion();
        if(m_bufferID == 0){
            m_bufferID = gl.glGenBuffer();
        }

        gl.glBindBuffer(target, m_bufferID);
        if(data == null){
            gl.glBufferData(target, (int)size, usage);
        }else{
            if(GLCheck.CHECK){
                if(size != BufferUtils.measureSize(data)){
                    throw new IllegalArgumentException();
                }
            }
            gl.glBufferData(target, data, usage);
        }

        m_target = target;
        m_usage = usage;
        m_bufferSize = size;
    }

    public void update(int offset, Buffer data){
        if(GLCheck.CHECK){
            int size = BufferUtils.measureSize(data);
            if(m_bufferID == 0 || offset < 0 || size < 0 || offset > m_bufferSize || size > m_bufferSize || offset + size > m_bufferSize){
                throw new IllegalArgumentException();
            }
        }

        gl.glBindBuffer(m_target, m_bufferID);
        GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
        gl.glBufferSubData(m_target, offset, data);
    }

    public ByteBuffer map(int offset, int bufferSize, int mapBits){
        if(GLCheck.CHECK){
            if(offset < 0 || bufferSize > m_bufferSize || offset + bufferSize > m_bufferSize)
                throw new IndexOutOfBoundsException();

            if(m_bInMapping){
                throw new IllegalStateException("The buffer had already in mapping.");
            }

            m_bInMapping = true;
        }

        gl.glBindBuffer(m_target, m_bufferID);
        m_mapBuffer = gl.glMapBufferRange(m_target, offset, bufferSize, mapBits, m_mapBuffer);
        return m_mapBuffer;
    }

    public void unmap(){
        if(GLCheck.CHECK){
            if(!m_bInMapping){
                throw new IllegalStateException("The buffer is not in mapping.");
            }

            m_bInMapping = false;
        }

        gl.glBindBuffer(m_target, m_bufferID);
        gl.glUnmapBuffer(m_target);
    }

    public void bind(){gl.glBindBuffer(m_target, m_bufferID);}
    public void unbind(){gl.glBindBuffer(m_target, 0);}
    public int getTarget() { return m_target;}
    public int getUsage()  { return m_usage;}
    public int getBuffer() { return m_bufferID;}
    public int getBufferSize() { return m_bufferSize;}

    @Override
    public void dispose() {
        gl.glDeleteBuffer(m_bufferID);
        m_bufferID = 0;
    }
}
