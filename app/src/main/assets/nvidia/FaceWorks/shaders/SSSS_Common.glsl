#include "../../../shader_libs/PostProcessingHLSLCompatiable.glsl"

/*layout(binding = 0) uniform ShaderRes
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
    float near;
    float far;
};*/

layout(binding = 1) uniform SceneRes
{
    float4x4 worldViewProjection;
    float4x4 world;
    float4x4 worldInverseTranspose;
    float4x4 lightViewProjectionNDC;

    float4 lightPos;
    float4 lightDir;
    float falloffAngle;
    float spotExponent;
    float lightAttenuation;
    float lightRange;
    float4 cameraPosition;
};