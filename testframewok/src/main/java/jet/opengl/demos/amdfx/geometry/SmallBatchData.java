package jet.opengl.demos.amdfx.geometry;

import org.lwjgl.util.vector.Readable;

import java.nio.ByteBuffer;

final class SmallBatchData implements Readable {
    static final int SIZE = 6 * 4;
    int meshIndex;         // Index into meshConstants
    int indexOffset;       // Index relative to the meshConstants[meshIndex].indexOffset
    int faceCount;         // Number of faces in this small batch
    int outputIndexOffset; // Offset into the output index buffer
    int drawIndex;         // Index into the SmallBatchDrawCallTable
    int drawBatchStart;    // First slot for the current draw call

    @Override
    public ByteBuffer store(ByteBuffer buf) {
        buf.putInt(meshIndex);
        buf.putInt(indexOffset);
        buf.putInt(faceCount);
        buf.putInt(outputIndexOffset);
        buf.putInt(drawIndex);
        buf.putInt(drawBatchStart);
        return buf;
    }
}
