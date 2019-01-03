package jet.opengl.demos.gpupro.rvi;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Readable;
import org.lwjgl.util.vector.Vector4f;

import java.nio.ByteBuffer;

import jet.opengl.postprocessing.util.CacheBuffer;

final class CustomCB implements Readable {
    static final int SIZE = Matrix4f.SIZE*6 + Vector4f.SIZE*5;

    final Matrix4f[] gridViewProjMatrices = new Matrix4f[6];
    final Vector4f gridCellSizes = new Vector4f();
    final Vector4f[] gridPositions = new Vector4f[2];
    final Vector4f[] snappedGridPositions = new Vector4f[2];

    @Override
    public ByteBuffer store(ByteBuffer buf) {
        CacheBuffer.put(buf, gridViewProjMatrices);
        gridCellSizes.store(buf);
        CacheBuffer.put(buf, gridPositions);
        CacheBuffer.put(buf, snappedGridPositions);
        return buf;
    }
}
