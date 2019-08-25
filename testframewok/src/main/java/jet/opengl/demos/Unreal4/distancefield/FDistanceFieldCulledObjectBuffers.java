package jet.opengl.demos.Unreal4.distancefield;

import org.lwjgl.util.vector.Vector4f;

import jet.opengl.demos.Unreal4.TextureBuffer;
import jet.opengl.postprocessing.common.GLenum;

public class FDistanceFieldCulledObjectBuffers {
    public static int ObjectDataStride;
    public static int ObjectBoxBoundsStride;

    public boolean bWantBoxBounds;
    public int MaxObjects;

    public final TextureBuffer ObjectIndirectArguments = new TextureBuffer();
    public final TextureBuffer ObjectIndirectDispatch = new TextureBuffer();
    public final TextureBuffer Bounds = new TextureBuffer();
    public final TextureBuffer Data = new TextureBuffer();
    public final TextureBuffer BoxBounds = new TextureBuffer();

    public FDistanceFieldCulledObjectBuffers()
    {
        MaxObjects = 0;
        bWantBoxBounds = false;
    }

    void Initialize()
    {
        if (MaxObjects > 0)
        {
			final int FastVRamFlag = 0 ;//GFastVRamConfig.DistanceFieldCulledObjectBuffers | ( IsTransientResourceBufferAliasingEnabled() ? BUF_Transient : BUF_None );

            final int PF_R32_UINT = GLenum.GL_R32UI;
            ObjectIndirectArguments.Initialize(/*sizeof(uint32)*/4, 5, PF_R32_UINT, 0/*BUF_Static | BUF_DrawIndirect*/);
            ObjectIndirectDispatch.Initialize(/*sizeof(uint32)*/4, 3, PF_R32_UINT, 0/*BUF_Static | BUF_DrawIndirect*/);
            Bounds.Initialize(/*sizeof(FVector4)*/Vector4f.SIZE, MaxObjects, GLenum.GL_RGBA32F/*BUF_Static | FastVRamFlag, TEXT("FDistanceFieldCulledObjectBuffers::Bounds")*/);
            Data.Initialize(/*sizeof(FVector4)*/Vector4f.SIZE, MaxObjects * ObjectDataStride,GLenum.GL_RGBA32F/* BUF_Static | FastVRamFlag, TEXT("FDistanceFieldCulledObjectBuffers::Data")*/);

            if (bWantBoxBounds)
            {
                BoxBounds.Initialize(/*sizeof(FVector4)*/Vector4f.SIZE, MaxObjects * ObjectBoxBoundsStride, GLenum.GL_RGBA32F/*BUF_Static | FastVRamFlag, TEXT("FDistanceFieldCulledObjectBuffers::BoxBounds")*/);
            }
        }
    }

    /*void AcquireTransientResource()
    {
        Bounds.AcquireTransientResource();
        Data.AcquireTransientResource();
        if (bWantBoxBounds)
        {
            BoxBounds.AcquireTransientResource();
        }
    }

    void DiscardTransientResource()
    {
        Bounds.DiscardTransientResource();
        Data.DiscardTransientResource();
        if (bWantBoxBounds)
        {
            BoxBounds.DiscardTransientResource();
        }
    }*/

    public void Release()
    {
        ObjectIndirectArguments.Release();
        ObjectIndirectDispatch.Release();
        Bounds.Release();
        Data.Release();
        BoxBounds.Release();
    }

    public int GetSizeBytes()
    {
        return ObjectIndirectArguments.NumBytes + ObjectIndirectDispatch.NumBytes + Bounds.NumBytes + Data.NumBytes + BoxBounds.NumBytes;
    }
}
