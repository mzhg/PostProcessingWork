package intel.avsm;

/**
 * Created by mazhen'gui on 2017/9/29.
 */

final class ParticleEmitter {
    final float[] mpPos = new float[3];
    final float[] mpVelocity = new float[3]; // Velocity of emitted particles, not the change in emitter position/time
    float mDrag = 4.0f;
    float mGravity = -5.0f;
    float mLifetime = 0.2f;
    float mStartSize = 0.03f;
    float mSizeRate = 4.0f;
    float mRandScaleX = 1.0f;
    float mRandScaleY = 5.0f;
    float mRandScaleZ = 1.0f;

    ParticleEmitter(){
        mpPos[0] = 0.0f;
        mpPos[1] = 0.0f;
        mpPos[2] = 0.1f;

        mpVelocity[0] = 0.0f;
        mpVelocity[1] = 2.0f;
        mpVelocity[2] = 0.0f;
    }
}
