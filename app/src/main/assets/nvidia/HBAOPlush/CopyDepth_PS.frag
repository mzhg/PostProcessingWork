/*
#permutation RESOLVE_DEPTH 0 1
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

#if API_GL
#define DepthTexture    g_t0
#define DepthTextureMS  g_t0
#endif

#if RESOLVE_DEPTH
//Texture2DMS<float> DepthTextureMS : register(t0);
layout(binding=0)  uniform sampler2DMS DepthTextureMS;
#else
//Texture2D<float> DepthTexture : register(t0);
layout(binding=0)  uniform sampler2D DepthTexture;
#endif


layout(location = 0) out float OutColor;
in vec2 m_Texcoord;

//----------------------------------------------------------------------------------
//float CopyDepth_PS(PostProc_VSOut IN) : SV_TARGET

//----------------------------------------------------------------------------------

void AddViewportOrigin(inout PostProc_VSOut IN)
{
    IN.pos.xy += g_f2InputViewportTopLeft;
    IN.uv = IN.pos.xy * g_f2InvFullResolution;
}

void main()
{
	PostProc_VSOut IN;
	IN.pos = gl_FragCoord;
	IN.uv = m_Texcoord;
	
    AddViewportOrigin(IN);

#if RESOLVE_DEPTH
    float ViewDepth = // DepthTextureMS.Load(int2(IN.pos.xy), g_iSampleIndex);
    				  texelFetch(DepthTextureMS, ivec2(IN.pos.xy), g_iSampleIndex).x;
#else
    float ViewDepth = // DepthTexture.Load(int3(IN.pos.xy, 0));
    				  texelFetch(DepthTexture, ivec2(IN.pos.xy), 0).x;
#endif

    OutColor=ViewDepth;
}
