package nv.visualFX.cloth.libs;

/**
 * Created by mazhen'gui on 2017/9/12.
 */

public class PhaseConfig {
    //These 4 floats need to be in order as they are loaded in to simd vectors in the solver
    public float mStiffness = 1.0f; // target convergence rate per iteration (1/solverFrequency)
    public float mStiffnessMultiplier = 1.0f;
    public float mCompressionLimit = 1.0f;
    public float mStretchLimit = 1.0f;

    public int mPhaseIndex = -1;
    public int mPadding = 0xffff;

    public PhaseConfig(){}
    public PhaseConfig(int index){
        mPhaseIndex = index;
    }
}
