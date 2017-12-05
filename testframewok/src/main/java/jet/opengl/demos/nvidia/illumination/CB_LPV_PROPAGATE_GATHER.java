package jet.opengl.demos.nvidia.illumination;

import org.lwjgl.util.vector.Readable;

import java.nio.ByteBuffer;

/**
 * Created by Administrator on 2017/11/13 0013.
 */

final class CB_LPV_PROPAGATE_GATHER implements Readable{
    static final int SIZE = PropagateConsts.SIZE * 36;

    final PropagateConsts[] propConsts = new PropagateConsts[36];

    CB_LPV_PROPAGATE_GATHER(){
        for(int i = 0; i < propConsts.length; i++)
            propConsts[i] = new PropagateConsts();
    }

    @Override
    public ByteBuffer store(ByteBuffer buf) {
        for(int i = 0; i < 36; i++){
            PropagateConsts consts = propConsts[i];
            consts.neighborOffset.store(buf);
            buf.putFloat(consts.solidAngle);
            buf.putFloat(consts.x);
            buf.putFloat(consts.y);
            buf.putFloat(consts.z);
        }
        return buf;
    }
}
