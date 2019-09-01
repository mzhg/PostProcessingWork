package jet.opengl.renderer.Unreal4.api;

public interface ERenderTargetActions {

    int LoadOpMask = 2;

    static int RTACTION_MAKE_MASK(ERenderTargetLoadAction Load,ERenderTargetStoreAction  Store){
//        (((uint8)ERenderTargetLoadAction::Load << (uint8)LoadOpMask) | (uint8)ERenderTargetStoreAction::Store)
        return Load.ordinal() <<LoadOpMask | Store.ordinal();
    }

    int DontLoad_DontStore =	RTACTION_MAKE_MASK(ERenderTargetLoadAction.ENoAction, ERenderTargetStoreAction.ENoAction),

    DontLoad_Store =		RTACTION_MAKE_MASK(ERenderTargetLoadAction.ENoAction, ERenderTargetStoreAction.EStore),
            Clear_Store =			RTACTION_MAKE_MASK(ERenderTargetLoadAction.EClear, ERenderTargetStoreAction.EStore),
            Load_Store =			RTACTION_MAKE_MASK(ERenderTargetLoadAction.ELoad, ERenderTargetStoreAction.EStore),

    Clear_DontStore =		RTACTION_MAKE_MASK(ERenderTargetLoadAction.EClear, ERenderTargetStoreAction.ENoAction),
            Load_DontStore =		RTACTION_MAKE_MASK(ERenderTargetLoadAction.ELoad, ERenderTargetStoreAction.ENoAction),
            Clear_Resolve =			RTACTION_MAKE_MASK(ERenderTargetLoadAction.EClear, ERenderTargetStoreAction.EMultisampleResolve),
            Load_Resolve =			RTACTION_MAKE_MASK(ERenderTargetLoadAction.ELoad, ERenderTargetStoreAction.EMultisampleResolve);
}
