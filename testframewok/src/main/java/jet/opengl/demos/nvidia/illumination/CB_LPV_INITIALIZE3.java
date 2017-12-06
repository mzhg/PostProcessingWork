package jet.opengl.demos.nvidia.illumination;

import org.lwjgl.util.vector.Readable;
import org.lwjgl.util.vector.Vector4f;
import org.lwjgl.util.vector.Writable;

import java.nio.ByteBuffer;

/**
 * Created by Administrator on 2017/11/13 0013.
 */

final class CB_LPV_INITIALIZE3 implements Readable{
    static final int SIZE = Vector4f.SIZE * 3;
    int g_numCols;    //the number of columns in the flattened 2D LPV
    int g_numRows;    //the number of columns in the flattened 2D LPV
    int LPV2DWidth; //the total width of the flattened 2D LPV
    int LPV2DHeight; //the total height of the flattened 2D LPV

    int LPV3DWidth;    //the width of the LPV in 3D
    int LPV3DHeight;   //the height of the LPV in 3D
    int LPV3DDepth;

    int useFluxWeight; //flux weight only needed for perspective light matrix not orthogonal

    float fluxWeight;

    @Override
    public ByteBuffer store(ByteBuffer buf) {
        buf.putInt(g_numCols);
        buf.putInt(g_numRows);
        buf.putInt(LPV2DWidth);
        buf.putInt(LPV2DHeight);

        buf.putInt(LPV3DWidth);
        buf.putInt(LPV3DHeight);
        buf.putInt(LPV3DDepth);
        buf.putInt(useFluxWeight);

        buf.putFloat(fluxWeight);
        buf.position(buf.position() + 12);
        return buf;
    }
    /*float padding0;
    float padding1;
    float padding2;*/
}
