package jet.opengl.demos.nvidia.waves.crest;

interface Wave_Const {
    int MAX_LOD_COUNT = 15;

    // NOTE: these MUST match the values in OceanLODData.hlsl
    // 64 recommended as a good common minimum: https://www.reddit.com/r/GraphicsProgramming/comments/aeyfkh/for_compute_shaders_is_there_an_ideal_numthreads/
     int THREAD_GROUP_SIZE_X = 8;
     int THREAD_GROUP_SIZE_Y = 8;
}
