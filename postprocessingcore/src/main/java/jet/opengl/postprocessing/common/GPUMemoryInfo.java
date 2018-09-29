package jet.opengl.postprocessing.common;

public class GPUMemoryInfo {

    /**
     * dedicated video memory, total size (in kb) of the GPU memory
     */
    public int dedicatedMemory;

    /**
     * total available memory, total size (in Kb) of the memory
     * available for allocations
     */
    public int maxmumDedicatedMemory;

    /**
     * current available dedicated video memory (in kb),
     * currently unused GPU memory
     */
    public int currentMemory;

    /**
     * count of total evictions seen by system
     */
    public int evictionCount;

    /**
     * size of total video memory evicted (in kb)
     */
    public int evictedMemory;

    @Override
    public String toString() {
        StringBuilder out = new StringBuilder();
        out.append("Dedicated video memory = ").append(dedicatedMemory/1024.f).append("MB, ");
        out.append("Total available memory = ").append(maxmumDedicatedMemory/1024.f).append("MB, ");
        out.append("Current available memory = ").append(currentMemory/1024.f).append("MB, ");
        out.append("Eviction Count = ").append(evictionCount).append(',');
        out.append("Eviction memory = ").append(evictedMemory/1024).append("MB.");

        return out.toString();
    }
}
