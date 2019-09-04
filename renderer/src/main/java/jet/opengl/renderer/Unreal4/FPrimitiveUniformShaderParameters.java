package jet.opengl.renderer.Unreal4;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

/**
 * The uniform shader parameters associated with a primitive.
 * Note: Must match FPrimitiveSceneData in shaders.
 * Note 2: Try to keep this 16 byte aligned. i.e |Matrix4x4|Vector3,float|Vector3,float|Vector4|  _NOT_  |Vector3,(waste padding)|Vector3,(waste padding)|Vector3. Or at least mark out padding if it can't be avoided.
 */
public class FPrimitiveUniformShaderParameters {
    public final Matrix4f LocalToWorld = new Matrix4f();
    public final Vector4f InvNonUniformScaleAndDeterminantSign = new Vector4f();
    public final Vector4f ObjectWorldPositionAndRadius = new Vector4f();
    public final Matrix4f WorldToLocal = new Matrix4f();
    public final Matrix4f PreviousLocalToWorld = new Matrix4f();
    public final Matrix4f PreviousWorldToLocal = new Matrix4f();
    public final Vector3f ActorWorldPosition = new Vector3f();
    public float UseSingleSampleShadowFromStationaryLights;
    public final Vector3f ObjectBounds = new Vector3f();
    public float LpvBiasMultiplier;
    public float DecalReceiverMask;
    public float PerObjectGBufferData;
    public float UseVolumetricLightmapShadowFromStationaryLights;
    public float DrawsVelocity;
    public final Vector4f ObjectOrientation = new Vector4f();
    public final Vector4f NonUniformScale = new Vector4f();
    public final Vector3f LocalObjectBoundsMin = new Vector3f();		// This is used in a custom material function (ObjectLocalBounds.uasset = new Vector3f();
    public int LightingChannelMask;
    public final Vector3f LocalObjectBoundsMax = new Vector3f();		// This is used in a custom material function (ObjectLocalBounds.uasset = new Vector3f();
    public int LightmapDataIndex;
    public final Vector3f PreSkinnedLocalBoundsMin = new Vector3f();
    public int SingleCaptureIndex;			// Should default to 0 if no reflection captures are provided;
    public final Vector3f PreSkinnedLocalBoundsMax = new Vector3f();
    public int OutputVelocity;
    public final Vector4f[] CustomPrimitiveData = new Vector4f[UE4Engine.NumCustomPrimitiveDataFloat4s];
}
