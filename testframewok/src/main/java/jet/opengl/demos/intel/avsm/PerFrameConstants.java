package jet.opengl.demos.intel.avsm;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector4f;

/**
 * Created by mazhen'gui on 2017/11/1.
 */

final class PerFrameConstants {
    static final int SIZE = Matrix4f.SIZE * 4 + Vector4f.SIZE + Matrix4f.SIZE * 6 + Vector4f.SIZE * 3 + UIConstants.SIZE;
    final Matrix4f mCameraWorldViewProj = new Matrix4f();
    final Matrix4f  mCameraWorldView = new Matrix4f();
    final Matrix4f  mCameraViewProj = new Matrix4f();
    final Matrix4f  mCameraProj = new Matrix4f();
    final Vector4f mCameraPos = new Vector4f();
    final Matrix4f  mLightWorldViewProj = new Matrix4f();
    final Matrix4f  mAvsmLightWorldViewProj = new Matrix4f();
    final Matrix4f  mCameraViewToLightProj = new Matrix4f();
    final Matrix4f  mCameraViewToLightView = new Matrix4f();
    final Matrix4f  mCameraViewToAvsmLightProj = new Matrix4f();
    final Matrix4f  mCameraViewToAvsmLightView = new Matrix4f();
    final Vector4f mLightDir = new Vector4f();
    final Vector4f mScreenResolution = new Vector4f();
    final Vector4f mScreenToViewConsts = new Vector4f();

    final UIConstants mUI = new UIConstants();
}
