package nv.visualFX.cloth.libs.dx;

import org.lwjgl.util.vector.Vector4f;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import jet.opengl.postprocessing.common.Disposeable;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.util.CacheBuffer;
import jet.opengl.postprocessing.util.LogUtil;
import nv.visualFX.cloth.libs.Cloth;
import nv.visualFX.cloth.libs.InterCollisionFilter;
import nv.visualFX.cloth.libs.IterationState;
import nv.visualFX.cloth.libs.Solver;
import nv.visualFX.cloth.libs.SwInterCollision;
import nv.visualFX.cloth.libs.SwInterCollisionData;

/**
 * Created by mazhen'gui on 2017/9/13.
 */

final class DxSolver extends DxContextLock implements Solver, Disposeable{
    private DxFactory mFactory;

//    typedef Vector<DxCloth*>::Type ClothVector;
    private final List<DxCloth> mCloths = new ArrayList<>();

    private final DxDeviceVector/*<DxClothData>*/ mClothData;
    private final List<DxClothData> mClothDataHostCopy = new ArrayList<>();
    private boolean mClothDataDirty;

    private final DxDeviceVector/*<DxFrameData>*/ mFrameData;
    private final DxDeviceVector/*<DxFrameData>*/ mFrameDataHostCopy;

    private final DxDeviceVector/*<DxIterationData>*/ mIterationData;
    private final List<DxIterationData> mIterationDataHostCopy = new ArrayList<>();

    private float mFrameDt;

    private int mSharedMemorySize;
    private int mSharedMemoryLimit;

    private int mKernelSharedMemorySize;
    private long mSyncQuery;

    private float mInterCollisionDistance;
    private float mInterCollisionStiffness = 1.f;
    private int mInterCollisionIterations;
    private InterCollisionFilter mInterCollisionFilter;
    private Object mInterCollisionScratchMem;
    private int mInterCollisionScratchMemSize;
    private final List<SwInterCollisionData> mInterCollisionInstances = new ArrayList<>();

    private boolean mComputeError;

//    friend void record(const DxSolver&);

    private Object mSimulateProfileEventData;
    private GLFuncProvider gl;


    public DxSolver(DxFactory factory) {
        super(factory);

        gl = GLFuncProviderFactory.getGLFuncProvider();
        mFactory = (factory);
        mFrameDt = (0.0f);
        mSharedMemorySize = (0);
        mSharedMemoryLimit = (0);

        final int target = GLenum.GL_SHADER_STORAGE_BUFFER;
        final int usage = GLenum.GL_STREAM_DRAW;
        mClothData = new DxDeviceVector(mFactory.mContextManager, target, usage, DxClothData.SIZE);
        mFrameData = new DxDeviceVector(mFactory.mContextManager, target, usage, DxFrameData.SIZE);
        mFrameDataHostCopy = new DxDeviceVector(mFactory.mContextManager, target, usage, DxFrameData.SIZE);
        mIterationData = new DxDeviceVector(mFactory.mContextManager, target, usage, DxIterationData.SIZE);

        /*ID3D11Device* device = mFactory.mContextManager->getDevice();
        if (device->GetFeatureLevel() < D3D_FEATURE_LEVEL_11_0)
        {
            D3D11_FEATURE_DATA_D3D10_X_HARDWARE_OPTIONS hwopts = { 0 };
            device->CheckFeatureSupport(D3D11_FEATURE_D3D10_X_HARDWARE_OPTIONS, &hwopts, sizeof(hwopts));
            if (!hwopts.ComputeShaders_Plus_RawAndStructuredBuffers_Via_Shader_4_x)
            {
                NV_CLOTH_LOG_WARNING("DirectCompute is not supported by this device\n");
                mComputeError = true;
            }
        }*/

        mSharedMemoryLimit = 32 * 1024 - mKernelSharedMemorySize;

//        D3D11_QUERY_DESC queryDesc = { D3D11_QUERY_EVENT, 0 };
//        device->CreateQuery(&queryDesc, &mSyncQuery);
//        mSyncQuery = gl.glFenceSync();

        super.release();

        mSimulateProfileEventData = null;
    }

