#include "VolumeRenderer.glsl"
layout(location = 0) in vec3 In_Position;

out vec3 m_WorldViewPos;

void main()
{
    gl_Position = mul(float4(In_Position,1), WorldViewProjection);
    m_WorldViewPos = mul(float4(In_Position,1), WorldView).xyz;
}