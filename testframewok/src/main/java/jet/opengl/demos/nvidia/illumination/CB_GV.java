package jet.opengl.demos.nvidia.illumination;

import org.lwjgl.util.vector.Readable;
import org.lwjgl.util.vector.Vector4f;

import java.nio.ByteBuffer;

/**
 * Created by Administrator on 2017/11/13 0013.
 */

final class CB_GV implements Readable{
    static final int SIZE = Vector4f.SIZE * 2;

    int useGVOcclusion;
    int temp;
    int useMultipleBounces;
    int copyPropToAccum; // in addition to adding the newly propagated values to the accumulation buffer also add in the original value of the LPV. This is only done for the first propagation iteration, to allow the accumulation buffer to have the original VPLs

    float fluxAmplifier;
    float reflectedLightAmplifier;
    float occlusionAmplifier;
    float temp2;

    @Override
    public ByteBuffer store(ByteBuffer buf) {
        buf.putInt(useGVOcclusion);
        buf.putInt(temp);
        buf.putInt(useMultipleBounces);
        buf.putInt(copyPropToAccum);

        buf.putFloat(fluxAmplifier);
        buf.putFloat(reflectedLightAmplifier);
        buf.putFloat(occlusionAmplifier);
        buf.putFloat(temp2);
        return buf;
    }
}
