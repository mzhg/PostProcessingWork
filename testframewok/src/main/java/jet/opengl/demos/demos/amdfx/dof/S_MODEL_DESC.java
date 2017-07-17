package jet.opengl.demos.demos.amdfx.dof;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Readable;
import org.lwjgl.util.vector.Vector4f;

import java.nio.ByteBuffer;

/**
 * Created by mazhen'gui on 2017/6/26.
 */

final class S_MODEL_DESC implements Readable{
    static final int SIZE = Matrix4f.SIZE * 6 + Vector4f.SIZE * 6;

    final Matrix4f m_World = new Matrix4f();
    final Matrix4f m_World_Inv = new Matrix4f();
    final Matrix4f m_WorldView = new Matrix4f();
    final Matrix4f m_WorldView_Inv = new Matrix4f();
    final Matrix4f m_WorldViewProjection = new Matrix4f();
    final Matrix4f m_WorldViewProjection_Inv = new Matrix4f();

    final Vector4f m_Position = new Vector4f();
    final Vector4f m_Orientation = new Vector4f();
    final Vector4f m_Scale = new Vector4f();
    final Vector4f m_Ambient = new Vector4f();
    final Vector4f m_Diffuse = new Vector4f();
    final Vector4f m_Specular = new Vector4f();

    @Override
    public ByteBuffer store(ByteBuffer buf) {
        m_World.store(buf);
        m_World_Inv.store(buf);
        m_WorldView.store(buf);
        m_WorldView_Inv.store(buf);
        m_WorldViewProjection.store(buf);
        m_WorldViewProjection_Inv.store(buf);

        m_Position.store(buf);
        m_Orientation.store(buf);
        m_Scale.store(buf);
        m_Ambient.store(buf);
        m_Diffuse.store(buf);
        m_Specular.store(buf);

        return buf;
    }
}
