package jet.opengl.demos.intel.fluid.render;

/**
 * Created by Administrator on 2018/4/5 0005.
 */

public class RasterState {
    public enum FillModeE
    {
        FILL_MODE_POINT     ,   ///< Draw vertices as points
        FILL_MODE_WIREFRAME ,   ///< Draw triangle edges
        FILL_MODE_SOLID     ,   ///< Draw triangle faces.
    } ;

    public enum CullModeE
    {
        CULL_MODE_NONE  ,   ///< Draw all triangles. Disable culling.
        CULL_MODE_FRONT ,   ///< Do not draw front-facing (counter-clockwise) triangles
        CULL_MODE_BACK  ,   ///< Do not draw back-facing (clockwise) triangles.
    } ;

    public FillModeE       mFillMode;   ///< How to draw primitives. Default is FILL_MODE_SOLID.
    public CullModeE       mCullMode;   ///< Which faces, if any, to cull.  Default is CULL_MODE_BACK.
    public boolean         mMultiSampleEnabled ;   ///< Whether to enable multi-sample anti-aliasing.  Default is false.
    public boolean         mSmoothLines        ;   ///< Whether to use smooth-line rendering.  Default is false.

    public RasterState()
    {
        mFillMode           = FillModeE.FILL_MODE_SOLID ;
        mCullMode           = CullModeE.CULL_MODE_BACK ;
        mMultiSampleEnabled = false ;
        mSmoothLines        = false ;
    }

    public void set(RasterState ohs){
        mFillMode = ohs.mFillMode;
        mCullMode = ohs.mCullMode;
        mMultiSampleEnabled = ohs.mMultiSampleEnabled;
        mSmoothLines = ohs.mSmoothLines;
    }
}
