package com.nvidia.developer.opengl.demos;

import com.nvidia.developer.opengl.app.NvSampleApp;
import com.nvidia.developer.opengl.demos.scenes.Ball200;
import com.nvidia.developer.opengl.ui.NvTweakBar;

import java.io.IOException;

import jet.opengl.postprocessing.common.GLCheck;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.core.PostProcessing;
import jet.opengl.postprocessing.core.PostProcessingFrameAttribs;
import jet.opengl.postprocessing.shader.FullscreenProgram;
import jet.opengl.postprocessing.shader.VisualDepthTextureProgram;
import jet.opengl.postprocessing.texture.SamplerUtils;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.Texture2DDesc;
import jet.opengl.postprocessing.texture.TextureUtils;

/**
 * Created by mazhen'gui on 2017/5/8.
 */
public class DOFBokehDemo extends NvSampleApp {
    Ball200 sceneBall;
    FullscreenProgram fullscreenProgram;
    private VisualDepthTextureProgram m_visTexShader;

    private int scene_rt2;
    private Texture2D scene_color_tex2;

    int m_DummyVAO;
    int sampler;

    private boolean m_showShadowMap;
    private float m_focalDepth = 35;
    private float m_focalRange = 10;
    private float m_fStop = 5;
    private float nearTransitionRegion = 5;
    private float farTransitionRegion = 10;

    private GLFuncProvider gl;
    private PostProcessing m_PostProcessing;
    private PostProcessingFrameAttribs m_frameAttribs;

    @Override
    public void initUI() {
        NvTweakBar tweakBar = mTweakBar;
        tweakBar.addValue("Show ShadowMap", createControl("m_showShadowMap"));
        tweakBar.addValue("Focal Depth", createControl("m_focalDepth"), 3f, 50);
        tweakBar.addValue("Focal Range", createControl("m_focalRange"), 3f, 50);
        tweakBar.addValue("Near Transition", createControl("nearTransitionRegion"), 3f, 50);
        tweakBar.addValue("Far Transition", createControl("farTransitionRegion"), 3f, 50);
        tweakBar.addValue("FStop", createControl("m_fStop"), 3f, 50);
    }

    @Override
    protected void initRendering() {
        gl = GLFuncProviderFactory.getGLFuncProvider();
        getGLContext().setSwapInterval(0);
        sceneBall = new Ball200(m_transformer);
        sceneBall.onCreate();

        fullscreenProgram = new FullscreenProgram();

        m_DummyVAO = gl.glGenVertexArray();

        GLCheck.checkError();

        sampler = SamplerUtils.getDefaultSampler();
        m_PostProcessing = new PostProcessing();
        m_frameAttribs = new PostProcessingFrameAttribs();
    }

