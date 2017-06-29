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

#if 0
Texture2D<float>    tDepth : register(t0);
Texture2D<float>    tCoc : register(t0);
RWTexture2D<float>  uCoC : register(u0);
RWTexture2D<float4> uDebugVisCoc : register(u0);

cbuffer CalcDOFParams
{
    uint4 ScreenParams;
    float zNear;
    float zFar;
    float focusDistance;
    float fStop;
    float focalLength;
    float maxRadius;
    float forceCoC;
};
#else
layout(binding =0) uniform sampler2D tDepth;
layout(binding =0) uniform sampler2D tCoc;

layout(r16f, binding = 0) uniform image2D uCoC;
layout(rgba8, binding = 0) uniform image2D uDebugVisCoc;

layout (binding = 0) uniform CalcDOFParams{
    uint4 ScreenParams;
    float zNear;
    float zFar;
    float focusDistance;
    float fStop;
    float focalLength;
    float maxRadius;
    float forceCoC;
};
#endif

float CocFromDepth(float sceneDepth/*, float focusDistance, float fStop, float focalLength*/)
{
    float cocScale             = (focalLength * focalLength) / fStop;  // move to constant buffer
    float distanceToLense      = sceneDepth - focalLength;
    float distanceToFocusPlane = distanceToLense - focusDistance;
    float coc                  = (distanceToLense > 0.0) ? (cocScale * (distanceToFocusPlane / distanceToLense)) : 0.0;

    coc = clamp(coc * float(ScreenParams.x) * 0.5, -maxRadius, maxRadius);

    return coc;
}

///////////////////////////////////////
// compute camera-space depth for current pixel
float CameraDepth(float depth/*, float zNear, float zFar*/)
{
    return zFar*zNear/(zFar-depth*(zFar-zNear));
}