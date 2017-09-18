package nv.visualFX.cloth.libs.dx;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Quaternion;
import org.lwjgl.util.vector.ReadableVector3f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import nv.visualFX.cloth.libs.IterationState;

/**
 * Created by mazhen'gui on 2017/9/13.
 */

public class IterationStateFactory {
    static final float PX_EPS_REAL = 1.192092896e-07F;
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

        mIterDtRatio = cloth.mPrevIterDt != 0 ? mIterDt / cloth.mPrevIterDt : 1.0f;
        mIterDtAverage = cloth.mIterDtAvg.empty() ? mIterDt : cloth.mIterDtAvg.average();

        mCurrentRotation.set(cloth.mCurrentMotion.q);
        mPrevLinearVelocity.set(cloth.mLinearVelocity);
        mPrevAngularVelocity.set(cloth.mAngularVelocity);

        // update cloth
        float invFrameDt = 1.0f / frameDt;
//        cloth.mLinearVelocity = invFrameDt * (cloth.mTargetMotion.p - cloth.mCurrentMotion.p);
        Vector3f.sub(cloth.mTargetMotion.p, cloth.mCurrentMotion.p, cloth.mLinearVelocity);
        cloth.mLinearVelocity.scale(invFrameDt);
//        physx::PxQuat dq = cloth.mTargetMotion.q * cloth.mCurrentMotion.q.getConjugate();
        Quaternion conjugate = Quaternion.negate(cloth.mCurrentMotion.q, null);
        Quaternion dq= Quaternion.mul(cloth.mTargetMotion.q, conjugate, conjugate);
//        cloth.mAngularVelocity = log(dq) * invFrameDt;
        log(dq, cloth.mAngularVelocity);
        cloth.mAngularVelocity.scale(invFrameDt);

