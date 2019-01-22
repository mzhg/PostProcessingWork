package jet.opengl.demos.amdfx.geometry;

public class GeometryFX_FilterRenderOptions {
    /**
     If filtering is disabled, the mesh will be rendered directly.
     */
    public boolean enableFiltering = true;

    /**
     Specify which filters should be enabled.
     */
    public int enabledFilters = 0xFF;

    /**
     If set, statistics counters will be enabled.

     If enabled, queries will be issued along with each draw call significantly
     reducing performance.
     */
    public GeometryFX_FilterStatistics statistics;
}
