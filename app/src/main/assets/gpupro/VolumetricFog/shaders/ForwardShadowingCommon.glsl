// Copyright 1998-2019 Epic Games, Inc. All Rights Reserved.

/*=============================================================================
	ForwardShadowingCommon.ush
=============================================================================*/

#include "ShadowFilteringCommon.glsl"

#ifndef ALLOW_STATIC_LIGHTING
#define ALLOW_STATIC_LIGHTING 0
#endif

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

    float4 DirectionalLightColor;
//    float DirectionalLightVolumetricScatteringIntensity;
    float4 DirectionalLightDirection;
//    float DirectionalLightDepthBias;

    uint4 CulledGridSize;
//    uint LightGridPixelSizeShift;

    float4 ForwardLocalLightBuffer[10];

    uint HasDirectionalLight;
    uint DirectionalLightUseStaticShadowing;
    uint NumDirectionalLightCascades;
    uint NumLocalLights;

    float4 LightGridZParams;
    float4 CascadeEndDepths;

//    uint NumCulledLightsGrid[12];

    float2 DirectionalLightDistanceFadeMAD;
//    uint DirectionalLightShadowMapChannelMask;
//    uint CulledLightDataGrid[4];

};

layout(binding = 0) uniform _ForwardLightData
{
    LocalLightingData ForwardLightData;
};

struct LightGridData
{
    uint LightGridPixelSizeShift;
    float3 LightGridZParams;
    int3 CulledGridSize;
};

#ifndef INSTANCED_STEREO
#define INSTANCED_STEREO 0
#endif

LightGridData GetLightGridData(uint EyeIndex)
{
    LightGridData Result;

    #if INSTANCED_STEREO
    if (EyeIndex == 0)
    {
        #endif

        Result.LightGridPixelSizeShift = ForwardLightData.CulledGridSize.w;
        Result.LightGridZParams = ForwardLightData.LightGridZParams.xyz;
        Result.CulledGridSize = int3(ForwardLightData.CulledGridSize.xyz);

        #if INSTANCED_STEREO
    }
    else
    {
        Result.LightGridPixelSizeShift = ForwardLightDataISR.LightGridPixelSizeShift;
        Result.LightGridZParams = ForwardLightDataISR.LightGridZParams;
        Result.CulledGridSize = ForwardLightDataISR.CulledGridSize;
    }
        #endif

    return Result;
}


uint ComputeLightGridCellIndex(uint2 PixelPos, float SceneDepth, uint EyeIndex)
{
    const LightGridData GridData = GetLightGridData(EyeIndex);
    uint ZSlice = uint(max(0, log2(SceneDepth * GridData.LightGridZParams.x + GridData.LightGridZParams.y) * GridData.LightGridZParams.z));
    ZSlice = min(ZSlice, uint(GridData.CulledGridSize.z - 1));
    uint3 GridCoordinate = uint3(PixelPos >> GridData.LightGridPixelSizeShift, ZSlice);
    uint GridIndex = (GridCoordinate.z * GridData.CulledGridSize.y + GridCoordinate.y) * GridData.CulledGridSize.x + GridCoordinate.x;
    return GridIndex;
}

uint ComputeLightGridCellIndex(uint2 PixelPos, float SceneDepth)
{
    return ComputeLightGridCellIndex(PixelPos, SceneDepth, 0);
}

#ifndef NUM_CULLED_LIGHTS_GRID_STRIDE
#define NUM_CULLED_LIGHTS_GRID_STRIDE 0
#endif

#ifndef LOCAL_LIGHT_DATA_STRIDE
#define LOCAL_LIGHT_DATA_STRIDE 0
#endif

uint GetNumLocalLights(uint EyeIndex)
{
    #if INSTANCED_STEREO
    return (EyeIndex == 0) ? ForwardLightData.NumLocalLights : ForwardLightDataISR.NumLocalLights;
    #else
    return ForwardLightData.NumLocalLights;
    #endif
}

struct FCulledLightsGridData
{
    uint NumLocalLights;
    uint DataStartIndex;
};

FCulledLightsGridData GetCulledLightsGrid(uint GridIndex, uint EyeIndex)
{
    FCulledLightsGridData Result;

    #if INSTANCED_STEREO
    if (EyeIndex == 0)
    {
        #endif

        Result.NumLocalLights = ForwardLightData.NumLocalLights; //min(ForwardLightData.NumCulledLightsGrid[GridIndex * NUM_CULLED_LIGHTS_GRID_STRIDE + 0], ForwardLightData.NumLocalLights);
        Result.DataStartIndex = 0;  //ForwardLightData.NumCulledLightsGrid[GridIndex * NUM_CULLED_LIGHTS_GRID_STRIDE + 1];

        #if INSTANCED_STEREO
    }
    else
    {
        Result.NumLocalLights = min(ForwardLightDataISR.NumCulledLightsGrid[GridIndex * NUM_CULLED_LIGHTS_GRID_STRIDE + 0], ForwardLightDataISR.NumLocalLights);
        Result.DataStartIndex = ForwardLightDataISR.NumCulledLightsGrid[GridIndex * NUM_CULLED_LIGHTS_GRID_STRIDE + 1];
    }
        #endif

    return Result;
}

