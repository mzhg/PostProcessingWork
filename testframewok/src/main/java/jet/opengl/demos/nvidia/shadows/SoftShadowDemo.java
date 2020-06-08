package jet.opengl.demos.nvidia.shadows;

import com.nvidia.developer.opengl.app.NvSampleApp;
import com.nvidia.developer.opengl.ui.NvUIReaction;

import jet.opengl.demos.scene.BaseScene;
import jet.opengl.postprocessing.common.GLCheck;

/**
 * Created by mazhen'gui on 2017/11/6.
 */
public final class SoftShadowDemo extends NvSampleApp {
    private BaseScene mScene;
    private ShadowConfig mShadowConfig = new ShadowConfig();

    @Override
    public void initUI() {
        mScene.onCreateUI(mTweakBar);
    }

    @Override
    protected void initRendering() {
        mShadowConfig.shadowType = ShadowMapGenerator.ShadowType.NONE;
        mScene = new SoftShadowScene(new ShadowMapGenerator());
//        mScene = new VarianceShadowScene();
        mScene.setNVApp(this);
        mScene.initScene();

        getGLContext().setSwapInterval(0);
        GLCheck.checkError();
    }

    @Override
    public void display() {
        mScene.draw(false, true);
//        mScene.resoveMultisampleTexture(GLenum.GL_COLOR_BUFFER_BIT);
        GLCheck.checkError();
    }

    @Override
    protected int handleReaction(NvUIReaction react) {

        return super.handleReaction(react);
    }

    @Override
    protected void reshape(int width, int height) {
        if(width <=0 || height <=0)
            return;

        mScene.onResize(width, height);
        GLCheck.checkError();
    }

    @Override
    public void onDestroy() {
        mScene.dispose();
    }
}
