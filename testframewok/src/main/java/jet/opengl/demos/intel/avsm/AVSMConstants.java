package jet.opengl.demos.intel.avsm;

import org.lwjgl.util.vector.Readable;
import org.lwjgl.util.vector.Vector4f;

import java.nio.ByteBuffer;

/**
 * Created by mazhen'gui on 2017/11/1.
 */

final class AVSMConstants implements Readable{
    static final int SIZE = Vector4f.SIZE * 6;
    final Vector4f mMask0 = new Vector4f();
    final Vector4f mMask1 = new Vector4f();
    final Vector4f mMask2 = new Vector4f();
    final Vector4f mMask3 = new Vector4f();
    final Vector4f mMask4 = new Vector4f();
    float       mEmptyNode;
    float       mOpaqueNodeTrans;
    float       mShadowMapSize;

    @Override
    public ByteBuffer store(ByteBuffer buf) {
        mMask0.store(buf);
        mMask1.store(buf);
        mMask2.store(buf);
        mMask3.store(buf);
        mMask4.store(buf);

        buf.putFloat(mEmptyNode);
        buf.putFloat(mOpaqueNodeTrans);
        buf.putFloat(mShadowMapSize);
        buf.putFloat(0);
        return buf;
    }
}
