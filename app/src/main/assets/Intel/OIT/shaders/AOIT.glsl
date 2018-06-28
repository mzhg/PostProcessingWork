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

#ifndef H_AOIT
#define H_AOIT

#include "../../../shader_libs/PostProcessingHLSLCompatiable.glsl"
#include "Common.glsl"
//typedef uint COLOR;
#define COLOR uint

//////////////////////////////////////////////
// Defines
//////////////////////////////////////////////

#ifdef aoit_node_count
#define AOIT_NODE_COUNT	 aoit_node_count
#endif


#ifndef AOIT_NODE_COUNT
#define AOIT_NODE_COUNT			(4)
#endif

#if AOIT_NODE_COUNT == 2
#define AOIT_RT_COUNT			(1)
#else
#define AOIT_RT_COUNT			(AOIT_NODE_COUNT / 4)
#endif

// Forces compression to only work on the second half of the nodes (cheaper and better IQ in some cases)

#if AOIT_NODE_COUNT >= 8
#define AOIT_DONT_COMPRESS_FIRST_HALF
#endif

//#define AOIT_EARLY_Z_CULL

#define AOIT_TILED_ADDRESSING

// Various constants used by the algorithm
#define AOIT_EMPTY_NODE_DEPTH   (3.40282E38)
#define AOIT_TRANS_BIT_COUNT    (8)
#define AOIT_MAX_UNNORM_TRANS   ((1 << AOIT_TRANS_BIT_COUNT) - 1)
#define AOIT_TRANS_MASK         (0xFFFFFFFFu - uint(AOIT_MAX_UNNORM_TRANS))








#ifdef dohdr

// ToRGBE - takes a float RGB value and converts it to a float RGB value with a shared exponent
float4 ToRGBE(float4 inColor)
{
	float base = max(inColor.r, max(inColor.g, inColor.b));
	int e;
	float m = frexp(base, e);
	return float4(saturate(inColor.rgb / exp2(e)), e + 127.);
}

// FromRGBE takes a float RGB value with a sahred exponent and converts it to a
//	float RGB value
float4 FromRGBE(float4 inColor)
{
	return float4(inColor.rgb*exp2(inColor.a - 127.), inColor.a);
}

// UnpackRGBA takes a uint value and converts it to a float4
float4 UnpackRGBA(COLOR packedInput)
{
	float4 unpackedOutput;
	uint4 p = uint4((packedInput & 0xFFU),
		(packedInput >> 8U) & 0xFFU,
		(packedInput >> 16U) & 0xFFU,
		(packedInput >> 24U));

	unpackedOutput = float4(p) / float4(255, 255, 255, 1.0f);
	return unpackedOutput;
}

// PackRGBA takes a float4 value and packs it into a UINT (8 bits / float)
COLOR PackRGBA(float4 unpackedInput)
{
	uint4 u = uint4(unpackedInput * float4(255, 255, 255, 1.0f));
	uint packedOutput = (u.w << 24U) | (u.z << 16U) | (u.y << 8U) | u.x;
	return packedOutput;
}

#else

float UnpackUnnormAlpha(COLOR packedInput)
{

	return float(packedInput >> 24U);
}

float3 UnpackRGB(COLOR packedInput)
{
	float3 unpackedOutput;
	uint3 p = uint3((packedInput & 0xFFU),
		(packedInput >> 8U) & 0xFFU,
		(packedInput >> 16U) & 0xFFU);

	unpackedOutput = float3(p) / 255;
	return unpackedOutput;
}



COLOR PackRGB(float3 unpackedInput)
{
	uint3 u = uint3(saturate(unpackedInput) * 255 + 0.5);
	uint  packedOutput = (u.z << 16U) | (u.y << 8U) | u.x;
	return packedOutput;
}

COLOR PackRGBA(float4 unpackedInput)
{
	uint4 u = uint4(saturate(unpackedInput) * 255 + 0.5);
	uint  packedOutput = (u.w << 24U) | (u.z << 16U) | (u.y << 8U) | u.x;
	return packedOutput;
}

float4 UnpackRGBA(COLOR packedInput)
{
	float4 unpackedOutput;
	uint4 p = uint4((packedInput & 0xFFU),
		(packedInput >> 8U) & 0xFFU,
		(packedInput >> 16U) & 0xFFU,
		(packedInput >> 24U));

	unpackedOutput = float4(p) / 255.0;
	return unpackedOutput;
}
#endif


