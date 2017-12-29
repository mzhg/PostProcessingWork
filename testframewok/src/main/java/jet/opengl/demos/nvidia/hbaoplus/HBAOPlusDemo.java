package jet.opengl.demos.nvidia.hbaoplus;

import com.nvidia.developer.opengl.app.NvSampleApp;
import com.nvidia.developer.opengl.ui.NvTweakEnumi;

import jet.opengl.demos.scenes.CubeScene;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;

/**
 * Created by mazhen'gui on 2017/12/9.
 */

public final class HBAOPlusDemo extends NvSampleApp {
    private GLFuncProvider gl;
    private CubeScene m_Scene;
    private HBAOPlusPostProcess m_SSAO;

    private final GFSDK_SSAO_InputData_GL input = new GFSDK_SSAO_InputData_GL();
    private final GFSDK_SSAO_Parameters AOParams = new GFSDK_SSAO_Parameters();
    private final GFSDK_SSAO_Output_GL output = new GFSDK_SSAO_Output_GL();

    private boolean mEnableSSAO = true;

    @Override
    public void initUI() {
        // Effect quality; -1 - lowest (low, half res checkerboard), 0 - low, 1 - medium, 2 - high, 3 - very high / adaptive; each quality level is roughly 2x more costly than the previous, except the q3 which is variable but, in general, above q2.
        // ASSAO quality
        NvTweakEnumi assaoQualities[] =
        {
                new NvTweakEnumi( "Lowest", -1 ),
                new NvTweakEnumi( "Low", 0 ),
                new NvTweakEnumi( "Medium", 1 ),
                new NvTweakEnumi( "High", 2 ),
                new NvTweakEnumi( "Adaptive", 3 ),
        };

//        mTweakBar.addEnum("ASSAO Quality", createControl("QualityLevel", m_Settings), assaoQualities, 1);

        mTweakBar.addValue("Enable SSAO", createControl("mEnableSSAO"));
    }

    @Override
    protected void initRendering() {
        getGLContext().setSwapInterval(0);
        gl = GLFuncProviderFactory.getGLFuncProvider();
        m_Scene = new CubeScene(m_transformer);
        m_Scene.onCreate();

        m_SSAO = new HBAOPlusPostProcess();
        m_SSAO.create();

//		m_Settings.QualityLevel = 3;
    }

    @Override
    public void display() {
        m_Scene.draw();
        m_Scene.resoveMultisampleTexture();

        if(!mEnableSSAO)
            return;

        input.depthData.depthTextureType = GFSDK_SSAO_DepthTextureType.GFSDK_SSAO_HARDWARE_DEPTHS;
        input.depthData.fullResDepthTexture = m_Scene.getSceneDepth();
        input.depthData.projectionMatrix.load(m_Scene.getProjMat());
        input.depthData.metersToViewSpaceUnits = 0.005f;
        input.depthData.near = m_Scene.getSceneNearPlane();
        input.depthData.far  = m_Scene.getSceneFarPlane();

        input.normalData.enable = false;

        output.outputFBO = 0;  // screen
        output.blend.mode = GFSDK_SSAO_BlendMode.GFSDK_SSAO_MULTIPLY_RGB;

        AOParams.radius = 2.f;
        AOParams.bias = 0.2f;
        AOParams.smallScaleAO = 1.f;
        AOParams.largeScaleAO = 1.f;
        AOParams.powerExponent = 2.f;
        AOParams.blur.enable = false;
        AOParams.blur.sharpness = 32.f;
        AOParams.blur.radius = GFSDK_SSAO_BlurRadius.GFSDK_SSAO_BLUR_RADIUS_4;

        m_SSAO.performancePostProcessing(input, AOParams, output, GFSDK_SSAO_RenderMask.GFSDK_SSAO_RENDER_AO);

        gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, 0);
        gl.glDisable(GLenum.GL_BLEND);
        gl.glViewport(0, 0, getGLContext().width(), getGLContext().height());
    }

    @Override
    protected void reshape(int width, int height) {
        if(width ==0 || height == 0)
            return;

        gl.glViewport(0, 0, width, height);
        m_Scene.onResize(width, height);

    }
}
