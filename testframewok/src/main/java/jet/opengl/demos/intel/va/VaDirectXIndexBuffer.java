package jet.opengl.demos.intel.va;

import jet.opengl.postprocessing.buffer.BufferGL;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;

/**
 * Created by mazhen'gui on 2017/11/20.
 */

public class VaDirectXIndexBuffer extends VaDirectXBuffer  {
    protected VaDirectXIndexBuffer(int elementSize) {
        super(elementSize);

        if(elementSize != 1 && elementSize != 2 && elementSize !=3)
            throw new IllegalArgumentException("Inavliad element size for the index buffer: " + elementSize);
    }

    void Create(int elementCount, Object initializeData /*= NULL*/, int usage /*= D3D11_USAGE_DEFAULT, uint32 cpuAccessFlags = 0*/ ){
        Create( elementCount, /*D3D11_BIND_VERTEX_BUFFER*/ GLenum.GL_ELEMENT_ARRAY_BUFFER, initializeData, usage/*, cpuAccessFlags*/ );
    }

    void Create( int elementCount, int usage /*= D3D11_USAGE_DEFAULT, uint32 cpuAccessFlags = 0*/ ){
        Create( elementCount, /*D3D11_BIND_VERTEX_BUFFER*/GLenum.GL_ELEMENT_ARRAY_BUFFER, null, usage/*, cpuAccessFlags*/ );
    }
    void Create(int elementCount, BufferGL resource, int usage /*= D3D11_USAGE_DEFAULT, uint32 cpuAccessFlags = 0*/ ){
        Create( elementCount, resource, true, /*D3D11_BIND_VERTEX_BUFFER*/GLenum.GL_ELEMENT_ARRAY_BUFFER, usage/*, cpuAccessFlags*/ );
    }
    void Update( /*ID3D11DeviceContext * context,*/ int elementCount, Object data ){
        if(m_elementSize == 0)
            throw new IllegalArgumentException();
        m_buffer.update(0, cast(data, m_elementSize));
        m_buffer.unbind();
    }

    final void SetToD3DContext(){SetToD3DContext(0,0);}

    void SetToD3DContext( /*ID3D11DeviceContext * context,*/ int slot /*= 0*/, int offsetInBytes /*= 0*/ ){
        GLFuncProviderFactory.getGLFuncProvider().glBindBuffer(m_buffer.getTarget(), m_buffer.getBuffer());
    }
}
