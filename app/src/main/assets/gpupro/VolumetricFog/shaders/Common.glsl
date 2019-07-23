#include "../../../shader_libs/PostProcessingHLSLCompatiable.glsl"

//RWTexture3D<float4> RWVBufferA;
//RWTexture3D<float4> RWVBufferB;

uniform vec3 Volumetric_FogGridZParams;
uniform vec3 VolumetricFog_GridSize;
uniform vec3 View_PreViewTranslation;
uniform vec4 FogStruct_ExponentialFogParameters;
uniform vec4 FogStruct_ExponentialFogParameters2;
uniform vec4 FogStruct_ExponentialFogParameters3;


float ComputeDepthFromZSlice(float ZSlice)
{
    float SliceDepth = (exp2(ZSlice / VolumetricFog_GridZParams.z) - VolumetricFog_GridZParams.y) / VolumetricFog_GridZParams.x;
    return SliceDepth;
}

uniform float4x4 UnjitteredClipToTranslatedWorld;
uniform float4x4 UnjitteredPrevWorldToClip;

float3 ComputeCellWorldPosition(uint3 GridCoordinate, float3 CellOffset, out float SceneDepth)
{
    float2 VolumeUV = (GridCoordinate.xy + CellOffset.xy) / VolumetricFog_GridSize.xy;
    float2 VolumeNDC = (VolumeUV * 2 - 1) /* float2(1, -1)*/;

    SceneDepth = ComputeDepthFromZSlice(GridCoordinate.z + CellOffset.z);

    float TileDeviceZ = ConvertToDeviceZ(SceneDepth);
    float4 CenterPosition = mul(float4(VolumeNDC, TileDeviceZ, 1), UnjitteredClipToTranslatedWorld);
    return CenterPosition.xyz / CenterPosition.w - View_PreViewTranslation;
}

float3 ComputeCellWorldPosition(uint3 GridCoordinate, float3 CellOffset)
{
    float Unused;
    return ComputeCellWorldPosition(GridCoordinate, CellOffset, Unused);
}

float3 RaleighScattering()
{
    float3 Wavelengths = float3(650.0f, 510.0f, 475.0f);
    float ParticleDiameter = 60;
    float ParticleRefractiveIndex = 1.3f;

    float3 ScaleDependentPortion = pow(ParticleDiameter, 6) / pow(Wavelengths, 4);
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
        MinVisibility = HemisphereConeTraceAgainstGlobalDistanceFieldClipmap((uint)0, WorldShadingPosition, ConeDirection, TanConeHalfAngle);
    }
    else
    {
        DistanceFromClipmap = ComputeDistanceFromBoxToPointInside(GlobalVolumeCenterAndExtent[1].xyz, GlobalVolumeCenterAndExtent[1].www, WorldShadingPosition);

//        BRANCH
        if (DistanceFromClipmap > AOGlobalMaxOcclusionDistance)
        {
            MinVisibility = HemisphereConeTraceAgainstGlobalDistanceFieldClipmap((uint)1, WorldShadingPosition, ConeDirection, TanConeHalfAngle);
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

float4x4 DirectionalLightFunctionWorldToShadow;
Texture2D LightFunctionTexture;
SamplerState LightFunctionSampler;

float GetLightFunction(float3 WorldPosition)
{
    float4 HomogeneousShadowPosition = mul(float4(WorldPosition, 1), DirectionalLightFunctionWorldToShadow);
    float2 LightFunctionUV = HomogeneousShadowPosition.xy * .5f + .5f;
    LightFunctionUV.y = 1 - LightFunctionUV.y;

    return Texture2DSampleLevel(LightFunctionTexture, LightFunctionSampler, LightFunctionUV, 0).x;
}