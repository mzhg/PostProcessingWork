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

%% MUX_END %%
*/

#include "ShaderCommon.frag"

// using the phase functions directly isn't correct, because they are supposed to be 
// integrated over the subtended solid angle. This falls apart as sin(theta)
// approaches 0 (ie. cos(theta) aproaches +1 or -1).
// We apply a sliding scale to the functions to compensate for this somewhat.

#define NORMALIZE_PHASE_FUNCTIONS 1

float ScatterPhase_Isotropic()
{
    return 1.0 / (4.0 * PI);
}

float ScatterPhase_Rayleigh(float cosa)
{
    float cos_term = cosa*cosa; // ^2
	float phase_term = (3.0/(16.0*PI)) * (1.0 + cos_term);
#if NORMALIZE_PHASE_FUNCTIONS
    cos_term *= cos_term; // ^4
    return phase_term*(1-cos_term/8.f);
#else
    return phase_term;
#endif
}

float ScatterPhase_HenyeyGreenstein(float cosa, float g)
{	
#if NORMALIZE_PHASE_FUNCTIONS
    // "normalized" Henyey-Greenstein
    float g_sqr = g*g;
    float num = (1 - abs(g));
    float denom = sqrt( max(1-2*g*cosa+g_sqr, 0) );
    float frac = num/denom;
    float scale = g_sqr + (1 - g_sqr) / (4*PI);
    return scale * (frac*frac*frac);
#else
    // Classic Henyey-Greenstein
	float k1 = (1.f-g*g);
	float k2 = (1.f + g*g - 2.f*g*cosa);
	return (1.f / (4.f*PI)) * k1 / pow(abs(k2), 1.5f);
#endif
}

float ScatterPhase_MieHazy(float cosa)
{
    float cos_term = 0.5f*(1+cosa);
    float cos_term_2 = cos_term*cos_term;           // ^2
    float cos_term_4 = cos_term_2*cos_term_2;       // ^4
    float cos_term_8 = cos_term_4*cos_term_4;       // ^8
	float phase_term = (1.f/(4.f*PI))*(0.5f+(9.f/2.f)*cos_term_8);
#if NORMALIZE_PHASE_FUNCTIONS
    return phase_term * (1-cos_term_8/2.0f);
#else
    return phase_term;
#endif
}

float ScatterPhase_MieMurky(float cosa)
{
    float cos_term = 0.5f*(1+cosa);
    float cos_term_2 = cos_term*cos_term;           // ^2
    float cos_term_4 = cos_term_2*cos_term_2;       // ^4
    float cos_term_8 = cos_term_4*cos_term_4;       // ^8
    float cos_term_16 = cos_term_8*cos_term_8;      // ^16
    float cos_term_32 = cos_term_16*cos_term_16;    // ^32
	float phase_term = (1.f/(4.f*PI))*(0.5f+(33.f/2.f)*cos_term_32);
#if NORMALIZE_PHASE_FUNCTIONS
    return phase_term * (1-cos_term_32/2.0f);
#else
    return phase_term;
#endif
}

in float2 vTex;
in float4 vWorldPos;

layout(location = 0) out float4 OutColor;

// float4 main(VS_QUAD_OUTPUT input) : SV_TARGET
void main()
{
	float cos_theta = -cos(PI*(vTex.y));
	float3 phase_factor = float3(0,0,0);
    float3 total_scatter = float3(0,0,0);

	// These must match the PhaseFunctionType enum in NvVolumetricLighting.h
	const int PHASEFUNC_ISOTROPIC = 0;
	const int PHASEFUNC_RAYLEIGH = 1;
	const int PHASEFUNC_HG = 2;
    const int PHASEFUNC_MIEHAZY = 3;
    const int PHASEFUNC_MIEMURKY = 4;
    
    const int phaseFunc[4] = int[4](1, 2, 3, 4);

    for (int i=0; i<g_uNumPhaseTerms; ++i)
	{
        float3 term_scatter = g_vPhaseParams[i].rgb;
        total_scatter += term_scatter;
		if (phaseFunc[i] == PHASEFUNC_ISOTROPIC)
		{
			phase_factor += term_scatter*ScatterPhase_Isotropic();
		}
		else if (phaseFunc[i] == PHASEFUNC_RAYLEIGH)
		{
			phase_factor += term_scatter*ScatterPhase_Rayleigh(cos_theta);
		}
		else if (phaseFunc[i] == PHASEFUNC_HG)
		{
			phase_factor += term_scatter*ScatterPhase_HenyeyGreenstein(cos_theta, g_vPhaseParams[i].a);
		}
		else if (phaseFunc[i] == PHASEFUNC_MIEHAZY)
		{
			phase_factor += term_scatter*ScatterPhase_MieHazy(cos_theta);
		}
		else if (phaseFunc[i] == PHASEFUNC_MIEMURKY)
		{
			phase_factor += term_scatter*ScatterPhase_MieMurky(cos_theta);
		}
    }
    phase_factor = phase_factor / total_scatter;
	OutColor = float4(phase_factor, 1);
}
