package nv.visualFX.cloth.libs.dx;

import java.nio.Buffer;
import java.nio.ByteBuffer;

import jet.opengl.postprocessing.buffer.BufferGL;
import jet.opengl.postprocessing.common.Disposeable;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.util.BufferUtils;
import nv.visualFX.cloth.libs.DxContextManagerCallback;

/**
 * Created by mazhen'gui on 2017/9/13.
 */

final class DxDeviceVector implements Disposeable{
    final int SizeOfT;

    BufferGL mBuffer;
    int mSize;

    private int m_target;
    private int m_usage;
    private DxContextManagerCallback m_manger;

    DxDeviceVector(DxContextManagerCallback manager, int target, int usage, int sizeOfT){
        SizeOfT = sizeOfT;
        m_target = target;
        m_usage = usage;
        m_manger = manager;
    }

    DxDeviceVector(Buffer data, DxContextManagerCallback manager, int target, int usage, int sizeOfT){
        SizeOfT = sizeOfT;
        m_target = target;
        m_usage = usage;
        m_manger = manager;

        mBuffer = new BufferGL();
        mBuffer.initlize(target, data.remaining(), data, usage);
        mBuffer.unbind();
    }

    DxDeviceVector(DxDeviceVector other){
        SizeOfT = other.SizeOfT;
        set(other);
    }

    void set(DxDeviceVector other){
        mBuffer = other.mBuffer;
//        SizeOfT = other.SizeOfT;
        m_target = other.m_target;
        m_usage = other.m_usage;
        m_manger = other.m_manger;
    }

    @Override
    public void dispose() {
        if(mBuffer != null){
            mBuffer.dispose();
            mBuffer = null;
        }
    }

    int capacity()
    {
        return /*mBuffer.mCapacity*/ mBuffer.getBufferSize()/SizeOfT;
    }
    boolean empty()
    {
        return mSize == 0;
    }
    int size()
    {
        return mSize;
    }

    void reserve(int n)
    {
//        mBuffer.reserve(n);
        if(mBuffer== null || mBuffer.getBufferSize() < n * SizeOfT){
            BufferGL new_buffer = new BufferGL();
            new_buffer.initlize(m_target, n * SizeOfT, null, m_usage);
            if(mBuffer != null && mSize > 0){
                DxBatchedStorage.CopySubresourceRegion(new_buffer, 0, mBuffer, 0, mBuffer.getBufferSize(), GLFuncProviderFactory.getGLFuncProvider());
            }

            if(mBuffer != null){
                mBuffer.dispose();
            }

            mBuffer = new_buffer;
        }
    }

    void resize(int n)
    {
        if (mBuffer == null ||  mBuffer.getBufferSize() < n * SizeOfT)
            reserve(Math.max(n, mBuffer.getBufferSize()/SizeOfT * 2));
        mSize = n;
    }

    void assign(Buffer data)
    {
        mSize = /*uint32_t(last - first)*/BufferUtils.measureSize(data)/SizeOfT;

        if (mSize == 0)
            return;

        if (mBuffer == null || mSize > mBuffer.getBufferSize()/SizeOfT)
        {
//            mBuffer = DxBuffer<T>(first, last, mBuffer.mManager, mBuffer);
            BufferGL new_buffer = new BufferGL();
            new_buffer.initlize(m_target, mSize * SizeOfT, data, m_usage);
            if(mBuffer != null){
                mBuffer.dispose();
            }
            mBuffer = new_buffer;
        }
        else
        {
            /*if (mBuffer.mUsage == D3D11_USAGE_DEFAULT)
            {
                CD3D11_BOX box(0, 0, 0, mSize * SizeOfT, 1, 1);
                mBuffer.context()->UpdateSubresource(mBuffer.mBuffer, 0, &box, first, 0, 0);
            }
            else
            {
                memcpy(map(D3D11_MAP_WRITE_DISCARD), first, mSize * SizeOfT);
                unmap();
            }*/

            mBuffer.update(0, data);
        }
    }

    void swap(DxDeviceVector other)
    {
//        physx::shdfnd::swap(mBuffer, other.mBuffer);
//        physx::shdfnd::swap(mSize, other.mSize);

        BufferGL tmp = mBuffer;
        mBuffer = other.mBuffer;
        other.mBuffer = tmp;

        int t = mSize;
        mSize = other.mSize;
        other.mSize = t;
    }

    ByteBuffer map(int mapType)
    {
        return mBuffer.map(mapType);
    }

    void unmap()
    {
        mBuffer.unmap();
    }

    // common interface with DxBatchedVector for DxVectorMap
    DxContextManagerCallback manager()
    {
        return m_manger;
    }
}
