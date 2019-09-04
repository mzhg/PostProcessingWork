package jet.opengl.renderer.Unreal4.views;

/**
 * Method used for second screen percentage method, that is a second spatial upscale pass at the
 * very end, independent of screen percentage show flag.
 */
public enum ESecondaryScreenPercentageMethod {
    // Helpful to work on aliasing issue on HighDPI monitors.
    NearestSpatialUpscale,

    // Upscale to simulate smaller pixel density on HighDPI monitors.
    LowerPixelDensitySimulation,
}
