#include "ComputeUnshadowedInscattering.frag"
#include "Tonemap.frag"

in float4 UVAndScreenPos;
in float  m_fInstID;

#if CORRECT_STATIC_SCENE == 0
//layout(origin_upper_left) in vec4 gl_FragCoord;
#endif

layout(location = 0) out float4 OutColor;
layout(location = 1) out float4 OutColor1;

void main()
{
	float2 f2UV = UVAndScreenPos.zw;
#if CORRECT_STATIC_SCENE == 1
	// remap the lower-left corner to upper_left corner.
	f2UV.y = -f2UV.y;
#endif

	if( g_bShowDepthBreaks )
	{
        OutColor = float4(0.5,1,0, 0);
        return;
	}
	
    float fCamSpaceZ = GetCamSpaceZ( UVAndScreenPos.xy );
    float3 f3BackgroundColor = float3(0);
//	float3 f3BackgroundColor = textureLod(g_tex2DColorBuffer, UVAndScreenPos.xy, 0.0).rgb;
//    [branch]
//    if( !g_PPAttribs.m_bShowLightingOnly )
    {
    	f3BackgroundColor = texelFetch(g_tex2DColorBuffer, int2(gl_FragCoord.xy), 0).rgb;
        f3BackgroundColor *= (fCamSpaceZ > g_fFarPlaneZ) ? g_f4ExtraterrestrialSunColor.rgb : float3(1);
        float3 f3ReconstructedPosWS = ProjSpaceXYZToWorldSpace(float3(f2UV, fCamSpaceZ));
        float3 f3Extinction = GetExtinction(g_f4CameraPos.xyz, f3ReconstructedPosWS);
        f3BackgroundColor *= f3Extinction.rgb;
    }
    
    float fCascade = g_fCascadeInd + m_fInstID;

#if ENABLE_LIGHT_SHAFTS
    float3 f3InsctrColor = 
        ComputeShadowedInscattering(f2UV, 
                              fCamSpaceZ,
                              fCascade,
                              false, // We cannot use min/max optimization at depth breaks
                              0 // Ignored
                              );
#else
    float3 f3InsctrColor, f3Extinction;
    ComputeUnshadowedInscattering(f2UV, fCamSpaceZ, g_uiInstrIntegralSteps, f3InsctrColor, f3Extinction);
    f3InsctrColor *= g_f4ExtraterrestrialSunColor.rgb ;
#endif

#if 1
	OutColor1 = float4(f3InsctrColor, 0);
//	return;
#endif

#if PERFORM_TONE_MAPPING
    OutColor.rgb = ToneMap(f3BackgroundColor + f3InsctrColor);
    OutColor.a = 0.0;
#else
    const float DELTA = 0.00001;
    float cmp = log( max(DELTA, dot(f3BackgroundColor + f3InsctrColor, RGB_TO_LUMINANCE)) );
    OutColor = float4(cmp);
//    OutColor.r = cmp;
//    OutColor.gba = float3(f3InsctrColor);
//    float cmp = dot(f3BackgroundColor, RGB_TO_LUMINANCE);
//    OutColor = float4(g_LightAttribs.f4ExtraterrestrialSunColor.rgb, fCamSpaceZ);
#endif
}