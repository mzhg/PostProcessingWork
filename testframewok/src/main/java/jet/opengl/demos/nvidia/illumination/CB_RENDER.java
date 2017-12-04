package jet.opengl.demos.nvidia.illumination;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Readable;
import org.lwjgl.util.vector.Vector4f;

import java.nio.ByteBuffer;

/**
 * Created by Administrator on 2017/11/13 0013.
 */

final class CB_RENDER implements Readable{
    static final int SIZE = Vector4f.SIZE * 3 + Matrix4f.SIZE * 6;
    float diffuseScale;
    int useDiffuseInterreflection;
    float directLight;
    float ambientLight;

    int useFloat4s;
    int useFloats;
//    int temp;
//    int temp2;

    float invSMSize;
    float normalMapMultiplier;
    int useDirectionalDerivativeClamping;
    float directionalDampingAmount;

    final Matrix4f worldToLPVNormTex = new Matrix4f();
    final Matrix4f worldToLPVNormTex1 = new Matrix4f();
    final Matrix4f worldToLPVNormTex2 = new Matrix4f();

    final Matrix4f worldToLPVNormTexRender = new Matrix4f();
    final Matrix4f worldToLPVNormTexRender1 = new Matrix4f();
    final Matrix4f worldToLPVNormTexRender2 = new Matrix4f();

    @Override
    public ByteBuffer store(ByteBuffer buf) {
        buf.putFloat(diffuseScale);
        buf.putInt(useDiffuseInterreflection);
        buf.putFloat(directLight);
        buf.putFloat(ambientLight);

        buf.putInt(useFloat4s);
        buf.putInt(useFloats);
        buf.putInt(0);
        buf.putInt(0);

        buf.putFloat(invSMSize);
        buf.putFloat(normalMapMultiplier);
        buf.putInt(useDirectionalDerivativeClamping);
        buf.putFloat(directionalDampingAmount);

        worldToLPVNormTex.store(buf);
        worldToLPVNormTex1.store(buf);
        worldToLPVNormTex2.store(buf);
        worldToLPVNormTexRender.store(buf);
        worldToLPVNormTexRender1.store(buf);
        worldToLPVNormTexRender2.store(buf);
        return buf;
    }
}
