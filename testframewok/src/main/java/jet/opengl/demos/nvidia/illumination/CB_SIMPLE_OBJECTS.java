package jet.opengl.demos.nvidia.illumination;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Readable;
import org.lwjgl.util.vector.Vector4f;

import java.nio.ByteBuffer;

/**
 * Created by Administrator on 2017/11/13 0013.
 */

final class CB_SIMPLE_OBJECTS implements Readable{
    static final int SIZE = Matrix4f.SIZE + Vector4f.SIZE * 3;

    final Matrix4f m_WorldViewProj = new Matrix4f();
    final Vector4f m_color = new Vector4f();
    final Vector4f m_lpvScale = new Vector4f();
    final Vector4f m_sphereScale = new Vector4f();

    @Override
    public ByteBuffer store(ByteBuffer buf) {
        m_WorldViewProj.store(buf);
        m_color.store(buf);
        m_lpvScale.store(buf);
        m_sphereScale.store(buf);
        return buf;
    }
}
