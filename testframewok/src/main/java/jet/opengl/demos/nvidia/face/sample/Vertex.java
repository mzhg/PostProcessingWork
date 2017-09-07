package jet.opengl.demos.nvidia.face.sample;

import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;

/**
 * Created by mazhen'gui on 2017/9/5.
 */

final class Vertex {
    static final int SIZE = Vector3f.SIZE * 4;
    final Vector3f m_pos = new Vector3f();
    final Vector3f m_normal = new Vector3f();
    final Vector2f m_uv = new Vector2f();
    final Vector3f m_tangent = new Vector3f();
    float m_curvature;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Vertex vertex = (Vertex) o;

        if (Float.compare(vertex.m_curvature, m_curvature) != 0) return false;
        if (!m_pos.equals(vertex.m_pos)) return false;
        if (!m_normal.equals(vertex.m_normal)) return false;
        if (!m_uv.equals(vertex.m_uv)) return false;
        return m_tangent.equals(vertex.m_tangent);

    }

    @Override
    public int hashCode() {
        int result = m_pos != null ? m_pos.hashCode() : 0;
        result = 31 * result + (m_normal != null ? m_normal.hashCode() : 0);
        result = 31 * result + (m_uv != null ? m_uv.hashCode() : 0);
        result = 31 * result + (m_tangent != null ? m_tangent.hashCode() : 0);
        result = 31 * result + (m_curvature != +0.0f ? Float.floatToIntBits(m_curvature) : 0);
        return result;
    }
}
