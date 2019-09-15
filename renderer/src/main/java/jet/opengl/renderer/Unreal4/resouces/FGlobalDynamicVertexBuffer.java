package jet.opengl.renderer.Unreal4.resouces;

import java.nio.ByteBuffer;
import java.util.ArrayList;

import jet.opengl.postprocessing.buffer.BufferGL;
import jet.opengl.postprocessing.common.Disposeable;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.util.LogUtil;
import jet.opengl.postprocessing.util.Numeric;
import jet.opengl.renderer.Unreal4.UE4Engine;

/**
 * A system for dynamically allocating GPU memory for vertices.
 */
public class FGlobalDynamicVertexBuffer implements Disposeable {

    public static int GGlobalBufferNumFramesUnusedThresold = 30;

    /**
     * Information regarding an allocation from this buffer.
     */
    public static final class FAllocation
    {
        /** The location of the buffer in main memory. */
        public ByteBuffer Buffer;
        /** The vertex buffer to bind for draw calls. */
        public BufferGL VertexBuffer;
        /** The offset in to the vertex buffer. */
        public int VertexOffset;

        /** Returns true if the allocation is valid. */
        public boolean IsValid()
        {
            return Buffer != null;
        }
    }

    /** The pool of vertex buffers from which allocations are made. */
    private FDynamicVertexBufferPool Pool = new FDynamicVertexBufferPool();

    /** A total of all allocations made since the last commit. Used to alert about spikes in memory usage. */
    private int TotalAllocatedSinceLastCommit;

    /**
     * Allocates space in the global vertex buffer.
     * @param SizeInBytes - The amount of memory to allocate in bytes.
     * @returns An FAllocation with information regarding the allocated memory.
     */
    public FAllocation Allocate(int SizeInBytes){
        FAllocation Allocation = new FAllocation();

        TotalAllocatedSinceLastCommit += SizeInBytes;
        if (IsRenderAlarmLoggingEnabled())
        {
//            UE_LOG(LogRendererCore, Warning, TEXT("FGlobalDynamicVertexBuffer::Allocate(%u), will have allocated %u total this frame"), SizeInBytes, TotalAllocatedSinceLastCommit);
            LogUtil.w(LogUtil.LogType.DEFAULT, String.format("FGlobalDynamicVertexBuffer::Allocate(%u), will have allocated %u total this frame", SizeInBytes, TotalAllocatedSinceLastCommit));
        }

        FDynamicVertexBuffer VertexBuffer = Pool.CurrentVertexBuffer;
        if (VertexBuffer == null || VertexBuffer.AllocatedByteCount + SizeInBytes > VertexBuffer.BufferSize)
        {
            // Find a buffer in the pool big enough to service the request.
            VertexBuffer = null;
            for (int BufferIndex = 0, NumBuffers = Pool.VertexBuffers.size(); BufferIndex < NumBuffers; ++BufferIndex)
            {
                FDynamicVertexBuffer VertexBufferToCheck = Pool.VertexBuffers.get(BufferIndex);
                if (VertexBufferToCheck.AllocatedByteCount + SizeInBytes <= VertexBufferToCheck.BufferSize)
                {
                    VertexBuffer = VertexBufferToCheck;
                    break;
                }
            }

            // Create a new vertex buffer if needed.
            if (VertexBuffer == null)
            {
                VertexBuffer = new FDynamicVertexBuffer(SizeInBytes);
                Pool.VertexBuffers.add(VertexBuffer);
                VertexBuffer.InitResource();
            }

            // Lock the buffer if needed.
            if (VertexBuffer.MappedBuffer == null)
            {
                VertexBuffer.Lock();
            }

            // Remember this buffer, we'll try to allocate out of it in the future.
            Pool.CurrentVertexBuffer = VertexBuffer;
        }

        UE4Engine.check(VertexBuffer != null);
//        checkf(VertexBuffer->AllocatedByteCount + SizeInBytes <= VertexBuffer->BufferSize, TEXT("Global vertex buffer allocation failed: BufferSize=%d AllocatedByteCount=%d SizeInBytes=%d"), VertexBuffer->BufferSize, VertexBuffer->AllocatedByteCount, SizeInBytes);

//        Allocation.Buffer = VertexBuffer->MappedBuffer + VertexBuffer.AllocatedByteCount;
        VertexBuffer.MappedBuffer.position(VertexBuffer.AllocatedByteCount);
        Allocation.Buffer = VertexBuffer.MappedBuffer.slice();
        VertexBuffer.MappedBuffer.position(0);

        Allocation.VertexOffset = VertexBuffer.AllocatedByteCount;
        VertexBuffer.AllocatedByteCount += SizeInBytes;

        return Allocation;
    }

