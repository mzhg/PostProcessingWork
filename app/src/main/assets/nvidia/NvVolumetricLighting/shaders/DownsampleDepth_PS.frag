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

/*
Define the shader permutations for code generation
%% MUX_BEGIN %%

- SAMPLEMODE:
    - SAMPLEMODE_SINGLE
    - SAMPLEMODE_MSAA

%% MUX_END %%
*/

#include "ShaderCommon.frag"

#if (SAMPLEMODE == SAMPLEMODE_SINGLE)
//Texture2D<float> tDepthMap : register(t0);
uniform sampler2D tDepthMap;
#elif (SAMPLEMODE == SAMPLEMODE_MSAA)
//Texture2DMS<float> tDepthMap : register(t0);
uniform sampler2DMS tDepthMap;
#endif

uniform int sampleIndex = 0;

/*
uint Unused(uint Input)
{
	return Input;
}*/

in float2 vTex;
in float4 vWorldPos;

float2 EvaluateAttributeAtSample(float2 location, int sampleID)
{
	return location;
}

//layout(location = 0) out float4 OutColor;

//float main(
//	VS_QUAD_OUTPUT input
//	, uint sampleID : SV_SAMPLEINDEX 
//	) : SV_DEPTH
void main()
{
	float2 jitter = float2(0.0f, 0.0f);
	uint2 pixelIdx = uint2(gl_FragCoord.xy);
	if (( (pixelIdx.x+pixelIdx.y)%2 ) != 0)
	{
		jitter.xy = g_vJitterOffset.xy;
	}
	else
	{
		jitter.xy = g_vJitterOffset.yx;
	}

#if defined(__PSSL__)
//	Unused(sampleIndex);//Fix a compiler warning with pssl.
	float2 tc = (floor(vTex.xy*g_vOutputViewportSize) + GetViVjLinearSample() + jitter)*g_vOutputSize_Inv;
#else
	float2 tc = (EvaluateAttributeAtSample(vTex.xy, sampleIndex)*g_vOutputViewportSize + jitter)*g_vOutputSize_Inv;
#endif
 
#if (SAMPLEMODE == SAMPLEMODE_SINGLE)
//	return tDepthMap.SampleLevel(sPoint, tc, 0).x;
	gl_FragDepth = textureLod(tDepthMap, tc, 0.0).x;
#elif (SAMPLEMODE == SAMPLEMODE_MSAA)
	int2 load_tc = int2(tc*g_vOutputSize);
//	return tDepthMap.Load(load_tc, 0).x;
	gl_FragDepth = texelFetch(tDepthMap, load_tc, sampleIndex).x;
#endif
//	OutColor.gba = float3(0);

// gl_FragDepth = max(1.0, gl_FragDepth);
}
