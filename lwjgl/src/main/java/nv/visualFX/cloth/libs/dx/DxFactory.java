package nv.visualFX.cloth.libs.dx;

import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import jet.opengl.postprocessing.buffer.BufferGL;
import jet.opengl.postprocessing.common.Disposeable;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.GLSLProgram;
import nv.visualFX.cloth.libs.Cloth;
import nv.visualFX.cloth.libs.DxContextManagerCallback;
import nv.visualFX.cloth.libs.Fabric;
import nv.visualFX.cloth.libs.Factory;
import nv.visualFX.cloth.libs.Platform;
import nv.visualFX.cloth.libs.Solver;

/**
 * Created by mazhen'gui on 2017/9/12.
 */

public class DxFactory implements Factory, Disposeable {
    final List<DxFabric> mFabrics = new ArrayList<>();

    DxContextManagerCallback mContextManager;
    BufferGL mStagingBuffer;

    GLSLProgram mSolverKernelComputeShader;

    int mNumThreadsPerBlock = 1024;

	final int mMaxThreadsPerBlock = 1024;

    DxBatchedStorage/*<DxConstraint>*/ mConstraints;
    DxBatchedStorage/*<DxConstraint>*/ mConstraintsHostCopy;
    DxBatchedStorage/*<float>*/ mStiffnessValues;
    DxBatchedStorage/*<DxTether>*/ mTethers;
    DxBatchedStorage/*<physx::PxVec4>*/ mParticles;
    DxBatchedStorage/*<physx::PxVec4>*/ mParticlesHostCopy;
    DxBatchedStorage/*<DxPhaseConfig>*/ mPhaseConfigs;

    DxBatchedStorage/*<physx::PxVec4>*/ mParticleAccelerations;
    DxBatchedStorage/*<physx::PxVec4>*/ mParticleAccelerationsHostCopy;

    DxBatchedStorage/*<IndexPair>*/ mCapsuleIndices;
    BufferGL/*DxBuffer<IndexPair>*/ mCapsuleIndicesDeviceCopy;

    DxBatchedStorage/*<physx::PxVec4>*/ mCollisionSpheres;
    BufferGL/*DxBuffer<physx::PxVec4>*/ mCollisionSpheresDeviceCopy;

    DxBatchedStorage/*<uint32_t>*/ mConvexMasks;
    BufferGL/*DxBuffer<uint32_t>*/ mConvexMasksDeviceCopy;

    DxBatchedStorage/*<physx::PxVec4>*/ mCollisionPlanes;
    BufferGL/*DxBuffer<physx::PxVec4>*/ mCollisionPlanesDeviceCopy;

    DxBatchedStorage/*<physx::PxVec3>*/ mCollisionTriangles;
    BufferGL/*DxBuffer<physx::PxVec3>*/ mCollisionTrianglesDeviceCopy;

    DxBatchedStorage/*<physx::PxVec4>*/ mMotionConstraints;
    DxBatchedStorage/*<physx::PxVec4>*/ mSeparationConstraints;

    DxBatchedStorage/*<physx::PxVec4>*/ mRestPositions;
    BufferGL/*<physx::PxVec4>*/ mRestPositionsDeviceCopy;

    DxBatchedStorage/*<uint32_t>*/ mSelfCollisionIndices;
    DxBatchedStorage/*<physx::PxVec4>*/ mSelfCollisionParticles;
    DxBatchedStorage/*<uint32_t>*/ mSelfCollisionData;

    DxBatchedStorage/*<uint32_t>*/ mTriangles;