    @Override
    public void addCloth(Cloth cloth) {
        DxCloth dxCloth = (DxCloth) cloth;

//        NV_CLOTH_ASSERT(mCloths.find(&dxCloth) == mCloths.end());
        if(mCloths.contains(dxCloth)){
            throw new IllegalArgumentException();
        }

        mCloths.add(dxCloth);
        // trigger update of mClothData array
        dxCloth.notifyChanged();

        // sort cloth instances by size
//        shdfnd::sort(mCloths.begin(), mCloths.size(), ClothSimCostGreater(), NonTrackingAllocator());
        mCloths.sort((left, right)-> Float.compare(left.mNumParticles * left.mSolverFrequency, right.mNumParticles * right.mSolverFrequency));

        DxContextLock contextLock = new DxContextLock(mFactory);

        // resize containers and update kernel data
        mClothDataHostCopy.add(new DxClothData());

        mClothData.resize(mCloths.size());
        mFrameDataHostCopy.resize(mCloths.size());

        // lazy compilation of compute shader
        mComputeError |= mFactory.mSolverKernelComputeShader == null;
        contextLock.release();
    }

    @Override
    public void removeCloth(Cloth cloth) {
        /*ClothVector::Iterator begin = mCloths.begin(), end = mCloths.end();
        ClothVector::Iterator it = mCloths.find(&dxCloth);

        if (it == end)
            return; // not found

        uint32_t index = uint32_t(it - begin);*/
        int index = mCloths.indexOf(cloth);
        if(index < 0)
            return;  // not found

        mCloths.remove(index);
        mClothDataHostCopy.remove(index);
        mClothData.resize(mCloths.size());
        mClothDataDirty = true;
    }

    @Override
    public int getNumCloths() {
        return mCloths.size();
    }

    @Override
    public List<DxCloth> getClothList() {
        return mCloths.size() > 0 ? mCloths : null;
    }

    @Override
    public boolean beginSimulation(float dt) {
        if (mCloths.isEmpty())
            return false;
        mFrameDt = dt;
        beginFrame();
        return true;
    }

    @Override
    public void simulateChunk(int idx) {
//        PX_UNUSED(idx);
        assert (!mCloths.isEmpty());
        assert (idx == 0);
        executeKernel();
    }

    @Override
    public void endSimulation() {
        assert (!mCloths.isEmpty());
        endFrame();
    }

    @Override
    public int getSimulationChunkCount() {
        return 1;
    }

    @Override
    public void setInterCollisionDistance(float distance) {
        mInterCollisionDistance = distance;
    }

    @Override
    public float getInterCollisionDistance() {
        return mInterCollisionDistance;
    }

    @Override
    public void setInterCollisionStiffness(float stiffness) {
        mInterCollisionStiffness = stiffness;
    }

    @Override
    public float getInterCollisionStiffness() {
        return mInterCollisionStiffness;
    }

    @Override
    public void setInterCollisionNbIterations(int nbIterations) {
        mInterCollisionIterations = nbIterations;
    }

    @Override
    public int getInterCollisionNbIterations() {
        return mInterCollisionIterations;
    }

    @Override
    public void setInterCollisionFilter(InterCollisionFilter filter) {
        mInterCollisionFilter = filter;
    }

    @Override
    public boolean hasError() {
        return mComputeError;
    }

