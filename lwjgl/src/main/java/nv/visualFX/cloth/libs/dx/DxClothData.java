package nv.visualFX.cloth.libs.dx;

/**
 * reference to cloth instance bulk data (POD)
 * should not need frequent updates (stored on device)<p></p>
 * Created by mazhen'gui on 2017/9/12.
 */

final class DxClothData {
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
}