//////////////////////////////////////////////
// Structs
//////////////////////////////////////////////

struct AOITCtrlSurface
{
	bool  clear;
	bool  opaque;
	float depth;
};

struct AOITSPData
{
	float4 depth[AOIT_RT_COUNT];
	uint4  color[AOIT_RT_COUNT];
};

struct AOITSPDepthData
{
	float4 depth[AOIT_RT_COUNT];
};

struct AOITSPColorData
{
	uint4  color[AOIT_RT_COUNT];
};

struct ATSPNode
{
    float  depth;
    float  trans;
    uint   color;
};

//////////////////////////////////////////////
// Resources
//////////////////////////////////////////////


#if AOIT_NODE_COUNT == 8
#define _AOITSPDepthDataUAV g8AOITSPDepthDataUAV
#define _AOITSPColorDataUAV g8AOITSPColorDataUAV
#define _AOITSPColorDataSRV g8AOITSPColorDataSRV
#define _AOITSPDepthDataSRV g8AOITSPDepthDataSRV
#else
#define _AOITSPDepthDataUAV gAOITSPDepthDataUAV
#define _AOITSPColorDataUAV gAOITSPColorDataUAV
#define _AOITSPColorDataSRV gAOITSPColorDataSRV
#define _AOITSPDepthDataSRV gAOITSPDepthDataSRV
#endif


// Since there's no reflection on the cpp side for these, set registers explicitly - don't change them, this is the expected order
#ifdef dopso
RasterizerOrderedTexture2D<uint> gAOITSPClearMaskUAV        : register( u1 );
#else
//RWTexture2D<uint> gAOITSPClearMaskUAV        : register(u1);
layout(r32ui, binding = 0) uniform image2D gAOITSPClearMaskUAV;
#endif
//RWStructuredBuffer<AOITSPDepthData> _AOITSPDepthDataUAV     : register( u2 );
//RWStructuredBuffer<AOITSPColorData> _AOITSPColorDataUAV     : register( u3 );

layout(binding = 0) buffer Buffer0RW
{
    AOITSPDepthData _AOITSPDepthDataUAV[];
};

layout(binding = 1) buffer Buffer1RW
{
    AOITSPColorData _AOITSPColorDataUAV[];
};

//Texture2D<uint> gAOITSPClearMaskSRV;
layout(binding = 0) uniform usampler2D gAOITSPClearMaskSRV;
//StructuredBuffer<AOITSPColorData> _AOITSPColorDataSRV;
//StructuredBuffer<AOITSPDepthData> _AOITSPDepthDataSRV;

layout(binding = 2) buffer Buffer2R
{
    AOITSPColorData _AOITSPColorDataSRV[];
};

layout(binding = 3) buffer Buffer3R
{
    AOITSPDepthData _AOITSPDepthDataSRV[];
};

//////////////////////////////////////////////
// Main AOIT fragment insertion code
//////////////////////////////////////////////

//////////////////////////////////////////////
// Main AOIT fragment insertion code
//////////////////////////////////////////////

