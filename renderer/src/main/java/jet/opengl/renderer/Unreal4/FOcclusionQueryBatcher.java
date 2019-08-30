package jet.opengl.renderer.Unreal4;

import java.util.ArrayList;

import jet.opengl.postprocessing.common.Disposeable;
import jet.opengl.renderer.Unreal4.utils.FRenderQueryPool;

/**
 * Combines consecutive primitives which use the same occlusion query into a single DrawIndexedPrimitive call.
 */
public final class FOcclusionQueryBatcher implements Disposeable {

    /** The maximum number of consecutive previously occluded primitives which will be combined into a single occlusion query. */
    public static final int OccludedPrimitiveQueryBatchSize = 16;

    /** Initialization constructor. */
    FOcclusionQueryBatcher(FSceneViewState ViewState, int InMaxBatchedPrimitives){
        MaxBatchedPrimitives = InMaxBatchedPrimitives;
    }

    /** @returns True if the batcher has any outstanding batches, otherwise false. */
    public boolean HasBatches() { return (NumBatchedPrimitives > 0); }

    /** Renders the current batch and resets the batch state. */
    void Flush(FRHICommandList& RHICmdList);

    /**
     * Batches a primitive's occlusion query for rendering.
     * @param Bounds - The primitive's bounds.
     */
    FRenderQueryRHIParamRef BatchPrimitive(const FVector& BoundsOrigin, const FVector& BoundsBoxExtent, FGlobalDynamicVertexBuffer& DynamicVertexBuffer);
    public int GetNumBatchOcclusionQueries()
    {
        return BatchOcclusionQueries.Num();
    }

    @Override
    public void dispose() {

    }

    private static final class FOcclusionBatch{
//        FRenderQueryRHIRef Query;
//        FGlobalDynamicVertexBuffer::FAllocation VertexAllocation;
        int Query;

    }

    /** The pending batches. */
    private final ArrayList<FOcclusionBatch> BatchOcclusionQueries = new ArrayList<>();

    /** The batch new primitives are being added to. */
    private FOcclusionBatch CurrentBatchOcclusionQuery;

    /** The maximum number of primitives in a batch. */
    private final int MaxBatchedPrimitives;

    /** The number of primitives in the current batch. */
    private int NumBatchedPrimitives;

    /** The pool to allocate occlusion queries from. */
    private FRenderQueryPool OcclusionQueryPool;
}
