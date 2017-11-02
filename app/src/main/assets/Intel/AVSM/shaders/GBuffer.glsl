// Copyright 2012 Intel Corporation
// All Rights Reserved

// GBuffer and related common utilities and structures

#include "Common.glsl"

#ifndef H_GBUFFER
#define H_GBUFFER

struct NONCPUT_UIConstants
{
    uint  faceNormals;
    uint  enableStats;
    uint  volumeShadowMethod;
    uint  enableVolumeShadowLookup;
    uint  pauseParticleAnimaton;
    uint  particleOpacity;
    uint  vertexShaderShadowLookup;
    uint  tessellate;
    uint  wireframe;
    uint  lightingOnly;
    float particleSize;
    float TessellatioDensity;               //  1/desired triangle size
};

layout(binding = 0) uniform PerFrameConstants
{
    /*row_major*/   float4x4    mCameraWorldViewProj;
    /*row_major*/   float4x4    mCameraWorldView;
    /*row_major*/   float4x4    mCameraViewProj;
    /*row_major*/   float4x4    mCameraProj;
                    float4      mCameraPos;
    /*row_major*/   float4x4    mLightWorldViewProj;
    /*row_major*/   float4x4    mAvsmLightWorldViewProj;
    /*row_major*/   float4x4    mCameraViewToLightProj;
    /*row_major*/   float4x4    mCameraViewToLightView;
    /*row_major*/   float4x4    mCameraViewToAvsmLightProj;
    /*row_major*/   float4x4    mCameraViewToAvsmLightView;
                    float4      mLightDir;
                    float4		mScreenResolution;
                    float4      mScreenToViewConsts;

    NONCPUT_UIConstants mUI;
};


// data that we can read or derived from the surface shader outputs
struct SurfaceData
{
    float3 positionView;         // View space position
    float3 positionViewDX;       // Screen space derivatives
    float3 positionViewDY;       // of view space position
    float3 normal;               // View space normal
    float4 albedo;
    float2 lightTexCoord;        // Texture coordinates in light space, [0, 1]
    float2 lightTexCoordDX;      // Screen space partial derivatives
    float2 lightTexCoordDY;      // of light space texture coordinates.
    float  lightSpaceZ;          // Z coordinate (depth) of surface in light space
};

float2 ProjectIntoLightTexCoord(float3 positionView)
{
    float4 positionLight = mul(float4(positionView, 1.0f), mCameraViewToLightProj);
#ifdef DEBUG_DX
    float2 texCoord = (positionLight.xy / positionLight.w) * float2(0.5f, -0.5f) + float2(0.5f, 0.5f);
#else
    float2 texCoord = (positionLight.xy / positionLight.w) * float2(0.5f, +0.5f) + float2(0.5f, 0.5f);
#endif
    return texCoord;
}

float2 ProjectIntoAvsmLightTexCoord(float3 positionView)
{
    float4 positionLight = mul(float4(positionView, 1.0f), mCameraViewToAvsmLightProj);
#ifdef DEBUG_DX
    float2 texCoord = (positionLight.xy / positionLight.w) * float2(0.5f, -0.5f) + float2(0.5f, 0.5f);
#else
    float2 texCoord = (positionLight.xy / positionLight.w) * float2(0.5f, +0.5f) + float2(0.5f, 0.5f);
#endif
    return texCoord;
}

#endif // H_GBUFFER
