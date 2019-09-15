package jet.opengl.renderer.Unreal4.mesh;

import jet.opengl.renderer.Unreal4.FPrimitiveSceneInfo;
import jet.opengl.renderer.Unreal4.UE4Engine;
import jet.opengl.renderer.Unreal4.utils.FHitProxyId;

/**
 * A mesh which is defined by a primitive at scene segment construction time and never changed.
 * Lights are attached and detached as the segment containing the mesh is added or removed from a scene.
 */
public class FStaticMeshBatch extends FMeshBatch {
    /** The render info for the primitive which created this mesh. */
    public FPrimitiveSceneInfo PrimitiveSceneInfo;

    /** The index of the mesh in the scene's static meshes array. */
    public int Id = UE4Engine.INDEX_NONE;

    /** Index of the mesh into the scene's StaticMeshBatchVisibility array. */
    public int BatchVisibilityId = UE4Engine.INDEX_NONE;

    // Constructor/destructor.
    public FStaticMeshBatch(
            FPrimitiveSceneInfo InPrimitiveSceneInfo,
		    FMeshBatch InMesh,
            FHitProxyId InHitProxyId
    ) {
        Set(InMesh);
        PrimitiveSceneInfo = InPrimitiveSceneInfo;
        BatchHitProxyId.Set(InHitProxyId);
    }

}
