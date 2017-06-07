package com.nvidia.developer.opengl.demos;

import com.nvidia.developer.opengl.app.NvSampleApp;
import com.nvidia.developer.opengl.demos.scenes.outdoor.OutDoorScene;

import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.core.CascadeShadowMapAttribs;
import jet.opengl.postprocessing.core.PostProcessing;
import jet.opengl.postprocessing.core.PostProcessingFrameAttribs;
import jet.opengl.postprocessing.core.outdoorLighting.OutdoorLightScatteringFrameAttribs;
import jet.opengl.postprocessing.core.outdoorLighting.OutdoorLightScatteringInitAttribs;
import jet.opengl.postprocessing.shader.FullscreenProgram;

/**
 * Created by mazhen'gui on 2017/6/2.
 */

public class OutdoorLightScatteringSample extends NvSampleApp {
    private OutDoorScene m_Scene;
    private GLFuncProvider gl;
    private FullscreenProgram fullscreenProgram;
    private int m_DummyVAO;

    private PostProcessing m_PostProcessing;
    private PostProcessingFrameAttribs m_frameAttribs;
    private OutdoorLightScatteringInitAttribs m_InitAttribs;
    private OutdoorLightScatteringFrameAttribs m_RuntimeAttribs;

    @Override
    protected void initRendering() {
        getGLContext().setSwapInterval(0);
        m_Scene = new OutDoorScene(this);
        m_Scene.onCreate();
        fullscreenProgram = new FullscreenProgram();
        gl = GLFuncProviderFactory.getGLFuncProvider();
        m_DummyVAO = gl.glGenVertexArray();

        m_PostProcessing = new PostProcessing();
        m_frameAttribs = new PostProcessingFrameAttribs();
        m_frameAttribs.cascadeShadowMapAttribs = new CascadeShadowMapAttribs();

        m_InitAttribs = new OutdoorLightScatteringInitAttribs();
        m_InitAttribs.m_bEnableEpipolarSampling = false;
        m_RuntimeAttribs = new OutdoorLightScatteringFrameAttribs();
        m_RuntimeAttribs.f4ExtraterrestrialSunColor.set(5,5,5,5);
    }

    @Override
    protected void reshape(int width, int height) {
        m_Scene.onResize(width, height);
    }

    @Override
    public void display() {
        m_Scene.draw(getFrameDeltaTime());

        {
            gl.glClear(GLenum.GL_COLOR_BUFFER_BIT);
            gl.glDisable(GLenum.GL_DEPTH_TEST);
            // Apply the DOF Bokeh and render result to scene_rt2
            m_frameAttribs.sceneColorTexture = m_Scene.getSceneColor();
            m_frameAttribs.sceneDepthTexture = m_Scene.getSceneDepth();
            m_frameAttribs.shadowMapTexture  = m_Scene.getShadowMap();
            m_frameAttribs.cameraNear = m_Scene.getSceneNearPlane();
            m_frameAttribs.cameraFar =  m_Scene.getSceneFarPlane();
            m_frameAttribs.lightDirection    = m_Scene.getLightDirection();
            m_Scene.getCascadeShadowMapInformations(m_frameAttribs.cascadeShadowMapAttribs);
            m_frameAttribs.outputTexture = null;
            m_frameAttribs.viewport.set(0,0, getGLContext().width(), getGLContext().height());
            m_frameAttribs.viewMat = m_Scene.getViewMat();
            m_frameAttribs.projMat = m_Scene.getProjMat();
            m_frameAttribs.fov =     m_Scene.getFovInRadian();

            m_PostProcessing.addOutdoorLight(m_InitAttribs, m_RuntimeAttribs);
            m_PostProcessing.performancePostProcessing(m_frameAttribs);
        }

//        m_Scene.resoveMultisampleTexture();
        if(true)return;
        gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, 0);
        gl.glViewport(0, 0, getGLContext().width(), getGLContext().height());
        gl.glClear(GLenum.GL_COLOR_BUFFER_BIT|GLenum.GL_DEPTH_BUFFER_BIT);
        gl.glDisable(GLenum.GL_DEPTH_TEST);

        fullscreenProgram.enable();
        gl.glBindVertexArray(m_DummyVAO);
//            scene_color_tex2.bind(0);
        gl.glActiveTexture(GLenum.GL_TEXTURE0);
        gl.glBindTexture(m_Scene.getSceneColor().getTarget(), m_Scene.getSceneColor().getTexture());
        gl.glDrawArrays(GLenum.GL_TRIANGLES, 0, 3);
        gl.glBindTexture(GLenum.GL_TEXTURE_2D, 0);
        gl.glUseProgram(0);
        gl.glBindVertexArray(0);
    }

    @Override
    public void onDestroy() {
        m_Scene.onDestroy();
        fullscreenProgram.dispose();
        gl.glDeleteVertexArray(m_DummyVAO);
    }
}