    // simulate helper functions
    private void beginFrame(){
        DxContextLock contextLock = new DxContextLock(mFactory);

//        mSimulateProfileEventData = NV_CLOTH_PROFILE_START_CROSSTHREAD("cloth::DxSolver::simulate", 0);
        /*
        ID3DUserDefinedAnnotation* annotation;
        mFactory.mContextManager->getContext()->QueryInterface(&annotation);
        annotation->BeginEvent(L"cloth::DxSolver::simulate");
        annotation->Release();
        */

        mIterationDataHostCopy.clear();

        // update cloth data
//        ClothVector::Iterator cIt, cEnd = mCloths.end();
//        Vector<DxClothData>::Type::Iterator dIt = mClothDataHostCopy.begin();
//        for (cIt = mCloths.begin(); cIt != cEnd; ++cIt, ++dIt)
//            mClothDataDirty |= (*cIt)->updateClothData(*dIt);
        for(int i = 0; i < mCloths.size(); i++){
            mClothDataDirty |= mCloths.get(i).updateClothData(mClothDataHostCopy.get(i));
        }

        int maxSharedMemorySize = 0;
        /*DxFrameData**/ByteBuffer frameDataIt = mFrameDataHostCopy.map(GLenum.GL_WRITE_ONLY);
//        for (cIt = mCloths.begin(); cIt != cEnd; ++cIt)
        for(int i = 0; i < mCloths.size(); i++)
        {
            DxCloth cloth = mCloths.get(i);

            int sharedMemorySize = cloth.mSharedMemorySize;
            int positionsSize = cloth.mNumParticles * /*sizeof(PxVec4)*/Vector4f.SIZE;

            int numSharedPositions = Math.min(2, (mSharedMemoryLimit - sharedMemorySize) / positionsSize);

            maxSharedMemorySize = Math.max(maxSharedMemorySize, sharedMemorySize + numSharedPositions * positionsSize);

            IterationStateFactory factory = new IterationStateFactory(cloth, mFrameDt);
            IterationState state = factory.create/*<Simd4f>*/(cloth);

//		*(frameDataIt++) = DxFrameData(cloth, numSharedPositions, state, mIterationDataHostCopy.size());
            DxFrameData frameData = new DxFrameData(cloth, numSharedPositions, state, mIterationDataHostCopy.size());
            frameData.store(frameDataIt);

            while (state.mRemainingIterations > 0)
            {
                mIterationDataHostCopy.add(new DxIterationData(state));
                state.update();
            }

            if (cloth.mDeviceParticlesDirty)
                cloth.mParticles = new DxBatchedVector(cloth.mParticlesHostCopy);

            // copy to device
            cloth.mParticleAccelerations.assign(cloth.mParticleAccelerationsHostCopy.remaining() * 4, cloth.mParticleAccelerationsHostCopy);
            if (cloth.mMotionConstraints.mHostCopy != null)
            {
//                (cloth.mMotionConstraints.mTarget.empty() ? cloth.mMotionConstraints.mStart : cloth.mMotionConstraints.mTarget) = cloth.mMotionConstraints.mHostCopy;
                if(cloth.mMotionConstraints.mTarget == null){
                    cloth.mMotionConstraints.mStart.assign(cloth.mMotionConstraints.mHostCopy.remaining() * 4, cloth.mMotionConstraints.mHostCopy);
                }else{
                    cloth.mMotionConstraints.mTarget.assign(cloth.mMotionConstraints.mHostCopy.remaining() * 4, cloth.mMotionConstraints.mHostCopy);
                }
            }
            if (cloth.mSeparationConstraints.mHostCopy != null)
            {
//                (cloth.mSeparationConstraints.mTarget.empty() ? cloth.mSeparationConstraints.mStart : cloth.mSeparationConstraints.mTarget) = cloth.mSeparationConstraints.mHostCopy;
                if(cloth.mSeparationConstraints.mTarget == null){
                    cloth.mSeparationConstraints.mStart.assign(cloth.mSeparationConstraints.mHostCopy.remaining()*4, cloth.mSeparationConstraints.mHostCopy);
                }else{
                    cloth.mSeparationConstraints.mTarget.assign(cloth.mSeparationConstraints.mHostCopy.remaining()*4, cloth.mSeparationConstraints.mHostCopy);
                }
            }
        }
        mFrameDataHostCopy.unmap();
        mSharedMemorySize = maxSharedMemorySize;

        mFrameData.set(mFrameDataHostCopy);
//        mIterationData = mIterationDataHostCopy;
        ByteBuffer iterationDataBuffer = CacheBuffer.getCachedByteBuffer(mIterationDataHostCopy.size() * DxIterationData.SIZE);
        for(DxIterationData iterationData : mIterationDataHostCopy){
            iterationData.store(iterationDataBuffer);
        }
        iterationDataBuffer.flip();
        mIterationData.assign(iterationDataBuffer);

        mFactory.mCapsuleIndicesDeviceCopy = mFactory.mCapsuleIndices.mBuffer;
        mFactory.mCollisionSpheresDeviceCopy = mFactory.mCollisionSpheres.mBuffer;
        mFactory.mConvexMasksDeviceCopy = mFactory.mConvexMasks.mBuffer;
        mFactory.mCollisionPlanesDeviceCopy = mFactory.mCollisionPlanes.mBuffer;
        mFactory.mCollisionTrianglesDeviceCopy = mFactory.mCollisionTriangles.mBuffer;
//	mFactory.mParticleAccelerations = mFactory.mParticleAccelerationsHostCopy;
        mFactory.mRestPositionsDeviceCopy = mFactory.mRestPositions.mBuffer;

        contextLock.release();
    }
    private void executeKernel(){
//        DxContextLock contextLock(mFactory);

        if (mClothDataDirty)
        {
            assert (mClothDataHostCopy.size() == mClothData.size());
            ByteBuffer clothData = CacheBuffer.getCachedByteBuffer(mClothDataHostCopy.size() * DxClothData.SIZE);
            for(int i = 0; i < mClothDataHostCopy.size(); i++){
                mClothDataHostCopy.get(i).store(clothData);
            }
            clothData.flip();
//            mClothData = mClothDataHostCopy;
            mClothData.assign(clothData);
            mClothDataDirty = false;
        }

//        ID3D11DeviceContext* context = mFactory.mContextManager->getContext();
        {
//            context->CSSetShader(mFactory.mSolverKernelComputeShader, NULL, 0);
            mFactory.mSolverKernelComputeShader.enable();
            GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();

            /*ID3D11ShaderResourceView* resourceViews[18] = {
                    mClothData.mBuffer.resourceView(), *//*mFrameData.mBuffer.resourceView()*//*NULL,
                    mIterationData.mBuffer.resourceView(), mFactory.mPhaseConfigs.mBuffer.resourceView(),
                    mFactory.mConstraints.mBuffer.resourceView(), mFactory.mTethers.mBuffer.resourceView(),
                    mFactory.mCapsuleIndicesDeviceCopy.resourceView(), mFactory.mCollisionSpheresDeviceCopy.resourceView(),
                    mFactory.mConvexMasksDeviceCopy.resourceView(), mFactory.mCollisionPlanesDeviceCopy.resourceView(),
                    mFactory.mCollisionTrianglesDeviceCopy.resourceView(),
                    mFactory.mMotionConstraints.mBuffer.resourceView(),
                    mFactory.mSeparationConstraints.mBuffer.resourceView(),
                    mFactory.mParticleAccelerations.mBuffer.resourceView(),
                    mFactory.mRestPositionsDeviceCopy.resourceView(),
                    mFactory.mSelfCollisionIndices.mBuffer.resourceView(),
                    mFactory.mStiffnessValues.mBuffer.resourceView(),
                    mFactory.mTriangles.mBuffer.resourceView()
            };
            context->CSSetShaderResources(0, 18, resourceViews);
            ID3D11UnorderedAccessView* accessViews[4] = {
                    mFactory.mParticles.mBuffer.accessViewRaw(),
                    mFactory.mSelfCollisionParticles.mBuffer.accessView(),
                    mFactory.mSelfCollisionData.mBuffer.accessView(),
                    mFrameData.mBuffer.accessView()
            };
            context->CSSetUnorderedAccessViews(0, 4, accessViews, NULL);*/

//            context->Dispatch(mCloths.size(), 1, 1);
            gl.glDispatchCompute(mCloths.size(), 1, 1);
            gl.glMemoryBarrier(GLenum.GL_SHADER_STORAGE_BARRIER_BIT);

            /*context->CSSetShader(NULL, NULL, 0);

            ID3D11ShaderResourceView* resourceViewsNULL[17] = {
                    NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL
            };
            context->CSSetShaderResources(0, 17, resourceViewsNULL);
            ID3D11UnorderedAccessView* accessViewsNULL[4] = { NULL, NULL, NULL, NULL };
            context->CSSetUnorderedAccessViews(0, 4, accessViewsNULL, NULL);*/
        }

        // copy particle data from device to host
        for (DxCloth it : mCloths)
        {
            it.mParticlesHostCopy.load(it.mParticles);
        }

        mFrameDataHostCopy.set(mFrameData);

//#if PX_DEBUG
        // cpu synchronization
//        context->End(mSyncQuery);
//        while (context->GetData(mSyncQuery, nullptr, 0, 0));  TODO

//#endif

    }
    private void endFrame(){
//        DxContextLock contextLock(mFactory);
        mFactory.mContextManager.acquireContext();

        ByteBuffer bytes = mFrameDataHostCopy.map(GLenum.GL_READ_ONLY);
        DxFrameData fIt = new DxFrameData();
//        ClothVector::Iterator cIt, cEnd = mCloths.end();
//        for (cIt = mCloths.begin(); cIt != cEnd; ++cIt, ++fIt)
        for(int i = 0; i < mCloths.size(); i++)
        {
            fIt.load(bytes);
            DxCloth cloth = /***cIt*/mCloths.get(i);

            cloth.mHostParticlesDirty = false;
            cloth.mDeviceParticlesDirty = false;

            cloth.mMotionConstraints.pop();
            // don't clear host copy because nothing is being uploaded yet
            // cloth.mMotionConstraints.mHostCopy.resize(0);

            cloth.mSeparationConstraints.pop();
            // don't clear host copy because nothing is being uploaded yet
            // cloth.mSeparationConstraints.mHostCopy.resize(0);

            if (!cloth.mTargetCollisionSpheres.empty())
            {
//                shdfnd::swap(cloth.mStartCollisionSpheres, cloth.mTargetCollisionSpheres);
                DxBatchedVector temp = cloth.mStartCollisionSpheres;
                cloth.mStartCollisionSpheres = cloth.mTargetCollisionSpheres;
                cloth.mTargetCollisionSpheres = temp;

                cloth.mTargetCollisionSpheres.resize(0);
            }

            if (!cloth.mTargetCollisionPlanes.empty())
            {
//                shdfnd::swap(cloth.mStartCollisionPlanes, cloth.mTargetCollisionPlanes);
                DxBatchedVector temp =cloth.mStartCollisionPlanes;
                cloth.mStartCollisionPlanes = cloth.mTargetCollisionPlanes;
                cloth.mTargetCollisionPlanes = temp;

                cloth.mTargetCollisionPlanes.resize(0);
            }

            if (!cloth.mTargetCollisionTriangles.empty())
            {
//                shdfnd::swap(cloth.mStartCollisionTriangles, cloth.mTargetCollisionTriangles);
                DxBatchedVector temp = cloth.mStartCollisionTriangles;
                cloth.mStartCollisionTriangles = cloth.mTargetCollisionTriangles;
                cloth.mTargetCollisionTriangles = temp;

                cloth.mTargetCollisionTriangles.resize(0);
            }

            for (int j = 0; j < 3; ++j)
            {
                float upper = fIt.mParticleBounds[j * 2 + 0];
                float negativeLower = fIt.mParticleBounds[j * 2 + 1];
                cloth.mParticleBoundsCenter.setValue(j, (upper - negativeLower) * 0.5f);
                cloth.mParticleBoundsHalfExtent.setValue(j, (upper + negativeLower) * 0.5f);
            }

            cloth.mSleepPassCounter = fIt.mSleepPassCounter;
            cloth.mSleepTestCounter = fIt.mSleepTestCounter;
        }
        mFrameDataHostCopy.unmap();

        interCollision();

	/*
	ID3DUserDefinedAnnotation* annotation;
	mFactory.mContextManager->getContext()->QueryInterface(&annotation);
	annotation->EndEvent();
	annotation->Release();
	*/
//        NV_CLOTH_PROFILE_STOP_CROSSTHREAD(mSimulateProfileEventData,"cloth::DxSolver::simulate", 0);
        mFactory.mContextManager.releaseContext();
    }

