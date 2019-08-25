package jet.opengl.demos.Unreal4.distancefield;

import jet.opengl.demos.Unreal4.TextureBuffer;
import jet.opengl.postprocessing.common.GLenum;

final class FObjectGridBuffers {
    static final int GMaxGridCulledObjects = 2048;

    int GridDimension;
    boolean b16BitIndices;
    final TextureBuffer CullGridObjectNum = new TextureBuffer();
    final TextureBuffer CullGridObjectArray = new TextureBuffer();

    FObjectGridBuffers()
    {
        GridDimension = 0;
    }

    void InitDynamicRHI()
    {
        if (GridDimension > 0)
        {
//			const uint32 FastVRamFlag = GFastVRamConfig.GlobalDistanceFieldCullGridBuffers | (IsTransientResourceBufferAliasingEnabled() ? BUF_Transient : BUF_None);
			final int TileNum = GridDimension * GridDimension * GridDimension;
            final int PF_R32_UINT = GLenum.GL_R32UI;
            final int PF_R16_UINT = GLenum.GL_R16UI;

            CullGridObjectNum.Initialize(
                    /*sizeof(uint32)*/4,
                    TileNum,
                    PF_R32_UINT/*,
                    BUF_Static | FastVRamFlag,
                    TEXT("GlobalDistanceField::TileObjectNum")*/);

            CullGridObjectArray.Initialize(
                    b16BitIndices ? /*sizeof(uint16)*/ 2: /*sizeof(uint32)*/4,
                    TileNum * GMaxGridCulledObjects,
                    b16BitIndices ? PF_R16_UINT : PF_R32_UINT/*,
                    BUF_Static | FastVRamFlag,
                    TEXT("GlobalDistanceField::TileObjectArray")*/);
        }
    }

    void AcquireTransientResource()
    {
//        CullGridObjectNum.AcquireTransientResource();
//        CullGridObjectArray.AcquireTransientResource();
    }

    void DiscardTransientResource()
    {
//        CullGridObjectNum.DiscardTransientResource();
//        CullGridObjectArray.DiscardTransientResource();
    }

    void ReleaseDynamicRHI()
    {
        CullGridObjectNum.Release();
        CullGridObjectArray.Release();
    }

    int GetSizeBytes()
    {
        return CullGridObjectNum.NumBytes + CullGridObjectArray.NumBytes;
    }
}
