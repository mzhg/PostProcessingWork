/*
#permutation ENABLE_BLUR 0 1
*/

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

#define USE_INTEGER_MATH !API_GL

#if API_GL
#define AOTexture       g_t0
#define DepthTexture    g_t1
#endif
/*
Texture2DArray<float> AOTexture     : register(t0);
Texture2D<float> DepthTexture       : register(t1);
sampler PointSampler                : register(s0);
*/
layout(binding=0) uniform sampler2DArray AOTexture;
layout(binding=1) uniform sampler2D 	 DepthTexture;

//----------------------------------------------------------------------------------
/*
struct PSOut
{
#if ENABLE_BLUR
    float2 AOZ  : SV_TARGET;
#else
    float4 AO   : SV_TARGET;
#endif
};
*/

layout(location = 0) out float4 OutColor;

void SubtractViewportOrigin(inout PostProc_VSOut IN)
{
    IN.pos.xy -= g_f2InputViewportTopLeft;
	float2 relativeFragCoord = gl_FragCoord.xy - g_f2InputViewportTopLeft;
    IN.uv = relativeFragCoord * g_f2InvFullResolution;
}

//-------------------------------------------------------------------------
// PSOut ReinterleaveAO_PS(PostProc_VSOut IN)
void main()
{
//    PSOut OUT;
	PostProc_VSOut IN;
	IN.pos = gl_FragCoord;
	IN.uv = vec2(0);
	
//#if !ENABLE_BLUR
    SubtractViewportOrigin(IN);
//#endif
    vec2 pos = gl_FragCoord.xy - g_f2InputViewportTopLeft;

#if USE_INTEGER_MATH
    int2 FullResPos = int2(pos.xy);
    int2 Offset = FullResPos & 3;
    int SliceId = Offset.y * 4 + Offset.x;
    int2 QuarterResPos = FullResPos >> 2;
#else
    float2 FullResPos = floor(pos.xy);
    float2 Offset = fmod(abs(FullResPos), float2(4,4));
    float SliceId = Offset.y * 4.0 + Offset.x;
    float2 QuarterResPos = FullResPos / 4.0;
#endif

#if ENABLE_BLUR
//    float AO = AOTexture.Load(int4(QuarterResPos, SliceId, 0));
	float AO = texelFetch(AOTexture, int3(QuarterResPos, SliceId), 0).r;
    float ViewDepth = //DepthTexture.Sample(PointSampler, IN.uv);
    					texture(DepthTexture, IN.uv).r;
    OutColor.xy = float2(AO, ViewDepth);
    OutColor.zw = float2(0);
#else
    float4 AO = //AOTexture.Load(int4(QuarterResPos, SliceId, 0));
    			texelFetch(AOTexture, int3(QuarterResPos, SliceId), 0);
    OutColor = float4(pow(saturate(AO.x), g_fPowExponent));
#endif

//    return OUT;
}
