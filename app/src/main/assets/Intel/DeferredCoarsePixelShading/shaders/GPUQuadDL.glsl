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

#ifndef GPU_QUAD_DL_HLSL
#define GPU_QUAD_DL_HLSL

#include "GPUQuad.glsl"

// This is a "deferred lighting" implementation of GPU quad which reduces the bandwidth
// required during the accumulation passes by accumulating diffuse and (monchromatic) specular
// components only during the lighting phase.

// Monochromatic specular color implemented as suggested by Naty Hoffman at
//   http://www.realtimerendering.com/blog/deferred-lighting-approaches/

// StephanieB5: fixing runtime error by adding different code path for MSAA_SAMPLES > 1
#if MSAA_SAMPLES > 1
layout(binding = 7) uniform sampler2DMS gDeferredLightingAccumTexture /*: register(t7)*/;
#else
layout(binding = 7) uniform sampler2D gDeferredLightingAccumTexture /*: register(t7)*/;
#endif // MSAA_SAMPLES > 1

layout(location = 0) out float4 Out_Color /*: SV_Target0*/;
in flat int lightIndex /*: lightIndex*/;

float RGBToLuminance(float3 color)
{
    return dot(color, float3(0.2126f, 0.7152f, 0.0722f));
}

// Only the pixel shader changes... quad generation is the same
float4 GPUQuadDL(int sampleIndex)
{
    float4 result;

    if (mUI.visualizeLightCount) {
        result = rcp(255.0f).xxxx;
    } else {
        SurfaceData surface = ComputeSurfaceDataFromGBufferSample(int2(gl_FragCoord.xy), sampleIndex);

        // Avoid shading skybox/background pixels
        // NOTE: Compiler doesn't quite seem to move all the unrelated surface computations inside here
        // We could force it to by restructuring the code a bit, but the "all skybox" case isn't useful for
        // our benchmarking anyways.
        float3 litDiffuse = float3(0.0f, 0.0f, 0.0f);
        float3 litSpecular = float3(0.0f, 0.0f, 0.0f);
        if (surface.positionView.z < mCameraNearFar.y) {
            PointLight light = gLight[lightIndex.x];
            AccumulateBRDFDiffuseSpecular(surface, light, litDiffuse, litSpecular);
        }

        // Convert to monochromatic specular for accumulation
        float specularLum = RGBToLuminance(litSpecular);
        result = float4(litDiffuse, specularLum);
    }

    return result;
}

float4 GPUQuadDLPS(GPUQuadGSOut input) : SV_Target
{
    // Shade only sample 0
    return GPUQuadDL(input, 0);
}

float4 GPUQuadDLPerSamplePS(GPUQuadGSOut input, uint sampleIndex : SV_SampleIndex) : SV_Target
{
    return GPUQuadDL(input, sampleIndex);
}

// Resolve separate diffuse/specular components stage
float4 GPUQuadDLResolve(/*FullScreenTriangleVSOut input,*/ int sampleIndex)
{
    // Read surface data and accumulated light data
    int2 coords = int2(gl_FragCoord.xy);
    SurfaceData surface = ComputeSurfaceDataFromGBufferSample(coords, sampleIndex);
// StephanieB5: fixing runtime error by adding different code path for MSAA_SAMPLES > 1
#if MSAA_SAMPLES > 1
    float4 accumulated = texelFetch(gDeferredLightingAccumTexture, coords, sampleIndex);
#else
    float4 accumulated = texelFetch(gDeferredLightingAccumTexture, coords, 0);
#endif // MSAA_SAMPLES > 1

    float3 lit = float3(0.0f, 0.0f, 0.0f);

    if (mUI.visualizeLightCount) {
        lit = accumulated.xxx;
    } else {
        // Resolve accumulated lighting
        float diffuseLum = RGBToLuminance(accumulated.xyz);

        // Prevent divide by zero
        const float epsilon = 0.000001f;
        lit = surface.albedo.xyz * (accumulated.xyz + surface.specularAmount * accumulated.xyz * (accumulated.w / (diffuseLum + epsilon)));
    }

    return float4(lit, 1.0f);
}

float4 GPUQuadDLResolvePS(FullScreenTriangleVSOut input) : SV_Target
{
    // Shade only sample 0
    return GPUQuadDLResolve(input, 0);
}

float4 GPUQuadDLResolvePerSamplePS(FullScreenTriangleVSOut input, uint sampleIndex : SV_SampleIndex) : SV_Target
{
    float4 result;
    if (mUI.visualizePerSampleShading) {
        result = float4(1, 0, 0, 1);
    } else {
        result = GPUQuadDLResolve(input, sampleIndex);
    }
    return result;
}

#endif // GPU_QUAD_DL_HLSL