void AOITSPInsertFragment(in float  fragmentDepth,
                          in float  fragmentTrans,
                          in float3 fragmentColor,
                          inout ATSPNode nodeArray[AOIT_NODE_COUNT])
{
    int i, j;

    float  depth[AOIT_NODE_COUNT + 1];
    float  trans[AOIT_NODE_COUNT + 1];
    uint   color[AOIT_NODE_COUNT + 1];

    ///////////////////////////////////////////////////
    // Unpack AOIT data
    ///////////////////////////////////////////////////
    for (i = 0; i < AOIT_NODE_COUNT; ++i) {
        depth[i] = nodeArray[i].depth;
        trans[i] = nodeArray[i].trans;
        color[i] = nodeArray[i].color;
    }

    // Find insertion index
    int index = 0;
    float prevTrans = 255;
    for (i = 0; i < AOIT_NODE_COUNT; ++i) {
        if (fragmentDepth > depth[i]) {
            index++;
            prevTrans = trans[i];
        }
    }

    // Make room for the new fragment. Also composite new fragment with the current curve
    // (except for the node that represents the new fragment)
    for (i = AOIT_NODE_COUNT - 1; i >= 0; --i) {
        if (index <= i) {
            depth[i + 1] = depth[i];
            trans[i + 1] = trans[i] * fragmentTrans;
            color[i + 1] = color[i];
        }
    }

	// Insert new fragment
	const float newFragTrans = fragmentTrans * prevTrans;
#ifdef dohdr
	const uint  newFragColor = PackRGBA(ToRGBE(float4(fragmentColor * (1.0 - fragmentTrans), 1)));
#else
	const uint  newFragColor = PackRGB(fragmentColor * (1.0 - fragmentTrans));
#endif
	for (i = 0; i <= AOIT_NODE_COUNT; ++i) {
		if (index == i) {
			depth[i] = fragmentDepth;
			trans[i] = newFragTrans;
			color[i] = newFragColor;
		}
	}

	float EMPTY_NODE = asfloat(asuint(float(AOIT_EMPTY_NODE_DEPTH)) & AOIT_TRANS_MASK);

#ifdef dohdr
    // pack representation if we have too many nodes
 	if (asfloat(asuint(float(depth[AOIT_NODE_COUNT])) & AOIT_TRANS_MASK) != EMPTY_NODE) {
		float3 toBeRemovedCol = FromRGBE(UnpackRGBA(color[AOIT_NODE_COUNT])).rgb;
		float3 toBeAccumulCol = FromRGBE(UnpackRGBA(color[AOIT_NODE_COUNT-1])).rgb;
		color[AOIT_NODE_COUNT - 1] = PackRGBA(ToRGBE(float4(toBeAccumulCol + toBeRemovedCol * trans[AOIT_NODE_COUNT - 1] *
			rcp(trans[AOIT_NODE_COUNT - 2]),1)));
		trans[AOIT_NODE_COUNT - 1] = trans[AOIT_NODE_COUNT];
	}
#else
	if (asfloat(asuint(float(depth[AOIT_NODE_COUNT])) & AOIT_TRANS_MASK) != EMPTY_NODE) {
		float3 toBeRemovedCol = UnpackRGB(color[AOIT_NODE_COUNT]);
		float3 toBeAccumulCol = UnpackRGB(color[AOIT_NODE_COUNT - 1]);
		color[AOIT_NODE_COUNT - 1] = PackRGB(toBeAccumulCol + toBeRemovedCol * trans[AOIT_NODE_COUNT - 1] *
			rcp(trans[AOIT_NODE_COUNT - 2]));
		trans[AOIT_NODE_COUNT - 1] = trans[AOIT_NODE_COUNT];
	}
#endif


    // Pack AOIT data
    for (i = 0; i < AOIT_NODE_COUNT; ++i) {
        nodeArray[i].depth = depth[i];
        nodeArray[i].trans = trans[i];
        nodeArray[i].color = color[i];
    }
}

/////////////////////////////////////////////////
// Address generation functions for the AOIT data
/////////////////////////////////////////////////

uint AOITAddrGen(uint2 addr2D, uint surfaceWidth)
{
#ifdef AOIT_TILED_ADDRESSING

	surfaceWidth	  = surfaceWidth >> 1U;
	uint2 tileAddr2D  = addr2D >> uint2(1U);
	uint  tileAddr1D  = (tileAddr2D[0] + surfaceWidth * tileAddr2D[1]) << 2U;
	uint2 pixelAddr2D = addr2D & uint2(0x1U);
	uint  pixelAddr1D = (pixelAddr2D[1] << 1U) + pixelAddr2D[0];

	return tileAddr1D | pixelAddr1D;
#else
	return addr2D[0] + surfaceWidth * addr2D[1];
#endif
}

uint AOITAddrGenUAV(uint2 addr2D)
{
	int2 dim =
//	gAOITSPClearMaskUAV.GetDimensions(dim[0], dim[1]);
    imageSize(gAOITSPClearMaskUAV);
	return AOITAddrGen(addr2D, uint(dim[0]));
}

uint AOITAddrGenSRV(uint2 addr2D)
{
	int2 dim =
//	gAOITSPClearMaskSRV.GetDimensions(dim[0], dim[1]);
    textureSize(gAOITSPClearMaskSRV, 0);
	return AOITAddrGen(addr2D, uint(dim[0]));
}


