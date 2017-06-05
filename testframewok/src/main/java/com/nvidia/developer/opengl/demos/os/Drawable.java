package com.nvidia.developer.opengl.demos.os;

import jet.opengl.postprocessing.buffer.VertexArrayObject;
import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.texture.Texture2D;

/**
 * Created by mazhen'gui on 2017/6/5.
 */

final class Drawable {
    final Transform transform = new Transform();
    GLSLProgram program;
    Texture2D texture;
    VertexArrayObject buffer;
}
