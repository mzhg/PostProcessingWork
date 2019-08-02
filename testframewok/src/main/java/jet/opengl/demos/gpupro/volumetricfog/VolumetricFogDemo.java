package jet.opengl.demos.gpupro.volumetricfog;

import com.nvidia.developer.opengl.app.NvKeyActionType;
import com.nvidia.developer.opengl.app.NvSampleApp;

import org.lwjgl.util.vector.Vector3f;

import jet.opengl.demos.scenes.Cube16;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.core.volumetricLighting.LightType;
import jet.opengl.postprocessing.shader.FullscreenProgram;

public class VolumetricFogDemo extends NvSampleApp {

    private Cube16 m_Scene;
    private GLFuncProvider gl;
    private FullscreenProgram fullscreenProgram;
    private int m_DummyVAO;

    private VolumetricFog m_PostProcessing;
    private final VolumetricFog.Params m_Params = new VolumetricFog.Params();

    @Override
    protected void initRendering() {
        getGLContext().setSwapInterval(0);
        m_Scene = new Cube16(this);

        m_Scene.onCreate();
        fullscreenProgram = new FullscreenProgram();
        gl = GLFuncProviderFactory.getGLFuncProvider();
        m_DummyVAO = gl.glGenVertexArray();

        m_PostProcessing = new VolumetricFog();
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
        m_PostProcessing.onResize(width, height);
    }

    @Override
    public void display() {
        m_Scene.draw(true);

        {
            // post processing...
            gl.glClear(GLenum.GL_COLOR_BUFFER_BIT);
            gl.glDisable(GLenum.GL_DEPTH_TEST);
            // Apply the DOF Bokeh and render result to scene_rt2
            m_Params.sceneColor = m_Scene.getSceneColor();
            m_Params.sceneDepth = m_Scene.getSceneDepth();
            m_Params.cameraNear = m_Scene.getSceneNearPlane();
            m_Params.cameraFar =  m_Scene.getSceneFarPlane();
//            m_frameAttribs.outputTexture = null;
//            m_frameAttribs.viewport.set(0,0, getGLContext().width(), getGLContext().height());
            m_Params.view = m_Scene.getViewMat();
            m_Params.proj = m_Scene.getProjMat();
//            m_frameAttribs.fov =     m_Scene.getFovInRadian();

            m_Params.shadowMap = m_Scene.getShadowMap();
            m_Params.addPointLight(m_Scene.getLightPos(), m_Scene.getLightFarlane(), new Vector3f(0.904016f, 0.843299f, 0.70132f), 1,
                    m_Scene.getCubeLightViewMats(), m_Scene.getLightProjMat(), m_Scene.getShadowMap());
//            m_frameAttribs.lightDirection = m_Scene.getLightDir();
//            m_frameAttribs.lightPos = m_Scene.getLightPos();
//            m_frameAttribs.lightProjMat = m_Scene.getLightProjMat();
//            m_frameAttribs.lightViewMat = m_Scene.getLightViewMat();
//            if(m_InitAttribs.m_uiLightType == LightType.SPOT){
//                m_LightFrameAttribs.m_f4LightColorAndIntensity.set(80.0f, 80.0f, 80.0f, 5711.714f);   // For the spot light
//            }else{
//                m_LightFrameAttribs.m_f4LightColorAndIntensity.set(0.904016f, 0.843299f, 0.70132f, 200.0f);  // for the direction light.
//            }
//
//            m_InitAttribs.m_uiLightType =m_Scene.getLightMode();
//            float fSceneExtent = 100;
//            m_LightFrameAttribs.m_fMaxTracingDistance = fSceneExtent * ( m_InitAttribs.m_uiLightType == LightType.DIRECTIONAL  ? 1.5f : 10.f);
//            m_LightFrameAttribs.m_fDistanceScaler = 60000.f / m_LightFrameAttribs.m_fMaxTracingDistance;
//            m_LightFrameAttribs.m_bShowLightingOnly = false;

            m_PostProcessing.renderVolumetricFog(m_Params);
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
