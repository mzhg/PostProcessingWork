package jet.opengl.demos.nvidia.illumination;

import org.lwjgl.util.vector.Vector4f;

/**
 * Created by Administrator on 2017/11/13 0013.
 */

final class CB_LPV_PROPAGATE {
    static final int SIZE = Vector4f.SIZE * 2;
    int g_numCols;    //the number of columns in the flattened 2D LPV
    int g_numRows;    //the number of columns in the flattened 2D LPV
    int LPV2DWidth; //the total width of the flattened 2D LPV
    int LPV2DHeight; //the total height of the flattened 2D LPV

    int LPV3DWidth;    //the width of the LPV in 3D
    int LPV3DHeight;   //the height of the LPV in 3D
    int LPV3DDepth;
    int b_accumulate;
}
