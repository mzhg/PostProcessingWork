#include "ASSAOCommon.frag"

layout(location = 0) out float4 Out_f4Color; 
in vec4 m_f4UVAndScreenPos;

void main()
{
	float2 inUV      = m_f4UVAndScreenPos.xy;
	float a = textureLod( g_FinalSSAO, float3( inUV.xy, 0 ), 0.0 ).x; // g_LinearClampSampler
    float d = textureLod( g_FinalSSAO, float3( inUV.xy, 3 ), 0.0 ).x; // g_LinearClampSampler
    float avg = (a+d) * 0.5;
    Out_f4Color = float4( avg.xxx, 1.0 );
}