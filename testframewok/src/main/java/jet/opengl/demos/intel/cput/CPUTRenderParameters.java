package jet.opengl.demos.intel.cput;

/**
 * Created by mazhen'gui on 2017/11/13.
 */

public class CPUTRenderParameters {
    public boolean         mShowBoundingBoxes;
    public boolean         mDrawModels = true;
    public boolean         mRenderOnlyVisibleModels = true;
    public CPUTCamera      mpCamera;
    public CPUTCamera      mpShadowCamera;
    public AVSMMethod      mAVSMMethod;

    public int             mWidth;
    public int             mHeight;
    public CPUTBuffer      mpPerModelConstants;
    public CPUTBuffer      mpPerFrameConstants;
}
