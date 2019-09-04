package jet.opengl.renderer.Unreal4.scenes;

import jet.opengl.postprocessing.util.Numeric;

/**
 * Computes the LOD to render for the list of static meshes in the given view.<p></p>
 * param StaticMeshes - List of static meshes.
 * param View - The view to render the LOD level for
 * param Origin - Origin of the bounds of the mesh in world space
 * param SphereRadius - Radius of the sphere to use to calculate screen coverage
 */
public class FLODMask {
    public final byte[] DitheredLODIndices = new byte[2];

    public FLODMask()
    {
        DitheredLODIndices[0] = Byte.MAX_VALUE;
        DitheredLODIndices[1] = Byte.MAX_VALUE;
    }

    public void SetLOD(int LODIndex)
    {
        DitheredLODIndices[0] = (byte) LODIndex;
        DitheredLODIndices[1] = (byte) LODIndex;
    }
    public void SetLODSample(int LODIndex, int SampleIndex)
    {
        DitheredLODIndices[SampleIndex] = (byte)LODIndex;
    }
    public void ClampToFirstLOD(byte FirstLODIdx)
    {
        DitheredLODIndices[0] = (byte)Math.max(DitheredLODIndices[0], FirstLODIdx);
        DitheredLODIndices[1] = (byte)Math.max(DitheredLODIndices[1], FirstLODIdx);
    }

    public boolean ContainsLOD(int LODIndex)
    {
        return DitheredLODIndices[0] == LODIndex || DitheredLODIndices[1] == LODIndex;
    }

    //#dxr_todo UE-72106: We should probably add both LoDs but mask them based on their
    //LodFade value within the BVH based on the LodFadeMask in the GBuffer
    public boolean ContainsRayTracedLOD(int LODIndex)
    {
        return DitheredLODIndices[0] == LODIndex;
    }

    public byte GetRayTracedLOD()
    {
        return DitheredLODIndices[0];
    }

    public boolean IsDithered()
    {
        return DitheredLODIndices[0] != DitheredLODIndices[1];
    }
}
