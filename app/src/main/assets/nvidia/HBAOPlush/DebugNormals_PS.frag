/*
#permutation FETCH_GBUFFER_NORMAL 0 1 2
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

#include "ReconstructNormal_Common.glsl"

//----------------------------------------------------------------------------------
float3 FetchFullResViewNormal(PostProc_VSOut IN, float3 ViewPosition)
{
#if FETCH_GBUFFER_NORMAL
    return FetchFullResViewNormal_GBuffer(IN);
#else
    return ReconstructNormal(IN.uv, ViewPosition);
#endif
}

void SubtractViewportOrigin(inout PostProc_VSOut IN)
{
    IN.pos.xy -= g_f2InputViewportTopLeft;
    IN.uv = IN.pos.xy * g_f2InvFullResolution;
}

layout(location = 0) out float4 OutColor;
in vec2 m_Texcoord;

//----------------------------------------------------------------------------------
//float4 DebugNormals_PS(PostProc_VSOut IN) : SV_TARGET
void main()
{
	PostProc_VSOut IN;
	IN.pos = gl_FragCoord;
	IN.uv = m_Texcoord;
	
    SubtractViewportOrigin(IN);

    float3 ViewPosition = FetchFullResViewPos(IN.uv);
    float3 ViewNormal = -FetchFullResViewNormal(IN, ViewPosition);

    OutColor.rgb = 
        (g_iDebugNormalComponent == 0) ? ViewNormal.xxx :
        (g_iDebugNormalComponent == 1) ? ViewNormal.yyy :
        (g_iDebugNormalComponent == 2) ? ViewNormal.zzz :
        ViewNormal;

//    return float4(OutColor, 0);
	OutColor.a = 0.0;
}
