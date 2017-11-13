package jet.opengl.demos.nvidia.illumination;

/**
 * Created by Administrator on 2017/11/13 0013.
 */

final class CB_LPV_PROPAGATE_GATHER {
    final PropagateConsts[] propConsts = new PropagateConsts[36];

    CB_LPV_PROPAGATE_GATHER(){
        for(int i = 0; i < propConsts.length; i++)
            propConsts[i] = new PropagateConsts();
    }
}
