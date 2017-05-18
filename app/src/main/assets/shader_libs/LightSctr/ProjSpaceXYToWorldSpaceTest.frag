#include "PostProcessingLightScatteringCommon.frag"

in float4 UVAndScreenPos;

layout(location = 0) out float4 OutColor;

float GetPrecomputedPtLghtSrcTexU(in float3 f3Pos, in float3 f3EyeDir, in float3 f3ClosestPointToLight)
{
    return (dot(f3Pos - f3ClosestPointToLight, f3EyeDir) + g_fMaxTracingDistance) / (2*g_fMaxTracingDistance);
}

void main()
{
	 float3 f3ReconstructedPos = ProjSpaceXYToWorldSpace(UVAndScreenPos.zw);

	 float3 f3RayStartPos = g_f4CameraPos.xyz;
	 float3 f3RayEndPos = f3ReconstructedPos;
	 float3 f3EyeVector = f3RayEndPos.xyz - f3RayStartPos;
	 float fTraceLength = length(f3EyeVector);
	 f3EyeVector /= fTraceLength;

/*	     
	#if LIGHT_TYPE == LIGHT_TYPE_DIRECTIONAL
	 // Update end position
	 fTraceLength = min(fTraceLength, g_fMaxTracingDistance);
	 f3RayEndPos = g_f4CameraPos.xyz + fTraceLength * f3EyeVector;
	#elif LIGHT_TYPE == LIGHT_TYPE_POINT || LIGHT_TYPE == LIGHT_TYPE_SPOT
*/	
	 //                       Light
	 //                        *                   -
	 //                     .' |\                  |
	 //                   .'   | \                 | fClosestDistToLight
	 //                 .'     |  \                |
	 //               .'       |   \               |
	 //          Cam *--------------*--------->    -
	 //              |<--------|     \
	 //                  \
	 //                  fStartDistFromProjection
	
	 float fDistToLight = length( g_f4LightWorldPos.xyz - g_f4CameraPos.xyz );
	 float fCosLV = dot(g_f4DirOnLight.xyz, f3EyeVector);
	 float fDistToClosestToLightPoint = fDistToLight * fCosLV;
	 float fClosestDistToLight = fDistToLight * sqrt(1.0 - fCosLV*fCosLV);
	 float fV = fClosestDistToLight / g_fMaxTracingDistance;
	 
	 float3 f3ClosestPointToLight = g_f4CameraPos.xyz + f3EyeVector * fDistToClosestToLightPoint;
	
	 float3 f3CameraInsctrIntegral = float3(0);
	 float3 f3RayTerminationInsctrIntegral = float3(0);
	#if INSCTR_INTGL_EVAL_METHOD == INSCTR_INTGL_EVAL_METHOD_MY_LUT
	
	 float fCameraU = GetPrecomputedPtLghtSrcTexU(g_f4CameraPos.xyz, f3EyeVector, f3ClosestPointToLight);
	 float fReconstrPointU = GetPrecomputedPtLghtSrcTexU(f3ReconstructedPos, f3EyeVector, f3ClosestPointToLight);
	
	 f3CameraInsctrIntegral = // g_tex2DPrecomputedPointLightInsctr.SampleLevel(samLinearClamp, float2(fCameraU, fV), 0);
			 					textureLod(g_tex2DPrecomputedPointLightInsctr, float2(fCameraU, fV), 0.0).xyz;
	 f3RayTerminationInsctrIntegral = exp(-fTraceLength*g_f4SummTotalBeta.rgb) * // g_tex2DPrecomputedPointLightInsctr.SampleLevel(samLinearClamp, float2(fReconstrPointU, fV), 0);
			 					textureLod(g_tex2DPrecomputedPointLightInsctr, float2(fReconstrPointU, fV), 0.0).xyz;
	
	#elif INSCTR_INTGL_EVAL_METHOD == INSCTR_INTGL_EVAL_METHOD_SRNN05
	
	 float3 f3Tsv = fDistToLight * g_MediaParams.f4SummTotalBeta.rgb;
	 float fSinGamma = max(sqrt( 1 - fCosLV*fCosLV ), 1e-6);
	 float3 f3A0 = g_f4SummTotalBeta.rgb * g_f4SummTotalBeta.rgb * 
	               //g_LightAttribs.f4LightColorAndIntensity.rgb * g_LightAttribs.f4LightColorAndIntensity.w *
	               exp(-f3Tsv * fCosLV) / 
	               (2.0*PI * f3Tsv * fSinGamma);
	 float3 f3A1 = f3Tsv * fSinGamma;
	 
	 f3CameraInsctrIntegral = -f3A0 * GetInsctrIntegral_SRNN05( f3A1, f3Tsv, fCosLV, fSinGamma, 0);
	 f3RayTerminationInsctrIntegral = -f3A0 * GetInsctrIntegral_SRNN05( f3A1, f3Tsv, fCosLV, fSinGamma, fTraceLength);
	
	#endif
	
	 float3 f3FullyLitInsctrIntegral = (f3CameraInsctrIntegral - f3RayTerminationInsctrIntegral) * 
	                                 g_f4LightColorAndIntensity.rgb * g_f4LightColorAndIntensity.w;
	 
	 float3 f3SpotLightAxisAndCosAngle = normalize(-g_f4LightWorldPos.xyz);
	 float fCosAngle = cos(PI / 4.0);
	 bool bIsCamInsideCone = dot( -g_f4DirOnLight.xyz, f3SpotLightAxisAndCosAngle) > fCosAngle;
	
	 // Eye rays directed at exactly the light source requires special handling
	 if( fCosLV > 1 - 1e-6 )
	 {
	#if INSCTR_INTGL_EVAL_METHOD == INSCTR_INTGL_EVAL_METHOD_ANALYTIC
	     f3FullyLitInsctrIntegral = 1e+8;
	#endif
	     float IsInLight = bIsCamInsideCone ? 
	//                     g_tex2DLightSpaceDepthMap.SampleCmpLevelZero( samComparison, g_LightAttribs.f4CameraUVAndDepthInShadowMap.xy, g_LightAttribs.f4CameraUVAndDepthInShadowMap.z ).x
	    		 		   textureLod(g_tex2DLightSpaceDepthMap, float3(g_f4CameraUVAndDepthInShadowMap.xy, g_f4CameraUVAndDepthInShadowMap.z), 0.0).x
	                         : 1.0;
	     // This term is required to eliminate bright point visible through scene geometry
	     // when the camera is outside the light cone
	     float fIsLightVisible = float(bIsCamInsideCone || (fDistToLight < fTraceLength));
	     OutColor.rgb = f3FullyLitInsctrIntegral * IsInLight * fIsLightVisible;
	     OutColor.a = 0.0;
	     return;
	 }
	
	 float fStartDistance;
//	 TruncateEyeRayToLightCone(f3EyeVector, f3RayStartPos, f3RayEndPos, fTraceLength, fStartDistance, bIsCamInsideCone);
	OutColor = float4(f3EyeVector, 0);
}