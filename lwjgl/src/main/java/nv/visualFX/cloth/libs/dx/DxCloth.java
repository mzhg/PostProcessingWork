package nv.visualFX.cloth.libs.dx;

import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.vector.ReadableVector3f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.List;

import jet.opengl.postprocessing.buffer.BufferGL;
import jet.opengl.postprocessing.common.Disposeable;
import jet.opengl.postprocessing.common.GLCheck;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.util.BufferUtils;
import jet.opengl.postprocessing.util.CachaRes;
import jet.opengl.postprocessing.util.CacheBuffer;
import jet.opengl.postprocessing.util.LogUtil;
import nv.visualFX.cloth.libs.Cloth;
import nv.visualFX.cloth.libs.ClothImpl;
import nv.visualFX.cloth.libs.Fabric;
import nv.visualFX.cloth.libs.Factory;
import nv.visualFX.cloth.libs.GpuParticles;
import nv.visualFX.cloth.libs.MovingAverage;
import nv.visualFX.cloth.libs.PhaseConfig;
import nv.visualFX.cloth.libs.TripletScheduler;

/**
 * Created by mazhen'gui on 2017/9/12.
 */

final class DxCloth extends ClothImpl implements Disposeable{

    DxFactory mFactory;
    DxFabric mFabric;

    boolean mClothDataDirty;
    boolean mClothCostDirty = true;

    // particle data
    int mNumParticles;
    DxBatchedVector/*<physx::PxVec4>*/ mParticles; // cur, prev
    final DxBatchedVector/*<physx::PxVec4>*/ mParticlesHostCopy;
    ByteBuffer mParticlesMapPointer;
    int mParticlesMapRefCount;

    boolean mDeviceParticlesDirty = true;
    boolean mHostParticlesDirty;

    final DxBatchedVector/*<DxPhaseConfig>*/ mPhaseConfigs;
    final List<PhaseConfig> mHostPhaseConfigs;

    // tether constraints stuff
    float mTetherConstraintLogStiffness;
    float mTetherConstraintScale;

    // motion constraints stuff
    final DxConstraints mMotionConstraints;
    float mMotionConstraintScale;
    float mMotionConstraintBias;
    float mMotionConstraintLogStiffness;

    // separation constraints stuff
    final DxConstraints mSeparationConstraints;

    // particle acceleration stuff
    final DxBatchedVector/*<physx::PxVec4>*/ mParticleAccelerations;
    FloatBuffer mParticleAccelerationsHostCopy;

    // collision stuff
    final DxBatchedVector/*<IndexPair>*/ mCapsuleIndices;
    DxBatchedVector/*<physx::PxVec4>*/ mStartCollisionSpheres;
    DxBatchedVector/*<physx::PxVec4>*/ mTargetCollisionSpheres;
    final DxBatchedVector/*<uint32_t>*/ mConvexMasks;
    DxBatchedVector/*<physx::PxVec4>*/ mStartCollisionPlanes;
    DxBatchedVector/*<physx::PxVec4>*/ mTargetCollisionPlanes;
    DxBatchedVector/*<physx::PxVec3>*/ mStartCollisionTriangles;
    DxBatchedVector/*<physx::PxVec3>*/ mTargetCollisionTriangles;
    boolean mEnableContinuousCollision;
    float mCollisionMassScale;
    float mFriction;

    // virtual particles
    DxDeviceVector/*<uint32_t>*/ mVirtualParticleSetSizes;
    DxDeviceVector/*<Vec4us>*/ mVirtualParticleIndices;
    DxDeviceVector/*<physx::PxVec4>*/ mVirtualParticleWeights;

    // self collision
    float mSelfCollisionDistance;
    float mSelfCollisionLogStiffness;

    final DxBatchedVector/*<physx::PxVec4>*/ mRestPositions;
    final DxBatchedVector/*<uint32_t>*/ mSelfCollisionIndices;
    IntBuffer mSelfCollisionIndicesHost;

    final DxBatchedVector/*<physx::PxVec4>*/ mSelfCollisionParticles;
    // 2x(key) per particle + cellStart (8322)
    final DxBatchedVector/*<uint32_t>*/ mSelfCollisionData;

    boolean mInitSelfCollisionData;

    int mSharedMemorySize;

    Object mUserData;

    DxCloth(DxFactory factory, DxFabric fabric, /*Range<const PxVec4>*/FloatBuffer particles){
        mFactory = factory;
        mFabric = fabric;
        mNumParticles = particles.remaining()/4;
        mMotionConstraints = new DxConstraints(mFactory.mMotionConstraints);
        mSeparationConstraints = new DxConstraints(mFactory.mSeparationConstraints);
        mParticles = new DxBatchedVector(mFactory.mParticles);
        mParticlesHostCopy = new DxBatchedVector(mFactory.mParticlesHostCopy);
        mPhaseConfigs = new DxBatchedVector(mFactory.mPhaseConfigs);
        mParticleAccelerations = new DxBatchedVector(mFactory.mParticleAccelerations);
        mCapsuleIndices = new DxBatchedVector(mFactory.mCapsuleIndices);
        mStartCollisionSpheres = new DxBatchedVector(mFactory.mCollisionSpheres);
        mTargetCollisionSpheres = new DxBatchedVector(mFactory.mCollisionSpheres);
        mConvexMasks = new DxBatchedVector(mFactory.mConvexMasks);
        mStartCollisionPlanes = new DxBatchedVector(mFactory.mCollisionPlanes);
        mTargetCollisionPlanes = new DxBatchedVector(mFactory.mCollisionPlanes);
        mStartCollisionTriangles = new DxBatchedVector(mFactory.mCollisionTriangles);
        mTargetCollisionTriangles = new DxBatchedVector(mFactory.mCollisionTriangles);
        mVirtualParticleSetSizes = new DxDeviceVector(mFactory.mContextManager, GLenum.GL_SHADER_STORAGE_BUFFER, GLenum.GL_DYNAMIC_READ, 4);
        mVirtualParticleIndices = new DxDeviceVector(mFactory.mContextManager, GLenum.GL_SHADER_STORAGE_BUFFER, GLenum.GL_DYNAMIC_READ, 4*2);
        mVirtualParticleWeights=new DxDeviceVector(mFactory.mContextManager, GLenum.GL_SHADER_STORAGE_BUFFER, GLenum.GL_DYNAMIC_READ, Vector4f.SIZE);
        mRestPositions = new DxBatchedVector(mFactory.mRestPositions);
        mSelfCollisionIndices = new DxBatchedVector(mFactory.mSelfCollisionIndices);
        mSelfCollisionParticles = new DxBatchedVector(mFactory.mSelfCollisionParticles);
        mSelfCollisionData = new DxBatchedVector(mFactory.mSelfCollisionData);
        mHostPhaseConfigs = new ArrayList<>();

        acquire();
        assert (particles.remaining() > 0);
        assert (particles.remaining()/4 == fabric.getNumParticles());

        initialize(this, particles);

        mParticlesHostCopy.resize(2 * mNumParticles);
//        PxVec4* pIt = mParticlesHostCopy.map(D3D11_MAP_WRITE);
//        memcpy(pIt, particles.begin(), mNumParticles * sizeof(PxVec4));
//        memcpy(pIt + mNumParticles, particles.begin(), mNumParticles * sizeof(PxVec4));
//        mParticlesHostCopy.unmap();
        ByteBuffer pIt = mParticlesHostCopy.map(GLenum.GL_WRITE_ONLY);
        long dst = MemoryUtil.memAddress(pIt);
        long src = MemoryUtil.memAddress(particles);
        MemoryUtil.memCopy(src, dst, mNumParticles * Vector4f.SIZE);
        MemoryUtil.memCopy(src, dst + mNumParticles * Vector4f.SIZE, mNumParticles * Vector4f.SIZE);
        mParticlesHostCopy.unmap();

        mParticles.resize(2 * mNumParticles);
//        mFabric.incRefCount();

        release();
    }

