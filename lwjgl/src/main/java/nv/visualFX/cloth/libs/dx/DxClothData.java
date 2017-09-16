package nv.visualFX.cloth.libs.dx;

import org.lwjgl.util.vector.Readable;

import java.nio.ByteBuffer;

/**
 * reference to cloth instance bulk data (POD)
 * should not need frequent updates (stored on device)<p></p>
 * Created by mazhen'gui on 2017/9/12.
 */

final class DxClothData implements Readable{
    static final int SIZE = /*31 * 4*/ 32 * 4;

    int mNumParticles;
    int mParticlesOffset;

    // fabric constraints
    int mNumPhases;
    int mPhaseConfigOffset;
    int mConstraintOffset;
    int mStiffnessOffset; //Offset inside per constraint stiffness buffer

    int mNumTethers;
    int mTetherOffset;
    float mTetherConstraintScale;

    int mNumTriangles;
    int mStartTriangleOffset;

    // motion constraint data
    float mMotionConstraintScale;
    float mMotionConstraintBias;

    // collision
    int mNumCapsules;
    int mCapsuleOffset;
    int mNumSpheres;

    int mNumPlanes;
    int mNumConvexes;
    int mConvexMasksOffset;

    int mNumCollisionTriangles;

    int mEnableContinuousCollision; //bool stored in int for dx alignment
    float mCollisionMassScale;
    float mFrictionScale;

    float mSelfCollisionDistance;

    int mNumSelfCollisionIndices;
    int mSelfCollisionIndicesOffset;
    int mSelfCollisionParticlesOffset;
    int mSelfCollisionDataOffset;

    // sleep data
    int mSleepTestInterval;
    int mSleepAfterCount;
    float mSleepThreshold;

    DxClothData(){}
    DxClothData(DxCloth cloth)
    {
        load(cloth);
    }

    @Override
    public ByteBuffer store(ByteBuffer buf) {
        return null;
    }

    void load(DxCloth cloth){
        mNumParticles = cloth.mNumParticles;
        mParticlesOffset = cloth.mParticles.mOffset;

        mNumPhases = cloth.mPhaseConfigs.size();
        mPhaseConfigOffset = cloth.mPhaseConfigs.mOffset;
        mConstraintOffset = cloth.mFabric.mConstraints.mOffset;
        mStiffnessOffset = cloth.mFabric.mStiffnessValues.empty() ? -1: cloth.mFabric.mStiffnessValues.mOffset;

        mNumTriangles = cloth.mFabric.getNumTriangles();
        mStartTriangleOffset = cloth.mFabric.mTriangles.mOffset;

        mNumTethers = cloth.mFabric.mTethers.size();
        mTetherOffset = cloth.mFabric.mTethers.mOffset;
        mTetherConstraintScale = cloth.mTetherConstraintScale * cloth.mFabric.mTetherLengthScale;

        mMotionConstraintScale = cloth.mMotionConstraintScale;
        mMotionConstraintBias = cloth.mMotionConstraintBias;

        mNumCapsules = cloth.mCapsuleIndices.size();
        mCapsuleOffset = cloth.mCapsuleIndices.mOffset;
        mNumSpheres = cloth.mStartCollisionSpheres.size();

        mNumPlanes = cloth.mStartCollisionPlanes.size();
        mNumConvexes = cloth.mConvexMasks.size();
        mConvexMasksOffset = cloth.mConvexMasks.mOffset;

        mNumCollisionTriangles = (cloth.mStartCollisionTriangles.size()) / 3;

        mEnableContinuousCollision = cloth.mEnableContinuousCollision?1:0;
        mCollisionMassScale = cloth.mCollisionMassScale;
        mFrictionScale = cloth.mFriction;

        mSelfCollisionDistance = cloth.mSelfCollisionDistance;
        mNumSelfCollisionIndices = cloth.mSelfCollisionIndices.empty() ? mNumParticles : cloth.mSelfCollisionIndices.size();
        mSelfCollisionIndicesOffset = cloth.mSelfCollisionIndices.empty() ? -1 : cloth.mSelfCollisionIndices.mOffset;
        mSelfCollisionParticlesOffset = cloth.mSelfCollisionParticles.mOffset;
        mSelfCollisionDataOffset = cloth.mSelfCollisionData.mOffset;

        mSleepTestInterval = cloth.mSleepTestInterval;
        mSleepAfterCount = cloth.mSleepAfterCount;
        mSleepThreshold = cloth.mSleepThreshold;
    }
}
