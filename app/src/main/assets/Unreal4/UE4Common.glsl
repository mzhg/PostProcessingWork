#include "../shader_libs/PostProcessingHLSLCompatiable.glsl"

#define Texture3DSampleLevel(tex, uv, lod) textureLod(tex, uv, lod)
#define Texture2DSampleLevel(tex, uv, lod) textureLod(tex, uv, lod)
#define Texture3DSample(tex, uv)           texture(tex, uv)
#define Texture2DSample(tex, uv)           texture(tex, uv)
#define COMP_SIZE(X,Y,Z)  layout(local_size_x = X, local_size_y = Y, local_size_z = Z)in
#define LOOP
#define BRANCH

struct UE4View
{
    float4x4 ViewToClip;
    float4x4 ClipToView;
    float4x4 ViewToTranslatedWorld;
    float4x4 TranslatedWorldToView;
    float4 ViewSizeAndInvSize;
    float3 PreViewTranslation;
};

layout(binding = 8) uniform ViewBuffer
{
    UE4View View;
};

#ifndef NUM_DIRECTIONAL_LIGHT_CASCADES
#define NUM_DIRECTIONAL_LIGHT_CASCADES 1
#endif

struct LocalLightingData
{
    float4x4 DirectionalLightWorldToStaticShadow;
    float4 DirectionalLightStaticShadowBufferSize;

    float4x4 DirectionalLightWorldToShadowMatrix[NUM_DIRECTIONAL_LIGHT_CASCADES];
    float4 DirectionalLightShadowmapMinMax[NUM_DIRECTIONAL_LIGHT_CASCADES];
    float4 DirectionalLightShadowmapAtlasBufferSize;

    float3 DirectionalLightColor;
    float DirectionalLightVolumetricScatteringIntensity;
    float3 DirectionalLightDirection;
    float DirectionalLightDepthBias;

    uint3 CulledGridSize;
    uint LightGridPixelSizeShift;

//    float4 ForwardLocalLightBuffer[10];

    uint HasDirectionalLight;
    uint DirectionalLightUseStaticShadowing;
    uint NumDirectionalLightCascades;
    uint NumLocalLights;

    float4 LightGridZParams;
    float4 CascadeEndDepths;

    uint DirectionalLightShadowMapChannelMask;
    uint NumGridCells;
    uint MaxCulledLightsPerCell;
    uint NumReflectionCaptures;

    float2 DirectionalLightDistanceFadeMAD;
};

layout(binding = 9) uniform _ForwardLightData
{
    LocalLightingData ForwardLightData;
};

#ifndef NUM_MAX_REFLECTION_CAPTURES
#define NUM_MAX_REFLECTION_CAPTURES 4
#endif

struct UE4ReflectionCapture
{
    float4 PositionAndRadius[NUM_MAX_REFLECTION_CAPTURES];
};

layout(binding = 10) uniform _ReflectionCapture
{
    UE4ReflectionCapture ReflectionCapture;
};

layout(binding = 11) readonly buffer _ForwardLocalLightBuffer
{
    float4 ForwardLocalLightBuffer[];
};

float ConvertToDeviceZ(float depth)
{
    vec4 clipPos = View.ViewToClip * vec4(0,0, depth, 1);
    return clipPos.z / clipPos.w;
}

/** Computes squared distance from a point in space to an AABB. */
float ComputeSquaredDistanceFromBoxToPoint(float3 BoxCenter, float3 BoxExtent, float3 InPoint)
{
	float3 AxisDistances = max(abs(InPoint - BoxCenter) - BoxExtent, float3(0));
	return dot(AxisDistances, AxisDistances);
}