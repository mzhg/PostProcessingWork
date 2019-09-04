package jet.opengl.renderer.Unreal4.mesh;

public enum EPrimitiveIdMode {

    /**
     * PrimitiveId will be taken from the FPrimitiveSceneInfo corresponding to the FMeshBatch.
     * Primitive data will then be fetched by supporting VF's from the GPUScene persistent PrimitiveBuffer.
     */
    PrimID_FromPrimitiveSceneInfo		,

    /**
     * The renderer will upload Primitive data from the FMeshBatchElement's PrimitiveUniformBufferResource to the end of the GPUScene PrimitiveBuffer, and assign the offset to DynamicPrimitiveShaderDataIndex.
     * PrimitiveId for drawing will be computed as Scene->NumPrimitives + FMeshBatchElement's DynamicPrimitiveShaderDataIndex.
     */
    PrimID_DynamicPrimitiveShaderData	,

    /**
     * PrimitiveId will always be 0.  Instancing not supported.
     * View.PrimitiveSceneDataOverrideSRV must be set in this configuration to control what the shader fetches at PrimitiveId == 0.
     */
    PrimID_ForceZero					,
}
