package jet.opengl.renderer.Unreal4.mesh;

import jet.opengl.renderer.Unreal4.scenes.FPrimitiveSceneProxy;

public class FMeshDecalBatch implements Comparable<FMeshDecalBatch>{

    public FMeshBatch Mesh;
    public FPrimitiveSceneProxy Proxy;
    public short SortKey;

    @Override
    public int compareTo(FMeshDecalBatch other) {
        return Integer.compare(SortKey, other.SortKey);
    }
}
