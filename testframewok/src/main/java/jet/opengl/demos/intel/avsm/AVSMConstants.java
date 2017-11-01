package jet.opengl.demos.intel.avsm;

import org.lwjgl.util.vector.Vector4f;

/**
 * Created by mazhen'gui on 2017/11/1.
 */

final class AVSMConstants {
    static final int SIZE = Vector4f.SIZE * 6;
    final Vector4f mMask0 = new Vector4f();
    final Vector4f mMask1 = new Vector4f();
    final Vector4f mMask2 = new Vector4f();
    final Vector4f mMask3 = new Vector4f();
    final Vector4f mMask4 = new Vector4f();
    float       mEmptyNode;
    float       mOpaqueNodeTrans;
    float       mShadowMapSize;
}
