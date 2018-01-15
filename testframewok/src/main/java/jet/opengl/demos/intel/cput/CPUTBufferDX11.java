package jet.opengl.demos.intel.cput;

import java.nio.ByteBuffer;

import jet.opengl.postprocessing.buffer.BufferGL;

/**
 * Created by mazhen'gui on 2018/1/15.
 */

public class CPUTBufferDX11 extends CPUTBuffer {
    private BufferGL mpBuffer;

    public CPUTBufferDX11(){}

    public CPUTBufferDX11(String name, BufferGL pBuffer){
        mpBuffer = pBuffer;
        if(mpBuffer != null){
            mpBuffer.setName(name);
        }
    }

    public BufferGL GetShaderResourceView()
    {
        return mpBuffer;
    }

    public BufferGL GetUnorderedAccessView()
    {
        return mpBuffer;
    }

    public void SetShaderResourceView(BufferGL pShaderResourceView)
    {
        mpBuffer = pShaderResourceView;
    }

    public void SetUnorderedAccessView(BufferGL pUnorderedAccessView)
    {
        mpBuffer = pUnorderedAccessView;
    }

    public void SetBufferAndViews(BufferGL pBuffer/*, ID3D11ShaderResourceView *pShaderResourceView, ID3D11UnorderedAccessView *pUnorderedAccessView*/ )
    {
        mpBuffer = pBuffer;
    }

    public BufferGL GetNativeBuffer() { return mpBuffer; }
    public ByteBuffer MapBuffer(CPUTRenderParameters params, CPUTMapType type, boolean wait/*=true*/ ){
        throw new UnsupportedOperationException();
    }

    public void UnmapBuffer( CPUTRenderParameters params ){
        throw new UnsupportedOperationException();
    }

    public void ReleaseBuffer()
    {
        SAFE_RELEASE(mpBuffer);
    }

    @Override
    public String getName() {
        return mpBuffer != null ? mpBuffer.getName() : null;
    }

    @Override
    public void dispose() {
        SAFE_RELEASE(mpBuffer);
        mpBuffer = null;
    }
}
