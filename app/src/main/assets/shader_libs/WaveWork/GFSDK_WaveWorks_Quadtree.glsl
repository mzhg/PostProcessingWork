/*
 * This code contains NVIDIA Confidential Information and is disclosed
 * under the Mutual Non-Disclosure Agreement.
 *
 * Notice
 * ALL NVIDIA DESIGN SPECIFICATIONS AND CODE ("MATERIALS") ARE PROVIDED "AS IS" NVIDIA MAKES
 * NO REPRESENTATIONS, WARRANTIES, EXPRESSED, IMPLIED, STATUTORY, OR OTHERWISE WITH RESPECT TO
 * THE MATERIALS, AND EXPRESSLY DISCLAIMS ANY IMPLIED WARRANTIES OF NONINFRINGEMENT,
 * MERCHANTABILITY, OR FITNESS FOR A PARTICULAR PURPOSE.
 *
 * NVIDIA Corporation assumes no responsibility for the consequences of use of such
 * information or for any infringement of patents or other rights of third parties that may
 * result from its use. No license is granted by implication or otherwise under any patent
 * or patent rights of NVIDIA Corporation. No third party distribution is allowed unless
 * expressly authorized by NVIDIA.  Details are subject to change without notice.
 * This code supersedes and replaces all information previously supplied.
 * NVIDIA Corporation products are not authorized for use as critical
 * components in life support devices or systems without express written approval of
 * NVIDIA Corporation.
 *
 * Copyright ?2008- 2013 NVIDIA Corporation. All rights reserved.
 *
 * NVIDIA Corporation and its licensors retain all intellectual property and proprietary
 * rights in and to this software and related documentation and any modifications thereto.
 * Any use, reproduction, disclosure or distribution of this software and related
 * documentation without an express license agreement from NVIDIA Corporation is
 * strictly prohibited.
 */

#include "../PostProcessingHLSLCompatiable.glsl"

#define MAX_FFT_RESOLUTION 512
#define WARP_WIDTH 8 // minimum number of threads which execute in lockstep

/*
 *
 *
 */

#if defined(GFSDK_WAVEWORKS_SM3) || defined(GFSDK_WAVEWORKS_GL)
	#define GFSDK_WAVEWORKS_BEGIN_GEOM_VS_CBUFFER(Label)
	#define GFSDK_WAVEWORKS_END_GEOM_VS_CBUFFER
#endif

#if defined( GFSDK_WAVEWORKS_USE_TESSELLATION )
//	GFSDK_WAVEWORKS_BEGIN_GEOM_HS_CBUFFER(eyepos_buffer)
//	GFSDK_WAVEWORKS_DECLARE_GEOM_HS_CONSTANT(float4, g_hsWorldEye, 0)
//	GFSDK_WAVEWORKS_DECLARE_GEOM_HS_CONSTANT(float4, g_tessellationParams, 1)
//	GFSDK_WAVEWORKS_END_GEOM_HS_CBUFFER

    layout(binding = 2) uniform eyepos_buffer
    {
        float4 g_hsWorldEye;
        float4 g_tessellationParams;
    };
#endif

#if 0
GFSDK_WAVEWORKS_BEGIN_GEOM_VS_CBUFFER(geom_buffer)
GFSDK_WAVEWORKS_DECLARE_GEOM_VS_CONSTANT(float4x3, g_matLocalWorld, 0)
GFSDK_WAVEWORKS_DECLARE_GEOM_VS_CONSTANT(float4, g_vsEyePos, 3)
GFSDK_WAVEWORKS_DECLARE_GEOM_VS_CONSTANT(float4, g_MorphParam, 4)
GFSDK_WAVEWORKS_END_GEOM_VS_CBUFFER
#endif

layout(binding = 3) uniform geom_buffer
{
   float4x4  g_matLocalWorld;
   float4    g_vsEyePos;
   float4    g_MorphParam;
};

/*
struct GFSDK_WAVEWORKS_VERTEX_INPUT
{
	float4 vPos SEMANTIC(POSITION);
};
*/

