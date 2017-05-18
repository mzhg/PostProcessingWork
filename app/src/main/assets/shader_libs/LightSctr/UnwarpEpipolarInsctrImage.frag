#include "PostProcessingLightScatteringCommon.frag"

in float4 UVAndScreenPos;

layout(location = 0) out float4 OutColor;

void main()
{
	// Get camera space z of the current screen pixel
    float fCamSpaceZ = GetCamSpaceZ( UVAndScreenPos.xy /*ProjToUV(In.m_f2PosPS)*/ );
    OutColor.rgb =  UnwarpEpipolarInsctrImage( UVAndScreenPos.zw, fCamSpaceZ );
    OutColor.a = 0.0;
}