        cloth.mPrevIterDt = mIterDt;
        cloth.mIterDtAvg.push(mNumIterations, mIterDt);
        cloth.mCurrentMotion.set(cloth.mTargetMotion);
    }

    static float fsel(float a, float b, float c)
    {
        return (a >= 0.0f) ? b : c;
    }

    static void log(Quaternion q, Vector3f result){
        float theta = /*q.getImaginaryPart().magnitude()*/Vector3f.length(q);
        float scale = theta > PX_EPS_REAL ? (float) (Math.asin(theta) / theta) : 1.0f;
        scale = fsel(q.w, scale, -scale);
        result.set(q.x * scale, q.y * scale, q.z * scale);
    }

    static void exp(ReadableVector3f v, Quaternion result)
    {
        float theta = Vector3f.length(v);
        float scale = theta > PX_EPS_REAL ? (float) (Math.sin(theta) / theta) : 1.0f;
        result.set(v.getX() * scale, v.getY() * scale, v.getZ() * scale, (float) Math.cos(theta));
    }

    public IterationState create(DxCloth cloth) {
        IterationState result = new IterationState();

        result.mRemainingIterations = mNumIterations;
        result.mInvNumIterations = mInvNumIterations;
        result.mIterDt = mIterDt;

        Vector3f curLinearVelocity = cloth.mLinearVelocity;
        Vector3f prevLinearVelocity = mPrevLinearVelocity;

        float iterDt = mIterDt;
        float dampExponent = cloth.mStiffnessFrequency * iterDt;

        Vector3f translation = Vector3f.scale(curLinearVelocity, iterDt, null);

        // gravity delta per iteration
//        Simd4f gravity = load(array(cloth.mGravity)) * static_cast<Simd4f>(simd4f(sqr(mIterDtAverage)));
        Vector3f gravity = new Vector3f(cloth.mGravity);
        gravity.scale(mIterDtAverage * mIterDtAverage);

        // scale of local particle velocity per iteration
//        Simd4f dampScale = exp2(load(array(cloth.mLogDamping)) * dampExponent);
        Vector3f dampScale = Vector3f.scale(cloth.mLogDamping, dampExponent, null);
        dampScale.x = (float) Math.pow(2, dampScale.x);
        dampScale.y = (float) Math.pow(2, dampScale.y);
        dampScale.z = (float) Math.pow(2, dampScale.z);
        // adjust for the change in time step during the first iteration
//        Simd4f firstDampScale = dampScale * simd4f(mIterDtRatio);
        Vector3f firstDampScale = Vector3f.scale(dampScale, mIterDtRatio, null);

        // portion of negative frame velocity to transfer to particle
//        Simd4f linearDrag = (gSimd4fOne - exp2(load(array(cloth.mLinearLogDrag)) * dampExponent)) * translation;
        Vector3f linearDrag = Vector3f.scale(cloth.mLinearLogDrag, dampExponent, null);
        linearDrag.x = (1.0f - (float) Math.pow(2, linearDrag.x)) * translation.x;
        linearDrag.y = (1.0f - (float) Math.pow(2, linearDrag.y)) * translation.y;
        linearDrag.z = (1.0f - (float) Math.pow(2, linearDrag.z)) * translation.z;

        // portion of frame acceleration to transfer to particle
//        Simd4f linearInertia = load(array(cloth.mLinearInertia)) * iterDt * (prevLinearVelocity - curLinearVelocity);
        Vector3f linearInertia = Vector3f.sub(prevLinearVelocity, curLinearVelocity, null);
        linearInertia.x *= cloth.mLinearInertia.x * iterDt;
        linearInertia.y *= cloth.mLinearInertia.y * iterDt;
        linearInertia.z *= cloth.mLinearInertia.z * iterDt;

        // for inertia, we want to violate newton physics to
        // match velocity and position as given by the user, which means:
        // vt = v0 + a * t and xt = x0 + v0 * t + (!) a * t^2
        // this is achieved by applying a different portion to cur and prev
        // position, compared to the normal +0.5 and -0.5 for '... 1/2 a*t^2'.
        // specifically, the portion is alpha=(n+1)/2n and 1-alpha.

        float linearAlpha = (mNumIterations + 1) * 0.5f * mInvNumIterations;
//        Simd4f curLinearInertia = linearInertia * simd4f(linearAlpha);
        Vector3f curLinearInertia = Vector3f.scale(linearInertia, linearAlpha, null);

        // rotate to local space (use mRotationMatrix temporarily to hold matrix)
//        physx::PxMat44 invRotation = physx::PxMat44(mCurrentRotation.getConjugate());
//        assign(result.mRotationMatrix, invRotation);
        Matrix4f mat4 = new Matrix4f();
        mCurrentRotation.toMatrix(mat4);
        mat4.getColumn(0, result.mRotationMatrix[0]);
        mat4.getColumn(1, result.mRotationMatrix[1]);
        mat4.getColumn(2, result.mRotationMatrix[2]);  // TODO

//        Simd4f maskXYZ = simd4f(simd4i(~0, ~0, ~0, 0));

        // Previously, we split the bias between previous and current position to
        // get correct disretized position and velocity. However, this made a
        // hanging cloth experience a downward velocity, which is problematic
        // when scaled by the iterDt ratio and results in jitter under variable
        // timesteps. Instead, we now apply the entire bias to current position
        // and accept a less noticeable error for a free falling cloth.

        Vector3f bias = Vector3f.sub(gravity, linearDrag, null);
        IterationState.transform(result.mRotationMatrix, Vector3f.add(curLinearInertia, bias, null), result.mCurBias) /*& maskXYZ*/;
        result.mCurBias.w = 0;
        IterationState.transform(result.mRotationMatrix, Vector3f.sub(linearInertia, curLinearInertia, null), result.mPrevBias) /*& maskXYZ*/;
        result.mPrevBias.w = 0;

//        Simd4f wind = load(array(cloth.mWind)) * iterDt; // multiply with delta time here already so we don't have to do it inside the solver
        Vector3f wind = Vector3f.scale(cloth.mWind, iterDt, null);
//        result.mWind = transform(result.mRotationMatrix, translation - wind) & maskXYZ;
        IterationState.transform(result.mRotationMatrix, Vector3f.sub(translation, wind, null), result.mWind);
        result.mWind.w = 0;

        result.mIsTurning = mPrevAngularVelocity.lengthSquared() + cloth.mAngularVelocity.lengthSquared() > 0.0f;

        if (result.mIsTurning) {
//            Simd4f curAngularVelocity = load(array(invRotation.rotate(cloth.mAngularVelocity)));
//            Simd4f prevAngularVelocity = load(array(invRotation.rotate(mPrevAngularVelocity)));
            Vector4f curAngularVelocity = new Vector4f();
            Vector4f prevAngularVelocity = new Vector4f();
            IterationState.transform(result.mRotationMatrix, cloth.mAngularVelocity, curAngularVelocity);
            IterationState.transform(result.mRotationMatrix, mPrevAngularVelocity, prevAngularVelocity);

            // rotation for one iteration in local space
//            Simd4f curInvAngle = -iterDt * curAngularVelocity;
//            Simd4f prevInvAngle = -iterDt * prevAngularVelocity;
            Vector4f curInvAngle = Vector4f.scale(curAngularVelocity, -iterDt, null);
            Vector4f prevInvAngle = Vector4f.scale(prevAngularVelocity, -iterDt, null);

//            physx::PxQuat curInvRotation = exp(castToPxVec3(curInvAngle));
//            physx::PxQuat prevInvRotation = exp(castToPxVec3(prevInvAngle));
            Quaternion curInvRotation = new Quaternion();
            Quaternion prevInvRotation = new Quaternion();
            exp(curInvAngle, curInvRotation);
            exp(prevInvAngle, prevInvRotation);

            /*physx::PxMat44 curMatrix = physx::PxMat44(curInvRotation);
            physx::PxMat44 prevMatrix = physx::PxMat44(prevInvRotation * curInvRotation);

            assign(result.mRotationMatrix, curMatrix);

            Simd4f angularDrag = gSimd4fOne - exp2(load(array(cloth.mAngularLogDrag)) * dampExponent);
            Simd4f centrifugalInertia = load(array(cloth.mCentrifugalInertia));
            Simd4f angularInertia = load(array(cloth.mAngularInertia));
            Simd4f angularAcceleration = curAngularVelocity - prevAngularVelocity;

            Simd4f epsilon = simd4f(sqrtf(FLT_MIN)); // requirement: sqr(epsilon) > 0
            Simd4f velocityLengthSqr = lengthSqr(curAngularVelocity) + epsilon;
            Simd4f dragLengthSqr = lengthSqr(Simd4f(curAngularVelocity * angularDrag)) + epsilon;
            Simd4f centrifugalLengthSqr = lengthSqr(Simd4f(curAngularVelocity * centrifugalInertia)) + epsilon;
            Simd4f accelerationLengthSqr = lengthSqr(angularAcceleration) + epsilon;
            Simd4f inertiaLengthSqr = lengthSqr(Simd4f(angularAcceleration * angularInertia)) + epsilon;

            float dragScale = array(rsqrt(velocityLengthSqr * dragLengthSqr) * dragLengthSqr)[0];
            float inertiaScale =
                    mInvNumIterations * array(rsqrt(accelerationLengthSqr * inertiaLengthSqr) * inertiaLengthSqr)[0];

            // magic factor found by comparing to global space simulation:
            // some centrifugal force is in inertia part, remainder is 2*(n-1)/n
            // after scaling the inertia part, we get for centrifugal:
            float centrifugalAlpha = (2 * mNumIterations - 1) * mInvNumIterations;
            float centrifugalScale =
                    centrifugalAlpha * array(rsqrt(velocityLengthSqr * centrifugalLengthSqr) * centrifugalLengthSqr)[0] -
                            inertiaScale;

            // slightly better in ClothCustomFloating than curInvAngle alone
            Simd4f centrifugalVelocity = (prevInvAngle + curInvAngle) * simd4f(0.5f);
		    const Simd4f data = lengthSqr(centrifugalVelocity);
            float centrifugalSqrLength = array(data)[0] * centrifugalScale;

            Simd4f coriolisVelocity = centrifugalVelocity * simd4f(centrifugalScale);
            physx::PxMat33 coriolisMatrix = physx::shdfnd::star(castToPxVec3(coriolisVelocity));

            const float* dampScalePtr = array(firstDampScale);
            const float* centrifugalPtr = array(centrifugalVelocity);

            for (unsigned int j = 0; j < 3; ++j)
            {
                float centrifugalJ = -centrifugalPtr[j] * centrifugalScale;
                for (unsigned int i = 0; i < 3; ++i)
                {
                    float damping = dampScalePtr[j];
                    float coriolis = coriolisMatrix(i, j);
                    float centrifugal = centrifugalPtr[i] * centrifugalJ;

                    prevMatrix(i, j) = centrifugal - coriolis + curMatrix(i, j) * (inertiaScale - damping) -
                            prevMatrix(i, j) * inertiaScale;
                    curMatrix(i, j) = centrifugal + coriolis + curMatrix(i, j) * (inertiaScale + damping + dragScale);
                }
                curMatrix(j, j) += centrifugalSqrLength - inertiaScale - dragScale;
                prevMatrix(j, j) += centrifugalSqrLength;
            }

            assign(result.mPrevMatrix, prevMatrix);
            assign(result.mCurMatrix, curMatrix);
        }
        else
        {
            Simd4f minusOne = -static_cast<Simd4f>(gSimd4fOne);
            result.mRotationMatrix[0] = minusOne;
            result.mPrevMatrix[0] = select(maskXYZ, firstDampScale, minusOne);
        }

        // difference of damp scale between first and other iterations
        result.mDampScaleUpdate = (dampScale - firstDampScale) & maskXYZ;*/


        }

        return result;
    }
}
