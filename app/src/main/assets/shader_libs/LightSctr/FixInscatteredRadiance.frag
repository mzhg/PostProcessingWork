
#include "CalculateInscattering.frag"

layout(location = 0) out float3 OutColor;

void main()
{
	if( g_bShowDepthBreaks )
	{
		OutColor = float3(0,0,1e+3);
		return;
	}

	OutColor = CalculateInscattering(m_f4UVAndScreenPos.zw,
                                 false, // Do not apply phase function
                                 false, // We cannot use min/max optimization at depth breaks
                                 0 // Ignored
                                 );
}