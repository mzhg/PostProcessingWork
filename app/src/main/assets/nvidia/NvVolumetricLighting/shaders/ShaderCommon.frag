
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
// Copyright (C) 2013, NVIDIA Corporation. All rights reserved.

/*===========================================================================
Constants
===========================================================================*/

#include "../../../shader_libs/PostProcessingHLSLCompatiable.glsl"

const float PI = 3.1415926535898;
const float EDGE_FACTOR = 1.0 - (2.0/64.0) * (1.0/64.0);
const uint MAX_PHASE_TERMS = 4;

#ifndef USE_UNIFORM_BLOCK
#define USE_UNIFORM_BLOCK 0
#endif

#ifdef __PSSL__
const vec2 SAMPLE_POSITIONS[15] = vec2[15]
(
    // 1x
	float2( 0, 0)/16.0,
	// 2x
	float2(-4, 4)/16.0,
	float2( 4,-4)/16.0,
	// 4x
	float2(-6, 6)/16.0,
	float2( 6,-6)/16.0,
	float2(-2,-2)/16.0,
	float2( 2, 2)/16.0,
	// 8x
	float2(-7,-3)/16.0,
	float2( 7, 3)/16.0,
	float2( 1,-5)/16.0,
	float2(-5, 5)/16.0,
	float2(-3,-7)/16.0,
	float2( 3, 7)/16.0,
	float2( 5,-1)/16.0,
	float2(-1, 1)/16.0
);

#endif

#if USE_UNIFORM_BLOCK
layout(binding=0) uniform cbContext  // egister(b0)
{
	float2 g_vOutputSize;				//: packoffset(c0);
	float2 g_vOutputSize_Inv;			//: packoffset(c0.z);
	float2 g_vBufferSize;				//: packoffset(c1);
	float2 g_vBufferSize_Inv;			//: packoffset(c1.z);
	float g_fResMultiplier;				//: packoffset(c2);
	uint g_uBufferSamples;				//: packoffset(c2.y);
};

layout(binding=1) uniform cbFrame 	//: register(b1)
{
	float4x4 g_mProj;					//: packoffset(c0);
	float4x4 g_mViewProj;				//: packoffset(c4);
    float4x4 g_mViewProjInv;				//: packoffset(c8);
    float2 g_vOutputViewportSize;        //: packoffset(c12);
    float2 g_vOutputViewportSize_Inv;    //: packoffset(c12.z);
    float2 g_vViewportSize;              //: packoffset(c13);
    float2 g_vViewportSize_Inv;          //: packoffset(c13.z);
    float3 g_vEyePosition;				//: packoffset(c14);
	float2 g_vJitterOffset;				//: packoffset(c15);
	float g_fZNear;						//: packoffset(c15.z);
	float g_fZFar;						//: packoffset(c15.w);
    float3 g_vScatterPower;              //: packoffset(c16);
    uint g_uNumPhaseTerms;       		//: packoffset(c16.w);
    float3 g_vSigmaExtinction;           //: packoffset(c17);
    uint g_uPhaseFunc[4];				//: packoffset(c18);
    float4 g_vPhaseParams[4];			//: packoffset(c22);
};

layout(binding=2) uniform cbVolume //: register(b2)
{
	float4x4 g_mLightToWorld;			//  : packoffset(c0);
	float g_fLightFalloffAngle;			//	: packoffset(c4.x);
	float g_fLightFalloffPower;			//	: packoffset(c4.y);
	float g_fGridSectionSize;			//	: packoffset(c4.z);
	float g_fLightToEyeDepth;			//	: packoffset(c4.w);
    float g_fLightZNear;                 //  : packoffset(c5);
    float g_fLightZFar;                  //  : packoffset(c5.y);
	float4 g_vLightAttenuationFactors;	//	: packoffset(c6);
	float4x4 g_mLightProj[4];			//  : packoffset(c7);
	float4x4 g_mLightProjInv[4];			//  : packoffset(c23);
	float3 g_vLightDir;					//	: packoffset(c39);
	float g_fGodrayBias;					//	: packoffset(c39.w);
	float3 g_vLightPos;					//	: packoffset(c40);
    uint g_uMeshResolution;      		//  : packoffset(c40.w);
	float3 g_vLightIntensity;			//	: packoffset(c41);
	float g_fTargetRaySize;				//	: packoffset(c41.w);
	float4 g_vElementOffsetAndScale[4];	//	: packoffset(c42); 
	float4 g_vShadowMapDim;				//	: packoffset(c46);
	uint g_uElementIndex[4];	    		//  : packoffset(c47);
};

layout(binding=3) uniform cbApply //: register(b3)
{
	float4x4 g_mHistoryXform;	       //   : packoffset(c0);	
	float g_fFilterThreshold;		   //	: packoffset(c4);
	float g_fHistoryFactor;			   //	: packoffset(c4.y);
	float3 g_vFogLight;				   //   : packoffset(c5);
	float g_fMultiScattering;		   //   : packoffset(c5.w);
};

#else
uniform float2 g_vOutputSize;				//: packoffset(c0);
uniform float2 g_vOutputSize_Inv;			//: packoffset(c0.z);
uniform float2 g_vBufferSize;				//: packoffset(c1);
uniform float2 g_vBufferSize_Inv;			//: packoffset(c1.z);
uniform float g_fResMultiplier;				//: packoffset(c2);
uniform int g_uBufferSamples;				//: packoffset(c2.y);

