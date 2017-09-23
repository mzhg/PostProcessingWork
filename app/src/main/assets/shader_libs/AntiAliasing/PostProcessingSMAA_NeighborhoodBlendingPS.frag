#define SMAA_GLSL_4 1
#include "PostProcessingSMAA.glsl"


/*void DX11_SMAAEdgeDetectionVS(float4 position : POSITION,
                              out float4 svPosition : SV_POSITION,
                              inout float2 texcoord : TEXCOORD0,
                              out float4 offset[3] : TEXCOORD1)
{
    SMAAEdgeDetectionVS(position, svPosition, texcoord, offset);
}*/

layout(location = 0) in float4 OutColor;

out float4 offset[3];
out float2 texcoord;

layout(binding = 0) uniform sampler2D colorTex;
layout(binding = 1) uniform sampler2D blendTex;

void main()
{
    OutColor = SMAANeighborhoodBlendingPS(texcoord, offset, colorTex, blendTex);
}