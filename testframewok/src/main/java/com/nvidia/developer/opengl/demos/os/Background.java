package com.nvidia.developer.opengl.demos.os;

import com.nvidia.developer.opengl.models.DrawMode;
import com.nvidia.developer.opengl.models.GLVAO;
import com.nvidia.developer.opengl.models.QuadricBuilder;
import com.nvidia.developer.opengl.models.QuadricMesh;
import com.nvidia.developer.opengl.models.QuadricSphere;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

import java.io.IOException;

import jet.opengl.postprocessing.common.Disposeable;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.TextureUtils;

/**
 * Created by mazhen'gui on 2017/6/6.
 */

final class Background implements Disposeable{
    GLVAO m_SphereVAO;
    Texture2D m_BackgroundTex;
    GLFuncProvider gl;

    final Matrix4f m_model = new Matrix4f();

    void initlize(){
        QuadricBuilder builder = new QuadricBuilder();
        builder.setAutoGenNormal(false).setGenNormal(false);
        builder.setXSteps(50).setYSteps(50);
        builder.setPostionLocation(/*program.getAttribPosition()*/0);
        builder.setTexCoordLocation(/*program.getAttribTexCoord()*/1);
        builder.setDrawMode(DrawMode.FILL);
        builder.setCenterToOrigin(true);
        m_SphereVAO = new QuadricMesh(builder, new QuadricSphere()).getModel().genVAO();

        try {
            m_BackgroundTex = TextureUtils.createTexture2DFromFile("OS/textures/background.jpg", true);
        } catch (IOException e) {
            e.printStackTrace();
        }

        gl = GLFuncProviderFactory.getGLFuncProvider();
    }

    void draw(SingleTextureProgram program, Matrix4f projView){
        gl.glActiveTexture(GLenum.GL_TEXTURE0);
        gl.glBindTexture(m_BackgroundTex.getTarget(), m_BackgroundTex.getTexture());
        gl.glDisable(GLenum.GL_BLEND);

        m_model.setIdentity();
        m_model.scale(60);
        m_model.rotate((float)Math.toRadians(90), Vector3f.X_AXIS);
        Matrix4f mvp =  Matrix4f.mul(projView, m_model, m_model);
        program.enable();
        program.setMVP(mvp);

        m_SphereVAO.bind();
        m_SphereVAO.draw(GLenum.GL_TRIANGLES);
        m_SphereVAO.unbind();
    }

    @Override
    public void dispose() {
        if(m_SphereVAO != null){
            m_SphereVAO.dispose();
            m_SphereVAO = null;
        }
    }
}
