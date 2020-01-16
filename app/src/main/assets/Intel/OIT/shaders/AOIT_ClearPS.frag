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
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
/////////////////////////////////////////////////////////////////////////////////////////////

#ifndef H_AIOT_RESOLVE
#define H_AIOT_RESOLVE


#include "AOIT.glsl"

//float4 AOITSPResolvePS(float4 pos: SV_POSITION, float2 tex : TEX_COORD0) : SV_Target
layout(location = 0) out float4 Out_Color;

//[earlydepthstencil]
//void AOITSPClearPS( float4 pos: SV_POSITION, float2 tex : TEX_COORD0 )
layout(early_fragment_tests) in;

void main()
{
    uint2 pixelAddr = uint2(gl_FragCoord.xy);

	uint addr = AOITAddrGenUAV(pixelAddr);

	uint data = 0x1; // is clear
//	gAOITSPClearMaskUAV[pixelAddr] = data;
    imageStore(gAOITSPClearMaskUAV, int2(pixelAddr), uint4(data));
}

#endif // H_AIOT_RESOLVE
