package nv.visualFX.cloth.libs.dx;

/**
 * per-frame data (stored in pinned memory)<p></p>
 * Created by mazhen'gui on 2017/9/12.
 */

final class DxFrameData {
    boolean mDeviceParticlesDirty;

    // number of particle copies that fit in shared memory (0, 1, or 2)
    int mNumSharedPositions;

    // iteration data
    float mIterDt;
    int mFirstIteration;
    int mNumIterations;

    float mTetherConstraintStiffness;

    // wind data
    float mDragCoefficient;
    float mLiftCoefficient;
    float mFluidDensity;
    final float[] mRotation = new float[9];

    // motion constraint data
    float mMotionConstraintStiffness;
    int mStartMotionConstrainsOffset;
    int mTargetMotionConstrainsOffset;

    // separation constraint data
    int mStartSeparationConstrainsOffset;
    int mTargetSeparationConstrainsOffset;

    // particle acceleration data
    int mParticleAccelerationsOffset;

    int mStartSphereOffset;
    int mTargetSphereOffset;

    int mStartCollisionPlaneOffset;
    int mTargetCollisionPlaneOffset;

    int mStartCollisionTrianglesOffset;
    int mTargetCollisionTrianglesOffset;

    float mSelfCollisionStiffness;

    final float[] mParticleBounds = new float[6]; // maxX, -minX, maxY, ...

    int mSleepPassCounter;
    int mSleepTestCounter;

    float mStiffnessExponent;

    int mRestPositionsOffset;

    boolean mInitSelfCollisionData;

    /*DxFrameData(DxCloth cloth, uint32_t numSharedPositions, const IterationState<Simd4f>& state, uint32_t firstIteration)
    {
        mDeviceParticlesDirty = cloth.mDeviceParticlesDirty;

        mNumSharedPositions = numSharedPositions;

        mIterDt = state.mIterDt;

        mFirstIteration = firstIteration;
        mNumIterations = state.mRemainingIterations;

        Simd4f stiffnessExponent = simd4f(cloth.mStiffnessFrequency * mIterDt);
        {
            Simd4f logStiffness = simd4f(0.0f, cloth.mSelfCollisionLogStiffness, cloth.mMotionConstraintLogStiffness,
                    cloth.mTetherConstraintLogStiffness);
            Simd4f stiffness = gSimd4fOne - exp2(logStiffness * stiffnessExponent);

            mTetherConstraintStiffness = array(stiffness)[3];
            mMotionConstraintStiffness = array(stiffness)[2];
            mSelfCollisionStiffness = array(stiffness)[1];
        }
        {
            Simd4f logStiffness = simd4f(cloth.mDragLogCoefficient, cloth.mLiftLogCoefficient, 0.0f, 0.0f);
            Simd4f stiffness = gSimd4fOne - exp2(logStiffness * stiffnessExponent);
            mDragCoefficient = array(stiffness)[0];
            mLiftCoefficient = array(stiffness)[1];
            mFluidDensity = cloth.mFluidDensity * 0.5f; //divide by 2 to so we don't have to compensate for double area from cross product in the solver
            for(int i = 0; i < 9; ++i)
                mRotation[i] = array(state.mRotationMatrix[i / 3])[i % 3];
        }

        mStartSphereOffset = cloth.mStartCollisionSpheres.mOffset;
        mTargetSphereOffset =
                cloth.mTargetCollisionSpheres.empty() ? mStartSphereOffset : cloth.mTargetCollisionSpheres.mOffset;

        mStartCollisionPlaneOffset = cloth.mStartCollisionPlanes.mOffset;
        mTargetCollisionPlaneOffset =
                cloth.mTargetCollisionPlanes.empty() ? mStartCollisionPlaneOffset : cloth.mTargetCollisionPlanes.mOffset;

        mStartCollisionTrianglesOffset = cloth.mStartCollisionTriangles.mOffset;
        mTargetCollisionTrianglesOffset =
                cloth.mTargetCollisionTriangles.empty() ? mStartCollisionTrianglesOffset : cloth.mTargetCollisionTriangles.mOffset;

        for (uint32_t i = 0; i < 3; ++i)
        {
            float c = array(cloth.mParticleBoundsCenter)[i];
            float r = array(cloth.mParticleBoundsHalfExtent)[i];
            mParticleBounds[i * 2 + 0] = r + c;
            mParticleBounds[i * 2 + 1] = r - c;
        }

        mSleepPassCounter = cloth.mSleepPassCounter;
        mSleepTestCounter = cloth.mSleepTestCounter;

        mStiffnessExponent = cloth.mStiffnessFrequency * mIterDt;

        mStartMotionConstrainsOffset = cloth.mMotionConstraints.mStart.empty() ? uint32_t(-1) : cloth.mMotionConstraints.mStart.mOffset;
        mTargetMotionConstrainsOffset = cloth.mMotionConstraints.mTarget.empty() ? mStartMotionConstrainsOffset : cloth.mMotionConstraints.mTarget.mOffset;

        mStartSeparationConstrainsOffset = cloth.mSeparationConstraints.mStart.empty() ? uint32_t(-1) : cloth.mSeparationConstraints.mStart.mOffset;
        mTargetSeparationConstrainsOffset = cloth.mSeparationConstraints.mTarget.empty() ? mStartSeparationConstrainsOffset : cloth.mSeparationConstraints.mTarget.mOffset;

        mParticleAccelerationsOffset = cloth.mParticleAccelerations.mOffset;
        mRestPositionsOffset = cloth.mRestPositions.empty() ? uint32_t(-1) : cloth.mRestPositions.mOffset;

        mInitSelfCollisionData = cloth.mInitSelfCollisionData;
        cloth.mInitSelfCollisionData = false;
    }*/
}
