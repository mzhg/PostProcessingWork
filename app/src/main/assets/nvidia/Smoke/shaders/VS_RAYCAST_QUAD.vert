#include "VolumeRenderer.glsl"
layout(location = 0) in vec3 In_Position;

out float4 m_PosInGrid;

void main()
{
    gl_Position = float4(In_Position,1);
    m_PosInGrid = mul( float4( In_Position.xy*ZNear, 0, ZNear ), InvWorldViewProjection );
}