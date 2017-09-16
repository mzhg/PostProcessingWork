package nv.visualFX.cloth.libs.dx;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import jet.opengl.postprocessing.buffer.BufferGL;
import jet.opengl.postprocessing.common.Disposeable;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.util.BufferUtils;
import nv.visualFX.cloth.libs.DxContextManagerCallback;

/**
 * Created by mazhen'gui on 2017/9/12.
 */

final class DxBatchedStorage implements Disposeable{
    BufferGL mBuffer;
    int mSize;
    final List<DxBatchedVector> mViews = new ArrayList<>();
    int mMapRefCount;
    ByteBuffer mMapPointer;

    private int m_target;
    private int m_usage;
    final int SizeOfT;
    private GLFuncProvider gl;
    DxContextManagerCallback m_manager;

    DxBatchedStorage(DxContextManagerCallback manager, int target, int usage, int stride)
//            : mBuffer(manager, flags), mSize(0), mMapRefCount(0), mMapPointer(0)
    {
        m_manager = manager;
//        mBuffer = new BufferGL();
        m_target = target;
        m_usage = usage;
        SizeOfT = stride;
        gl = GLFuncProviderFactory.getGLFuncProvider();
    }

    void add(DxBatchedVector view)
    {
        mViews.add(view);
        view.mOffset = mSize;
    }

    void reserve(DxBatchedVector view, int capacity)
    {
        if (view.mCapacity >= capacity)
            return;
        int index = view.mOffset + view.mSize;
        int delta = capacity - view.mCapacity;
//        replace(index, index, nullptr, (T*)nullptr + delta);
        replace(index, index, delta * SizeOfT, null);
        update(view, delta);
        view.mCapacity = capacity;
    }

    void assign(DxBatchedVector view, Buffer data, int newSize)
    {
        int offset = view.mOffset;
        int oldSize = Math.min(newSize, view.mCapacity);
        replace(offset, offset + oldSize, newSize, data);
        update(view, newSize - oldSize);
        view.mSize = newSize;
        if (newSize > view.mCapacity)
            view.mCapacity = newSize;
    }

    void remove(DxBatchedVector view)
    {
        int offset = view.mOffset;
        int capacity = view.mCapacity;
        replace(offset, offset + capacity, 0, null);
        update(view, -capacity);
//        DxBatchedVector<T>** it = mViews.find(view);
//        mViews.remove(uint32_t(it - mViews.begin()));
        mViews.remove(view);
    }

    ByteBuffer map(int mapType /*= D3D11_MAP_READ_WRITE*/)
    {
//        if (!mMapRefCount++)
        if(mMapRefCount == 0) {
            mMapPointer = mBuffer.map(mapType);
            mMapRefCount ++;
        }
        return mMapPointer;
    }

    void unmap()
    {
//        if (!--mMapRefCount)
        if(mMapRefCount == 1)
        {
            mBuffer.unmap();
            mMapPointer = null;
        }
    }

    // not updating mSize!
    void replace(int first, int last, int data_length, Buffer data)
    {
        int tail = first + /*uint32_t(end - begin)*/ (data != null ? BufferUtils.measureSize(data) / SizeOfT : data_length /SizeOfT);
        int newSize = tail == last ? 0 : mSize + tail - last;
        if (newSize > 0)
        {
            // Optimization: dx11.1 would allow in place copies
            // with ID3D11DeviceContext1::CopySubresourceRegion1

//            DxBuffer<T> buffer = DxBuffer<T>(mBuffer.mManager, mBuffer);
//            buffer.reserve(newSize);
            BufferGL buffer = new BufferGL();
            buffer.initlize(m_target, newSize * SizeOfT, null, m_usage);

            if (0 < first && mBuffer != null)
            {
                assert(mMapRefCount == 0);
//                CD3D11_BOX box(0, 0, 0, first * SizeOfT, 1, 1);
//                mBuffer.context()->CopySubresourceRegion(buffer.mBuffer, 0, 0, 0, 0, mBuffer.mBuffer, 0, &box);
                CopySubresourceRegion(buffer, 0, mBuffer, 0, first * SizeOfT, gl);
            }

            if (last < mSize && mBuffer != null)
            {
//                NV_CLOTH_ASSERT(!mMapRefCount);
                assert(mMapRefCount == 0);
//                CD3D11_BOX box(last * SizeOfT, 0, 0, mSize * SizeOfT, 1, 1);
//                mBuffer.context()->CopySubresourceRegion(buffer.mBuffer, 0, tail * SizeOfT, 0, 0, mBuffer.mBuffer, 0,
//                    &box);
                CopySubresourceRegion(buffer, tail * SizeOfT, mBuffer, last * SizeOfT, (mSize-last) * SizeOfT, gl);
            }

//            physx::shdfnd::swap(mBuffer, buffer);
            BufferGL tmp = mBuffer;
            mBuffer = buffer;
            buffer = tmp;
            if(buffer != null){
                buffer.dispose();
            }
        }

//        if (begin && end > begin)
        if(data!=null&& data.remaining() > 0)
        {
            /*if (mBuffer.mUsage == D3D11_USAGE_DEFAULT)
            {
                NV_CLOTH_ASSERT(!mMapRefCount);
                CD3D11_BOX box(first * SizeOfT, 0, 0, tail * SizeOfT, 1, 1);
                mBuffer.context()->UpdateSubresource(mBuffer.mBuffer, 0, &box, begin, 0, 0);
            }
            else
            {
                memcpy(map(D3D11_MAP_WRITE) + first, begin, uint32_t(end - begin) * SizeOfT);
                unmap();
            }*/
            mBuffer.update(first * SizeOfT, data);
            mBuffer.unbind();
        }
    }

    void update(DxBatchedVector view, int delta)
    {
		final int offset = view.mOffset;
//        DxBatchedVector<T>** it = mViews.begin();
        for (int i = mViews.size() -1; 0 <= i--;)
        {
            DxBatchedVector it = mViews.get(i);
            if (it != view && it.mOffset >= offset)
                it.mOffset += delta;
        }
        mSize += delta;
    }

    static void CopySubresourceRegion(BufferGL writeBuffer, int writeOffset,
                                      BufferGL readBuffer, int readOffset, int length, GLFuncProvider gl){
        gl.glBindBuffer(GLenum.GL_COPY_READ_BUFFER, readBuffer.getBuffer());
        gl.glBindBuffer(GLenum.GL_COPY_WRITE_BUFFER, writeBuffer.getBuffer());
        gl.glCopyBufferSubData(GLenum.GL_COPY_READ_BUFFER, GLenum.GL_COPY_WRITE_BUFFER, readOffset, writeOffset, length);
    }

    @Override
    public void dispose() {
        if(mBuffer != null) mBuffer.dispose();
    }
}
