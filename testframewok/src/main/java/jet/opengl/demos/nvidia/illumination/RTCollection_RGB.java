package jet.opengl.demos.nvidia.illumination;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.ReadableVector3f;

import jet.opengl.postprocessing.common.Disposeable;

/**
 * Created by Administrator on 2017/11/13 0013.
 */

interface RTCollection_RGB extends Disposeable{

    int Create(int levels, /*ID3D11Device* pd3dDevice,*/ int width2D, int height2D, int width3D, int height3D, int depth3D,
           int format, boolean uav, boolean doublebuffer, boolean use3DTex, boolean use2DTexArray, int numRTs /*= 1*/ );

    void setLPVTransformsRotatedAndOffset(float LPVscale, ReadableVector3f LPVtranslate, Matrix4f cameraViewMatrix, ReadableVector3f viewVector);
    void clearRenderTargetView(/*ID3D11DeviceContext* pd3dContext,*/ float clearColor[], boolean front, int level);

    default SimpleRT getRed(int level){ return getRed(level, true);}
    default SimpleRT getBlue(int level){ return getBlue(level, true);}
    default SimpleRT getGreen(int level){ return getGreen(level, true);}

    int getNumLevels();

    SimpleRT_RGB get(int level);

    SimpleRT getRed(int level, boolean front/*=true*/);
    SimpleRT getBlue(int level, boolean front/*=true*/);
    SimpleRT getGreen(int level, boolean front/*=true*/);

    SimpleRT getRedFront(int level);
    SimpleRT getBlueFront(int level);
    SimpleRT getGreenFront(int level);

    SimpleRT getRedBack(int level);
    SimpleRT getBlueBack(int level);
    SimpleRT getGreenBack(int level);

    int getWidth3D(int level);
    int getHeight3D(int level);
    int getDepth3D(int level);

    int getWidth2D(int level);
    int getHeight2D(int level);

    int getNumCols(int level);
    int getNumRows(int level);
}
