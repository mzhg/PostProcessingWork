//
// Copyright (c) 2017 Advanced Micro Devices, Inc. All rights reserved.
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
//
#include "../../../shader_libs/PostProcessingHLSLCompatiable.glsl"

#ifndef OUT_FORMAT
#define OUT_FORMAT rgba8
#endif

#ifndef DOUBLE_INTEGRATE
#define DOUBLE_INTEGRATE 1
#endif

#ifndef CONVERT_TO_SRGB
#define CONVERT_TO_SRGB 1
#endif

#if 0
#define RWTexToUse RWStructuredBuffer<int4>

Texture2D<float4> tColor : register(t0);
Texture2D<float>  tCoc : register(t1);

sampler pointSampler : register(s0);

RWTexToUse intermediate : register(u0);
RWTexToUse intermediate_transposed : register(u1);

RWTexture2D<float4> resultColor : register(u2);

///////////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////////////
cbuffer Params : register(b0)
{
    int2   sourceResolution;
    float2 invSourceResolution;
    int2   bufferResolution;
    float  scale_factor;
    int    padding;
    int4   bartlettData[9];
    int4   boxBartlettData[4];
};

#else
layout(binding = 0) uniform sampler2D tColor;
layout(binding = 1) uniform sampler2D tCoc;

layout(r32i, binding = 0) uniform iimageBuffer intermediate;
layout(rgba32i, binding = 0) uniform iimageBuffer intermediate_transpose;
layout(rgba32i, binding = 1) uniform iimageBuffer intermediate_read;
layout(OUT_FORMAT, binding = 2) uniform image2D resultColor;

layout (std140, binding = 0) uniform BufferObject
{
	int2   sourceResolution;
	int2   bufferResolution;
    float2 invSourceResolution;
    float  scale_factor;
    float  padding;
    int4   bartlettData[9];
    int4   boxBartlettData[4];
};

#endif


///////////////////////////////////////////////////////////////////////////////////////////////////
// Calc offset into buffer from (x,y)
///////////////////////////////////////////////////////////////////////////////////////////////////
int GetOffset(int2 addr2d) { return ((addr2d.y * bufferResolution.x) + addr2d.x); }

///////////////////////////////////////////////////////////////////////////////////////////////////
// Calc offset into transposed buffer from (x,y)
///////////////////////////////////////////////////////////////////////////////////////////////////
int GetOffsetTransposed(int2 addr2d) { return ((addr2d.x * bufferResolution.y) + addr2d.y); }

///////////////////////////////////////////////////////////////////////////////////////////////////
// Atomic add color to buffer
///////////////////////////////////////////////////////////////////////////////////////////////////
void InterlockedAddToBuffer(/*imageBuffer _buffer,*/ int2 addr2d, int4 color)
{
    int offset = GetOffset(addr2d) * 4;
//    atomicAdd(buffer[offset].r, color.r);
//    atomicAdd(buffer[offset].g, color.g);
//    atomicAdd(buffer[offset].b, color.b);
//    atomicAdd(buffer[offset].a, color.a);
    imageAtomicAdd(intermediate, offset, color.r);
    imageAtomicAdd(intermediate, offset+1, color.g);
    imageAtomicAdd(intermediate, offset+2, color.b);
    imageAtomicAdd(intermediate, offset+3, color.a);

//    int4 orgin = imageLoad(intermediate, offset);
//    imageStore(intermediate, offset, orgin + color);
}

///////////////////////////////////////////////////////////////////////////////////////////////////
// Read result from buffer
///////////////////////////////////////////////////////////////////////////////////////////////////
int4 ReadFromBuffer(/*imageBuffer _buffer,*/ int2 addr2d)
{
    int       offset = GetOffset(addr2d);
    int4      result = imageLoad(intermediate_read, offset);
    return result;
}

#if 0
///////////////////////////////////////////////////////////////////////////////////////////////////
// write color to buffer
///////////////////////////////////////////////////////////////////////////////////////////////////
void WriteToBuffer(/*imageBuffer _buffer,*/ int2 addr2d, int4 color)
{
    const int offset = GetOffset(addr2d);
//    buffer[offset]   = color;
    imageStore(_buffer, offset, color);
}
#endif

///////////////////////////////////////////////////////////////////////////////////////////////////
// write color to transposed buffer
///////////////////////////////////////////////////////////////////////////////////////////////////
void WriteToBufferTransposed(/*imageBuffer _buffer,*/ int2 addr2d, int4 color)
{
    const int offset = GetOffsetTransposed(addr2d);
//    buffer[offset]   = color;
    imageStore(intermediate_transpose, offset, color);
}

///////////////////////////////////////////////////////////////////////////////////////////////////
// convert Circle of Confusion to blur radius in pixels
///////////////////////////////////////////////////////////////////////////////////////////////////
int CocToBlurRadius(in float fCoc, in int BlurRadius) { return clamp(abs(int(fCoc)), 0, BlurRadius); }

///////////////////////////////////////////////////////////////////////////////////////////////////
// convert color from float to int and divide by kernel weight
///////////////////////////////////////////////////////////////////////////////////////////////////
int4 normalizeBlurColor(in float4 color, in int blur_radius)
{
    float half_width = blur_radius + 1;

    // the weight for the bartlett is half_width^4
    float weight = (half_width * half_width * half_width * half_width);

    float normalization = scale_factor / weight;

    return int4(round(color * normalization));
}

///////////////////////////////////////////////////////////////////////////////////////////////////
// write the bartlett data to the buffer
///////////////////////////////////////////////////////////////////////////////////////////////////
void WriteDeltaBartlett(/*imageBuffer deltaBuffer,*/ float3 vColor, int blur_radius, int2 loc)
{
    int4 intColor = normalizeBlurColor(float4(vColor, 1.0), blur_radius);
    /*[loop]*/ for (int i = 0; i < 9; ++i)
    {
        const int2 delta       = bartlettData[i].xy * (blur_radius + 1);
        const int  delta_value = bartlettData[i].z;

        // Offset the location by location of the delta and padding
        // Need to offset by (1,1) because the kernel is not centered
        int2 bufLoc = loc.xy + delta + int(padding) + 1;

        // Write the delta
        // Use interlocked add to prevent the threads from stepping on each other
        InterlockedAddToBuffer(/*deltaBuffer,*/ bufLoc, intColor * delta_value);
    }
}

///////////////////////////////////////////////////////////////////////////////////////////////////
// write the bartlett data to the buffer for box filter
///////////////////////////////////////////////////////////////////////////////////////////////////
void WriteBoxDeltaBartlett(/*imageBuffer deltaBuffer,*/ float3 vColor, int blur_radius, int2 loc)
{
    float normalization = scale_factor / float(blur_radius * 2 + 1);
    int4  intColor      = int4(round(float4(vColor, 1.0) * normalization));
    for (int i = 0; i < 4; ++i)
    {
        const int2 delta       = boxBartlettData[i].xy * blur_radius;
        const int2 offset      = int2( greaterThan(boxBartlettData[i].xy, int2(0, 0)));
        const int  delta_value = boxBartlettData[i].z;

        // Offset the location by location of the delta and padding
        int2 bufLoc = loc.xy + delta + int(padding) + offset;

        // Write the delta
        // Use interlocked add to prevent the threads from stepping on each other's toes
        InterlockedAddToBuffer(/*deltaBuffer,*/ bufLoc, intColor * delta_value);
    }
}