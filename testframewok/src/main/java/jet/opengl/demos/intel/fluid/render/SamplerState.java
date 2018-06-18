package jet.opengl.demos.intel.fluid.render;

import jet.opengl.postprocessing.common.GLenum;

/**
 * Created by Administrator on 2018/3/13 0013.
 */

public class SamplerState {
    enum FilterE
    {
        FILTER_NO_MIPMAP,   ///< Disable MIPmap.
        FILTER_LINEAR   ,   ///< Linearly interpolate between samples.
        FILTER_NEAREST  ,   ///< Use nearest sample.
    } ;

    enum AddressE
    {
        ADDRESS_CLAMP   ,   /// Texture coordinates outside [0,1] set to 0 or 1.
        ADDRESS_REPEAT  ,   /// Texture coordinates outside [0,1] repeat.
    } ;

    enum CombineOperationE
    {
        COMBO_OP_REPLACE    ,   ///< Replace
        COMBO_OP_MODULATE   ,   ///< Multiply material colors. Default.
        //COMBO_OP_DECAL      ,   ///< Overwrite or blend color with texture.  Keep alpha as-is.
    } ;

    public FilterE   mMinFilter = FilterE.FILTER_LINEAR; ///< Filter mode used to interpret texture coordinates not exactly centered on a texel. Default is FILTER_LINEAR.
    public FilterE   mMagFilter  = FilterE.FILTER_LINEAR         ; ///< Filter mode used to interpret texture coordinates not exactly centered on a texel. Default is FILTER_LINEAR.
    public FilterE   mMipFilter  = FilterE.FILTER_LINEAR         ; ///< Filter mode used to interpret texture coordinates not exactly centered on a texel. Default is FILTER_LINEAR.
    public AddressE   mAddressU  = AddressE.ADDRESS_CLAMP         ; ///< Address mode used to interpret texture coordinates outside [0,1]. Default is ADDRESS_CLAMP.
    public AddressE   mAddressV  = AddressE.ADDRESS_CLAMP         ; ///< Address mode used to interpret texture coordinates outside [0,1]. Default is ADDRESS_CLAMP.
    public AddressE   mAddressW  = AddressE.ADDRESS_CLAMP         ; ///< Address mode used to interpret texture coordinates outside [0,1]. Default is ADDRESS_CLAMP.
    public CombineOperationE   mCombineOperation = CombineOperationE.COMBO_OP_MODULATE  ; ///< Operation used to combine multiple materials.  Default is OP_MODULATE.
}
