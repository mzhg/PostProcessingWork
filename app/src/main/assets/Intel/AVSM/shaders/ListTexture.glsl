// Copyright 2012 Intel Corporation
// All Rights Reserved

//--------------------------------------------------------------------------------------
// List Texture
//--------------------------------------------------------------------------------------

#include "Common.glsl"
#ifndef H_LIST_TEXTURE
#define H_LIST_TEXTURE

#define NODE_LIST_NULL 0xFFFFFFFFU

//#define ENABLE_PACKED_NODES

//////////////////////////////////////////////
// Structs
//////////////////////////////////////////////

struct ListTexSegmentNode
{
    uint    next;
    float   depth[2];
    float   trans;
    float   sortKey;
};

struct ListTexVisibilityNode
{
    uint    next;
    float   depth;
    float   trans;
};

struct ListTexPackedVisibilityNode
{
    uint    next;
#ifdef ENABLE_PACKED_NODES
    // depth (16 MSBs) and transmittance (16 LSBs)are packed as half precision values
    uint    pck_depth_trans;
#else
    float   depth;
    float   trans;
#endif
};

//////////////////////////////////////////////
// Resource Views
//////////////////////////////////////////////

#if 0
RWTexture2D<uint>                          gListTexFirstSegmentNodeAddressUAV;
Texture2D<uint>                            gListTexFirstSegmentNodeAddressSRV       : register(t31);

RWStructuredBuffer<ListTexSegmentNode>     gListTexSegmentNodesUAV;
StructuredBuffer<ListTexSegmentNode>       gListTexSegmentNodesSRV                  : register(t32);

RWTexture2D<uint>                          gListTexFirstVisibilityNodeAddressUAV;
Texture2D<uint>                            gListTexFirstVisibilityNodeAddressSRV    : register(t16);

RWStructuredBuffer<ListTexVisibilityNode>  gListTexVisibilityNodesUAV;
StructuredBuffer<ListTexVisibilityNode>    gListTexVisibilityNodesSRV               : register(t17);

// Sorted (front to back) list of entry points
RWTexture2D<uint>                          gListTexFirstSrtdEntryAddressUAV;
Texture2D<uint>                            gListTexFirstSrtdEntryAddressSRV         : register(t18);

RWStructuredBuffer<ListTexVisibilityNode>  gListTexSrtdEntryUAV;
StructuredBuffer<ListTexVisibilityNode>    gListTexSrtdEntrySRV                     : register(t19);

// Sorted (front to back) list of exit points
RWTexture2D<uint>                          gListTexFirstSrtdExitAddressUAV;
Texture2D<uint>                            gListTexFirstSrtdExitAddressSRV          : register(t20);

RWStructuredBuffer<ListTexVisibilityNode>  gListTexSrtdExitUAV;
StructuredBuffer<ListTexVisibilityNode>    gListTexSrtdExitSRV                      : register(t21);
#else
layout(binding =0, r32ui) uniform uimage2D gListTexFirstSegmentNodeAddressUAV;
layout(binding =7) uniform usampler2D gListTexFirstSegmentNodeAddressSRV;

layout(binding = 0) buffer ListShaderBuffer0
{
  ListTexSegmentNode gListTexSegmentNodesUAV[];
};

layout(binding = 11) buffer ListUniformBuffer0
{
    ListTexSegmentNode gListTexSegmentNodesSRV[];
};

layout(binding = 1, offset = 0) uniform atomic_uint gListTexSegmentNodesAtomicCounter;

layout(binding =1, r32ui) uniform uimage2D gListTexFirstVisibilityNodeAddressUAV;
layout(binding =1) uniform usampler2D gListTexFirstVisibilityNodeAddressSRV;

layout(binding = 1) buffer ListShaderBuffer1
{
    ListTexVisibilityNode gListTexVisibilityNodesUAV[];
};