#ifdef dohdr
void AOITSPClearData(inout AOITSPData data, float depth, float4 color)
{
	uint packedColor = PackRGBA(ToRGBE(float4(0, 0, 0,1)));

#if AOIT_NODE_COUNT == 2
	data.depth[0] = 0;
	data.color[0][0] = asuint((asuint(depth) & AOIT_TRANS_MASK) | (uint(saturate(1.0f - color.w) * 255 + 0.5) & uint(AOIT_MAX_UNNORM_TRANS)));
	data.color[0][1] = asuint((asuint(float(AOIT_EMPTY_NODE_DEPTH)) & AOIT_TRANS_MASK) | uint(saturate(1.0f - color.w) * 255 + 0.5));
	data.color[0][2] = PackRGBA(ToRGBE(float4(color.www * color.xyz,1)));
	data.color[0][3] = packedColor;
#else
	for (uint i = 0; i < AOIT_RT_COUNT; i++) {
		data.depth[i] = asfloat(((asuint(AOIT_EMPTY_NODE_DEPTH) & AOIT_TRANS_MASK)) | uint(saturate(1.0f - color.w) * 255 + 0.5));
		data.color[i] = packedColor;
	}
	data.depth[0][0] = asfloat(((asuint(depth) & AOIT_TRANS_MASK)) | ((uint(saturate(1.0f - color.w) * 255 + 0.5)) & uint(AOIT_MAX_UNNORM_TRANS)));
	data.color[0][0] = PackRGBA(ToRGBE(float4(color.www * color.xyz, 1)));
#endif
}

#else
void AOITSPClearData(inout AOITSPData data, float depth, float4 color)
{
	uint packedColor = PackRGBA(float4(0, 0, 0, 1.0f - color.w));

#if AOIT_NODE_COUNT == 2
	data.depth[0] = 0;
	data.color[0][0] = asuint(depth);
	data.color[0][1] = asuint(float(AOIT_EMPTY_NODE_DEPTH));
	data.color[0][2] = PackRGBA(float4(color.www * color.xyz, 1.0f - color.w));
	data.color[0][3] = packedColor;
#else
	for(uint i = 0; i < AOIT_RT_COUNT; i++) {
		data.depth[i] = AOIT_EMPTY_NODE_DEPTH;
		data.color[i] = packedColor;
	}
	data.depth[0][0] = depth;
	data.color[0][0] = PackRGBA(float4(color.www * color.xyz, 1.0f - color.w));
#endif
}
#endif
/////////////////////////////////////////////////
// Load/store functions for the AOIT data
/////////////////////////////////////////////////
#ifdef dohdr
void AOITSPLoadDataSRV(in uint2 pixelAddr, out ATSPNode nodeArray[AOIT_NODE_COUNT])
{
	AOITSPData data;
	uint addr  = AOITAddrGenSRV(pixelAddr);
	data.color = _AOITSPColorDataSRV[addr];

#if AOIT_NODE_COUNT == 2
	for(uint j = 0; j < 2; j++) {

		ATSPNode node = { 0, float(data.color[0][j] & uint(AOIT_MAX_UNNORM_TRANS)),
			data.color[0][2 + j] };
        nodeArray[j] = node;
	}
#else
	data.depth = _AOITSPDepthDataSRV[addr];
	for(uint i = 0; i < AOIT_RT_COUNT; i++) {
		for(uint j = 0; j < 4; j++) {
            ATSPNode node = { 0, float(asuint(data.depth[i][j]) & uint(AOIT_MAX_UNNORM_TRANS)),
							     data.color[i][j] };
            nodeArray[4 * i + j] = node;
		}
	}
#endif
}
#else
void AOITSPLoadDataSRV(in uint2 pixelAddr, out ATSPNode nodeArray[AOIT_NODE_COUNT])
{
	AOITSPData data;
	uint addr = AOITAddrGenSRV(pixelAddr);
	data.color = _AOITSPColorDataSRV[addr];

#if AOIT_NODE_COUNT == 2
	for (uint j = 0; j < 2; j++) {
		ATSPNode node = { 0, UnpackUnnormAlpha(data.color[0][2 + j]),
			data.color[0][2 + j] & 0xFFFFFFU };
		nodeArray[j] = node;
	}
#else
	for (uint i = 0; i < AOIT_RT_COUNT; i++) {
		for (uint j = 0; j < 4; j++) {
			ATSPNode node = { 0, UnpackUnnormAlpha(data.color[i][j]),
				data.color[i][j] & 0xFFFFFFU };
			nodeArray[4 * i + j] = node;
		}
	}
#endif
}
#endif


