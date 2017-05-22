#include "PostProcessingLightScatteringCommon.frag"

layout(location = 0) out float4 OutColor;
in vec4 m_f4UVAndScreenPos;
void main()
{
	// Get camera space z of the current screen pixel
    float fCamSpaceZ = GetCamSpaceZ( m_f4UVAndScreenPos.xy /*ProjToUV(In.m_f2PosPS)*/ );
    OutColor.rgb =  UnwarpEpipolarInsctrImage( m_f4UVAndScreenPos.zw, fCamSpaceZ );
    OutColor.a = 0.0;
}