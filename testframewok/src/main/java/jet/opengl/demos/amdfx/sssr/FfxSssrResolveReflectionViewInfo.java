package jet.opengl.demos.amdfx.sssr;

/**
 The parameters for resolving a reflection view.
 */
public class FfxSssrResolveReflectionViewInfo {
    public int flags;
    public float temporalStabilityScale; ///< Value between 0 and 1. High values prioritize temporal stability wheras low values avoid ghosting.
    public int maxTraversalIterations; ///< Maximum number of iterations to find the intersection with the depth buffer.
    public int mostDetailedDepthHierarchyMipLevel; ///< Applies only to non-mirror reflections. Mirror reflections always use 0 as most detailed mip.
    public int minTraversalOccupancy; ///< Minimum number of threads per wave to keep the intersection kernel running.
    public float depthBufferThickness; ///< Unit in view space. Any intersections further behind the depth buffer are rejected as invalid hits.
    public FfxSssrRaySamplesPerQuad samplesPerQuad; ///< Number of samples per 4 pixels in denoised regions. Mirror reflections are not affected by this.
    public float roughnessThreshold; ///< Shoot reflection rays for roughness values that are lower than this threshold.
}
