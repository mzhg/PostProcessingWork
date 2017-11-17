package jet.opengl.demos.intel.cput;

import org.lwjgl.util.vector.Vector3f;

import jet.opengl.postprocessing.util.Numeric;

/**
 * Created by mazhen'gui on 2017/11/11.
 */

final class CPUTRawMeshData {
    int                       mStride; // how big a "vertex" is
    int                       mVertexCount;
    byte[]                    mpVertices;
    int                       mIndexCount;
    int[]                    mpIndices; // TODO: what about 16-bit indices?  Use void*?
    int                       mFormatDescriptorCount;
    CPUTVertexElementDesc[]     mpElements;
    long           mTotalVerticesSizeInBytes;
    int        mTopology;
    final Vector3f mBboxCenter = new Vector3f();
    final Vector3f                     mBboxHalf = new Vector3f();
    int  mIndexType;
    int                       mPaddingSize;

    void Allocate(int numElements){
        mVertexCount = numElements;
        mStride += mPaddingSize; // TODO: move this to stride computation
        mTotalVerticesSizeInBytes = mVertexCount * mStride;
        mpVertices = new byte[(int)mTotalVerticesSizeInBytes];
//    ::memset( mpVertices, 0, (size_t)mTotalVerticesSizeInBytes );
    }

    int Read(byte[] data, int position){
        /*unsigned __int32 magicCookie;
        modelFile.read((char*)&magicCookie,sizeof(magicCookie));
        if( !modelFile.good() ) return false; // TODO: Yuck!  Why do we need to get here to figure out we're done?
        ASSERT( magicCookie == 1234, _L("Invalid model file.") );*/
        int magicCookie = Numeric.getInt(data, position); position += 4;
        if(magicCookie != 1234){
            throw new IllegalArgumentException("Invalid model file.");
        }

        /*modelFile.read((char*)&mStride,                   sizeof(mStride));
        modelFile.read((char*)&mPaddingSize,              sizeof(mPaddingSize)); // DWM TODO: What is this?
        modelFile.read((char*)&mTotalVerticesSizeInBytes, sizeof(mTotalVerticesSizeInBytes));
        modelFile.read((char*)&mVertexCount,              sizeof(mVertexCount));
        modelFile.read((char*)&mTopology,                 sizeof(mTopology));
        modelFile.read((char*)&mBboxCenter,               sizeof(mBboxCenter));
        modelFile.read((char*)&mBboxHalf,                 sizeof(mBboxHalf));*/
        mStride = Numeric.getInt(data, position);  position+=4;
        mPaddingSize = Numeric.getInt(data, position);  position+=4;
        mTotalVerticesSizeInBytes = Numeric.getLong(data, position);  position+=8;
        mVertexCount = Numeric.getInt(data, position);  position+=4;
        mTopology = Numeric.getInt(data, position);  position+=4;
        mBboxCenter.x = Numeric.getFloat(data, position);  position+=4;
        mBboxCenter.y = Numeric.getFloat(data, position);  position+=4;
        mBboxCenter.z = Numeric.getFloat(data, position);  position+=4;
        mBboxHalf.x = Numeric.getFloat(data, position);  position+=4;
        mBboxHalf.y = Numeric.getFloat(data, position);  position+=4;
        mBboxHalf.z = Numeric.getFloat(data, position);  position+=4;

        // read  format descriptors
        /*modelFile.read((char*)&mFormatDescriptorCount, sizeof(mFormatDescriptorCount));
        ASSERT( modelFile.good(), _L("Model file bad" ) );*/
        mFormatDescriptorCount = Numeric.getInt(data, position);  position+=4;

        mpElements = new CPUTVertexElementDesc[mFormatDescriptorCount];
        for( int ii=0; ii<mFormatDescriptorCount; ++ii )
        {
            mpElements[ii] = new CPUTVertexElementDesc();
            position = mpElements[ii].Read(data, position);
        }
        /*modelFile.read((char*)&mIndexCount, sizeof(mIndexCount));
        modelFile.read((char*)&mIndexType, sizeof(mIndexType));
        ASSERT( modelFile.good(), _L("Bad model file(1)." ) );*/
        mIndexCount = Numeric.getInt(data, position);  position+=4;
        mIndexType = Numeric.getInt(data, position);  position+=4;

        mpIndices = new int[mIndexCount];
        if( mIndexCount != 0 )
        {
//            modelFile.read((char*)mpIndices, mIndexCount * sizeof(UINT));
            Numeric.toInts(data, position, mpIndices, 0, mIndexCount);
            position += mIndexCount*4;
        }
        /*modelFile.read((char*)&magicCookie, sizeof(magicCookie));
        ASSERT( magicCookie == 1234, _L("Model file missing magic cookie.") );
        ASSERT( modelFile.good(),    _L("Bad model file(2).") );*/
        magicCookie = Numeric.getInt(data, position);  position+=4;
        if(magicCookie != 1234){
            throw new IllegalArgumentException("Invalid model file.");
        }

        if ( 0 != mTotalVerticesSizeInBytes )
        {
            Allocate(mVertexCount);  // recalculates some things
//            modelFile.read((char*)(mpVertices), mTotalVerticesSizeInBytes);
            System.arraycopy(data, position, mpVertices, 0, (int)mTotalVerticesSizeInBytes);
            position += (int)mTotalVerticesSizeInBytes;
        }
        /*modelFile.read((char*)&magicCookie, sizeof(magicCookie));
        ASSERT( modelFile.good() && magicCookie == 1234, _L("Bad model file(3).") );*/
        magicCookie = Numeric.getInt(data, position);  position+=4;
        if(magicCookie != 1234){
            throw new IllegalArgumentException("Invalid model file.");
        }

        return position;
    }
}
