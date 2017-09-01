package jet.opengl.demos.nvidia.fire;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

/**
 * Created by mazhen'gui on 2017/9/1.
 */

final class UniformData {
    final Matrix4f[] mCubeViewMatrixs = new Matrix4f[6];
    final Matrix4f mCubeProjMatrix = new Matrix4f;
    final Matrix4f mWorldViewProj = new Matrix4f();
    final Vector3f vEyePos = new Vector3f();
    final Vector3f vLightPos = new Vector3f();
    float fLightIntensity;
    float fStepSize;
    float fTime;
    float fNoiseScale;
    float fRoughness;
    final float[] fFrequencyWeights = new float[5];
    boolean bJitter;
    int iCubeMapFace;
}
