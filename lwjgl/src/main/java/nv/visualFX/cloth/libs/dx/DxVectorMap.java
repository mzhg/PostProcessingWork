package nv.visualFX.cloth.libs.dx;

import org.lwjgl.util.vector.ReadableVector3f;
import org.lwjgl.util.vector.ReadableVector4f;
import org.lwjgl.util.vector.Vector4f;

import java.nio.ByteBuffer;

import jet.opengl.postprocessing.common.Disposeable;

/**
 * Created by mazhen'gui on 2017/9/13.
 */

final class DxVectorMap implements Disposeable{
    private DxBatchedVector mVector;
    private ByteBuffer mData;
    private final Vector4f m_tmp = new Vector4f();  // internal use

    DxVectorMap( DxBatchedVector vector, int mapType){
        mVector = vector;
        mVector.mStorage.m_manager.acquireContext();
        mData = vector.map(mapType);
    }

    // not actually initializing values!
    void resize(int size)
    {
        assert (size <= mVector.capacity());
        mVector.resize(size);
    }

    int size()
    {
        return mVector.size();
    }

    ByteBuffer begin()
    {
        return mData;
    }

    /*ValueType* end()
    {
        return mData + mVector.mSize;
    }*/

//    ValueType& operator[](uint32_t i)
//    {
//        return mData[i];
//    }
    ReadableVector4f getV4(int i){
        int offset = mVector.SizeOfT * i;
        m_tmp.x = mData.getFloat(offset); offset +=4;
        m_tmp.y = mData.getFloat(offset); offset +=4;
        m_tmp.z = mData.getFloat(offset); offset +=4;
        m_tmp.w = mData.getFloat(offset); offset +=4;

        return m_tmp;
    }

    void set(int i, ReadableVector4f v){
        int offset = mVector.SizeOfT * i;
        mData.putFloat(offset, v.getX()); offset+=4;
        mData.putFloat(offset, v.getY()); offset+=4;
        mData.putFloat(offset, v.getZ()); offset+=4;
        mData.putFloat(offset, v.getW()); offset+=4;
    }

    ReadableVector3f getV3(int i){
        int offset = mVector.SizeOfT * i;
        m_tmp.x = mData.getFloat(offset); offset +=4;
        m_tmp.y = mData.getFloat(offset); offset +=4;
        m_tmp.z = mData.getFloat(offset); offset +=4;

        return m_tmp;
    }

    void set(int i, ReadableVector3f v){
        int offset = mVector.SizeOfT * i;
        mData.putFloat(offset, v.getX()); offset+=4;
        mData.putFloat(offset, v.getY()); offset+=4;
        mData.putFloat(offset, v.getZ()); offset+=4;
    }

    void set(int i, int v){
        int offset = mVector.SizeOfT * i;
        mData.putInt(offset, v);
    }

    void pushBack(ReadableVector4f v)
    {
        assert (mVector.mCapacity > mVector.mSize);
//        mData[mVector.mSize++] = value;
        int offset = mVector.SizeOfT * mVector.mSize;
        mData.putFloat(offset, v.getX()); offset+=4;
        mData.putFloat(offset, v.getY()); offset+=4;
        mData.putFloat(offset, v.getZ()); offset+=4;
        mData.putFloat(offset, v.getW()); offset+=4;

        mVector.mSize++;
    }

    void pushBack(ReadableVector3f v)
    {
        assert (mVector.mCapacity > mVector.mSize);
//        mData[mVector.mSize++] = value;
        int offset = mVector.SizeOfT * mVector.mSize;
        mData.putFloat(offset, v.getX()); offset+=4;
        mData.putFloat(offset, v.getY()); offset+=4;
        mData.putFloat(offset, v.getZ()); offset+=4;

        mVector.mSize++;
    }

    void replaceWithLast(){--mVector.mSize;}

    void replaceWithLast(Vector4f it)
    {
//		*it = mData[--mVector.mSize];
        --mVector.mSize;
        int offset = mVector.SizeOfT * mVector.mSize;
        it.x = mData.getFloat(offset); offset +=4;
        it.y = mData.getFloat(offset); offset +=4;
        it.z = mData.getFloat(offset); offset +=4;
        it.w = mData.getFloat(offset); offset +=4;
    }

    @Override
    public void dispose() {
        mVector.mStorage.m_manager.releaseContext();
        if(mData != null){
            mVector.unmap();
        }

        mVector = null;
        mData = null;
    }
}