    public DxFactory(DxContextManagerCallback contextManager){
        mContextManager = contextManager;
        final int target = GLenum.GL_UNIFORM_BUFFER;
        final int usage = GLenum.GL_STREAM_DRAW;
        mConstraints = new DxBatchedStorage(mContextManager, target, usage, DxConstraint.SIZE);
        mConstraintsHostCopy = new DxBatchedStorage(mContextManager, /*DxStagingBufferPolicy()*/ target, usage, DxConstraint.SIZE);  // cpu acess
        mStiffnessValues = new DxBatchedStorage(mContextManager, target, usage, 4);
        mTethers = new DxBatchedStorage(mContextManager, target, usage, DxTether.SIZE);
        mParticles = new DxBatchedStorage(mContextManager, GLenum.GL_ARRAY_BUFFER, GLenum.GL_DYNAMIC_DRAW, Vector4f.SIZE);
        mParticlesHostCopy = new DxBatchedStorage(mContextManager, GLenum.GL_ARRAY_BUFFER, GLenum.GL_DYNAMIC_DRAW, Vector4f.SIZE); // cpu acess
        mParticleAccelerations = new DxBatchedStorage(mContextManager, GLenum.GL_ARRAY_BUFFER, GLenum.GL_DYNAMIC_DRAW, Vector4f.SIZE);
        mParticleAccelerationsHostCopy = new DxBatchedStorage(mContextManager, GLenum.GL_ARRAY_BUFFER, GLenum.GL_DYNAMIC_DRAW, Vector4f.SIZE); // cpu acess
        mPhaseConfigs = new DxBatchedStorage(mContextManager, target, usage, DxPhaseConfig.SIZE);
        mCapsuleIndices = new DxBatchedStorage(mContextManager, target, usage, 8);
        mCapsuleIndicesDeviceCopy = new BufferGL();
        mCollisionSpheres = new DxBatchedStorage(mContextManager, GLenum.GL_ARRAY_BUFFER, GLenum.GL_DYNAMIC_DRAW, Vector4f.SIZE);
        mCollisionSpheresDeviceCopy = new BufferGL();// cpu acess
        mConvexMasks = new DxBatchedStorage(mContextManager, GLenum.GL_ARRAY_BUFFER, GLenum.GL_DYNAMIC_DRAW, 4);
        mConvexMasksDeviceCopy = new BufferGL();
        mCollisionPlanes = new DxBatchedStorage(mContextManager, GLenum.GL_ARRAY_BUFFER, GLenum.GL_DYNAMIC_DRAW, Vector4f.SIZE);
        mCollisionPlanesDeviceCopy = new BufferGL();
        mCollisionTriangles = new DxBatchedStorage(mContextManager, GLenum.GL_ARRAY_BUFFER, GLenum.GL_DYNAMIC_DRAW, Vector3f.SIZE);
        mCollisionTrianglesDeviceCopy = new BufferGL();
        mMotionConstraints = new DxBatchedStorage(mContextManager, GLenum.GL_ARRAY_BUFFER, GLenum.GL_DYNAMIC_DRAW, Vector4f.SIZE);
        mSeparationConstraints = new DxBatchedStorage(mContextManager, GLenum.GL_ARRAY_BUFFER, GLenum.GL_DYNAMIC_DRAW, Vector4f.SIZE);
        mRestPositions = new DxBatchedStorage(mContextManager, GLenum.GL_ARRAY_BUFFER, GLenum.GL_DYNAMIC_DRAW, Vector4f.SIZE);
        mRestPositionsDeviceCopy = new BufferGL();
        mSelfCollisionIndices = new DxBatchedStorage(mContextManager, GLenum.GL_ARRAY_BUFFER, GLenum.GL_DYNAMIC_DRAW, 4);
        mSelfCollisionParticles = new DxBatchedStorage(mContextManager, GLenum.GL_ARRAY_BUFFER, GLenum.GL_DYNAMIC_DRAW, Vector4f.SIZE);
        mSelfCollisionData = new DxBatchedStorage(mContextManager, GLenum.GL_ARRAY_BUFFER, GLenum.GL_DYNAMIC_DRAW, 4);
        mTriangles = new DxBatchedStorage(mContextManager, GLenum.GL_ARRAY_BUFFER, GLenum.GL_DYNAMIC_DRAW, 4);

        if (mContextManager.synchronizeResources())
        {
            // allow particle interop with other device
//            mParticles.mBuffer.mMiscFlag = TODO
//                    D3D11_RESOURCE_MISC_FLAG(mParticles.mBuffer.mMiscFlag | D3D11_RESOURCE_MISC_SHARED_KEYEDMUTEX);
        }
    }

