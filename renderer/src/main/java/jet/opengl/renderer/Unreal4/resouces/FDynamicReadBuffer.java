package jet.opengl.renderer.Unreal4.resouces;

import java.nio.ByteBuffer;

import jet.opengl.postprocessing.common.GLenum;

public class FDynamicReadBuffer extends FReadBuffer {
    /** Pointer to the vertex buffer mapped in main memory. */
    public ByteBuffer MappedBuffer;

    /** Default constructor. */
    FDynamicReadBuffer()
//		: MappedBuffer(nullptr)
    {
    }

    public void Initialize(int BytesPerElement, int NumElements, int Format, int AdditionalUsage/* = 0*/)
    {
        /*ensure(
                AdditionalUsage & (BUF_Dynamic | BUF_Volatile | BUF_Static) &&								// buffer should be Dynamic or Volatile or Static
                        (AdditionalUsage & (BUF_Dynamic | BUF_Volatile)) ^ (BUF_Dynamic | BUF_Volatile) // buffer should not be both
        );*/

        super.Initialize(BytesPerElement, NumElements, Format, AdditionalUsage);
    }

    /**
     * Locks the vertex buffer so it may be written to.
     */
    void Lock()
    {
//        check(MappedBuffer == nullptr);
//        check(IsValidRef(Buffer));
//        MappedBuffer = (uint8*)RHILockVertexBuffer(Buffer, 0, NumBytes, RLM_WriteOnly);
        MappedBuffer = Buffer.map(0, NumBytes, GLenum.GL_WRITE_ONLY);
    }

    /**
     * Unocks the buffer so the GPU may read from it.
     */
    void Unlock()
    {
//        check(MappedBuffer);
//        check(IsValidRef(Buffer));
//        RHIUnlockVertexBuffer(Buffer);
        Buffer.unmap();
        MappedBuffer = null;
    }
}
