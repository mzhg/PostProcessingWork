#include "ComputeUnshadowedInscattering.frag"

in float4 m_f4UVAndScreenPos;
in float  m_fInstID;

layout(location = 0) out float3 f3Inscattering;
#if EXTINCTION_EVAL_MODE == EXTINCTION_EVAL_MODE_EPIPOLAR
//                                  , out float3 f3Extinction   : SV_Target1
layout(location = 1) out float3 f3Extinction;
#endif

void main()
{
	// Compute unshadowed inscattering from the camera to the ray end point using few steps
    float fCamSpaceZ =  
//    				g_tex2DEpipolarCamSpaceZ.Load( uint3(In.m_f4Pos.xy, 0) );
    				texelFetch(g_tex2DEpipolarCamSpaceZ, int2(gl_FragCoord.xy), 0).r;
    float2 f2SampleLocation = 
//    				g_tex2DCoordinates.Load( uint3(In.m_f4Pos.xy, 0) );
    				texelFetch(g_tex2DCoordinates, int2(gl_FragCoord.xy), 0).xy;
#if EXTINCTION_EVAL_MODE != EXTINCTION_EVAL_MODE_EPIPOLAR
    float3 f3Extinction = float3(1);
#endif

    ComputeUnshadowedInscattering(f2SampleLocation, fCamSpaceZ, 
                                  7, // Use hard-coded constant here so that compiler can optimize the code
                                     // more efficiently
                                  f3Inscattering, f3Extinction);
    f3Inscattering *= g_f4ExtraterrestrialSunColor.rgb;
}

