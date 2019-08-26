package jet.opengl.renderer.Unreal4.volumetricfog;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

class InjectLocalLightParameters {
    final Vector2f DepthBiasParameters = new Vector2f();
    Vector4f[] FrameJitterOffsets;

    int HistoryMissSuperSampleCount = 4;
    float HistoryWeight;
    float InvShadowmapResolution;
    float InverseSquaredLightDistanceBiasScale;

    int MinZ;
    float PhaseG;
//    Uniform [samplerCubeShadow name=ShadowDepthCubeTexture2, location=23, value = 0]
//    Uniform [sampler2D name=ShadowDepthTexture, location=24, value = 0]
    final Vector4f ShadowInjectParams = new Vector4f();
    final Matrix4f[] ShadowViewProjectionMatrices = new Matrix4f[6];
    final Vector4f ShadowmapMinMax = new Vector4f();
    final Vector4f StaticShadowBufferSize = new Vector4f();

//    Uniform [sampler2D name=StaticShadowDepthTexture, location=34, value = 0]

    final Matrix4f UnjitteredClipToTranslatedWorld = new Matrix4f();
    Matrix4f UnjitteredPrevWorldToClip;
    final Matrix4f ViewToVolumeClip = new Matrix4f();
    final Vector3f ViewForward = new Vector3f();
    final Vector3f View_PreViewTranslation = new Vector3f();
    final Vector4f ViewSpaceBoundingSphere = new Vector4f();
    final Vector3f VolumetricFog_GridSize = new Vector3f();
    final Vector3f VolumetricFog_GridZParams = new Vector3f();
    final Vector3f WorldCameraOrigin = new Vector3f();

    final Matrix4f WorldToShadowMatrix = new Matrix4f();
    final Matrix4f WorldToStaticShadowMatrix = new Matrix4f();
    final Matrix4f g_ViewProj = new Matrix4f();
    boolean bStaticallyShadowed;
    float cameraNear, cameraFar;
}