    /**
     * Commits allocated memory to the GPU.
     *		WARNING: Once this buffer has been committed to the GPU, allocations
     *		remain valid only until the next call to Allocate!
     */
    void Commit(){
        for (int BufferIndex = 0, NumBuffers = Pool.VertexBuffers.size(); BufferIndex < NumBuffers; ++BufferIndex)
        {
            FDynamicVertexBuffer VertexBuffer = Pool.VertexBuffers.get(BufferIndex);
            if (VertexBuffer.MappedBuffer != null)
            {
                VertexBuffer.Unlock();
            }
            else if (GGlobalBufferNumFramesUnusedThresold > 0 && VertexBuffer.AllocatedByteCount == 0)
            {
                ++VertexBuffer.NumFramesUnused;
                if (VertexBuffer.NumFramesUnused >= GGlobalBufferNumFramesUnusedThresold)
                {
                    // Remove the buffer, assumes they are unordered.
                    VertexBuffer.ReleaseResource();
//                    Pool.VertexBuffers.RemoveAtSwap(BufferIndex);
                    Pool.VertexBuffers.remove(BufferIndex);
                    --BufferIndex;
                    --NumBuffers;
                }
            }
        }
        Pool.CurrentVertexBuffer = null;
        TotalAllocatedSinceLastCommit = 0;
    }

    /** Returns true if log statements should be made because we exceeded GMaxVertexBytesAllocatedPerFrame */
    public boolean IsRenderAlarmLoggingEnabled(){
        return UE4Engine.CHECKING;
    }

    @Override
    public void dispose() {
        Pool.release();
        Pool = null;
    }

    private static final class FDynamicVertexBufferPool{
        /** List of vertex buffers. */
        final ArrayList<FDynamicVertexBuffer> VertexBuffers = new ArrayList<>();
        /** The current buffer from which allocations are being made. */
        FDynamicVertexBuffer CurrentVertexBuffer;

        void release(){
            int NumVertexBuffers = VertexBuffers.size();
            for (int BufferIndex = 0; BufferIndex < NumVertexBuffers; ++BufferIndex)
            {
                VertexBuffers.get(BufferIndex).ReleaseRHI();
            }
        }
    }

    public static final int ALIGNMENT = (1 << 16);

    /**
     * An individual dynamic vertex buffer.
     */
    private static final class FDynamicVertexBuffer extends FVertexBuffer{
        /** Pointer to the vertex buffer mapped in main memory. */
        ByteBuffer MappedBuffer;
        /** Size of the vertex buffer in bytes. */
        int BufferSize;
        /** Number of bytes currently allocated from the buffer. */
        int AllocatedByteCount;
        /** Number of successive frames for which AllocatedByteCount == 0. Used as a metric to decide when to free the allocation. */
        int NumFramesUnused = 0;

        /** Default constructor. */
        FDynamicVertexBuffer(int InMinBufferSize)
//		: MappedBuffer(NULL)
//		, BufferSize(FMath::Max<uint32>(Align(InMinBufferSize,ALIGNMENT),ALIGNMENT))
//                , AllocatedByteCount(0)
        {
            BufferSize = Math.max(Numeric.align(InMinBufferSize,ALIGNMENT),ALIGNMENT);
        }

        /**
         * Locks the vertex buffer so it may be written to.
         */
        void Lock()
        {
            UE4Engine.check(MappedBuffer == null);
            UE4Engine.check(AllocatedByteCount == 0);
//            check(IsValidRef(VertexBufferRHI));
            MappedBuffer = // (uint8*)RHILockVertexBuffer(VertexBufferRHI, 0, BufferSize, RLM_WriteOnly);
            VertexBufferRHI.map(0, BufferSize, GLenum.GL_WRITE_ONLY);
        }

        /**
         * Unocks the buffer so the GPU may read from it.
         */
        void Unlock()
        {
            UE4Engine.check(MappedBuffer != null);
//            UE4Engine.check(IsValidRef(VertexBufferRHI));
//            RHIUnlockVertexBuffer(VertexBufferRHI);
            VertexBufferRHI.unmap();
            MappedBuffer = null;
            AllocatedByteCount = 0;
            NumFramesUnused = 0;
        }

        // FRenderResource interface.
        @Override
        public void InitRHI()
        {
//            check(!IsValidRef(VertexBufferRHI));
//            FRHIResourceCreateInfo CreateInfo;
//            VertexBufferRHI = RHICreateVertexBuffer(BufferSize, BUF_Volatile, CreateInfo);
            VertexBufferRHI.initlize(GLenum.GL_ARRAY_BUFFER, BufferSize, null, GLenum.GL_DYNAMIC_DRAW);
            MappedBuffer = null;
            AllocatedByteCount = 0;
        }

        @Override
        public void ReleaseRHI()
        {
            super.ReleaseRHI();
            MappedBuffer = null;
            AllocatedByteCount = 0;
        }

        public String GetFriendlyName()
        {
            return "FDynamicVertexBuffer";
        }
    }
}
