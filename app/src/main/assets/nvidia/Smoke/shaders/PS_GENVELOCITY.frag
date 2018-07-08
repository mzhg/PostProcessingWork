#include "Voxelizer.glsl"

in float3 m_Velocity;
layout(location = 0) out float4 Velocity;
layout(location = 1) out float Obstacle;

void main()
{
    Velocity = float4(m_Velocity, 1.0);
    Obstacle = OBSTACLE_BOUNDARY;
}