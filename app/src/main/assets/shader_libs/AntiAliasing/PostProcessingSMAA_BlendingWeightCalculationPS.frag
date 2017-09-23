#define SMAA_GLSL_4 1
#include "PostProcessingSMAA.glsl"

layout(location = 0) out float4 OutColor;

in  float4 offset[3];
in  float2 texcoord;
in  float2 pixcoord;

/**
 * Temporal textures
 */
layout(binding = 0) uniform sampler2D edgesTex;

/**
 * Pre-computed area and search textures
 */
layout(binding = 1) uniform sampler2D areaTex;
layout(binding = 2) uniform sampler2D searchTex;

layout(binding = 0) uniform SMAAConstants
{
    float2 c_PixelSize;
    float2 c_Dummy;

    // This is only required for temporal modes (SMAA T2x).
    float4 c_SubsampleIndices;

/**
 * This can be ignored; its purpose is to support interactive custom parameter
 * tweaking.
 */
//    float c_threshld;
//    float c_maxSearchSteps;
//    float c_maxSearchStepsDiag;
//    float c_cornerRounding;

/**
 * This is required for blending the results of previous subsample with the
 * output render target; it's used in SMAA S2x and 4x, for other modes just use
 * 1.0 (no blending).
 */
//    float c_blendFactor = 1.0;

}

void main()
{
    OutColor = SMAABlendingWeightCalculationPS(texcoord, pixcoord, offset, edgesTex, areaTex, searchTex, c_SubsampleIndices);
}