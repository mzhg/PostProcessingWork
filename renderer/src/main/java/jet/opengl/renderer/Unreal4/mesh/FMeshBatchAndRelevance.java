package jet.opengl.renderer.Unreal4.mesh;

import jet.opengl.renderer.Unreal4.scenes.FPrimitiveSceneProxy;

public class FMeshBatchAndRelevance {
    public FMeshBatch Mesh;

    /** The render info for the primitive which created this mesh, required. */
    public FPrimitiveSceneProxy PrimitiveSceneProxy;

    /**
     * Cached usage information to speed up traversal in the most costly passes (depth-only, base pass, shadow depth),
     * This is done so the Mesh does not have to be dereferenced to determine pass relevance.
     */
    private boolean bHasOpaqueMaterial;
    private boolean bHasMaskedMaterial;
    private boolean bHasTranslucentMaterialWithVelocity;
    private boolean bRenderInMainPass;

    public FMeshBatchAndRelevance(FMeshBatch InMesh, FPrimitiveSceneProxy InPrimitiveSceneProxy, int FeatureLevel){
        throw new UnsupportedOperationException();
    }

    public boolean GetHasOpaqueMaterial()  { return bHasOpaqueMaterial; }
    public boolean GetHasMaskedMaterial()  { return bHasMaskedMaterial; }
    public boolean GetHasOpaqueOrMaskedMaterial()  { return bHasOpaqueMaterial || bHasMaskedMaterial; }
    public boolean GetHasTranslucentMaterialWithVelocity()  { return bHasTranslucentMaterialWithVelocity; }
    public boolean GetRenderInMainPass()  { return bRenderInMainPass; }
}
