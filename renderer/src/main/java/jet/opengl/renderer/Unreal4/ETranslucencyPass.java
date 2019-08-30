package jet.opengl.renderer.Unreal4;

public interface ETranslucencyPass {
    int
    TPT_StandardTranslucency = 0,
    TPT_TranslucencyAfterDOF = 1,

    /** Drawing all translucency, regardless of separate or standard.  Used when drawing translucency outside of the main renderer, eg FRendererModule::DrawTile. */
    TPT_AllTranslucency = 2,
    TPT_MAX = 3;
}
