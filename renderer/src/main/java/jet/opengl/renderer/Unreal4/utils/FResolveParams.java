package jet.opengl.renderer.Unreal4.utils;

public class FResolveParams {
    /** used to specify face when resolving to a cube map texture */
    public int CubeFace;
    /** resolve RECT bounded by [X1,Y1]..[X2,Y2]. Or -1 for fullscreen */
    public final FResolveRect Rect = new FResolveRect();
    public final FResolveRect DestRect = new FResolveRect();
    /** The mip index to resolve in both source and dest. */
    public int MipIndex;
    /** Array index to resolve in the source. */
    public int SourceArrayIndex;
    /** Array index to resolve in the dest. */
    public int DestArrayIndex;

    /** constructor */
    /*FResolveParams(
		const FResolveRect& InRect = FResolveRect(),
    ECubeFace InCubeFace = CubeFace_PosX,
    int32 InMipIndex = 0,
    int32 InSourceArrayIndex = 0,
    int32 InDestArrayIndex = 0,
		const FResolveRect& InDestRect = FResolveRect())
            :	CubeFace(InCubeFace)
		,	Rect(InRect)
		,	DestRect(InDestRect)
		,	MipIndex(InMipIndex)
		,	SourceArrayIndex(InSourceArrayIndex)
		,	DestArrayIndex(InDestArrayIndex)
    {}*/

    public FResolveParams(){
        CubeFace = 0;
        MipIndex = 0;
        SourceArrayIndex = 0;
        DestArrayIndex = 0;
    }

    public FResolveParams(FResolveRect InRect){
        CubeFace = 0;
        Rect.Set(InRect);
        DestRect.Reset();
        MipIndex = 0;
        SourceArrayIndex = 0;
        DestArrayIndex = 0;
    }

    public FResolveParams(FResolveRect InRect, int InCubeFace){
        CubeFace = InCubeFace;
        Rect.Set(InRect);
        DestRect.Reset();
        MipIndex = 0;
        SourceArrayIndex = 0;
        DestArrayIndex = 0;
    }

    public FResolveParams(FResolveRect InRect, int InCubeFace, int InMipIndex){
        CubeFace = InCubeFace;
        Rect.Set(InRect);
        DestRect.Reset();
        MipIndex = InMipIndex;
        SourceArrayIndex = 0;
        DestArrayIndex = 0;
    }

    public FResolveParams(FResolveRect InRect, int InCubeFace, int InMipIndex, int InSourceArrayIndex){
        CubeFace = InCubeFace;
        Rect.Set(InRect);
        DestRect.Reset();
        MipIndex = InMipIndex;
        SourceArrayIndex = InSourceArrayIndex;
        DestArrayIndex = 0;
    }

    public FResolveParams(FResolveRect InRect, int InCubeFace, int InMipIndex, int InSourceArrayIndex, int InDestArrayIndex){
        CubeFace = InCubeFace;
        Rect.Set(InRect);
        DestRect.Reset();
        MipIndex = InMipIndex;
        SourceArrayIndex = InSourceArrayIndex;
        DestArrayIndex = InDestArrayIndex;
    }

    public FResolveParams(FResolveRect InRect, int InCubeFace, int InMipIndex, int InSourceArrayIndex, int InDestArrayIndex,
                          FResolveRect InDestRect){
        CubeFace = InCubeFace;
        Rect.Set(InRect);
        DestRect.Set(InDestRect);
        MipIndex = InMipIndex;
        SourceArrayIndex = InSourceArrayIndex;
        DestArrayIndex = InDestArrayIndex;
    }

    public FResolveParams(FResolveParams Other)/*
		: CubeFace(Other.CubeFace)
		, Rect(Other.Rect)
		, DestRect(Other.DestRect)
		, MipIndex(Other.MipIndex)
		, SourceArrayIndex(Other.SourceArrayIndex)
		, DestArrayIndex(Other.DestArrayIndex)*/
    {
        CubeFace = Other.CubeFace;
        Rect.Set(Other.Rect);
        DestRect.Set(Other.DestRect);
        MipIndex = Other.MipIndex;
        SourceArrayIndex = (Other.SourceArrayIndex);
        DestArrayIndex = (Other.DestArrayIndex);
    }
}
