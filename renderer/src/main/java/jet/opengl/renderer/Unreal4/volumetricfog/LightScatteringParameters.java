package jet.opengl.renderer.Unreal4.volumetricfog;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

final class LightScatteringParameters {
    Matrix4f DirectionalLightFunctionWorldToShadow = new Matrix4f();  // TODO not used so far

    Vector4f[] FrameJitterOffsets;
    final Vector3f HeightFogDirectionalLightInscatteringColor = new Vector3f();
    int HistoryMissSuperSampleCount;
    float HistoryWeight;
    float InverseSquaredLightDistanceBiasScale;

    float PhaseG;
    Matrix4f UnjitteredClipToTranslatedWorld;
    Matrix4f UnjitteredPrevWorldToClip;
    boolean UseDirectionalLightShadowing;
    boolean UseHeightFogColors;
    Vector3f View_PreViewTranslation;
    Vector3f VolumetricFog_GridSize;
    Vector3f VolumetricFog_GridZParams;
    Vector3f WorldCameraOrigin;
    Matrix4f g_ViewProj;

//    Uniform [sampler2D name=LightFunctionTexture, location=22, value = 0]
//    Uniform [sampler3D name=LightScatteringHistory, location=23, value = 0]
//    Uniform [image3D name=LocalShadowedLightScattering, location=24, value = 0]
//    Uniform [image3D name=RWLightScattering, location=26, value = 3]
//    Uniform [sampler2D name=DirectionalLightShadowmapAtlas, location=1, value = 0]
//    Uniform [image3D name=VBufferA, location=31, value = 1]
//    Uniform [image3D name=VBufferB, location=32, value = 2]
}
