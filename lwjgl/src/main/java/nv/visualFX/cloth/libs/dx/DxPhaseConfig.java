package nv.visualFX.cloth.libs.dx;

import org.lwjgl.util.vector.Readable;

import java.nio.ByteBuffer;

/**
 * Created by mazhen'gui on 2017/9/12.
 */

final class DxPhaseConfig implements Readable{
    static final int SIZE = 8 * 4;
    float mStiffness;
    float mStiffnessMultiplier;
    float mCompressionLimit;
    float mStretchLimit;

    int mFirstConstraint;
    int mNumConstraints;


    @Override
    public ByteBuffer store(ByteBuffer buf) {
        buf.putFloat(mStiffness);
        buf.putFloat(mStiffnessMultiplier);
        buf.putFloat(mCompressionLimit);
        buf.putFloat(mStretchLimit);

        buf.putInt(mFirstConstraint);
        buf.putInt(mNumConstraints);
        buf.putInt(0);
        buf.putInt(0);
        return buf;
    }
}
