// Copyright 2012 Intel Corporation
// All Rights Reserved

#ifndef H_AVSM_GEN
#define H_AVSM_GEN
#include "AVSM_Gen_def.h"

//////////////////////////////////////////////
// Structs
//////////////////////////////////////////////

struct AVSMGenData
{
	// this holds the nodes for a single pixel in depth 1 f32 per node w/ lower 8 bits = transmittence
	vec4 data[AVSM_RT_COUNT];
};

struct AVSMGenNode
{
    float  depth;
    float  trans;
};

//////////////////////////////////////////////
// Resources
//////////////////////////////////////////////
#if 0
RWTexture2D<float>                  gAVSMGenClearMaskUAV;
RWStructuredBuffer<AVSMGenData>     gAVSMGenDataUAV;

Texture2D<float>                    NONCPUT_gAVSMGenClearMaskSRV        : register(t29);
StructuredBuffer<AVSMGenData>       NONCPUT_gAVSMGenDataSRV             : register(t25);

SamplerState						gAVSMGenCtrlSurfaceSampler          : register(s3);     // set in the CPUT DefaultRenderStates
#else
layout(binding = 0) uniform image2D gAVSMGenClearMaskUAV;
layout(binding = 0) buffer ShaderBuffer0
{
    AVSMGenData gAVSMGenDataUAV[];
};

layout(binding = 0) uniform sampler2D NONCPUT_gAVSMGenClearMaskSRV;
layout(binding = 1) buffer ShaderBuffer1   // todo
{
    AVSMGenData NONCPUT_gAVSMGenDataSRV[];
};
#endif

//////////////////////////////////////////////
// Main AVSM fragment insertion code
//////////////////////////////////////////////

struct AVSMGenSegment {
	int	  index;
	float depthA;
	float depthB;
	float transA;
	float transB;
};

float AVSMGenLinearInterp(in float d, in AVSMGenSegment seg)
{
	const  float m = (seg.transB - seg.transA) * rcp(seg.depthB - seg.depthA);
	return seg.transA + m * (d - seg.depthA);
}

float AVSMCompMetricSoft(float d0, float d1, float d2, float t0, float t1, float t2)
{
	return abs((d2-d0)*(t2-t1) - (d2-d1)*(t2-t0));
}

// This can be used to try out different node count configs, but less than 4 nodes
// is untested
//#define AVSM_NODE_COUNT_CUT (AVSM_NODE_COUNT-2) <-- you get 2 and 6 nodes with this
#define AVSM_NODE_COUNT_CUT (AVSM_NODE_COUNT)