struct FDirectionalLightData
{
    uint HasDirectionalLight;
    uint DirectionalLightShadowMapChannelMask;
    float2 DirectionalLightDistanceFadeMAD;
    float3 DirectionalLightColor;
    float3 DirectionalLightDirection;
};

FDirectionalLightData GetDirectionalLightData(uint EyeIndex)
{
    FDirectionalLightData Result;

    #if INSTANCED_STEREO
    if (EyeIndex == 0)
    {
        #endif

        Result.HasDirectionalLight = uint(ForwardLightData.HasDirectionalLight);
        Result.DirectionalLightShadowMapChannelMask = 0; //ForwardLightData.DirectionalLightShadowMapChannelMask;
        Result.DirectionalLightDistanceFadeMAD = ForwardLightData.DirectionalLightDistanceFadeMAD;
        Result.DirectionalLightColor = ForwardLightData.DirectionalLightColor.xyz;
        Result.DirectionalLightDirection = ForwardLightData.DirectionalLightDirection.xyz;

        #if INSTANCED_STEREO
    }
    else
    {
        Result.HasDirectionalLight = ForwardLightDataISR.HasDirectionalLight;
        Result.DirectionalLightShadowMapChannelMask = ForwardLightDataISR.DirectionalLightShadowMapChannelMask;
        Result.DirectionalLightDistanceFadeMAD = ForwardLightDataISR.DirectionalLightDistanceFadeMAD;
        Result.DirectionalLightColor = ForwardLightDataISR.DirectionalLightColor;
        Result.DirectionalLightDirection = ForwardLightDataISR.DirectionalLightDirection;
    }
        #endif

    return Result;
}

struct FLocalLightData
{
    float4 LightPositionAndInvRadius;
    float4 LightColorAndFalloffExponent;
    float4 SpotAnglesAndSourceRadiusPacked;
    float4 LightDirectionAndShadowMask;
    float4 LightTangentAndSoftSourceRadius;
};

FLocalLightData GetLocalLightData(uint GridIndex, uint EyeIndex)
{
    FLocalLightData Result;

    #if INSTANCED_STEREO
    if (EyeIndex == 0)
    {
        #endif

        uint LocalLightIndex = 0; //ForwardLightData.CulledLightDataGrid[GridIndex];
        uint LocalLightBaseIndex = LocalLightIndex * LOCAL_LIGHT_DATA_STRIDE;
        Result.LightPositionAndInvRadius = ForwardLightData.ForwardLocalLightBuffer[LocalLightBaseIndex + 0];
        Result.LightColorAndFalloffExponent = ForwardLightData.ForwardLocalLightBuffer[LocalLightBaseIndex + 1];
        Result.LightDirectionAndShadowMask = ForwardLightData.ForwardLocalLightBuffer[LocalLightBaseIndex + 2];
        Result.SpotAnglesAndSourceRadiusPacked = ForwardLightData.ForwardLocalLightBuffer[LocalLightBaseIndex + 3];
        Result.LightTangentAndSoftSourceRadius = ForwardLightData.ForwardLocalLightBuffer[LocalLightBaseIndex + 4];

        #if INSTANCED_STEREO
    }
    else
    {
        uint LocalLightIndex = ForwardLightDataISR.CulledLightDataGrid[GridIndex];
        uint LocalLightBaseIndex = LocalLightIndex * LOCAL_LIGHT_DATA_STRIDE;
        Result.LightPositionAndInvRadius = ForwardLightDataISR.ForwardLocalLightBuffer[LocalLightBaseIndex + 0];
        Result.LightColorAndFalloffExponent = ForwardLightDataISR.ForwardLocalLightBuffer[LocalLightBaseIndex + 1];
        Result.LightDirectionAndShadowMask = ForwardLightDataISR.ForwardLocalLightBuffer[LocalLightBaseIndex + 2];
        Result.SpotAnglesAndSourceRadiusPacked = ForwardLightDataISR.ForwardLocalLightBuffer[LocalLightBaseIndex + 3];
        Result.LightTangentAndSoftSourceRadius = ForwardLightDataISR.ForwardLocalLightBuffer[LocalLightBaseIndex + 4];
    }
        #endif

    return Result;
}

layout(binding = 4) uniform sampler2D DirectionalLightStaticShadowmap;
layout(binding = 3) uniform sampler2D DirectionalLightShadowmapAtlas;

