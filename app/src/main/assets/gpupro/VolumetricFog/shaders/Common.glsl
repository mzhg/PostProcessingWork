#include "../../../shader_libs/PostProcessingHLSLCompatiable.glsl"

//RWTexture3D<float4> RWVBufferA;
//RWTexture3D<float4> RWVBufferB;

#ifndef DISTANCE_FIELD_SKY_OCCLUSION
#define DISTANCE_FIELD_SKY_OCCLUSION 0
#endif

#define PI 3.1415926

uniform vec3 VolumetricFog_GridZParams;
uniform vec3 VolumetricFog_GridSize;
uniform vec3 View_PreViewTranslation;
uniform vec4 FogStruct_ExponentialFogParameters;
uniform vec4 FogStruct_ExponentialFogParameters2;
uniform vec4 FogStruct_ExponentialFogParameters3;
uniform float g_CameraNear;
uniform float g_CameraFar;
uniform mat4  g_ViewProj;  // TODO It is a proj.

#define USE_PUNCTUAL_LIGHT  1

#define Texture3DSampleLevel(tex, uv, lod) textureLod(tex, uv, lod)

float ConvertToDeviceZ(float depth)
{
    vec4 clipPos = g_ViewProj * vec4(0,0, depth, 1);
    return clipPos.z / clipPos.w;

    float invDiff = 1.0/(g_CameraFar-g_CameraNear);
    float DeviceZ = (-(g_CameraFar + g_CameraNear) * invDiff * depth - 2 * g_CameraFar * g_CameraNear * invDiff)/(-depth);
    return DeviceZ;
}

float ComputeDepthFromZSlice(float ZSlice)
{
    float SliceDepth = (exp2(ZSlice / VolumetricFog_GridZParams.z) - VolumetricFog_GridZParams.y) / VolumetricFog_GridZParams.x;
    return SliceDepth;
}

float ComputeZSliceFromDepth(float SceneDepth, float Offset){
    return log2(SceneDepth*VolumetricFog_GridZParams.x+VolumetricFog_GridZParams.y)*VolumetricFog_GridZParams.z + Offset;
}

float Luminance(in vec3 rgb)
{
    float R = rgb.x;
    float G = rgb.y;
    float B = rgb.z;

    return R*0.299 + G*0.587 + B*0.114;
}

float4 MakePositiveFinite(float4 v)
{
    bool4 test = isinf(v);
    float4 result;
    result.x = test.x ? 0.0 : v.x;
    result.y = test.y ? 0.0 : v.y;
    result.z = test.z ? 0.0 : v.z;
    result.w = test.w ? 0.0 : v.w;

    return result;
}

float Square(float x) { return x * x;}

uniform float4x4 UnjitteredClipToTranslatedWorld;
uniform float4x4 UnjitteredPrevWorldToClip;
uniform float HistoryWeight = 0.9;

uniform uint HistoryMissSuperSampleCount;
uniform float4 FrameJitterOffsets[16];
uniform float InverseSquaredLightDistanceBiasScale;
uniform float PhaseG;

float3 ComputeCellWorldPosition(uint3 GridCoordinate, float3 CellOffset, out float SceneDepth)
{
    float2 VolumeUV = (GridCoordinate.xy + CellOffset.xy) / VolumetricFog_GridSize.xy;
    float2 VolumeNDC = (VolumeUV * 2 - 1) /* float2(1, -1)*/;

    SceneDepth = ComputeDepthFromZSlice(GridCoordinate.z + CellOffset.z);

    float TileDeviceZ = ConvertToDeviceZ(-SceneDepth);
    float4 CenterPosition = mul(float4(VolumeNDC, TileDeviceZ, 1), UnjitteredClipToTranslatedWorld);
    return CenterPosition.xyz / CenterPosition.w - View_PreViewTranslation;
}

float3 ComputeCellWorldPosition(uint3 GridCoordinate, float3 CellOffset)
{
    float Unused;
    return ComputeCellWorldPosition(GridCoordinate, CellOffset, Unused);
}

uniform vec3 g_CameraRange;  // xy: camera near and for; z : 0 for the orth, otherwise for the perspective.
// ClipPosDevice range are [0,1]
float3 ComputeCellGrid(float3 ClipPosDevice, float3 CellOffset)
{
    float3 GridCoordinate;
    GridCoordinate.xy = ClipPosDevice.xy * VolumetricFog_GridSize.xy + CellOffset.xy;

    float mZFar = g_CameraFar;
    float mZNear = g_CameraNear;
    float SceneDepth = mZFar*mZNear/(mZFar-ClipPosDevice.z*(mZFar-mZNear));;
    GridCoordinate.z = ComputeZSliceFromDepth(SceneDepth, CellOffset.z);

    return GridCoordinate;
}

