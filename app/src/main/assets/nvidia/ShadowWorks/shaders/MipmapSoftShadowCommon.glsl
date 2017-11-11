//----------------------------------------------------------------------------------
// File:   SoftShadows.fx
// Author: Kirill Dmitriev
// Email:  sdkfeedback@nvidia.com
//
// Copyright (c) 2007 NVIDIA Corporation. All rights reserved.
//
// TO  THE MAXIMUM  EXTENT PERMITTED  BY APPLICABLE  LAW, THIS SOFTWARE  IS PROVIDED
// *AS IS*  AND NVIDIA AND  ITS SUPPLIERS DISCLAIM  ALL WARRANTIES,  EITHER  EXPRESS
// OR IMPLIED, INCLUDING, BUT NOT LIMITED  TO, IMPLIED WARRANTIES OF MERCHANTABILITY
// AND FITNESS FOR A PARTICULAR PURPOSE.  IN NO EVENT SHALL  NVIDIA OR ITS SUPPLIERS
// BE  LIABLE  FOR  ANY  SPECIAL,  INCIDENTAL,  INDIRECT,  OR  CONSEQUENTIAL DAMAGES
// WHATSOEVER (INCLUDING, WITHOUT LIMITATION,  DAMAGES FOR LOSS OF BUSINESS PROFITS,
// BUSINESS INTERRUPTION, LOSS OF BUSINESS INFORMATION, OR ANY OTHER PECUNIARY LOSS)
// ARISING OUT OF THE  USE OF OR INABILITY  TO USE THIS SOFTWARE, EVEN IF NVIDIA HAS
// BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
//
//
//----------------------------------------------------------------------------------

//--------------------------------------------------------------------------------------
// Global variables
//--------------------------------------------------------------------------------------
#include "../../../shader_libs/PostProcessingHLSLCompatiable.glsl"
#define N_LEVELS 10
#define DEPTH_RES 1024
#if 0
Texture2D<float> DepthTex0;
Texture2D<float2> DepthMip2;
Texture2D DiffuseTex;
SamplerComparisonState DepthCompare;
SamplerState DepthSampler
{
    Filter = MIN_MAG_MIP_POINT;
    AddressU = Clamp;
    AddressV = Clamp;
};
SamplerState DiffuseSampler
{
    Filter = MIN_MAG_MIP_LINEAR;
    AddressU = Wrap;
    AddressV = Wrap;
};
cbuffer cb0 : register(b0)
{
    float4 g_vMaterialKd;
    float3 g_vLightPos; ///< light in world CS
    float4 g_vLightFlux;
    float g_fFilterSize, g_fDoubleFilterSizeRev;
    row_major float4x4 mViewProj;
    row_major float4x4 mLightView;
    row_major float4x4 mLightViewProjClip2Tex;
    row_major float4x4 mLightProjClip2TexInv;
    bool bTextured;
};
cbuffer cb1 : register(b1)
{
    float g_fResRev[N_LEVELS] = { 1./1024, 1./512, 1./256, 1./128, 1./64, 1./32, 1./16, 1./8, 1./4, 1./2 };
};

#else
layout(binding = 0) uniform sampler2D DepthTex0;
layout(binding = 1) uniform sampler2D DepthMip2;
layout(binding = 2) uniform sampler2D DiffuseTex;

layout(binding = 0) uniform cb0
{
    float4 g_vMaterialKd;
    float3 g_vLightPos; ///< light in world CS
    float4 g_vLightFlux;
    float g_fFilterSize;
    float g_fDoubleFilterSizeRev;
    float4x4 mViewProj;
    float4x4 mLightView;
    float4x4 mLightViewProjClip2Tex;
    float4x4 mLightProjClip2TexInv;
    bool bTextured;
};

const float g_fResRev[N_LEVELS] = float[10]( 1./1024., 1./512., 1./256., 1./128., 1./64., 1./32., 1./16., 1./8., 1./4., 1./2. );
#endif