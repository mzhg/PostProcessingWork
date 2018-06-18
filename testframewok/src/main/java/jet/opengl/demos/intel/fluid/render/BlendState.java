package jet.opengl.demos.intel.fluid.render;

/**
 * Created by Administrator on 2018/4/5 0005.
 */

public class BlendState {
    public enum BlendFactorE
    {
        BLEND_FACTOR_ZERO           ,
        BLEND_FACTOR_ONE            ,
        BLEND_FACTOR_SRC_ALPHA      ,
        BLEND_FACTOR_SRC_COLOR      ,
        BLEND_FACTOR_INV_SRC_ALPHA  ,
        BLEND_FACTOR_INV_SRC_COLOR  ,
        BLEND_FACTOR_DST_ALPHA      ,
        BLEND_FACTOR_DST_COLOR      ,
        BLEND_FACTOR_INV_DST_ALPHA  ,
        BLEND_FACTOR_INV_DST_COLOR  ,
    } ;

    public enum BlendOpE
    {
        BLEND_OP_ADD        ,
        BLEND_OP_SUBTRACT   ,
        BLEND_OP_MIN        ,
        BLEND_OP_MAX        ,
    } ;

    public boolean         mBlendEnabled   ;   ///< Whether to perform alpha blending.  Default is false.
    public BlendFactorE    mBlendSrcColor  ;   ///< Blend factor for source term.  Default is one.
    public BlendFactorE    mBlendDstColor  ;   ///< Blend factor for destination term.  Default is zero.
    public BlendOpE        mBlendOpColor   ;   ///< Blend operation to perform to combine src and dst terms.  Default is add.
    public BlendFactorE    mBlendSrcAlpha  ;   ///< Blend factor for source term.  Default is one.
    public BlendFactorE    mBlendDstAlpha  ;   ///< Blend factor for destination term.  Default is zero
    public BlendOpE        mBlendOpAlpha   ;   ///< Blend operation to perform to combine src and dst terms.  Default is add.

    public void set(BlendState ohs){
        mBlendEnabled = ohs.mBlendEnabled;
        mBlendSrcColor = ohs.mBlendSrcColor;
        mBlendDstColor = ohs.mBlendDstColor;
        mBlendOpColor = ohs.mBlendOpColor;
        mBlendSrcAlpha = ohs.mBlendSrcAlpha;
        mBlendDstAlpha = ohs.mBlendDstAlpha;
        mBlendOpAlpha = ohs.mBlendOpAlpha;
    }

    public BlendState()
    {
        mBlendEnabled  = false              ;
        mBlendSrcColor = BlendFactorE.BLEND_FACTOR_ONE   ;
        mBlendDstColor = BlendFactorE.BLEND_FACTOR_ZERO  ;
        mBlendOpColor  = BlendOpE.BLEND_OP_ADD       ;
        mBlendSrcAlpha = BlendFactorE.BLEND_FACTOR_ONE   ;
        mBlendDstAlpha = BlendFactorE.BLEND_FACTOR_ZERO  ;
        mBlendOpAlpha  = BlendOpE.BLEND_OP_ADD       ;
    }

    public void setOpaque()
    {
        mBlendEnabled  = false              ;
        mBlendSrcColor = BlendFactorE.BLEND_FACTOR_ONE   ;
        mBlendDstColor = BlendFactorE.BLEND_FACTOR_ZERO  ;
        mBlendOpColor  = BlendOpE.BLEND_OP_ADD       ;
        mBlendSrcAlpha = BlendFactorE.BLEND_FACTOR_ONE   ;
        mBlendDstAlpha = BlendFactorE.BLEND_FACTOR_ZERO  ;
        mBlendOpAlpha  = BlendOpE.BLEND_OP_ADD       ;
    }

    public void setAlpha()
    {
        mBlendEnabled  = true                       ;
        mBlendSrcColor = BlendFactorE.BLEND_FACTOR_SRC_ALPHA     ;
        mBlendDstColor = BlendFactorE.BLEND_FACTOR_INV_SRC_ALPHA ;
        mBlendOpColor  = BlendOpE.BLEND_OP_ADD               ;
        mBlendSrcAlpha = BlendFactorE.BLEND_FACTOR_SRC_ALPHA     ;
        mBlendDstAlpha = BlendFactorE.BLEND_FACTOR_INV_SRC_ALPHA ;
        mBlendOpAlpha  = BlendOpE.BLEND_OP_ADD               ;
    }

    public void setAdditive()
    {
        mBlendEnabled  = true                   ;
        mBlendSrcColor = BlendFactorE.BLEND_FACTOR_SRC_ALPHA ;
        mBlendDstColor = BlendFactorE.BLEND_FACTOR_ONE       ;
        mBlendOpColor  = BlendOpE.BLEND_OP_ADD           ;
        mBlendSrcAlpha = BlendFactorE.BLEND_FACTOR_SRC_ALPHA ;
        mBlendDstAlpha = BlendFactorE.BLEND_FACTOR_ONE       ;
        mBlendOpAlpha  = BlendOpE.BLEND_OP_ADD           ;
    }
}
