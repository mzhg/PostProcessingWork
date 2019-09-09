package jet.opengl.renderer.Unreal4.scenes;

/** Blend modes supported for simple element rendering */
public enum ESimpleElementBlendMode {
    SE_BLEND_Opaque,
    SE_BLEND_Masked,
    SE_BLEND_Translucent,
    SE_BLEND_Additive,
    SE_BLEND_Modulate,
    SE_BLEND_MaskedDistanceField,
    SE_BLEND_MaskedDistanceFieldShadowed,
    SE_BLEND_TranslucentDistanceField,
    SE_BLEND_TranslucentDistanceFieldShadowed,
    SE_BLEND_AlphaComposite,
    SE_BLEND_AlphaHoldout,
    // Like SE_BLEND_Translucent, but modifies destination alpha
    SE_BLEND_AlphaBlend,
    // Like SE_BLEND_Translucent, but reads from an alpha-only texture
    SE_BLEND_TranslucentAlphaOnly,
    SE_BLEND_TranslucentAlphaOnlyWriteAlpha,

    SE_BLEND_RGBA_MASK_START,
    SE_BLEND_RGBA_MASK_END /*= SE_BLEND_RGBA_MASK_START + 31*/, //Using 5bit bit-field for red, green, blue, alpha and desaturation
}
