package nv.samples.culling;

import jet.opengl.postprocessing.shader.GLSLProgram;

final class Programs {
    GLSLProgram object_frustum;
    GLSLProgram  object_hiz;
    GLSLProgram  object_raster;

    GLSLProgram  bit_temporallast;
    GLSLProgram  bit_temporalnew;
    GLSLProgram  bit_regular;
    GLSLProgram  depth_mips;
}
