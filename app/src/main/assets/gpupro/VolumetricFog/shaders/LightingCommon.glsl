#include "Common.glsl"

/**
 * Data about a single light.
 * Putting the light data in this struct allows the same lighting code to be used between standard deferred,
 * Where many light properties are known at compile time, and tiled deferred, where all light properties have to be fetched from a buffer.
 */
// TODO: inherit or compose FLightShaderParameters
struct FDeferredLightData
{
    float3 Position;
    float  InvRadius;
    float3 Color;
    float  FalloffExponent;
    float3 Direction;
    float VolumetricScatteringIntensity;
    float3 Tangent;
    float SoftSourceRadius;
    float2 SpotAngles;
    float SourceRadius;
    float SourceLength;
    float SpecularScale;
    float ContactShadowLength;
    float2 DistanceFadeMAD;
    float4 ShadowMapChannelMask;
/** Whether ContactShadowLength is in World Space or in Screen Space. */
    bool ContactShadowLengthInWS;
/** Whether to use inverse squared falloff. */
    bool bInverseSquared;
/** Whether this is a light with radial attenuation, aka point or spot light. */
    bool bRadialLight;
/** Whether this light needs spotlight attenuation. */
    bool bSpotLight;
    bool bRectLight;
/** Whether the light should apply shadowing. */
    uint ShadowedBits;
    float RectLightBarnCosAngle;
    float RectLightBarnLength;
};

layout(binding = 0) uniform LightData22
{
    FDeferredLightData LightData;
};

/**
 * Calculates attenuation for a spot light.
 * L normalize vector to light.
 * SpotDirection is the direction of the spot light.
 * SpotAngles.x is CosOuterCone, SpotAngles.y is InvCosConeDifference.
 */

float SpotAttenuation(float3 L, float3 SpotDirection, float2 SpotAngles)
{
    float ConeAngleFalloff = Square(saturate((dot(L, -SpotDirection) - SpotAngles.x) * SpotAngles.y));
    return ConeAngleFalloff;
}

/**
 * Returns a radial attenuation factor for a point light.
 * WorldLightVector is the vector from the position being shaded to the light, divided by the radius of the light.
 */
float RadialAttenuation(float3 WorldLightVector, float FalloffExponent)
{
    float NormalizeDistanceSquared = dot(WorldLightVector, WorldLightVector);

    // UE3 (fast, but now we not use the default of 2 which looks quite bad):
    return pow(1.0f - saturate(NormalizeDistanceSquared), FalloffExponent);

// new UE4 (more physically correct but slower and has a more noticable cutoff ring in the dark):
// AttenFunc(x) = 1 / (x * x + 1)
// derived: InvAttenFunc(y) = sqrtf(1 / y - 1)
// FalloffExponent is ignored
// the following code is a normalized (scaled and biased f(0)=1 f(1)=0) and optimized
/*
	// light less than x % is considered 0
	// 20% produces a bright sphere, 5 % is ok for performance, 8% looks close to the old one, smaller numbers would be more realistic but then the attenuation radius also should be increased.
	// we can expose CutoffPercentage later, alternatively we also can compute the attenuation radius from the CutoffPercentage and the brightness
	const float CutoffPercentage = 5.0f;

	float CutoffFraction = CutoffPercentage * 0.01f;

	// those could be computed on C++ side
	float PreCompX = 1.0f - CutoffFraction;
	float PreCompY = CutoffFraction;
	float PreCompZ = CutoffFraction / PreCompX;

	return (1 / ( NormalizeDistanceSquared * PreCompX + PreCompY) - 1) * PreCompZ;
*/
}


float GetLocalLightAttenuation(
float3 WorldPosition,
FDeferredLightData LightData,
inout float3 ToLight,
inout float3 L)
{
    ToLight = LightData.Position - WorldPosition;

    float DistanceSqr = dot( ToLight, ToLight );
    L = ToLight * rsqrt( DistanceSqr );

    float LightMask;
    if (LightData.bInverseSquared)
    {
        LightMask = Square( saturate( 1 - Square( DistanceSqr * Square(LightData.InvRadius) ) ) );
    }
    else
    {
        LightMask = RadialAttenuation(ToLight * LightData.InvRadius, LightData.FalloffExponent);
    }

    if (LightData.bSpotLight)
    {
        LightMask *= SpotAttenuation(L, -LightData.Direction, LightData.SpotAngles);
    }

    if( LightData.bRectLight )
    {
        // Rect normal points away from point
        LightMask = dot( LightData.Direction, L ) < 0 ? 0 : LightMask;
    }

    return LightMask;
}

