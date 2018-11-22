
#include "PostProcessingLightScatteringCommon.frag"

float GetPrecomputedPtLghtSrcTexU(in float3 f3Pos, in float3 f3EyeDir, in float3 f3ClosestPointToLight)
{
    return (dot(f3Pos - f3ClosestPointToLight, f3EyeDir) + g_fMaxTracingDistance) / (2*g_fMaxTracingDistance);
}

void TruncateEyeRayToLightCone(in float3 f3EyeVector, 
        inout float3 f3RayStartPos, 
        inout float3 f3RayEndPos, 
        inout float fTraceLength, 
        out float fStartDistance,
        bool bIsCamInsideCone)
{
	// Intersect view ray with the light cone
	float2 f2ConeIsecs = 
			RayConeIntersect(g_f4LightWorldPos.xyz, g_f4SpotLightAxisAndCosAngle.xyz, g_f4SpotLightAxisAndCosAngle.w,
							 g_f4CameraPos.xyz, f3EyeVector);
//	f3RayStartPos = float3(f2ConeIsecs, bIsCamInsideCone ? 1: 0);
//	fStartDistance = 1;
//	return;
	
	if( bIsCamInsideCone  )
	{
		f3RayStartPos = g_f4CameraPos.xyz;
		fStartDistance = 0;
		if( f2ConeIsecs.x > 0 )
		{
			// 
			//   '.       *     .' 
			//     '.      \  .'   
			//       '.     \'  x > 0
			//         '. .' \
			//           '    \ 
			//         '   '   \y = -FLT_MAX 
			//       '       ' 
			fTraceLength = min(f2ConeIsecs.x, fTraceLength);
		}
		else if( f2ConeIsecs.y > 0 )
		{
			// 
			//                '.             .' 
			//    x = -FLT_MAX  '.---*---->.' y > 0
			//                    '.     .'
			//                      '. .'  
			//                        '
			fTraceLength = min(f2ConeIsecs.y, fTraceLength);
		}
		f3RayEndPos = g_f4CameraPos.xyz + fTraceLength * f3EyeVector;
	}
	else if( /*all(f2ConeIsecs > 0)*/ f2ConeIsecs.x > 0.0 && f2ConeIsecs.y > 0.0)
	{
		// 
		//          '.             .' 
		//    *-------'.-------->.' y > 0
		//          x>0 '.     .'
		//                '. .'  
		//                  '
		fTraceLength = min(f2ConeIsecs.y,fTraceLength);
		f3RayEndPos   = g_f4CameraPos.xyz + fTraceLength * f3EyeVector;
		f3RayStartPos = g_f4CameraPos.xyz + f2ConeIsecs.x * f3EyeVector;
		fStartDistance = f2ConeIsecs.x;
		fTraceLength -= f2ConeIsecs.x;
	}
	else if( f2ConeIsecs.y > 0 )
	{
		// 
		//   '.       \     .'                '.         |   .' 
		//     '.      \  .'                    '.       | .'   
		//       '.     \'  y > 0                 '.     |'  y > 0
		//         '. .' \                          '. .'| 
		//           '    *                           '  |   
		//         '   '   \x = -FLT_MAX            '   '|   x = -FLT_MAX 
		//       '       '                        '      |' 
		//                                               *
		//
		f3RayEndPos   = g_f4CameraPos.xyz + fTraceLength * f3EyeVector;
		f3RayStartPos = g_f4CameraPos.xyz + f2ConeIsecs.y * f3EyeVector;
		fStartDistance = f2ConeIsecs.y;
		fTraceLength -= f2ConeIsecs.y;
	}
	else
	{
		fTraceLength = 0;
		fStartDistance = 0;
		f3RayStartPos = g_f4CameraPos.xyz;
		f3RayEndPos   = g_f4CameraPos.xyz;
	}
	fTraceLength = max(fTraceLength,0);
}