layout(binding = 1) uniform ListUniformBuffer1
{
    ListTexVisibilityNode gListTexVisibilityNodesSRV[];
};

layout(binding = 5, offset = 4) uniform atomic_uint gListTexVisibilityNodesAtomicCounter;

// Sorted (front to back) list of entry points
layout(binding =2, r32ui) uniform uimage2D gListTexFirstSrtdEntryAddressUAV;
layout(binding =2) uniform usampler2D gListTexFirstSrtdEntryAddressSRV;

layout(binding = 2) buffer ListShaderBuffer2
{
    ListTexVisibilityNode gListTexSrtdEntryUAV[];
};

layout(binding = 2) uniform ListUniformBuffer2
{
    ListTexVisibilityNode gListTexSrtdEntrySRV[];
};

layout(binding = 5, offset = 8) uniform atomic_uint gListTexSrtdEntryAtomicCounter;

// Sorted (front to back) list of exit points
layout(binding =3, r32ui) uniform uimage2D gListTexFirstSrtdExitAddressUAV;
layout(binding =3) uniform usampler2D gListTexFirstSrtdExitAddressSRV;

layout(binding = 3) buffer ListShaderBuffer3
{
    ListTexVisibilityNode gListTexSrtdExitUAV[];
};

layout(binding = 3) uniform ListUniformBuffer3
{
    ListTexVisibilityNode gListTexSrtdExitSRV[];
};

layout(binding = 5, offset = 12) uniform atomic_uint gListTexSrtdExitAtomicCounter;

#endif

//////////////////////////////////////////////
// Constants
//////////////////////////////////////////////

#if 0
cbuffer LT_Constants : register(b9)
{
    uint  mMaxNodes;
    float mFirstNodeMapSize;
}
#else

layout(binding = 4) uniform LT_Constants
{
    uint  mMaxNodes;
    float mFirstNodeMapSize;
};

#endif

//////////////////////////////////////////////
// Segment related functions
//////////////////////////////////////////////

uint LT_GetFirstSegmentNodeOffset(int2 screenAddress)
{
//    return gListTexFirstSegmentNodeAddressSRV[screenAddress];
    return texelFetch(gListTexFirstSegmentNodeAddressSRV, screenAddress, 0).x;
}

bool LT_AllocSegmentNode(out uint newNodeAddress1D)
{
    // alloc a new node
//    newNodeAddress1D = gListTexSegmentNodesUAV.IncrementCounter();
    newNodeAddress1D  = atomicCounterIncrement(gListTexSegmentNodesAtomicCounter);

#ifdef AVSM_ENABLE_MEMORY_STATS
    uint realMaxNodes = mMaxNodes - 1;
    ListTexSegmentNode stats;
    stats.next = newNodeAddress1D; // allocated nodes
    // Write out some sentinel values in case we want to validate
    // that this node did not get stomped on.
    stats.depth[0] = 64738;
    stats.depth[1] = 12345;
    stats.trans    = 13579;
    stats.sortKey  = 24680;
    gListTexSegmentNodesUAV[realMaxNodes] = stats;
#else
    uint realMaxNodes = mMaxNodes;
#endif

    // running out of memory?
    return newNodeAddress1D <= realMaxNodes;
}

// Insert a new node at the head of the list
void LT_InsertFirstSegmentNode(in int2 screenAddress, in uint newNodeAddress, in ListTexSegmentNode newNode)
{
    uint oldNodeAddress;
//    InterlockedExchange(gListTexFirstSegmentNodeAddressUAV[screenAddress], newNodeAddress, oldNodeAddress);
    oldNodeAddress = imageAtomicExchange(gListTexFirstSegmentNodeAddressUAV, screenAddress, newNodeAddress);

    newNode.next = oldNodeAddress;
    gListTexSegmentNodesUAV[newNodeAddress] =  newNode;
}

