package jet.opengl.renderer.Unreal4;

import java.util.ArrayList;

/** Information about a visible light which isn't view-specific. */
public class FVisibleLightInfo {
    /** Projected shadows allocated on the scene rendering mem stack. */
    final ArrayList<FProjectedShadowInfo> MemStackProjectedShadows = new ArrayList<>();

    /** All visible projected shadows, output of shadow setup.  Not all of these will be rendered. */
    final ArrayList<FProjectedShadowInfo> AllProjectedShadows = new ArrayList<>();

    /** Shadows to project for each feature that needs special handling. */
    final ArrayList<FProjectedShadowInfo> ShadowsToProject = new ArrayList<>();
    final ArrayList<FProjectedShadowInfo> CapsuleShadowsToProject = new ArrayList<>();
    final ArrayList<FProjectedShadowInfo> RSMsToProject = new ArrayList<>();

    /** All visible projected preshdows.  These are not allocated on the mem stack so they are refcounted. */
    final ArrayList<FProjectedShadowInfo> ProjectedPreShadows = new ArrayList<>();

    /** A list of per-object shadows that were occluded. We need to track these so we can issue occlusion queries for them. */
    final ArrayList<FProjectedShadowInfo> OccludedPerObjectShadows = new ArrayList<>();
}
