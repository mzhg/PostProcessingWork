package intel.avsm;

/**
 * The Particle structure contains per-particle state required to simulate the behavior<p></p>
 * Created by mazhen'gui on 2017/9/29.
 */

final class Particle {
    final float[] mpPos = new float[3];
    final float[] mpVelocity = new float[3];
    float mRemainingLife;
    float mSize;
    float mOpacity;
    float mSortDistance;
    int  mEmitterIdx;
}
