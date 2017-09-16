package nv.visualFX.cloth.libs;

import org.lwjgl.util.vector.ReadableVector3f;
import org.lwjgl.util.vector.Vector3f;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * Created by mazhen'gui on 2017/9/13.
 */

public class SwInterCollisionData {
    public FloatBuffer mParticles;
    public FloatBuffer mPrevParticles;
    public int mNumParticles;
    public IntBuffer mIndices;
    public final PxTransform mGlobalPose = new PxTransform();
    public final Vector3f mBoundsCenter = new Vector3f();
    public final Vector3f mBoundsHalfExtent=new Vector3f();
    public float mImpulseScale;
    public Object mUserData;

    public SwInterCollisionData() {}

    public SwInterCollisionData(FloatBuffer particles, FloatBuffer prevParticles, int numParticles, IntBuffer indices,
                                PxTransform globalPose, ReadableVector3f boundsCenter, ReadableVector3f boundsHalfExtents,
                                float impulseScale, Object userData){
        mParticles = particles;
        mPrevParticles = prevParticles;
        mNumParticles = numParticles;
        mIndices = indices;
        mGlobalPose.set(globalPose);
        mBoundsCenter.set(boundsCenter);
        mBoundsHalfExtent.set(boundsHalfExtents);
        mImpulseScale = impulseScale;
        mUserData = userData;
    }

}