float3 RaleighScattering()
{
    float3 Wavelengths = float3(650.0f, 510.0f, 475.0f);
    float ParticleDiameter = 60;
    float ParticleRefractiveIndex = 1.3f;

    float3 ScaleDependentPortion = pow(ParticleDiameter, 6) / pow(Wavelengths, float3(4));
    float RefractiveIndexPortion = (ParticleRefractiveIndex * ParticleRefractiveIndex - 1) / (ParticleRefractiveIndex * ParticleRefractiveIndex + 2);
    return (2 * pow(PI, 5) * RefractiveIndexPortion * RefractiveIndexPortion) * ScaleDependentPortion / 3.0f;
}

float3 ScatteringFunction()
{
    return float3(1);
    //return RaleighScattering();
}

float IsotropicPhase()
{
    return 1 / (4 * PI);
}

float HenyeyGreensteinPhase(float g, float CosTheta)
{
    g = -g;
    return (1 - g * g) / (4 * PI * pow(1 + g * g - 2 * g * CosTheta, 1.5f));
}

float SchlickPhase(float k, float CosTheta)
{
    float Inner = (1 + k * CosTheta);
    return (1 - k * k) / (4 * PI * Inner * Inner);
}

float RaleighPhase(float CosTheta)
{
    return 3.0f * (1.0f + CosTheta * CosTheta) / (16.0f * PI);
}

// Positive g = forward scattering
// Zero g = isotropic
// Negative g = backward scattering
float PhaseFunction(float g, float CosTheta)
{
    return HenyeyGreensteinPhase(g, CosTheta);
}

//Texture3D<float4> VBufferA;
//Texture3D<float4> VBufferB;

//Texture3D<float4> LightScatteringHistory;
//SamplerState LightScatteringHistorySampler;

//Texture3D<float4> LocalShadowedLightScattering;

//RWTexture3D<float4> RWLightScattering;

float4 EncodeHDR(float4 Color)
{
    return Color;

    //float Exposure = 1;
    //return float4(Color.rgb * rcp((Color.r*0.299 + Color.g*0.587 + Color.b*0.114) * Exposure + 1.0), Color.a);
}

float4 DecodeHDR(float4 Color)
{
    return Color;

    //float Exposure = 1;
    //return float4(Color.rgb * rcp((Color.r*(-0.299) + Color.g*(-0.587) + Color.b*(-0.114)) * Exposure + 1.0), Color.a);
}

#if DISTANCE_FIELD_SKY_OCCLUSION
float HemisphereConeTraceAgainstGlobalDistanceFieldClipmap(
uint ClipmapIndex,
float3 WorldShadingPosition,
float3 ConeDirection,
float TanConeHalfAngle)
{
    float MinStepSize = GlobalVolumeCenterAndExtent[ClipmapIndex].w * 2 / 100.0f;
    float InvAOGlobalMaxOcclusionDistance = 1.0f / AOGlobalMaxOcclusionDistance;

    float MinVisibility = 1;
    float WorldStepOffset = 2;

//    LOOP
    for (uint StepIndex = 0; StepIndex < NUM_CONE_STEPS && WorldStepOffset < AOGlobalMaxOcclusionDistance; StepIndex++)
    {
        float3 WorldSamplePosition = WorldShadingPosition + ConeDirection * WorldStepOffset;
        float3 StepVolumeUV = ComputeGlobalUV(WorldSamplePosition, ClipmapIndex);
        float DistanceToOccluder = SampleGlobalDistanceField(ClipmapIndex, StepVolumeUV).x;
        float SphereRadius = WorldStepOffset * TanConeHalfAngle;
        float InvSphereRadius = rcpFast(SphereRadius);

        // Derive visibility from 1d intersection
        float Visibility = saturate(DistanceToOccluder * InvSphereRadius);

        float OccluderDistanceFraction = (WorldStepOffset + DistanceToOccluder) * InvAOGlobalMaxOcclusionDistance;

        // Fade out occlusion based on distance to occluder to avoid a discontinuity at the max AO distance
        Visibility = max(Visibility, saturate(OccluderDistanceFraction * OccluderDistanceFraction * .6f));

        MinVisibility = min(MinVisibility, Visibility);

        WorldStepOffset += max(DistanceToOccluder, MinStepSize);
    }

    return MinVisibility;
}

