

#include "ComputeUnshadowedInscattering.frag"

in float4 m_f4UVAndScreenPos;
in flat int m_iInstID;

#if CORRECT_STATIC_SCENE == 0
// layout(origin_upper_left) in vec4 gl_FragCoord;
#endif

layout(location = 0) out float3 OutColor;

void main()
{
	float2 f2UV = m_f4UVAndScreenPos.zw;
#if CORRECT_STATIC_SCENE == 1
	// remap the lower-left corner to upper_left corner.
	f2UV.y = -f2UV.y;
#endif

	float2 f2SampleLocation = 
//					g_tex2DCoordinates.Load( uint3(In.m_f4Pos.xy, 0) );
					texelFetch(g_tex2DCoordinates, int2(gl_FragCoord.xy), 0.0).rg;
    float fRayEndCamSpaceZ = 
//    				g_tex2DEpipolarCamSpaceZ.Load( uint3(In.m_f4Pos.xy, 0) );
    				texelFetch(g_tex2DEpipolarCamSpaceZ, int2(gl_FragCoord.xy), 0.0).x;

//    [branch]
    if( any(abs(f2SampleLocation) > 1+1e-3) )
    {
    	OutColor = float3(0);
        return;
	}
#if ENABLE_LIGHT_SHAFTS
    float fCascade = g_MiscParams.fCascadeInd + float(m_iInstID);
    OutColor = ComputeShadowedInscattering(f2SampleLocation, 
                                 fRayEndCamSpaceZ,
                                 fCascade,
                                 false, // Do not use min/max optimization
                                 0 // Ignored
                                 );
#else
    float3 f3Inscattering, f3Extinction;
    ComputeUnshadowedInscattering(f2SampleLocation, fRayEndCamSpaceZ, g_uiInstrIntegralSteps, f3Inscattering, f3Extinction);
    f3Inscattering *= g_f4ExtraterrestrialSunColor.rgb;
    OutColor = f3Inscattering;
#endif
}