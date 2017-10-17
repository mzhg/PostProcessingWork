package jet.opengl.demos.intel.antialiasing;

import com.nvidia.developer.opengl.app.NvSampleApp;
import com.nvidia.developer.opengl.ui.NvUIEventResponse;
import com.nvidia.developer.opengl.ui.NvUIReaction;

import jet.opengl.demos.scenes.WireSphere;
import jet.opengl.postprocessing.common.GLCheck;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.texture.Texture2D;

/**
 * Created by mazhen'gui on 2017/10/17.
 */

public final class AntiAliasingDemo extends NvSampleApp {
    private WireSphere mScene;
    private CMAAEffect mCMAA;
    private SAAPostProcessGPU mSAA;
    private SMAAEffect mSMAA;
    private float mTotalTime;

    private boolean mEnablePostProcessing;
    private boolean mSaveScreenTex;

    @Override
    public void initUI() {
        mScene.onCreateUI(mTweakBar);
        mTweakBar.addValue("EnablAntiAliasing", createControl("mEnablePostProcessing"));
        mTweakBar.addButton("Save Screen Texture", 1);
    }

    @Override
    protected void initRendering() {
        mScene = new WireSphere();
        mScene.setNVApp(this);
        mScene.initScene();

        mCMAA = new CMAAEffect();
        mCMAA.OnCreate();

        mSAA = new SAAPostProcessGPU();
        mSAA.OnCreate();

        mSMAA = new SMAAEffect();
        mSMAA.OnCreate(getGLContext().width(), getGLContext().height());
        GLCheck.checkError();
    }

    @Override
    public void display() {
        mScene.draw(true, true);

        if(mSaveScreenTex){
            System.out.println("-------------");
            mSaveScreenTex = false;
        }

        if(mEnablePostProcessing){
            // Ad-hoc CMAA-zoom setup using g_ZoomBox - we need to think of a nicer way to do this perhaps :)
            // The only problem is that the DbgDisplayZoomBoxPS from CMAA.hlsl displays detailed debug info on edges which needs
            // access to the algorithm buffers and constants so that's why it's not done using the g_ZoomBox
            Texture2D zoomBoxColourTex = null;
            Texture2D zoomBoxEdgesInfoTex = null;
//        if( CPUT_CHECKBOX_CHECKED == mpShowZoom->GetCheckboxState() )
//        {
//            zoomBoxColourTex    = g_CPUTZoomBox.GetZoomColourTxtNoAddRef();
//            zoomBoxEdgesInfoTex = g_CPUTZoomBox.GetZoomEdgeTxtNoAddRef();
//        }
            float EdgeThreshold = 1.0f / 13.0f;
            float NonDominantEdgeRemovalAmount = 0.35f;
            Texture2D m_SceneColor2SRV_UNORM = mScene.getSceneColorTex();
            mCMAA.Draw( /*mpContext,*/ mTotalTime, EdgeThreshold, NonDominantEdgeRemovalAmount,
//                m_SceneColor2->GetColorResourceView(), m_SceneColor2SRV_UNORM,
                    mScene.getSceneColorTex(), m_SceneColor2SRV_UNORM,
                    mScene.getSceneColorTex(), null,
//                (CPUT_CHECKBOX_CHECKED == mpShowEdge->GetCheckboxState())?(CMAAEffect::DT_ShowEdges):(CMAAEffect::DT_Normal),
                    CMAAEffect.DT_Normal,
                    zoomBoxColourTex, zoomBoxEdgesInfoTex );
        }

        mScene.resoveMultisampleTexture(GLenum.GL_COLOR_BUFFER_BIT);

        GLCheck.checkError();
        mTotalTime += getFrameDeltaTime();
    }

    @Override
    protected int handleReaction(NvUIReaction react) {
        if(react.code  == 1){
            mSaveScreenTex = true;
            return NvUIEventResponse.nvuiEventHandled;
        }else
            return super.handleReaction(react);
    }

    @Override
    protected void reshape(int width, int height) {
        if(width <=0 || height <=0)
            return;

        mScene.onResize(width, height);

        mCMAA.OnSize(width, height);
        mSAA.OnSize(width, height);
        mSMAA.OnSize(width, height);
        GLCheck.checkError();
    }

    @Override
    public void onDestroy() {
        mScene.dispose();
    }
}
