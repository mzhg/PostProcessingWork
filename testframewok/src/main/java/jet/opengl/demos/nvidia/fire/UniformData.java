package jet.opengl.demos.nvidia.fire;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

/**
 * Created by mazhen'gui on 2017/9/1.
 */

final class UniformData {
    final Matrix4f[] mCubeViewMatrixs = new Matrix4f[6];
    final Matrix4f mCubeProjMatrix = new Matrix4f();
    final Matrix4f mWorldViewProj = new Matrix4f();
    final Matrix4f mProj = new Matrix4f();
    final Vector3f vEyePos = new Vector3f();
    final Vector3f vLightPos = new Vector3f();
    float fLightIntensity;
    float fStepSize = 3.0f;
    float fTime;
    float fNoiseScale = 1.35f;
    float fRoughness = 3.2f;
    final float[] fFrequencyWeights = {
            1.0f, 0.5f, 0.25f, 0.125f, 0.0525f
    };
    boolean bJitter = true;
    int iCubeMapFace;

    UniformData(){
        for(int i = 0; i < mCubeViewMatrixs.length; i++){
            mCubeViewMatrixs[i] = new Matrix4f();
        }
    }
}
