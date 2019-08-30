package jet.opengl.renderer.Unreal4;

import java.util.ArrayList;

/** Information about a visible light which is specific to the view it's visible in. */
public class FVisibleLightViewInfo {
    /** The dynamic primitives which are both visible and affected by this light. */
    final ArrayList<FPrimitiveSceneInfo> VisibleDynamicLitPrimitives = new ArrayList<>();

    /** Whether each shadow in the corresponding FVisibleLightInfo::AllProjectedShadows array is visible. */
    FSceneBitArray ProjectedShadowVisibilityMap;

    /** The view relevance of each shadow in the corresponding FVisibleLightInfo::AllProjectedShadows array. */
    TArray<FPrimitiveViewRelevance,SceneRenderingAllocator> ProjectedShadowViewRelevanceMap;

    /** true if this light in the view frustum (dir/sky lights always are). */
    boolean bInViewFrustum = false;

    /** List of CSM shadow casters. Used by mobile renderer for culling primitives receiving static + CSM shadows */
    FMobileCSMSubjectPrimitives MobileCSMSubjectPrimitives;
}
