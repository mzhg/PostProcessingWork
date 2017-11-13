package jet.opengl.demos.nvidia.illumination;

import org.lwjgl.util.vector.Vector4f;

/**
 * Created by Administrator on 2017/11/13 0013.
 */

final class CB_VS_GLOBAL {
    final Vector4f g_lightWorldPos = new Vector4f();
    float g_depthBiasFromGUI;
    int bUseSM;
    int g_minCascadeMethod;
    int g_numCascadeLevels;
}
