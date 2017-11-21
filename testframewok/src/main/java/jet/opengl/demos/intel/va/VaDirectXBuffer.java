package jet.opengl.demos.intel.va;

import org.lwjgl.util.vector.Readable;

import java.nio.Buffer;

import jet.opengl.postprocessing.buffer.BufferGL;
import jet.opengl.postprocessing.common.Disposeable;
import jet.opengl.postprocessing.common.GLCheck;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.util.CacheBuffer;

/**
 * Created by mazhen'gui on 2017/11/20.
 */

public class VaDirectXBuffer implements Disposeable {
    protected BufferGL m_buffer;
    protected int      m_elementCount;
    protected final int   m_elementSize;

    public VaDirectXBuffer(int elementSize){
        m_elementSize = elementSize;
    }

    @Override
    public void dispose() {
        if(m_buffer != null){
            m_buffer.dispose();
            m_buffer = null;
        }
    }

    protected static Buffer cast(Object obj, int elementSize){
        if(obj == null)
            return null;

        if(obj instanceof  Buffer){
            return (Buffer)obj;
        }else if(obj instanceof Readable){
            return CacheBuffer.wrap(elementSize, (Readable)obj);
        }else if(obj instanceof Readable[]){
            return CacheBuffer.wrap(elementSize, (Readable[])obj);
        }else{
            return CacheBuffer.wrapPrimitiveArray(obj);
        }
    }

    public int GetTarget() { return m_buffer.getTarget();}

    public void Create(int elementCount, int target, Object initializeData, int usage /*= D3D11_USAGE_DEFAULT*//*, uint32 cpuAccessFlags = 0, uint32 miscFlags = 0 */){
        assert( m_buffer == null );
        SAFE_RELEASE( m_buffer );

        m_elementCount = elementCount;
//        m_buffer = vaDirectXTools::CreateBuffer( elementCount * sizeof(ElementType), bindFlags, usage, cpuAccessFlags, miscFlags, 0, initializeData );
        m_buffer = new BufferGL();
        m_buffer.initlize(target, elementCount * m_elementSize, cast(initializeData, m_elementSize), usage);
        m_buffer.unbind();
    }


    public void Create( int elementCount, BufferGL resource, boolean verify, int bindFlags, int usage /*= D3D11_USAGE_DEFAULT, uint32 cpuAccessFlags = 0, uint32 miscFlags = 0*/ ){
        dispose();
        m_buffer = resource;
        m_elementCount = elementCount;
        if(verify){
            // todo
            /*D3D11_BUFFER_DESC desc;
            m_buffer->GetDesc( &desc );
            assert( desc.Usage == usage );
            assert( desc.CPUAccessFlags == cpuAccessFlags );
            assert( desc.MiscFlags == miscFlags );
            int expectedSize = sizeof(ElementType) * elementCount;*/
        }

        throw new UnsupportedOperationException();
    }

    public int                          GetElementCount( )            { return m_elementCount; }
    public int                          GetSizeInBytes( )             { return m_elementCount * /*sizeof(ElementType)*/m_elementSize; }
    public BufferGL                     GetBuffer()                   { return m_buffer; }
//    operator ID3D11Buffer *const * ()   { return &m_buffer; }

//    std::shared_ptr<vaDirectXBuffer<ElementType>>
    public VaDirectXBuffer CreateStagingCopy( /*ID3D11DeviceContext * copyContext*/ ){
        if( m_buffer == null )
//            return std::shared_ptr<vaDirectXBuffer<ElementType>>( NULL );
            return null;

//        std::shared_ptr<vaDirectXBuffer<ElementType>> newBuffer ( new vaDirectXBuffer<ElementType>( ) );
        VaDirectXBuffer newBuffer = new VaDirectXBuffer(m_elementSize);
        newBuffer.Create( m_elementCount, newBuffer.GetTarget(), null, GLenum.GL_DYNAMIC_READ/*, D3D11_CPU_ACCESS_READ*/ );

//        copyContext->CopyResource( newBuffer->GetBuffer(), m_buffer );
        GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
        gl.glBindBuffer(GLenum.GL_COPY_READ_BUFFER, m_buffer.getBuffer());
        gl.glBindBuffer(GLenum.GL_COPY_WRITE_BUFFER, newBuffer.m_buffer.getBuffer());
        gl.glCopyBufferSubData(GLenum.GL_COPY_READ_BUFFER, GLenum.GL_COPY_WRITE_BUFFER,0,0, m_elementCount * m_elementSize);
        gl.glBindBuffer(GLenum.GL_COPY_READ_BUFFER, 0);
        gl.glBindBuffer(GLenum.GL_COPY_WRITE_BUFFER, 0);

        if(GLCheck.CHECK)
            GLCheck.checkError();

        return newBuffer;
    }
}
