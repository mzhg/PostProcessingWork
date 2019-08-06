package jet.opengl.demos.gpupro.volumetricfog;

import com.nvidia.developer.opengl.app.NvKeyActionType;
import com.nvidia.developer.opengl.app.NvSampleApp;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector3i;
import org.lwjgl.util.vector.Vector4f;

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
    private float m_intensity = 1;

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

        g_CameraFar = 310;
        g_CameraNear = 0.05f;
        g_CameraPos.set(0, 0, -17.5f);

        Matrix4f.lookAt(g_CameraPos, Vector3f.ZERO, Vector3f.Y_AXIS, g_View);
        Matrix4f.perspective(60, (float)1280/720, g_CameraNear, g_CameraFar, g_Proj);
        Matrix4f.mul(g_Proj, g_View, g_ProjView);
        Matrix4f.invert(g_ProjView, g_ProjViewInv);

        float lastClipZ= -1;
        int validCount = 0;
        int totalCount = 0;
        for(int i = 0; i < VolumetricFog_GridSize.x; i++){
            for(int j = 0; j < VolumetricFog_GridSize.y; j++){
                for(int k = 0; k < VolumetricFog_GridSize.z; k++){
                    Vector3f grid = new Vector3f(i,j,k);
                    Vector3f worldPos = ComputeCellWorldPosition(grid, new Vector3f(0.5f));
                    Vector4f clipPos = Matrix4f.transform(g_ProjView, new Vector4f(worldPos, 1), null);
                    clipPos.scale(1/clipPos.w);
                    if(Math.abs(clipPos.x) <= 1 && Math.abs(clipPos.y) <= 1 && Math.abs(clipPos.z) <=1){
                        validCount++;
                    }

                    if(totalCount > 0){
                        if(clipPos.z < lastClipZ){
//                            System.out.println("ClipZ:"+clipPos.z + " less than " + lastClipZ);
                        }
                    }

                    totalCount ++;
                    lastClipZ = clipPos.z;
                }
            }
        }

        System.out.println("Valid Rate: " + ((float)validCount/totalCount) + ", TotalCount = " + totalCount);
    }

    private static float g_CameraFar;
    private static float g_CameraNear;

    private static final Vector3f g_CameraPos = new Vector3f();
    private static final Vector3f VolumetricFog_GridZParams = new Vector3f(0.010035428f, 0.9036599f, 32.0f);
    private static final Vector3f VolumetricFog_GridSize = new Vector3f(80.0f, 45.0f, 64.0f);
    private static final Matrix4f g_Proj = new Matrix4f();
    private static final Matrix4f g_View = new Matrix4f();
    private static final Matrix4f g_ProjView = new Matrix4f();
    private static final Matrix4f g_ProjViewInv = new Matrix4f();

    private static float ConvertToDeviceZ(float depth)
    {
        Vector4f clipPos = Matrix4f.transform(g_Proj, new Vector4f(0,0, depth, 1), null);
        return clipPos.z / clipPos.w;

//        float invDiff = 1.0/(g_CameraFar-g_CameraNear);
//        float DeviceZ = (-(g_CameraFar + g_CameraNear) * invDiff * depth - 2 * g_CameraFar * g_CameraNear * invDiff)/(-depth);
//        return DeviceZ;
    }

    private static float ComputeDepthFromZSlice(float ZSlice) {
        float SliceDepth = (float) ((Numeric.exp2(ZSlice / VolumetricFog_GridZParams.z) - VolumetricFog_GridZParams.y) / VolumetricFog_GridZParams.x);
        return SliceDepth;
    }

    private static float ComputeZSliceFromDepth(float SceneDepth, float Offset){
        return (float) (Numeric.log2(SceneDepth*VolumetricFog_GridZParams.x+VolumetricFog_GridZParams.y)*VolumetricFog_GridZParams.z + Offset);
    }

    private static Vector3f ComputeCellWorldPosition(Vector3f GridCoordinate, Vector3f CellOffset)
    {
//        float2 VolumeUV = (GridCoordinate.xy + CellOffset.xy) / VolumetricFog_GridSize.xy;
//        float2 VolumeNDC = (VolumeUV * 2 - 1) /* float2(1, -1)*/;

        float VolumeU = (GridCoordinate.x + CellOffset.x) / VolumetricFog_GridSize.x;
        float VolumeV = (GridCoordinate.y + CellOffset.y) / VolumetricFog_GridSize.y;
        VolumeU = VolumeU * 2 - 1;
        VolumeV = VolumeV * 2 - 1;

        float SceneDepth = ComputeDepthFromZSlice(GridCoordinate.z + CellOffset.z);

        float TileDeviceZ = ConvertToDeviceZ(-SceneDepth);
        Vector4f CenterPosition = //mul(float4(VolumeNDC, TileDeviceZ, 1), UnjitteredClipToTranslatedWorld);
         Matrix4f.transform(g_ProjViewInv, new Vector4f(VolumeU, VolumeV, TileDeviceZ, 1), null);
//        return CenterPosition.xyz / CenterPosition.w - View_PreViewTranslation;
        CenterPosition.scale(1.0f/CenterPosition.w);

        return new Vector3f(CenterPosition);
    }

    @Override
    public void initUI() {
        mTweakBar.addValue("Enable Volumetric Fog", createControl("m_EnableVolumetricFog"));
        mTweakBar.addValue("Scattering Intensity", createControl("m_intensity"), 0.1f, 10);
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
            m_Params.NearClippingDistance = 0.1f;
            m_Params.VolumetricFogDistance = m_Params.cameraFar+200;
//            m_frameAttribs.outputTexture = null;
//            m_frameAttribs.viewport.set(0,0, getGLContext().width(), getGLContext().height());
            m_Params.view = m_Scene.getViewMat();
            m_Params.proj = m_Scene.getProjMat();
//            m_frameAttribs.fov =     m_Scene.getFovInRadian();

            m_Params.shadowMap = m_Scene.getShadowMap();
            m_Params.resetLights();
            switch (m_Scene.getLightMode()){
                case DIRECTIONAL:
                    m_Params.addDirectionLight(m_Scene.getLightDir(), Vector3f.ONE, m_intensity,
                            m_Scene.getLightViewMat(), m_Scene.getLightProjMat(), m_Scene.getShadowMap());
                    break;
                case POINT:
                    m_Params.addPointLight(m_Scene.getLightPos(), m_Scene.getLightFarlane(), new Vector3f(0.904016f, 0.843299f, 0.70132f), m_intensity,
                            m_Scene.getCubeLightViewMats(), m_Scene.getLightProjMat(), m_Scene.getShadowMap());
                    break;
                case SPOT:
                    m_Params.addSpotLight(m_Scene.getLightPos(), m_Scene.getLightFarlane(), m_Scene.getFovInRadian(),
                            m_Scene.getLightDir(), Vector3f.ONE, m_intensity,
                            m_Scene.getLightViewMat(), m_Scene.getLightProjMat(), m_Scene.getShadowMap());
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
