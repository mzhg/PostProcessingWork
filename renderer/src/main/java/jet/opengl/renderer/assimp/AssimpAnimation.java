package jet.opengl.renderer.assimp;

import org.lwjgl.util.vector.Transform;

import java.util.List;

public class AssimpAnimation {
    private int mLastPositionIndex;
    private int mLastScallingIndex;
    private int mLastRotationIndex;

    float mDuration;
    float mTicksPerSecond;

    int mBoneName;
    VectorKey[] mPositionKeys;
    VectorKey[] mScalingKeys;
    QuatKey[]   mRotationKeys;

    void interpolate(float time, Transform transform){

    }
}
