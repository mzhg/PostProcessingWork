//
// Copyright (c) 2016 Advanced Micro Devices, Inc. All rights reserved.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.
#include "../../../shader_libs/PostProcessingHLSLCompatiable.glsl"

layout(binding = 0) uniform DrawCallConstantBuffer  //: register(b0)
{
    matrix  world               /*: packoffset(c0)*/;
    matrix  worldView           /*: packoffset(c4)*/;
    uint    meshIndex           /*: packoffset(c8.x)*/;
};

layout(binding = 1) uniform FrameConstantBuffer     //: register(b1)
{
    matrix  view                /*: packoffset(c0)*/;
    matrix  projection          /*: packoffset(c4)*/;
    uint    cullFlags           /*: packoffset(c8.x)*/;
    uint    windowWidth         /*: packoffset(c8.y)*/;
    uint    windowHeight        /*: packoffset(c8.z)*/;
};

struct MeshConstants
{
    uint    vertexCount;
    uint    faceCount;
    uint    indexOffset;
    uint    vertexOffset;
};

struct Vertex
{
    float3 p;
};

struct IndirectArgs
{
    uint IndexCountPerInstance;
    uint InstanceCount;
    uint StartIndexLocation;
    int BaseVertexLocation;
    uint StartInstanceLocation;
};

struct SmallBatchDrawConstants
{
    matrix  world;
    matrix  worldView;
    uint    meshIndex;
    uint    padding [3];
};

struct SmallBatchData
{
    uint    meshIndex;
    uint    indexOffset;
    uint    faceCount;
    uint    outputIndexOffset;
    uint    drawIndex;
    uint    drawBatchStart;
};

#define CULL_INDEX_FILTER     0x1
#define CULL_BACKFACE         0x2
#define CULL_FRUSTUM          0x8
#define CULL_SMALL_PRIMITIVES  0x20

#define ENABLE_CULL_INDEX           1
#define ENABLE_CULL_BACKFACE        1
#define ENABLE_CULL_FRUSTUM         1
#define ENABLE_CULL_SMALL_PRIMITIVES 1

#if 0
RWBuffer<uint>                  filteredIndices             : register(u0);
RWBuffer<uint>                  indirectArgs                : register(u1);

ByteAddressBuffer                           vertexData      : register(t0);
Buffer<uint>                                indexData       : register(t1);
StructuredBuffer<MeshConstants>             meshConstants   : register(t2);
StructuredBuffer<SmallBatchDrawConstants>   drawConstants   : register(t3);
StructuredBuffer<SmallBatchData>            smallBatchData  : register(t4);
#else
layout(binding = 2) buffer ShaderBuffer2
{
    uint filteredIndices[];
};

layout(binding = 3) buffer ShaderBuffer3
{
    uint indirectArgs[];
};

layout(binding = 4) buffer ShaderBuffer4
{
    float vertexData[];
};

layout(binding = 5) buffer ShaderBuffer5
{
    MeshConstants meshConstants[];
};

layout(binding = 6) buffer ShaderBuffer6
{
    SmallBatchDrawConstants drawConstants[];
};

layout(binding = 7) buffer ShaderBuffer7
{
    SmallBatchData smallBatchData[];
};
#endif