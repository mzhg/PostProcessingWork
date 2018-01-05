package jet.opengl.demos.intel.va;

import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;

/**
 * Created by mazhen'gui on 2017/11/20.
 */

public class VaDirectXConstantsBuffer extends VaDirectXBuffer {
    public VaDirectXConstantsBuffer(int elementSize) {
        super(elementSize);
    }

    public final void Create(){
        Create(null);
    }

    public void Create( Object initializeData /*= NULL*/ ){
        Create( 1, /*D3D11_BIND_CONSTANT_BUFFER*/GLenum.GL_UNIFORM_BUFFER, initializeData, /*D3D11_USAGE_DEFAULT*/GLenum.GL_STREAM_DRAW );
    }

    public void Update( /*ID3D11DeviceContext * context,*/ Object data ){
        if(m_elementSize == 0)
            throw new IllegalArgumentException();
        m_buffer.update(0, cast(data, m_elementSize));
    }

    public void SetToD3DContextAllShaderTypes( /*ID3D11DeviceContext * context,*/ int slot /*= DefaultSlot*/ ){
        GLFuncProviderFactory.getGLFuncProvider().glBindBufferBase(m_buffer.getTarget(), slot, m_buffer.getBuffer());
    }
}
