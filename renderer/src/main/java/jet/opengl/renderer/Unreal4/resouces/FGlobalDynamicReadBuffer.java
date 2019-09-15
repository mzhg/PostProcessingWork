package jet.opengl.renderer.Unreal4.resouces;

import java.nio.ByteBuffer;
import java.util.ArrayList;

import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.util.LogUtil;
import jet.opengl.postprocessing.util.Numeric;
import jet.opengl.renderer.Unreal4.UE4Engine;
import jet.opengl.renderer.Unreal4.utils.RenderUtils;

/**
 * A system for dynamically allocating GPU memory for rendering. Note that this must derive from FRenderResource
 so that we can safely free the shader resource views for OpenGL and other platforms. If we wait until the module is shutdown,
 the renderer RHI will have already been destroyed and we can execute code on invalid data. By making ourself a render resource, we
 clean up immediately before the renderer dies.
 */
public class FGlobalDynamicReadBuffer extends FRenderResource {

    // The allocator works by looking for the first free buffer that contains the required number of elements.  There is currently no trim so buffers stay in memory.
    // To avoid increasing allocation sizes over multiple frames causing severe memory bloat (i.e. 100 elements, 1001 elements) we first align the required
    // number of elements to GMinReadBufferRenderingBufferSize, we then take the max(aligned num, GMinReadBufferRenderingBufferSize)
    public static final int GMinReadBufferRenderingBufferSize = 256 * 1024;

    public static final int GAlignReadBufferRenderingBufferSize = 64 * 1024;
    /**
     * Information regarding an allocation from this buffer.
     */
    public static final class FAllocation
    {
        /** The location of the buffer in main memory. */
        public ByteBuffer Buffer;
        /** The read buffer to bind for draw calls. */
        public FDynamicAllocReadBuffer ReadBuffer;
        /** The offset in to the read buffer. */
        public int FirstIndex;

        /** Returns true if the allocation is valid. */
        public boolean IsValid()
        {
            return Buffer != null;
        }
    }

    /** The pools of read buffers from which allocations are made. */
    protected FDynamicReadBufferPool FloatBufferPool = new FDynamicReadBufferPool();
    protected FDynamicReadBufferPool Int32BufferPool = new FDynamicReadBufferPool();

    /** A total of all allocations made since the last commit. Used to alert about spikes in memory usage. */
    protected int TotalAllocatedSinceLastCommit;

    public FAllocation AllocateFloat(int Num){
        FAllocation Allocation = new FAllocation();

        TotalAllocatedSinceLastCommit += Num;
        if (IsRenderAlarmLoggingEnabled())
        {
//            UE_LOG(LogRendererCore, Warning, TEXT("FGlobalReadBuffer::AllocateFloat(%u), will have allocated %u total this frame"), Num, TotalAllocatedSinceLastCommit);
            LogUtil.w(LogUtil.LogType.DEFAULT, String.format("FGlobalReadBuffer::AllocateFloat(%d), will have allocated %u total this frame", Num, TotalAllocatedSinceLastCommit));
        }
        int SizeInBytes = /*sizeof(float)*/4 * Num;
        FDynamicAllocReadBuffer Buffer = FloatBufferPool.CurrentBuffer;
        if (Buffer == null || Buffer.AllocatedByteCount + SizeInBytes > Buffer.NumBytes)
        {
            // Find a buffer in the pool big enough to service the request.
            Buffer = null;
            for (int BufferIndex = 0, NumBuffers = FloatBufferPool.Buffers.size(); BufferIndex < NumBuffers; ++BufferIndex)
            {
                FDynamicAllocReadBuffer BufferToCheck = FloatBufferPool.Buffers.get(BufferIndex);
                if (BufferToCheck.AllocatedByteCount + SizeInBytes <= BufferToCheck.NumBytes)
                {
                    Buffer = BufferToCheck;
                    break;
                }
            }

            // Create a new vertex buffer if needed.
            if (Buffer == null)
            {
			    final int AlignedNum = Numeric.divideAndRoundUp(Num, GAlignReadBufferRenderingBufferSize) * GAlignReadBufferRenderingBufferSize;
                final int NewBufferSize = Math.max(AlignedNum, GMinReadBufferRenderingBufferSize);
                Buffer = new FDynamicAllocReadBuffer();
                FloatBufferPool.Buffers.add(Buffer);
                Buffer.Initialize(/*sizeof(float)*/4, NewBufferSize, GLenum.GL_FLOAT, /*BUF_Dynamic*/ GLenum.GL_DYNAMIC_DRAW);
            }

            // Lock the buffer if needed.
            if (Buffer.MappedBuffer == null)
            {
                Buffer.Lock();
            }

            // Remember this buffer, we'll try to allocate out of it in the future.
            FloatBufferPool.CurrentBuffer = Buffer;
        }

//        check(Buffer != NULL);
//        checkf(Buffer->AllocatedByteCount + SizeInBytes <= Buffer->NumBytes, TEXT("Global dynamic read buffer float buffer allocation failed: BufferSize=%d AllocatedByteCount=%d SizeInBytes=%d"), Buffer->NumBytes, Buffer->AllocatedByteCount, SizeInBytes);
//        Allocation.Buffer = Buffer->MappedBuffer + Buffer->AllocatedByteCount;
        Buffer.MappedBuffer = RenderUtils.slice(Buffer.MappedBuffer, Buffer.AllocatedByteCount);
        Allocation.ReadBuffer = Buffer;
        Allocation.FirstIndex = Buffer.AllocatedByteCount;
        Buffer.AllocatedByteCount += SizeInBytes;

        return Allocation;
    }

