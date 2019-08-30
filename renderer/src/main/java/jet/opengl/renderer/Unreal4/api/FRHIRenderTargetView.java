package jet.opengl.renderer.Unreal4.api;

import jet.opengl.postprocessing.texture.TextureGL;

public class FRHIRenderTargetView {
    public TextureGL Texture;
    public int MipIndex;

    /** Array slice or texture cube face.  Only valid if texture resource was created with TexCreate_TargetArraySlicesIndependently! */
    public int ArraySliceIndex = -1;

    public ERenderTargetLoadAction LoadAction = ERenderTargetLoadAction.ENoAction;
    public ERenderTargetStoreAction StoreAction = ERenderTargetStoreAction.ENoAction;


    public FRHIRenderTargetView(FRHIRenderTargetView Other) /*:
    Texture(Other.Texture),
    MipIndex(Other.MipIndex),
    ArraySliceIndex(Other.ArraySliceIndex),
    LoadAction(Other.LoadAction),
    StoreAction(Other.StoreAction)*/
    {
        Texture = Other.Texture;
        MipIndex = Other.MipIndex;
        ArraySliceIndex = Other.ArraySliceIndex;
        LoadAction = Other.LoadAction;
        StoreAction = Other.StoreAction;
    }

    //common case
    public FRHIRenderTargetView(TextureGL InTexture, ERenderTargetLoadAction InLoadAction) /*:
    Texture(InTexture),
    MipIndex(0),
    ArraySliceIndex(-1),
    LoadAction(InLoadAction),
    StoreAction(ERenderTargetStoreAction::EStore)*/
    {
        Texture = InTexture;
        MipIndex = 0;
        ArraySliceIndex = -1;
        LoadAction = InLoadAction;
        StoreAction = ERenderTargetStoreAction.EStore;
    }

    //common case
    public FRHIRenderTargetView(TextureGL InTexture, ERenderTargetLoadAction InLoadAction, int InMipIndex, int InArraySliceIndex) /*:
    Texture(InTexture),
    MipIndex(InMipIndex),
    ArraySliceIndex(InArraySliceIndex),
    LoadAction(InLoadAction),
    StoreAction(ERenderTargetStoreAction::EStore)*/
    {
        Texture = InTexture;
        MipIndex = InMipIndex;
        ArraySliceIndex = InArraySliceIndex;
        LoadAction = InLoadAction;
        StoreAction = ERenderTargetStoreAction.EStore;
    }

    public FRHIRenderTargetView(TextureGL InTexture, int InMipIndex, int InArraySliceIndex, ERenderTargetLoadAction InLoadAction, ERenderTargetStoreAction InStoreAction)
    /*Texture(InTexture),
    MipIndex(InMipIndex),
    ArraySliceIndex(InArraySliceIndex),
    LoadAction(InLoadAction),
    StoreAction(InStoreAction)*/
    {
        Texture = InTexture;
        MipIndex = InMipIndex;
        ArraySliceIndex = InArraySliceIndex;
        LoadAction = InLoadAction;
        StoreAction = InStoreAction;
    }

    /*bool operator==(const FRHIRenderTargetView& Other) const
    {
        return
                Texture == Other.Texture &&
                        MipIndex == Other.MipIndex &&
                        ArraySliceIndex == Other.ArraySliceIndex &&
                        LoadAction == Other.LoadAction &&
                        StoreAction == Other.StoreAction;
    }*/

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FRHIRenderTargetView that = (FRHIRenderTargetView) o;

        if (MipIndex != that.MipIndex) return false;
        if (ArraySliceIndex != that.ArraySliceIndex) return false;
        if (Texture != null ? !Texture.equals(that.Texture) : that.Texture != null) return false;
        if (LoadAction != that.LoadAction) return false;
        return StoreAction == that.StoreAction;
    }

    @Override
    public int hashCode() {
        int result = Texture != null ? Texture.hashCode() : 0;
        result = 31 * result + MipIndex;
        result = 31 * result + ArraySliceIndex;
        result = 31 * result + (LoadAction != null ? LoadAction.hashCode() : 0);
        result = 31 * result + (StoreAction != null ? StoreAction.hashCode() : 0);
        return result;
    }
}
