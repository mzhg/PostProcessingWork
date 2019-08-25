package jet.opengl.demos.Unreal4.distancefield;

import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3i;

import jet.opengl.postprocessing.util.BoundingBox;
import jet.opengl.postprocessing.util.StackByte;

/** Distance field data payload and output of the mesh build process. */
public class FDistanceFieldVolumeData {
    /**
     * FP16 Signed distance field volume stored in local space.
     * This has to be kept around after the inital upload to GPU memory to support reallocs of the distance field atlas, so it is compressed.
     */
    public final StackByte CompressedDistanceFieldVolume = new StackByte();

    /** Dimensions of DistanceFieldVolume. */
    public final Vector3i Size = new Vector3i();

    /** Local space bounding box of the distance field volume. */
    public final BoundingBox LocalBoundingBox = new BoundingBox();

    public final Vector2f DistanceMinMax = new Vector2f();

    /** Whether the mesh was closed and therefore a valid distance field was supported. */
    public boolean bMeshWasClosed = true;

    /** Whether the distance field was built assuming that every triangle is a frontface. */
    public boolean bBuiltAsIfTwoSided;

    /** Whether the mesh was a plane with very little extent in Z. */
    public boolean bMeshWasPlane;

    public final FDistanceFieldVolumeTexture VolumeTexture = new FDistanceFieldVolumeTexture();

    /*FDistanceFieldVolumeData() :
    Size(FIntVector(0, 0, 0)),
    LocalBoundingBox(ForceInit),
    DistanceMinMax(FVector2D(0, 0)),
    bMeshWasClosed(true),
    bBuiltAsIfTwoSided(false),
    bMeshWasPlane(false),
    VolumeTexture(*this)
    {}*/

    /*void GetResourceSizeEx(FResourceSizeEx& CumulativeResourceSize)
    {
        CumulativeResourceSize.AddDedicatedSystemMemoryBytes(sizeof(*this));
        CumulativeResourceSize.AddDedicatedSystemMemoryBytes(CompressedDistanceFieldVolume.GetAllocatedSize());
    }

    public int GetResourceSizeBytes()
    {
        FResourceSizeEx ResSize;
        GetResourceSizeEx(ResSize);
        return ResSize.GetTotalMemoryBytes();
    }

#if WITH_EDITORONLY_DATA

    void CacheDerivedData(const FString& InDDCKey, UStaticMesh* Mesh, UStaticMesh* GenerateSource, float DistanceFieldResolutionScale, bool bGenerateDistanceFieldAsIfTwoSided);

#endif

    friend FArchive& operator<<(FArchive& Ar,FDistanceFieldVolumeData& Data)
    {
        // Note: this is derived data, no need for versioning (bump the DDC guid)
        Ar << Data.CompressedDistanceFieldVolume << Data.Size << Data.LocalBoundingBox << Data.DistanceMinMax << Data.bMeshWasClosed << Data.bBuiltAsIfTwoSided << Data.bMeshWasPlane;
        return Ar;
    }*/
}
