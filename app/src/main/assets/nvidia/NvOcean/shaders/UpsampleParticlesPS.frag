#include "skybox.glsl"

layout(location = 0) out float4 OutColor;

layout(binding = 2) uniform sampler2D g_texColor;
//Texture2DMS<float> g_texDepthMS;
layout(binding = 1) uniform sampler2D g_texDepth;

layout(binding = 0) uniform sampler2DMS g_texDepthMS;

in vec4 m_f4UVAndScreenPos;

void main()
{
    float4 pixelColor = float4(0);
    uint sampleCount = 0;

    float4 linearColor = texture(g_texColor, m_f4UVAndScreenPos.xy);   // DefaultSampler

    for (uint s=0; s<4; ++s)
    {
        float sampleDepth = texelFetch(g_texDepthMS, int2(gl_FragCoord.xy), s);
        float4 sampleClipPos = float4(0, 0, sampleDepth, 1.0);
        float4 sampleViewSpace = mul(sampleClipPos, g_matProjInv);
        sampleViewSpace.z /= sampleViewSpace.w;

        float4 combinedColor = float4(0);
        float combinedWeight = 0;

        int radius = 1;
        for (int i=-radius; i<=radius; ++i)
        {
            for (int j=-radius; j<=radius; ++j)
            {
                int2 iCoarseCoord = int2(In.Position.xy) / 2 + int2(i, j);
                float4 color = texelFetch(g_texColor,iCoarseCoord,0);
                float depth = texelFetch(g_texDepth,iCoarseCoord,0).x;

                float4 clipPos = float4(0, 0, depth, 1.0);
                float4 viewSpace = mul(clipPos, g_matProjInv);
                viewSpace.z /= viewSpace.w;

                float depthDifference = abs(sampleViewSpace.z - viewSpace.z);
                float weight = 1.0f / (abs(sampleViewSpace.z - viewSpace.z) + 0.001f);

                combinedColor += color * weight;
                combinedWeight += weight;
            }
        }

        if(combinedWeight > 0.00001)
        {
            pixelColor += combinedColor / combinedWeight;
            ++sampleCount;
        }
    }

    if (sampleCount == 0) discard;

    float4 finalColor = pixelColor / float4(sampleCount);
    finalColor = lerp(linearColor, finalColor, finalColor.a);

    OutColor = finalColor;
}