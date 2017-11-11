package jet.opengl.demos.amdfx.shadows;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Readable;
import org.lwjgl.util.vector.Vector4f;

import java.nio.ByteBuffer;

final class S_UNIT_CUBE_TRANSFORM implements Readable{
	static final int SIZE = 208;

	final Matrix4f    m_Transform = new Matrix4f();
	final Matrix4f    m_Inverse   = new Matrix4f();
	final Matrix4f    m_Forward   = new Matrix4f();
    final Vector4f    m_Color     = new Vector4f();
    
	@Override
	public ByteBuffer store(ByteBuffer buf) {
		m_Transform.store(buf);
		m_Inverse.store(buf);
		m_Forward.store(buf);
		m_Color.store(buf);
		return buf;
	}
}
