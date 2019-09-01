package jet.opengl.renderer.Unreal4.api;

// Hints for some RHIs that support subpasses
public enum ESubpassHint {
    // Regular rendering
    None,

    // Render pass has depth reading subpass
    DepthReadSubpass,
}