uniform float4x4 g_mProj;					//: packoffset(c0);
uniform float4x4 g_mViewProj;				//: packoffset(c4);
uniform float4x4 g_mViewProjInv;				//: packoffset(c8);
uniform float2 g_vOutputViewportSize;        //: packoffset(c12);
uniform float2 g_vOutputViewportSize_Inv;    //: packoffset(c12.z);
uniform float2 g_vViewportSize;              //: packoffset(c13);
uniform float2 g_vViewportSize_Inv;          //: packoffset(c13.z);
uniform float3 g_vEyePosition;				//: packoffset(c14);
uniform float2 g_vJitterOffset;				//: packoffset(c15);
uniform float g_fZNear;						//: packoffset(c15.z);
uniform float g_fZFar;						//: packoffset(c15.w);
uniform float3 g_vScatterPower;              //: packoffset(c16);
uniform int g_uNumPhaseTerms;       		//: packoffset(c16.w);
uniform float3 g_vSigmaExtinction;           //: packoffset(c17);
uniform ivec4 g_uPhaseFunc;				//: packoffset(c18);
uniform float4 g_vPhaseParams[4];			//: packoffset(c22);

uniform float4x4 g_mLightToWorld;			//  : packoffset(c0);
uniform float g_fLightFalloffAngle;			//	: packoffset(c4.x);
uniform float g_fLightFalloffPower;			//	: packoffset(c4.y);
uniform float g_fGridSectionSize;			//	: packoffset(c4.z);
uniform float g_fLightToEyeDepth;			//	: packoffset(c4.w);
uniform float g_fLightZNear;                 //  : packoffset(c5);
uniform float g_fLightZFar;                  //  : packoffset(c5.y);
uniform float4 g_vLightAttenuationFactors;	//	: packoffset(c6);
uniform float4x4 g_mLightProj[4];			//  : packoffset(c7);
uniform float4x4 g_mLightProjInv[4];			//  : packoffset(c23);
uniform float3 g_vLightDir;					//	: packoffset(c39);
uniform float g_fGodrayBias;					//	: packoffset(c39.w);
uniform float3 g_vLightPos;					//	: packoffset(c40);
uniform int g_uMeshResolution;      		//  : packoffset(c40.w);
uniform float3 g_vLightIntensity;			//	: packoffset(c41);
uniform float g_fTargetRaySize;				//	: packoffset(c41.w);
uniform float4 g_vElementOffsetAndScale[4];	//	: packoffset(c42); 
uniform float4 g_vShadowMapDim;				//	: packoffset(c46);
uniform int g_uElementIndex[4];	    		//  : packoffset(c47);

uniform float4x4 g_mHistoryXform;	       //   : packoffset(c0);	
uniform float g_fFilterThreshold;		   //	: packoffset(c4);
uniform float g_fHistoryFactor;			   //	: packoffset(c4.y);
uniform float3 g_vFogLight;				   //   : packoffset(c5);
uniform float g_fMultiScattering;		   //   : packoffset(c5.w);

#endif

/*===========================================================================
Common functions
===========================================================================*/

float LinearizeDepth(float d, float zn, float zf)
{
	return d * zn / (zf - ((zf - zn) * d));
}

float WarpDepth(float z, float zn, float zf)
{
	return z * (1+zf/zn) / (1+z*zf/zn);
}

float MapDepth(float d, float zn, float zf)
{
	return (d - zn) / (zf - zn);
}

// Approximates a non-normalized gaussian with Sigma == 1
float GaussianApprox(float2 sample_pos, float width)
{
	float x_sqr = sample_pos.x*sample_pos.x + sample_pos.y*sample_pos.y;
	// exp(-0.5*(x/w)^2) ~ (1-(x/(8*w))^2)^32
	float w = saturate(1.0f - x_sqr/(64.0f * width*width));
	w = w*w;	// ^2
	w = w*w;	// ^4
	w = w*w;	// ^8
	w = w*w;	// ^16
	w = w*w;	// ^32
	return w;
}

#if defined(ATTENUATIONMODE)
float AttenuationFunc(float d)
{
    if (ATTENUATIONMODE == ATTENUATIONMODE_POLYNOMIAL)
    {
        // 1-(A+Bx+Cx^2)
        return saturate(1.0f - (g_vLightAttenuationFactors.x + g_vLightAttenuationFactors.y*d + g_vLightAttenuationFactors.z*d*d));
    }
    else if (ATTENUATIONMODE == ATTENUATIONMODE_INV_POLYNOMIAL)
    {
        // 1 / (A+Bx+Cx^2) + D
        return saturate(1.0f / (g_vLightAttenuationFactors.x + g_vLightAttenuationFactors.y*d + g_vLightAttenuationFactors.z*d*d) + g_vLightAttenuationFactors.w);
    }
    else //if (ATTENUATIONMODE == ATTENUATIONMODE_NONE)
    {
        return 1.0f;
    }
}
#endif

float3 GetPhaseFactor(sampler2D tex, float cos_theta)
{
    float2 tc;
    tc.x = 0;
    tc.y = acos(clamp(-cos_theta, -1.0f, 1.0f)) / PI;
    return g_vScatterPower*
    		//tex.SampleLevel(sBilinear, tc, 0).rgb;
    		textureLod(tex, tc, 0.0).rgb;
}