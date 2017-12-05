package jet.opengl.demos.nvidia.illumination;

import org.lwjgl.util.vector.Readable;
import org.lwjgl.util.vector.Vector4f;

import java.nio.ByteBuffer;

/**
 * Created by Administrator on 2017/11/13 0013.
 */

final class CB_RENDER_LPV implements Readable{
    static final int SIZE = Vector4f.SIZE * 2;
    int g_numCols;    //the number of columns in the flattened 2D LPV
    int g_numRows;    //the number of columns in the flattened 2D LPV
    int LPV2DWidth; //the total width of the flattened 2D LPV
    int LPV2DHeight; //the total height of the flattened 2D LPV

    int LPV3DWidth;    //the width of the LPV in 3D
    int LPV3DHeight;   //the height of the LPV in 3D
    int LPV3DDepth;

    @Override
    public ByteBuffer store(ByteBuffer buf) {
        buf.putInt(g_numCols);
        buf.putInt(g_numRows);
        buf.putInt(LPV2DWidth);
        buf.putInt(LPV2DHeight);

        buf.putInt(LPV3DWidth);
        buf.putInt(LPV3DHeight);
        buf.putInt(LPV3DDepth);
        buf.putInt(LPV3DWidth);
        return buf;
    }
//    int padding;
}
