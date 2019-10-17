#include "OceanLODData.glsl"
in float depth;

layout(location = 0) out float4 OutColor;

void main()
{
    OutColor = float4(depth, 0, 0, 0);
}