    private void interCollision(){
        if (mInterCollisionIterations == 0 || mInterCollisionDistance == 0.0f)
            return;
        if (mInterCollisionFilter == null)
        {
            LogUtil.i(LogUtil.LogType.DEFAULT, "Inter collision will not work unless an inter collision filter is set using Solver::setInterCollisionFilter.");
            return;
        }

//        typedef SwInterCollision<Simd4f> SwInterCollision;

        // rebuild cloth instance array
        mInterCollisionInstances.resize(0);
        DxFrameData* frameData = mFrameDataHostCopy.map(D3D11_MAP_READ);
        for (uint32_t i = 0, n = mCloths.size(); i < n; ++i)
        {
            DxCloth& cloth = *mCloths[i];

            cloth.mapParticles();
            float elasticity = 1.0f / frameData[i].mNumIterations;
            NV_CLOTH_ASSERT(!cloth.mHostParticlesDirty);
            PxVec4* particles = cloth.mParticlesMapPointer;
            uint32_t* indices = NULL, numIndices = cloth.mNumParticles;
            if (!cloth.mSelfCollisionIndices.empty())
            {
                indices = cloth.mSelfCollisionIndicesHost.begin();
                numIndices = uint32_t(cloth.mSelfCollisionIndices.size());
            }

            mInterCollisionInstances.pushBack(SwInterCollisionData(
                    particles, particles + cloth.mNumParticles, numIndices, indices, cloth.mTargetMotion,
                    cloth.mParticleBoundsCenter, cloth.mParticleBoundsHalfExtent, elasticity, cloth.mUserData));

            cloth.mDeviceParticlesDirty = true;
        }
        mFrameDataHostCopy.unmap();

        uint32_t requiredTempMemorySize = uint32_t(
                SwInterCollision::estimateTemporaryMemory(&mInterCollisionInstances[0], mInterCollisionInstances.size()));

        // realloc temp memory if necessary
        if (mInterCollisionScratchMemSize < requiredTempMemorySize)
        {
            if (mInterCollisionScratchMem)
                NV_CLOTH_FREE(mInterCollisionScratchMem);

            mInterCollisionScratchMem = NV_CLOTH_ALLOC(requiredTempMemorySize, "cloth::SwSolver::mInterCollisionScratchMem");
            mInterCollisionScratchMemSize = requiredTempMemorySize;
        }

        SwKernelAllocator allocator(mInterCollisionScratchMem, mInterCollisionScratchMemSize);

        // run inter-collision
        SwInterCollision(mInterCollisionInstances.begin(), mInterCollisionInstances.size(), mInterCollisionDistance,
                mInterCollisionStiffness, mInterCollisionIterations, mInterCollisionFilter, allocator)();

        for (uint32_t i = 0, n = mCloths.size(); i < n; ++i)
            mCloths[i]->unmapParticles();
    }

    @Override
    public void dispose() {
        if(!mCloths.isEmpty())
            throw new AssertionError();
        release();
    }
}
