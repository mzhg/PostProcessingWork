package jet.opengl.demos.nvidia.illumination;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Readable;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;

/**
 * Created by Administrator on 2017/11/13 0013.
 */

final class CB_VS_PER_OBJECT implements Readable{
    static final int SIZE = Matrix4f.SIZE * 4;
    final Matrix4f m_WorldViewProj = new Matrix4f();
    final Matrix4f m_WorldViewIT = new Matrix4f();
    final Matrix4f m_World = new Matrix4f();
    final Matrix4f m_LightViewProjClip2Tex = new Matrix4f();


    @Override
    public ByteBuffer store(ByteBuffer buf) {
        m_WorldViewProj.store(buf);
        m_WorldViewIT.store(buf);
        m_World.store(buf);
        m_LightViewProjClip2Tex.store(buf);
        return buf;
    }
}
