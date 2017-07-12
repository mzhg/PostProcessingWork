/////////////////////////////////////////////////////////////////////////////////////////////
// Copyright 2017 Intel Corporation
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or imlied.
// See the License for the specific language governing permissions and
// limitations under the License.
/////////////////////////////////////////////////////////////////////////////////////////////
#include "CloudsBase.glsl"

#ifndef DENSITY_GENERATION_METHOD
#   define DENSITY_GENERATION_METHOD 0
#endif

#if 0
Texture3D<float>       g_tex3DNoise                 : register( t0 );
Texture3D<float>       g_tex3DSingleScattering      : register( t0 );
Texture3D<float>       g_tex3DMultipleScattering    : register( t1 );
Texture3D<float>       g_tex3DPrevSctrOrder         : register( t0 );
Texture3D<float>       g_tex3DGatheredScattering    : register( t0 );
Texture3D<float>       g_tex3DSingleScatteringLUT    : register( t0 );
Texture3D<float>       g_tex3DMultipleScatteringLUT  : register( t1 );

cbuffer cbPostProcessingAttribs : register( b0 )
{
    SGlobalCloudAttribs g_GlobalCloudAttribs;
};

SamplerState samLinearWrap : register( s1 );
SamplerState samPointWrap : register( s2 );
#else
layout(binding = 0) uniform sampler3D g_tex3DNoise;
layout(binding = 0) uniform sampler3D g_tex3DSingleScattering;
layout(binding = 1) uniform sampler3D g_tex3DMultipleScattering;
layout(binding = 0) uniform sampler3D g_tex3DPrevSctrOrder;
layout(binding = 0) uniform sampler3D g_tex3DGatheredScattering;
layout(binding = 0) uniform sampler3D g_tex3DSingleScatteringLUT;
layout(binding = 1) uniform sampler3D g_tex3DMultipleScatteringLUT;
uniform SGlobalCloudAttribs g_GlobalCloudAttribs;
#endif

#define NUM_INTEGRATION_STEPS 64

float GetRandomDensity(in float3 f3Pos, float fStartFreq, int iNumOctaves /*= 3*/, float fAmplitudeScale /*= 0.6*/)
{
    float fNoise = 0;
    float fAmplitude = 1;
    float fFreq = fStartFreq;
    for(int i=0; i < iNumOctaves; ++i)
    {
        fNoise += (textureLod(g_tex3DNoise, f3Pos*fFreq, 0.0).x - 0.5) * fAmplitude;   // samLinearWrap
        fFreq *= 1.7;
        fAmplitude *= fAmplitudeScale;
    }
    return fNoise;
}

float GetPyroSphereDensity(float3 f3CurrPos)
{
    float fDistToCenter = length(f3CurrPos);
    float3 f3NormalizedPos = f3CurrPos / fDistToCenter;
    float fNoise = GetRandomDensity(f3NormalizedPos, 0.15, 4, 0.8);
    float fDensity = fDistToCenter + 0.35*fNoise < 0.8 ? 1 : 0;
    return fDensity;
}

float GetMetabolDensity(in float r)
{
    float r2 = r*r;
    float r4 = r2*r2;
    float r6 = r4*r2;
    return saturate(-4.0/9.0 * r6 + 17.0/9.0 * r4 - 22.0/9.0 * r2 + 1);
}

float ComputeDensity(float3 f3CurrPos)
{
	float fDistToCenter = length(f3CurrPos);
    float fMetabolDensity = GetMetabolDensity(fDistToCenter);
	float fDensity = 0.f;
#if DENSITY_GENERATION_METHOD == 0
    fDensity = saturate( 1.0*saturate(fMetabolDensity) + 1*pow(fMetabolDensity,0.5)*(GetRandomDensity(f3CurrPos + 0.5, 0.15, 4, 0.7 )) );
#elif DENSITY_GENERATION_METHOD == 1
    fDensity = 1.0*saturate(fMetabolDensity) + 1.0*pow(fMetabolDensity,0.5)*(GetRandomDensity(f3CurrPos, 0.1,4,0.8)) > 0.1 ? 1 : 0;
    //fDensity = saturate(fMetabolDensity) - 2*pow(fMetabolDensity,0.5)*GetRandomDensity(f3CurrPos, 0.2, 4, 0.7) > 0.05 ? 1 : 0;
#elif DENSITY_GENERATION_METHOD == 2
    fDensity = GetPyroSphereDensity(f3CurrPos);
#endif
	return fDensity;
}