    DxCloth(DxFactory factory, DxCloth cloth){
        mFactory = factory;

        acquire();
        mFabric = (cloth.mFabric);
        mNumParticles = (cloth.mNumParticles);
        mParticles = (cloth.mParticles);
        mParticlesHostCopy = new DxBatchedVector(cloth.mParticlesHostCopy);
        mParticlesMapPointer = null;
        mParticlesMapRefCount = 0;
        mDeviceParticlesDirty = (cloth.mDeviceParticlesDirty);
        mHostParticlesDirty = (cloth.mHostParticlesDirty);
        mPhaseConfigs = cloth.mPhaseConfigs;
        mHostPhaseConfigs = cloth.mHostPhaseConfigs;
        mMotionConstraints = new DxConstraints(cloth.mMotionConstraints);
        mSeparationConstraints = new DxConstraints(cloth.mSeparationConstraints);
        mParticleAccelerations = new DxBatchedVector(cloth.mParticleAccelerations);
        mParticleAccelerationsHostCopy =  (cloth.mParticleAccelerationsHostCopy);  // TODO reference copy
        mCapsuleIndices = new DxBatchedVector(cloth.mCapsuleIndices);
        mStartCollisionSpheres = new DxBatchedVector(cloth.mStartCollisionSpheres);
        mTargetCollisionSpheres = new DxBatchedVector(cloth.mTargetCollisionSpheres);
        mConvexMasks = new DxBatchedVector(cloth.mConvexMasks);
        mStartCollisionPlanes = new DxBatchedVector(cloth.mStartCollisionPlanes);
        mTargetCollisionPlanes = new DxBatchedVector(cloth.mTargetCollisionPlanes);
        mStartCollisionTriangles = new DxBatchedVector(cloth.mStartCollisionTriangles);
        mTargetCollisionTriangles = new DxBatchedVector(cloth.mTargetCollisionTriangles);
        mVirtualParticleSetSizes = new DxDeviceVector(cloth.mVirtualParticleSetSizes);
        mVirtualParticleIndices = new DxDeviceVector(cloth.mVirtualParticleIndices);
        mVirtualParticleWeights = new DxDeviceVector(cloth.mVirtualParticleWeights);
        mRestPositions = new DxBatchedVector(cloth.mRestPositions);
        mSelfCollisionIndices = new DxBatchedVector(cloth.mSelfCollisionIndices);
        mSelfCollisionParticles = new DxBatchedVector(cloth.mSelfCollisionParticles);
        mSelfCollisionData =new DxBatchedVector(cloth.mSelfCollisionData);
        mInitSelfCollisionData =(cloth.mInitSelfCollisionData);
        mSharedMemorySize=(cloth.mSharedMemorySize);
        mUserData=(cloth.mUserData);

        copy(this, cloth);
        release();
    }

    @Override
    public Cloth clone(Factory factory) {
        return null;
    }

    @Override
    public Fabric getFabric() {
        return null;
    }

    @Override
    public Factory getFactory() {
        return null;
    }

    @Override
    public int getNumParticles() {
        return mNumParticles;
    }

    @Override
    public void lockParticles() {
        mapParticles();
    }

    @Override
    public void unlockParticles() {
        unmapParticles();
    }

    @Override
    public FloatBuffer getCurrentParticles() {
        wakeUp();
        lockParticles();
        mDeviceParticlesDirty = true;
        return mParticlesMapPointer.asFloatBuffer();  // TODO
    }

    @Override
    public FloatBuffer getPreviousParticles() {

        wakeUp();
        lockParticles();
        mDeviceParticlesDirty = true;

        mParticlesMapPointer.position(mNumParticles * Vector4f.SIZE);
        FloatBuffer result = mParticlesMapPointer.asFloatBuffer();
        mParticlesMapPointer.position(0);
        return result /*getMappedParticles(mParticlesMapPointer + mNumParticles)*/;
    }

    @Override
    public GpuParticles getGpuParticles() {
        BufferGL buffer = mParticles.buffer();
//        PxVec4* offset = (PxVec4*)nullptr + mParticles.mOffset;
//        GpuParticles result = { offset, offset + mNumParticles, buffer };
        int offset = mParticles.mOffset;
        return new GpuParticles(offset, offset + mNumParticles, buffer);
    }

    // convert from user input to solver format
    static PhaseConfig transform(PhaseConfig config)
    {
        PhaseConfig result = new PhaseConfig(config.mPhaseIndex);

        result.mStiffness = safeLog2(1.0f - config.mStiffness);
        result.mStiffnessMultiplier = safeLog2(config.mStiffnessMultiplier);

        // negative for compression, positive for stretch
        result.mCompressionLimit = 1.f - 1.f / config.mCompressionLimit;
        result.mStretchLimit = 1.f - 1.f / config.mStretchLimit;

        return result;
    }

    @Override
    public void setPhaseConfig(List<PhaseConfig> configs) {
        List<PhaseConfig> transformedConfigs = new ArrayList<>(configs.size());

        for(PhaseConfig config : configs){
            if(config.mStiffness > 0.0f){
                transformedConfigs.add(transform(config));
            }
        }

        // TODO bad performance.
        setPhaseConfigInternal(transformedConfigs);
        notifyChanged();
        wakeUp();
    }

    private void acquire(){
        mFactory.mContextManager.acquireContext();
    }

    private void release(){
        mFactory.mContextManager.releaseContext();
    }

//    private static final byte[] m_bytes16 = new byte[16];

    static void move(ByteBuffer it, int first, int last, int result, int stride)
    {
        if (result > first)
        {
            result += last - first;
            while (first < last){
//                it[--result] = it[--last];

                int last_offset = (--last) * stride;
                int result_offset = (--result) * stride;
                MemoryUtil.memCopy(MemoryUtil.memAddress(it, last_offset), MemoryUtil.memAddress(it, result_offset), stride);
            }
        }
        else
        {
            while (first < last) {
//                it[result++] = it[first++];
                int first_offset = (first++) * stride;
                int result_offset = (result++) * stride;
                MemoryUtil.memCopy(MemoryUtil.memAddress(it, first_offset), MemoryUtil.memAddress(it, result_offset), stride);
            }
        }
    }

    // update capsule index
    static boolean updateIndex(int index, int first, int delta)
    {
        return index >= first && (index += delta) < (first);
    }