float ComputeDirectionalLightStaticShadowing(float3 WorldPosition)
{
    float ShadowFactor = 1;
#if ALLOW_STATIC_LIGHTING
    if (ForwardLightData.DirectionalLightUseStaticShadowing)
    {
        // Transform the world position into shadowmap space
        float4 HomogeneousShadowPosition = mul(float4(WorldPosition, 1), ForwardLightData.DirectionalLightWorldToStaticShadow);
        float2 ShadowUVs = HomogeneousShadowPosition.xy / HomogeneousShadowPosition.w;

        // Treat as unshadowed if the voxel is outside of the shadow map
        if (all(greaterThanEqual(ShadowUVs, 0)) && all(lessThanEqual(ShadowUVs, 1)))
        {
#define FILTER_STATIC_SHADOWING 0
#if FILTER_STATIC_SHADOWING
            FPCFSamplerSettings Settings;
//            Settings.ShadowDepthTexture = ForwardLightData.DirectionalLightStaticShadowmap;
//            Settings.ShadowDepthTextureSampler = ForwardLightData.StaticShadowmapSampler;
            Settings.ShadowBufferSize = ForwardLightData.DirectionalLightStaticShadowBufferSize;
            Settings.SceneDepth = HomogeneousShadowPosition.z;
            Settings.TransitionScale = 40;
            Settings.bSubsurface = false;
            // We can sample outside of the static shadowmap, which is centered around the lightmass importance volume.  These 'infinite' depth values should not cause occlusion.
            Settings.bTreatMaxDepthUnshadowed = true;
            Settings.DensityMulConstant = 0;
            Settings.ProjectionDepthBiasParameters = float2(0, 0);

            ShadowFactor = Manual1x1PCF(ShadowUVs, Settings);
#else
            // Sample the shadowmap depth and determine if this voxel is shadowed
            float ShadowDepth = Texture2DSampleLevel(DirectionalLightStaticShadowmap, ShadowUVs, 0).x;
            ShadowFactor = float(HomogeneousShadowPosition.z < ShadowDepth || ShadowDepth > .99f);
#endif
        }
    }
#endif
    return ShadowFactor;
}

#ifndef FILTER_DIRECTIONAL_LIGHT_SHADOWING
#define FILTER_DIRECTIONAL_LIGHT_SHADOWING 1
#endif

float ComputeDirectionalLightDynamicShadowing(float3 WorldPosition, float SceneDepth)
{
    float ShadowFactor = 1;

    if (ForwardLightData.NumDirectionalLightCascades > 0)
    {
        uint CascadeIndex = ForwardLightData.NumDirectionalLightCascades;

        for (uint TestCascadeIndex = 0; TestCascadeIndex < ForwardLightData.NumDirectionalLightCascades; TestCascadeIndex++)
        {
            if (SceneDepth < ForwardLightData.CascadeEndDepths[TestCascadeIndex])
            {
                CascadeIndex = TestCascadeIndex;
                break;
            }
        }

        if (CascadeIndex < ForwardLightData.NumDirectionalLightCascades)
        {
            // Transform the world position into shadowmap space
            float4 HomogeneousShadowPosition = mul(float4(WorldPosition, 1), ForwardLightData.DirectionalLightWorldToShadowMatrix[CascadeIndex]);
            HomogeneousShadowPosition /= HomogeneousShadowPosition.w;
            HomogeneousShadowPosition.xyz = HomogeneousShadowPosition.xyz * 0.5 + 0.5;
//            float2 ShadowUVs = HomogeneousShadowPosition.xy / HomogeneousShadowPosition.w;
            float2 ShadowUVs = HomogeneousShadowPosition.xy;
            float4 ShadowmapMinMax = ForwardLightData.DirectionalLightShadowmapMinMax[CascadeIndex];

            // Treat as unshadowed if the voxel is outside of the shadow map
            if (all(greaterThanEqual(ShadowUVs, ShadowmapMinMax.xy)) && all(lessThanEqual(ShadowUVs, ShadowmapMinMax.zw)))
            {
#if FILTER_DIRECTIONAL_LIGHT_SHADOWING
                FPCFSamplerSettings Settings;
//                Settings.ShadowDepthTexture = ForwardLightData.DirectionalLightShadowmapAtlas;
//                Settings.ShadowDepthTextureSampler = ForwardLightData.ShadowmapSampler;
                Settings.ShadowBufferSize = ForwardLightData.DirectionalLightShadowmapAtlasBufferSize;
                Settings.SceneDepth = HomogeneousShadowPosition.z;
                Settings.TransitionScale = 4000;
                Settings.bSubsurface = false;
                Settings.bTreatMaxDepthUnshadowed = false;
                Settings.DensityMulConstant = 0;
                Settings.ProjectionDepthBiasParameters = float2(0, 0);

                ShadowFactor = Manual1x1PCF(ShadowUVs, Settings);
#else
                // Sample the shadowmap depth and determine if this voxel is shadowed
                float ShadowDepth = Texture2DSampleLevel(DirectionalLightShadowmapAtlas, ShadowUVs, 0).x;
                ShadowFactor = float(HomogeneousShadowPosition.z < ShadowDepth-ForwardLightData.DirectionalLightDirection.w);
#endif
            }
        }
    }

    return ShadowFactor;
}