#define SMAA_GLSL_4 1
#define SMAA_INCLUDE_VS 1
#define SMAA_INCLUDE_PS 0

#include "PostProcessingSMAA.glsl"


/*void DX11_SMAAEdgeDetectionVS(float4 position : POSITION,
                              out float4 svPosition : SV_POSITION,
                              inout float2 texcoord : TEXCOORD0,
                              out float4 offset[3] : TEXCOORD1)
{
    SMAAEdgeDetectionVS(position, svPosition, texcoord, offset);
}*/

layout(location = 0) in float4 In_Position;
layout(location = 1) in float2 In_Texcoord;

out float4 offset[3];
out float2 texcoord;
out float2 pixcoord;
/*
void DX11_SMAABlendingWeightCalculationVS(float4 position : POSITION,
                                       out float4 svPosition : SV_POSITION,
                                       inout float2 texcoord : TEXCOORD0,
                                       out float2 pixcoord : TEXCOORD1,
                                       out float4 offset[3] : TEXCOORD2)
{
    SMAABlendingWeightCalculationVS(position, svPosition, texcoord, pixcoord, offset);
}
*/

void main()
{
    SMAABlendingWeightCalculationVS(In_Texcoord, pixcoord, offset);
    texcoord = In_Texcoord;
    gl_Position = In_Position;
}