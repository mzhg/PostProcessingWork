package jet.opengl.renderer.Unreal4.resouces;

import jet.opengl.renderer.Unreal4.TextureBuffer;

/** Encapsulates a GPU read buffer with its SRV. */
public class FReadBuffer {
    public TextureBuffer Buffer;
//    FShaderResourceViewRHIRef SRV;
    public int NumBytes;

    public FReadBuffer()/*: NumBytes(0)*/ {}

    public void Initialize(int BytesPerElement, int NumElements, int Format, int AdditionalUsage /*= 0*/)
    {
//        check(GSupportsResourceView);
        NumBytes = BytesPerElement * NumElements;

//        FRHIResourceCreateInfo CreateInfo;
//        Buffer = RHICreateVertexBuffer(NumBytes, BUF_ShaderResource | AdditionalUsage, CreateInfo);
//        SRV = RHICreateShaderResourceView(Buffer, BytesPerElement, Format);
        Buffer = new TextureBuffer();
        Buffer.Initialize(BytesPerElement, NumElements, Format, AdditionalUsage);
    }

    public void Release()
    {
        NumBytes = 0;
        Buffer.Release();
//        SRV.SafeRelease();
    }
}
