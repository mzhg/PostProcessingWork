package jet.opengl.demos.nvidia.illumination;

import jet.opengl.postprocessing.texture.TextureGL;

/**
 * Created by Administrator on 2017/11/23 0023.
 */

interface RenderTarget {
    int getNumRTs();

    TextureGL get_pSRV(int index);

    boolean is2DTexture();
    int getWidth();
    int getHeight();
    int getDepth();
}
