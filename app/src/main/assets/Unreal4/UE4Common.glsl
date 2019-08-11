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

}

layout(binding = 0) uniform ViewBuffer
{
    UE4View View;
};

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

    float4 ForwardLocalLightBuffer[10];

    uint HasDirectionalLight;
    uint DirectionalLightUseStaticShadowing;
    uint NumDirectionalLightCascades;
    uint NumLocalLights;

    float4 LightGridZParams;
    float4 CascadeEndDepths;

    uint NumGridCells;
    uint MaxCulledLightsPerCell;

//    uint NumCulledLightsGrid[12];

    float2 DirectionalLightDistanceFadeMAD;
//    uint DirectionalLightShadowMapChannelMask;
//    uint CulledLightDataGrid[4];

};

layout(binding = 1) uniform _ForwardLightData
{
    LocalLightingData ForwardLightData;
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