package jet.opengl.demos.amdfx.shadows;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Readable;
import org.lwjgl.util.vector.Vector4f;

import java.nio.ByteBuffer;

final class S_MODEL_DATA implements Readable{
	static final int SIZE = 240;
	
	final Matrix4f    m_World = new Matrix4f();
	final Matrix4f    m_WorldViewProjection = new Matrix4f();
	final Matrix4f    m_WorldViewProjectionLight = new Matrix4f();
    final Vector4f    m_Diffuse = new Vector4f();
    final Vector4f    m_Ambient = new Vector4f();
    final Vector4f    m_Parameter0 = new Vector4f();
	@Override
	public ByteBuffer store(ByteBuffer buf) {
		m_World.store(buf);
		m_WorldViewProjection.store(buf);
		m_WorldViewProjectionLight.store(buf);
		m_Diffuse.store(buf);
		m_Ambient.store(buf);
		m_Parameter0.store(buf);
		return buf;
	}
}
