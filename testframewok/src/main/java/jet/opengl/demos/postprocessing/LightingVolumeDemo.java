package jet.opengl.demos.postprocessing;

import com.nvidia.developer.opengl.app.NvKeyActionType;
import com.nvidia.developer.opengl.app.NvSampleApp;

import jet.opengl.demos.nvidia.volumelight.VolumeLightProcess;
import jet.opengl.demos.scenes.Cube16;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.core.PostProcessing;
import jet.opengl.postprocessing.core.PostProcessingFrameAttribs;
import jet.opengl.postprocessing.core.volumetricLighting.AccelStruct;
import jet.opengl.postprocessing.core.volumetricLighting.InscaterringIntegralEvalution;
import jet.opengl.postprocessing.core.volumetricLighting.LightScatteringFrameAttribs;
import jet.opengl.postprocessing.core.volumetricLighting.LightScatteringInitAttribs;
import jet.opengl.postprocessing.core.volumetricLighting.LightType;
import jet.opengl.postprocessing.shader.FullscreenProgram;

/**
 * Created by mazhen'gui on 2017-05-15 15:56:59.
 */

public class LightingVolumeDemo extends NvSampleApp {
    private Cube16 m_Scene;
    private GLFuncProvider gl;
    private FullscreenProgram fullscreenProgram;
    private int m_DummyVAO;

    private PostProcessing m_PostProcessing;
    private PostProcessingFrameAttribs m_frameAttribs;
    private LightScatteringInitAttribs m_InitAttribs;
    private LightScatteringFrameAttribs m_LightFrameAttribs;

    private VolumeLightProcess m_volumeLight;

    @Override
    protected void initRendering() {
        getGLContext().setSwapInterval(0);
        m_Scene = new Cube16(this);

        m_Scene.onCreate();
        fullscreenProgram = new FullscreenProgram();
        gl = GLFuncProviderFactory.getGLFuncProvider();
        m_DummyVAO = gl.glGenVertexArray();

        m_volumeLight = new VolumeLightProcess();
        m_volumeLight.initlizeGL(m_Scene.getShadowMapResolution());

        m_PostProcessing = new PostProcessing();
        m_frameAttribs = new PostProcessingFrameAttribs();
        m_InitAttribs = new LightScatteringInitAttribs();
        m_InitAttribs.m_iDownscaleFactor = 1;
        m_InitAttribs.m_bCorrectScatteringAtDepthBreaks = false;
        m_InitAttribs.m_bEnableEpipolarSampling = true; // Debug
        m_InitAttribs.m_bOptimizeSampleLocations  = true; // This setting will igorned.
        m_InitAttribs.m_bAnisotropicPhaseFunction = true;
        m_InitAttribs.m_bShowSampling = false;
        m_InitAttribs.m_bStainedGlass = false;
        m_InitAttribs.m_uiAccelStruct = AccelStruct.MIN_MAX_TREE;
        m_InitAttribs.m_uiInsctrIntglEvalMethod = InscaterringIntegralEvalution.MY_LUT;

        m_LightFrameAttribs = new LightScatteringFrameAttribs();
    }

    @Override
    public void initUI() {
        m_Scene.initUI(mTweakBar);
    }

    @Override
    protected void reshape(int width, int height) {
        if(width <= 0 || height <= 0)
            return;

        m_Scene.onResize(width, height);
        m_volumeLight.onResize(width, height);

        m_InitAttribs.m_uiBackBufferWidth = width;
        m_InitAttribs.m_uiBackBufferHeight = height;
    }

    @Override
    public void display() {
        m_Scene.draw();
//        if(true) return;

        {
            // post processing...
            gl.glClear(GLenum.GL_COLOR_BUFFER_BIT);
            gl.glDisable(GLenum.GL_DEPTH_TEST);
            // Apply the DOF Bokeh and render result to scene_rt2
            m_frameAttribs.sceneColorTexture = m_Scene.getSceneColor();
            m_frameAttribs.sceneDepthTexture = m_Scene.getSceneDepth();
            m_frameAttribs.cameraNear = m_Scene.getSceneNearPlane();
            m_frameAttribs.cameraFar =  m_Scene.getSceneFarPlane();
            m_frameAttribs.outputTexture = null;
            m_frameAttribs.viewport.set(0,0, getGLContext().width(), getGLContext().height());
            m_frameAttribs.viewMat = m_Scene.getViewMat();
            m_frameAttribs.projMat = m_Scene.getProjMat();
            m_frameAttribs.fov =     m_Scene.getFovInRadian();

            m_frameAttribs.shadowMapTexture = m_Scene.getShadowMap();
            m_frameAttribs.lightDirection = m_Scene.getLightDir();
            m_frameAttribs.lightPos = m_Scene.getLightPos();
            m_frameAttribs.lightProjMat = m_Scene.getLightProjMat();
            m_frameAttribs.lightViewMat = m_Scene.getLightViewMat();
            if(m_InitAttribs.m_uiLightType == LightType.SPOT){
                m_LightFrameAttribs.m_f4LightColorAndIntensity.set(80.0f, 80.0f, 80.0f, 5711.714f);   // For the spot light
            }else{
                m_LightFrameAttribs.m_f4LightColorAndIntensity.set(0.904016f, 0.843299f, 0.70132f, 200.0f);  // for the direction light.
            }

            m_InitAttribs.m_uiLightType = LightType.values()[m_Scene.getLightMode()];
            float fSceneExtent = 100;
            m_LightFrameAttribs.m_fMaxTracingDistance = fSceneExtent * ( m_InitAttribs.m_uiLightType == LightType.DIRECTIONAL  ? 1.5f : 10.f);
            m_LightFrameAttribs.m_fDistanceScaler = 60000.f / m_LightFrameAttribs.m_fMaxTracingDistance;
            m_LightFrameAttribs.m_bShowLightingOnly = false;

            m_PostProcessing.addVolumeLight(m_InitAttribs, m_LightFrameAttribs);
            m_PostProcessing.performancePostProcessing(m_frameAttribs);
        }
    }

    @Override
    public void onDestroy() {
        m_Scene.onDestroy();
        fullscreenProgram.dispose();
        gl.glDeleteVertexArray(m_DummyVAO);
    }

    @Override
    public boolean handleKeyInput(int code, NvKeyActionType action) {
        return m_Scene.handleKeyInput(code, action);
    }
}
