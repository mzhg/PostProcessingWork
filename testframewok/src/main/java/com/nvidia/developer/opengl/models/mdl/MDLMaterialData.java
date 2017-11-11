package com.nvidia.developer.opengl.models.mdl;

import java.util.ArrayList;
import java.util.List;

import jet.opengl.postprocessing.buffer.BufferGL;
import jet.opengl.postprocessing.shader.GLSLProgram;

/**
 * Created by mazhen'gui on 2017/11/11.
 */

final class MDLMaterialData {
    final List<TextureData> textures = new ArrayList<>();
    final List<BufferGL> buffers = new ArrayList<>();
    int[] buffer_slots;
    GLSLProgram program;
}
