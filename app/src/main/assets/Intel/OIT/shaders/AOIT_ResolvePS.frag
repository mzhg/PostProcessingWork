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
in float2 m_Tex;
layout(location = 0) out float4 Out_Color;

void main()
{
	float4 outColor = float4(0, 0, 0, 1);
    uint2 pixelAddr = uint2(gl_FragCoord.xy);

    // display debug colour
    //return float4( 0, 1, 0, 1.0 );

	// Load control surface
	AOITCtrlSurface ctrlSurface;
	AOITLoadControlSurfaceSRV(pixelAddr, ctrlSurface);

	// Any transparent fragment contributing to this pixel?
	if (!ctrlSurface.clear)
	{
		// Load all nodes for this pixel
		ATSPNode nodeArray[AOIT_NODE_COUNT];
		AOITSPLoadDataSRV(pixelAddr, nodeArray);

		// Accumulate final transparent colors
		float  trans = 1;
		float3 color = float3(0);
		for(uint i = 0; i < AOIT_NODE_COUNT; i++) {
#ifdef dohdr
			color += trans * FromRGBE(UnpackRGBA(nodeArray[i].color)).rgb;
#else
			color += trans * UnpackRGB(nodeArray[i].color);
#endif
			trans  = nodeArray[i].trans / 255.;
		}
		outColor = float4(color, nodeArray[AOIT_NODE_COUNT - 1].trans / 255.);
	}

    // blend accumualted transparent color with opaque background color
    Out_Color = outColor;
}

#endif // H_AIOT_RESOLVE
