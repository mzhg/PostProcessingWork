package jet.opengl.renderer.Unreal4.api;

public interface EDepthStencilTargetActions{
    int DepthMask = 4;

    static int // RTACTION_MAKE_MASK(Depth, Stencil) (((uint8)ERenderTargetActions::Depth << (uint8)DepthMask) | (uint8)ERenderTargetActions::Stencil)
            RTACTION_MAKE_MASK(int Depth, int Stencil){
        return  (Depth << DepthMask) | Stencil;
    }

    int
            DontLoad_DontStore =						RTACTION_MAKE_MASK(ERenderTargetActions.DontLoad_DontStore, ERenderTargetActions.DontLoad_DontStore),
            DontLoad_StoreDepthStencil =				RTACTION_MAKE_MASK(ERenderTargetActions.DontLoad_Store, ERenderTargetActions.DontLoad_Store),
            DontLoad_StoreStencilNotDepth =				RTACTION_MAKE_MASK(ERenderTargetActions.DontLoad_DontStore, ERenderTargetActions.DontLoad_Store),
            ClearDepthStencil_StoreDepthStencil =		RTACTION_MAKE_MASK(ERenderTargetActions.Clear_Store, ERenderTargetActions.Clear_Store),
            LoadDepthStencil_StoreDepthStencil =		RTACTION_MAKE_MASK(ERenderTargetActions.Load_Store, ERenderTargetActions.Load_Store),
            LoadDepthNotStencil_DontStore =				RTACTION_MAKE_MASK(ERenderTargetActions.Load_DontStore, ERenderTargetActions.DontLoad_DontStore),
            LoadDepthStencil_StoreStencilNotDepth =		RTACTION_MAKE_MASK(ERenderTargetActions.Load_DontStore, ERenderTargetActions.Load_Store),

    ClearDepthStencil_DontStoreDepthStencil =	RTACTION_MAKE_MASK(ERenderTargetActions.Clear_DontStore, ERenderTargetActions.Clear_DontStore),
            LoadDepthStencil_DontStoreDepthStencil =	RTACTION_MAKE_MASK(ERenderTargetActions.Load_DontStore, ERenderTargetActions.Load_DontStore),
            ClearDepthStencil_StoreDepthNotStencil =	RTACTION_MAKE_MASK(ERenderTargetActions.Clear_Store, ERenderTargetActions.Clear_DontStore),
            ClearDepthStencil_StoreStencilNotDepth =	RTACTION_MAKE_MASK(ERenderTargetActions.Clear_DontStore, ERenderTargetActions.Clear_Store),
            ClearDepthStencil_ResolveDepthNotStencil =	RTACTION_MAKE_MASK(ERenderTargetActions.Clear_Resolve, ERenderTargetActions.Clear_DontStore),
            ClearDepthStencil_ResolveStencilNotDepth =	RTACTION_MAKE_MASK(ERenderTargetActions.Clear_DontStore, ERenderTargetActions.Clear_Resolve),

    ClearStencilDontLoadDepth_StoreStencilNotDepth = RTACTION_MAKE_MASK(ERenderTargetActions.DontLoad_DontStore, ERenderTargetActions.Clear_Store);
}
