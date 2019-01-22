package jet.opengl.demos.amdfx.geometry;

public class GeometryFX_FilterDesc {

    // This is only used if filtering is disabled. If set to -1, it assumes
    // every mesh is drawn exactly once. If instancing is used, each instance
    // counts as a separate draw call.
    public int maximumDrawCallCount = -1;

    // Emulate indirect draw. If the extension is present, it will be not used.
    public boolean emulateMultiIndirectDraw = false;
}