#if INSCTR_INTGL_EVAL_METHOD == INSCTR_INTGL_EVAL_METHOD_SRNN05
float3 GetInsctrIntegral_SRNN05( in float3 f3A1, in float3 f3Tsv, in float fCosGamma, in float fSinGamma, in float fDistFromCamera)
{
    // f3A1 depends only on the location of the camera and the light source
    // f3Tsv = fDistToLight * g_MediaParams.f4SummTotalBeta.rgb
    float3 f3Tvp = fDistFromCamera * g_f4SummTotalBeta.rgb;
    float3 f3Ksi = PI/4.f + 0.5f * atan( (f3Tvp - f3Tsv * fCosGamma) / (f3Tsv * fSinGamma) );
    float2 f2SRNN05LUTParamLimits = GetSRNN05LUTParamLimits();
    // float fGamma = acos(fCosGamma);
    // F(A1, Gamma/2) defines constant offset and thus is not required
    return float3(
//              g_tex2DPrecomputedPointLightInsctr.SampleLevel(samLinearClamp, float2(f3A1.x, f3Ksi.x)/f2SRNN05LUTParamLimits, 0).x ,
    			textureLod(g_tex2DPrecomputedPointLightInsctr, float2(f3A1.x, f3Ksi.x)/f2SRNN05LUTParamLimits, 0.0).x, 
//              g_tex2DPrecomputedPointLightInsctr.SampleLevel(samLinearClamp, float2(f3A1.y, f3Ksi.y)/f2SRNN05LUTParamLimits, 0).x,
    			textureLod(g_tex2DPrecomputedPointLightInsctr, float2(f3A1.y, f3Ksi.y)/f2SRNN05LUTParamLimits, 0.0).x,
//              g_tex2DPrecomputedPointLightInsctr.SampleLevel(samLinearClamp, float2(f3A1.z, f3Ksi.z)/f2SRNN05LUTParamLimits, 0).x
    			textureLod(g_tex2DPrecomputedPointLightInsctr, float2(f3A1.z, f3Ksi.z)/f2SRNN05LUTParamLimits, 0.0).x
                );
}
#endif

float3 EvaluatePhaseFunction(float fCosTheta);