    public FAllocation AllocateInt32(int Num){
        FAllocation Allocation = new FAllocation();

        TotalAllocatedSinceLastCommit += Num;
        if (IsRenderAlarmLoggingEnabled())
        {
//            UE_LOG(LogRendererCore, Warning, TEXT("FGlobalReadBuffer::AllocateInt32(%u), will have allocated %u total this frame"), Num, TotalAllocatedSinceLastCommit);
            LogUtil.w(LogUtil.LogType.DEFAULT, String.format("FGlobalReadBuffer::AllocateInt32(%d), will have allocated %u total this frame", Num, TotalAllocatedSinceLastCommit));
        }
        int SizeInBytes = /*sizeof(int32)*/4 * Num;
        FDynamicAllocReadBuffer Buffer = Int32BufferPool.CurrentBuffer;
        if (Buffer == null || Buffer.AllocatedByteCount + SizeInBytes > Buffer.NumBytes)
        {
            // Find a buffer in the pool big enough to service the request.
            Buffer = null;
            for (int BufferIndex = 0, NumBuffers = Int32BufferPool.Buffers.size(); BufferIndex < NumBuffers; ++BufferIndex)
            {
                FDynamicAllocReadBuffer BufferToCheck = Int32BufferPool.Buffers.get(BufferIndex);
                if (BufferToCheck.AllocatedByteCount + SizeInBytes <= BufferToCheck.NumBytes)
                {
                    Buffer = BufferToCheck;
                    break;
                }
            }

            // Create a new vertex buffer if needed.
            if (Buffer == null)
            {
			    final int AlignedNum = Numeric.divideAndRoundUp(Num, GAlignReadBufferRenderingBufferSize) * GAlignReadBufferRenderingBufferSize;
                final int NewBufferSize = Math.max(AlignedNum, GMinReadBufferRenderingBufferSize);
                Buffer = new FDynamicAllocReadBuffer();
                Int32BufferPool.Buffers.add(Buffer);
                Buffer.Initialize(/*sizeof(int32)*/4, NewBufferSize, GLenum.GL_R32I, /*BUF_Dynamic*/GLenum.GL_DYNAMIC_DRAW);
            }

            // Lock the buffer if needed.
            if (Buffer.MappedBuffer == null)
            {
                Buffer.Lock();
            }

            // Remember this buffer, we'll try to allocate out of it in the future.
            Int32BufferPool.CurrentBuffer = Buffer;
        }

//        check(Buffer != NULL);
//        checkf(Buffer->AllocatedByteCount + SizeInBytes <= Buffer->NumBytes, TEXT("Global dynamic read buffer int32 buffer allocation failed: BufferSize=%d AllocatedByteCount=%d SizeInBytes=%d"), Buffer->NumBytes, Buffer->AllocatedByteCount, SizeInBytes);
//        Allocation.Buffer = Buffer->MappedBuffer + Buffer->AllocatedByteCount;
        Allocation.Buffer = RenderUtils.slice(Buffer.MappedBuffer, Buffer.AllocatedByteCount);
        Allocation.ReadBuffer = Buffer;
        Allocation.FirstIndex = Buffer.AllocatedByteCount;
        Buffer.AllocatedByteCount += SizeInBytes;

        return Allocation;
    }

