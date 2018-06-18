package jet.opengl.demos.intel.fluid.render;

/**
 * Created by Administrator on 2018/4/5 0005.
 */

public class AlphaState {
    public boolean         mAlphaTest  ;   ///< Whether to enable alpha-test.  Default is false.
    public CompareFuncE    mAlphaFunc  ;   ///< Test to use to accept or reject pixel, based on its alpha value.  Default is GREATEREQUAL.
    public float           mAlphaRef   ;   ///< Value to use for the current/source alpha during alpha-test comparisons.  Default is 0.

    public AlphaState()
    {
        mAlphaTest  = false ;
        mAlphaFunc  = CompareFuncE.CMP_FUNC_GREATER_EQUAL ;
        mAlphaRef   = 0.0f ;
    }

    public void set(AlphaState ohs){
        mAlphaTest = ohs.mAlphaTest;
        mAlphaFunc = ohs.mAlphaFunc;
        mAlphaRef = ohs.mAlphaRef;
    }
}
