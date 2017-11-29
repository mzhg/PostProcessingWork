package jet.opengl.demos.nvidia.illumination;

import org.lwjgl.util.vector.Vector4f;

/**
 * Created by Administrator on 2017/11/13 0013.
 */

final class PropagateConsts2 {
    static final int SIZE = Vector4f.SIZE * 2;

    int occlusionOffsetX;
    int occlusionOffsetY;
    int occlusionOffsetZ;
    int occlusionOffsetW;

    int multiBounceOffsetX;
    int multiBounceOffsetY;
    int multiBounceOffsetZ;
    int multiBounceOffsetW;
}
