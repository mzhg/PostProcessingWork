package nv.visualFX.cloth.libs.dx;

import java.nio.Buffer;
import java.nio.ByteBuffer;

import jet.opengl.postprocessing.buffer.BufferGL;
import jet.opengl.postprocessing.common.Disposeable;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;

/**
 * Created by mazhen'gui on 2017/9/12.
 */

final class DxBatchedVector implements Disposeable{
    DxBatchedStorage mStorage;
    int mOffset, mSize, mCapacity;
    final int SizeOfT;
    private GLFuncProvider gl;

    DxBatchedVector(DxBatchedStorage array) //: mStorage(array), mSize(0), mCapacity(0)
    {
        mStorage = array;
        mStorage.add(this);
        SizeOfT =array.SizeOfT;
        gl = GLFuncProviderFactory.getGLFuncProvider();
    }


    @Override
    public void dispose() {
        mStorage.remove(this);
    }

    DxBatchedVector(DxBatchedVector other)// : mStorage(other.mStorage), mSize(0), mCapacity(0)
    {
        mStorage = other.mStorage;
        mStorage.add(this);
        mStorage.reserve(this, other.mCapacity);
        SizeOfT =other.SizeOfT;
        gl = GLFuncProviderFactory.getGLFuncProvider();

//        CD3D11_BOX box(other.mOffset * sizeof(T), 0, 0, (other.mOffset + other.size()) * sizeof(T), 1, 1);
//        mStorage.mBuffer.context()->CopySubresourceRegion(buffer(), 0, mOffset * sizeof(T), 0, 0, other.buffer(), 0,
//            &box);
        DxBatchedStorage.CopySubresourceRegion(buffer(), mOffset * SizeOfT, other.buffer(), other.mOffset * SizeOfT, other.size() * SizeOfT, gl);

        mSize = other.size();
    }

//    template <typename Alloc>
//    DxBatchedVector& operator = (const physx::shdfnd::Array<T, Alloc>& other)
//    {
//        assign(other.begin(), other.end());
//        return *this;
//    }

    /*DxBatchedVector& operator = (const DxBatchedVector& other)
    {
        NV_CLOTH_ASSERT(mSize == other.size()); // current limitation
        NV_CLOTH_ASSERT(!mStorage.mMapRefCount); // This will trigger if the user still has a reference to the MappedRange returned by Cloth::getCurrentParticles

        CD3D11_BOX box(other.mOffset * sizeof(T), 0, 0, (other.mOffset + other.size()) * sizeof(T), 1, 1);
        mStorage.mBuffer.context()->CopySubresourceRegion(buffer(), 0, mOffset * sizeof(T), 0, 0, other.buffer(), 0,
            &box);

        return *this;
    }

    DxBatchedVector& operator = (const DxDeviceVector<T>& other)
    {
        NV_CLOTH_ASSERT(mSize == other.size()); // current limitation
        NV_CLOTH_ASSERT(!mStorage.mMapRefCount);

        mStorage.mBuffer.context()->CopySubresourceRegion(buffer(), 0, mOffset * sizeof(T), 0, 0, other.buffer(), 0,
            nullptr);

        return *this;
    }*/

    void load(DxBatchedVector other){
        assert (mSize == other.size()); // current limitation
        assert (mStorage.mMapRefCount == 0); // This will trigger if the user still has a reference to the MappedRange returned by Cloth::getCurrentParticles

        DxBatchedStorage.CopySubresourceRegion(buffer(), mOffset * SizeOfT, other.buffer(), other.mOffset * SizeOfT, other.size() * SizeOfT, gl);
    }

    int capacity()
    {
        return mCapacity;
    }
    boolean empty()
    {
        return mSize == 0;
    }
    int size()
    {
        return mSize;
    }

    void reserve(int capacity)
    {
        mStorage.reserve(this, capacity);
    }

    void resize(int size)
    {
        mStorage.assign(this, null, size);
    }

    void assign(int size, Buffer data)
    {
//        mStorage.assign(this, first, uint32_t(last - first));
        mStorage.assign(this, data, size);
    }

    // attention: data of this vector starts at mOffset
    BufferGL buffer()
    {
        return mStorage.mBuffer;
    }

    ByteBuffer map() { return map(GLenum.GL_READ_WRITE);}
    ByteBuffer map(int mapType /*= D3D11_MAP_READ_WRITE*/)
    {
//        return buffer() != null ? mStorage.map(mapType) + mOffset : 0;
        BufferGL bufferGL = buffer();
        if(bufferGL != null){
            ByteBuffer data = mStorage.map(mapType);
            data.position(mOffset * SizeOfT);
            return data;
        }

        return null;
    }

    void unmap()
    {
        mStorage.unmap();
    }

    // common interface with DxDeviceVector for DxVectorMap
//    DxContextManagerCallback manager()
//    {
//        return mStorage.mBuffer.mManager;
//    }

    void swap(DxBatchedVector other)
    {
        assert(mStorage == other.mStorage);
//        physx::shdfnd::swap(mOffset, other.mOffset);
//        physx::shdfnd::swap(mSize, other.mSize);
//        physx::shdfnd::swap(mCapacity, other.mCapacity);

        {
            int it = mOffset;
            mOffset = other.mOffset;
            other.mOffset = it;
        }

        {
            int it = mSize;
            mSize = other.mSize;
            other.mSize = it;
        }

        {
            int it = mCapacity;
            mCapacity = other.mCapacity;
            other.mCapacity = it;
        }

        // alternative to running through all elements in DxBatchedStorage::update()
        // however, swap should be O(1) and is used more frequently than reserve/add/remove
        // nvidia::swap(*mStorage.mViews.find(&left), *other.mStorage.mViews.find(&right));
    }

    void clear()
    {
        //TODO: make more efficient impl.
//        DxBatchedVector<T> temp(mStorage);
//        this->swap(temp);
        mSize = 0;
        mOffset = 0;
        mCapacity = 0;
    }
}
