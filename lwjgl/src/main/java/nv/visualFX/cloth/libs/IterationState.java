package nv.visualFX.cloth.libs;

import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

/**
 * Created by mazhen'gui on 2017/9/13.
 */

public class IterationState {
    public final Vector4f[] mRotationMatrix = new Vector4f[3]; // should rename to 'mRotation'

    public final Vector4f mCurBias = new Vector4f();  // in local space
    public final Vector4f mPrevBias = new Vector4f(); // in local space
    public final Vector4f mWind = new Vector4f();     // delta position per iteration (wind velocity * mIterDt)

    public final Vector4f[] mPrevMatrix = new Vector4f[3];
    public final Vector4f[] mCurMatrix = new Vector4f[3];
    public final Vector4f mDampScaleUpdate = new Vector4f();

    // iteration counter
    public int mRemainingIterations;

    // reciprocal total number of iterations
    public float mInvNumIterations;

    // time step size per iteration
    public float mIterDt;

    public boolean mIsTurning; // if false, mPositionScale = mPrevMatrix[0]

    static void transform(Vector4f[] matrix, Vector4f vec, Vector4f out){
        final int count = Math.min(matrix.length, 4);

        out.set(0,0,0,0);
        for(int i = 0; i < count; i++){
            float v = vec.get(i);
            out.x += matrix[i].x * v;
            out.y += matrix[i].y * v;
            out.z += matrix[i].z * v;
            out.w += matrix[i].w * v;
        }
    }

    public static void transform(Vector4f[] matrix, Vector3f vec, Vector4f out){
        final int count = Math.min(matrix.length, 3);

        out.set(0,0,0, 0);
        for(int i = 0; i < count; i++){
            float v = vec.get(i);
            out.x += matrix[i].x * v;
            out.y += matrix[i].y * v;
            out.z += matrix[i].z * v;
        }
    }

    public void update(){
        if (mIsTurning)
        {
            // only need to turn bias, matrix is unaffected (todo: verify)
            transform(mRotationMatrix, mCurBias, mCurBias);
            transform(mRotationMatrix, mPrevBias, mPrevBias);
            transform(mRotationMatrix, mWind, mWind);
        }

        // remove time step ratio in damp scale after first iteration
        for (int i = 0; i < 3; ++i)
        {
            mPrevMatrix[i].x = mPrevMatrix[i].x - mRotationMatrix[i].x * mDampScaleUpdate.x;
            mPrevMatrix[i].y = mPrevMatrix[i].y - mRotationMatrix[i].y * mDampScaleUpdate.y;
            mPrevMatrix[i].z = mPrevMatrix[i].z - mRotationMatrix[i].z * mDampScaleUpdate.z;
            mPrevMatrix[i].w = mPrevMatrix[i].w - mRotationMatrix[i].w * mDampScaleUpdate.w;

            mCurMatrix[i].x = mCurMatrix[i].x + mRotationMatrix[i].x * mDampScaleUpdate.x;
            mCurMatrix[i].y = mCurMatrix[i].y + mRotationMatrix[i].y * mDampScaleUpdate.y;
            mCurMatrix[i].z = mCurMatrix[i].z + mRotationMatrix[i].z * mDampScaleUpdate.z;
            mCurMatrix[i].w = mCurMatrix[i].w + mRotationMatrix[i].w * mDampScaleUpdate.w;
        }

        mDampScaleUpdate.set(0,0,0,0); // only once

        --mRemainingIterations;
    }

    public float getCurrentAlpha()
    {
        return getPreviousAlpha() + mInvNumIterations;
    }

    public float getPreviousAlpha()
    {
        return 1.0f - mRemainingIterations * mInvNumIterations;
    }
}
