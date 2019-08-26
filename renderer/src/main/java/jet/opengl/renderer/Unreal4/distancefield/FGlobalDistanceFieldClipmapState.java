package jet.opengl.renderer.Unreal4.distancefield;

import org.lwjgl.util.vector.Vector3i;

public class FGlobalDistanceFieldClipmapState {
    public FGlobalDistanceFieldClipmapState()
    {
//        FullUpdateOrigin = FIntVector::ZeroValue;
//        LastPartialUpdateOrigin = FIntVector::ZeroValue;
        CachedMaxOcclusionDistance = 0;
        CachedGlobalDistanceFieldViewDistance = 0;
        CacheMostlyStaticSeparately = true;
//        LastUsedSceneDataForFullUpdate = nullptr;

        for(int i = 0; i < Cache.length; i++)
            Cache[i] = new FGlobalDistanceFieldCacheTypeState();
    }

    public final Vector3i FullUpdateOrigin = new Vector3i();
    public final Vector3i LastPartialUpdateOrigin = new Vector3i();
    public float CachedMaxOcclusionDistance;
    public float CachedGlobalDistanceFieldViewDistance;
    public boolean CacheMostlyStaticSeparately;

    public final FGlobalDistanceFieldCacheTypeState[] Cache = new FGlobalDistanceFieldCacheTypeState[GlobalDistanceField.GDF_Num];

    // Used to perform a full update of the clip map when the scene data changes
//	const class FDistanceFieldSceneData* LastUsedSceneDataForFullUpdate;
}
