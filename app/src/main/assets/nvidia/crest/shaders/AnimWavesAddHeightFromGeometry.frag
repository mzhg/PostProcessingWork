#include "OceanLODData.glsl"

in float3 worldPos;

layout(location = 0) out float4 OutColor;

void main()
{
    // Write displacement to get from sea level of ocean to the y value of this geometry
    float addHeight = worldPos.y - _OceanCenterPosWorld.y;
    OutColor = float4(0.0, addHeight, 0.0, 1.0);
}