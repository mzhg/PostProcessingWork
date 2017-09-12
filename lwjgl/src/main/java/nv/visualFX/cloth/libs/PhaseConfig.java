package nv.visualFX.cloth.libs;

/**
 * Created by mazhen'gui on 2017/9/12.
 */

public class PhaseConfig {
    //These 4 floats need to be in order as they are loaded in to simd vectors in the solver
    float mStiffness = 1.0f; // target convergence rate per iteration (1/solverFrequency)
    float mStiffnessMultiplier = 1.0f;
    float mCompressionLimit = 1.0f;
    float mStretchLimit = 1.0f;

    int mPhaseIndex = -1;
    int mPadding = 0xffff;
}
