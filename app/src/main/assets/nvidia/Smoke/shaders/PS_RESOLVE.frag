#include "Voxelizer.glsl"

in float3 m_Tex;
layout(location = 0) out float OutColor;

void main()
{
    if(texelFetch(stencilbufferTex2D, int2(m_Tex.xy), 0).g != 0)
    {
        OutColor = float4(OBSTACLE_INTERIOR);
    }
    else
    {
        OutColor = float4(OBSTACLE_EXTERIOR);
    }
}