#if !defined(GFSDK_WAVEWORKS_USE_TESSELLATION)
float3 GFSDK_WaveWorks_GetUndisplacedVertexWorldPosition(float4 In_vPos)
{
	float2 vpos = In_vPos.xy;

	// Use multiple levels of geo-morphing to smooth away LOD boundaries
	float geomorph_scale = 0.25f;

	float2 geomorph_offset = float2(g_MorphParam.w,g_MorphParam.w);
	float2 vpos_src = vpos;
	float2 vpos_target = vpos_src;
	float geomorph_amount = 0.f;

	for(int geomorph_level = 0; geomorph_level != 4; ++geomorph_level) {

		float2 intpart;
		float2 rempart = modf(geomorph_scale*vpos_src.xy,intpart);

		float2 mirror = float2(1.0f, 1.0f);

		if(rempart.x >  0.5f)
		{
			rempart.x = 1.0f - rempart.x;
			mirror.x = -mirror.x;
		}
		if(rempart.y >  0.5f)
		{
			rempart.y = 1.0f - rempart.y;
			mirror.y = -mirror.y;
		}


		if(0.25f == rempart.x && 0.25f == rempart.y) vpos_target.xy = vpos_src.xy - geomorph_offset*mirror;
		else if(0.25f == rempart.x) vpos_target.x = vpos_src.x + geomorph_offset.x*mirror.x;
		else if(0.25f == rempart.y) vpos_target.y = vpos_src.y + geomorph_offset.y*mirror.y;

		float3 eyevec = mul(float4(vpos_target,0.f,1.f), g_matLocalWorld).xyz - g_vsEyePos.xyz;
		float d = length(eyevec);
		float geomorph_target_level = log2(d * g_MorphParam.x) + 1.f;
		geomorph_amount = saturate(2.0*(geomorph_target_level - float(geomorph_level)));
		if(geomorph_amount < 1.f)
		{
			break;
		}
		else
		{
			vpos_src = vpos_target;
			geomorph_scale *= 0.5f;
			geomorph_offset *= -2.f;
		}
	}

	vpos.xy = lerp(vpos_src, vpos_target, geomorph_amount);
	return mul(float4(vpos,In_vPos.zw), g_matLocalWorld).xyz;
}
#endif


#if defined(GFSDK_WAVEWORKS_USE_TESSELLATION)
float3 GFSDK_WaveWorks_GetUndisplacedVertexWorldPosition(float4 In_vPos)
{
	float2 vpos = In_vPos.xy;
	// Use multiple levels of geo-morphing to smooth away LOD boundaries
	float geomorph_scale = 0.5f;
	float geomorph_offset = abs(g_MorphParam.w);
	float2 vpos_src = vpos;
	float2 vpos_target = vpos_src;
	float geomorph_amount = 0.f;

	//vpos_target.x += 0.25*geomorph_offset;
	//vpos_src.x += 0.25*geomorph_offset;

	for(int geomorph_level = 0; geomorph_level != 4; ++geomorph_level) {

		float2 intpart;
		float2 rempart = modf(geomorph_scale*vpos_src.xy,intpart);
		if(0.5f == rempart.x)
		{
			vpos_target.x = vpos_src.x + geomorph_offset;
		}

		if(0.5f == rempart.y)
		{
			vpos_target.y = vpos_src.y + geomorph_offset;
		}

		float3 eyevec = mul(float4(vpos_target,0.f,1.f), g_matLocalWorld).xyz - g_vsEyePos.xyz;
		float d = length(eyevec);
		float geomorph_target_level = log2(d * g_MorphParam.x) + 1.f;
		geomorph_amount = saturate(3.0*(geomorph_target_level - float(geomorph_level)));
		if(geomorph_amount < 1.f) {
			break;
		} else {
			vpos_src = vpos_target;
			geomorph_scale *= 0.5f;
			geomorph_offset *= 2.f;
		}
	}
	vpos.xy = lerp(vpos_src, vpos_target, geomorph_amount);
	return mul(float4(vpos,In_vPos.zw), g_matLocalWorld).xyz;
}

float GFSDK_WaveWorks_GetEdgeTessellationFactor(float4 vertex1, float4 vertex2)
{
	float3 edge_center = 0.5*(vertex1.xyz + vertex2.xyz);
	float edge_length = length (vertex1.xyz - vertex2.xyz);
	float edge_distance = length(g_hsWorldEye.xyz - edge_center.xyz);
	return g_tessellationParams.x * edge_length / edge_distance;
}

float GFSDK_WaveWorks_GetVertexTargetTessellatedEdgeLength(float3 vertex)
{
	float vertex_distance = length(g_hsWorldEye.xyz - vertex.xyz);
	return vertex_distance / g_tessellationParams.x;
}

#endif