float HemisphereConeTraceAgainstGlobalDistanceField(float3 WorldShadingPosition, float3 ConeDirection, float TanConeHalfAngle)
{
    float MinVisibility = 1.0f;
    float DistanceFromClipmap = ComputeDistanceFromBoxToPointInside(GlobalVolumeCenterAndExtent[0].xyz, GlobalVolumeCenterAndExtent[0].www, WorldShadingPosition);

//    BRANCH
    if (DistanceFromClipmap > AOGlobalMaxOcclusionDistance)
    {
        MinVisibility = HemisphereConeTraceAgainstGlobalDistanceFieldClipmap(0, WorldShadingPosition, ConeDirection, TanConeHalfAngle);
    }
    else
    {
        DistanceFromClipmap = ComputeDistanceFromBoxToPointInside(GlobalVolumeCenterAndExtent[1].xyz, GlobalVolumeCenterAndExtent[1].www, WorldShadingPosition);

//        BRANCH
        if (DistanceFromClipmap > AOGlobalMaxOcclusionDistance)
        {
            MinVisibility = HemisphereConeTraceAgainstGlobalDistanceFieldClipmap(1, WorldShadingPosition, ConeDirection, TanConeHalfAngle);
        }
        else
        {
            DistanceFromClipmap = ComputeDistanceFromBoxToPointInside(GlobalVolumeCenterAndExtent[2].xyz, GlobalVolumeCenterAndExtent[2].www, WorldShadingPosition);
            float DistanceFromLastClipmap = ComputeDistanceFromBoxToPointInside(GlobalVolumeCenterAndExtent[3].xyz, GlobalVolumeCenterAndExtent[3].www, WorldShadingPosition);

//        BRANCH
            if (DistanceFromClipmap > AOGlobalMaxOcclusionDistance)
            {
                MinVisibility = HemisphereConeTraceAgainstGlobalDistanceFieldClipmap(2u, WorldShadingPosition, ConeDirection, TanConeHalfAngle);
            }
            else if (DistanceFromLastClipmap > AOGlobalMaxOcclusionDistance)
            {
                MinVisibility = HemisphereConeTraceAgainstGlobalDistanceFieldClipmap(3u, WorldShadingPosition, ConeDirection, TanConeHalfAngle);
            }
        }
    }

    return MinVisibility;
}

#endif

float SkyLightUseStaticShadowing;

float ComputeSkyVisibility(float3 WorldPosition, float3 BrickTextureUVs)
{
    float Visibility = 1;

#if DISTANCE_FIELD_SKY_OCCLUSION
    // Trace one 45 degree cone straight up for sky occlusion
    float TanConeHalfAngle = tan(PI / 4);

    Visibility = HemisphereConeTraceAgainstGlobalDistanceField(WorldPosition, float3(0, 0, 1), TanConeHalfAngle);

#endif

#if ALLOW_STATIC_LIGHTING
    if (SkyLightUseStaticShadowing > 0)
    {
        float3 SkyBentNormal = GetVolumetricLightmapSkyBentNormal(BrickTextureUVs);
        Visibility = length(SkyBentNormal);
    }
#endif

    return Visibility;
}

uniform float4x4 DirectionalLightFunctionWorldToShadow;
layout(binding = 5) uniform sampler2D LightFunctionTexture;
//SamplerState LightFunctionSampler;

float GetLightFunction(float3 WorldPosition)
{
    float4 HomogeneousShadowPosition = mul(float4(WorldPosition, 1), DirectionalLightFunctionWorldToShadow);
    float2 LightFunctionUV = HomogeneousShadowPosition.xy * .5f + .5f;
//    LightFunctionUV.y = 1 - LightFunctionUV.y;

//    return Texture2DSampleLevel(LightFunctionTexture, LightFunctionSampler, LightFunctionUV, 0).x;
    return textureLod(LightFunctionTexture, LightFunctionUV, 0.0).x;
}

float ComputeNormalizedZSliceFromDepth(float SceneDepth)
{
    return log2(SceneDepth * VolumetricFog_GridZParams.x + VolumetricFog_GridZParams.y) * VolumetricFog_GridZParams.z / VolumetricFog_GridSize.z;
}

float3 ComputeVolumeUV(float3 WorldPosition, float4x4 WorldToClip)
{
    float4 NDCPosition = mul(float4(WorldPosition, 1), WorldToClip);
    NDCPosition.xy /= NDCPosition.w;
    return float3(NDCPosition.xy * float2(.5f) + .5f, ComputeNormalizedZSliceFromDepth(NDCPosition.w));
}

#ifndef SUPPORT_VOLUMETRIC_FOG
#define SUPPORT_VOLUMETRIC_FOG 0
#endif

float4 CombineVolumetricFog(float4 GlobalFog, float3 VolumeUV)
{
    float4 VolumetricFogLookup = float4(0, 0, 0, 1);
#if SUPPORT_VOLUMETRIC_FOG
    if (FogStruct.ApplyVolumetricFog > 0)
    {
        VolumetricFogLookup = Texture3DSampleLevel(FogStruct.IntegratedLightScattering, SharedIntegratedLightScatteringSampler, VolumeUV, 0);
    }
#endif
    // Visualize depth distribution
    //VolumetricFogLookup.rgb += .1f * frac(min(ZSlice, 1.0f) / View.VolumetricFogInvGridSize.z);
    return float4(VolumetricFogLookup.rgb + GlobalFog.rgb * VolumetricFogLookup.a, VolumetricFogLookup.a * GlobalFog.a);

}