#ifdef dohdr
void AOITSPLoadDataUAV(in uint2 pixelAddr, out ATSPNode nodeArray[AOIT_NODE_COUNT])
{
	AOITSPData data;
	uint addr  = AOITAddrGenUAV(pixelAddr);
	data.color = _AOITSPColorDataUAV[addr];

#if AOIT_NODE_COUNT == 2
	for(uint j = 0; j < 2; j++) {
		ATSPNode node = { asfloat(data.color[0][j] & AOIT_TRANS_MASK), float(data.color[0][j] & uint(AOIT_MAX_UNNORM_TRANS)),
			data.color[0][2 + j]  };
        nodeArray[j] = node;
	}
#else
	data.depth = _AOITSPDepthDataUAV[addr];
	for(uint i = 0; i < AOIT_RT_COUNT; i++) {
		for(uint j = 0; j < 4; j++) {
            ATSPNode node = { asfloat(asuint(data.depth[i][j]) & uint(AOIT_TRANS_MASK)),
								float(asuint(data.depth[i][j]) & uint(AOIT_MAX_UNNORM_TRANS)),
							  data.color[i][j] };
            nodeArray[4 * i + j] = node;
		}
	}
#endif
}
#else
void AOITSPLoadDataUAV(in uint2 pixelAddr, out ATSPNode nodeArray[AOIT_NODE_COUNT])
{
	AOITSPData data;
	uint addr = AOITAddrGenUAV(pixelAddr);
	data.color = _AOITSPColorDataUAV[addr];

#if AOIT_NODE_COUNT == 2
	for (uint j = 0; j < 2; j++) {
		ATSPNode node = { asfloat(data.color[0][j]),
			UnpackUnnormAlpha(data.color[0][2 + j]),
			data.color[0][2 + j] & 0xFFFFFFU };
		nodeArray[j] = node;
	}
#else
	data.depth = _AOITSPDepthDataUAV[addr];
	for (uint i = 0; i < AOIT_RT_COUNT; i++) {
		for (uint j = 0; j < 4; j++) {
			ATSPNode node = { data.depth[i][j],
				UnpackUnnormAlpha(data.color[i][j]),
				data.color[i][j] & 0xFFFFFFU };
			nodeArray[4 * i + j] = node;
		}
	}
#endif
}
#endif


#ifdef dohdr
void AOITSPStoreDataUAV(in uint2 pixelAddr, ATSPNode nodeArray[AOIT_NODE_COUNT])
{
	AOITSPData data;
	uint addr = AOITAddrGenUAV(pixelAddr);

#if AOIT_NODE_COUNT == 2
	for(uint j = 0; j < 2; j++) {
		data.color[0][j] = asuint((asuint(float(nodeArray[j].depth)) & AOIT_TRANS_MASK) | (uint(nodeArray[j].trans)& uint(AOIT_MAX_UNNORM_TRANS)));
		data.color[0][2 + j] = nodeArray[j].color;
	}
#else
	for(uint i = 0; i < AOIT_RT_COUNT; i++) {
		for(uint j = 0; j < 4; j++) {
			data.depth[i][j] = asfloat((asuint(nodeArray[4 * i + j].depth) & AOIT_TRANS_MASK) | (uint(nodeArray[4 * i + j].trans)& uint(AOIT_MAX_UNNORM_TRANS)));
			data.color[i][j] = (nodeArray[4 * i + j].color);
		}
	}
	_AOITSPDepthDataUAV[addr] = data.depth;
#endif

	_AOITSPColorDataUAV[addr] = data.color;
}
#else
void AOITSPStoreDataUAV(in uint2 pixelAddr, ATSPNode nodeArray[AOIT_NODE_COUNT])
{
	AOITSPData data;
	uint addr = AOITAddrGenUAV(pixelAddr);

#if AOIT_NODE_COUNT == 2
	for (uint j = 0; j < 2; j++) {
		data.color[0][j] = asuint((asuint(float(nodeArray[j].depth)) & AOIT_TRANS_MASK) | uint(nodeArray[j].trans));
		data.color[0][2 + j] = (nodeArray[j].color & 0xFFFFFFU) |
			((uint(nodeArray[j].trans)) << 24U);
	}
#else
	for (uint i = 0; i < AOIT_RT_COUNT; i++) {
		for (uint j = 0; j < 4; j++) {
			data.depth[i][j] = nodeArray[4 * i + j].depth;
			data.color[i][j] = (nodeArray[4 * i + j].color & 0xFFFFFFU) |
				((uint(nodeArray[4 * i + j].trans)) << 24U);
		}
	}
	_AOITSPDepthDataUAV[addr] = data.depth;
#endif

	_AOITSPColorDataUAV[addr] = data.color;
}
#endif


