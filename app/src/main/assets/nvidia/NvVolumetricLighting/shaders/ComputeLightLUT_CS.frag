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

- LIGHTMODE:
    - LIGHTMODE_OMNI
    - LIGHTMODE_SPOTLIGHT

- ATTENUATIONMODE:
    - ATTENUATIONMODE_NONE
    - ATTENUATIONMODE_POLYNOMIAL
    - ATTENUATIONMODE_INV_POLYNOMIAL

- COMPUTEPASS:
    - COMPUTEPASS_CALCULATE
    - COMPUTEPASS_SUM

%% MUX_END %%
*/

#include "ShaderCommon.frag"

float4 PackLut(float3 v, float s)
{
    return float4(v/s, s);
}

float3 UnpackLut(float4 v)
{
    return v.rgb*v.a;
}

//Texture2D<float4> tPhaseLUT : register(t4);
//RWTexture2D<float4> rwLightLUT_P : register(u0);
//RWTexture2D<float4> rwLightLUT_S1 : register(u1);
//RWTexture2D<float4> rwLightLUT_S2 : register(u2);

uniform sampler2D tPhaseLUT;
layout (rgba16f, binding = 0) uniform image2D rwLightLUT_P;
layout (rgba16f, binding = 1) uniform image2D rwLightLUT_S1;
layout (rgba16f, binding = 2) uniform image2D rwLightLUT_S2;

// These need to match the values in context_common.h
const uint LIGHT_LUT_DEPTH_RESOLUTION = 128;
const uint LIGHT_LUT_WDOTV_RESOLUTION = 512;

#if (COMPUTEPASS == COMPUTEPASS_CALCULATE)

#define BLOCK_SIZE int2(32, 8)
shared float3 sAccum_P[BLOCK_SIZE.x*BLOCK_SIZE.y];

#if (LIGHTMODE == LIGHTMODE_SPOTLIGHT)
shared float3 sAccum_S1[BLOCK_SIZE.x*BLOCK_SIZE.y];
shared float3 sAccum_S2[BLOCK_SIZE.x*BLOCK_SIZE.y];
#endif

//[numthreads( BLOCK_SIZE.x, BLOCK_SIZE.y, 1 )]
layout (local_size_x = 32, local_size_y = 8) in;
//void main(uint3 gthreadID : SV_GroupThreadID, uint2 dispatchID : SV_DispatchThreadID, uint2 groupID : SV_GroupID)
void main()
{
	uvec3 gthreadID = gl_LocalInvocationID;
	uvec2 dispatchID = gl_GlobalInvocationID.xy;
	uint2 groupID = gl_WorkGroupID.xy;
	
    uint idx = gthreadID.y*BLOCK_SIZE.x + gthreadID.x;
    float2 coord = float2(dispatchID) / float2(LIGHT_LUT_DEPTH_RESOLUTION-1, LIGHT_LUT_WDOTV_RESOLUTION-1);

    float angle = coord.y * PI;
    float cos_WV = -cos(angle);

    float3 vW = g_vEyePosition - g_vLightPos;
    float Wsqr = dot(vW, vW);
    float W_length = sqrt(Wsqr);
    float t0 = max(0.0f, W_length-g_fLightZFar);
    float t_range = g_fLightZFar + W_length - t0;
    float t = t0 + coord.x*t_range;

    float WdotV = cos_WV*W_length;
    float Dsqr = max(Wsqr+2*WdotV*t+t*t, 0.0f);
    float D = sqrt(Dsqr);
    float cos_phi = (t>0.0 && D>0.0) ? (t*t + Dsqr - Wsqr) / (2.0 * t*D) : cos_WV;
    float3 extinction = exp(-g_vSigmaExtinction*(D+t));
    float3 phase_factor = GetPhaseFactor(tPhaseLUT, -cos_phi);
    float attenuation = AttenuationFunc(D);
    float3 inscatter = phase_factor*attenuation*extinction;

    // Scale by dT because we are doing quadrature
    inscatter *= t_range / float(LIGHT_LUT_DEPTH_RESOLUTION);

    inscatter = inscatter / g_vScatterPower;
    sAccum_P[idx] = inscatter;
#if (LIGHTMODE == LIGHTMODE_SPOTLIGHT)
    sAccum_S1[idx] = (D==0) ? float3(0) : inscatter/D;
    sAccum_S2[idx] = t*sAccum_S1[idx];
#endif

    
//    [unroll]
    for (uint d=1; d<32; d = d<<1)
    {
        if (gthreadID.x >= d)
        {
            sAccum_P[idx] += sAccum_P[idx - d];
#if (LIGHTMODE == LIGHTMODE_SPOTLIGHT)
            sAccum_S1[idx] += sAccum_S1[idx - d];
            sAccum_S2[idx] += sAccum_S2[idx - d];
#endif
        }
    }

    const float LUT_SCALE = 32.0f / 32768.0f;
//    rwLightLUT_P[dispatchID] = PackLut(sAccum_P[idx], LUT_SCALE);
	imageStore(rwLightLUT_P, int2(dispatchID), PackLut(sAccum_P[idx], LUT_SCALE));
#if (LIGHTMODE == LIGHTMODE_SPOTLIGHT)
    float max_t = 2*(t0 + t_range);
//    rwLightLUT_S1[dispatchID] = PackLut(sAccum_S1[idx], LUT_SCALE);
	imageStore(rwLightLUT_S1, int2(dispatchID), PackLut(sAccum_S1[idx], LUT_SCALE));
//    rwLightLUT_S2[dispatchID] = PackLut(sAccum_S2[idx], LUT_SCALE*max_t);
	imageStore(rwLightLUT_S2, int2(dispatchID), PackLut(sAccum_S2[idx], LUT_SCALE*max_t));
#endif
}

