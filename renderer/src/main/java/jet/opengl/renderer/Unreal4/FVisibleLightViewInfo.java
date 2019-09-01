package jet.opengl.renderer.Unreal4;

import java.util.ArrayList;

import jet.opengl.renderer.Unreal4.utils.TBitArray;

/** Information about a visible light which is specific to the view it's visible in. */
public class FVisibleLightViewInfo {
    /** The dynamic primitives which are both visible and affected by this light. */
    final ArrayList<FPrimitiveSceneInfo> VisibleDynamicLitPrimitives = new ArrayList<>();

    /** Whether each shadow in the corresponding FVisibleLightInfo::AllProjectedShadows array is visible. */
    final TBitArray ProjectedShadowVisibilityMap = new TBitArray();

    /** The view relevance of each shadow in the corresponding FVisibleLightInfo::AllProjectedShadows array. */
    final ArrayList<FPrimitiveViewRelevance> ProjectedShadowViewRelevanceMap = new ArrayList<>();

    /** true if this light in the view frustum (dir/sky lights always are). */
    boolean bInViewFrustum;

    /** List of CSM shadow casters. Used by mobile renderer for culling primitives receiving static + CSM shadows */
    final FMobileCSMSubjectPrimitives MobileCSMSubjectPrimitives = new FMobileCSMSubjectPrimitives();
}
