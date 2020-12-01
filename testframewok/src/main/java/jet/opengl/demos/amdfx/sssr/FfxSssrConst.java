package jet.opengl.demos.amdfx.sssr;

public interface FfxSssrConst {
    /**
     FfxSssrCreateReflectionViewFlagBits::The available flags for creating a reflection view.
     */
    int
            FFX_SSSR_CREATE_REFLECTION_VIEW_FLAG_ENABLE_PERFORMANCE_COUNTERS    = 1 << 0, ///< Set this flag if the application wishes to retrieve timing results. Don't set this flag in release builds.
            FFX_SSSR_CREATE_REFLECTION_VIEW_FLAG_PING_PONG_NORMAL_BUFFERS = 1 << 1, ///< Set this flag if the application writes to alternate surfaces. Don't set this flag to signal that the application copies the provided normal surfaces each frame.
            FFX_SSSR_CREATE_REFLECTION_VIEW_FLAG_PING_PONG_ROUGHNESS_BUFFERS = 1 << 2; ///< Set this flag if the application writes to alternate surfaces. Don't set this flag to signal that the application copies the provided roughness surfaces each frame.

    /**
     FfxSssrResolveReflectionViewFlagBits::The available flags for resolving a reflection view.
     */
    int
            FFX_SSSR_RESOLVE_REFLECTION_VIEW_FLAG_DENOISE = 1 << 0, ///< Run denoiser passes on intersection results.
            FFX_SSSR_RESOLVE_REFLECTION_VIEW_FLAG_ENABLE_VARIANCE_GUIDED_TRACING = 1 << 1; ///< Enforces shooting a ray for temporally unstable pixels.

}