    @Override
    public void setSpheres(/*Range<const physx::PxVec4>*/FloatBuffer spheres, int first, int last) {
        int oldSize = getChildCloth().mStartCollisionSpheres.size();
        int newSize = spheres.remaining()/4 + oldSize - last + first;

        assert (newSize <= 32);
        assert (first <= oldSize);
        assert (last <= oldSize);

//#if PX_DEBUG
        if(GLCheck.CHECK){
//            for (const physx::PxVec4* it = spheres.begin(); it < spheres.end(); ++it)
//            NV_CLOTH_ASSERT(it->w >= 0.0f);
            for(int i = spheres.position() + 3; i < spheres.limit(); i+=4){
                if(spheres.get(i) < 0.0f){
                    throw new IllegalArgumentException();
                }
            }
        }

//#endif

        if (oldSize == 0 && newSize == 0)
            return;

        if (oldSize == 0)
        {
//            ContextLockType contextLock(getChildCloth()->mFactory);
            acquire();
            getChildCloth().mStartCollisionSpheres.assign(spheres.remaining() * 4, spheres);
            getChildCloth().notifyChanged();
            release();
        }
        else
        {
            if (Math.max(oldSize, newSize) >
                    Math.min(getChildCloth().mStartCollisionSpheres.capacity(), getChildCloth().mTargetCollisionSpheres.capacity()))
            {
//                ContextLockType contextLock(getChildCloth()->mFactory);
                acquire();
                getChildCloth().mStartCollisionSpheres.reserve(newSize);
                getChildCloth().mTargetCollisionSpheres.reserve(Math.max(oldSize, newSize));
                release();
            }

            DxVectorMap start = new DxVectorMap(getChildCloth().mStartCollisionSpheres, GLenum.GL_READ_WRITE);
            DxVectorMap target = new DxVectorMap(getChildCloth().mTargetCollisionSpheres, GLenum.GL_READ_WRITE);

            // fill target from start
            for (int i = target.size(); i < oldSize; ++i)
                target.pushBack(start.getV4(i));

            // resize to larger of oldSize and newSize
            start.resize(Math.max(oldSize, newSize));
            target.resize(Math.max(oldSize, newSize));

            final int delta = (newSize - oldSize);
            if (delta > 0)
            {
                // move past-range elements to new place
                move(start.begin(), last, oldSize, last + delta, Vector4f.SIZE);
                move(target.begin(), last, oldSize, last + delta, Vector4f.SIZE);

                // fill new elements from spheres
                Vector4f tmp = new Vector4f();
                for (int i = last; i < last + delta; ++i) {
//                    start[i] = spheres[i - first];
                    tmp.x = spheres.get((i - first) * 4 + 0);  // TODO maybe have bugs
                    tmp.y = spheres.get((i - first) * 4 + 1);
                    tmp.z = spheres.get((i - first) * 4 + 2);
                    tmp.w = spheres.get((i - first) * 4 + 3);
                    start.set(i, tmp);
                }

                // adjust capsule indices
//                typename T::MappedIndexVectorType indices = getChildCloth()->mCapsuleIndices;
                DxVectorMap indices = new DxVectorMap(mCapsuleIndices, GLenum.GL_READ_WRITE);
//                Vector<IndexPair>::Type::Iterator cIt, cEnd = indices.end();
                ByteBuffer indices_data = indices.begin();
                int location = 0;
                int cEnd = indices_data.limit();
                for (int i = indices_data.position(); i < cEnd; i+= 8)
                {
                    int cIt_first = indices_data.getInt(location);
                    int cIt_second = indices_data.getInt(location + 4);
                    boolean removed = false;
                    removed |= updateIndex(cIt_first, last + Math.min(0, delta), delta);
                    removed |= updateIndex(cIt_second, last + Math.min(0, delta), delta);
                    if (!removed) {
//                        ++cIt;
                        location += 8;
                    }else
                    {
                        indices.replaceWithLast();
                        cIt_first = indices_data.getInt(cEnd - 8);
                        cIt_second = indices_data.getInt(cEnd - 4);

//                        cEnd = indices.end();
                        cEnd -= 8;
                    }
                }

                start.resize(newSize);
                target.resize(newSize);

                getChildCloth().notifyChanged();
                indices.dispose();
            }

            Vector4f tmp = new Vector4f();
            // fill target elements with spheres
            for (int i = spheres.position(); i < spheres.limit(); i+=4){
//                target[first + i] = spheres[i];
                tmp.x = spheres.get(i + 0);
                tmp.y = spheres.get(i + 1);
                tmp.z = spheres.get(i + 2);
                tmp.w = spheres.get(i + 3);
                target.set(first+i, tmp);
            }

            start.dispose();
            target.dispose();
        }

        getChildCloth().wakeUp();
    }

    @Override
    public int getNumSpheres() {
        return (getChildCloth().mStartCollisionSpheres.size());
    }

    @Override
    public void setCapsules(IntBuffer capsules, int first, int last) {
        int oldSize = getChildCloth().mCapsuleIndices.size();
        int newSize = capsules.remaining() / 2 + oldSize - last + first;

        assert (newSize <= 32);
        assert (first <= oldSize);
        assert (last <= oldSize);

//	    const IndexPair* srcIndices = reinterpret_cast<const IndexPair*>(capsules.begin());
        IntBuffer srcIndices = capsules;

        if (getChildCloth().mCapsuleIndices.capacity() < newSize)
        {
//            ContextLockType contextLock(getChildCloth()->mFactory);
            acquire();
            getChildCloth().mCapsuleIndices.reserve(newSize);
            release();
        }

        // resize to larger of oldSize and newSize
        getChildCloth().mCapsuleIndices.resize(Math.max(oldSize, newSize));

//        typename T::MappedIndexVectorType dstIndices = getChildCloth().mCapsuleIndices;
        DxVectorMap dstIndices = new DxVectorMap(getChildCloth().mCapsuleIndices, GLenum.GL_READ_WRITE);
        int delta = newSize - oldSize;
        if (delta > 0)
        {
            // move past-range elements to new place
            move(dstIndices.begin(), last, oldSize, last + delta, 8);

            ByteBuffer dstIndicesData = dstIndices.begin();
            // fill new elements from capsules
            for (int i = last; i < last + delta; ++i) {
//                dstIndices[i] = srcIndices[i - first];
                int firstValue = srcIndices.get((i-first) * 2 + 0);
                int secondValue = srcIndices.get((i-first) * 2 + 1);

                dstIndicesData.putInt(i * 8, firstValue);
                dstIndicesData.putInt(i * 8+4, secondValue);
            }

            dstIndices.resize(newSize);
            getChildCloth().notifyChanged();
        }

        ByteBuffer dstIndicesData = dstIndices.begin();
        // fill existing elements from capsules
        for (int i = first; i < last; ++i) {
//            dstIndices[i] = srcIndices[i - first];
            int firstValue = srcIndices.get((i-first) * 2 + 0);
            int secondValue = srcIndices.get((i-first) * 2 + 1);

            dstIndicesData.putInt(i * 8, firstValue);
            dstIndicesData.putInt(i * 8+4, secondValue);
        }

        getChildCloth().wakeUp();
        dstIndices.dispose();
    }

    @Override
    public int getNumCapsules() {
        return mCapsuleIndices.size();
    }

