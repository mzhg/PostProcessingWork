package com.nvidia.developer.opengl.demos.os;

import org.lwjgl.util.vector.Matrix4f;

import java.io.IOException;

import jet.opengl.postprocessing.shader.AttribBinder;
import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.util.CacheBuffer;

/**
 * Created by mazhen'gui on 2017/6/5.
 */

class SingleTextureProgram extends GLSLProgram{
    int uniformMVP = -1;

    SingleTextureProgram(){
        setAttribBinding(new AttribBinder("aPosition", 0), new AttribBinder("aTexCoord", 1));
        try {
            setSourceFromFiles("OS/shaders/simple_v_t2.glvs", "OS/shaders/simple_v_t2.glfs");
        } catch (IOException e) {
            e.printStackTrace();
        }

        uniformMVP = getUniformLocation("uMvp");
        enable();
        setTextureUniform("uTexSampler", 0);
    }

    void setMVP(Matrix4f mat){ gl.glUniformMatrix4fv(uniformMVP, false, CacheBuffer.wrap(mat));}
}