void AVSMGenInsertFragmentSoft(in float depthLeft, in float depthRight, in float fragmentTrans, inout AVSMGenNode nodeArray[AVSM_NODE_COUNT])
{
	int i, j;
    float  depth[AVSM_NODE_COUNT_CUT + 2];
    float  trans[AVSM_NODE_COUNT_CUT + 2];

	const float EMPTY_NODE = asfloat(asuint(float(AVSM_GEN_EMPTY_NODE_DEPTH)) & uint(AVSM_GEN_TRANS_MASK));

    ///////////////////////////////////////////////////
    // Unpack AVSM data
    ///////////////////////////////////////////////////
    /*[unroll]*/ for (i = 0; i < AVSM_NODE_COUNT_CUT; ++i) {
        depth[i] = nodeArray[i].depth;
        trans[i] = nodeArray[i].trans;
    }
	depth[AVSM_NODE_COUNT_CUT] = EMPTY_NODE;

    // Find LEFT
	AVSMGenSegment segLeft = /*(AVSMGenSegment)(0)*/ {0, 0.0, 0.0, 0.0, 0.0};
    /*[unroll]*/ for (i = 0; i < AVSM_NODE_COUNT_CUT; ++i) {
        /*[flatten]*/if (depthLeft > depth[i]) {
            segLeft.index++;
			segLeft.depthA = depth[i];
            segLeft.transA = trans[i];
			const int nextIdx = i + 1;
			/*[flatten]*/if (nextIdx < AVSM_NODE_COUNT_CUT) {
				segLeft.depthB = depth[nextIdx];
				segLeft.transB = trans[nextIdx];
			}
        }
    }

    // Find RIGHT
	AVSMGenSegment segRight = /*(AVSMGenSegment)(0)*/{0, 0.0, 0.0, 0.0, 0.0};
    /*[unroll]*/ for (i = 0; i < AVSM_NODE_COUNT_CUT; ++i) {
        /*[flatten]*/if (depthRight > depth[i]) {
            segRight.index++;
			segRight.depthA = depth[i];
            segRight.transA = trans[i];
			const int nextIdx = i + 1;
			/*[flatten]*/if (nextIdx < AVSM_NODE_COUNT_CUT) {
				segRight.depthB = depth[nextIdx];
				segRight.transB = trans[nextIdx];
			}
        }
    }
	// increment by 1 to make room for the insertion of the left node
	segRight.index++;

    // Compute transmittance at location of the left node (before insertion)
	float transLeft;
	/*[flatten]*/if (depthLeft <= depth[0]) {
		transLeft = float(AVSM_GEN_MAX_UNNORM_TRANS);
	} else if (depthLeft > depth[AVSM_NODE_COUNT_CUT - 1]) {
		transLeft = trans[AVSM_NODE_COUNT_CUT - 1];
	} else {
		transLeft = AVSMGenLinearInterp(depthLeft, segLeft);
	}

    // Compute transmittance at location of the right node (before insertion)
	float transRight;
	/*[flatten]*/if (depthRight <= depth[0]) {
		transRight = float(AVSM_GEN_MAX_UNNORM_TRANS);
	} else if (depthRight > depth[AVSM_NODE_COUNT_CUT - 1]) {
		transRight = trans[AVSM_NODE_COUNT_CUT - 1];
	} else {
		transRight = AVSMGenLinearInterp(depthRight, segRight);
	}

	AVSMGenSegment newSeg = {0, depthLeft, depthRight, 1.0, fragmentTrans};

	/*[unroll]*/for (i = AVSM_NODE_COUNT_CUT + 1; i >=0; i--) {
		float d, t;
		/*[flatten]*/if (i > segRight.index) {
			d = depth[i - 2];
			t = trans[i - 2] * fragmentTrans;
		} else if (i == segRight.index) {
			d = depthRight;
			t = transRight * fragmentTrans;
		} else if (i > segLeft.index) {
			d = depth[i - 1];
			t = trans[i - 1] * AVSMGenLinearInterp(d, newSeg);
		} else if (i == segLeft.index) {
			d = depthLeft;
			t = transLeft;
		} else {
			d = depth[i];
			t = trans[i];
		}

		depth[i] = d;
		trans[i] = t;
	}

    // pack representation if we have too many nodes
    /*[branch]*/if (depth[AVSM_NODE_COUNT_CUT] != EMPTY_NODE)
	{
        // That's total number of nodes that can be possibly removed
        int removalCandidateCount = (AVSM_NODE_COUNT_CUT + 2) - 1;
		const int startRemovalIdx = 1;

        float nodeUnderError[AVSM_NODE_COUNT_CUT + 1];
        /*[unroll]*/for (i = startRemovalIdx; i < removalCandidateCount; ++i) {
            nodeUnderError[i] = AVSMCompMetricSoft(depth[i-1], depth[i], depth[i+1],
												   trans[i-1], trans[i], trans[i+1]);
        }

        // Find the node the generates the smallest removal error
        int smallestErrorIdx = startRemovalIdx;
        float smallestError  = nodeUnderError[smallestErrorIdx];

        /*[unroll]*/for (i = startRemovalIdx + 1; i < removalCandidateCount; ++i) {
            /*[flatten]*/if (nodeUnderError[i] < smallestError) {
                smallestError = nodeUnderError[i];
                smallestErrorIdx = i;
            }
        }

        // Remove that node..
        /*[unroll]*/for (i = startRemovalIdx; i < AVSM_NODE_COUNT_CUT + 1; ++i) {
            /*[flatten]*/if (smallestErrorIdx <= i) {
                depth[i] = depth[i + 1];
                trans[i] = trans[i + 1];
            }
        }

		/////////////////////////////////////////

		// That's total number of nodes that can be possibly removed
        removalCandidateCount--;

        /*[unroll]*/for (i = startRemovalIdx; i < removalCandidateCount; ++i) {
            nodeUnderError[i] = AVSMCompMetricSoft(depth[i-1], depth[i], depth[i+1],
												   trans[i-1], trans[i], trans[i+1]);
        }

        // Find the node the generates the smallest removal error
        smallestErrorIdx = startRemovalIdx;
        smallestError  = nodeUnderError[smallestErrorIdx];

        /*[unroll]*/for (i = startRemovalIdx + 1; i < removalCandidateCount; ++i) {
            /*[flatten]*/if (nodeUnderError[i] < smallestError) {
                smallestError = nodeUnderError[i];
                smallestErrorIdx = i;
            }
        }

        // Remove that node..
        /*[unroll]*/for (i = startRemovalIdx; i < AVSM_NODE_COUNT_CUT; ++i) {
            /*[flatten]*/if (smallestErrorIdx <= i) {
                depth[i] = depth[i + 1];
                trans[i] = trans[i + 1];
            }
        }
    }

    // Pack AVSM data
    /*[unroll]*/ for (i = 0; i < AVSM_NODE_COUNT_CUT; ++i) {
        nodeArray[i].depth = depth[i];
        nodeArray[i].trans = trans[i];
    }
}

