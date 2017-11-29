package jet.opengl.demos.nvidia.illumination;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector4f;

/**
 * Created by Administrator on 2017/11/13 0013.
 */

final class CB_LPV_INITIALIZE {
    static final int SIZE = Vector4f.SIZE + Matrix4f.SIZE;
    int RSMWidth;
    int RSMHeight;
    float lightStrength;
    float temp;

    final Matrix4f InvProj = new Matrix4f();
}
