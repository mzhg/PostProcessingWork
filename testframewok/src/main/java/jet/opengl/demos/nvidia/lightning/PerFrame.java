package jet.opengl.demos.nvidia.lightning;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector4f;

/**
 * Created by mazhen'gui on 2017/8/30.
 */

final class PerFrame {
    static final int SIZE = Matrix4f.SIZE + Vector4f.SIZE;

    final Matrix4f world = new Matrix4f();
    final Matrix4f view = new Matrix4f();
    final Matrix4f projection = new Matrix4f();

    final Matrix4f world_view = new Matrix4f();
    final Matrix4f world_view_projection = new Matrix4f();

    float time;
}