/////////////////////////////////////////////////
// Address generation functions for the AVSM data
/////////////////////////////////////////////////

uint AVSMGenAddrGen(uint2 addr2D, uint surfaceWidth)
{
	return addr2D[0] + surfaceWidth * addr2D[1];
}

uint AVSMGenAddrGenUAV(uint2 addr2D)
{
	uint2 dim;
	gAVSMGenClearMaskUAV.GetDimensions(dim[0], dim[1]);
	return AVSMGenAddrGen(addr2D, dim[0]);
}

uint AVSMGenAddrGenSRV(uint2 addr2D)
{
	/*uint2 dim;
	NONCPUT_gAVSMGenClearMaskSRV.GetDimensions(dim[0], dim[1]);*/

	int2 dim = textureSize(NONCPUT_gAVSMGenClearMaskSRV, 0);
	return AVSMGenAddrGen(addr2D, dim[0]);
}

/////////////////////////////////////////////////
// Clear function for the AVSM data
/////////////////////////////////////////////////

void AVSMGenInitDataSoft(inout AVSMGenData data, float depthLeft, float depthRight, float fragmentTrans)
{

	const uint  packedTrans = (uint(float(AVSM_GEN_MAX_UNNORM_TRANS) * fragmentTrans)) & uint(AVSM_GEN_MAX_UNNORM_TRANS);
    const float defaultData = asfloat((asuint(float(AVSM_GEN_EMPTY_NODE_DEPTH)) &
                              uint(AVSM_GEN_TRANS_MASK)) | packedTrans);
    const float firstNode   = asfloat(asuint(depthLeft)  | uint(AVSM_GEN_MAX_UNNORM_TRANS));
	const float secondNode  = asfloat((asuint(depthRight) & uint(AVSM_GEN_TRANS_MASK)) | packedTrans);

    /*[unroll]*/for(uint i = 0; i < AVSM_RT_COUNT; i++) {
		data.data[i] = float4(defaultData, defaultData, defaultData, defaultData);
	}
	data.data[0][0] = firstNode;
	data.data[0][1] = secondNode;
}

/////////////////////////////////////////////////
// Load/store functions for the AVSM data
/////////////////////////////////////////////////

void AVSMGenLoadRawDataSRV(in uint2 pixelAddr, out AVSMGenData avsmData)
{
	uint addr = AVSMGenAddrGenSRV(pixelAddr);
	avsmData = NONCPUT_gAVSMGenDataSRV[addr];
}

void AVSMGenLoadDataSRV(in uint2 pixelAddr, out AVSMGenNode nodeArray[AVSM_NODE_COUNT])
{
	uint addr = AVSMGenAddrGenSRV(pixelAddr);
	AVSMGenData avsmData = NONCPUT_gAVSMGenDataSRV[addr];

    uint data;
    float depth, trans;
	/*[unroll]*/for(uint i = 0; i < AVSM_RT_COUNT; i++) {
		[unroll]for(uint j = 0; j < 4; j++) {
            data   = avsmData.data[i][j];

            nodeArray[4 * i + j].depth = asfloat(data & (uint)AVSM_GEN_TRANS_MASK);
            nodeArray[4 * i + j].trans = (float)(data & (uint)AVSM_GEN_MAX_UNNORM_TRANS);
		}
	}
}

