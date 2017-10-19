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
    int idx = gl_VertexID % 3;
    float2 m_f4UVAndScreenPos = float2((idx << 1) & 2, idx & 2);
    gl_Position = vec4(m_f4UVAndScreenPos.xy * 2.0 - 1.0, 0, 1);

    SMAABlendingWeightCalculationVS(m_f4UVAndScreenPos, pixcoord, offset);
    texcoord = m_f4UVAndScreenPos;
}