package jet.opengl.renderer.Unreal4.mesh;

import jet.opengl.postprocessing.buffer.BufferGL;

/**
 * A batch mesh element definition.
 */
public class FMeshBatchElement {
    /**
     * Primitive uniform buffer RHI
     * Must be null for vertex factories that manually fetch primitive data from scene data, in which case FPrimitiveSceneProxy::UniformBuffer will be used.
     */
    public BufferGL PrimitiveUniformBuffer;

    /**
     * Primitive uniform buffer to use for rendering, used when PrimitiveUniformBuffer is null.
     * This interface allows a FMeshBatchElement to be setup for a uniform buffer that has not been initialized yet, (TUniformBuffer* is known but not the FRHIUniformBuffer*)
     */
	public /*FPrimitiveUniformShaderParameters*/BufferGL PrimitiveUniformBufferResource;

    /** Assigned by renderer */
    public EPrimitiveIdMode PrimitiveIdMode;

    /** Assigned by renderer */
    public int DynamicPrimitiveShaderDataIndex;

	public BufferGL IndexBuffer;

    /** If !bIsSplineProxy, Instance runs, where number of runs is specified by NumInstances.  Run structure is [StartInstanceIndex, EndInstanceIndex]. */
    public int[] InstanceRuns;
    /** If bIsSplineProxy, a pointer back to the proxy */
//    public FSplineMeshSceneProxy SplineMeshSceneProxy;
	public Object UserData;

    public int FirstIndex;
    /** When 0, IndirectArgsBuffer will be used. */
    public int NumPrimitives;

    /** Number of instances to draw.  If InstanceRuns is valid, this is actually the number of runs in InstanceRuns. */
    public int NumInstances;
    public int BaseVertexIndex;
    public int MinVertexIndex;
    public int MaxVertexIndex;
    // Meaning depends on the vertex factory, e.g. FGPUSkinPassthroughVertexFactory: element index in FGPUSkinCache::CachedElements
    public Object VertexFactoryUserData;
    public int UserIndex;
    public float MinScreenSize;
    public float MaxScreenSize;

    public int InstancedLODIndex;
    public int InstancedLODRange;
    public boolean bUserDataIsColorVertexBuffer;
    public boolean bIsInstancedMesh;
    public boolean bIsSplineProxy;
    public boolean bIsInstanceRuns;

//#if !(UE_BUILD_SHIPPING || UE_BUILD_TEST)
    /** Conceptual element index used for debug viewmodes. */
    public int VisualizeElementIndex;
//#endif
    public BufferGL IndirectArgsBuffer;
    public int IndirectArgsOffset;

    public int GetNumPrimitives()
    {
        if (bIsInstanceRuns && InstanceRuns != null)
        {
            int Count = 0;
            for (int Run = 0; Run < NumInstances; Run++)
            {
                Count += NumPrimitives * (InstanceRuns[Run * 2 + 1] - InstanceRuns[Run * 2] + 1);
            }
            return Count;
        }
        else
        {
            return NumPrimitives * NumInstances;
        }
    }
}
