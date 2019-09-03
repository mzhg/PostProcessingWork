package jet.opengl.renderer.Unreal4.editor;

public interface MetaHint {
    int HideAlphaChannel = 1,
            LegacyTonemapper = 1 << 1,
            PinHiddenByDefault = 1 << 2,
            InlineEditConditionToggle = 1 << 3;

}
