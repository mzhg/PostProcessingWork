package nv.visualFX.cloth.libs.dx;

import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import jet.opengl.postprocessing.common.Disposeable;
import jet.opengl.postprocessing.util.CacheBuffer;
import jet.opengl.postprocessing.util.Numeric;
import jet.opengl.postprocessing.util.StackInt;
import nv.visualFX.cloth.libs.Fabric;
import nv.visualFX.cloth.libs.Factory;

/**
 * Created by mazhen'gui on 2017/9/12.
 */

final class DxFabric implements Fabric, Disposeable{

    DxFactory mFactory;

    int mNumParticles;

    IntBuffer mPhases; // index of set to use
    IntBuffer mSets;   // offset of last restvalue

    DxBatchedVector/*<DxConstraint>*/ mConstraints;
    DxBatchedVector/*<DxConstraint>*/ mConstraintsHostCopy;
    DxBatchedVector/*<float>*/ mStiffnessValues;

    DxBatchedVector/*<DxTether>*/ mTethers;
    float mTetherLengthScale;

    DxBatchedVector/*<uint32_t>*/ mTriangles; //data stored is actually uint16_t
    int mNumTriangles;

    StackInt mFirstConstraintInPhase;
    StackInt mNumConstraintsInPhase;

    int mId;

//    @CachaRes
    DxFabric(DxFactory factory, int numParticles, IntBuffer phaseIndices, IntBuffer sets,
             FloatBuffer restvalues, FloatBuffer stiffnessValues, IntBuffer indices, IntBuffer anchors,
             FloatBuffer tetherLengths, IntBuffer triangles, int id){
        mFactory = factory;
        mFactory.mContextManager.acquireContext();
        mNumParticles = (numParticles);
        mPhases = phaseIndices;
        mSets = sets;
        mConstraints = new DxBatchedVector(mFactory.mConstraints);
        mConstraintsHostCopy = new DxBatchedVector(mFactory.mConstraintsHostCopy);
        mTriangles = new DxBatchedVector(mFactory.mTriangles);
        mStiffnessValues = new DxBatchedVector(mFactory.mStiffnessValues);
        mTethers = new DxBatchedVector(mFactory.mTethers);
        mId = id;

        // should no longer be prefixed with 0
//        assert (sets.front() != 0);
//        assert (sets.back() == restvalues.size());
//        assert (restvalues.size() * 2 == indices.size());
//        assert (restvalues.size() == stiffnessValues.size() || stiffnessValues.size() == 0);
//        assert (mNumParticles > *shdfnd::maxElement(indices.begin(), indices.end()));

        // manually convert uint32_t indices to uint16_t in temp memory
        /*Vector<DxConstraint>::Type hostConstraints;
        hostConstraints.resizeUninitialized(restvalues.size());
        Vector<DxConstraint>::Type::Iterator cIt = hostConstraints.begin();
        Vector<DxConstraint>::Type::Iterator cEnd = hostConstraints.end();
        const uint32_t* iIt = indices.begin();
        const float* rIt = restvalues.begin();
        for (; cIt != cEnd; ++cIt)
        {
            cIt->mRestvalue = *rIt++;
            cIt->mFirstIndex = uint16_t(*iIt++);
            cIt->mSecondIndex = uint16_t(*iIt++);
        }*/
        byte[] hostConstraints = new byte[restvalues.remaining()*DxConstraint.SIZE];
        int position = 0;
        int rIt = restvalues.position();
        int iIt = indices.position();
        for(int i = 0; i < restvalues.remaining(); i++){
            position = Numeric.getBytes(restvalues.get(rIt++), hostConstraints, position);
            position = Numeric.getBytes((short)indices.get(iIt++), hostConstraints, position);
            position = Numeric.getBytes((short)indices.get(iIt++), hostConstraints, position);
        }

        // copy to device vector in one go
//#if 0
//        // Workaround for NvAPI SCG device updateSubresource size limit
//        mConstraintsHostCopy.assign(hostConstraints.begin(), hostConstraints.end());
//        mConstraints.resize(mConstraintsHostCopy.size());
//        mConstraints = mConstraintsHostCopy;
//#else
        mConstraints.assign(/*hostConstraints.begin(), hostConstraints.end(),*/ hostConstraints.length, CacheBuffer.wrap(hostConstraints));
//#endif

        mStiffnessValues.assign(/*stiffnessValues.begin(), stiffnessValues.end()*/ stiffnessValues.remaining() * 4, stiffnessValues);

        // gather data per phase
        mFirstConstraintInPhase.reserve(phaseIndices.remaining());
        mNumConstraintsInPhase.reserve(phaseIndices.remaining());
        for (int  pIt = phaseIndices.position(); pIt != phaseIndices.limit(); ++pIt)
        {
            int setIndex = phaseIndices.get(pIt);
            int firstIndex = setIndex !=0? sets.get(setIndex - 1) : 0;
            int lastIndex = sets.get(setIndex);
            mFirstConstraintInPhase.push(firstIndex);
            mNumConstraintsInPhase.push(lastIndex - firstIndex);
        }

        // tethers
        assert (anchors.remaining() == tetherLengths.remaining());
        mTetherLengthScale =
                tetherLengths.remaining()!=0 ? 1.0f :DxUtil.maxElement(/*tetherLengths.begin(), tetherLengths.end()*/tetherLengths) / /*USHRT_MAX*/ Numeric.MAX_USHORT;
        float inverseScale = 1 / (mTetherLengthScale + Float.MIN_NORMAL);
        /*Vector<DxTether>::Type tethers;
        tethers.reserve(anchors.size());
        for (; !anchors.empty(); anchors.popFront(), tetherLengths.popFront())
        {
            tethers.pushBack(DxTether(uint16_t(anchors.front()), uint16_t(tetherLengths.front() * inverseScale + 0.5f)));
        }
        mTethers.assign(tethers.begin(), tethers.end());*/
        byte[] tethers = new byte[anchors.remaining() * DxTether.SIZE];
        position = 0;
        while(anchors.remaining()>0){
            position = Numeric.getBytes((short)anchors.get(), tethers, position);
            position = Numeric.getBytes((short)(tetherLengths.get() * inverseScale + 0.5f), tethers, position);
        }
        mTethers.assign(tethers.length, CacheBuffer.wrap(tethers));


        mNumTriangles = triangles.remaining();
        // triangles
        /*Vector<uint32_t>::Type hostTriangles;
        //make sure there is an even number of elements allocated
        hostTriangles.resizeUninitialized(triangles.size()>>1);
        Vector<uint32_t>::Type::Iterator tIt = hostTriangles.begin();
        for (; !triangles.empty(); triangles.popFront())
        {
            uint32_t packed = triangles.front();
            triangles.popFront();
            if(!triangles.empty())
                packed |= triangles.front()<<16;
		*tIt++ = packed;
        }*/
        int[] hostTriangles = new int[mNumTriangles >>1];
        int tIt = 0;
        while (triangles.remaining() > 0){
            int packed = triangles.get();
//            triangles.popFront();
            if(triangles.remaining() > 0)
                packed |= triangles.get(triangles.position())<<16;
//		*tIt++ = packed;
            hostTriangles[tIt++] = packed;
        }

        mTriangles.assign(/*hostTriangles.begin(), hostTriangles.end()*/hostTriangles.length * 4, CacheBuffer.wrap(hostTriangles));

        mFactory.mContextManager.releaseContext();

        // add to factory
        mFactory.mFabrics.add(this);
    }

