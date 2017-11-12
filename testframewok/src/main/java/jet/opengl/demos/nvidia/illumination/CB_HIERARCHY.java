package jet.opengl.demos.nvidia.illumination;

import org.lwjgl.util.vector.Readable;
import org.lwjgl.util.vector.Vector4f;

import java.nio.ByteBuffer;

/**
 * Created by Administrator on 2017/11/12 0012.
 */

final class CB_HIERARCHY implements Readable{
    static final int SIZE = Vector4f.SIZE * 3;
    int finerLevelWidth;
    int finerLevelHeight;
    int finerLevelDepth;
    int g_numColsFiner;

    int g_numRowsFiner;
    int g_numColsCoarser;
    int g_numRowsCoarser;
    int corserLevelWidth;

    int corserLevelHeight;
    int corserLevelDepth;

    @Override
    public ByteBuffer store(ByteBuffer buf) {
        buf.putInt(finerLevelWidth);
        buf.putInt(finerLevelHeight);
        buf.putInt(finerLevelDepth);
        buf.putInt(g_numColsFiner);

        buf.putInt(g_numRowsFiner);
        buf.putInt(g_numColsCoarser);
        buf.putInt(g_numRowsCoarser);
        buf.putInt(corserLevelWidth);

        buf.putInt(corserLevelHeight);
        buf.putInt(corserLevelDepth);
        buf.putInt(0);
        buf.putInt(0);

        return buf;
    }
}
