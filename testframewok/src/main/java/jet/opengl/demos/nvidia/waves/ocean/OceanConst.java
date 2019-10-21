package jet.opengl.demos.nvidia.waves.ocean;

import jet.opengl.postprocessing.common.GLenum;

interface OceanConst {
    int MaxNumVessels =           1;
    int MaxNumSpotlights   =     11;

    int ENABLE_SHADOWS          =1;

    int ENABLE_GPU_SIMULATION   =1;
    int SPRAY_PARTICLE_SORTING  =1;

    int BitonicSortCSBlockSize  =512;

    int SPRAY_PARTICLE_COUNT    =(BitonicSortCSBlockSize * 256);

    int SprayParticlesCSBlocksSize =256;
    int SimulateSprayParticlesCSBlocksSize =256;

//#define ENABLE_SPRAY_PARTICLES  0

    float kSpotlightShadowResolution = 2048;

    int EmitParticlesCSBlocksSize =256;
    int SimulateParticlesCSBlocksSize =256;
    int PSMPropagationCSBlockSize =16;

    int TransposeCSBlockSize =16;

    String SHADER_PATH = "";

    int DXGI_FORMAT_R32G32B32A32_FLOAT = GLenum.GL_RGBA32F;

    // Ocean grid setting
    int BICOLOR_TEX_SIZE			= 256;

    int LOCAL_FOAMMAP_TEX_SIZE	    = 1024;
}
