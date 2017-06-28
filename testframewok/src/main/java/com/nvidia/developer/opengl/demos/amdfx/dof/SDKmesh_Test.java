package com.nvidia.developer.opengl.demos.amdfx.dof;

import com.nvidia.developer.opengl.app.NvSampleApp;
import com.nvidia.developer.opengl.demos.amdfx.common.AMD_Mesh;
import com.nvidia.developer.opengl.models.DrawMode;
import com.nvidia.developer.opengl.models.GLVAO;
import com.nvidia.developer.opengl.models.QuadricBuilder;
import com.nvidia.developer.opengl.models.QuadricMesh;
import com.nvidia.developer.opengl.models.QuadricSphere;

import org.lwjgl.util.vector.Matrix4f;

import java.io.IOException;

import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.TextureUtils;
import jet.opengl.postprocessing.util.CacheBuffer;

/**
 * Created by mazhen'gui on 2017/6/27.
 */

public class SDKmesh_Test extends NvSampleApp {
    AMD_Mesh m_mesh;
    ModelProgram m_modelProgram;

    GLVAO        m_Sphere;
    Texture2D    m_texture;

    final Matrix4f m_Proj = new Matrix4f();
    final Matrix4f m_View = new Matrix4f();
    GLFuncProvider gl;

    @Override
    protected void initRendering() {
        gl = GLFuncProviderFactory.getGLFuncProvider();
        gl.glBindVertexArray(0);
        m_mesh = new AMD_Mesh();
        m_mesh.Create("amdfx/DepthOfFieldFX/models/Tank", "TankScene.sdkmesh", true);

        QuadricBuilder builder = new QuadricBuilder();
        builder.setXSteps(50).setYSteps(50);
        builder.setPostionLocation(0);
        builder.setNormalLocation(1);
        builder.setTexCoordLocation(2);

        builder.setDrawMode(DrawMode.FILL);
        builder.setCenterToOrigin(true);
        m_Sphere = new QuadricMesh(builder, new QuadricSphere()).getModel().genVAO();

        try {
            m_texture = TextureUtils.createTexture2DFromFile("OS/textures/background.jpg", true);
        } catch (IOException e) {
            e.printStackTrace();
        }

        m_transformer.setTranslation(0,0,-10);
        m_modelProgram = new ModelProgram();
    }

    @Override
    public void display() {
        gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, 0);
        gl.glClearColor(0,0,0,0);
        gl.glClearDepthf(1.0f);
        gl.glClear(GLenum.GL_COLOR_BUFFER_BIT|GLenum.GL_DEPTH_BUFFER_BIT);

        m_transformer.getModelViewMat(m_View);

//        Matrix4f.lookAt(new Vector3f(0,0, 3), Vector3f.ZERO, Vector3f.Y_AXIS, m_View);
        Matrix4f.mul(m_Proj, m_View, m_View);

        m_modelProgram.enable();
        m_modelProgram.setWorldViewProjection(m_View);
        gl.glDisable(GLenum.GL_CULL_FACE);
        gl.glDisable(GLenum.GL_BLEND);
        gl.glEnable(GLenum.GL_DEPTH_TEST);
        gl.glDepthFunc(GLenum.GL_LESS);

//        gl.glActiveTexture(GLenum.GL_TEXTURE0);
//        gl.glBindTexture(m_texture.getTarget(), m_texture.getTexture());
        m_mesh.Render();


//        m_Sphere.bind();
//        m_Sphere.draw(GLenum.GL_TRIANGLES);
//        m_Sphere.unbind();
    }

    @Override
    protected void reshape(int width, int height) {
        if(width <= 0 || height <= 0)
            return;

        Matrix4f.perspective(60, (float)width/height, 0.1f, 100.0f, m_Proj);
        gl.glViewport(0,0, width, height);
    }

    private static final class ModelProgram extends GLSLProgram{
//        uniform mat4 m_WorldViewProjection;
//        uniform mat4 m_World;

        private final int worldViewProjectionIndex;
        private final int worldIndex;

        ModelProgram(){
            final String shaderPath = "amdfx/DepthOfFieldFX/shaders/";
            try {
                setSourceFromFiles(shaderPath + "MeshTestVS.vert", shaderPath + "MeshTestPS.frag");
            } catch (IOException e) {
                e.printStackTrace();
            }

            worldViewProjectionIndex = getUniformLocation("m_WorldViewProjection");
            worldIndex = getUniformLocation("m_World");

            if(worldIndex >= 0) {
                enable();
                gl.glUniformMatrix4fv(worldIndex, false, CacheBuffer.wrap(Matrix4f.IDENTITY));
            }
        }

        void setWorldViewProjection(Matrix4f wvp){
            gl.glUniformMatrix4fv(worldViewProjectionIndex, false, CacheBuffer.wrap(wvp));
        }
    }
}
