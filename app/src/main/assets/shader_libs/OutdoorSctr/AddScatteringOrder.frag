#include "Scattering.frag"

in float4 UVAndScreenPos;
in float  m_fInstID;

layout(location = 0) out float3 OutColor;

void main()
{
	OutColor = texelFetch(g_tex3DPreviousSctrOrder, int3(gl_FragCoord.xy, g_uiDepthSlice), 0).rgb;
}