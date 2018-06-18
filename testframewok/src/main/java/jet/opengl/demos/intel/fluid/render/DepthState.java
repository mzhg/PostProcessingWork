package jet.opengl.demos.intel.fluid.render;

/**
 * Created by Administrator on 2018/4/5 0005.
 */

public class DepthState {
    public CompareFuncE    mDepthFunc          ;   ///< Whether to enable depth test, and if so, which test to use.  Default is LESS.
    public boolean         mDepthWriteEnabled  ;   ///< Whether to write to the depth buffer. Default is true.
    public int             mDepthBias          ;   ///< Depth value added to each pixel. Default is 0.
    public boolean         mDepthClip          ;   ///< Whether to enable depth-clipping. Default is true.

    public DepthState()
    {
        mDepthFunc          = CompareFuncE.CMP_FUNC_LESS ;
        mDepthWriteEnabled  = true ;
        mDepthBias          = 0 ;
        mDepthClip          = true ;
    }

    public void set(DepthState ohs){
        mDepthFunc = ohs.mDepthFunc;
        mDepthWriteEnabled = ohs.mDepthWriteEnabled;
        mDepthBias = ohs.mDepthBias;
        mDepthClip = ohs.mDepthClip;
    }
}
