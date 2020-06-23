package jet.opengl.demos.labs.ogl;

import com.nvidia.developer.opengl.app.NvSampleApp;
import com.nvidia.developer.opengl.models.DrawMode;
import com.nvidia.developer.opengl.models.Function;
import com.nvidia.developer.opengl.models.GLVAO;
import com.nvidia.developer.opengl.models.QuadricBuilder;
import com.nvidia.developer.opengl.models.QuadricMesh;
import com.nvidia.developer.opengl.models.QuadricSphere;
import com.nvidia.developer.opengl.models.RevolutionMesh;

import org.lwjgl.util.vector.Axis;
import org.lwjgl.util.vector.Matrix3f;
import org.lwjgl.util.vector.Matrix4f;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import jet.opengl.postprocessing.common.GLCheck;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.shader.GLSLUtil;
import jet.opengl.postprocessing.shader.Macro;
import jet.opengl.postprocessing.util.CacheBuffer;
import jet.opengl.postprocessing.util.DebugTools;

/**
 *  2020-06-23: 18-13
 */
public class DervitiveComputShaderTest extends NvSampleApp {
    static final int FBO_SIZE = 512;
    GLVAO sphere;

    int deferedFBO;
    int normalTex;
    int depthTex;

    int normalTexDDX;
    int normalTexDDY;

    GLFuncProvider gl;

    @Override
    protected void initRendering() {
        gl = GLFuncProviderFactory.getGLFuncProvider();

        createShape();
        createTextures();

        final float near = 0.1f;
        final float far = 10.0f;

        Matrix4f proj = Matrix4f.perspective(45, 1, near, far, null);
        Matrix4f lookAt = Matrix4f.lookAt(0, 0, 2, 0,0,0, 0,1,0, null);
        Matrix4f mvp = Matrix4f.mul(proj, lookAt, null);
        Matrix4f mvpInv = Matrix4f.invert(proj, null);
        Matrix4f viewInv = Matrix4f.invert(lookAt, null);
        Matrix3f normalMat = Matrix4f.getNormalMatrix(lookAt, (Matrix3f) null);
        System.out.println("normalMat: " + normalMat);

        final String shadersPath = "labs/DDX/shaders/";
        // First Pass: render the depth and normal to the textures.
        {
            GLSLProgram sceneProgram = GLSLProgram.createProgram(shadersPath + "SceneNorm.vert", shadersPath + "SceneNorm.frag", null);
            GLCheck.checkError();

            gl.glEnable(GLenum.GL_DEPTH_TEST);
            gl.glDepthFunc(GLenum.GL_LESS);
            sceneProgram.enable();

            GLSLUtil.setMat4(sceneProgram,"u_MVP", mvp);
            GLSLUtil.setMat3(sceneProgram,"u_Norm", normalMat);

            gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, deferedFBO);
            gl.glViewport(0, 0, FBO_SIZE, FBO_SIZE);
            FloatBuffer clearcolors = CacheBuffer.wrap(0.0f,0.0f,0.0f,0.0f);
            gl.glClearBufferfv(GLenum.GL_COLOR, 0, clearcolors);
            gl.glClearBufferfv(GLenum.GL_COLOR, 1, clearcolors);
            gl.glClearBufferfv(GLenum.GL_COLOR, 2, clearcolors);
            gl.glClearBufferfv(GLenum.GL_DEPTH, 0, CacheBuffer.wrap(1.0f));

            sphere.bind();
            sphere.draw(DrawMode.FILL.getGLMode());
            sphere.unbind();
            gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, 0);
            sceneProgram.disable();
            gl.glFlush();
            GLCheck.checkError();

