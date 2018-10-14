#include "../../../shader_libs/PostProcessingHLSLCompatiable.glsl"

layout(binding = 0) uniform ShaderRes
{
    float2 pixelSize;
    float sssLevel;
    float correction;

    float3 projection;
    float maxdd;

    float4 weight;

    float depth;
    float width;

    float material;
};