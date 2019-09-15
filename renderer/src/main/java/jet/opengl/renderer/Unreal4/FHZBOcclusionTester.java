package jet.opengl.renderer.Unreal4;

import java.nio.ByteBuffer;
import java.util.ArrayList;

import jet.opengl.postprocessing.texture.TextureGL;

public class FHZBOcclusionTester {

    private static final int SizeX = 256;
    private static final int SizeY = 256;
    private static final int FrameNumberMask = 0x7fffffff;
    private static final int InvalidFrameNumber = 0xffffffff;

    private final ArrayList< FOcclusionPrimitive > Primitives = new ArrayList<>();

    private TextureGL ResultsTextureCPU;
	private ByteBuffer ResultsBuffer;


    bool IsInvalidFrame() const;

    // set ValidFrameNumber to a number that cannot be set by SetValidFrameNumber so IsValidFrame will return false for any frame number
    void SetInvalidFrameNumber();

    private int ValidFrameNumber;

    FHZBOcclusionTester(){

    }

    // FRenderResource interface
    void	InitDynamicRHI();
    void	ReleaseDynamicRHI();

    public int			GetNum() { return Primitives.size(); }

    uint32			AddBounds( const FVector& BoundsOrigin, const FVector& BoundsExtent );
    void			Submit(FRHICommandListImmediate& RHICmdList, const FViewInfo2& View);

    void			MapResults(FRHICommandListImmediate& RHICmdList);
    void			UnmapResults(FRHICommandListImmediate& RHICmdList);
    bool			IsVisible( uint32 Index ) const;

    bool IsValidFrame(uint32 FrameNumber) const;

    void SetValidFrameNumber(uint32 FrameNumber);


}
