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
#define FLT_MAX 3.402823466e+38

#if 0
Buffer<uint>                            g_ValidCellsCounter             : register( t0 );
StructuredBuffer<SParticleIdAndDist>    g_VisibleParticlesUnorderedList : register( t1 );
StructuredBuffer<SParticleIdAndDist>    g_PartiallySortedList           : register( t1 );
RWStructuredBuffer<SParticleIdAndDist>  g_rwMergedList                  : register( u0 );
RWStructuredBuffer<SParticleIdAndDist>  g_rwPartiallySortedBuf          : register( u0 );

groupshared SParticleIdAndDist g_LocalParticleIdAndDist[ THREAD_GROUP_SIZE ];
#else
layout(r32ui, binding=0) uniform uimageBuffer  g_ValidCellsCounter;
layout(binding =1) buffer StructuredBuffer0
{
    SParticleIdAndDist g_VisibleParticlesUnorderedList[];
};

layout(binding =1) buffer StructuredBuffer1
{
    SParticleIdAndDist g_PartiallySortedList[];
};

layout(binding =0) buffer StructuredBuffer2
{
    SParticleIdAndDist g_rwMergedList[];
};

layout(binding =0) buffer StructuredBuffer3
{
    SParticleIdAndDist g_rwPartiallySortedBuf[];
};

shared SParticleIdAndDist g_LocalParticleIdAndDist[ THREAD_GROUP_SIZE ];
#endif

uniform SGlobalCloudAttribs g_GlobalCloudAttribs;

