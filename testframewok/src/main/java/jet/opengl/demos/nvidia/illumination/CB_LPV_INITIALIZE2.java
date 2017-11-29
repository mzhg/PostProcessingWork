package jet.opengl.demos.nvidia.illumination;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

/**
 * Created by Administrator on 2017/11/13 0013.
 */

final class CB_LPV_INITIALIZE2 {
    static final int SIZE = Matrix4f.SIZE * 2 + Vector4f.SIZE;

    final Matrix4f g_ViewToLPV = new Matrix4f();
    final Matrix4f g_LPVtoView = new Matrix4f();
    final Vector3f lightDirGridSpace = new Vector3f();   //light direction in the grid's space
    float displacement;
}
