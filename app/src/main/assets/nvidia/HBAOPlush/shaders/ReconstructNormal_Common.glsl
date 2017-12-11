/*
* Copyright (c) 2008-2016, NVIDIA CORPORATION. All rights reserved.
*
* NVIDIA CORPORATION and its licensors retain all intellectual property
* and proprietary rights in and to this software, related documentation
* and any modifications thereto. Any use, reproduction, disclosure or
* distribution of this software and related documentation without an express
* license agreement from NVIDIA CORPORATION is strictly prohibited.
*/

#include "ConstantBuffers.glsl"

#if API_GL
#define FullResNormalTexture    g_t1
#define FullResNormalTextureMS  g_t1
#endif

#if FETCH_GBUFFER_NORMAL == 2
//Texture2DMS<float3> FullResNormalTextureMS  : register(t1);
layout(binding = 1) uniform sampler2DMS FullResNormalTextureMS;
#else
//Texture2D<float3> FullResNormalTexture      : register(t1);
layout(binding = 1) uniform sampler2D FullResNormalTexture;
#endif

//----------------------------------------------------------------------------------
float3 FetchFullResWorldNormal_GBuffer(PostProc_VSOut IN)
{
    AddViewportOrigin(IN);

#if FETCH_GBUFFER_NORMAL == 2
    return texelFetch(FullResNormalTextureMS, int2(IN.pos.xy), g_iSampleIndex).xyz;
#else
    return texelFetch(FullResNormalTexture, int2(IN.pos.xy), 0).xyz;
#endif
}

//----------------------------------------------------------------------------------
float3 FetchFullResViewNormal_GBuffer(PostProc_VSOut IN)
{
    float3 WorldNormal = FetchFullResWorldNormal_GBuffer(IN) * g_fNormalDecodeScale + g_fNormalDecodeBias;
    float3 ViewNormal = normalize(float3x3(g_f44NormalMatrix) * WorldNormal);
    return ViewNormal;
}

// Disabled as a WAR for nvbug 1370844
#define USE_GATHER4 0

#if API_GL
#define FullResDepthTexture g_t0
#endif

#if 0
Texture2D<float>  FullResDepthTexture       : register(t0);
sampler           PointClampSampler         : register(s0);
#else
layout(binding = 0) uniform sampler2D FullResDepthTexture;
#endif

//----------------------------------------------------------------------------------
float3 UVToView(float2 UV, float viewDepth)
{
    UV = g_f2UVToViewA * UV + g_f2UVToViewB;
    return float3(UV * viewDepth, viewDepth);
}

//----------------------------------------------------------------------------------
float3 FetchFullResViewPos(float2 UV)
{
#if 0
    float ViewDepth = FullResDepthTexture.SampleLevel(PointClampSampler, UV, 0);
#else
    float ViewDepth = textureLod(FullResDepthTexture, UV, 0.).x;
#endif
    return UVToView(UV, ViewDepth);
}

//----------------------------------------------------------------------------------
float3 MinDiff(float3 P, float3 Pr, float3 Pl)
{
    float3 V1 = Pr - P;
    float3 V2 = P - Pl;
    return (dot(V1,V1) < dot(V2,V2)) ? V1 : V2;
}

//----------------------------------------------------------------------------------
#if USE_GATHER4
float4 GatherR4(Texture2D<float> Texture, float2 UV, int2 o0, int2 o1, int2 o2, int2 o3)
{
    return float4(
        textureLod(Texture, UV, 0, o0).x,    // PointClampSampler
        textureLod(Texture, UV, 0, o1).x,
        textureLod(Texture, UV, 0, o2).x,
        textureLod(Texture, UV, 0, o3).x
        );
}
#endif

//----------------------------------------------------------------------------------
float3 ReconstructNormal(float2 UV, float3 P)
{
#if USE_GATHER4
    float4 S = GatherR4(FullResDepthTexture, UV, int2(1,0), int2(-1,0), int2(0,1), int2(0,-1));
    float3 Pr = UVToView(float2(UV.x + g_f2InvFullResolution.x, UV.y), S.x);
    float3 Pl = UVToView(float2(UV.x - g_f2InvFullResolution.x, UV.y), S.y);
    float3 Pt = UVToView(float2(UV.x, UV.y + g_f2InvFullResolution.y), S.z);
    float3 Pb = UVToView(float2(UV.x, UV.y - g_f2InvFullResolution.y), S.w);
#else
    float3 Pr = FetchFullResViewPos(UV + float2(g_f2InvFullResolution.x, 0));
    float3 Pl = FetchFullResViewPos(UV + float2(-g_f2InvFullResolution.x, 0));
    float3 Pt = FetchFullResViewPos(UV + float2(0, g_f2InvFullResolution.y));
    float3 Pb = FetchFullResViewPos(UV + float2(0, -g_f2InvFullResolution.y));
#endif
    return normalize(cross(MinDiff(P, Pr, Pl), MinDiff(P, Pt, Pb)));
}