ListTexSegmentNode LT_GetSegmentNode(uint nodeAddress)
{
    return gListTexSegmentNodesSRV[nodeAddress];
}

//////////////////////////////////////////////
// General visibility encoding functions
//////////////////////////////////////////////

ListTexPackedVisibilityNode PackVisibilityNode(in ListTexVisibilityNode node)
{
    ListTexPackedVisibilityNode packedNode;

    packedNode.next   = node.next;

#ifdef ENABLE_PACKED_NODES
    packedNode.pck_depth_trans = ((f32tof16(node.depth) << 16) & 0xFFFF0000) | f32tof16(node.trans);
#else
    packedNode.depth  = node.depth;
    packedNode.trans  = node.trans;
#endif

    return packedNode;
}

ListTexVisibilityNode UnpackVisibilityNode(in ListTexPackedVisibilityNode packedNode)
{
    ListTexVisibilityNode node;

    node.next  = packedNode.next;

#ifdef ENABLE_PACKED_NODES
    node.depth = f16tof32(packedNode.pck_depth_trans >> 16);
    node.trans = f16tof32(packedNode.pck_depth_trans);
#else
    node.depth = packedNode.depth;
    node.trans = packedNode.trans;
#endif

    return node;
}

float LT_Interp(float d0, float d1, float t0, float t1, float r)
{
    float depth = linstep(d0, d1, r);
    return t0 + (t1 - t0) * depth;
}

uint LT_GetFirstVisibilityNodeOffset(int2 screenAddress)
{
//    return gListTexFirstVisibilityNodeAddressSRV[screenAddress];
    return texelFetch(gListTexFirstVisibilityNodeAddressSRV, screenAddress, 0).r;
}

uint LT_GetFirstSrtdEntryOffset(int2 screenAddress)
{
//    return gListTexFirstSrtdEntryAddressSRV[screenAddress];
    return texelFetch(gListTexFirstSrtdEntryAddressSRV, screenAddress, 0).r;
}

uint LT_GetFirstSrtdExitOffset(int2 screenAddress)
{
//    return gListTexFirstSrtdExitAddressSRV[screenAddress];
    return texelFetch(gListTexFirstSrtdExitAddressSRV, screenAddress, 0).r;
}

ListTexVisibilityNode LT_GetVisibilityNode(uint nodeAddress)
{
    return gListTexVisibilityNodesSRV[int(nodeAddress)];
}

ListTexVisibilityNode LT_GetSrtdEntryNode(uint nodeAddress)
{
    return gListTexSrtdEntrySRV[int(nodeAddress)];
}

ListTexVisibilityNode LT_GetSrtdExitNode(uint nodeAddress)
{
    return gListTexSrtdExitSRV[int(nodeAddress)];
}

bool LT_AllocVisibilityNode(out uint newNodeAddress1D)
{
    // alloc a new node
//    newNodeAddress1D = gListTexVisibilityNodesUAV.IncrementCounter();
    newNodeAddress1D = atomicCounterIncrement(gListTexVisibilityNodesAtomicCounter);

#ifdef AVSM_ENABLE_MEMORY_STATS
    uint realMaxNodes = mMaxNodes - 1;
    ListTexVisibilityNode stats;
    stats.next = newNodeAddress1D; // allocated nodes
    // Write out some sentinel values in case we want to validate
    // that this node did not get stomped on.
    stats.depth = 64738;
    stats.trans = 13579;
    gListTexVisibilityNodesUAV[realMaxNodes] = stats;
#else
    uint realMaxNodes = mMaxNodes;
#endif

    // running out of memory?
    return newNodeAddress1D <= realMaxNodes;
}

bool LT_AllocSrtdEntryNode(out uint newNodeAddress1D)
{
    // alloc a new node
//    newNodeAddress1D = gListTexSrtdEntryUAV.IncrementCounter();
    newNodeAddress1D = atomicCounterIncrement(gListTexSrtdEntryAtomicCounter);

    // running out of memory?
    return newNodeAddress1D <= mMaxNodes;
}