    @Override
    public void setPlanes(/*Range<const physx::PxVec4>*/FloatBuffer planes, int first, int last) {
        int oldSize = getChildCloth().mStartCollisionPlanes.size();
        int newSize = planes.remaining()/4 + oldSize - last + first;

        assert (newSize <= 32);
        assert (first <= oldSize);
        assert (last <= oldSize);
        if(GLCheck.CHECK){
            int logCount = 0;
            int plans_size = planes.remaining()/4;
            Vector3f tmp = new Vector3f();
            for (int i = planes.position(); i<planes.limit(); i+= 4)
            {
                tmp.x = planes.get(i);
                tmp.y = planes.get(i+1);
                tmp.z = planes.get(i+2);
                if (Math.abs(/*planes[i].getXYZ().magnitudeSquared()*/tmp.lengthSquared() - 1.0f) > 0.01f)
                {
                    if (logCount == 0)
                        LogUtil.e(LogUtil.LogType.DEFAULT, String.format("The plane normals passed to Cloth::setPlanes are not normalized. First error encounterd at plane %d (%f, %f, %f, %f)",
                                i, tmp.x, tmp.y,tmp.z, planes.get(i+3)));
                    logCount++;
                }
            }
            if (logCount>1)
            {
                LogUtil.e(LogUtil.LogType.DEFAULT, String.format("This error was encountered %d more times.", logCount-1));
            }
        }

        if (oldSize ==0 && newSize == 0)
            return;

        if (oldSize == 0)
        {
//            ContextLockType contextLock(getChildCloth()->mFactory);
            acquire();
            getChildCloth().mStartCollisionPlanes.assign(/*planes.begin(), planes.end()*/planes.remaining() * 4, planes);
            getChildCloth().notifyChanged();
            release();
        }
        else
        {
            if (Math.max(oldSize, newSize) >
                    Math.min(getChildCloth().mStartCollisionPlanes.capacity(), getChildCloth().mTargetCollisionPlanes.capacity()))
            {
//                ContextLockType contextLock(getChildCloth()->mFactory);
                acquire();
                getChildCloth().mStartCollisionPlanes.reserve(newSize);
                getChildCloth().mTargetCollisionPlanes.reserve(Math.max(oldSize, newSize));
                release();
            }

//            typename T::MappedVec4fVectorType start = getChildCloth()->mStartCollisionPlanes;
//            typename T::MappedVec4fVectorType target = getChildCloth()->mTargetCollisionPlanes;
            DxVectorMap start = new DxVectorMap(mStartCollisionPlanes, GLenum.GL_READ_WRITE);
            DxVectorMap target = new DxVectorMap(mTargetCollisionPlanes, GLenum.GL_READ_WRITE);

            // fill target from start
            for (int i = target.size(); i < oldSize; ++i) {
                target.pushBack(start.getV4(i));
            }

            // resize to larger of oldSize and newSize
            start.resize(Math.max(oldSize, newSize));
            target.resize(Math.max(oldSize, newSize)/*, physx::PxVec4(0.0f)*/);

            int delta = (newSize - oldSize);
            if (delta > 0)
            {
                // move past-range elements to new place
                move(start.begin(), last, oldSize, last + delta, Vector4f.SIZE);
                move(target.begin(), last, oldSize, last + delta, Vector4f.SIZE);

                Vector4f tmp = new Vector4f();
                // fill new elements from planes
                for (int i = last; i < last + delta; ++i) {
//                    start[i] = planes[i - first];
                    tmp.x = planes.get((i - first) * 4 + 0);
                    tmp.y = planes.get((i - first) * 4 + 1);
                    tmp.z = planes.get((i - first) * 4 + 2);
                    tmp.w = planes.get((i - first) * 4 + 3);
                    start.set(i, tmp);
                }

                // adjust convex indices
                int mask = (1 << (last + Math.min(delta, 0))) - 1;
//                typename T::MappedMaskVectorType masks = getChildCloth()->mConvexMasks;
                DxVectorMap masks = new DxVectorMap(mConvexMasks, GLenum.GL_READ_WRITE);
//                Vector<uint32_t>::Type::Iterator cIt, cEnd = masks.end();
//                for (cIt = masks.begin(); cIt != cEnd;)
                ByteBuffer masks_data = masks.begin();
                int cEnd = masks_data.limit();
                for(int i = masks_data.position(); i < cEnd; /*i+=4*/)
                {
                    int cIt = masks_data.getInt(i);
                    int convex = (cIt & mask);
                    if (delta < 0)
                        convex |= cIt >> -delta & ~mask;
				    else
                        convex |= (cIt & ~mask) << delta;
                    if (convex != 0) {
//					*cIt++ = convex;
                        masks_data.putInt(i, convex);
                        i+=4;
                    }
				    else
                    {
                        masks.replaceWithLast();
                        cIt = masks_data.getInt(cEnd - 4);
//                        cEnd = masks.end();
                        cEnd -= 4;
                    }
                }

                start.resize(newSize);
                target.resize(newSize);

                getChildCloth().notifyChanged();
                masks.dispose();
            }

            final Vector4f tmp = new Vector4f();
            // fill target elements with planes
            for (int i = planes.position(); i < planes.limit(); i+= 4) {
//                target[first + i] = planes[i];

                tmp.x = planes.get(i + 0);
                tmp.y = planes.get(i + 1);
                tmp.z = planes.get(i + 2);
                tmp.w = planes.get(i + 3);
                target.set(first+i, tmp);
            }

            start.dispose();
            target.dispose();
        }

        wakeUp();
    }

    @Override
    public int getNumPlanes() {
        return mStartCollisionPlanes.size();
    }

    @Override
    public void setConvexes(IntBuffer convexMasks, int first, int last) {
        int oldSize = getChildCloth().mConvexMasks.size();
        int newSize = convexMasks.remaining() + oldSize - last + first;

        assert (newSize <= 32);
        assert (first <= oldSize);
        assert (last <= oldSize);
        if(GLCheck.CHECK){
            for (int i = convexMasks.position(); i<convexMasks.limit(); i++)
            {
                if (convexMasks.get(i) == 0)
                {
                    LogUtil.e(LogUtil.LogType.DEFAULT, String.format("Cloth::setConvexes expects bit masks of the form (1<<planeIndex1)|(1<<planeIndex2). 0 is not a valid mask/plane index. Error found in location %d", i));
                    continue;
                }
            }
        }

        if (getChildCloth().mConvexMasks.capacity() < newSize)
        {
//            ContextLockType contextLock(getChildCloth()->mFactory);
            acquire();
            getChildCloth().mConvexMasks.reserve(newSize);
            release();
        }

        // resize to larger of oldSize and newSize
        getChildCloth().mConvexMasks.resize(Math.max(oldSize, newSize));
        int delta = newSize - oldSize;
        if (delta > 0)
        {
//            typename T::MappedMaskVectorType masks = getChildCloth()->mConvexMasks;
            DxVectorMap masks = new DxVectorMap(mConvexMasks, GLenum.GL_READ_WRITE);

            // move past-range elements to new place
            move(masks.begin(), last, oldSize, last + delta, 4);

            // fill new elements from capsules
            for (int i = last; i < last + delta; ++i) {
//                masks[i] = convexMasks[i - first];
                masks.set(i, convexMasks.get(i - first));
            }

            masks.resize(newSize);
            getChildCloth().notifyChanged();
            masks.dispose();
        }

        wakeUp();
    }

    @Override
    public int getNumConvexes() {
        return mConvexMasks.size();
    }

