package jet.opengl.renderer.Unreal4.mesh;

import jet.opengl.renderer.Unreal4.api.ERasterizerCullMode;
import jet.opengl.renderer.Unreal4.api.ERasterizerFillMode;

public class FVisibleMeshDrawCommand {
    // Note: no ctor as TChunkedArray::CopyToLinearArray requires POD types

    public void Setup(
        FMeshDrawCommand InMeshDrawCommand,
        int InDrawPrimitiveId,
        int InScenePrimitiveId,
        int InStateBucketId,
        ERasterizerFillMode InMeshFillMode,
        ERasterizerCullMode InMeshCullMode,
        /*FMeshDrawCommandSortKey*/long InSortKey)
    {
        MeshDrawCommand = InMeshDrawCommand;
        DrawPrimitiveId = InDrawPrimitiveId;
        ScenePrimitiveId = InScenePrimitiveId;
        PrimitiveIdBufferOffset = -1;
        StateBucketId = InStateBucketId;
        MeshFillMode = InMeshFillMode;
        MeshCullMode = InMeshCullMode;
        SortKey = InSortKey;
    }

    // Mesh Draw Command stored separately to avoid fetching its data during sorting
	public FMeshDrawCommand MeshDrawCommand;

    // Sort key for non state based sorting (e.g. sort translucent draws by depth).
    public /*FMeshDrawCommandSortKey*/long SortKey;

    // Draw PrimitiveId this draw command is associated with - used by the shader to fetch primitive data from the PrimitiveSceneData SRV.
    // If it's < Scene->Primitives.Num() then it's a valid Scene PrimitiveIndex and can be used to backtrack to the FPrimitiveSceneInfo.
    public int DrawPrimitiveId;

    // Scene PrimitiveId that generated this draw command, or -1 if no FPrimitiveSceneInfo. Can be used to backtrack to the FPrimitiveSceneInfo.
    public int ScenePrimitiveId;

    // Offset into the buffer of PrimitiveIds built for this pass, in int32's.
    public int PrimitiveIdBufferOffset;

    // Dynamic instancing state bucket ID.
    // Any commands with the same StateBucketId can be merged into one draw call with instancing.
    // A value of -1 means the draw is not in any state bucket and should be sorted by other factors instead.
    public int StateBucketId;

    // Needed for view overrides
    public ERasterizerFillMode MeshFillMode;
    public ERasterizerCullMode MeshCullMode;
}