    /**
     * Commits allocated memory to the GPU.
     *		WARNING: Once this buffer has been committed to the GPU, allocations
     *		remain valid only until the next call to Allocate!
     */
    public void Commit(){
        final int GGlobalBufferNumFramesUnusedThresold = FGlobalDynamicVertexBuffer.GGlobalBufferNumFramesUnusedThresold;

        for (int BufferIndex = 0, NumBuffers = FloatBufferPool.Buffers.size(); BufferIndex < NumBuffers; ++BufferIndex)
        {
            FDynamicAllocReadBuffer Buffer = FloatBufferPool.Buffers.get(BufferIndex);
            if (Buffer.MappedBuffer != null)
            {
                Buffer.Unlock();
            }
            else if (GGlobalBufferNumFramesUnusedThresold>0 && Buffer.AllocatedByteCount>0)
            {
                ++Buffer.NumFramesUnused;
                if (Buffer.NumFramesUnused >= GGlobalBufferNumFramesUnusedThresold )
                {
                    // Remove the buffer, assumes they are unordered.
                    Buffer.Release();
                    FloatBufferPool.Buffers.remove(BufferIndex);
                    --BufferIndex;
                    --NumBuffers;
                }
            }
        }
        FloatBufferPool.CurrentBuffer = null;

        for (int BufferIndex = 0, NumBuffers = Int32BufferPool.Buffers.size(); BufferIndex < NumBuffers; ++BufferIndex)
        {
            FDynamicAllocReadBuffer Buffer = Int32BufferPool.Buffers.get(BufferIndex);
            if (Buffer.MappedBuffer != null)
            {
                Buffer.Unlock();
            }
            else if (GGlobalBufferNumFramesUnusedThresold>0  && Buffer.AllocatedByteCount>0)
            {
                ++Buffer.NumFramesUnused;
                if (Buffer.NumFramesUnused >= GGlobalBufferNumFramesUnusedThresold )
                {
                    // Remove the buffer, assumes they are unordered.
                    Buffer.Release();
                    Int32BufferPool.Buffers.remove(BufferIndex);
                    --BufferIndex;
                    --NumBuffers;
                }
            }
        }
        Int32BufferPool.CurrentBuffer = null;
        TotalAllocatedSinceLastCommit = 0;
    }


    /** Returns true if log statements should be made because we exceeded GMaxVertexBytesAllocatedPerFrame */
    public boolean IsRenderAlarmLoggingEnabled() { return UE4Engine.CHECKING; }

    public void InitRHI(){

    }
    public void ReleaseRHI(){
        Cleanup();
    }

    protected void Cleanup(){
        if (FloatBufferPool != null)
        {
//            UE_LOG(LogRendererCore, Log, TEXT("FGlobalDynamicReadBuffer::Cleanup()"));
            LogUtil.i(LogUtil.LogType.DEFAULT, "FGlobalDynamicReadBuffer::Cleanup()");
            FloatBufferPool.Release();
            FloatBufferPool = null;
        }

        if (Int32BufferPool != null)
        {
            Int32BufferPool.Release();
            Int32BufferPool = null;
        }
    }

    private static final class FDynamicReadBufferPool
    {
        /** List of vertex buffers. */
        final ArrayList<FDynamicAllocReadBuffer> Buffers = new ArrayList<>();
        /** The current buffer from which allocations are being made. */
        FDynamicAllocReadBuffer CurrentBuffer;


        /** Destructor. */
        void Release()
        {
            int NumVertexBuffers = Buffers.size();
            for (int BufferIndex = 0; BufferIndex < NumVertexBuffers; ++BufferIndex)
            {
                Buffers.get(BufferIndex).Release();
            }
        }

//        FCriticalSection CriticalSection;
    };
}
