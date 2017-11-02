package jet.opengl.demos.intel.avsm;

import org.lwjgl.util.vector.Readable;

import java.nio.ByteBuffer;

/**
 * Created by mazhen'gui on 2017/11/1.
 */

final class LT_Constants implements Readable{
    static final int SIZE = 16;
    int   mMaxNodes;
    float mFirstNodeMapSize;

    @Override
    public ByteBuffer store(ByteBuffer buf) {
        buf.putInt(mMaxNodes);
        buf.putFloat(mFirstNodeMapSize);
        buf.putInt(0);
        buf.putInt(0);
        return buf;
    }
}
