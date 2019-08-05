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
import jet.opengl.postprocessing.util.Numeric;

public class VolumetricFogDemo extends NvSampleApp {

    private Cube16 m_Scene;
    private GLFuncProvider gl;
    private FullscreenProgram fullscreenProgram;
    private int m_DummyVAO;

    private VolumetricFog m_PostProcessing;
    private final VolumetricFog.Params m_Params = new VolumetricFog.Params();

    private boolean m_EnableVolumetricFog = true;

    @Override
    protected void initRendering() {
        getGLContext().setSwapInterval(0);
        m_Scene = new Cube16(this, true);
        m_Scene.onCreate();
        m_Scene.setLightType(LightType.DIRECTIONAL);
        fullscreenProgram = new FullscreenProgram();
        gl = GLFuncProviderFactory.getGLFuncProvider();
        m_DummyVAO = gl.glGenVertexArray();

        m_PostProcessing = new VolumetricFog();

        /*for(int slize = 0; slize < 64; slize++){
            float sceneDepth = ComputeDepthFromZSlice(slize);
            float slice = ComputeZSliceFromDepth(sceneDepth, 0);

            System.out.printf("SceneDepth = %f, slice = %d\n", sceneDepth, (int)slice);
        }*/
    }

    private static final Vector3f VolumetricFog_GridZParams = new Vector3f(0.0029425365f, 0.97175163f, 32.0f);
    private static float ComputeDepthFromZSlice(float ZSlice) {
        float SliceDepth = (float) ((Numeric.exp2(ZSlice / VolumetricFog_GridZParams.z) - VolumetricFog_GridZParams.y) / VolumetricFog_GridZParams.x);
        return SliceDepth;
    }

    private static float ComputeZSliceFromDepth(float SceneDepth, float Offset){
        return (float) (Numeric.log2(SceneDepth*VolumetricFog_GridZParams.x+VolumetricFog_GridZParams.y)*VolumetricFog_GridZParams.z + Offset);
    }

    @Override
    public void initUI() {
        mTweakBar.addValue("Enable Volumetric Fog", createControl("m_EnableVolumetricFog"));
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
        m_Scene.draw(!m_EnableVolumetricFog);

        if(!m_EnableVolumetricFog)return;

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
            m_Params.resetLights();
            switch (m_Scene.getLightMode()){
                case DIRECTIONAL:
                    m_Params.addDirectionLight(m_Scene.getLightDir(),  new Vector3f(0.904016f, 0.843299f, 0.70132f), 1,
                            m_Scene.getLightViewMat(), m_Scene.getLightProjMat(), m_Scene.getShadowMap());
                    break;
                case POINT:
                    m_Params.addPointLight(m_Scene.getLightPos(), m_Scene.getLightFarlane(), new Vector3f(0.904016f, 0.843299f, 0.70132f), 1,
                            m_Scene.getCubeLightViewMats(), m_Scene.getLightProjMat(), m_Scene.getShadowMap());
                    break;
            }


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
