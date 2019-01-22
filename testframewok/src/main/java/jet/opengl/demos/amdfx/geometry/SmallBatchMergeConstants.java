package jet.opengl.demos.amdfx.geometry;

interface SmallBatchMergeConstants {
    // If this changes, the shaders have to be recompiled as well
    int BATCH_SIZE = 4 * 64; // Should be a multiple of the wavefront size
    int BATCH_COUNT = 1 * 384;
}
