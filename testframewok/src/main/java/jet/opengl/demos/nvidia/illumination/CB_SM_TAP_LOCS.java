package jet.opengl.demos.nvidia.illumination;

import org.lwjgl.util.vector.Vector4f;

/**
 * Created by Administrator on 2017/11/13 0013.
 */

final class CB_SM_TAP_LOCS {
    int numTaps;
    float filterSize;
//    float padding7;
//    float padding8;
    final Vector4f[] samples = new Vector4f[Defines.MAX_P_SAMPLES];

    CB_SM_TAP_LOCS(){
        for(int i = 0; i < samples.length; i++){
            samples[i] = new Vector4f();
        }
    }
}
