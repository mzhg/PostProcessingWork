/* 
* Copyright (c) 2008-2017, NVIDIA CORPORATION. All rights reserved. 
* 
* NVIDIA CORPORATION and its licensors retain all intellectual property 
* and proprietary rights in and to this software, related documentation 
* and any modifications thereto. Any use, reproduction, disclosure or 
* distribution of this software and related documentation without an express 
* license agreement from NVIDIA CORPORATION is strictly prohibited. 
*/

#include "ConstantBuffers.glsl"

// Do not use gather instructions on GL to support Core 3.2
#define USE_GATHER4 !API_GL

#define MRT_COUNT MAX_NUM_MRTS

#if API_GL
#define DepthTexture g_t0
#endif

//Texture2D<float> DepthTexture   : register(t0);
//sampler PointClampSampler       : register(s0);
layout(binding = 0) uniform sampler2D DepthTexture;

//----------------------------------------------------------------------------------
#if 0
struct PSOutputDepthTextures
{
    float Z00 : SV_Target0;
    float Z10 : SV_Target1;
    float Z20 : SV_Target2;
    float Z30 : SV_Target3;
#if MRT_COUNT == 8
    float Z01 : SV_Target4;
    float Z11 : SV_Target5;
    float Z21 : SV_Target6;
    float Z31 : SV_Target7;
#endif
};
#endif

layout(location = 0) out float Z00;
layout(location = 1) out float Z10;
layout(location = 2) out float Z20;
layout(location = 3) out float Z30;
#if MRT_COUNT == 8
layout(location = 4) out float Z01;
layout(location = 5) out float Z11;
layout(location = 6) out float Z21;
layout(location = 7) out float Z31;
#endif

#if USE_GATHER4

//----------------------------------------------------------------------------------
//PSOutputDepthTextures DeinterleaveDepth_PS(PostProc_VSOut IN)
void main()
{
//    PSOutputDepthTextures OUT;
	PostProc_VSOut IN;
    IN.pos.xy = floor(gl_FragCoord.xy) * 4.0 + (g_PerPassConstants.f2Offset + 0.5);
    IN.uv = IN.pos.xy * g_f2InvFullResolution;

    // Gather sample ordering: (-,+),(+,+),(+,-),(-,-),
//    float4 S0 = DepthTexture.GatherRed(PointClampSampler, IN.uv);
//    float4 S1 = DepthTexture.GatherRed(PointClampSampler, IN.uv, int2(2,0));
	float4 S0 = textureGather(DepthTexture, IN.uv);
	float4 S1 = textureGatherOffset(DepthTexture, IN.uv, int2(2,0));
	
    Z00 = S0.w;
    Z10 = S0.z;
    Z20 = S1.w;
    Z30 = S1.z;

#if MRT_COUNT == 8
    Z01 = S0.x;
    Z11 = S0.y;
    Z21 = S1.x;
    Z31 = S1.y;
#endif
}

#else

//----------------------------------------------------------------------------------
//PSOutputDepthTextures DeinterleaveDepth_PS(PostProc_VSOut IN)
void main()
{
	PostProc_VSOut IN;
    IN.pos.xy = floor(gl_FragCoord.xy) * 4.0 + g_PerPassConstants.f2Offset;
    IN.uv = IN.pos.xy * g_f2InvFullResolution;

//    PSOutputDepthTextures OUT;

    Z00 = texture(DepthTexture, IN.uv);   // PointClampSampler
    Z10 = textureOffset(DepthTexture, IN.uv, int2(1,0));
    Z20 = textureOffset(DepthTexture, IN.uv, int2(2,0));
    Z30 = textureOffset(DepthTexture, IN.uv, int2(3,0));

#if MRT_COUNT == 8
    Z01 = textureOffset(DepthTexture, IN.uv, int2(0,1));
    Z11 = textureOffset(DepthTexture, IN.uv, int2(1,1));
    Z21 = textureOffset(DepthTexture, IN.uv, int2(2,1));
    Z31 = textureOffset(DepthTexture, IN.uv, int2(3,1));
#endif

//    return OUT;
}

#endif
