package jet.opengl.demos.amdfx.shadows;

import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector4f;

final class LightData {

	final Camera                         m_Camera = new Camera();
    final Vector2f                       m_Size = new Vector2f();
    final Vector2f                       m_SizeInv = new Vector2f();
    final Vector4f                       m_Region = new Vector4f();

    final Vector4f                       m_Weight = new Vector4f();

    float                                m_SunArea;
    float                                m_DepthTestOffset;
    float                                m_NormalOffsetScale;
    int                                  m_ArraySlice;
}