bool LT_AllocSrtdExitNode(out uint newNodeAddress1D)
{
    // alloc a new node
//    newNodeAddress1D = gListTexSrtdExitUAV.IncrementCounter();
    newNodeAddress1D = atomicCounterIncrement(gListTexSrtdExitAtomicCounter);

    // running out of memory?
    return newNodeAddress1D <= mMaxNodes;
}

// Insert a new node at the head of the list
void LT_InsertFirstVisibilityNode(in int2 screenAddress, in uint newNodeAddress, in ListTexVisibilityNode newNode)
{
    uint oldNodeAddress;
//    InterlockedExchange(gListTexFirstVisibilityNodeAddressUAV[screenAddress], newNodeAddress, oldNodeAddress);
    oldNodeAddress = imageAtomicExchange(gListTexFirstVisibilityNodeAddressUAV, screenAddress, newNodeAddress);

    newNode.next = oldNodeAddress;
    gListTexVisibilityNodesUAV[newNodeAddress] =  newNode;
}

// Insert a new node at the head of the list
void LT_InsertFirstSrtdEntryNode(in int2 screenAddress, in uint newNodeAddress, in ListTexVisibilityNode newNode)
{
    uint oldNodeAddress;
//    InterlockedExchange(gListTexFirstSrtdEntryAddressUAV[screenAddress], newNodeAddress, oldNodeAddress);
    oldNodeAddress = imageAtomicExchange(gListTexFirstSrtdEntryAddressUAV, screenAddress, newNodeAddress);

    newNode.next = oldNodeAddress;
    gListTexSrtdEntryUAV[newNodeAddress] =  newNode;
}

// Insert a new node at the head of the list
void LT_InsertFirstSrtdExitNode(in int2 screenAddress, in uint newNodeAddress, in ListTexVisibilityNode newNode)
{
    uint oldNodeAddress;
//    InterlockedExchange(gListTexFirstSrtdExitAddressUAV[screenAddress], newNodeAddress, oldNodeAddress);
    oldNodeAddress = imageAtomicExchange(gListTexFirstSrtdExitAddressUAV, screenAddress, newNodeAddress);

    newNode.next = oldNodeAddress;
    gListTexSrtdExitUAV[newNodeAddress] =  newNode;
}

// Insert a new node at the tail of the list
void LT_InsertLastVisibilityNode(in uint tailNodeAddress, in uint newNodeAddress, in ListTexVisibilityNode newNode)
{
    uint oldNodeAddress;
    newNode.next = NODE_LIST_NULL;
    gListTexVisibilityNodesUAV[newNodeAddress] =  newNode;
//    InterlockedExchange(gListTexVisibilityNodesUAV[tailNodeAddress].next, newNodeAddress, oldNodeAddress);
    atomicExchange(gListTexVisibilityNodesUAV[tailNodeAddress].next, newNodeAddress);
}

//////////////////////////////////////////////
// List Texture Segment Sampling Functions
//////////////////////////////////////////////

float LT_SegmentPointSample(in float2 uv, in float receiverDepth)
{
    float transmittance = 1.0f;

    // Get fragment  coordinates
    int2 screenAddress = int2(mFirstNodeMapSize.xx * uv);

    // Get offset to the first node
    uint nodeOffset = LT_GetFirstSegmentNodeOffset(screenAddress);

    // Fetch nodes
    ListTexSegmentNode node;
    /*[loop]*/while ((nodeOffset != NODE_LIST_NULL)) {
        // Get node..
        node = LT_GetSegmentNode(nodeOffset);

        // Compute and composite transmittance for this segment
        transmittance *= LT_Interp(node.depth[0], node.depth[1], 1.0f, node.trans, receiverDepth);

        // Move to next node
        nodeOffset = node.next;
    }

//    /*[flatten]*/if (any(uv > 1.0f.xx) || any(uv < 0.0f.xx))
    if(any(greaterThan(uv, vec2(1))) || any(lessThan(uv, vec2(0))))
    {
        transmittance = 1.0f;
    }

    return transmittance;
}

