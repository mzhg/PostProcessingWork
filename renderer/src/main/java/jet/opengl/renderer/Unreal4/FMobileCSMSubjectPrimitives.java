package jet.opengl.renderer.Unreal4;

import java.util.ArrayList;

import jet.opengl.renderer.Unreal4.utils.TBitArray;

/** Stores a list of CSM shadow casters. Used by mobile renderer for culling primitives receiving static + CSM shadows. */
public class FMobileCSMSubjectPrimitives {
    /** List of this light's shadow subject primitives. */
    protected final TBitArray ShadowSubjectPrimitivesEncountered = new TBitArray();
    protected final ArrayList<FPrimitiveSceneInfo> ShadowSubjectPrimitives = new ArrayList<>();

    /** Adds a subject primitive */
    public void AddSubjectPrimitive(FPrimitiveSceneInfo PrimitiveSceneInfo, int PrimitiveId)
    {
//        checkSlow(PrimitiveSceneInfo->GetIndex() == PrimitiveId);
		final int PrimitiveIndex = PrimitiveSceneInfo.GetIndex();
        if (!ShadowSubjectPrimitivesEncountered.Get(PrimitiveId))
        {
            ShadowSubjectPrimitives.add(PrimitiveSceneInfo);
//            ShadowSubjectPrimitivesEncountered[PrimitiveId] = true;
            ShadowSubjectPrimitivesEncountered.Set(PrimitiveId, true);
        }
    }

    /** Returns the list of subject primitives */
	public ArrayList<FPrimitiveSceneInfo> GetShadowSubjectPrimitives()
    {
        return ShadowSubjectPrimitives;
    }

    /** Used to initialize the ShadowSubjectPrimitivesEncountered bit array
     * to prevent shadow primitives being added more than once. */
    public void InitShadowSubjectPrimitives(int PrimitiveCount)
    {
        ShadowSubjectPrimitivesEncountered.Init(false, PrimitiveCount);
    }
}
