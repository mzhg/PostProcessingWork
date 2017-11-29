package jet.opengl.demos.nvidia.illumination;

/**
 * Created by Administrator on 2017/11/13 0013.
 */

interface RTCollection_RGB {

    int Create(int levels, /*ID3D11Device* pd3dDevice,*/ int width2D, int height2D, int width3D, int height3D, int depth3D,
           int format, boolean uav, boolean doublebuffer, boolean use3DTex, boolean use2DTexArray, int numRTs /*= 1*/ );
}
