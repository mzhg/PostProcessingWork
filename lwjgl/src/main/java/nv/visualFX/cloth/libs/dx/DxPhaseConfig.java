package nv.visualFX.cloth.libs.dx;

/**
 * Created by mazhen'gui on 2017/9/12.
 */

final class DxPhaseConfig {
    static final int SIZE = 8 * 4;
    float mStiffness;
    float mStiffnessMultiplier;
    float mCompressionLimit;
    float mStretchLimit;

    int mFirstConstraint;
    int mNumConstraints;
}
