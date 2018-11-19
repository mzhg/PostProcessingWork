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

/*struct RESOLVE_OUTPUT
{
	float3 color : SV_TARGET0;
	float2 depth : SV_TARGET1;
};*/

layout(location = 0) out float4 OutColor0;
layout(location = 1) out float4 OutColor1;

#if (SAMPLEMODE == SAMPLEMODE_MSAA)
//Texture2DMS<float4> tGodraysBuffer : register(t0);
//Texture2DMS<float> tGodraysDepth : register(t1);
 uniform sampler2DMS tGodraysBuffer;
 uniform sampler2DMS tGodraysDepth;
#elif (SAMPLEMODE == SAMPLEMODE_SINGLE)
//Texture2D<float4> tGodraysBuffer : register(t0);
//Texture2D<float> tGodraysDepth : register(t1);
 uniform sampler2D tGodraysBuffer;
 uniform sampler2D tGodraysDepth;
#endif

#if (defined(__PSSL__) && (SAMPLEMODE == SAMPLEMODE_MSAA))
// Texture2D<int2> tFMask_color : register(t2);
   uniform isampler2D tFMask_color;
#endif

#if defined(__PSSL__)
const int FMASK_UNKNOWN = 1 << 3; // color "unknown" is always represented as high bit in the 4bit fragment index

int2 getFmask(isampler2D tex, int sample_count, int2 coord)
{
	// if 8 or less coverage samples, only load one VGPR (32bits / 4bits per sample)
	// if more than 8 coverage samples, we need to load 2 VGPRs
	int2 fmask;
	if (sample_count <= 8)
	{
//		fmask.x = tex.Load(int3(coord, 0)).x;
		fmask.x = texelFetch(tex, coord, 0).x;
		fmask.y = 0x88888888; // all invalid -- though in theory we shouldn't need to refer to them at all.
	}
	else
	{
//		fmask.xy = tex.Load(int3(coord, 0)).xy;
		fmask.xy = texelFetch(tex, coord, 0).xy;
	}
	return fmask;
}

int getFptr(int index, int2 fmask)
{
	const int     bitShift = 4;     // fmask load always returns a 4bit fragment index (fptr) per coverage sample, regardless of actual number of fragments.
	const int     mask = (1 << bitShift) - 1;
	if (index < 8)
		return (fmask.x >> (index*bitShift)) & mask;
	else
		return (fmask.y >> ((index-8)*bitShift)) & mask;
}
#endif

// RESOLVE_OUTPUT main(VS_QUAD_OUTPUT input)
in float2 vTex;
in float4 vWorldPos;

void main()
{	
	float3 result_color = float3(0.0f);
	float result_depth = 0.0f;
	float result_depth_sqr = 0.0f;

#if (SAMPLEMODE == SAMPLEMODE_MSAA)
//	uint2 buffer_size;
	int buffer_samples = gl_NumSamples;
//	tGodraysBuffer.GetDimensions(buffer_size.x, buffer_size.y, buffer_samples);
	int2 buffer_size = textureSize(tGodraysBuffer);
#elif (SAMPLEMODE == SAMPLEMODE_SINGLE)
	int buffer_samples = 1;
#endif

	int2 base_tc = int2(vTex * g_vViewportSize);
	const float FILTER_SCALE = 1.0f;
	const int KERNEL_WIDTH = 1;
	float total_weight = 0.0f;
//	[unroll]
	for (int ox=-KERNEL_WIDTH; ox<=KERNEL_WIDTH; ++ox)
	{
		if ((base_tc.x + ox) < 0 || (base_tc.x + ox) >= g_vViewportSize.x) continue;

//		[unroll]
		for (int oy=-KERNEL_WIDTH; oy<=KERNEL_WIDTH; ++oy)
		{
			if ((base_tc.y + oy) < 0 || (base_tc.y + oy) >= g_vViewportSize.y) continue;

			int2 offset = int2(ox, oy);
			int2 tc = base_tc + offset;	

#if (defined(__PSSL__) && (SAMPLEMODE == SAMPLEMODE_MSAA))
			int2 fmask = getFmask(tFMask_color, buffer_samples, tc);
#endif

#if (SAMPLEMODE == SAMPLEMODE_MSAA)
			for (uint s=0; s<buffer_samples; ++s)
			{
				float2 so = offset + //tGodraysBuffer.GetSamplePosition(s);
										gl_SamplePosition.xy;
#elif (SAMPLEMODE == SAMPLEMODE_SINGLE)
			{
				float2 so = offset;
#endif
				bool is_valid_sample = false;
#if (SAMPLEMODE == SAMPLEMODE_MSAA)
#	if defined(__PSSL__)
				float3 sample_value = float3(0,0,0);
				float sample_depth = 0.0f;
				int fptr = getFptr(s, fmask);
				if (fptr != FMASK_UNKNOWN)
				{
//					sample_value = tGodraysBuffer.Load(tc, fptr).rgb;
//					sample_depth = tGodraysDepth.Load( tc, fptr ).r;
					sample_value = texelFetch(tGodraysBuffer, tc, fptr).rgb;
					sample_depth = texelFetch(tGodraysDepth , tc, fptr).r;
					is_valid_sample = true;
				}
#	else // !defined(__PSSL__)
				is_valid_sample = true;
				float3 sample_value = //tGodraysBuffer.Load( tc, s ).rgb;
										texelFetch(tGodraysBuffer, tc, s).rgb;
				float sample_depth = // tGodraysDepth.Load( tc, s ).r;
										texelFetch(tGodraysDepth, tc, s).r;
#	endif
#elif (SAMPLEMODE == SAMPLEMODE_SINGLE)
				is_valid_sample = true;
				float3 sample_value = //tGodraysBuffer.Load( int3(tc, 0) ).rgb;
										texelFetch(tGodraysBuffer, tc, 0).rgb;
				float sample_depth = //tGodraysDepth.Load( int3(tc, 0) ).r;
										texelFetch(tGodraysDepth, tc, 0).r;
#endif
				sample_depth = LinearizeDepth(sample_depth, g_fZNear, g_fZFar);
				if (!all(isfinite(sample_value)))
				{
					is_valid_sample = false;
				}

				if (is_valid_sample)
				{
					so *= g_fResMultiplier;
					float weight = GaussianApprox(so, FILTER_SCALE);
					result_color += weight * sample_value;
					result_depth += weight * sample_depth;
					result_depth_sqr += weight * sample_depth*sample_depth;
					total_weight += weight;
				}
			}
		}
	}

//	RESOLVE_OUTPUT output;
	OutColor0.rgb = (total_weight > 0.0f) ? result_color/total_weight : float3(0.f, 0.f, 0.f);
	OutColor0.a = 0.0;
	OutColor1.rg = (total_weight > 0.0f) ? float2(result_depth, result_depth_sqr)/total_weight : float2(1.0);
	OutColor1.ba = float2(0);
//	return output;
}