            try {
                DebugTools.saveTextureAsText(GLenum.GL_TEXTURE_2D, normalTexDDX, 0, "E:/textures/BufferNormal/fragDDX.txt");
                DebugTools.saveTextureAsText(GLenum.GL_TEXTURE_2D, normalTexDDY, 0, "E:/textures/BufferNormal/fragDDY.txt");
                DebugTools.saveTextureAsText(GLenum.GL_TEXTURE_2D, normalTex, 0, "E:/textures/BufferNormal/fragNormal.txt");
            } catch (IOException e) {
                e.printStackTrace();
            }
            GLCheck.checkError();
            gl.glDisable(GLenum.GL_DEPTH_TEST);
        }

        // Second the pass, use the depth texture to reconstruct normals.
        {
            final int COMPUTE_SHADER_TILE_GROUP_DIM = 4;

            GLSLProgram invertProgram = GLSLProgram.createProgram(shadersPath + "ComputeDDXY.comp", Macro.asMacros("COMPUTE_SHADER_TILE_GROUP_DIM",COMPUTE_SHADER_TILE_GROUP_DIM));
            invertProgram.enable();
            GLCheck.checkError();

            gl.glActiveTexture(GLenum.GL_TEXTURE0);
            gl.glBindTexture(GLenum.GL_TEXTURE_2D, normalTex);

            gl.glClearTexImage(normalTexDDX, 0, GLenum.GL_RGBA, GLenum.GL_FLOAT, null);
            gl.glClearTexImage(normalTexDDY, 0, GLenum.GL_RGBA, GLenum.GL_FLOAT, null);

            gl.glBindImageTexture(0, normalTexDDX, 0, false, 0, GLenum.GL_WRITE_ONLY, GLenum.GL_RGBA32F);
            gl.glBindImageTexture(1, normalTexDDY, 0, false, 0, GLenum.GL_WRITE_ONLY, GLenum.GL_RGBA32F);

            gl.glDispatchCompute(FBO_SIZE/COMPUTE_SHADER_TILE_GROUP_DIM, FBO_SIZE/COMPUTE_SHADER_TILE_GROUP_DIM, 1);

            try {
                DebugTools.saveTextureAsText(GLenum.GL_TEXTURE_2D, normalTexDDX, 0, "E:/textures/BufferNormal/compDDX.txt");
                DebugTools.saveTextureAsText(GLenum.GL_TEXTURE_2D, normalTexDDY, 0, "E:/textures/BufferNormal/compDDY.txt");
            } catch (IOException e) {
                e.printStackTrace();
            }
            GLCheck.checkError();
        }


    }

    @Override
    public void display() {
        super.display();
    }

    private void createTextures(){
        deferedFBO = gl.glGenFramebuffer();

        gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, deferedFBO);
        {
            normalTex = createTexture2D(GLenum.GL_RGBA32F, GLenum.GL_NEAREST);
            normalTexDDX = createTexture2D(GLenum.GL_RGBA32F, GLenum.GL_NEAREST);
            normalTexDDY = createTexture2D(GLenum.GL_RGBA32F, GLenum.GL_NEAREST);

            gl.glFramebufferTexture2D(GLenum.GL_FRAMEBUFFER, GLenum.GL_COLOR_ATTACHMENT0, GLenum.GL_TEXTURE_2D, normalTex, 0);
            gl.glFramebufferTexture2D(GLenum.GL_FRAMEBUFFER, GLenum.GL_COLOR_ATTACHMENT1, GLenum.GL_TEXTURE_2D, normalTexDDX, 0);
            gl.glFramebufferTexture2D(GLenum.GL_FRAMEBUFFER, GLenum.GL_COLOR_ATTACHMENT2, GLenum.GL_TEXTURE_2D, normalTexDDY, 0);

            depthTex = createTexture2D(GLenum.GL_DEPTH_COMPONENT32F, GLenum.GL_NEAREST);
            gl.glFramebufferTexture2D(GLenum.GL_FRAMEBUFFER, GLenum.GL_DEPTH_ATTACHMENT, GLenum.GL_TEXTURE_2D, depthTex, 0);

            IntBuffer drawBuffers = CacheBuffer.getCachedIntBuffer(3);
            drawBuffers.put(GLenum.GL_COLOR_ATTACHMENT0);
            drawBuffers.put(GLenum.GL_COLOR_ATTACHMENT1);
            drawBuffers.put(GLenum.GL_COLOR_ATTACHMENT2);
            drawBuffers.flip();

            gl.glDrawBuffers(drawBuffers);
        }
        GLCheck.checkFramebufferStatus();
    }

    private int createTexture2D(int format, int filter){

        int textureID = gl.glGenTexture();
        gl.glBindTexture(GLenum.GL_TEXTURE_2D, textureID);
        gl.glTexStorage2D(GLenum.GL_TEXTURE_2D, 1, format, FBO_SIZE, FBO_SIZE);
        gl.glTexParameteri(GLenum.GL_TEXTURE_2D, GLenum.GL_TEXTURE_MIN_FILTER, filter);
        gl.glTexParameteri(GLenum.GL_TEXTURE_2D, GLenum.GL_TEXTURE_MAG_FILTER, filter);
        gl.glTexParameteri(GLenum.GL_TEXTURE_2D, GLenum.GL_TEXTURE_WRAP_S, GLenum.GL_CLAMP_TO_EDGE);
        gl.glTexParameteri(GLenum.GL_TEXTURE_2D, GLenum.GL_TEXTURE_WRAP_T, GLenum.GL_CLAMP_TO_EDGE);
        gl.glBindTexture(GLenum.GL_TEXTURE_2D, 0);

        return textureID;
    }

    private void createShape(){
        QuadricBuilder builder = new QuadricBuilder();
        builder.setXSteps(50).setYSteps(50);
        builder.setPostionLocation(0);
        builder.setNormalLocation(1);
        builder.setTexCoordLocation(2);

        builder.setDrawMode(DrawMode.FILL);
        builder.setCenterToOrigin(true);
        RevolutionMesh mesh = new RevolutionMesh(new Function() {
            public float value(float s) {	return (float)Math.sin(s);}
            public float deri(float s)  {  return -(float)Math.cos(s);}
        }, Axis.X);
        mesh.setScale((float)Math.PI);
        sphere = new QuadricMesh(builder, new QuadricSphere()).getModel().genVAO();
    }
}
