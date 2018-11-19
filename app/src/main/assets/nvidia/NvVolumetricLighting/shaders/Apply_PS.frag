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

- UPSAMPLEMODE:
	- UPSAMPLEMODE_POINT
	- UPSAMPLEMODE_BILINEAR
	- UPSAMPLEMODE_BILATERAL

- FOGMODE:
	- FOGMODE_NONE
	- FOGMODE_NOSKY
	- FOGMODE_FULL

%% MUX_END %%
*/

#include "ShaderCommon.frag"

uniform sampler2D tGodraysBuffer;
#if (SAMPLEMODE == SAMPLEMODE_MSAA)
	uniform sampler2DMS tSceneDepth;
#elif (SAMPLEMODE == SAMPLEMODE_SINGLE)
	uniform sampler2D tSceneDepth;
#endif

uniform sampler2D tGodraysDepth;
uniform sampler2D tPhaseLUT;

in float2 vTex;
in float4 vWorldPos;

layout(location = 0) out float4 inscatter;
layout(location = 1) out float4 transmission;

float3 Tonemap(float3 s)
{
	return s / (float3(1,1,1) + s);
}

float3 Tonemap_Inv(float3 s)
{
	return s / (float3(1,1,1) - s);
}

float CalcVariance(float x, float x_sqr)
{
	return abs(x_sqr - x*x);
}

void main()
{
	transmission = float4(1,1,1,1);
	inscatter = float4(0,0,0,1);
	
	float2 texcoord = vTex * g_vViewportSize * g_vBufferSize_Inv;
	
#if (SAMPLEMODE == SAMPLEMODE_MSAA)
//	float scene_depth = tSceneDepth.Load(int2(input.vTex*g_vOutputViewportSize), sampleID).x;
	float scene_depth = texelFetch(tSceneDepth, int2(vTex*g_vOutputViewportSize), gl_SampleID).r;
#elif (SAMPLEMODE == SAMPLEMODE_SINGLE)
//	float scene_depth = tSceneDepth.SampleLevel(sPoint, input.vTex * g_vViewportSize * g_vBufferSize_Inv, 0).x;
	float scene_depth = textureLod(tSceneDepth, texcoord, 0.0).x;
#endif
	scene_depth = LinearizeDepth(scene_depth, g_fZNear, g_fZFar);
	
	// Quality of the upsampling interpolator
	// 0: Point (no up-sample)
	// 1: Bilinear
	// 2: Bilateral
	float3 inscatter_sample = float3(0,0,0);
	if (UPSAMPLEMODE == UPSAMPLEMODE_POINT || UPSAMPLEMODE == UPSAMPLEMODE_BILINEAR )
	{
		inscatter_sample = // tGodraysBuffer.SampleLevel( sPoint, texcoord, 0).rgb;
							textureLod(tGodraysBuffer, texcoord, 0.0).rgb;
	}
//	else if (UPSAMPLEMODE == UPSAMPLEMODE_BILINEAR)
//	{
//		inscatter_sample = tGodraysBuffer.SampleLevel( sBilinear, texcoord, 0).rgb;
//	}
	else if (UPSAMPLEMODE == UPSAMPLEMODE_BILATERAL)
	{
		const float2 NEIGHBOR_OFFSETS[9] = float2[9](
			float2(-1, -1),	float2( 0, -1),	float2( 1, -1),
			float2(-1,  0),	float2( 0,  0),	float2( 1,  0),
			float2(-1,  1),	float2( 0,  1),	float2( 1,  1)
		);
		const float GAUSSIAN_WIDTH = 1.0f;

		float2 max_dimensions = floor(g_vViewportSize);
		float2 base_tc = vTex * max_dimensions;

		float total_weight = 0;
//		[unroll]
		for (int n=0; n<9; ++n)
		{
			float2 sample_tc = max( float2(0,0), min(max_dimensions, base_tc + NEIGHBOR_OFFSETS[n]));

			float weight = 0.0f;		
			float2 sample_location = floor(sample_tc) + float2(0.5f, 0.5f);
			weight = GaussianApprox(sample_location - base_tc, GAUSSIAN_WIDTH);

			const float DEPTH_RANGE = 0.10f;

			float2 neighbor_depth = //tGodraysDepth.Load(int3(sample_location.xy, 0)).rg;
									  texelFetch(tGodraysDepth, int2(sample_location), 0).rg;
			float depth_diff = abs(scene_depth - neighbor_depth.r);
			float neighbor_variance = CalcVariance(neighbor_depth.r, neighbor_depth.g);
			float neighbor_stddev = sqrt(neighbor_variance);
			float depth_weight = saturate(1 - depth_diff / DEPTH_RANGE);
			depth_weight = depth_weight*depth_weight*(1-neighbor_stddev);
			weight *= depth_weight;

			inscatter_sample += weight * Tonemap(
								//tGodraysBuffer.Load(int3(sample_location.xy, 0)).rgb
								  texelFetch(tGodraysBuffer, int2(sample_location), 0).rgb
								);
			total_weight += weight;
		}

		if (total_weight > 0.0f)
		{
			inscatter_sample = Tonemap_Inv(inscatter_sample / total_weight);
		}
		else
		{
			inscatter_sample = // tGodraysBuffer.SampleLevel(sBilinear, texcoord, 0).rgb;
								textureLod(tGodraysBuffer, texcoord, 0.0).rgb;
		}
	}

	inscatter.rgb = inscatter_sample.rgb;
	if (FOGMODE != FOGMODE_NONE)
	{
        if ((FOGMODE != FOGMODE_NOSKY) || (scene_depth < 1.f))
		{
			float scene_distance = g_fZFar * scene_depth;
            float3 sigma_ext = g_vSigmaExtinction;
			inscatter.rgb += g_fMultiScattering * g_vFogLight * g_vScatterPower * (1.0-exp(-sigma_ext*scene_distance)) / sigma_ext;
			transmission.rgb = exp(-sigma_ext*scene_distance);
		}
	}
}