//This function calculates inscattered light integral over the ray from the camera to 
//the specified world space position using ray marching
float3 CalculateInscattering( in float2 f2RayMarchingSampleLocation,
                           in /*uniform const*/ bool bApplyPhaseFunction /*= false*/,
                           in /*uniform const*/ bool bUse1DMinMaxMipMap /*= false*/,
                           int uiEpipolarSliceInd /*= 0*/ )
{
 float3 f3ReconstructedPos = ProjSpaceXYToWorldSpace(f2RayMarchingSampleLocation);
 
// return f3ReconstructedPos;

 float3 f3RayStartPos = g_f4CameraPos.xyz;
 float3 f3RayEndPos = f3ReconstructedPos;
 float3 f3EyeVector = f3RayEndPos.xyz - f3RayStartPos;
 float fTraceLength = length(f3EyeVector);
 f3EyeVector /= fTraceLength;
 
// return f3EyeVector;

//	return float3(LIGHT_TYPE, STAINED_GLASS, 0);
     
#if LIGHT_TYPE == LIGHT_TYPE_DIRECTIONAL
 // Update end position
 fTraceLength = min(fTraceLength, g_fMaxTracingDistance);
 f3RayEndPos = g_f4CameraPos.xyz + fTraceLength * f3EyeVector;
#elif LIGHT_TYPE == LIGHT_TYPE_POINT || LIGHT_TYPE == LIGHT_TYPE_SPOT

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
 
// return f3ClosestPointToLight;

//return float3(INSCTR_INTGL_EVAL_METHOD, STAINED_GLASS, 0);
 float3 f3CameraInsctrIntegral = float3(0);
 float3 f3RayTerminationInsctrIntegral = float3(0);
#if INSCTR_INTGL_EVAL_METHOD == INSCTR_INTGL_EVAL_METHOD_MY_LUT

 float fCameraU = GetPrecomputedPtLghtSrcTexU(g_f4CameraPos.xyz, f3EyeVector, f3ClosestPointToLight);
 float fReconstrPointU = GetPrecomputedPtLghtSrcTexU(f3ReconstructedPos, f3EyeVector, f3ClosestPointToLight);

 f3CameraInsctrIntegral = // g_tex2DPrecomputedPointLightInsctr.SampleLevel(samLinearClamp, float2(fCameraU, fV), 0);
		 					textureLod(g_tex2DPrecomputedPointLightInsctr, float2(fCameraU, fV), 0.0).xyz;
 f3RayTerminationInsctrIntegral = exp(-fTraceLength*g_f4SummTotalBeta.rgb) * // g_tex2DPrecomputedPointLightInsctr.SampleLevel(samLinearClamp, float2(fReconstrPointU, fV), 0);
		 					textureLod(g_tex2DPrecomputedPointLightInsctr, float2(fReconstrPointU, fV), 0.0).xyz;
// return float3(fCameraU, fReconstrPointU, 0);
// return f3CameraInsctrIntegral;
// return f3RayTerminationInsctrIntegral;
#elif INSCTR_INTGL_EVAL_METHOD == INSCTR_INTGL_EVAL_METHOD_SRNN05

 float3 f3Tsv = fDistToLight * g_f4SummTotalBeta.rgb;
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
 
// return f3FullyLitInsctrIntegral;
 bool bIsCamInsideCone = dot( -g_f4DirOnLight.xyz, g_f4SpotLightAxisAndCosAngle.xyz) > g_f4SpotLightAxisAndCosAngle.w;

 // Eye rays directed at exactly the light source requires special handling
 if( fCosLV > 1 - 1e-6 )
 {
#if INSCTR_INTGL_EVAL_METHOD == INSCTR_INTGL_EVAL_METHOD_ANALYTIC
     f3FullyLitInsctrIntegral = float3(1e+8);
#endif
     float IsInLight = bIsCamInsideCone ? 
//                     g_tex2DLightSpaceDepthMap.SampleCmpLevelZero( samComparison, g_LightAttribs.f4CameraUVAndDepthInShadowMap.xy, g_LightAttribs.f4CameraUVAndDepthInShadowMap.z ).x
    		 		   textureLod(g_tex2DLightSpaceDepthMap, float3(g_f4CameraUVAndDepthInShadowMap.xy, g_f4CameraUVAndDepthInShadowMap.z), 0.0).x
                         : 1.0;
     // This term is required to eliminate bright point visible through scene geometry
     // when the camera is outside the light cone
     float fIsLightVisible = float(bIsCamInsideCone || (fDistToLight < fTraceLength));
     return f3FullyLitInsctrIntegral * IsInLight * fIsLightVisible;
 }

 float fStartDistance;
 TruncateEyeRayToLightCone(f3EyeVector, f3RayStartPos, f3RayEndPos, fTraceLength, fStartDistance, bIsCamInsideCone);

// return f3RayStartPos;
//   return f3RayEndPos;
//   return float3(fStartDistance);
#endif
 
 // If tracing distance is very short, we can fall into an inifinte loop due to
 // 0 length step and crash the driver. Return from function in this case
 if( fTraceLength < g_fMaxTracingDistance * 0.0001)
 {
#   if LIGHT_TYPE == LIGHT_TYPE_POINT
     return f3FullyLitInsctrIntegral;
#   else
     return float3(0,0,0);
#   endif
 }

 // We trace the ray not in the world space, but in the light projection space

#if LIGHT_TYPE == LIGHT_TYPE_DIRECTIONAL
 // Get start and end positions of the ray in the light projection space
 float4 f4StartUVAndDepthInLightSpace = float4(g_f4CameraUVAndDepthInShadowMap.xyz,1);
#elif LIGHT_TYPE == LIGHT_TYPE_POINT || LIGHT_TYPE == LIGHT_TYPE_SPOT
 float4 f4StartUVAndDepthInLightSpace = WorldSpaceToShadowMapUV(f3RayStartPos);
// float3 f3StartUVAndDepth = f4StartUVAndDepthInLightSpace.xyz * f4StartUVAndDepthInLightSpace.w;
// return f3StartUVAndDepth;
#endif

 //f4StartUVAndDepthInLightSpace.z -= SHADOW_MAP_DEPTH_BIAS;
 // Compute shadow map UV coordiantes of the ray end point and its depth in the light space
 float4 f4EndUVAndDepthInLightSpace = WorldSpaceToShadowMapUV(f3RayEndPos);
// float3 f3EndUVAndDepth = f4StartUVAndDepthInLightSpace.xyz;
// return f3EndUVAndDepth;
 //f4EndUVAndDepthInLightSpace.z -= SHADOW_MAP_DEPTH_BIAS;

 // Calculate normalized trace direction in the light projection space and its length
 float3 f3ShadowMapTraceDir = f4EndUVAndDepthInLightSpace.xyz - f4StartUVAndDepthInLightSpace.xyz;
 // If the ray is directed exactly at the light source, trace length will be zero
 // Clamp to a very small positive value to avoid division by zero
 // Also assure that trace len is not longer than maximum meaningful length
 float fTraceLenInShadowMapUVSpace = clamp( length( f3ShadowMapTraceDir.xy ), 1e-6, sqrt(2.f) );
 f3ShadowMapTraceDir /= fTraceLenInShadowMapUVSpace;
 
 float fShadowMapUVStepLen = 0;
 float2 f2SliceOriginUV = float2(0);
 if( bUse1DMinMaxMipMap )
 {
     // Get UV direction for this slice
     float4 f4SliceUVDirAndOrigin = // g_tex2DSliceUVDirAndOrigin.Load( uint3(uiEpipolarSliceInd,0,0) );
    		 						texelFetch(g_tex2DSliceUVDirAndOrigin, int2(uiEpipolarSliceInd,0), 0);
     if( all(f4SliceUVDirAndOrigin == g_f4IncorrectSliceUVDirAndStart) )
     {
#   if LIGHT_TYPE == LIGHT_TYPE_POINT
        return f3FullyLitInsctrIntegral;
#   else
         return float3(0,0,0);
#   endif
     }

     // Scale with the shadow map texel size
     fShadowMapUVStepLen = length(f4SliceUVDirAndOrigin.xy * g_f2ShadowMapTexelSize);
     f2SliceOriginUV = f4SliceUVDirAndOrigin.zw;
 }
 else
 {
     //Calculate length of the trace step in light projection space
     fShadowMapUVStepLen = g_f2ShadowMapTexelSize.x / max( abs(f3ShadowMapTraceDir.x), abs(f3ShadowMapTraceDir.y) );
     // Take into account maximum number of steps specified by the g_MiscParams.fMaxStepsAlongRay
     fShadowMapUVStepLen = max(fTraceLenInShadowMapUVSpace/g_fMaxStepsAlongRay, fShadowMapUVStepLen);
 }
 
//	return f3ShadowMapTraceDir;
//	return float3(fShadowMapUVStepLen,0,0);
 
 // Calcualte ray step length in world space
 float fRayStepLengthWS = fTraceLength * (fShadowMapUVStepLen / fTraceLenInShadowMapUVSpace);
 // Assure that step length is not 0 so that we will not fall into an infinite loop and
 // will not crash the driver
 //fRayStepLengthWS = max(fRayStepLengthWS, g_PPAttribs.m_fMaxTracingDistance * 1e-5);

 // Scale trace direction in light projection space to calculate the final step
 float3 f3ShadowMapUVAndDepthStep = f3ShadowMapTraceDir * fShadowMapUVStepLen;

 float3 f3InScatteringIntegral = float3(0);
 float3 f3PrevInsctrIntegralValue = float3(1); // exp( -0 * g_MediaParams.f4SummTotalBeta.rgb );
 // March the ray
 float fTotalMarchedDistance = 0;
 float fTotalMarchedDistInUVSpace = 0;
 float3 f3CurrShadowMapUVAndDepthInLightSpace = f4StartUVAndDepthInLightSpace.xyz;

 // The following variables are used only if 1D min map optimization is enabled
 int uiMinLevel = 0;//max( log2( (fTraceLenInShadowMapUVSpace/fShadowMapUVStepLen) / g_MiscParams.fMaxStepsAlongRay), 0 );
 int uiCurrSamplePos = 0;

 // For spot light, the slice start UV is either location of camera in light proj space
 // or intersection of the slice with the cone rib. No adjustment is required in either case
//#if LIGHT_TYPE == LIGHT_TYPE_SPOT
// uiCurrSamplePos = length(f4StartUVAndDepthInLightSpace.xy - f2SliceOriginUV.xy) / fShadowMapUVStepLen;
//#endif

#if LIGHT_TYPE == LIGHT_TYPE_POINT || LIGHT_TYPE == LIGHT_TYPE_SPOT

#   if INSCTR_INTGL_EVAL_METHOD == INSCTR_INTGL_EVAL_METHOD_MY_LUT
     float fInsctrTexStartU = GetPrecomputedPtLghtSrcTexU(f3RayStartPos, f3EyeVector, f3ClosestPointToLight);
     float fInsctrTexEndU = GetPrecomputedPtLghtSrcTexU(f3RayEndPos, f3EyeVector, f3ClosestPointToLight);
     f3PrevInsctrIntegralValue = exp(-fStartDistance*g_f4SummTotalBeta.rgb) * 
    	 //g_tex2DPrecomputedPointLightInsctr.SampleLevel(samLinearClamp, float2(fInsctrTexStartU, fV), 0);
    	   textureLod(g_tex2DPrecomputedPointLightInsctr, float2(fInsctrTexStartU, fV), 0.0).rgb;
#   elif INSCTR_INTGL_EVAL_METHOD == INSCTR_INTGL_EVAL_METHOD_SRNN05
     f3PrevInsctrIntegralValue = -f3A0 * GetInsctrIntegral_SRNN05( f3A1, f3Tsv, fCosLV, fSinGamma, fStartDistance);
#   endif

#   if LIGHT_TYPE == LIGHT_TYPE_POINT && (INSCTR_INTGL_EVAL_METHOD == INSCTR_INTGL_EVAL_METHOD_MY_LUT || INSCTR_INTGL_EVAL_METHOD == INSCTR_INTGL_EVAL_METHOD_SRNN05)
     // Add inscattering contribution outside the light cone
     f3InScatteringIntegral = 
                 ( f3CameraInsctrIntegral - 
                   f3PrevInsctrIntegralValue +
#       if INSCTR_INTGL_EVAL_METHOD == INSCTR_INTGL_EVAL_METHOD_MY_LUT
                   exp(-(fStartDistance+fTraceLength)*g_f4SummTotalBeta.rgb) * 
 //                  g_tex2DPrecomputedPointLightInsctr.SampleLevel(samLinearClamp, float2(fInsctrTexEndU, fV), 0)
                   	 textureLod(g_tex2DPrecomputedPointLightInsctr, float2(fInsctrTexEndU, fV), 0.0)
#       else
                   - f3A0 * GetInsctrIntegral_SRNN05( f3A1, f3Tsv, fCosLV, fSinGamma, fStartDistance+fTraceLength) 
#       endif
                   - f3RayTerminationInsctrIntegral ) * g_f4LightColorAndIntensity.rgb;
#   endif
#endif

//	return f3PrevInsctrIntegralValue;
//	return f3InScatteringIntegral;
 int uiCurrTreeLevel = 0;
 // Note that min/max shadow map does not contain finest resolution level
 // The first level it contains corresponds to step == 2
 int iLevelDataOffset = -int(g_uiMinMaxShadowMapResolution);
 float fStep = 1.f;
 float fMaxShadowMapStep = float(g_uiMaxShadowMapStep);

#if (LIGHT_TYPE == LIGHT_TYPE_POINT || LIGHT_TYPE == LIGHT_TYPE_SPOT) && INSCTR_INTGL_EVAL_METHOD == INSCTR_INTGL_EVAL_METHOD_ANALYTIC
 float fPrevDistFromCamera = fStartDistance;
#endif

//	return float3(fTraceLenInShadowMapUVSpace, 0, 0);
//	return f3ShadowMapUVAndDepthStep;
// [loop]
 while( fTotalMarchedDistInUVSpace < fTraceLenInShadowMapUVSpace )
 {
     // Clamp depth to a very small positive value to not let the shadow rays get clipped at the
     // shadow map far clipping plane
     float fCurrDepthInLightSpace = max(f3CurrShadowMapUVAndDepthInLightSpace.z, 1e-7);
     float IsInLight = 0.0;

#if ACCEL_STRUCT > ACCEL_STRUCT_NONE
     if( bUse1DMinMaxMipMap )
     {

         // If the step is smaller than the maximum allowed and the sample
         // is located at the appropriate position, advance to the next coarser level
         if( fStep < fMaxShadowMapStep && ((uiCurrSamplePos & ((2<<uiCurrTreeLevel)-1)) == 0) )
         {
             iLevelDataOffset += g_uiMinMaxShadowMapResolution >> uiCurrTreeLevel;
             uiCurrTreeLevel++;
             fStep *= 2.f;
         }

         while(uiCurrTreeLevel > uiMinLevel)
         {
             // Compute light space depths at the ends of the current ray section

             // What we need here is actually depth which is divided by the camera view space z
             // Thus depth can be correctly interpolated in screen space:
             // http://www.comp.nus.edu.sg/~lowkl/publications/lowk_persp_interp_techrep.pdf
             // A subtle moment here is that we need to be sure that we can skip fStep samples 
             // starting from 0 up to fStep-1. We do not need to do any checks against the sample fStep away:
             //
             //     --------------->
             //
             //          *
             //               *         *
             //     *              *     
             //     0    1    2    3
             //
             //     |------------------>|
             //           fStep = 4
             float fNextLightSpaceDepth = f3CurrShadowMapUVAndDepthInLightSpace.z + f3ShadowMapUVAndDepthStep.z * (fStep-1);
             float2 f2StartEndDepthOnRaySection = float2(f3CurrShadowMapUVAndDepthInLightSpace.z, fNextLightSpaceDepth);
             f2StartEndDepthOnRaySection = max(f2StartEndDepthOnRaySection, 1e-7);

             // Load 1D min/max depths
             float4 fnCurrMinMaxDepth = //g_tex2DMinMaxLightSpaceDepth.Load( uint3( (uiCurrSamplePos>>uiCurrTreeLevel) + iLevelDataOffset, uiEpipolarSliceInd, 0) );
            		 		texelFetch(g_tex2DMinMaxLightSpaceDepth, int2((uiCurrSamplePos>>uiCurrTreeLevel) + iLevelDataOffset, uiEpipolarSliceInd), 0);
             
#   if ACCEL_STRUCT == ACCEL_STRUCT_BV_TREE
             float4 f4CurrMinMaxDepth = fnCurrMinMaxDepth;
#   elif ACCEL_STRUCT == ACCEL_STRUCT_MIN_MAX_TREE
             float4 f4CurrMinMaxDepth = fnCurrMinMaxDepth.xyxy;
#   endif

#if TEST_STATIC_SCENE == 1
#   if !STAINED_GLASS
             IsInLight = float(all( greaterThanEqual(f2StartEndDepthOnRaySection, f4CurrMinMaxDepth.yw) ));
#   endif
             bool bIsInShadow = all( lessThan(f2StartEndDepthOnRaySection, f4CurrMinMaxDepth.xz) );
#else
#   if !STAINED_GLASS
             IsInLight = float(all( lessThanEqual(f2StartEndDepthOnRaySection, f4CurrMinMaxDepth.xz) ));
#   endif
             bool bIsInShadow = all( greaterThan(f2StartEndDepthOnRaySection, f4CurrMinMaxDepth.yw) );
#endif

             if( bool(IsInLight) || bIsInShadow )
                 // If the ray section is fully lit or shadow, we can break the loop
                 break;
             // If the ray section is neither fully lit, nor shadowed, we have to go to the finer level
             uiCurrTreeLevel--;
             iLevelDataOffset -= g_uiMinMaxShadowMapResolution >> uiCurrTreeLevel;
             fStep /= 2.f;
         };

         // If we are at the finest level, sample the shadow map with PCF
//         [branch]
         if( uiCurrTreeLevel <= uiMinLevel )
         {
//             IsInLight = g_tex2DLightSpaceDepthMap.SampleCmpLevelZero( samComparison, f3CurrShadowMapUVAndDepthInLightSpace.xy, fCurrDepthInLightSpace  ).x;
        	 IsInLight = textureLod(g_tex2DLightSpaceDepthMap, float3(f3CurrShadowMapUVAndDepthInLightSpace.xy, fCurrDepthInLightSpace), 0.0).x;
         }
     }
     else
#endif
     {
//         IsInLight = g_tex2DLightSpaceDepthMap.SampleCmpLevelZero( samComparison, f3CurrShadowMapUVAndDepthInLightSpace.xy, fCurrDepthInLightSpace ).x;
    	 IsInLight = textureLod(g_tex2DLightSpaceDepthMap, float3(f3CurrShadowMapUVAndDepthInLightSpace.xy, fCurrDepthInLightSpace), 0.0).x;
//    	 IsInLight = max(1.0, IsInLight);
     }

     float3 LightColorInCurrPoint;
     LightColorInCurrPoint = g_f4LightColorAndIntensity.rgb;

#if STAINED_GLASS
     float4 SGWColor = //g_tex2DStainedGlassColorDepth.SampleLevel( samLinearClamp, f3CurrShadowMapUVAndDepthInLightSpace.xy, 0).rgba;
     		textureLod(g_tex2DStainedGlassColorDepth, f3CurrShadowMapUVAndDepthInLightSpace.xy, 0.0);
     LightColorInCurrPoint.rgb *= ((SGWColor.a < fCurrDepthInLightSpace) ? float3(1,1,1) : SGWColor.rgb*3);
#endif

     f3CurrShadowMapUVAndDepthInLightSpace += f3ShadowMapUVAndDepthStep * fStep;
     fTotalMarchedDistInUVSpace += fShadowMapUVStepLen * fStep;
     uiCurrSamplePos += 1 << uiCurrTreeLevel; // int -> float conversions are slow

#if LIGHT_TYPE == LIGHT_TYPE_DIRECTIONAL
     fTotalMarchedDistance += fRayStepLengthWS * fStep;
     float fIntegrationDist = min(fTotalMarchedDistance, fTraceLength);
     // Calculate inscattering integral from the camera to the current point analytically:
     float3 f3CurrInscatteringIntegralValue = exp( -fIntegrationDist * g_f4SummTotalBeta.rgb );
#elif LIGHT_TYPE == LIGHT_TYPE_SPOT || LIGHT_TYPE == LIGHT_TYPE_POINT
     // http://www.comp.nus.edu.sg/~lowkl/publications/lowk_persp_interp_techrep.pdf
     // An attribute A itself cannot be correctly interpolated in screen space
     // However, A/z where z is the camera view space coordinate, does interpolate correctly
     // 1/z also interpolates correctly, thus to properly interpolate A it is necessary to
     // do the following: lerp( A/z ) / lerp ( 1/z )
     // Note that since eye ray directed at exactly the light source is handled separately,
     // camera space z can never become zero
     float fRelativePos = saturate(fTotalMarchedDistInUVSpace / fTraceLenInShadowMapUVSpace);
     float fCurrW = lerp(f4StartUVAndDepthInLightSpace.w, f4EndUVAndDepthInLightSpace.w, fRelativePos);
     float fDistFromCamera = lerp(fStartDistance * f4StartUVAndDepthInLightSpace.w, (fStartDistance+fTraceLength) * f4EndUVAndDepthInLightSpace.w, fRelativePos) / fCurrW;
     float3 f3CurrInscatteringIntegralValue = float3(0);
#   if INSCTR_INTGL_EVAL_METHOD == INSCTR_INTGL_EVAL_METHOD_MY_LUT
         float fCurrU = lerp(fInsctrTexStartU * f4StartUVAndDepthInLightSpace.w, fInsctrTexEndU * f4EndUVAndDepthInLightSpace.w, fRelativePos) / fCurrW;
         f3CurrInscatteringIntegralValue = exp(-fDistFromCamera*g_f4SummTotalBeta.rgb) * 
//        		 g_tex2DPrecomputedPointLightInsctr.SampleLevel(samLinearClamp, float2(fCurrU, fV), 0);
        		 textureLod(g_tex2DPrecomputedPointLightInsctr, float2(fCurrU, fV), 0).rgb;
#   elif INSCTR_INTGL_EVAL_METHOD == INSCTR_INTGL_EVAL_METHOD_SRNN05
         f3CurrInscatteringIntegralValue = -f3A0 * GetInsctrIntegral_SRNN05( f3A1, f3Tsv, fCosLV, fSinGamma, fDistFromCamera);
#   elif INSCTR_INTGL_EVAL_METHOD == INSCTR_INTGL_EVAL_METHOD_ANALYTIC
         float3 f3DirFromLight = g_f4CameraPos.xyz  + fDistFromCamera * f3EyeVector - g_f4LightWorldPos.xyz;
         float fDistFromLightSqr = max(dot(f3DirFromLight, f3DirFromLight), 1e-10);
         float fDistFromLight = sqrt(fDistFromLightSqr);
         f3DirFromLight /= fDistFromLight;
         float fCosTheta = dot(-f3EyeVector, f3DirFromLight);
         float3 f3Extinction = exp(-(fDistFromCamera+fDistFromLight)*g_f4SummTotalBeta.rgb);
         f3CurrInscatteringIntegralValue = float3(0);
         f3PrevInsctrIntegralValue = f3Extinction * EvaluatePhaseFunction(fCosTheta) * (fDistFromCamera - fPrevDistFromCamera) / fDistFromLightSqr;
         fPrevDistFromCamera = fDistFromCamera;
#   endif

#endif

     float3 dScatteredLight;
     // dScatteredLight contains correct scattering light value with respect to extinction
     dScatteredLight.rgb = (f3PrevInsctrIntegralValue.rgb - f3CurrInscatteringIntegralValue.rgb) * IsInLight;
     dScatteredLight.rgb *= LightColorInCurrPoint;
     f3InScatteringIntegral.rgb += dScatteredLight.rgb;

     f3PrevInsctrIntegralValue.rgb = f3CurrInscatteringIntegralValue.rgb;
 }

//	return f3InScatteringIntegral;
#if LIGHT_TYPE == LIGHT_TYPE_DIRECTIONAL
 f3InScatteringIntegral = f3InScatteringIntegral / g_f4SummTotalBeta.rgb;
#else
 f3InScatteringIntegral *= g_f4LightColorAndIntensity.w;
#endif

#if LIGHT_TYPE == LIGHT_TYPE_DIRECTIONAL
 if( bApplyPhaseFunction )
     return ApplyPhaseFunction(f3InScatteringIntegral, dot(f3EyeVector, g_f4DirOnLight.xyz));
 else
#endif
     return f3InScatteringIntegral;
}
