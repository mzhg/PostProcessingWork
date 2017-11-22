package jet.opengl.demos.nvidia.illumination;

import org.lwjgl.util.vector.Readable;
import org.lwjgl.util.vector.Vector4f;

import java.nio.ByteBuffer;

/**
 * Created by Administrator on 2017/11/13 0013.
 */

final class CB_VS_GLOBAL implements Readable{
    static final int SIZE = Vector4f.SIZE * 3;

    final Vector4f g_lightWorldPos = new Vector4f();
    float g_depthBiasFromGUI;
    int bUseSM;
    int g_minCascadeMethod;
    int g_numCascadeLevels;

    @Override
    public ByteBuffer store(ByteBuffer buf) {
        g_lightWorldPos.store(buf);
        buf.putFloat(g_depthBiasFromGUI);
        buf.putFloat(0);
        buf.putFloat(0);
        buf.putFloat(0);

        buf.putInt(bUseSM);
        buf.putInt(g_minCascadeMethod);
        buf.putInt(g_numCascadeLevels);
        buf.putInt(0);

        return buf;
    }
}
