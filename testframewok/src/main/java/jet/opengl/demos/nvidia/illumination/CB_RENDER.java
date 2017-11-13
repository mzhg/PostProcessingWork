package jet.opengl.demos.nvidia.illumination;

import org.lwjgl.util.vector.Matrix4f;

/**
 * Created by Administrator on 2017/11/13 0013.
 */

final class CB_RENDER {
    float diffuseScale;
    int useDiffuseInterreflection;
    float directLight;
    float ambientLight;

    int useFloat4s;
    int useFloats;
    float temp;
    float temp2;

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
}
