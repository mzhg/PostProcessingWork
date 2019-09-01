package jet.opengl.renderer.Unreal4;

import org.lwjgl.util.vector.ReadableVector4f;

import java.util.ArrayList;

import jet.opengl.postprocessing.common.Disposeable;
import jet.opengl.renderer.Unreal4.scenes.FSceneViewState;
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
    void Flush(/*FRHICommandList& RHICmdList*/){
        throw new UnsupportedOperationException();
    }

    /**
     * Batches a primitive's occlusion query for rendering.
     * @param BoundsOrigin - The primitive's bounds.
     */
    public int BatchPrimitive(ReadableVector4f BoundsOrigin, ReadableVector4f BoundsBoxExtent/*, FGlobalDynamicVertexBuffer& DynamicVertexBuffer*/){
        throw new UnsupportedOperationException();
    }

    public int GetNumBatchOcclusionQueries()
    {
        return BatchOcclusionQueries.size();
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
