package jet.opengl.demos.nvidia.waves.crest;

import com.nvidia.developer.opengl.app.NvCameraMotionType;
import com.nvidia.developer.opengl.app.NvSampleApp;

import org.lwjgl.util.vector.Matrix4f;

import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;

public class Wave_Animation_Test extends NvSampleApp {

    private Wave_CDClipmap_Params m_Clipmap_Params = new Wave_CDClipmap_Params();
    private Wave_CDClipmap mCDClipmap;

    private Wave_Simulation_Params m_Simulation_Params = new Wave_Simulation_Params();
    private Wave_Simulation mAnimation;

    private Wave_Renderer m_Renderer;

    private final Matrix4f mProj = new Matrix4f();
    private final Matrix4f mView = new Matrix4f();

    private GLFuncProvider gl;
    protected void initRendering(){
        getGLContext().setSwapInterval(0);

        mCDClipmap = new Wave_CDClipmap();
        mCDClipmap.init(m_Clipmap_Params);

        m_Renderer = new Wave_Renderer();
        m_Renderer.init(mCDClipmap, mAnimation);

        gl = GLFuncProviderFactory.getGLFuncProvider();

        m_transformer.setMotionMode(NvCameraMotionType.FIRST_PERSON);
        m_transformer.setTranslation(0, -5.1f, 0.1f);
    }

    @Override
    public void display() {
        gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, 0);
        gl.glClearColor(1,0,0,0);
        gl.glClearDepthf(1);
        gl.glClear(GLenum.GL_COLOR_BUFFER_BIT|GLenum.GL_DEPTH_BUFFER_BIT);

        m_transformer.getModelViewMat(mView);

        mCDClipmap.updateWave(mView);
        m_Renderer.waveShading(mProj, mView, true);
    }

    @Override
    protected void reshape(int width, int height) {
        if(width <= 0 || height <= 0)
            return;

        gl.glViewport(0,0, width, height);
        Matrix4f.perspective(60, (float)width/height, 0.1f, 1000, mProj);
    }
}