    @Override
    public Factory getFactory() {
        return mFactory;
    }

    @Override
    public int getNumPhases() {
        return mPhases.remaining();
    }

    @Override
    public int getNumRestvalues() {
        return mConstraints.size();
    }

    @Override
    public int getNumStiffnessValues() {
        return mStiffnessValues.size();
    }

    @Override
    public int getNumSets() {
        return mSets.remaining();
    }

    @Override
    public int getNumIndices() {
        return mConstraints.size() * 2;
    }

    @Override
    public int getNumParticles() {
        return mNumParticles;
    }

    @Override
    public int getNumTethers() {
        return mTethers.size();
    }

    @Override
    public int getNumTriangles() {
        return mNumTriangles/3;
    }

    @Override
    public void scaleRestvalues(float scale) {
//        DxContextLock contextLock(mFactory);
        mFactory.mContextManager.acquireContext();

//        Vector<DxConstraint>::Type constraints(uint32_t(mConstraints.size()));
        ByteBuffer constraints = MemoryUtil.memAlloc(mConstraints.size() * DxConstraint.SIZE);
        mFactory.copyToHost(constraints, mConstraints.buffer(), mConstraints.mOffset * DxConstraint.SIZE,
                /*constraints.size() * sizeof(DxConstraint)*/constraints.remaining());

//        Vector<DxConstraint>::Type::Iterator cIt, cEnd = constraints.end();
//        for (cIt = constraints.begin(); cIt != cEnd; ++cIt)
//            cIt->mRestvalue *= scale;
        for(int cIt = 0; cIt < constraints.remaining(); cIt+=DxConstraint.SIZE){
            float restValue = constraints.getFloat(cIt);
            restValue*=scale;
            constraints.putFloat(cIt, restValue);
        }

        mConstraints.assign(constraints.remaining(), constraints);
        mFactory.mContextManager.releaseContext();
        MemoryUtil.memFree(constraints);
    }

    @Override
    public void scaleTetherLengths(float scale) {
        // cloth instances won't pick this up until DxClothData is dirty!
        mTetherLengthScale *= scale;
    }

    @Override
    public void dispose() {
        mFactory.mFabrics.remove(this);
    }
}