    @Override
    protected void reshape(int width, int height) {
        sceneBall.onResize(width, height);

        if(width == 0 || height == 0){
            return;
        }

        if(scene_color_tex2 != null && scene_color_tex2.getWidth() == width && scene_color_tex2.getHeight() == height ){
            return;
        }

        if(scene_color_tex2 != null){
            scene_color_tex2.dispose();
        }

        {
            // create scene render target and textures.
            Texture2DDesc colorDesc = new Texture2DDesc(width, height, GLenum.GL_RGBA16F);
            scene_color_tex2 = TextureUtils.createTexture2D(colorDesc, null);
//            scene_color_tex2.setMagFilter(GL11.GL_LINEAR);
//            scene_color_tex2.setMinFilter(GL11.GL_LINEAR);
//            scene_color_tex2.setWrapS(GL12.GL_CLAMP_TO_EDGE);
//            scene_color_tex2.setWrapT(GL12.GL_CLAMP_TO_EDGE);

            if(scene_rt2 == 0){
                scene_rt2 = gl.glGenFramebuffer();
            }

            gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, scene_rt2);
            {
                gl.glFramebufferTexture2D(GLenum.GL_FRAMEBUFFER, GLenum.GL_COLOR_ATTACHMENT0, scene_color_tex2.getTarget(), scene_color_tex2.getTexture(), 0);
            }
            gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, 0);
        }

        GLCheck.checkError();
    }

    @Override
    public void display() {
        sceneBall.draw();
        GLCheck.checkError();

        {   // Show the shadowmap if enabled.
            if(m_showShadowMap){
                showShadownMap();
                return;
            }
        }

        {
            gl.glClear(GLenum.GL_COLOR_BUFFER_BIT);
//            gl.glDisable(GLenum.GL_DEPTH_TEST);
            // Apply the DOF Bokeh and render result to scene_rt2
            m_frameAttribs.sceneColorTexture = sceneBall.getSceneColor();
            m_frameAttribs.sceneDepthTexture = sceneBall.getSceneDepth();
            m_frameAttribs.cameraNear = sceneBall.getSceneNearPlane();
            m_frameAttribs.cameraFar =  sceneBall.getSceneFarPlane();
            m_frameAttribs.outputTexture = null;
            m_frameAttribs.viewport.set(0,0, getGLContext().width(), getGLContext().height());
            m_frameAttribs.viewMat = sceneBall.getViewMat();
            m_frameAttribs.projMat = sceneBall.getProjMat();

            m_PostProcessing.addDOFBokeh(m_focalDepth, m_focalRange, m_fStop);
//            m_PostProcessing.addDOFGaussion(m_focalDepth, m_focalRange, nearTransitionRegion, farTransitionRegion, 1, true, false);
            m_PostProcessing.performancePostProcessing(m_frameAttribs);
        }

        {   // Apply tone mapping to the result
//            gl.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
//            GL11.glViewport(0, 0, nvApp.width(), nvApp.height());
//            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT|GL11.GL_DEPTH_BUFFER_BIT);
//            GL11.glDisable(GL11.GL_DEPTH_TEST);
//
//            tone_prog.enable();
//            tone_prog.applyAtt(1.0f);
//            tone_prog.applyExposure(0.8f);
//            tone_prog.applyBrightThreshold(5.0f);
//            tone_prog.applyGamma(0.9f);
//
//            GL30.glBindVertexArray(m_DummyVAO);
//            scene_color_tex2.bind(0);
//            GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 3);
//            scene_color_tex2.unbind();
//            GL20.glUseProgram(0);
//            GL30.glBindVertexArray(0);
        }

        {
            if(true) return;
            gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, 0);
            gl.glViewport(0, 0, getGLContext().width(), getGLContext().height());
            gl.glClear(GLenum.GL_COLOR_BUFFER_BIT|GLenum.GL_DEPTH_BUFFER_BIT);
            gl.glDisable(GLenum.GL_DEPTH_TEST);

            fullscreenProgram.enable();
            gl.glBindVertexArray(m_DummyVAO);
//            scene_color_tex2.bind(0);
            gl.glActiveTexture(GLenum.GL_TEXTURE0);
            gl.glBindTexture(sceneBall.getSceneColor().getTarget(), sceneBall.getSceneColor().getTexture());
            gl.glDrawArrays(GLenum.GL_TRIANGLES, 0, 3);
            gl.glBindTexture(GLenum.GL_TEXTURE_2D, 0);
            gl.glUseProgram(0);
            gl.glBindVertexArray(0);
        }
    }

    private void showShadownMap() {
        if(m_visTexShader == null)
            try {
                m_visTexShader = new VisualDepthTextureProgram(false);
            } catch (IOException e) {
                e.printStackTrace();
            }

        gl.glViewport(0, 0, getGLContext().width(), getGLContext().height());
        gl.glDisable(GLenum.GL_DEPTH_TEST);
        gl.glDisable(GLenum.GL_CULL_FACE);
        gl.glClearColor(0, 0, 0, 0);
        gl.glClear(GLenum.GL_COLOR_BUFFER_BIT);

        m_visTexShader.enable();

        m_visTexShader.setUniforms(sceneBall.getShadowNearPlane(), sceneBall.getShadowFarPlane(), 0, 1.0f);
//        sceneBall.getShadowMap().bind(0, sampler);
        gl.glActiveTexture(GLenum.GL_TEXTURE0);
        gl.glBindTexture(sceneBall.getShadowMap().getTarget(), sceneBall.getShadowMap().getTexture());
        gl.glBindSampler(0, sampler);

        GLCheck.checkError();

        gl.glDisable(GLenum.GL_DEPTH_TEST);
        gl.glDisable(GLenum.GL_CULL_FACE);

        gl.glBindVertexArray(m_DummyVAO);
        gl.glDrawArrays(GLenum.GL_TRIANGLES, 0, 3);

        m_visTexShader.disable();
        gl.glBindSampler(0, 0);
        gl.glBindVertexArray(0);
//        sceneBall.getShadowMap().unbind();
    }

}