struct FCapsuleLight
{
    float Length;
    float Radius;
    float SoftRadius;
    float DistBiasSqr;
    float3 LightPos[2];
};

FCapsuleLight GetCapsule( float3 ToLight, FDeferredLightData LightData )
{
    FCapsuleLight Capsule;
    Capsule.Length = LightData.SourceLength;
    Capsule.Radius = LightData.SourceRadius;
    Capsule.SoftRadius = LightData.SoftSourceRadius;
    Capsule.DistBiasSqr = 1;
    Capsule.LightPos[0] = ToLight - 0.5 * Capsule.Length * LightData.Tangent;
    Capsule.LightPos[1] = ToLight + 0.5 * Capsule.Length * LightData.Tangent;
    return Capsule;
}

float3 LineIrradiance( float3 N, float3 Line0, float3 Line1, float DistanceBiasSqr, out float CosSubtended, out float BaseIrradiance, out float NoL )
{
    float LengthSqr0 = dot( Line0, Line0 );
    float LengthSqr1 = dot( Line1, Line1 );
    float InvLength0 = rsqrt( LengthSqr0 );
    float InvLength1 = rsqrt( LengthSqr1 );
    float InvLength01 = InvLength0 * InvLength1;

    CosSubtended = dot( Line0, Line1 ) * InvLength01;
    BaseIrradiance = InvLength01 / ( CosSubtended * 0.5 + 0.5 + DistanceBiasSqr * InvLength01 );
    NoL = 0.5 * ( dot(N, Line0) * InvLength0 + dot(N, Line1) * InvLength1 );

    float3 VectorIrradiance = ( BaseIrradiance * 0.5 ) * ( Line0 * InvLength0 + Line1 * InvLength1 );
    return VectorIrradiance;
}

// Should this be SH instead?
float IntegrateLight( FCapsuleLight Capsule, bool bInverseSquared )
{
    float Falloff;
    if( Capsule.Length > 0 )
    {
        float NoL;
        float LineCosSubtended = 1;
        LineIrradiance( float3(0), Capsule.LightPos[0], Capsule.LightPos[1], Capsule.DistBiasSqr, LineCosSubtended, Falloff, NoL );
    }
    else
    {
        float3 ToLight = Capsule.LightPos[0];
        float DistSqr = dot( ToLight, ToLight );
        Falloff = rcp( DistSqr + Capsule.DistBiasSqr );
    }

    Falloff = bInverseSquared ? Falloff : 1;

    return Falloff;
}

// Copyright 1998-2019 Epic Games, Inc. All Rights Reserved.

/**
 * VolumeLightingCommon.usf
 */

// use low quality shadow sampling on translucency for better performance
#define SHADOW_QUALITY 2
//uniform sampler2D ShadowDepthTexture;

#include "ShadowProjectionCommon.glsl"
#include "ShadowFilteringCommon.glsl"

/** Parameters needed to access the shadow map of the light. */
uniform float4x4 WorldToShadowMatrix;
uniform float4 ShadowmapMinMax;
// .x:1/SplitNearFadeRegion, .y:1/SplitFarFadeRegion .zw:DistanceFadeMAD
uniform float4 ShadowInjectParams;
uniform float2 DepthBiasParameters;

/** Whether to compute static shadowing. */
uniform bool bStaticallyShadowed;

/** Shadow depth map computed for static geometry by Lightmass. */
layout(binding = 2) uniform sampler2D StaticShadowDepthTexture;
//SamplerState StaticShadowDepthTextureSampler;

/** Transform used for static shadowing by spot and directional lights. */
uniform float4x4 WorldToStaticShadowMatrix;
uniform float4 StaticShadowBufferSize;

uniform float3 WorldCameraOrigin;
uniform float3 ViewForward;

