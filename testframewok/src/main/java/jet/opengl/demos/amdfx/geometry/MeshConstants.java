package jet.opengl.demos.amdfx.geometry;

import org.lwjgl.util.vector.Readable;
import org.lwjgl.util.vector.Vector4f;

import java.nio.ByteBuffer;

final class MeshConstants implements Readable {
    static final int SIZE = Vector4f.SIZE;

    int vertexCount;
    int faceCount;
    int indexOffset;
    int vertexOffset;

    @Override
    public ByteBuffer store(ByteBuffer buf) {
        buf.putInt(vertexCount);
        buf.putInt(faceCount);
        buf.putInt(indexOffset);
        buf.putInt(vertexOffset);
        return buf;
    }
}
