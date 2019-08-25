package jet.opengl.demos.Unreal4.distancefield;

import java.util.ArrayList;
import java.util.List;

import jet.opengl.postprocessing.texture.Texture3D;

public class FDistanceFieldVolumeTextureAtlas {

    /** Manages the atlas layout. */
//    FTextureLayout3d BlockAllocator;

    /** Allocations that are waiting to be added until the next update. */
    final ArrayList<FDistanceFieldVolumeTexture> PendingAllocations = new ArrayList<>();

    /** Allocations that have already been added, stored in case we need to realloc. */
    final ArrayList<FDistanceFieldVolumeTexture> CurrentAllocations = new ArrayList<>();

    /** Incremented when the atlas is reallocated, so dependencies know to update. */
    public int Generation;

    public boolean bInitialized;
    public int Format;
    public Texture3D VolumeTextureRHI;

    public FDistanceFieldVolumeTextureAtlas(){

    }

    public void InitializeIfNeeded(){

    }

    public void ReleaseRHI()
    {
        VolumeTextureRHI.dispose();
    }

    public int GetSizeX() { return VolumeTextureRHI.getWidth(); }
    public int GetSizeY() { return VolumeTextureRHI.getHeight(); }
    public int GetSizeZ() { return VolumeTextureRHI.getDepth(); }

    public void ListMeshDistanceFields(){
        throw new UnsupportedOperationException();
    }

    /*ENGINE_API FString GetSizeString() const;


    *//** Add an allocation to the atlas. *//*
    void AddAllocation(FDistanceFieldVolumeTexture* Texture);

    *//** Remove an allocation from the atlas. This must be done prior to deleting the FDistanceFieldVolumeTexture object. *//*
    void RemoveAllocation(FDistanceFieldVolumeTexture* Texture);

    *//** Reallocates the volume texture if necessary and uploads new allocations. *//*
    ENGINE_API void UpdateAllocations();*/

    public int GetGeneration() { return Generation; }


}
