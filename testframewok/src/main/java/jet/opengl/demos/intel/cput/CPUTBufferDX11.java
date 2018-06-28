package jet.opengl.demos.intel.cput;

import java.nio.ByteBuffer;

import jet.opengl.postprocessing.buffer.BufferGL;
import jet.opengl.postprocessing.texture.Texture2D;

/**
 * Created by mazhen'gui on 2018/1/15.
 */

public class CPUTBufferDX11 extends CPUTBuffer {
    private BufferGL mpBuffer;
    private Texture2D mpTexture;

    public CPUTBufferDX11(){}

    public CPUTBufferDX11(String name, BufferGL pBuffer){
        mpBuffer = pBuffer;
        if(mpBuffer != null){
            mpBuffer.setName(name);
        }
    }

    public CPUTBufferDX11(String name, Texture2D view){
        mpTexture = view;
        if(view != null){
            view.setName(name);
        }
    }

    public CPUTBufferDX11(String name, BufferGL pBuffer, Texture2D view){
        mpBuffer = pBuffer;
        mpTexture = view;
        if(view != null){
            view.setName(name);
        }
    }

    public Texture2D GetShaderResourceView()
    {
        return mpTexture;
    }

    public Texture2D GetUnorderedAccessView()
    {
        return mpTexture;
    }

    public void SetShaderResourceView(BufferGL pShaderResourceView)
    {
        mpBuffer = pShaderResourceView;
    }

    public void SetShaderResourceView(Texture2D pShaderResourceView)
    {
        mpTexture = pShaderResourceView;
    }

    public void SetUnorderedAccessView(Texture2D pUnorderedAccessView)
    {
        mpTexture = pUnorderedAccessView;
    }

    public void SetBufferAndViews(BufferGL pBuffer, /*ID3D11ShaderResourceView *pShaderResourceView,*/ Texture2D pUnorderedAccessView )
    {
        mpBuffer = pBuffer;
        mpTexture = pUnorderedAccessView;
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
