package jet.opengl.demos.nvidia.lightning;

import org.lwjgl.util.vector.Readable;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import java.nio.ByteBuffer;

import jet.opengl.postprocessing.util.CacheBuffer;

/**
 * Created by mazhen'gui on 2017/8/28.
 */

final class ChainLightningProperties implements Readable{
    static final int MaxTargets = 8;
    static final int SIZE = Vector4f.SIZE + Vector4f.SIZE * MaxTargets + Vector4f.SIZE;

    final Vector3f ChainSource = new Vector3f();

    final Vector4f[] ChainTargetPositions = new Vector4f[MaxTargets];
    int			NumTargets;


    @Override
    public ByteBuffer store(ByteBuffer buf) {
        ChainSource.store(buf);
        buf.putFloat(0);
        CacheBuffer.put(buf, ChainTargetPositions);
        buf.putFloat(NumTargets);
        buf.putFloat(0);
        buf.putFloat(0);
        buf.putFloat(0);

        return buf;
    }
}
