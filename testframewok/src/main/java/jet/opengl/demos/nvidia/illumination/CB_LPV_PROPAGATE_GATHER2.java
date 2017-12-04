package jet.opengl.demos.nvidia.illumination;

import org.lwjgl.util.vector.Readable;

import java.nio.ByteBuffer;

/**
 * Created by Administrator on 2017/11/13 0013.
 */

final class CB_LPV_PROPAGATE_GATHER2 implements Readable{
    static final int SIZE = PropagateConsts2.SIZE * 36;

    final PropagateConsts2[] propConsts2 = new PropagateConsts2[36];

    CB_LPV_PROPAGATE_GATHER2(){
        for(int i = 0;i<propConsts2.length; i++){
            propConsts2[i] = new PropagateConsts2();
        }
    }

    @Override
    public ByteBuffer store(ByteBuffer buf) {
        for(int i = 0; i < propConsts2.length; i++){
            PropagateConsts2 consts2 = propConsts2[i];
            buf.putInt(consts2.occlusionOffsetX);
            buf.putInt(consts2.occlusionOffsetY);
            buf.putInt(consts2.occlusionOffsetZ);
            buf.putInt(consts2.occlusionOffsetW);

            buf.putInt(consts2.multiBounceOffsetX);
            buf.putInt(consts2.multiBounceOffsetY);
            buf.putInt(consts2.multiBounceOffsetZ);
            buf.putInt(consts2.multiBounceOffsetW);
        }
        return buf;
    }
}
