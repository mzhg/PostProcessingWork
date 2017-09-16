package nv.visualFX.cloth.libs.dx;

import org.lwjgl.util.vector.Quaternion;
import org.lwjgl.util.vector.Vector3f;

import nv.visualFX.cloth.libs.IterationState;

/**
 * Created by mazhen'gui on 2017/9/13.
 */

public class IterationStateFactory {
    public int mNumIterations;
    public float mInvNumIterations;
    public float mIterDt, mIterDtRatio, mIterDtAverage;
    public final Quaternion mCurrentRotation = new Quaternion();
    public final Vector3f mPrevLinearVelocity = new Vector3f();
    public final Vector3f mPrevAngularVelocity = new Vector3f();

    public IterationStateFactory(DxCloth cloth, float frameDt){
        mNumIterations = Math.max(1, (int)(frameDt * cloth.mSolverFrequency + 0.5f));
        mInvNumIterations = 1.0f / mNumIterations;
        mIterDt = frameDt * mInvNumIterations;

        mIterDtRatio = cloth.mPrevIterDt ? mIterDt / cloth.mPrevIterDt : 1.0f;
        mIterDtAverage = cloth.mIterDtAvg.empty() ? mIterDt : cloth.mIterDtAvg.average();

        mCurrentRotation = cloth.mCurrentMotion.q;
        mPrevLinearVelocity = cloth.mLinearVelocity;
        mPrevAngularVelocity = cloth.mAngularVelocity;

        // update cloth
        float invFrameDt = 1.0f / frameDt;
        cloth.mLinearVelocity = invFrameDt * (cloth.mTargetMotion.p - cloth.mCurrentMotion.p);
        physx::PxQuat dq = cloth.mTargetMotion.q * cloth.mCurrentMotion.q.getConjugate();
        cloth.mAngularVelocity = log(dq) * invFrameDt;

        cloth.mPrevIterDt = mIterDt;
        cloth.mIterDtAvg.push(static_cast<uint32_t>(mNumIterations), mIterDt);
        cloth.mCurrentMotion = cloth.mTargetMotion;
    }

    public IterationState create(DxCloth cloth) {

    }
}
