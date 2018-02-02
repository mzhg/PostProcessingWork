package jet.opengl.demos.intel.avsm;

import com.nvidia.developer.opengl.app.NvSampleApp;

import jet.opengl.postprocessing.common.GLCheck;

/**
 * Created by mazhen'gui on 2018/1/13.
 */

public class AVSMDemo extends NvSampleApp {

    private AVSMSampler m_Sampler;

    @Override
    protected void initRendering() {
        m_Sampler = new AVSMSampler();
        m_Sampler.setNVApp(this);
        m_Sampler.initScene();
    }

    @Override
    public void initUI() {
        m_Sampler.onCreateUI(mTweakBar);
    }

    @Override
    public void display() {
        m_Sampler.draw(false, true);
        GLCheck.checkError();

        GLCheck.checkError();
    }

    @Override
    protected void reshape(int width, int height) {
        if(width <=0 || height <=0){
            return;
        }

        m_Sampler.onResize(width, height);
    }

    @Override
    public boolean handleCharacterInput(char c) {
        if(m_Sampler != null)
            m_Sampler.handleCharacterInput(c);

        return false;
    }

    @Override
    public void onDestroy() {
        m_Sampler.dispose();
    }
}
