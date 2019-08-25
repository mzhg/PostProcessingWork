package jet.opengl.demos.Unreal4;

import org.lwjgl.util.vector.Vector4f;

import java.util.ArrayList;

import jet.opengl.demos.Unreal4.distancefield.GlobalDistanceField;

/** Scene data used to manage distance field object buffers on the GPU. */
public class FDistanceFieldSceneData {
    public FDistanceFieldSceneData(/*EShaderPlatform ShaderPlatform*/){
//        static const auto CVar = IConsoleManager::Get().FindTConsoleVariableDataInt(TEXT("r.GenerateMeshDistanceFields"));

        bTrackAllPrimitives = false; //(DoesPlatformSupportDistanceFieldAO(ShaderPlatform) || DoesPlatformSupportDistanceFieldShadowing(ShaderPlatform)) && CVar->GetValueOnGameThread() != 0;

        bCanUse16BitObjectIndices = false; //RHISupportsBufferLoadTypeConversion(ShaderPlatform);
    }

    public void AddPrimitive(FPrimitiveSceneInfo InPrimitive){
        /*const FPrimitiveSceneProxy* Proxy = InPrimitive->Proxy;

        if ((bTrackAllPrimitives || Proxy->CastsDynamicIndirectShadow())
                && Proxy->CastsDynamicShadow()
                && Proxy->AffectsDistanceFieldLighting())
        {
            if (Proxy->SupportsHeightfieldRepresentation())
            {
                HeightfieldPrimitives.Add(InPrimitive);
                FBoxSphereBounds PrimitiveBounds = Proxy->GetBounds();
                FGlobalDFCacheType CacheType = Proxy->IsOftenMoving() ? GDF_Full : GDF_MostlyStatic;
                PrimitiveModifiedBounds[CacheType].Add(FVector4(PrimitiveBounds.Origin, PrimitiveBounds.SphereRadius));
            }

            if (Proxy->SupportsDistanceFieldRepresentation())
            {
                checkSlow(!PendingAddOperations.Contains(InPrimitive));
                checkSlow(!PendingUpdateOperations.Contains(InPrimitive));
                PendingAddOperations.Add(InPrimitive);
            }
        }*/

        throw new UnsupportedOperationException();
    }

    public void UpdatePrimitive(FPrimitiveSceneInfo InPrimitive){

    }

    public void RemovePrimitive(FPrimitiveSceneInfo InPrimitive){

    }

    public void Release(){

    }

    public void VerifyIntegrity(){

    }

    public boolean HasPendingOperations()
    {
//        return  PendingAddOperations.Num() > 0 || PendingUpdateOperations.Num() > 0 || PendingRemoveOperations.Num() > 0;
        return false;
    }

    public boolean HasPendingRemovePrimitive(FPrimitiveSceneInfo Primitive)
    {
        /*for (int32 RemoveIndex = 0; RemoveIndex < PendingRemoveOperations.Num(); RemoveIndex++)
        {
            if (PendingRemoveOperations[RemoveIndex].Primitive == Primitive)
            {
                return true;
            }
        }*/

        return false;
    }

    public boolean CanUse16BitObjectIndices()
    {
        return bCanUse16BitObjectIndices && (NumObjectsInBuffer < (1 << 16));
    }

    public int NumObjectsInBuffer;
//    public FDistanceFieldObjectBuffers ObjectBuffers;

    /** Stores the primitive and instance index of every entry in the object buffer. */
    public final ArrayList<FPrimitiveAndInstance> PrimitiveInstanceMapping = new ArrayList<>();
    public final ArrayList<FPrimitiveSceneInfo> HeightfieldPrimitives = new ArrayList<>();

//    class FSurfelBuffers* SurfelBuffers;
//    FSurfelBufferAllocator SurfelAllocations;

//    class FInstancedSurfelBuffers* InstancedSurfelBuffers;
//    FSurfelBufferAllocator InstancedSurfelAllocations;

    /** Pending operations on the object buffers to be processed next frame. */
//    TArray<FPrimitiveSceneInfo*> PendingAddOperations;
//    TSet<FPrimitiveSceneInfo*> PendingUpdateOperations;
//    TArray<FPrimitiveRemoveInfo> PendingRemoveOperations;
//    TArray<FVector4> PrimitiveModifiedBounds[GDF_Num];
    public ArrayList<Vector4f>[] PrimitiveModifiedBounds = new ArrayList[GlobalDistanceField.GDF_Num];
    /** Used to detect atlas reallocations, since objects store UVs into the atlas and need to be updated when it changes. */
    public int AtlasGeneration;

    public boolean bTrackAllPrimitives;
    public boolean bCanUse16BitObjectIndices;
}
