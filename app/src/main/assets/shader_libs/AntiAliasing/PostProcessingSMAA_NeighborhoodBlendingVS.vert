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

out float4 offset;
out float2 texcoord;

void main()
{
    int idx = gl_VertexID % 3;
    float2 In_Texcoord = float2((idx << 1) & 2, idx & 2);
    gl_Position = vec4(In_Texcoord.xy * 2.0 - 1.0, 0, 1);

    SMAANeighborhoodBlendingVS(In_Texcoord, offset);
    texcoord = In_Texcoord;
}