/////////////////////////////////////////////////////////////
// Control Surface functions for the AOIT data
// We use this surface to remove the overhead incurred in
// clearing large AOIT buffers by storing for each
// pixel on the screen a to-be-cleared flag.
// We use the same structure to store some additional
// per-pixel information such as the depth of the most
// distant transparent fragment and its total transmittance,
// which in turn can be used to perform early-z culling over
// pixels covered by transparent fragments
/////////////////////////////////////////////////////////////

void AOITLoadControlSurface(in uint data, inout AOITCtrlSurface surface)
{
	surface.clear	= data & 0x1u ? true : false;
	surface.opaque  = data & 0x2u ? true : false;
	surface.depth   = asfloat((data & 0xFFFFFFFCU) | 0x3U);
}

void AOITLoadControlSurfaceUAV(in uint2 pixelAddr, inout AOITCtrlSurface surface)
{
//	uint data = gAOITSPClearMaskUAV[pixelAddr];
    uint data = imageLoad(gAOITSPClearMaskUAV, pixelAddr).x;
	AOITLoadControlSurface(data, surface);
}

void AOITLoadControlSurfaceSRV(in uint2 pixelAddr, inout AOITCtrlSurface surface)
{
//	uint data = gAOITSPClearMaskSRV[pixelAddr];
    uint data = texelFetch(gAOITSPClearMaskSRV, pixelAddr, 0).x;
	AOITLoadControlSurface(data, surface);
}

void AOITStoreControlSurface(in uint2 pixelAddr, in AOITCtrlSurface surface)
{
	uint data;
	data  = asuint(surface.depth) & 0xFFFFFFFCU;
	data |= surface.opaque ? 0x2u : 0x0u;
	data |= surface.clear  ? 0x1u : 0x0u;
//	gAOITSPClearMaskUAV[pixelAddr] = data;
    imageStore(gAOITSPClearMaskUAV, pixelAddr, uint4(data,0,0,0));
}

void WriteNewPixelToAOIT(float2 Position, float  surfaceDepth, float4 surfaceColor)
{
	// From now on serialize all UAV accesses (with respect to other fragments shaded in flight which map to the same pixel)
	ATSPNode nodeArray[AOIT_NODE_COUNT];
	uint2 pixelAddr = uint2(Position.xy);


	// Load AOIT control surface
	AOITCtrlSurface ctrlSurface;
	AOITLoadControlSurfaceUAV(pixelAddr, ctrlSurface);

	// If we are modifying this pixel for the first time we need to clear the AOIT data
	if (ctrlSurface.clear)
	{
		// Clear AOIT data and initialize it with first transparent layer
		AOITSPData data;
		AOITSPClearData(data, surfaceDepth, surfaceColor);

		// Store AOIT data
		uint addr = AOITAddrGenUAV(pixelAddr);
#if AOIT_NODE_COUNT != 2
		_AOITSPDepthDataUAV[addr] = data.depth;
#endif
		_AOITSPColorDataUAV[addr] = data.color;

		// Update control surface
        // ( depth and opaque flag can be used to branch out early if adding behind already near-opaque contents of AOIT )
		// ctrlSurface.clear  = false;
		// ctrlSurface.opaque = false; // 1.f == surfaceColor.w;
		// ctrlSurface.depth  = 0; // surfaceDepth;
		// AOITStoreControlSurface(pixelAddr, ctrlSurface);

//        gAOITSPClearMaskUAV[pixelAddr] = 0;
        imageStore(gAOITSPClearMaskUAV, pixelAddr, uint4(0));
	}
	else
	{
		// Load AOIT data
		AOITSPLoadDataUAV(pixelAddr, nodeArray);

		// Update AOIT data
		AOITSPInsertFragment(surfaceDepth,
							 1.0f - surfaceColor.w,  // transmittance = 1 - alpha
							 surfaceColor.xyz,
							 nodeArray);
		// Store AOIT data
		AOITSPStoreDataUAV(pixelAddr, nodeArray);
	}
}

#endif // H_AOIT

