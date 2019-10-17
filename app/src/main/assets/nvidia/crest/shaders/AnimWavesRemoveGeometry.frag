#include "OceanLODData.glsl"


// Vert shader use the AnimWavesAddHeightFromGeometry.vert
in float3 worldPos;

layout(location = 0) out float4 OutColor;

void main()
{
    // Write displacement to get from sea level of ocean to the y value of this geometry.

    // Write large XZ components - using min blending so this should not affect them.

    OutColor = float4(10000.0, worldPos.y - _OceanCenterPosWorld.y, 10000.0, 1.0);
}