void AVSMGenLoadDataUAV(in uint2 pixelAddr, out AVSMGenNode nodeArray[AVSM_NODE_COUNT])
{
	uint addr = AVSMGenAddrGenUAV(pixelAddr);
	AVSMGenData avsmData = gAVSMGenDataUAV[addr];

    uint data;
    float depth, trans;
	/*[unroll]*/for(uint i = 0; i < AVSM_RT_COUNT; i++) {
		/*[unroll]*/for(uint j = 0; j < 4; j++) {
            data   = asuint(avsmData.data[i][j]);

            nodeArray[4 * i + j].depth = asfloat(data & uint(AVSM_GEN_TRANS_MASK));
            nodeArray[4 * i + j].trans = float(data & uint(AVSM_GEN_MAX_UNNORM_TRANS));  // TODO
		}
	}
}

void AVSMGenStoreRawDataUAV(in uint2 pixelAddr, in AVSMGenData avsmData)
{
	uint addr = AVSMGenAddrGenUAV(pixelAddr);
	gAVSMGenDataUAV[addr] = avsmData;
}

void AVSMGenStoreDataUAV(in uint2 pixelAddr, AVSMGenNode nodeArray[AVSM_NODE_COUNT])
{
	AVSMGenData avsmData;
	/*[unroll]*/for(uint i = 0; i < AVSM_RT_COUNT; i++) {
		/*[unroll]*/for(uint j = 0; j < 4; j++) {
            const uint depth = asuint(nodeArray[4 * i + j].depth) & uint(AVSM_GEN_TRANS_MASK);
            const uint trans = uint(nodeArray[4 * i + j].trans) & uint(AVSM_GEN_MAX_UNNORM_TRANS);

			avsmData.data[i][j] = asfloat(depth | trans);
		}
	}

	uint addr = AVSMGenAddrGenUAV(pixelAddr);
	gAVSMGenDataUAV[addr] = avsmData;
}

/////////////////////////////////////////////////
// Control surface functions
/////////////////////////////////////////////////

float AVSMGenLoadControlSurfaceUAV(in uint2 pixelAddr)
{
//	return gAVSMGenClearMaskUAV[pixelAddr];
    return imageLod(gAVSMGenClearMaskUAV, int2(pixelAddr)).r;
}

float AVSMGenLoadControlSurfaceSRV(in uint2 pixelAddr)
{
//	return NONCPUT_gAVSMGenClearMaskSRV[pixelAddr];
    return texelFetch(NONCPUT_gAVSMGenClearMaskSRV, int2(pixelAddr), 0).r;
}

float4 AVSMGenGather4ControlSurfaceSRV(in float2 textureCoords)
{
//	return NONCPUT_gAVSMGenClearMaskSRV.Gather(gAVSMGenCtrlSurfaceSampler, textureCoords);
	return textureGather(NONCPUT_gAVSMGenClearMaskSRV, textureCoords);
}

void AVSMGenStoreControlSurface(in uint2 pixelAddr, float ctrlSurface)
{
//	gAVSMGenClearMaskUAV[pixelAddr] = ctrlSurface;
    imageStore(gAVSMGenClearMaskUAV, int2(pixelAddr), float4(ctrlSurface,0,0,0));
}

/////////////////////////////////////////////////
// AVSM sampling/filtering functions
/////////////////////////////////////////////////

float AVSMGenUnpackTrans(float value)
{
	return float(asuint(value) & uint(AVSM_GEN_MAX_UNNORM_TRANS)) / float(AVSM_GEN_MAX_UNNORM_TRANS);
}