    static int sNextFabricId = 0;
    private static int getNextFabricId()
    {
        return sNextFabricId++;
    }

    @Override
    public Platform getPlatform() {
        return Platform.DX11;
    }

    @Override
    public Fabric createFabric(int numParticles, IntBuffer phaseIndices, IntBuffer sets, FloatBuffer restvalues, FloatBuffer stiffnessValues, IntBuffer indices, IntBuffer anchors, FloatBuffer tetherLengths, IntBuffer triangles) {
        return new DxFabric(this, numParticles, phaseIndices, sets, restvalues, stiffnessValues, indices, anchors, tetherLengths, triangles,
                getNextFabricId());
    }

    @Override
    public Cloth createCloth(FloatBuffer particles, Fabric fabric) {
        return new DxCloth(this, (DxFabric)fabric, particles);
    }

    @Override
    public Solver createSolver() {
        CompileComputeShaders(); //Make sure our compute shaders are ready
        DxSolver solver = new DxSolver(this);

        if (solver.hasError())
        {
//            NV_CLOTH_DELETE(solver);
            return null;
        }

        return solver;
    }

    @Override
    public Cloth clone(Cloth cloth) {
        return null;
    }

    @Override
    public void extractFabricData(Fabric fabric, IntBuffer phaseIndices, IntBuffer sets, FloatBuffer restvalues, FloatBuffer stiffnessValues, IntBuffer indices, IntBuffer anchors, FloatBuffer tetherLengths, IntBuffer triangles) {
        DxContextLock contextLock = new DxContextLock(this);

	    DxFabric dxFabric = (DxFabric) fabric;

        /*if (phaseIndices.remaining() > 0)
        {
            assert(phaseIndices.remaining() == dxFabric.mPhases.size());
            memcpy(phaseIndices.begin(), dxFabric.mPhases.begin(), phaseIndices.size() * sizeof(uint32_t));
        }

        if (!restvalues.empty())
        {
            NV_CLOTH_ASSERT(restvalues.size() == dxFabric.mConstraints.size());
            Vector<DxConstraint>::Type hostConstraints(restvalues.size());
            copyToHost(hostConstraints.begin(), dxFabric.mConstraints.buffer(), dxFabric.mConstraints.mOffset * sizeof(DxConstraint),
                    uint32_t(hostConstraints.size() * sizeof(DxConstraint)));
            for (uint32_t i = 0, n = restvalues.size(); i < n; ++i)
                restvalues[i] = hostConstraints[i].mRestvalue;
        }

        if (!stiffnessValues.empty())
        {
            NV_CLOTH_ASSERT(stiffnessValues.size() == dxFabric.mStiffnessValues.size());
            Vector<float>::Type hostStiffnessValues(stiffnessValues.size());
            copyToHost(hostStiffnessValues.begin(), dxFabric.mStiffnessValues.buffer(), dxFabric.mStiffnessValues.mOffset * sizeof(float),
            uint32_t(hostStiffnessValues.size() * sizeof(float)));
            for (uint32_t i = 0, n = stiffnessValues.size(); i < n; ++i)
                stiffnessValues[i] = hostStiffnessValues[i];
        }

        if (!sets.empty())
        {
            // need to skip copying the first element
            NV_CLOTH_ASSERT(sets.size() == dxFabric.mSets.size());
            memcpy(sets.begin(), dxFabric.mSets.begin(), sets.size() * sizeof(uint32_t));
        }

        if (!indices.empty())
        {
            NV_CLOTH_ASSERT(indices.size() == dxFabric.mConstraints.size()*2);
            Vector<DxConstraint>::Type hostConstraints(dxFabric.mConstraints.size());
            copyToHost(hostConstraints.begin(), dxFabric.mConstraints.buffer(), dxFabric.mConstraints.mOffset * sizeof(DxConstraint),
                    uint32_t(hostConstraints.size() * sizeof(DxConstraint)));

            auto cIt = hostConstraints.begin(), cEnd = hostConstraints.end();
            for (uint32_t* iIt = indices.begin(); cIt != cEnd; ++cIt)
            {
			*iIt++ = cIt->mFirstIndex;
			*iIt++ = cIt->mSecondIndex;
            }
        }

        if (!anchors.empty() || !tetherLengths.empty())
        {
            uint32_t numTethers = uint32_t(dxFabric.mTethers.size());
            Vector<DxTether>::Type tethers(numTethers, DxTether(0, 0));
            copyToHost(tethers.begin(), dxFabric.mTethers.buffer(), dxFabric.mTethers.mOffset  * sizeof(DxTether),
                    uint32_t(tethers.size() * sizeof(DxTether)));

            NV_CLOTH_ASSERT(anchors.empty() || anchors.size() == tethers.size());
            for (uint32_t i = 0; !anchors.empty(); ++i, anchors.popFront())
                anchors.front() = tethers[i].mAnchor;

            NV_CLOTH_ASSERT(tetherLengths.empty() || tetherLengths.size() == tethers.size());
            for (uint32_t i = 0; !tetherLengths.empty(); ++i, tetherLengths.popFront())
                tetherLengths.front() = tethers[i].mLength * dxFabric.mTetherLengthScale;
        }

        if (!triangles.empty())
        {
            // todo triangles
        }*/

        contextLock.release();
    }