    @Override
    public void setTriangles(/*Range<const physx::PxVec3>*/FloatBuffer triangles, int first, int last) {
        // convert from triangle to vertex count
        first *= 3;
        last *= 3;

        triangles = getChildCloth().clampTriangleCount(triangles, last - first);
        assert (0 == triangles.remaining() % 9);

        int oldSize = (getChildCloth().mStartCollisionTriangles.size());
        int newSize = triangles.remaining()/3 + oldSize - last + first;

        assert (first <= oldSize);
        assert (last <= oldSize);

        if (oldSize==0 && newSize==0)
            return;

        if (oldSize==0)
        {
//            ContextLockType contextLock(getChildCloth()->mFactory);
            acquire();
            getChildCloth().mStartCollisionTriangles.assign(/*triangles.begin(), triangles.end()*/triangles.remaining() * 4, triangles);
            getChildCloth().notifyChanged();
            release();
        }
        else
        {
            if (Math.max(oldSize, newSize) >
                    Math.min(getChildCloth().mStartCollisionTriangles.capacity(), getChildCloth().mTargetCollisionTriangles.capacity()))
            {
//                ContextLockType contextLock(getChildCloth()->mFactory);
                acquire();
                getChildCloth().mStartCollisionTriangles.reserve(newSize);
                getChildCloth().mTargetCollisionTriangles.reserve(Math.max(oldSize, newSize));
                release();
            }

//            typename T::MappedVec3fVectorType start = getChildCloth()->mStartCollisionTriangles;
//            typename T::MappedVec3fVectorType target = getChildCloth()->mTargetCollisionTriangles;
            DxVectorMap start = new DxVectorMap(mStartCollisionTriangles, GLenum.GL_READ_WRITE);
            DxVectorMap target = new DxVectorMap(mTargetCollisionTriangles, GLenum.GL_READ_WRITE);

            // fill target from start
            for (int i = target.size(); i < oldSize; ++i)
                target.pushBack(start.getV3(i));

            // resize to larger of oldSize and newSize
            start.resize(Math.max(oldSize, newSize)/*, physx::PxVec3(0.0f)*/);
            target.resize(Math.max(oldSize, newSize)/*, physx::PxVec3(0.0f)*/);
            final Vector3f tmp = new Vector3f();
            int delta = (newSize - oldSize);
            if (delta > 0)
            {
                // move past-range elements to new place
                move(start.begin(), last, oldSize, last + delta, Vector3f.SIZE);
                move(target.begin(), last, oldSize, last + delta, Vector3f.SIZE);

                // fill new elements from triangles
                for (int i = last; i < last + delta; ++i) {
//                    start[i] = triangles[i - first];
                    tmp.x = triangles.get((i - first) * 3 + 0);
                    tmp.y = triangles.get((i - first) * 3 + 1);
                    tmp.z = triangles.get((i - first) * 3 + 2);
                    start.set(i, tmp);
                }

                start.resize(newSize);
                target.resize(newSize);

                getChildCloth().notifyChanged();
            }
            //////////////////////

            //	if (std::max(oldSize, newSize) >
            //	   std::min(getChildCloth()->mStartCollisionTriangles.capacity(), getChildCloth()->mTargetCollisionTriangles.capacity()))
            //	{
            //		ContextLockType contextLock(getChildCloth()->mFactory);
            //		getChildCloth()->mStartCollisionTriangles.reserve(newSize);
            //		getChildCloth()->mTargetCollisionTriangles.reserve(std::max(oldSize, newSize));
            //	}
            //
            //	// fill target from start
            //	for (uint32_t i = getChildCloth()->mTargetCollisionTriangles.size(); i < oldSize; ++i)
            //		getChildCloth()->mTargetCollisionTriangles.pushBack(getChildCloth()->mStartCollisionTriangles[i]);
            //
            //	// resize to larger of oldSize and newSize
            //	getChildCloth()->mStartCollisionTriangles.resize(std::max(oldSize, newSize));
            //	getChildCloth()->mTargetCollisionTriangles.resize(std::max(oldSize, newSize));
            //
            //	if (uint32_t delta = newSize - oldSize)
            //	{
            //		// move past-range elements to new place
            //		move(getChildCloth()->mStartCollisionTriangles.begin(), last, oldSize, last + delta);
            //		move(getChildCloth()->mTargetCollisionTriangles.begin(), last, oldSize, last + delta);
            //
            //		// fill new elements from triangles
            //		for (uint32_t i = last; i < last + delta; ++i)
            //			getChildCloth()->mStartCollisionTriangles[i] = triangles[i - first];
            //
            //		getChildCloth()->mStartCollisionTriangles.resize(newSize);
            //		getChildCloth()->mTargetCollisionTriangles.resize(newSize);
            //
            //		notifyChanged();
            //	}

            // fill target elements with triangles
            //	for (uint32_t i = 0; i < triangles.size(); ++i)
            //		getChildCloth()->mTargetCollisionTriangles[first + i] = triangles[i];

            // fill target elements with triangles
            for (int i = triangles.position(); i < triangles.limit(); i+=3) {
//                target[first + i] = triangles[i];
                tmp.x = triangles.get(i);
                tmp.y = triangles.get(i+1);
                tmp.z = triangles.get(i+2);

                target.set(first+i, tmp);
            }
            start.dispose();
            target.dispose();
        }

        wakeUp();
    }

    @Override
    public void setTriangles(/*Range<const physx::PxVec3>*/FloatBuffer startTriangles, /*Range<const physx::PxVec3>*/FloatBuffer targetTriangles, int first) {
        assert (startTriangles.remaining() == targetTriangles.remaining());

        // convert from triangle to vertex count
        first *= 3;

        int last = getChildCloth().mStartCollisionTriangles.size();

        startTriangles = getChildCloth().clampTriangleCount(startTriangles, last - first);
        targetTriangles = getChildCloth().clampTriangleCount(targetTriangles, last - first);

        int oldSize = getChildCloth().mStartCollisionTriangles.size();
        int newSize = startTriangles.remaining()/3 + oldSize - last + first;

        assert (first <= oldSize);
        assert (last == oldSize); // this path only supports replacing the tail

        if (oldSize == 0 && newSize == 0)
            return;

        if (newSize > Math.min(getChildCloth().mStartCollisionTriangles.capacity(), getChildCloth().mTargetCollisionTriangles.capacity()))
        {
//            ContextLockType contextLock(getChildCloth()->mFactory);
            acquire();
            getChildCloth().mStartCollisionTriangles.assign(/*startTriangles.begin(), startTriangles.end()*/startTriangles.remaining() * 4, startTriangles);
            getChildCloth().mTargetCollisionTriangles.assign(/*targetTriangles.begin(), targetTriangles.end()*/targetTriangles.remaining() * 4, targetTriangles);
            getChildCloth().notifyChanged();
            release();
        }
	else
        {
            int retainSize = oldSize - last + first;
            getChildCloth().mStartCollisionTriangles.resize(retainSize);
            getChildCloth().mTargetCollisionTriangles.resize(retainSize);

            getChildCloth().mStartCollisionTriangles.assign(/*startTriangles.begin(), startTriangles.end()*/startTriangles.remaining()*4, startTriangles);
            getChildCloth().mTargetCollisionTriangles.assign(/*targetTriangles.begin(), targetTriangles.end()*/targetTriangles.remaining()*4, targetTriangles);

            if (newSize - oldSize != 0)
                getChildCloth().notifyChanged();
        }

        wakeUp();
    }

    @Override
    public int getNumTriangles() {
        return (getChildCloth().mStartCollisionTriangles.size()) / 3;
    }

    @Override
    public boolean isContinuousCollisionEnabled() {
        return mEnableContinuousCollision;
    }

    @Override
    public void enableContinuousCollision(boolean enable) {
        if (enable == mEnableContinuousCollision)
            return;

        getChildCloth().mEnableContinuousCollision = enable;
        getChildCloth().notifyChanged();
        wakeUp();
    }

    @Override
    public float getCollisionMassScale() {
        return mCollisionMassScale;
    }

    @Override
    public void setCollisionMassScale(float scale) {
        if (scale == getChildCloth().mCollisionMassScale)
            return;

        getChildCloth().mCollisionMassScale = scale;
        getChildCloth().notifyChanged();
        wakeUp();
    }

    @Override
    public void setFriction(float friction) {
        getChildCloth().mFriction = friction;
        getChildCloth().notifyChanged();
    }

    @Override
    public float getFriction() {
        return mFriction;
    }