float AVSMGenPointSampleInternalSoft(in int2 pixelAddr, in float receiverDepth)
{
	float rec = receiverDepth;

    // mask out transmittance during depth search
    receiverDepth = asfloat(asuint(receiverDepth) | uint(AVSM_GEN_MAX_UNNORM_TRANS));

    AVSMGenData avsmData;
    AVSMGenLoadRawDataSRV(pixelAddr, avsmData);

	float value;
	/*[flatten]*/if (receiverDepth <= avsmData.data[0][0]) {
		value = 1;
	} else /*[flatten]*/ if (receiverDepth > avsmData.data[AVSM_RT_COUNT - 1][3]) {
		value = AVSMGenUnpackTrans(avsmData.data[AVSM_RT_COUNT - 1][3]);
	} else {
		float nextNode;
		/*[unroll]*/for (int i = AVSM_RT_COUNT - 1; i >= 0; i--) {
			/*[unroll]*/ for (int j = 3; j >= 0; j--) {
				/*[flatten]*/if (receiverDepth > avsmData.data[i][j]) {
					// node found! make it negative so it won't be updated anymore
					receiverDepth = asfloat(asuint(avsmData.data[i][j]) | 0x80000000UL);
					/*[flatten]*/if (3 != j) {
							nextNode = avsmData.data[i][j + 1];
					} else if (AVSM_RT_COUNT - 1 != i) {
							nextNode = avsmData.data[i + 1][0];
					}
				}
			}
		}


		const float transLeft  = AVSMGenUnpackTrans(receiverDepth);
		const float transRight = AVSMGenUnpackTrans(nextNode);
		const float depthLeft  = asfloat(asuint(receiverDepth) & (uint)AVSM_GEN_TRANS_MASK2);
		const float depthRight = asfloat(asuint(nextNode) & (uint)AVSM_GEN_TRANS_MASK);

		AVSMGenSegment seg = {0, depthLeft, depthRight, transLeft, transRight};

		value = AVSMGenLinearInterp(rec, seg);
	}
	return value;
}

float AVSMGenPointSampleSoft(in float2 textureCoords, in float receiverDepth)
{
    /*uint2 dim;
	NONCPUT_gAVSMGenClearMaskSRV.GetDimensions(dim[0], dim[1]);*/
	int2 dim = textureSize(NONCPUT_gAVSMGenClearMaskSRV, 0);
    const int2 pixelAddr = int2(textureCoords * float2(dim));

	float shadow = 1;
    float ctrlSurface = AVSMGenLoadControlSurfaceSRV(pixelAddr);

    if (0 != ctrlSurface) {
		shadow = AVSMGenPointSampleInternalSoft(pixelAddr, receiverDepth);
    }
    return shadow;
}

float AVSMGenBilinearSampleSoft(in float2 textureCoords, in float receiverDepth)
{
//	uint2 dim;
    float shadow = 1;
//	NONCPUT_gAVSMGenClearMaskSRV.GetDimensions(dim[0], dim[1]);
	int2 dim = textureSize(NONCPUT_gAVSMGenClearMaskSRV, 0);
	const float2 unnormCoords = textureCoords * float2(dim);

	const float a = frac(unnormCoords.x  - 0.5f);
	const float b = frac(unnormCoords.y  - 0.5f);
	const int i   = int(unnormCoords.x - 0.5f);
	const int j   = int(unnormCoords.y - 0.5f);

	// replacing the following four texture fetches with a gather4
	// generates artifacts since the second group of four
	// texture fetches uses texture coordinates that don't always
	// match the HW generated coordinates used by gather4
	bool4 mustSample;
	mustSample[0] = 0 != NONCPUT_gAVSMGenClearMaskSRV[int2(i, j)];
	mustSample[1] = 0 != NONCPUT_gAVSMGenClearMaskSRV[int2(i, j + 1)];
	mustSample[2] = 0 != NONCPUT_gAVSMGenClearMaskSRV[int2(i + 1, j)];
	mustSample[3] = 0 != NONCPUT_gAVSMGenClearMaskSRV[int2(i + 1, j + 1)];

	/*[branch]*/if (any(mustSample)) {
		const float sample00 = mustSample[0] ? AVSMGenPointSampleInternalSoft(int2(i, j)         , receiverDepth) : 1;
		const float sample01 = mustSample[1] ? AVSMGenPointSampleInternalSoft(int2(i, j + 1)     , receiverDepth) : 1;
		const float sample10 = mustSample[2] ? AVSMGenPointSampleInternalSoft(int2(i + 1, j)     , receiverDepth) : 1;
		const float sample11 = mustSample[3] ? AVSMGenPointSampleInternalSoft(int2(i + 1, j + 1) , receiverDepth) : 1;
		shadow = (1 - a)*(1 - b)*sample00 + a*(1-b)*sample10 + (1-a)*b*sample01 + a*b*sample11;
	}
    return shadow;
}

