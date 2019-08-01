package jet.opengl.demos.gpupro.volumetricfog;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector3i;
import org.lwjgl.util.vector.Vector4f;

final class LocalLightData {
    final Matrix4f DirectionalLightWorldToStaticShadow = new Matrix4f();
    final Vector4f DirectionalLightStaticShadowBufferSize = new Vector4f();

    final Matrix4f[] DirectionalLightWorldToShadowMatrix;
    final Vector4f[] DirectionalLightShadowmapMinMax;
    final Vector4f DirectionalLightShadowmapAtlasBufferSize= new Vector4f();

    boolean HasDirectionalLight;
    boolean DirectionalLightUseStaticShadowing;
    int NumDirectionalLightCascades;
    final float[] CascadeEndDepths;
    final Vector3f DirectionalLightColor = new Vector3f();
    float DirectionalLightVolumetricScatteringIntensity;
    final Vector3f DirectionalLightDirection = new Vector3f();
    float DirectionalLightDepthBias;

    int LightGridPixelSizeShift;
    final Vector3f LightGridZParams = new Vector3f();
    final Vector3i CulledGridSize = new Vector3i();
    int NumLocalLights;
    final int[] NumCulledLightsGrid;
    int DirectionalLightShadowMapChannelMask;
    final Vector2f DirectionalLightDistanceFadeMAD = new Vector2f();
    final int[] CulledLightDataGrid;
    final Vector4f[] ForwardLocalLightBuffer;

    LocalLightData(int numCascade, int numLocalLights){
        this.NumDirectionalLightCascades = numCascade;
        this.NumLocalLights = numLocalLights;

        DirectionalLightWorldToShadowMatrix = new Matrix4f[numCascade];
        DirectionalLightShadowmapMinMax = new Vector4f[numCascade];
        CascadeEndDepths = new float[numCascade];

        NumCulledLightsGrid = new int[numLocalLights];
        CulledLightDataGrid = new int[numLocalLights];
        ForwardLocalLightBuffer = new Vector4f[5];

        for(int i = 0; i < numCascade; i++) {
            DirectionalLightWorldToShadowMatrix[i] = new Matrix4f();
            DirectionalLightShadowmapMinMax[i] = new Vector4f();
        }
    }
}
