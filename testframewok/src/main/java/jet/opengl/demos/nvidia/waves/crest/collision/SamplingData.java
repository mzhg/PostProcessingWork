package jet.opengl.demos.nvidia.waves.crest.collision;

/** Sampling state used to speed up queries. */
public class SamplingData {
    // Tag is only used by displacement texture readback. In the future this class could be removed completely and replaced with
    // just the min spatial length float
    public Object _tag = null;
    public float _minSpatialLength = -1f;
}
