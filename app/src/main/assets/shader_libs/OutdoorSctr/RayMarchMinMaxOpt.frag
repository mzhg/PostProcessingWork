
#include "ComputeUnshadowedInscattering.frag"

in float4 UVAndScreenPos;
in float m_fInstID;

#if CORRECT_STATIC_SCENE == 0
// layout(origin_upper_left) in vec4 gl_FragCoord;
#endif

layout(location = 0) out float3 OutColor;

void main()
{
	int2 i2SamplePosSliceInd = int2(gl_FragCoord.xy);
    float2 f2SampleLocation = //g_tex2DCoordinates.Load( uint3(ui2SamplePosSliceInd, 0) );
    							texelFetch(g_tex2DCoordinates, i2SamplePosSliceInd, 0).xy;
    float fRayEndCamSpaceZ = //g_tex2DEpipolarCamSpaceZ.Load( uint3(ui2SamplePosSliceInd, 0) );
    							texelFetch(g_tex2DEpipolarCamSpaceZ, i2SamplePosSliceInd, 0).r;

//    [branch]
    if( any(greaterThan(abs(f2SampleLocation), float2(1+1e-3))) )
    {
    	OutColor = float3(0);
        return;
    }

    float fCascade = g_fCascadeInd + m_fInstID;

#if ENABLE_LIGHT_SHAFTS
    OutColor = ComputeShadowedInscattering(f2SampleLocation, 
                                 fRayEndCamSpaceZ,
                                 fCascade,
                                 true,  // Use min/max optimization
                                 i2SamplePosSliceInd.y);
#else
    float3 f3Inscattering, f3Extinction;
    ComputeUnshadowedInscattering(f2SampleLocation, fRayEndCamSpaceZ, g_uiInstrIntegralSteps, f3Inscattering, f3Extinction);
    f3Inscattering *= g_f4ExtraterrestrialSunColor.rgb;
    OutColor = f3Inscattering;
#endif
}