#ifndef AVSM_GEN_SOFT

//////////////////////////////////////////////
// Main AVSM fragment insertion code
//////////////////////////////////////////////

void AVSMGenInsertFragment(in float fragmentDepth, in float  fragmentTrans, inout AVSMGenNode nodeArray[AVSM_NODE_COUNT])
{
    int i, j;
    float  depth[AVSM_NODE_COUNT_CUT + 1];
    float  trans[AVSM_NODE_COUNT_CUT + 1];

    ///////////////////////////////////////////////////
    // Unpack AVSM data
    ///////////////////////////////////////////////////
    /*[unroll]*/ for (i = 0; i < AVSM_NODE_COUNT_CUT; ++i) {
        depth[i] = nodeArray[i].depth;
        trans[i] = nodeArray[i].trans;
    }

    // Find insertion index
    int index = 0;
    float prevTrans = 1.0f;
    /*[unroll]*/ for (i = 0; i < AVSM_NODE_COUNT_CUT; ++i) {
        if (fragmentDepth > depth[i]) {
            index++;
            prevTrans = trans[i];
        }
    }

    // Make room for the new fragment. Also composite new fragment with the current curve
    // (except for the node that represents the new fragment)
    /*[unroll]*/for (i = AVSM_NODE_COUNT_CUT - 1; i >= 0; --i) {
        /*[flatten]*/if (index <= i) {
            depth[i + 1] = depth[i];
            trans[i + 1] = trans[i] * fragmentTrans;
        }
    }

    // Insert new fragment
    /*[unroll]*/for (i = 0; i <= AVSM_NODE_COUNT_CUT; ++i) {
        /*[flatten]*/if (index == i) {
            depth[i] = fragmentDepth;
            trans[i] = fragmentTrans * prevTrans;
        }
    }

    // pack representation if we have too many nodes
    /*[branch]*/if (depth[AVSM_NODE_COUNT_CUT] != AVSM_GEN_EMPTY_NODE_DEPTH) {

        // That's total number of nodes that can be possibly removed
        const int removalCandidateCount = (AVSM_NODE_COUNT_CUT + 1) - 1;
		const int startRemovalIdx = 1;

        float nodeUnderError[removalCandidateCount];
        /*[unroll]*/for (i = startRemovalIdx; i < removalCandidateCount; ++i) {
            nodeUnderError[i] = (depth[i] - depth[i - 1]) * (trans[i - 1] - trans[i]);
        }

        // Find the node the generates the smallest removal error
        int smallestErrorIdx = startRemovalIdx;
        float smallestError  = nodeUnderError[smallestErrorIdx];

        /*[unroll]*/for (i = startRemovalIdx + 1; i < removalCandidateCount; ++i) {
            /*[flatten]*/if (nodeUnderError[i] < smallestError) {
                smallestError = nodeUnderError[i];
                smallestErrorIdx = i;
            }
        }

        // Remove that node..
        /*[unroll]*/for (i = startRemovalIdx; i < AVSM_NODE_COUNT_CUT; ++i) {
            /*[flatten]*/if (smallestErrorIdx <= i) {
                depth[i] = depth[i + 1];
            }
        }
        /*[unroll]*/for (i = startRemovalIdx - 1; i < AVSM_NODE_COUNT_CUT; ++i) {
            /*[flatten]*/if (smallestErrorIdx - 1 <= i) {
                trans[i] = trans[i + 1];
            }
        }
    }

    // Pack AVSM data
    /*[unroll]*/ for (i = 0; i < AVSM_NODE_COUNT_CUT; ++i) {
        nodeArray[i].depth = depth[i];
        nodeArray[i].trans = trans[i];
    }
}

