package jet.opengl.demos.gpupro.clustered;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Readable;
import org.lwjgl.util.vector.Vector4f;

import java.nio.ByteBuffer;

import jet.opengl.postprocessing.util.CacheBuffer;

final class ScreenToView implements Readable {
    static final int SIZE = Matrix4f.SIZE + Vector4f.SIZE * 2;

    final Matrix4f inverseProjectionMat = new Matrix4f();
    final int[] tileSizes = new int[4];
    int screenWidth;
    int screenHeight;
    float sliceScalingFactor;
    float sliceBiasFactor;

    @Override
    public ByteBuffer store(ByteBuffer buf) {
        inverseProjectionMat.store(buf);
        CacheBuffer.put(buf, tileSizes);
        buf.putInt(screenWidth);
        buf.putInt(screenHeight);
        buf.putFloat(sliceScalingFactor);
        buf.putFloat(sliceBiasFactor);
        return buf;
    }
}
