#define SMAA_GLSL_4 1
#define SMAA_INCLUDE_VS 0
#define SMAA_INCLUDE_PS 1

#include "PostProcessingSMAA.glsl"


/*void DX11_SMAAEdgeDetectionVS(float4 position : POSITION,
                              out float4 svPosition : SV_POSITION,
                              inout float2 texcoord : TEXCOORD0,
                              out float4 offset[3] : TEXCOORD1)
{
    SMAAEdgeDetectionVS(position, svPosition, texcoord, offset);
}*/

layout(location = 0) out float2 OutColor;

in float4 offset[3];
in float2 texcoord;

layout(binding = 0) uniform sampler2D colorTex_UNORM/*    : register( t1 )*/;
layout(binding = 1) uniform sampler2D depthTex;

void main()
{
    #if SMAA_PREDICATION == 1
    OutColor = SMAAColorEdgeDetectionPS(texcoord, offset, colorTex_UNORM, depthTex);
    #else
    OutColor = SMAAColorEdgeDetectionPS(texcoord, offset, colorTex_UNORM);
    #endif
}