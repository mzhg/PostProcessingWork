package jet.opengl.demos.nvidia.waves.crest;

import com.nvidia.developer.opengl.app.NvCameraMotionType;
import com.nvidia.developer.opengl.app.NvSampleApp;

import org.lwjgl.util.vector.Matrix4f;

import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;

public class Wave_CDClipmap_Test extends NvSampleApp {

    private  Wave_CDClipmap_Params mParams = new Wave_CDClipmap_Params();
    private Wave_CDClipmap mCDClipmap;

    private final Matrix4f mProj = new Matrix4f();
    private final Matrix4f mView = new Matrix4f();

    private GLFuncProvider gl;
    protected void initRendering(){
        mCDClipmap = new Wave_CDClipmap();
        mCDClipmap.init(mParams);

        gl = GLFuncProviderFactory.getGLFuncProvider();

        m_transformer.setMotionMode(NvCameraMotionType.FIRST_PERSON);
        m_transformer.setTranslation(0, -0.1f, 0.1f);
    }

    @Override
    public void display() {
        gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, 0);
        gl.glClearColor(1,0,0,0);
        gl.glClearDepthf(1);
        gl.glClear(GLenum.GL_COLOR_BUFFER_BIT|GLenum.GL_DEPTH_BUFFER_BIT);

        m_transformer.getModelViewMat(mView);

        mCDClipmap.debugDrawWave(mProj, mView);
    }

    @Override
    protected void reshape(int width, int height) {
        if(width <= 0 || height <= 0)
            return;

        gl.glViewport(0,0, width, height);
        Matrix4f.perspective(60, (float)width/height, 0.1f, 1000, mProj);
    }
}
