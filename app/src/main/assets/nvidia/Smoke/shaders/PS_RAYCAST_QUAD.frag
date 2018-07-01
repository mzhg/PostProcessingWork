#include "VolumeRenderer.glsl"

layout(location = 0) out float4 OutColor;

in float4 m_PosInGrid;

#include "RayCast.glsl"

void main()
{
    OutColor = Raycast(raycastMode, sampleFactor);
}