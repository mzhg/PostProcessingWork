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
    return ReconstructNormal(IN.uv, ViewPosition);
}

//----------------------------------------------------------------------------------
//float4 ReconstructNormal_PS(PostProc_VSOut IN) : SV_TARGET
layout(location = 0) out float4 OutColor;

void main()
{
	PostProc_VSOut IN;
	IN.pos = gl_FragCoord;
	IN.uv = vec2(0);
	
    float3 ViewPosition = FetchFullResViewPos(IN.uv);
    float3 ViewNormal = FetchFullResViewNormal(IN, ViewPosition);

    OutColor = float4(ViewNormal * 0.5 + 0.5, 0);
}