    @CachaRes
    @Override
    public void setVirtualParticles(/*Range<const uint32_t[4]>*/IntBuffer indices, /*Range<const PxVec3>*/FloatBuffer weights) {
        // shuffle indices to form independent SIMD sets
        TripletScheduler scheduler = new TripletScheduler(indices);
        scheduler.warp(mNumParticles, 32);

        // convert to 16bit indices
//        Vector<Vec4us>::Type hostIndices;
//        hostIndices.reserve(indices.size());
//        TripletScheduler::ConstTripletIter tIt = scheduler.mTriplets.begin();
//        TripletScheduler::ConstTripletIter tEnd = scheduler.mTriplets.end();
//        for (; tIt != tEnd; ++tIt)
//            hostIndices.pushBack(Vec4us(*tIt));
        ShortBuffer hostIndices = BufferUtils.createShortBuffer(scheduler.mTriplets.remaining());
        for(int i = scheduler.mTriplets.position(); i < scheduler.mTriplets.limit(); i++){
            hostIndices.put((short)scheduler.mTriplets.get(i));
        }
        hostIndices.flip();
        // printf("num sets = %u, num replays = %u\n", scheduler.mSetSizes.size(),
        //	calculateNumReplays(scheduler.mTriplets, scheduler.mSetSizes));

        // add normalization weight
        /*Vector<PxVec4>::Type hostWeights;
        hostWeights.reserve(weights.size());
        for (; !weights.empty(); weights.popFront())
        {
            PxVec3 w = reinterpret_cast<const PxVec3&>(weights.front());
            float scale = 1.f / w.magnitudeSquared();
            hostWeights.pushBack(PxVec4(w.x, w.y, w.z, scale));
        }*/
        FloatBuffer hostWeights = BufferUtils.createFloatBuffer(weights.remaining()/3 * 4);
        for(int i = weights.position(); i < weights.limit(); i+=3){
            float x = weights.get(i);
            float y = weights.get(i+1);
            float z = weights.get(i+2);

            float scale = 1.0f/Vector3f.lengthSquare(x,y,z);
            hostWeights.put(x);
            hostWeights.put(y);
            hostWeights.put(z);
            hostWeights.put(scale);
        }
        hostWeights.flip();

//        DxContextLock contextLock(mFactory);
        acquire();
        // todo: 'swap' these to force reallocation?
        mVirtualParticleIndices.assign(hostIndices);
        mVirtualParticleSetSizes.assign(CacheBuffer.wrap(scheduler.mSetSizes.getData(), 0, scheduler.mSetSizes.size()));  // TODO
        mVirtualParticleWeights.assign(hostWeights);

        notifyChanged();
        wakeUp();
        release();
    }

    @Override
    public int getNumVirtualParticles() {
        return mVirtualParticleIndices.size();
    }

    @Override
    public int getNumVirtualParticleWeights() {
        return mVirtualParticleWeights.size();
    }

    @Override
    public void setTetherConstraintScale(float scale) {
        if (scale == mTetherConstraintScale)
            return;

        mTetherConstraintScale = scale;
        notifyChanged();
        wakeUp();
    }

    @Override
    public float getTetherConstraintScale() {
        return mTetherConstraintScale;
    }

    @Override
    public void setTetherConstraintStiffness(float stiffness) {
        float value = safeLog2(1 - stiffness);
        if (value == mTetherConstraintLogStiffness)
            return;

        mTetherConstraintLogStiffness = value;
        notifyChanged();
        wakeUp();
    }

    @Override
    public float getTetherConstraintStiffness() {
        return 1.f - safeExp2(mTetherConstraintLogStiffness);
    }

    @Override
    public FloatBuffer getMotionConstraints() {
        wakeUp();
        return push(mMotionConstraints);
    }

    @Override
    public void clearMotionConstraints() {
        clear(mMotionConstraints);
        wakeUp();
    }

    @Override
    public int getNumMotionConstraints() {
        return mMotionConstraints.mStart.size();
    }

    @Override
    public void setMotionConstraintScaleBias(float scale, float bias) {
        if (scale == getChildCloth().mMotionConstraintScale && bias == getChildCloth().mMotionConstraintBias)
            return;

        mMotionConstraintScale = scale;
        mMotionConstraintBias = bias;
        notifyChanged();
        wakeUp();
    }

    @Override
    public float getMotionConstraintScale() {
        return mMotionConstraintScale;
    }

    @Override
    public float getMotionConstraintBias() {
        return mMotionConstraintBias;
    }

    @Override
    public void setMotionConstraintStiffness(float stiffness) {
        float value = safeLog2(1 - stiffness);
        if (value == mMotionConstraintLogStiffness)
            return;

        mMotionConstraintLogStiffness = value;
        notifyChanged();
        wakeUp();
    }

    @Override
    public float getMotionConstraintStiffness() {
        return 1.f - safeExp2(getChildCloth().mMotionConstraintLogStiffness);
    }

    @Override
    public FloatBuffer getSeparationConstraints() {
        wakeUp();
        return push(getChildCloth().mSeparationConstraints);
    }

    @Override
    public void clearSeparationConstraints() {
        clear(getChildCloth().mSeparationConstraints);
        wakeUp();
    }

    @Override
    public int getNumSeparationConstraints() {
        return mSeparationConstraints.mStart.size();
    }

    @Override
    public void clearInterpolation() {
        if (!mTargetCollisionSpheres.empty())
        {
//            ContextLockType contextLock(getChildCloth()->mFactory);
            acquire();
//            physx::shdfnd::swap(getChildCloth().mStartCollisionSpheres, getChildCloth().mTargetCollisionSpheres);
            DxBatchedVector temp = mStartCollisionSpheres;
            mStartCollisionSpheres.load(mTargetCollisionSpheres);
//            mTargetCollisionSpheres.load(temp);  TODO
            mTargetCollisionSpheres.resize(0);
        }
        mMotionConstraints.pop();
        mSeparationConstraints.pop();
        wakeUp();
    }

    @Override
    public FloatBuffer getParticleAccelerations() {
        if (mParticleAccelerations.empty())
        {
//            DxContextLock contextLock(mFactory);
            acquire();
            mParticleAccelerations.resize(mNumParticles);
            release();
        }

        if (mParticleAccelerationsHostCopy == null)
        {
//            DxContextLock contextLock(mFactory);
//            mParticleAccelerationsHostCopy.reserve(mNumParticles);
            mParticleAccelerationsHostCopy = BufferUtils.createFloatBuffer(mNumParticles * 4);
        }
//        mParticleAccelerationsHostCopy.resizeUninitialized(mNumParticles);
        mParticleAccelerationsHostCopy.limit(mNumParticles * 4);

        wakeUp();

//        PxVec4* data = mParticleAccelerationsHostCopy.begin();
//        return Range<PxVec4>(data, mParticleAccelerationsHostCopy.end());
        return mParticleAccelerationsHostCopy;
    }

    @Override
    public void clearParticleAccelerations() {
//        DxContextLock contextLock(mFactory);
        acquire();
        mParticleAccelerations.clear();
//        Vector<PxVec4>::Type().swap(mParticleAccelerationsHostCopy);
        mParticleAccelerationsHostCopy = null;
        wakeUp();
        release();
    }

    @Override
    public int getNumParticleAccelerations() {
        return mParticleAccelerations.size();
    }

    @Override
    public void setSelfCollisionDistance(float distance) {
        if (distance == getChildCloth().mSelfCollisionDistance)
            return;

        mSelfCollisionDistance = distance;
        notifyChanged();
        wakeUp();
    }

    @Override
    public float getSelfCollisionDistance() {
        return mSelfCollisionDistance;
    }

    @Override
    public void setSelfCollisionStiffness(float stiffness) {
        assert (stiffness <= 1.0f);
        float value = safeLog2(1 - stiffness);
        if (value == mSelfCollisionLogStiffness)
            return;

        mSelfCollisionLogStiffness = value;
        notifyChanged();
        wakeUp();
    }

    @Override
    public float getSelfCollisionStiffness() {
        return 1.f - safeExp2(mSelfCollisionLogStiffness);
    }