#elif (COMPUTEPASS == COMPUTEPASS_SUM)

const uint2 BLOCK_SIZE = uint2(32, 4);

//Texture2D<float4> tLightLUT_P : register(t5);
//Texture2D<float4> tLightLUT_S1 : register(t6);
//Texture2D<float4> tLightLUT_S2 : register(t7);
uniform sampler2D tLightLUT_P;
uniform sampler2D tLightLUT_S1;
uniform sampler2D tLightLUT_S2;

//layout (rgba8, binding = 3) uniform uimage2D tLightLUT_P;
//layout (rgba8, binding = 4) uniform uimage2D tLightLUT_S1;
//layout (rgba8, binding = 5) uniform uimage2D tLightLUT_S2;

shared float3 sOffset[BLOCK_SIZE.y];

//[numthreads( BLOCK_SIZE.x, BLOCK_SIZE.y, 1 )]
//void main(uint3 gthreadID : SV_GroupThreadID, uint3 dispatchID : SV_DispatchThreadID, uint2 groupID : SV_GroupID)
layout (local_size_x = BLOCK_SIZE.x, local_size_y = BLOCK_SIZE.y) in;
void main()
{
	uvec3 gthreadID = gl_LocalInvocationID;
	uvec3 dispatchID = gl_GlobalInvocationID;
	uint2 groupID = gl_WorkGroupID.xy;
    uint t_offset = 0;

    if (gthreadID.x == 0)
    {
        sOffset[gthreadID.y] = float3(0, 0, 0);
    }

//    [unroll]
    for (uint t = 0; t < LIGHT_LUT_DEPTH_RESOLUTION; t += BLOCK_SIZE.x)
    {
        int2 tc = int2(dispatchID.xy + uint2(t, 0));
        float4 s = float4(0,0,0,0);
#if (LIGHTMODE == LIGHTMODE_SPOTLIGHT)
        if (dispatchID.z == 2)
//            s = tLightLUT_S2[tc];
			s = texelFetch(tLightLUT_S2, tc, 0);
        else if (dispatchID.z == 1)
//            s = tLightLUT_S1[tc];
			s = texelFetch(tLightLUT_S1, tc, 0);
        else
//            s = tLightLUT_P[tc];
			s = texelFetch(tLightLUT_P, tc, 0);
#else
//        s = tLightLUT_P[tc];
        s = texelFetch(tLightLUT_P, tc, 0);
#endif
        float3 v = UnpackLut(s) + sOffset[gthreadID.y];
        if (gthreadID.x == (BLOCK_SIZE.x-1))
        {
            sOffset[gthreadID.y] = v;
        }
        s.a *= LIGHT_LUT_DEPTH_RESOLUTION/32;
#if (LIGHTMODE == LIGHTMODE_SPOTLIGHT)
        if (dispatchID.z == 2)
//            rwLightLUT_S2[tc] = PackLut(v, s.a);
			imageStore(rwLightLUT_S2, tc, PackLut(v, s.a));
        else if (dispatchID.z == 1)
//            rwLightLUT_S1[tc] = PackLut(v, s.a);
			imageStore(rwLightLUT_S1, tc, PackLut(v, s.a));
        else
//            rwLightLUT_P[tc] = PackLut(v, s.a);
			imageStore(rwLightLUT_P, tc, PackLut(v, s.a));
#else
//        rwLightLUT_P[tc] = PackLut(v, s.a);
		  imageStore(rwLightLUT_P, tc, PackLut(v, s.a));
#endif
    }
}

#endif