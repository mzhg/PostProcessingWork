package com.nvidia.developer.opengl.demos;

import com.nvidia.developer.opengl.app.NvSampleApp;
import com.nvidia.developer.opengl.utils.NvImage;

import org.lwjgl.util.vector.Matrix4f;

import java.io.IOException;

import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.core.PostProcessing;
import jet.opengl.postprocessing.core.PostProcessingFrameAttribs;
import jet.opengl.postprocessing.shader.FullscreenProgram;
import jet.opengl.postprocessing.texture.FramebufferGL;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.Texture2DDesc;
import jet.opengl.postprocessing.texture.TextureAttachDesc;
import jet.opengl.postprocessing.texture.TextureUtils;

/**
 * Created by mazhen'gui on 2017/4/24.
 */

public class RadiualBlurDemo extends NvSampleApp {

    private FramebufferGL m_framebuffer;
    private Texture2D     m_fbo_tex;
    private Texture2D     m_screenTex;
    private int     m_sourceTexture;
    private GLFuncProvider gl;
    private PostProcessing m_PostProcessing;
    private Texture2D m_outputTex;
    private FullscreenProgram m_ScreenQuadProgram;

    private final Matrix4f modelMatrix = new Matrix4f();
    private PostProcessingFrameAttribs m_frameAttribs;
    private float m_globalTime = 0;

    @Override
    protected void initRendering() {
        getGLContext().setSwapInterval(0);
        gl = GLFuncProviderFactory.getGLFuncProvider();

        //load input texture
        NvImage m_sourceImage = null;
        try {
            m_sourceImage = NvImage.createFromDDSFile("ComputeBasicGLSL\\textures\\flower1024.dds");
        } catch (IOException e) {
            e.printStackTrace();
        }
        m_sourceTexture = m_sourceImage.updaloadTexture();

        gl.glBindTexture(GLenum.GL_TEXTURE_2D, m_sourceTexture);
        gl.glTexParameteri(GLenum.GL_TEXTURE_2D, GLenum.GL_TEXTURE_MIN_FILTER, GLenum.GL_LINEAR);
        gl.glTexParameteri(GLenum.GL_TEXTURE_2D, GLenum.GL_TEXTURE_MAG_FILTER, GLenum.GL_LINEAR);
        gl.glBindTexture(GLenum.GL_TEXTURE_2D, 0);

        m_ScreenQuadProgram = new FullscreenProgram(true);
        m_PostProcessing = new PostProcessing();
        m_frameAttribs = new PostProcessingFrameAttribs();

        m_screenTex = TextureUtils.createTexture2D(GLenum.GL_TEXTURE_2D, m_sourceTexture);
    }

    @Override
    public void display() {
        gl.glClearColor(0.2f, 0.0f, 0.2f, 1.0f);
        gl.glClear(GLenum.GL_COLOR_BUFFER_BIT | GLenum.GL_DEPTH_BUFFER_BIT);

        m_frameAttribs.sceneColorTexture =m_screenTex;
        m_frameAttribs.viewport.set(0,0, getGLContext().width(), getGLContext().height());
        m_frameAttribs.outputTexture = null;

        float radiu_jitter = (float)Math.sin(m_globalTime) * 0.1f;
        final float radius = 0.32f + radiu_jitter;

        float sin = (float)Math.sin(m_globalTime/1.75);
        float cos = (float)Math.cos(m_globalTime/1.75);

        float centerX = radius * cos + 0.5f;
        float centerY = radius * sin + 0.5f;
        m_PostProcessing.addRadialBlur(centerX, centerY, 20);
        m_PostProcessing.performancePostProcessing(m_frameAttribs);

//        gl.glActiveTexture(GLenum.GL_TEXTURE0);
//        gl.glBindTexture(GLenum.GL_TEXTURE_2D, m_sourceTexture);
//
////         scale the position.x to the apect ratio.
//        m_ScreenQuadProgram.enable();
//        m_ScreenQuadProgram.applyPositionTransform(modelMatrix);
//        gl.glDrawArrays(GLenum.GL_TRIANGLE_STRIP, 0, 4);
//        m_ScreenQuadProgram.disable();

        m_globalTime += getFrameDeltaTime();
    }

    @Override
    protected void reshape(int width, int height) {
        if(width == 0 || height == 0 || (m_fbo_tex != null && m_fbo_tex.getWidth() == width && m_fbo_tex.getHeight() == height)){
            return;
        }

        if(m_framebuffer != null){
            m_framebuffer.dispose();
        }

        m_framebuffer = new FramebufferGL();
        int format = gl.getGLAPIVersion().major >= 3 ? GLenum.GL_RGB8 : GLenum.GL_RGB;
        m_framebuffer.bind();
        m_fbo_tex = m_framebuffer.addTexture2D(new Texture2DDesc(width, height, format), new TextureAttachDesc());
        m_framebuffer.unbind();

        gl.glViewport(0,0, width, height);

        modelMatrix.m00 = (float)height/width;
    }
}
