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
}
