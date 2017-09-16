package nv.visualFX.cloth.libs.dx;

import java.nio.FloatBuffer;

/**
 * Created by mazhen'gui on 2017/9/13.
 */

final class DxConstraints {
//    DxConstraints::DxConstraints(DxBatchedStorage<physx::PxVec4>& storage)
//		: mStart(storage), mTarget(storage)
//    {
//    }
    DxConstraints(DxBatchedStorage storage){
        mStart = new DxBatchedVector(storage);
        mTarget = new DxBatchedVector(storage);
    }

    DxConstraints(DxConstraints other){
        mStart = new DxBatchedVector(other.mStart);
        mTarget = new DxBatchedVector(other.mTarget);
        //TODO mHostCopy
    }

    void pop()
    {
        if (!mTarget.empty())
        {
            mStart.swap(mTarget);
            mTarget.resize(0);
        }
    }

    DxBatchedVector/*<physx::PxVec4>*/ mStart;
    DxBatchedVector/*<physx::PxVec4>*/ mTarget;
//    Vector<physx::PxVec4>::Type mHostCopy;
    FloatBuffer mHostCopy;
}
