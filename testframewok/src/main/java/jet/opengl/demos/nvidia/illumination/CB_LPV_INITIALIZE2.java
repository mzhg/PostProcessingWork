package jet.opengl.demos.nvidia.illumination;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Readable;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import java.nio.ByteBuffer;

/**
 * Created by Administrator on 2017/11/13 0013.
 */

final class CB_LPV_INITIALIZE2 implements Readable{
    static final int SIZE = Matrix4f.SIZE * 2 + Vector4f.SIZE;

    final Matrix4f g_ViewToLPV = new Matrix4f();
    final Matrix4f g_LPVtoView = new Matrix4f();
    final Vector3f lightDirGridSpace = new Vector3f();   //light direction in the grid's space
    float displacement;

    @Override
    public ByteBuffer store(ByteBuffer buf) {
        g_ViewToLPV.store(buf);
        g_LPVtoView.store(buf);
        lightDirGridSpace.store(buf);
        buf.putFloat(displacement);
        return buf;
    }
}