/** Computes dynamic and static shadowing for a point anywhere in space. */
float ComputeVolumeShadowing(float3 WorldPositionForLighting, bool bPointLight, bool bSpotLight)
{
    float ShadowFactor = 1;

    if (bStaticallyShadowed)
    {
        bool bUsePointLightShadowing = bPointLight;
        if (bUsePointLightShadowing)
        {
            float3 LightVector = WorldPositionForLighting - LightData.Position;
            float DistanceToLight = length(LightVector);
            float3 NormalizedLightVector = LightVector / DistanceToLight;

            //@todo - use parametrization without slow inverse trig.  Dual paraboloid?
            float NormalizedTheta = atan2(NormalizedLightVector.y, NormalizedLightVector.x) / (2 * PI);
            // atan2 returns in the range [-PI, PI], wrap the negative portion to [.5, 1]
            float U = NormalizedTheta > 0 ? NormalizedTheta : 1 + NormalizedTheta;
            float V = acos(NormalizedLightVector.z) / PI;
            float2 UnwrappedUVs = float2(U, V);

            float ShadowDepth = Texture2DSampleLevel(StaticShadowDepthTexture, UnwrappedUVs, 0).x;
            ShadowFactor = DistanceToLight * float(LightData.InvRadius < ShadowDepth);
        }
        else
        {
            // This path is used for directional lights and spot lights, which only require a single projection
            // Transform the world position into shadowmap space
            float4 HomogeneousShadowPosition = mul(float4(WorldPositionForLighting, 1), WorldToStaticShadowMatrix);
            float2 ShadowUVs = HomogeneousShadowPosition.xy / HomogeneousShadowPosition.w;

            // Treat as unshadowed if the voxel is outside of the shadow map
            if (all(greaterThanEqual(ShadowUVs, float2(0))) && all(lessThanEqual(ShadowUVs, float2(1))))
            {
                FPCFSamplerSettings Settings;
//                Settings.ShadowDepthTexture = StaticShadowDepthTexture;
//                Settings.ShadowDepthTextureSampler = StaticShadowDepthTextureSampler;
                Settings.ShadowBufferSize = StaticShadowBufferSize;
                Settings.SceneDepth = HomogeneousShadowPosition.z;
                Settings.TransitionScale = 40;
                Settings.bSubsurface = false;
                // We can sample outside of the static shadowmap, which is centered around the scene.  These 'infinite' depth values should not cause occlusion.
                Settings.bTreatMaxDepthUnshadowed = true;
                Settings.DensityMulConstant = 0;
                Settings.ProjectionDepthBiasParameters = float2(0, 0);

                ShadowFactor = Manual1x1PCF(ShadowUVs, Settings);

                /*
                // Sample the shadowmap depth and determine if this voxel is shadowed
                float ShadowDepth = Texture2DSampleLevel(StaticShadowDepthTexture, StaticShadowDepthTextureSampler, ShadowUVs, 0).x;
                ShadowFactor = HomogeneousShadowPosition.z < ShadowDepth;
                */
            }
        }
    }

#if DYNAMICALLY_SHADOWED
    bool bUseCubemapShadowing = bPointLight;
    float DynamicShadowFactor = 1;

    if (bUseCubemapShadowing)
    {
        DynamicShadowFactor = CubemapHardwarePCF(WorldPositionForLighting, LightData.Position, LightData.InvRadius, 0.03f * 512 * InvShadowmapResolution);
    }
    else
    {
        // Transform the world position into shadowmap space
        float4 HomogeneousShadowPosition = mul(float4(WorldPositionForLighting, 1), WorldToShadowMatrix);
        float2 ShadowUVs = HomogeneousShadowPosition.xy / HomogeneousShadowPosition.w;

        // Treat as unshadowed if the voxel is outside of the shadow map
        if (all(greaterThanEqual(ShadowUVs , ShadowmapMinMax.xy)) && all(lessThanEqual(ShadowUVs, ShadowmapMinMax.zw)))
        {
            // Sample the shadowmap depth and determine if this voxel is shadowed
            float ShadowDepth = Texture2DSampleLevel(ShadowDepthTexture, ShadowUVs, 0).x;
            DynamicShadowFactor = float(HomogeneousShadowPosition.z < ShadowDepth) - DepthBiasParameters.x;
        }
    }

    // fade shadows in the distance
    if (!bPointLight && !bSpotLight)
    {
        float Depth = dot(WorldPositionForLighting - WorldCameraOrigin, ViewForward);
        float DistanceFade = saturate(Depth * ShadowInjectParams.z + ShadowInjectParams.w);
        DynamicShadowFactor = lerp(DynamicShadowFactor, 1.0f, DistanceFade * DistanceFade);
    }

    // Combine static shadowing and dynamic shadowing, important for stationary directional lights with CSM
    ShadowFactor = min(ShadowFactor, DynamicShadowFactor);
#endif

    return ShadowFactor;
}