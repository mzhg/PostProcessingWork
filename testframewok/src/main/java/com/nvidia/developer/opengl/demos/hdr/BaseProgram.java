package com.nvidia.developer.opengl.demos.hdr;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.util.CacheBuffer;

/**
 * Created by mazhen'gui on 2017/3/16.
 */

abstract class BaseProgram extends GLSLProgram{

    int u_mvp;
    int u_modelView;
    int u_eyePos;

    void initVS() {
        u_mvp       = gl.glGetUniformLocation(getProgram(), "viewProjMatrix");
        u_modelView = gl.glGetUniformLocation(getProgram(), "ModelMatrix");
        u_eyePos    = gl.glGetUniformLocation(getProgram(), "eyePos");
    }

    public void applyMVP(Matrix4f mat) {if(u_mvp >= 0) gl.glUniformMatrix4fv(u_mvp,false, CacheBuffer.wrap(mat != null ? mat : Matrix4f.IDENTITY));}
    public void applyModelView(Matrix4f mat) {if(u_modelView != -1) gl.glUniformMatrix4fv(u_modelView, false, CacheBuffer.wrap(mat != null ? mat : Matrix4f.IDENTITY));}
    public void applyEyePos(Vector3f eyePos){ if(u_eyePos >= 0) gl.glUniform3f(u_eyePos, eyePos.x, eyePos.y, eyePos.z);}

    public abstract void applyEmission(float r, float g, float b);
    public abstract void applyColor(float r, float g, float b, float a);
    public abstract int getAttribNormal();
    public abstract int getAttribPosition();
}
