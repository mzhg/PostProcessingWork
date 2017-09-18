package nv.visualFX.cloth.libs.dx;


import org.lwjgl.util.vector.Readable;
import org.lwjgl.util.vector.Vector4f;
import org.lwjgl.util.vector.Writable;

import java.nio.ByteBuffer;

import nv.visualFX.cloth.libs.IterationState;

/**
 * per-frame data (stored in pinned memory)<p></p>
 * Created by mazhen'gui on 2017/9/12.
 */

final class DxFrameData implements Readable, Writable{
    static final int SIZE = 0;
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

    DxFrameData(){}
    DxFrameData(DxCloth cloth, int numSharedPositions, IterationState state, int firstIteration)
    {
        mDeviceParticlesDirty = cloth.mDeviceParticlesDirty;

        mNumSharedPositions = numSharedPositions;

        mIterDt = state.mIterDt;

        mFirstIteration = firstIteration;
        mNumIterations = state.mRemainingIterations;

        float stiffnessExponent = cloth.mStiffnessFrequency * mIterDt;
        {
            Vector4f logStiffness = new Vector4f(0.0f, cloth.mSelfCollisionLogStiffness, cloth.mMotionConstraintLogStiffness,
                    cloth.mTetherConstraintLogStiffness);
//            Simd4f stiffness = gSimd4fOne - exp2(logStiffness * stiffnessExponent);
            Vector4f stiffness = new Vector4f();
            stiffness.x = (float) (1 - Math.pow(2, logStiffness.x * stiffnessExponent));
            stiffness.y = (float) (1 - Math.pow(2, logStiffness.y * stiffnessExponent));
            stiffness.z = (float) (1 - Math.pow(2, logStiffness.z * stiffnessExponent));
            stiffness.w = (float) (1 - Math.pow(2, logStiffness.w * stiffnessExponent));

            mTetherConstraintStiffness = stiffness.get(3);
            mMotionConstraintStiffness = stiffness.get(2);
            mSelfCollisionStiffness = stiffness.get(1);
        }
        {
            Vector4f logStiffness = new Vector4f(cloth.mDragLogCoefficient, cloth.mLiftLogCoefficient, 0.0f, 0.0f);
//            Simd4f stiffness = gSimd4fOne - exp2(logStiffness * stiffnessExponent);
            Vector4f stiffness = new Vector4f();
            stiffness.x = (float) (1 - Math.pow(2, logStiffness.x * stiffnessExponent));
            stiffness.y = (float) (1 - Math.pow(2, logStiffness.y * stiffnessExponent));
            stiffness.z = (float) (1 - Math.pow(2, logStiffness.z * stiffnessExponent));
            stiffness.w = (float) (1 - Math.pow(2, logStiffness.w * stiffnessExponent));

            mDragCoefficient = stiffness.get(0);
            mLiftCoefficient = stiffness.get(1);
            mFluidDensity = cloth.mFluidDensity * 0.5f; //divide by 2 to so we don't have to compensate for double area from cross product in the solver
            for(int i = 0; i < 9; ++i)
                mRotation[i] = state.mRotationMatrix[i / 3].get(i % 3);
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

        for (int i = 0; i < 3; ++i)
        {
            float c = cloth.mParticleBoundsCenter.get(i);
            float r = cloth.mParticleBoundsHalfExtent.get(i);
            mParticleBounds[i * 2 + 0] = r + c;
            mParticleBounds[i * 2 + 1] = r - c;
        }

        mSleepPassCounter = cloth.mSleepPassCounter;
        mSleepTestCounter = cloth.mSleepTestCounter;

        mStiffnessExponent = cloth.mStiffnessFrequency * mIterDt;

        mStartMotionConstrainsOffset = cloth.mMotionConstraints.mStart.empty() ? -1 : cloth.mMotionConstraints.mStart.mOffset;
        mTargetMotionConstrainsOffset = cloth.mMotionConstraints.mTarget.empty() ? mStartMotionConstrainsOffset : cloth.mMotionConstraints.mTarget.mOffset;

        mStartSeparationConstrainsOffset = cloth.mSeparationConstraints.mStart.empty() ? -1 : cloth.mSeparationConstraints.mStart.mOffset;
        mTargetSeparationConstrainsOffset = cloth.mSeparationConstraints.mTarget.empty() ? mStartSeparationConstrainsOffset : cloth.mSeparationConstraints.mTarget.mOffset;

        mParticleAccelerationsOffset = cloth.mParticleAccelerations.mOffset;
        mRestPositionsOffset = cloth.mRestPositions.empty() ? -1 : cloth.mRestPositions.mOffset;

        mInitSelfCollisionData = cloth.mInitSelfCollisionData;
        cloth.mInitSelfCollisionData = false;
    }

    @Override
    public ByteBuffer store(ByteBuffer buf) {
        // TODO
        return buf;
    }

    @Override
    public Writable load(ByteBuffer buf) {
        return null;
    }
}
