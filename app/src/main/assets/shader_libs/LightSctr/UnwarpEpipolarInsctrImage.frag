#include "PostProcessingLightScatteringCommon.frag"

layout(location = 0) out float4 OutColor;

void main()
{
	// Get camera space z of the current screen pixel
    float fCamSpaceZ = GetCamSpaceZ( m_f4UVAndScreenPos.xy /*ProjToUV(In.m_f2PosPS)*/ );
    OutColor.rgb =  UnwarpEpipolarInsctrImage( m_f4UVAndScreenPos.zw, fCamSpaceZ );
    OutColor.a = 0.0;
}