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
    List<VectorKey> mPositionKeys;
    List<VectorKey> mScallingKeys;
    List<QuatKey>   mRotationKeys;

    void interpolate(float time, Transform transform){

    }
}
