package jet.opengl.renderer.Unreal4.resouces;

 class FDynamicAllocReadBuffer extends FDynamicReadBuffer {
    int AllocatedByteCount = 0;
    /** Number of successive frames for which AllocatedByteCount == 0. Used as a metric to decide when to free the allocation. */
    int NumFramesUnused = 0;

    /**
     * Unocks the buffer so the GPU may read from it.
     */
    void Unlock()
    {
        super.Unlock();
        AllocatedByteCount = 0;
        NumFramesUnused = 0;
    }
}
