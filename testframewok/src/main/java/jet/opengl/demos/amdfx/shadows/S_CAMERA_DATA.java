package jet.opengl.demos.amdfx.shadows;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Readable;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector4f;

import java.nio.ByteBuffer;

final class S_CAMERA_DATA implements Readable{
	static final int SIZE = 544;
	
	final Matrix4f    m_View = new Matrix4f();
	final Matrix4f    m_Projection = new Matrix4f();
	final Matrix4f    m_ViewInv = new Matrix4f();
	final Matrix4f    m_ProjectionInv = new Matrix4f();
	final Matrix4f    m_ViewProjection = new Matrix4f();
    final Matrix4f    m_ViewProjectionInv = new Matrix4f();

    final Vector2f    m_BackBufferDim = new Vector2f();
    final Vector2f    m_BackBufferDimRcp = new Vector2f();
    final Vector4f    m_Color = new Vector4f();

    final Vector4f    m_Position = new Vector4f();
    final Vector4f    m_Direction = new Vector4f();
    final Vector4f    m_Up = new Vector4f();
    float       	  m_Fov;
    float       	  m_Aspect;
    float             m_zNear;
    float       	  m_zFar;

    final Vector4f    m_Parameter0 = new Vector4f();
    final Vector4f    m_Parameter1 = new Vector4f();
    final Vector4f    m_Parameter2 = new Vector4f();
    final Vector4f    m_Parameter3 = new Vector4f();
    
	@Override
	public ByteBuffer store(ByteBuffer buf) {
		m_View.store(buf);
		m_Projection.store(buf);
		m_ViewInv.store(buf);
		m_ProjectionInv.store(buf);
		m_ViewProjection.store(buf);
		m_ViewProjectionInv.store(buf);
		m_BackBufferDim.store(buf);
		m_BackBufferDimRcp.store(buf);
		m_Color.store(buf);
		m_Position.store(buf);
		m_Direction.store(buf);
		m_Up.store(buf);
		buf.putFloat(m_Fov);
		buf.putFloat(m_Aspect);
		buf.putFloat(m_zNear);
		buf.putFloat(m_zFar);
		m_Parameter0.store(buf);
		m_Parameter1.store(buf);
		m_Parameter2.store(buf);
		m_Parameter3.store(buf);
		return buf;
	}
}