    @Override
    public void setSelfCollisionIndices(IntBuffer indices) {
//        ContextLockType lock(mFactory);
        acquire();
        mSelfCollisionIndices.assign(indices.remaining() * 4, indices);
//        mSelfCollisionIndicesHost.assign(indices.begin(), indices.end());
        if(mSelfCollisionIndicesHost == null || mSelfCollisionIndicesHost.capacity() < indices.remaining()){
            mSelfCollisionIndicesHost = BufferUtils.createIntBuffer(indices.remaining());
            int old_position = indices.position();
            mSelfCollisionIndicesHost.put(indices).flip();
            indices.position(old_position);
        }

        notifyChanged();
        wakeUp();
        release();
    }

    @Override
    public int getNumSelfCollisionIndices() {
        return mSelfCollisionIndices.size();
    }

    @Override
    public void setRestPositions(/*Range<const physx::PxVec4>*/FloatBuffer restPositions) {
        assert (restPositions.remaining() == 0 || restPositions.remaining()/4 == getNumParticles());
//        ContextLockType contextLock(getChildCloth()->mFactory);
        acquire();
        mRestPositions.assign(/*restPositions.begin(), restPositions.end()*/restPositions.remaining() * 4, restPositions);
        release();
        wakeUp();
    }

    @Override
    public int getNumRestPositions() {
        return mRestPositions.size();
    }

    @Override
    public ReadableVector3f getBoundingBoxCenter() {
        return mParticleBoundsCenter;
    }

    @Override
    public ReadableVector3f getBoundingBoxScale() {
        return mParticleBoundsHalfExtent;
    }

    @Override
    public void setUserData(Object userData) {
        mUserData = userData;
    }

    @Override
    public Object getUserData() {
        return mUserData;
    }

    @Override
    protected void makeClothCostDirty() {
        mClothCostDirty = true;
    }

    @Override
    protected void notifyChanged() {
        mClothDataDirty = true;
    }

    private final boolean isSelfCollisionEnabled(DxCloth unused)
    {
        return Math.min(mSelfCollisionDistance, -mSelfCollisionLogStiffness) > 0.0f;
    }

    boolean updateClothData(DxClothData clothData)
    {
        if (!mClothDataDirty)
        {
            assert (mSharedMemorySize == getSharedMemorySize());
            return false;
        }

        mSharedMemorySize = getSharedMemorySize();

        if (mSelfCollisionData.empty() && isSelfCollisionEnabled(this))
        {
            int numSelfCollisionIndices =
                    mSelfCollisionIndices.empty() ? mNumParticles : mSelfCollisionIndices.size();

            int keySize = 2 * numSelfCollisionIndices;           // 2x for radix buffer
            int cellStartSize = (129 + 128 * 128 + 130);

            mSelfCollisionParticles.resize(mNumParticles);
            mSelfCollisionData.resize(keySize + cellStartSize);
            // checkSuccess( cuMemsetD32((mSelfCollisionData.begin()
            //	+ particleSize + keySize).dev(), 0xffffffff, cellStartSize) );

            mInitSelfCollisionData = true;
        }

        clothData.load(this);
        mClothDataDirty = false;

        return true;
    }

    int getSharedMemorySize()
    {
        int numPhases = (mPhaseConfigs.size());
        int numSpheres = (mStartCollisionSpheres.size());
        int numCones = (mCapsuleIndices.size());
        int numPlanes = (mStartCollisionPlanes.size());
        int numConvexes = (mConvexMasks.size());
        int numTriangles = (mStartCollisionTriangles.size() / 3);

        int phaseConfigSize = numPhases * DxPhaseConfig.SIZE;

        boolean storePrevCollisionData = mEnableContinuousCollision || mFriction > 0.0f;
        int continuousCollisionSize = storePrevCollisionData ? 4 * numSpheres + 10 * numCones : 0;
        continuousCollisionSize += 4 * numCones + numConvexes; // capsule and convex masks
        int discreteCollisionSize = 4 * numSpheres + Math.max(10 * numCones + 96, 208);
        discreteCollisionSize = Math.max(discreteCollisionSize, Math.max(4 * numPlanes, 19 * numTriangles));

        // scratch memory for prefix sum and histogram
        int selfCollisionSize = isSelfCollisionEnabled(this) ? 544 : 0;

        // see CuSolverKenel.cu::gSharedMemory comment for details
        return phaseConfigSize + /*sizeof(float)*/4 * (continuousCollisionSize + Math.max(selfCollisionSize, discreteCollisionSize));
    }

    void setPhaseConfigInternal(/*Range<const PhaseConfig>*/List<PhaseConfig> configs)
    {
//        mHostPhaseConfigs.assign(configs.begin(), configs.end());
        mHostPhaseConfigs.clear();
        mHostPhaseConfigs.addAll(configs);

//        Vector<DxPhaseConfig>::Type deviceConfigs;
//        deviceConfigs.reserve(configs.size());
//	    const PhaseConfig* cEnd = configs.end();
//        for (const PhaseConfig* cIt = configs.begin(); cIt != cEnd; ++cIt)
        ByteBuffer deviceConfigs = CacheBuffer.getCachedByteBuffer(configs.size() * DxPhaseConfig.SIZE);
        DxPhaseConfig config = new DxPhaseConfig();
        for(PhaseConfig cIt : configs)
        {
            config.mStiffness = cIt.mStiffness;
            config.mStiffnessMultiplier = cIt.mStiffnessMultiplier;
            config.mCompressionLimit = cIt.mCompressionLimit;
            config.mStretchLimit = cIt.mStretchLimit;

            int phaseIndex = cIt.mPhaseIndex;
            config.mFirstConstraint = mFabric.mFirstConstraintInPhase.get(phaseIndex);
            config.mNumConstraints = mFabric.mNumConstraintsInPhase.get(phaseIndex);

//            deviceConfigs.add(config);
            config.store(deviceConfigs);
        }
        deviceConfigs.flip();

//        DxContextLock contextLock(mFactory);

        mPhaseConfigs.assign(deviceConfigs.remaining(), deviceConfigs);
    }

    FloatBuffer push(DxConstraints constraints)
    {
        if (constraints.mTarget.capacity() == 0)
        {
//            DxContextLock contextLock(mFactory);
            acquire();
            constraints.mTarget.reserve(mNumParticles);
            release();
        }
        if (constraints.mHostCopy == null)
            constraints.mTarget.resize(mNumParticles);

        if (constraints.mStart.empty()) // initialize start first
        {
//            DxContextLock contextLock(mFactory);
            acquire();
            constraints.mStart.swap(constraints.mTarget);
            release();
        }

        if (/*!constraints.mHostCopy.capacity()*/constraints.mHostCopy == null)
        {
//            DxContextLock contextLock(mFactory);
//            constraints.mHostCopy.reserve(mNumParticles);
            constraints.mHostCopy = BufferUtils.createFloatBuffer(mNumParticles*4);
            constraints.mHostCopy.limit(0);  // no data
        }
//        constraints.mHostCopy.resizeUninitialized(mNumParticles);
        constraints.mHostCopy.limit(mNumParticles * 4);

//        PxVec4* data = &constraints.mHostCopy.front();
//        return Range<PxVec4>(data, data + constraints.mHostCopy.size());
        return constraints.mHostCopy;
    }

    void clear(DxConstraints constraints)
    {
//        DxContextLock contextLock(mFactory);
        acquire();
        constraints.mStart.clear();
        constraints.mTarget.clear();
        release();
    }

    void mapParticles()
    {
        if (mHostParticlesDirty)
        {
//            DxContextLock contextLock(mFactory);
            mParticlesHostCopy.load(mParticles);
            mHostParticlesDirty = false;
        }
        if (0 == mParticlesMapRefCount++)
        {
//            DxContextLock contextLock(mFactory);
            acquire();
            mParticlesMapPointer = mParticlesHostCopy.map();
            release();
        }
    }

