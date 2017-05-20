
#include "CalculateInscattering.frag"

layout(location = 0) out float3 OutColor;
layout(location = 1) out float4 InsctrColor;

void main()
{
	float2 f2PosPS = m_f4UVAndScreenPos.zw;
#if CORRECT_STATIC_SCENE == 1
	// remap the lower-left corner to upper_left corner.
	f2PosPS.y = -f2PosPS.y;
#endif
	if( g_bShowDepthBreaks )
	{
		OutColor = float3(0,1,0);
		return;
	}

	float3 f3BackgroundColor = GetAttenuatedBackgroundColor(f2PosPS, gl_FragCoord.xy);
#if 0
    // We can sample camera space z texture using bilinear filtering
	float fCamSpaceZ = textureLod(g_tex2DCamSpaceZ, ProjToUV(f2PosPS), 0.0).x;
    float3 f3InsctrColor = ProjSpaceXYZToWorldSpace(float3(f2PosPS, fCamSpaceZ));
#endif
    
    float3 f3InsctrColor = 
        CalculateInscattering(//In.m_f2PosPS.xy,
        					  f2PosPS,
                              true, // Apply phase function
                              false, // We cannot use min/max optimization at depth breaks
                              0 // Ignored
                              );
//	float3 f3TpsRGB = float3(1.0/2.2);
//	f3InsctrColor = pow(f3InsctrColor, f3TpsRGB);
    
    OutColor = ToneMap(f3BackgroundColor + f3InsctrColor.rgb);
    InsctrColor = float4(f3InsctrColor, 0);
#if TEST_STATIC_SCENE != 1
//	OutColor = float3(1,0,0);
#endif
}