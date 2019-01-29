package jet.opengl.demos.intel.clustered;

interface ICONST {
    int
            CULL_FORWARD_NONE = 0,
            CULL_DEFERRED_NONE = 1,
            CULL_QUAD = 2,
            CULL_CLUSTERED = 3,
            CULL_COMPUTE_SHADER_TILE = 4;

    int MAX_LIGHTS_POWER  = 12;
    int MAX_LIGHTS = (1<<MAX_LIGHTS_POWER);

    // reduce maximum light list size per tile for better occupancy (more realistic performance)
    int MAX_SMEM_LIGHTS = 512;

    // This determines the tile size for light binning and associated tradeoffs
    int COMPUTE_SHADER_TILE_GROUP_DIM = 16;
    int COMPUTE_SHADER_TILE_GROUP_SIZE = (COMPUTE_SHADER_TILE_GROUP_DIM*COMPUTE_SHADER_TILE_GROUP_DIM);

    int LIGHT_GRID_TEXTURE_WIDTH = 1024;
    int LIGHT_GRID_TEXTURE_HEIGHT = 1024;
}