float LT_SegmentBilinearSample(in float2 textureCoords, in float receiverDepth)
{
    float2 unnormCoords = textureCoords * mFirstNodeMapSize.xx;

    const float a = frac(unnormCoords.x - 0.5f);
    const float b = frac(unnormCoords.y - 0.5f);
    const float i = floor(unnormCoords.x - 0.5f);
    const float j = floor(unnormCoords.y - 0.5f);

    float sample00 = LT_SegmentPointSample(float2(i, j)         / mFirstNodeMapSize.xx, receiverDepth);
    float sample01 = LT_SegmentPointSample(float2(i, j + 1)     / mFirstNodeMapSize.xx, receiverDepth);
    float sample10 = LT_SegmentPointSample(float2(i + 1, j)     / mFirstNodeMapSize.xx, receiverDepth);
    float sample11 = LT_SegmentPointSample(float2(i + 1, j + 1) / mFirstNodeMapSize.xx, receiverDepth);

	return (1 - a)*(1 - b)*sample00 + a*(1-b)*sample10 + (1-a)*b*sample01 + a*b*sample11;
}

//////////////////////////////////////////////
// List Texture Visibility Sampling Functions
//////////////////////////////////////////////

float LT_VisibilityPointSample(in float2 uv, in float receiverDepth)
{
    // Get fragment  coordinates
    int2 screenAddress = int2(mFirstNodeMapSize.xx * uv);

    // Get offset to the first node
    uint nodeOffset = LT_GetFirstVisibilityNodeOffset(screenAddress);

    // empty list
    if (nodeOffset == NODE_LIST_NULL) {
        return 1.0;
    }

    uint prevNodeOffset, nodeCount = 0;
    ListTexVisibilityNode node;
    while (nodeOffset != NODE_LIST_NULL) {
        nodeCount++;
        node = LT_GetVisibilityNode(nodeOffset);

        if (receiverDepth < node.depth)
            break;

        prevNodeOffset = nodeOffset;
        nodeOffset =  node.next;
    }

    if (nodeCount == 0) {
        return 1.0f;
    }

    if (nodeOffset == NODE_LIST_NULL) {
        return  node.trans;
    } else {
        if (nodeCount == 1) {
            return 1.0f;
        } else {
            ListTexVisibilityNode prevNode = LT_GetVisibilityNode(prevNodeOffset);
            return LT_Interp(prevNode.depth, node.depth, prevNode.trans, node.trans, receiverDepth);
        }
    }
}

float LT_VisibilityBilinearSample(in float2 textureCoords, in float receiverDepth)
{
    float2 unnormCoords = textureCoords * mFirstNodeMapSize.xx;

    const float a = frac(unnormCoords.x - 0.5f);
    const float b = frac(unnormCoords.y - 0.5f);
    const float i = floor(unnormCoords.x - 0.5f);
    const float j = floor(unnormCoords.y - 0.5f);

    float sample00 = LT_VisibilityPointSample(float2(i, j)         / mFirstNodeMapSize.xx, receiverDepth);
    float sample01 = LT_VisibilityPointSample(float2(i, j + 1)     / mFirstNodeMapSize.xx, receiverDepth);
    float sample10 = LT_VisibilityPointSample(float2(i + 1, j)     / mFirstNodeMapSize.xx, receiverDepth);
    float sample11 = LT_VisibilityPointSample(float2(i + 1, j + 1) / mFirstNodeMapSize.xx, receiverDepth);

	return (1 - a)*(1 - b)*sample00 + a*(1-b)*sample10 + (1-a)*b*sample01 + a*b*sample11;
}

#endif // H_LIST_TEXTURE
