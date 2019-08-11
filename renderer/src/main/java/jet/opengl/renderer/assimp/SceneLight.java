package jet.opengl.renderer.assimp;

import org.lwjgl.util.vector.Vector3f;

import jet.opengl.postprocessing.core.volumetricLighting.LightType;

public class SceneLight {
    final Vector3f mPosition = new Vector3f();
    final Vector3f mDirection = new Vector3f();
    final Vector3f mUp = new Vector3f();
    final Vector3f mDiffuseColor = new Vector3f();
    final Vector3f mSpecularColor = new Vector3f();
    final Vector3f mAmbientColor = new Vector3f();
    float mRange;
    float mSpotAngle;  // In radians
    float mSpotPenumbraAngle; // The penumbra angle in radians, must be less the spotAngle.
    float mAttenuationQuadratic;
    float mAttenuationLinear;
    float mAttenuationConstant;
    LightType mType;

}