    @Override
    public void extractCollisionData(Cloth cloth, FloatBuffer spheres, IntBuffer capsules, FloatBuffer planes, IntBuffer convexes, FloatBuffer triangles) {

    }

    @Override
    public void extractMotionConstraints(Cloth cloth, FloatBuffer destConstraints) {

    }

    @Override
    public void extractSeparationConstraints(Cloth cloth, FloatBuffer destConstraints) {

    }

    @Override
    public void extractParticleAccelerations(Cloth cloth, FloatBuffer destAccelerations) {

    }

    @Override
    public void extractVirtualParticles(Cloth cloth, IntBuffer destIndices, FloatBuffer destWeights) {

    }

    @Override
    public void extractSelfCollisionIndices(Cloth cloth, IntBuffer destIndices) {

    }

    @Override
    public void extractRestPositions(Cloth cloth, FloatBuffer destRestPositions) {

    }

    void copyToHost(ByteBuffer dst, BufferGL buffer, int offset, int size) //size and offset in bytes (or in pixels when buffer is a texture?)
    {
        if (size == 0)
            return;

        DxContextLock contextLock = new DxContextLock(this);

        reserveStagingBuffer(size);
//        CD3D11_BOX box(offset, 0, 0, offset + size, 1, 1);
//        mContextManager->getContext()->CopySubresourceRegion(mStagingBuffer, 0, 0, 0, 0, srcBuffer, 0, &box);
        DxBatchedStorage.CopySubresourceRegion(mStagingBuffer, 0, buffer, offset, size, GLFuncProviderFactory.getGLFuncProvider());
//	void* mapIt = mapStagingBuffer(D3D11_MAP_READ);
//        memcpy(dst, mapIt, size);
        ByteBuffer mapIt = mapStagingBuffer(GLenum.GL_READ_ONLY);
        mapIt.position(0).limit(size);
        MemoryUtil.memCopy(MemoryUtil.memAddress(mapIt), MemoryUtil.memAddress(dst), size);

        unmapStagingBuffer();

        contextLock.release();
    }

    void CompileComputeShaders() // this is called once to setup the shaders
    {

    }

    void reserveStagingBuffer(int size){

    }
    ByteBuffer mapStagingBuffer(int mapBits){

        return null;
    }
    void unmapStagingBuffer(){

    }

    @Override
    public void dispose() {
//        DxContextLock(this);
        assert mFabrics.size() == 0: "All fabrics created by this factory need to be deleted before this factory is destroyed.";
        if (mStagingBuffer != null)
            mStagingBuffer.dispose();

        if (mSolverKernelComputeShader != null)
            mSolverKernelComputeShader.dispose();
    }
}