    void unmapParticles()
    {
        if (0 == --mParticlesMapRefCount)
        {
//            DxContextLock contextLock(mFactory);
            acquire();
            mParticlesHostCopy.unmap();
            mParticlesMapPointer = null;
            release();
        }
    }

    FloatBuffer clampTriangleCount(/*Range<const PxVec3>*/FloatBuffer range, int replaceSize)
    {
        // clamp to 500 triangles (1500 vertices) to prevent running out of shared memory
        int removedSize = mStartCollisionTriangles.size() - replaceSize;
//	    const PxVec3* clamp = range.begin() + 1500 - removedSize;
        final int clamp = range.position()/3 + 1500 - removedSize;
        final int end = range.limit()/3;

        if (end > clamp)
        {
            LogUtil.e(LogUtil.LogType.DEFAULT, "Too many collision triangles specified for cloth, dropping all but first 500.");
        }

//        return Range<const PxVec3>(range.begin(), std::min(range.end(), clamp));
        range.limit(Math.min(end, clamp) * 3);
        FloatBuffer result = range.slice();
        range.limit(end*3);
        return result;
    }

    private final DxCloth getChildCloth() { return this;}

    private static void initialize(DxCloth cloth, FloatBuffer paritcles)
    {
        // initialize particles bounding box
        /*physx::PxVec4 lower = physx::PxVec4(FLT_MAX), upper = -lower;
        for (; pIt != pEnd; ++pIt)
        {
            lower = lower.minimum(*pIt);
            upper = upper.maximum(*pIt);
        }
        physx::PxVec4 center = (upper + lower) * 0.5f;
        physx::PxVec4 extent = (upper - lower) * 0.5f;
        cloth.mParticleBoundsCenter = reinterpret_cast<const physx::PxVec3&>(center);
        cloth.mParticleBoundsHalfExtent = reinterpret_cast<const physx::PxVec3&>(extent);*/
        final Vector4f lower = new Vector4f(Float.MAX_VALUE,Float.MAX_VALUE,Float.MAX_VALUE,Float.MAX_VALUE);
        final Vector4f upper = Vector4f.scale(lower, -1, null);
        final Vector4f value = new Vector4f();
        int count = paritcles.remaining()/4;
        int position = paritcles.position();
        for(int i = 0; i < count; i++){
            value.load(paritcles);
            Vector4f.max(upper, value, upper);
            Vector4f.min(lower, value, lower);
        }
        Vector3f.mix(upper, lower, 0.5f, cloth.mParticleBoundsCenter);
        Vector3f.sub(upper, cloth.mParticleBoundsCenter, cloth.mParticleBoundsHalfExtent);

        cloth.mGravity.set(0,0,0);
        cloth.mLogDamping.set(0,0,0);
        cloth.mLinearLogDrag.set(0,0,0);
        cloth.mAngularLogDrag.set(0,0,0);
        cloth.mLinearInertia.set(1,1,1);
        cloth.mAngularInertia.set(1,1,1);
        cloth.mCentrifugalInertia.set(1,1,1);
        cloth.mSolverFrequency = 300.0f;
        cloth.mStiffnessFrequency = 10.0f;
        cloth.mTargetMotion.setIdentity();
        cloth.mCurrentMotion.setIdentity();
        cloth.mLinearVelocity.set(0,0,0);
        cloth.mAngularVelocity.set(0,0,0);
        cloth.mPrevIterDt = 0.0f;
        cloth.mIterDtAvg = new MovingAverage(30);
        cloth.mTetherConstraintLogStiffness = (-FLT_MAX_EXP);
        cloth.mTetherConstraintScale = 1.0f;
        cloth.mMotionConstraintScale = 1.0f;
        cloth.mMotionConstraintBias = 0.0f;
        cloth.mMotionConstraintLogStiffness = (-FLT_MAX_EXP);
        cloth.mWind.set(0,0,0);
        cloth.mDragLogCoefficient = 0.0f;
        cloth.mLiftLogCoefficient = 0.0f;
        cloth.mFluidDensity = 1.0f;
        cloth.mEnableContinuousCollision = false;
        cloth.mCollisionMassScale = 0.0f;
        cloth.mFriction = 0.0f;
        cloth.mSelfCollisionDistance = 0.0f;
        cloth.mSelfCollisionLogStiffness = (-FLT_MAX_EXP);
        cloth.mSleepTestInterval = (-1);
        cloth.mSleepAfterCount = (-1);
        cloth.mSleepThreshold = 0.0f;
        cloth.mSleepPassCounter = 0;
        cloth.mSleepTestCounter = 0;
    }

    static void copy(DxCloth dstCloth, DxCloth srcCloth)
    {
        dstCloth.mParticleBoundsCenter.set(srcCloth.mParticleBoundsCenter);
        dstCloth.mParticleBoundsHalfExtent.set(srcCloth.mParticleBoundsHalfExtent);
        dstCloth.mGravity.set(srcCloth.mGravity);
        dstCloth.mLogDamping.set(srcCloth.mLogDamping);
        dstCloth.mLinearLogDrag.set(srcCloth.mLinearLogDrag);
        dstCloth.mAngularLogDrag.set(srcCloth.mAngularLogDrag);
        dstCloth.mLinearInertia.set(srcCloth.mLinearInertia);
        dstCloth.mAngularInertia.set(srcCloth.mAngularInertia);
        dstCloth.mCentrifugalInertia.set(srcCloth.mCentrifugalInertia);
        dstCloth.mSolverFrequency = srcCloth.mSolverFrequency;
        dstCloth.mStiffnessFrequency = srcCloth.mStiffnessFrequency;
        dstCloth.mTargetMotion.set(srcCloth.mTargetMotion);
        dstCloth.mCurrentMotion.set(srcCloth.mCurrentMotion);
        dstCloth.mLinearVelocity.set(srcCloth.mLinearVelocity);
        dstCloth.mAngularVelocity.set(srcCloth.mAngularVelocity);
        dstCloth.mPrevIterDt = srcCloth.mPrevIterDt;
        dstCloth.mIterDtAvg = srcCloth.mIterDtAvg;
        dstCloth.mTetherConstraintLogStiffness = srcCloth.mTetherConstraintLogStiffness;
        dstCloth.mTetherConstraintScale = srcCloth.mTetherConstraintScale;
        dstCloth.mMotionConstraintScale = srcCloth.mMotionConstraintScale;
        dstCloth.mMotionConstraintBias = srcCloth.mMotionConstraintBias;
        dstCloth.mMotionConstraintLogStiffness = srcCloth.mMotionConstraintLogStiffness;
        dstCloth.mWind.set(srcCloth.mWind);
        dstCloth.mDragLogCoefficient = srcCloth.mDragLogCoefficient;
        dstCloth.mLiftLogCoefficient = srcCloth.mLiftLogCoefficient;
        dstCloth.mEnableContinuousCollision = srcCloth.mEnableContinuousCollision;
        dstCloth.mCollisionMassScale = srcCloth.mCollisionMassScale;
        dstCloth.mFriction = srcCloth.mFriction;
        dstCloth.mSelfCollisionDistance = srcCloth.mSelfCollisionDistance;
        dstCloth.mSelfCollisionLogStiffness = srcCloth.mSelfCollisionLogStiffness;
        dstCloth.mSleepTestInterval = srcCloth.mSleepTestInterval;
        dstCloth.mSleepAfterCount = srcCloth.mSleepAfterCount;
        dstCloth.mSleepThreshold = srcCloth.mSleepThreshold;
        dstCloth.mSleepPassCounter = srcCloth.mSleepPassCounter;
        dstCloth.mSleepTestCounter = srcCloth.mSleepTestCounter;
        dstCloth.mUserData = srcCloth.mUserData;
    }

    @Override
    public void dispose() {

    }
}
