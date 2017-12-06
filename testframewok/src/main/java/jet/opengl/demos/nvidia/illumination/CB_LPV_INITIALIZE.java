package jet.opengl.demos.nvidia.illumination;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Readable;
import org.lwjgl.util.vector.Vector4f;

import java.nio.ByteBuffer;

/**
 * Created by Administrator on 2017/11/13 0013.
 */

final class CB_LPV_INITIALIZE implements Readable{
    static final int SIZE = Vector4f.SIZE + Matrix4f.SIZE;
    int RSMWidth;
    int RSMHeight;
    float lightStrength;
    float temp;

    final Matrix4f InvProj = new Matrix4f();

    @Override
    public ByteBuffer store(ByteBuffer buf) {
        buf.putInt(RSMWidth);
        buf.putInt(RSMHeight);
        buf.putFloat(lightStrength);
        buf.putFloat(temp);

        InvProj.store(buf);
        return buf;
    }
}
