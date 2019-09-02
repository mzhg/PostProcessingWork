package jet.opengl.renderer.Unreal4.scenes;

/** Method used for primary screen percentage method. */
public enum EPrimaryScreenPercentageMethod {
    // Add spatial upscale pass at the end of post processing chain, before the secondary upscale.
    SpatialUpscale,

    // Let temporal AA's do the upscale.
    TemporalUpscale,

    // No upscaling or up sampling, just output the view rect smaller.
    // This is useful for VR's render thread dynamic resolution with MSAA.
    RawOutput,
}
