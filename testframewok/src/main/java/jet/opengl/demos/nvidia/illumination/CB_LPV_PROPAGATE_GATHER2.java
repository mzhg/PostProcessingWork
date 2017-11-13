package jet.opengl.demos.nvidia.illumination;

/**
 * Created by Administrator on 2017/11/13 0013.
 */

final class CB_LPV_PROPAGATE_GATHER2 {
    final PropagateConsts2[] propConsts2 = new PropagateConsts2[36];

    CB_LPV_PROPAGATE_GATHER2(){
        for(int i = 0;i<propConsts2.length; i++){
            propConsts2[i] = new PropagateConsts2();
        }
    }
}
