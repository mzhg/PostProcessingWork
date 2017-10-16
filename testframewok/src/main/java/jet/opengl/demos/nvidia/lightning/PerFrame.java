package jet.opengl.demos.nvidia.lightning;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Readable;
import org.lwjgl.util.vector.Vector4f;

import java.nio.ByteBuffer;

/**
 * Created by mazhen'gui on 2017/8/30.
 */

final class PerFrame implements Readable{
    static final int SIZE = Matrix4f.SIZE * 5 + Vector4f.SIZE;

    final Matrix4f world = new Matrix4f();
    final Matrix4f view = new Matrix4f();
    final Matrix4f projection = new Matrix4f();

    final Matrix4f world_view = new Matrix4f();
    final Matrix4f world_view_projection = new Matrix4f();

    float time;

    @Override
    public ByteBuffer store(ByteBuffer buf) {
        world.store(buf);
        view.store(buf);
        projection.store(buf);
        world_view.store(buf);
        world_view_projection.store(buf);
        buf.putFloat(time);
        buf.putInt(0);
        buf.putInt(0);
        buf.putInt(0);
        return buf;
    }
}
