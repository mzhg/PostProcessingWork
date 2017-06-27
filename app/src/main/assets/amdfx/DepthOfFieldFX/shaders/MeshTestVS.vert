#include "../../../shader_libs/PostProcessingHLSLCompatiable.glsl"

layout(location = 0) in float3 In_Position;
layout(location = 1) in float3 In_Normal;
layout(location = 2) in float2 In_TexCoord;

out float3 m_worldPos;
out float3 m_Normal;
out float2 m_TexCoord;

uniform mat4 m_WorldViewProjection;
uniform mat4 m_World;

void main()
{
    // Transform the position from object space to homogeneous projection space
    gl_Position = mul(float4(In_Position, 1.0f), m_WorldViewProjection);
    m_worldPos = mul(float4(In_Position, 1.0f), m_World).xyz;

    // Transform the normal from object space to world space
    m_Normal = normalize(mul(In_Normal, float3x3(m_World)));

    // Pass through texture coords
    m_TexCoord = In_TexCoord;
}