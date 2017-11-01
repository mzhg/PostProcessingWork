#include "ASSAOCommon.frag"

layout(location = 0) out float4 Out_f4Color; 
in vec4 m_f4UVAndScreenPos;

// edge-ignorant blur & apply (for the lowest quality level 0)
void main()
{
	float2 inUV      = m_f4UVAndScreenPos.xy;
	float a = textureLod( g_FinalSSAO, float3( inUV.xy, 0 ), 0.0 ).x;  // g_LinearClampSampler
    float b = textureLod( g_FinalSSAO, float3( inUV.xy, 1 ), 0.0 ).x;
    float c = textureLod( g_FinalSSAO, float3( inUV.xy, 2 ), 0.0 ).x;
    float d = textureLod( g_FinalSSAO, float3( inUV.xy, 3 ), 0.0 ).x;
    float avg = (a+b+c+d) * 0.25;
    Out_f4Color = float4( avg.xxx, 1.0 );
}