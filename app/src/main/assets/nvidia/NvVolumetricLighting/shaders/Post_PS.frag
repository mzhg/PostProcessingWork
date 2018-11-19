// This code contains NVIDIA Confidential Information and is disclosed 
// under the Mutual Non-Disclosure Agreement. 
// 
// Notice 
// ALL NVIDIA DESIGN SPECIFICATIONS AND CODE ("MATERIALS") ARE PROVIDED "AS IS" NVIDIA MAKES 
// NO REPRESENTATIONS, WARRANTIES, EXPRESSED, IMPLIED, STATUTORY, OR OTHERWISE WITH RESPECT TO 
// THE MATERIALS, AND EXPRESSLY DISCLAIMS ANY IMPLIED WARRANTIES OF NONINFRINGEMENT, 
// MERCHANTABILITY, OR FITNESS FOR A PARTICULAR PURPOSE. 
// 
// NVIDIA Corporation assumes no responsibility for the consequences of use of such 
// information or for any infringement of patents or other rights of third parties that may 
// result from its use. No license is granted by implication or otherwise under any patent 
// or patent rights of NVIDIA Corporation. No third party distribution is allowed unless 
// expressly authorized by NVIDIA.  Details are subject to change without notice. 
// This code supersedes and replaces all information previously supplied. 
// NVIDIA Corporation products are not authorized for use as critical 
// components in life support devices or systems without express written approval of 
// NVIDIA Corporation. 
// 
// Copyright (c) 2003 - 2016 NVIDIA Corporation. All rights reserved.
//
// NVIDIA Corporation and its licensors retain all intellectual property and proprietary
// rights in and to this software and related documentation and any modifications thereto.
// Any use, reproduction, disclosure or distribution of this software and related
// documentation without an express license agreement from NVIDIA Corporation is
// strictly prohibited.
//
#include "../../../shader_libs/PostProcessingHLSLCompatiable.glsl"

in float2 vTex;
layout(location = 0) out float4 OutColor;

layout(binding = 0) uniform sampler2D tScene;

vec3 tonemap(vec3 C)
{
	// Filmic -- model film properties
	C = max(vec3(0), C - 0.004);
	return (C*(6.2*C+0.5))/(C*(6.2*C+1.7)+0.06);
}

// Texture2DMS<float4> tScene : register(t0);

//float4 main(VS_QUAD_OUTPUT input, uint sampleID : SV_SAMPLEINDEX) : SV_Target0
void main()
{
//    float3 output = float3(0, 0, 0);
//   float3 s_hdr = tScene.Load(int2(input.vPos.xy), sampleID).rgb;
//    output = tonemap(s_hdr);
//    return float4(output, 1);
	int sampleID = /*gl_SampleID*/ 0;
	float3 s_hdr = texelFetch(tScene, int2(gl_FragCoord.xy), sampleID).rgb;
	OutColor.rgb = tonemap(s_hdr);
	OutColor.a = 1.0;
}