package jet.opengl.demos.intel.coarse;

interface LightCullTechnique {
    int
    CULL_FORWARD_NONE =0,
    CULL_FORWARD_PREZ_NONE=1,
    CULL_DEFERRED_NONE=2,
    CULL_QUAD=3,
    CULL_QUAD_DEFERRED_LIGHTING=4,
    CULL_COMPUTE_SHADER_TILE=5;
}
