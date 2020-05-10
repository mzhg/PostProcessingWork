package nv.samples.culling;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector4f;

final class View {
    static final int SIZE = Matrix4f.SIZE + Vector4f.SIZE * 3;

    // std140 padding
    final Matrix4f viewProjMatrix = new Matrix4f();

    float[]  viewDir = new float[3];
    float  _pad0;

    float[]  viewPos = new float[3];
    float  _pad1;

    float  viewWidth;
    float  viewHeight;
    float  viewCullThreshold;
    float  _pad2;
}
