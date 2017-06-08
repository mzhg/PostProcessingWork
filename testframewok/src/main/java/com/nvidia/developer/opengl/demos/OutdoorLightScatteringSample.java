package com.nvidia.developer.opengl.demos;

import com.nvidia.developer.opengl.app.NvSampleApp;
import com.nvidia.developer.opengl.demos.scenes.outdoor.OutDoorScene;
import com.nvidia.developer.opengl.utils.FieldControl;

import java.io.IOException;

import jet.opengl.postprocessing.common.GLCheck;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.core.CascadeShadowMapAttribs;
import jet.opengl.postprocessing.core.PostProcessing;
import jet.opengl.postprocessing.core.PostProcessingFrameAttribs;
import jet.opengl.postprocessing.core.outdoorLighting.OutdoorLightScatteringFrameAttribs;
import jet.opengl.postprocessing.core.outdoorLighting.OutdoorLightScatteringInitAttribs;
import jet.opengl.postprocessing.shader.FullscreenProgram;
import jet.opengl.postprocessing.shader.VisualDepthTextureProgram;

/**
 * Created by mazhen'gui on 2017/6/2.
 */

public class OutdoorLightScatteringSample extends NvSampleApp {
    private OutDoorScene m_Scene;
    private GLFuncProvider gl;
    private FullscreenProgram fullscreenProgram;
    private VisualDepthTextureProgram m_visTexShader;
    private int m_DummyVAO;

    private PostProcessing m_PostProcessing;
    private PostProcessingFrameAttribs m_frameAttribs;
    private OutdoorLightScatteringInitAttribs m_InitAttribs;
    private OutdoorLightScatteringFrameAttribs m_RuntimeAttribs;

    private boolean m_bVisualShadownMap;
    private int m_slice = 0;

    @Override
    public void initUI() {
        mTweakBar.addValue("Visualize Depth", new FieldControl(this, "m_bVisualShadownMap"));
        mTweakBar.addValue("Slice of Depth Texture", new FieldControl(this, "m_slice"), 0, 4);
        mTweakBar.syncValues();
    }

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
        m_InitAttribs.m_bEnableEpipolarSampling = true;
        m_InitAttribs.m_bEnableLightShafts = true;
        m_InitAttribs.m_bAutoExposure = true;
        m_InitAttribs.m_bCorrectScatteringAtDepthBreaks = false;
        m_RuntimeAttribs = new OutdoorLightScatteringFrameAttribs();
        m_RuntimeAttribs.f4ExtraterrestrialSunColor.set(5,5,5,5);
    }

    @Override
    protected void reshape(int width, int height) {
        m_Scene.onResize(width, height);

        m_InitAttribs.m_uiBackBufferWidth = width;
        m_InitAttribs.m_uiBackBufferHeight = height;
    }

    @Override
    public void display() {
        m_Scene.draw(getFrameDeltaTime());

        if(m_bVisualShadownMap){
            gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, 0);
            showShadownMap();
            return;
        }

        {
            gl.glClear(GLenum.GL_COLOR_BUFFER_BIT);
            gl.glDisable(GLenum.GL_DEPTH_TEST);
            // Apply the DOF Bokeh and render result to scene_rt2
            m_frameAttribs.sceneColorTexture = m_Scene.getSceneColor();
            m_frameAttribs.sceneDepthTexture = m_Scene.getSceneDepth();
            m_frameAttribs.shadowMapTexture  = m_Scene.getShadowMap();
            m_frameAttribs.elapsedTime       = getFrameDeltaTime();
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

    private void showShadownMap() {
        if(m_visTexShader == null)
            try {
                m_visTexShader = new VisualDepthTextureProgram(true);
            } catch (IOException e) {
                e.printStackTrace();
            }

        gl.glViewport(0, 0, getGLContext().width(), getGLContext().height());
        gl.glDisable(GLenum.GL_DEPTH_TEST);
        gl.glDisable(GLenum.GL_CULL_FACE);
        gl.glClearColor(0, 0, 0, 0);
        gl.glClear(GLenum.GL_COLOR_BUFFER_BIT);

        m_visTexShader.enable();
        m_visTexShader.setUniforms(m_Scene.getSceneNearPlane(), m_Scene.getSceneFarPlane(), m_slice, 1.0f);
//        m_visTexShader.setSlice(m_slice);
//        m_visTexShader.setLightZFar(m_shadowMapInput.farPlane);
//        m_visTexShader.setLightZNear(m_shadowMapInput.nearPlane);
        gl.glActiveTexture(GLenum.GL_TEXTURE0);
        gl.glBindTexture(GLenum.GL_TEXTURE_2D_ARRAY, m_Scene.getShadowMap().getTexture());
        gl.glTexParameteri(GLenum.GL_TEXTURE_2D_ARRAY, GLenum.GL_TEXTURE_COMPARE_MODE, GLenum.GL_NONE);
        GLCheck.checkError();

        gl.glDisable(GLenum.GL_DEPTH_TEST);
        gl.glDisable(GLenum.GL_CULL_FACE);

        gl.glBindVertexArray(m_DummyVAO);
        gl.glDrawArrays(GLenum.GL_TRIANGLES, 0, 3);
        gl.glBindVertexArray(0);

        m_visTexShader.disable();
        gl.glBindTexture(GLenum.GL_TEXTURE_2D_ARRAY, 0);
    }

    @Override
    public void onDestroy() {
        m_PostProcessing.dispose();
        m_Scene.onDestroy();
        fullscreenProgram.dispose();
        gl.glDeleteVertexArray(m_DummyVAO);
    }
}
