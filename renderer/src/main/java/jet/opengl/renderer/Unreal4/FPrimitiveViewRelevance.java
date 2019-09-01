package jet.opengl.renderer.Unreal4;

/**
 * The different types of relevance a primitive scene proxy can declare towards a particular scene view.
 * the class is only storing bits, and has an |= operator
 */
public class FPrimitiveViewRelevance {
    // Warning: This class is memzeroed externally as 0 is assumed a
    // valid value for all members meaning 'not relevant'. If this
    // changes existing class usage should be re-evaluated

    // from FMaterialRelevance (could be made the base class):

    /** The LightingProfile supported by this primitive, as a bitmask. */
    public short ShadingModelMaskRelevance;
    /** The primitive has one or more opaque or masked elements. */
    public boolean bOpaqueRelevance;
    /** The primitive has one or more masked elements. */
    public boolean bMaskedRelevance;
    /** The primitive has one or more translucent elements which output velocity. */
    public boolean bOutputsTranslucentVelocityRelevance;
    /** The primitive has one or more distortion elements. */
    public boolean bDistortionRelevance;
    /** The primitive has one or more elements that have SeparateTranslucency. */
    public boolean bSeparateTranslucencyRelevance;
    /** The primitive has one or more elements that have normal translucency. */
    public boolean bNormalTranslucencyRelevance;
    /** For translucent primitives reading the scene color. */
    public boolean bUsesSceneColorCopy;
    /** For primitive that can't render in offscreen buffers (blend modulate). */
    public boolean bDisableOffscreenRendering;
    /** */
    public boolean bUsesGlobalDistanceField;

    // others:

    /** The primitive's static elements are rendered for the view. */
    public boolean bStaticRelevance;
    /** The primitive's dynamic elements are rendered for the view. */
    public boolean bDynamicRelevance;
    /** The primitive is drawn. */
    public boolean bDrawRelevance;
    /** The primitive is casting a shadow. */
    public boolean bShadowRelevance;
    /** The primitive should render velocity. */
    public boolean bVelocityRelevance;
    /** The primitive should render to the custom depth pass. */
    public boolean bRenderCustomDepth;
    /** The primitive should render to the base pass / normal depth / velocity rendering. */
    public boolean bRenderInMainPass;
    /** The primitive has materials using the volume domain. */
    public boolean bHasVolumeMaterialDomain;
    /** The primitive is drawn only in the editor and composited onto the scene after post processing */
    public boolean bEditorPrimitiveRelevance;
    /** The primitive's static elements are selected and rendered again in the selection outline pass*/
    public boolean bEditorStaticSelectionRelevance;
    /** The primitive is drawn only in the editor and composited onto the scene after post processing using no depth testing */
    public boolean bEditorNoDepthTestPrimitiveRelevance;
    /** The primitive should have GatherSimpleLights called on the proxy when gathering simple lights. */
    public boolean bHasSimpleLights;
    /** The primitive has one or more elements that have World Position Offset. */
    public boolean bUsesWorldPositionOffset;
    /** Whether the primitive uses non-default lighting channels. */
    public boolean bUsesLightingChannels;
    /** */
    public boolean bDecal;
    /** Whether the primitive has materials that use translucent surface lighting. */
    public boolean bTranslucentSurfaceLighting;
    /** Whether the primitive has materials that use volumetric translucent self shadow. */
    public boolean bTranslucentSelfShadow;
    /** Whether the primitive has materials that read the scene depth. */
    public boolean bUsesSceneDepth;
    /** Whether the primitive has materials that read the scene depth. */
    public boolean bUsesSkyMaterial;
    /** Whether the view use custom data. */
    public boolean bUseCustomViewData;

    /**
     * Whether this primitive view relevance has been initialized this frame.
     * Primitives that have not had ComputeRelevanceForView called on them (because they were culled) will not be initialized,
     * But we may still need to render them from other views like shadow passes, so this tracks whether we can reuse the cached relevance or not.
     */
    public boolean bInitializedThisFrame;

    public boolean HasTranslucency()
    {
        return bSeparateTranslucencyRelevance || bNormalTranslucencyRelevance;
    }

    /** Default constructor */
    public FPrimitiveViewRelevance()
    {
        // the class is only storing bits, the following avoids code redundancy
        /*uint8 * RESTRICT p = (uint8*)this;
        for(uint32 i = 0; i < sizeof(*this); ++i)
        {
			*p++ = 0;
        }*/

        // only exceptions (bugs we need to fix?):

        bOpaqueRelevance = true;
        // without it BSP doesn't render
        bRenderInMainPass = true;
    }

    /** Bitwise OR operator.  Sets any relevance bits which are present in either. */
    public FPrimitiveViewRelevance OrSelf(FPrimitiveViewRelevance B){
        throw new UnsupportedOperationException();
    }

    /** Bitwise OR operator.  Sets any relevance bits which are present in either. */
    /*FPrimitiveViewRelevance& operator|=(const FPrimitiveViewRelevance& B)
    {
        // the class is only storing bits, the following avoids code redundancy
		const uint8 * RESTRICT s = (const uint8*)&B;
        uint8 * RESTRICT d = (uint8*)this;
        for(uint32 i = 0; i < sizeof(*this); ++i)
        {
			*d = *d | *s;
            ++s;++d;
        }
        return *this;
    }*/
}