void AVSMGenInitData(inout AVSMGenData data, float depth, float trans)
{
	const uint  packedTrans = (uint(float(AVSM_GEN_MAX_UNNORM_TRANS) * trans)) & uint(AVSM_GEN_MAX_UNNORM_TRANS);
    const float defaultData = asfloat((asuint(float(AVSM_GEN_EMPTY_NODE_DEPTH)) &
                              uint(AVSM_GEN_TRANS_MASK)) | packedTrans);
    const float firstNode   = asfloat((asuint(depth) & uint(AVSM_GEN_TRANS_MASK)) | packedTrans);

    /*[unroll]*/for(uint i = 0; i < AVSM_RT_COUNT; i++) {
		data.data[i] = float4(defaultData, defaultData, defaultData, defaultData);
	}
	data.data[0][0] = firstNode;
}

float AVSMGenPointSampleInternal(in int2 pixelAddr, in float receiverDepth)
{
    // mask out transmittance during depth search
    receiverDepth = asfloat(asuint(receiverDepth) | uint(AVSM_GEN_MAX_UNNORM_TRANS));

    AVSMGenData avsmData;
    AVSMGenLoadRawDataSRV(pixelAddr, avsmData);

    /*[unroll]*/for (int i = AVSM_RT_COUNT - 1; i >= 0; i--) {
        /*[unroll]*/ for (int j = 3; j >= 0; j--) {
            /*[flatten]*/if (receiverDepth > avsmData.data[i][j]) {
				// node found! make it negative so it won't be updated anymore
				receiverDepth = asfloat(asuint(avsmData.data[i][j]) | 0x80000000UL);
            }
        }
    }

	return float(asint(receiverDepth) & uint(AVSM_GEN_MAX_UNNORM_TRANS)) * rcp(float(AVSM_GEN_MAX_UNNORM_TRANS));
}

float AVSMGenPointSample(in float2 textureCoords, in float receiverDepth)
{
    /*uint2 dim;
	NONCPUT_gAVSMGenClearMaskSRV.GetDimensions(dim[0], dim[1]);*/
	int2 dim = textureSize(NONCPUT_gAVSMGenClearMaskSRV, 0);
    const int2 pixelAddr = int2(textureCoords * float2(dim));

	float shadow = 1;
    float ctrlSurface = AVSMGenLoadControlSurfaceSRV(pixelAddr);

    if (0 != ctrlSurface) {
		shadow = AVSMGenPointSampleInternal(pixelAddr, receiverDepth);
    }
    return shadow;
}

float AVSMGenBilinearSample(in float2 textureCoords, in float receiverDepth)
{
    float shadow = 1;
	const float4 ctrlSurface = AVSMGenGather4ControlSurfaceSRV(textureCoords);
	const bool4  clearFlag	 = equal(ctrlSurface, float4(0, 0, 0, 0));

	/*[branch]*/if (!all(clearFlag)) {
//		uint2 dim;
		const int2 pixelAddr = int2(textureCoords);

//		NONCPUT_gAVSMGenClearMaskSRV.GetDimensions(dim[0], dim[1]);
		int2 dim = textureSize(NONCPUT_gAVSMGenClearMaskSRV, 0);
		const float2 unnormCoords = textureCoords * float2(dim);

		const float a = frac(unnormCoords.x  - 0.5f);
		const float b = frac(unnormCoords.y  - 0.5f);
		const float i = floor(unnormCoords.x - 0.5f);
		const float j = floor(unnormCoords.y - 0.5f);

		const float sample00 = clearFlag[3] ? 1 : AVSMGenPointSampleInternal(float2(i, j)         , receiverDepth);
		const float sample01 = clearFlag[0] ? 1 : AVSMGenPointSampleInternal(float2(i, j + 1)     , receiverDepth);
		const float sample10 = clearFlag[2] ? 1 : AVSMGenPointSampleInternal(float2(i + 1, j)     , receiverDepth);
		const float sample11 = clearFlag[1] ? 1 : AVSMGenPointSampleInternal(float2(i + 1, j + 1) , receiverDepth);

		shadow = (1 - a)*(1 - b)*sample00 + a*(1-b)*sample10 + (1-a)*b*sample01 + a*b*sample11;
	}
    return shadow;
}

#endif // #ifndef AVSM_GEN_SOFT

#endif // H_AVSM_GEN

