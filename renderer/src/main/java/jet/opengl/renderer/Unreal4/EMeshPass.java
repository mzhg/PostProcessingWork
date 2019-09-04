package jet.opengl.renderer.Unreal4;

public enum EMeshPass {
    SkyPass,
    CSMShadowDepth,
    Distortion,
    Velocity,
    TranslucencyStandard,
    TranslucencyAfterDOF,
    TranslucencyAll, /** Drawing all translucency, regardless of separate or standard.  Used when drawing translucency outside of the main renderer, eg FRendererModule::DrawTile. */
    LightmapDensity,
    DebugViewMode, /** Any of EDebugViewShaderMode */
    CustomDepth,
    MobileBasePassCSM,  /** Mobile base pass with CSM shading enabled */
    MobileInverseOpacity,  /** Mobile specific scene capture, Non-cached */
    VirtualTexture,

        HitProxy,
        HitProxyOpaqueOnly,
        EditorSelection,
}
