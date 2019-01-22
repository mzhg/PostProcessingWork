package jet.opengl.demos.amdfx.geometry;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Readable;
import org.lwjgl.util.vector.Vector4f;

import java.nio.ByteBuffer;

final class FrameConstantBuffer implements Readable {
    static final int SIZE = Matrix4f.SIZE * 2 + Vector4f.SIZE;

    final Matrix4f view = new Matrix4f();
    final Matrix4f projection = new Matrix4f();
    int cullFlags;
    int width, height;

    @Override
    public ByteBuffer store(ByteBuffer buf) {
        view.store(buf);
        projection.store(buf);
        buf.putInt(cullFlags);
        buf.putInt(width);
        buf.putInt(height);
        buf.putInt(0);
        return buf;
    }
}
