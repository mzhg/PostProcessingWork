
#include "InsctrLUTCoords2WorldParams.frag"
#include "LookUpPrecomputedScattering.frag"

// This function for analytical evaluation of particle density integral is 
// provided by Eric Bruneton
// http://www-evasion.inrialpes.fr/Membres/Eric.Bruneton/
//
// optical depth for ray (r,mu) of length d, using analytic formula
// (mu=cos(view zenith angle)), intersections with ground ignored
float2 GetDensityIntegralAnalytic(float r, float mu, float d) 
{
    float2 f2A = sqrt( (0.5/g_f2ParticleScaleHeight.xy) * r );
    float4 f4A01 = f2A.xxyy * float2(mu, mu + d / r).xyxy;
    float4 f4A01s = sign(f4A01);
    float4 f4A01sq = f4A01*f4A01;
    
    float2 f2X;
    f2X.x = f4A01s.y > f4A01s.x ? exp(f4A01sq.x) : 0.0;
    f2X.y = f4A01s.w > f4A01s.z ? exp(f4A01sq.z) : 0.0;
    
    float4 f4Y = f4A01s / (2.3193*abs(f4A01) + sqrt(1.52*f4A01sq + 4.0)) * float3(1.0, exp(-d/g_f2ParticleScaleHeight.xy*(d/(2.0*r)+mu))).xyxz;

    return sqrt((6.2831*g_f2ParticleScaleHeight)*r) * exp((g_fEarthRadius-r)/g_f2ParticleScaleHeight.xy) * (f2X + float2( dot(f4Y.xy, float2(1.0, -1.0)), dot(f4Y.zw, float2(1.0, -1.0)) ));
}

float3 GetExtinctionUnverified(in float3 f3StartPos, in float3 f3EndPos, float3 f3EyeDir, float3 f3EarthCentre)
{
#if 0
    float2 f2ParticleDensity = IntegrateParticleDensity(f3StartPos, f3EndPos, f3EarthCentre, 20);
#else
    float r = length(f3StartPos-f3EarthCentre);
    float fCosZenithAngle = dot(f3StartPos-f3EarthCentre, f3EyeDir) / r;
    float2 f2ParticleDensity = GetDensityIntegralAnalytic(r, fCosZenithAngle, length(f3StartPos - f3EndPos));
#endif

    // Get optical depth
    float3 f3TotalRlghOpticalDepth = g_f4RayleighExtinctionCoeff.rgb * f2ParticleDensity.x;
    float3 f3TotalMieOpticalDepth  = g_f4MieExtinctionCoeff.rgb * f2ParticleDensity.y;
        
    // Compute extinction
    float3 f3Extinction = exp( -(f3TotalRlghOpticalDepth + f3TotalMieOpticalDepth) );
    return f3Extinction;
}

void ComputeUnshadowedInscattering(float2 f2SampleLocation, 
                                   float fCamSpaceZ,
                                   /*uniform const*/ float fNumSteps,
                                   out float3 f3Inscattering,
                                   out float3 f3Extinction)
{
    f3Inscattering = float3(0);
    f3Extinction = float3(1);
    float3 f3RayTermination = ProjSpaceXYZToWorldSpace( float3(f2SampleLocation, fCamSpaceZ) );
    float3 f3CameraPos = g_f4CameraPos.xyz;
    float3 f3ViewDir = f3RayTermination - f3CameraPos;
    float fRayLength = length(f3ViewDir);
    f3ViewDir /= fRayLength;

	
//	f3Inscattering = f3RayTermination;
//	return;

    float3 f3EarthCentre =  - float3(0,1,0) * g_fEarthRadius;
    float2 f2RayAtmTopIsecs;
    GetRaySphereIntersection( f3CameraPos, f3ViewDir, f3EarthCentre, 
                              g_fAtmTopRadius, 
                              f2RayAtmTopIsecs);
    if( f2RayAtmTopIsecs.y <= 0 )
        return;

    float3 f3RayStart = f3CameraPos + f3ViewDir * max(0, f2RayAtmTopIsecs.x);
    if( fCamSpaceZ > g_fFarPlaneZ ) // fFarPlaneZ is pre-multiplied with 0.999999f
    {
//    	float2 f2UV = f2SampleLocation/float2(g_Proj[0][0], -g_Proj[1][1]);
//		float3 f3Dir = (g_ViewInv * float4(f2UV, 1, 0)).xyz;
//		f3ViewDir = normalize(f3Dir);  // convert the GL dir to DX dir.
        fRayLength = +FLT_MAX;
//        f3ViewDir.yz *= -1.0;
    }
    float3 f3RayEnd = f3CameraPos + f3ViewDir * min(fRayLength, f2RayAtmTopIsecs.y);
            
#if SINGLE_SCATTERING_MODE == SINGLE_SCTR_MODE_INTEGRATION
    IntegrateUnshadowedInscattering(f3RayStart, 
                                    f3RayEnd,
                                    f3ViewDir,
                                    f3EarthCentre,
                                    g_f4DirOnLight.xyz,
                                    fNumSteps,
                                    f3Inscattering,
                                    f3Extinction);
#endif


#if SINGLE_SCATTERING_MODE == SINGLE_SCTR_MODE_LUT || MULTIPLE_SCATTERING_MODE > MULTIPLE_SCTR_MODE_NONE

#if MULTIPLE_SCATTERING_MODE > MULTIPLE_SCTR_MODE_NONE
    #if SINGLE_SCATTERING_MODE == SINGLE_SCTR_MODE_LUT
        #define tex3DSctrLUT g_tex3DMultipleSctrLUT
    #elif SINGLE_SCATTERING_MODE == SINGLE_SCTR_MODE_NONE || SINGLE_SCATTERING_MODE == SINGLE_SCTR_MODE_INTEGRATION
        #define tex3DSctrLUT g_tex3DHighOrderSctrLUT
    #endif
#else
    #define tex3DSctrLUT g_tex3DSingleSctrLUT
#endif

    f3Extinction = GetExtinctionUnverified(f3RayStart, f3RayEnd, f3ViewDir, f3EarthCentre);

    // To avoid artifacts, we must be consistent when performing look-ups into the scattering texture, i.e.
    // we must assure that if the first look-up is above (below) horizon, then the second look-up
    // is also above (below) horizon. 
    float4 f4UVWQ = float4(-1);
    f3Inscattering +=                LookUpPrecomputedScattering(f3RayStart, f3ViewDir, f3EarthCentre, g_f4DirOnLight.xyz, tex3DSctrLUT, f4UVWQ); 
    // Provide previous look-up coordinates to the function to assure that look-ups are consistent
    f3Inscattering -= f3Extinction * LookUpPrecomputedScattering(f3RayEnd,   f3ViewDir, f3EarthCentre, g_f4DirOnLight.xyz, tex3DSctrLUT, f4UVWQ);

#undef tex3DSctrLUT
#endif


}

float3 GetExtinction(in float3 f3StartPos, in float3 f3EndPos)
{
    float3 f3EyeDir = f3EndPos - f3StartPos;
    float fRayLength = length(f3EyeDir);
    f3EyeDir /= fRayLength;

    float3 f3EarthCentre = /*g_CameraAttribs.f4CameraPos.xyz*float3(1,0,1)*/ - float3(0,1,0) * g_fEarthRadius;

    float2 f2RayAtmTopIsecs; 
    // Compute intersections of the view ray with the atmosphere
    GetRaySphereIntersection(f3StartPos, f3EyeDir, f3EarthCentre, g_fAtmTopRadius, f2RayAtmTopIsecs);
    // If the ray misses the atmosphere, there is no extinction
    if( f2RayAtmTopIsecs.y < 0 )return float3(1);

    // Do not let the start and end point be outside the atmosphere
    f3EndPos = f3StartPos + f3EyeDir * min(f2RayAtmTopIsecs.y, fRayLength);
    f3StartPos += f3EyeDir * max(f2RayAtmTopIsecs.x, 0);

    return GetExtinctionUnverified(f3StartPos, f3EndPos, f3EyeDir, f3EarthCentre);
}

// This function calculates inscattered light integral over the ray from the camera to 
// the specified world space position using ray marching
float3 ComputeShadowedInscattering( in float2 f2RayMarchingSampleLocation,
                                    in float fRayEndCamSpaceZ,
                                    in float fCascadeInd,
                                    in /*uniform const*/ bool bUse1DMinMaxMipMap = false,
                                    uint uiEpipolarSliceInd = 0 )
{   
    float3 f3CameraPos = g_f4CameraPos.xyz;
    int uiCascadeInd = int(fCascadeInd);
    
    // Compute the ray termination point, full ray length and view direction
    float3 f3RayTermination = ProjSpaceXYZToWorldSpace( float3(f2RayMarchingSampleLocation, fRayEndCamSpaceZ) );
    float3 f3FullRay = f3RayTermination - f3CameraPos;
    float fFullRayLength = length(f3FullRay);
    float3 f3ViewDir = f3FullRay / fFullRayLength;

    const float3 f3EarthCentre = float3(0, -g_fEarthRadius, 0);

    // Intersect the ray with the top of the atmosphere and the Earth:
    float4 f4Isecs;
    GetRaySphereIntersection2(f3CameraPos, f3ViewDir, f3EarthCentre, 
                              float2(g_fAtmTopRadius, g_fEarthRadius), f4Isecs);
    float2 f2RayAtmTopIsecs = f4Isecs.xy; 
    float2 f2RayEarthIsecs  = f4Isecs.zw;
//    return	f4Isecs.xyz;
//	return float3(CASCADE_PROCESSING_MODE, USE_COMBINED_MIN_MAX_TEXTURE, SINGLE_SCATTERING_MODE);
//	return float3(MULTIPLE_SCATTERING_MODE,0,0);
    
    if( f2RayAtmTopIsecs.y <= 0 )
    {
        //                                                          view dir
        //                                                        /
        //             d<0                                       /
        //               *--------->                            *
        //            .      .                             .   /  . 
        //  .  '                    '  .         .  '         /\         '  .
        //                                                   /  f2rayatmtopisecs.y < 0
        //
        // the camera is outside the atmosphere and the ray either does not intersect the
        // top of it or the intersection point is behind the camera. In either
        // case there is no inscattering
        return float3(0);
    }

    // Restrict the camera position to the top of the atmosphere
    float fDistToAtmosphere = max(f2RayAtmTopIsecs.x, 0.0);
    float3 f3RestrainedCameraPos = f3CameraPos + fDistToAtmosphere * f3ViewDir;

    // Limit the ray length by the distance to the top of the atmosphere if the ray does not hit terrain
    float fOrigRayLength = fFullRayLength;
    if( fRayEndCamSpaceZ > g_fFarPlaneZ ) // fFarPlaneZ is pre-multiplied with 0.999999f
        fFullRayLength = +FLT_MAX;
    // Limit the ray length by the distance to the point where the ray exits the atmosphere
    fFullRayLength = min(fFullRayLength, f2RayAtmTopIsecs.y);

    // If there is an intersection with the Earth surface, limit the tracing distance to the intersection
    if( f2RayEarthIsecs.x > 0 )
    {
        fFullRayLength = min(fFullRayLength, f2RayEarthIsecs.x);
    }

    fRayEndCamSpaceZ *= fFullRayLength / fOrigRayLength; 
//    return float3(fRayEndCamSpaceZ,0,0);
//    return f3RestrainedCameraPos;
    
    float3 f3RayleighInscattering = float3(0);
    float3 f3MieInscattering = float3(0);
    float2 f2ParticleNetDensityFromCam = float2(0);
    float3 f3RayEnd = float3(0), f3RayStart = float3(0);
    
    // Note that cosTheta = dot(DirOnCamera, LightDir) = dot(ViewDir, DirOnLight) because
    // DirOnCamera = -ViewDir and LightDir = -DirOnLight
    float cosTheta = dot(f3ViewDir, g_f4DirOnLight.xyz);
    
    float fCascadeEndCamSpaceZ = 0;
    float fTotalLitLength = 0, fTotalMarchedLength = 0; // Required for multiple scattering
    float fDistToFirstLitSection = -1; // Used only in when SINGLE_SCATTERING_MODE == SINGLE_SCTR_MODE_LUT

#if CASCADE_PROCESSING_MODE == CASCADE_PROCESSING_MODE_SINGLE_PASS
    for(; uiCascadeInd < g_iNumCascades; ++uiCascadeInd, ++fCascadeInd)
#else
    for(int i=0; i<1; ++i)
#endif
    {
        float2 f2CascadeStartEndCamSpaceZ = g_f4ShadowAttribs_Cascades_StartEndZ[uiCascadeInd].xy;
        float fCascadeStartCamSpaceZ = f2CascadeStartEndCamSpaceZ.x;//(uiCascadeInd > (uint)g_PPAttribs.m_iFirstCascade) ? f2CascadeStartEndCamSpaceZ.x : 0;
        fCascadeEndCamSpaceZ = f2CascadeStartEndCamSpaceZ.y;
        
        // Check if the ray terminates before it enters current cascade 
        if( fRayEndCamSpaceZ < fCascadeStartCamSpaceZ )
        {
#if CASCADE_PROCESSING_MODE == CASCADE_PROCESSING_MODE_SINGLE_PASS
            break;
#else
            return float3(0);
#endif
        }

        // Truncate the ray against the far and near planes of the current cascade:
        float fRayEndRatio = min( fRayEndCamSpaceZ, fCascadeEndCamSpaceZ ) / fRayEndCamSpaceZ;
        float fRayStartRatio = fCascadeStartCamSpaceZ / fRayEndCamSpaceZ;
        float fDistToRayStart = fFullRayLength * fRayStartRatio;
        float fDistToRayEnd   = fFullRayLength * fRayEndRatio;

        // If the camera is outside the atmosphere and the ray intersects the top of it,
        // we must start integration from the first intersection point.
        // If the camera is in the atmosphere, first intersection point is always behind the camera 
        // and thus is negative
        //                               
        //                      
        //                     
        //                   *                                              /
        //              .   /  .                                       .   /  . 
        //    .  '         /\         '  .                   .  '         /\         '  .
        //                /  f2RayAtmTopIsecs.x > 0                      /  f2RayAtmTopIsecs.y > 0
        //                                                              *
        //                 f2RayAtmTopIsecs.y > 0                         f2RayAtmTopIsecs.x < 0
        //                /                                              /
        //
        fDistToRayStart = max(fDistToRayStart, f2RayAtmTopIsecs.x);
        fDistToRayEnd   = max(fDistToRayEnd,   f2RayAtmTopIsecs.x);
        
        // To properly compute scattering from the space, we must 
        // set up ray end position before extiting the loop
        f3RayEnd   = f3CameraPos + f3ViewDir * fDistToRayEnd;
        f3RayStart = f3CameraPos + f3ViewDir * fDistToRayStart;

#if CASCADE_PROCESSING_MODE != CASCADE_PROCESSING_MODE_SINGLE_PASS
        float r = length(f3RestrainedCameraPos - f3EarthCentre);
        float fCosZenithAngle = dot(f3RestrainedCameraPos-f3EarthCentre, f3ViewDir) / r;
        float fDist = max(fDistToRayStart - fDistToAtmosphere, 0);
        f2ParticleNetDensityFromCam = GetDensityIntegralAnalytic(r, fCosZenithAngle, fDist);
#endif

        float fRayLength = fDistToRayEnd - fDistToRayStart;
        if( fRayLength <= 10 )
        {
#if CASCADE_PROCESSING_MODE == CASCADE_PROCESSING_MODE_SINGLE_PASS
            continue;
#else
            if( int(uiCascadeInd) == g_iNumCascades-1 )
                // We need to process remaining part of the ray
                break;
            else
                return float3(0);
#endif
        }

        // We trace the ray in the light projection space, not in the world space
        // Compute shadow map UV coordinates of the ray end point and its depth in the light space
        mat4 mWorldToShadowMapUVDepth = g_WorldToShadowMapUVDepth[uiCascadeInd];
        float3 f3StartUVAndDepthInLightSpace = WorldSpaceToShadowMapUV(f3RayStart, mWorldToShadowMapUVDepth);
//        return f3StartUVAndDepthInLightSpace;
        //f3StartUVAndDepthInLightSpace.z -= SHADOW_MAP_DEPTH_BIAS;
        float3 f3EndUVAndDepthInLightSpace = WorldSpaceToShadowMapUV(f3RayEnd, mWorldToShadowMapUVDepth);
        //f3EndUVAndDepthInLightSpace.z -= SHADOW_MAP_DEPTH_BIAS;

        // Calculate normalized trace direction in the light projection space and its length
        float3 f3ShadowMapTraceDir = f3EndUVAndDepthInLightSpace.xyz - f3StartUVAndDepthInLightSpace.xyz;
        // If the ray is directed exactly at the light source, trace length will be zero
        // Clamp to a very small positive value to avoid division by zero
        float fTraceLenInShadowMapUVSpace = max( length( f3ShadowMapTraceDir.xy ), 1e-7 );
        // Note that f3ShadowMapTraceDir.xy can be exactly zero
        f3ShadowMapTraceDir /= fTraceLenInShadowMapUVSpace;
        if (uiCascadeInd == 2)
		{
//			return f3RayEnd;
//			return float3(fCascadeEndCamSpaceZ, 0,0);
		}
//    return f3ShadowMapTraceDir;
        float fShadowMapUVStepLen = 0;
        float2 f2SliceOriginUV = float2(0);
        float2 f2SliceDirUV = float2(0);
        uint uiMinMaxTexYInd = 0;
        if( bUse1DMinMaxMipMap )
        {
            // Get UV direction for this slice
            float4 f4SliceUVDirAndOrigin = 
//            					g_tex2DSliceUVDirAndOrigin.Load( uint3(uiEpipolarSliceInd,uiCascadeInd,0) );
								texelFetch(g_tex2DSliceUVDirAndOrigin, int2(uiEpipolarSliceInd,uiCascadeInd), 0);
            f2SliceDirUV = f4SliceUVDirAndOrigin.xy;
            //if( all(f4SliceUVDirAndOrigin == g_f4IncorrectSliceUVDirAndStart) )
            //{
            //    return float3(0,0,0);
            //}
            //return float3(f4SliceUVDirAndOrigin.xy,0);
            // Scale with the shadow map texel size
            fShadowMapUVStepLen = length(f2SliceDirUV);
            f2SliceOriginUV = f4SliceUVDirAndOrigin.zw;
         
#if USE_COMBINED_MIN_MAX_TEXTURE
            uiMinMaxTexYInd = uiEpipolarSliceInd + (uiCascadeInd - g_iFirstCascade) * g_uiNumEpipolarSlices;
#else
            uiMinMaxTexYInd = uiEpipolarSliceInd;
#endif

        }
        else
        {
            //Calculate length of the trace step in light projection space
            float fMaxTraceDirDim = max( abs(f3ShadowMapTraceDir.x), abs(f3ShadowMapTraceDir.y) );
            fShadowMapUVStepLen = (fMaxTraceDirDim > 0) ? (g_f2ShadowMapTexelSize.x / fMaxTraceDirDim) : 0;
            // Take into account maximum number of steps specified by the g_MiscParams.fMaxStepsAlongRay
            fShadowMapUVStepLen = max(fTraceLenInShadowMapUVSpace/g_fMaxStepsAlongRay, fShadowMapUVStepLen);
        }
    
        // Calcualte ray step length in world space
        float fRayStepLengthWS = fRayLength * (fShadowMapUVStepLen / fTraceLenInShadowMapUVSpace);
        // Note that fTraceLenInShadowMapUVSpace can be very small when looking directly at sun
        // Since fShadowMapUVStepLen is at least one shadow map texel in size, 
        // fShadowMapUVStepLen / fTraceLenInShadowMapUVSpace >> 1 in this case and as a result
        // fRayStepLengthWS >> fRayLength

        // March the ray
        float fDistanceMarchedInCascade = 0;
        float3 f3CurrShadowMapUVAndDepthInLightSpace = f3StartUVAndDepthInLightSpace.xyz;

        // The following variables are used only if 1D min map optimization is enabled
        uint uiMinLevel = 0;
        // It is essential to round initial sample pos to the closest integer
        uint uiCurrSamplePos = uint(length(f3StartUVAndDepthInLightSpace.xy - f2SliceOriginUV.xy)/fShadowMapUVStepLen + 0.5);
        uint uiCurrTreeLevel = 0;
        // Note that min/max shadow map does not contain finest resolution level
        // The first level it contains corresponds to step == 2
        int iLevelDataOffset = -int(g_uiMinMaxShadowMapResolution);
        float fStepScale = 1.f;
        float fMaxStepScale = g_fMaxShadowMapStep;
#if SINGLE_SCATTERING_MODE == SINGLE_SCTR_MODE_INTEGRATION
        // In order for the numerical integration to be accurate enough, it is necessary to make 
        // at least 10 steps along the ray. To assure this, limit the maximum world step by 
        // 1/10 of the ray length.
        // To avoid aliasing artifacts due to unstable sampling along the view ray, do this for
        // each cascade separately
        float fMaxAllowedWorldStepLen = fRayLength/10;
        fMaxStepScale = min(fMaxStepScale, fMaxAllowedWorldStepLen/fRayStepLengthWS);
        
        // Make sure that the world step length is not greater than the maximum allowable length
        if( fRayStepLengthWS > fMaxAllowedWorldStepLen )
        {
            fRayStepLengthWS = fMaxAllowedWorldStepLen;
            // Recalculate shadow map UV step len
            fShadowMapUVStepLen = fTraceLenInShadowMapUVSpace * fRayStepLengthWS / fRayLength;
            // Disable 1D min/max optimization. Note that fMaxStepScale < 1 anyway since 
            // fRayStepLengthWS > fMaxAllowedWorldStepLen. Thus there is no real need to
            // make the max shadow map step negative. We do this just for clarity
            fMaxStepScale = -1;
        }
#endif

        // Scale trace direction in light projection space to calculate the step in shadow map
        float3 f3ShadowMapUVAndDepthStep = f3ShadowMapTraceDir * fShadowMapUVStepLen;
        if (uiCascadeInd == 2)
		{
//			return f3ShadowMapTraceDir;
		}
        
//        [loop]
        while( fDistanceMarchedInCascade < fRayLength )
        {
            // Clamp depth to a very small positive value to avoid z-fighting at camera location
            float fCurrDepthInLightSpace = max(f3CurrShadowMapUVAndDepthInLightSpace.z, 1e-7);
            float IsInLight = 0;

            if( bUse1DMinMaxMipMap )
            {
                // If the step scale can be doubled without exceeding the maximum allowed scale and 
                // the sample is located at the appropriate position, advance to the next coarser level
                if( 2*fStepScale < fMaxStepScale && ((uiCurrSamplePos & ((2<<uiCurrTreeLevel)-1)) == 0) )
                {
                    iLevelDataOffset += g_uiMinMaxShadowMapResolution >> uiCurrTreeLevel;
                    uiCurrTreeLevel++;
                    fStepScale *= 2.f;
                }

                while(uiCurrTreeLevel > uiMinLevel)
                {
                    // Compute light space depths at the ends of the current ray section

                    // What we need here is actually depth which is divided by the camera view space z
                    // Thus depth can be correctly interpolated in screen space:
                    // http://www.comp.nus.edu.sg/~lowkl/publications/lowk_persp_interp_techrep.pdf
                    // A subtle moment here is that we need to be sure that we can skip fStepScale samples 
                    // starting from 0 up to fStepScale-1. We do not need to do any checks against the sample fStepScale away:
                    //
                    //     --------------->
                    //
                    //          *
                    //               *         *
                    //     *              *     
                    //     0    1    2    3
                    //
                    //     |------------------>|
                    //        fStepScale = 4
                    float fNextLightSpaceDepth = f3CurrShadowMapUVAndDepthInLightSpace.z + f3ShadowMapUVAndDepthStep.z * (fStepScale-1);
                    float2 f2StartEndDepthOnRaySection = float2(f3CurrShadowMapUVAndDepthInLightSpace.z, fNextLightSpaceDepth);
                    f2StartEndDepthOnRaySection = f2StartEndDepthOnRaySection;//max(f2StartEndDepthOnRaySection, 1e-7);

                    // Load 1D min/max depths
                    float2 f2CurrMinMaxDepth = 
                    			//g_tex2DMinMaxLightSpaceDepth.Load( uint3( (uiCurrSamplePos>>uiCurrTreeLevel) + iLevelDataOffset, uiMinMaxTexYInd, 0) );
                				texelFetch(g_tex2DMinMaxLightSpaceDepth, int2((uiCurrSamplePos>>uiCurrTreeLevel) + iLevelDataOffset, uiMinMaxTexYInd), 0).rg;
                    // Since we use complimentary depth buffer, the relations are reversed
#if TEST_STATIC_SCENE == 1
                    IsInLight = all( greaterThanEqual(f2StartEndDepthOnRaySection, f2CurrMinMaxDepth.yy) ) ? 1.0: 0.0;  //TODO In normal form, greaterThanEqual=>lessThanEqual
                    bool bIsInShadow = all( lessThan(f2StartEndDepthOnRaySection, f2CurrMinMaxDepth.xx) );				//TODO In normal form, lessThan=> greaterThan
#else
					IsInLight = all( lessThan(f2StartEndDepthOnRaySection, f2CurrMinMaxDepth.xx) ) ? 1.0: 0.0;
                    bool bIsInShadow = all( greaterThanEqual(f2StartEndDepthOnRaySection, f2CurrMinMaxDepth.yy) );
#endif
                    if( IsInLight != 0.0 || bIsInShadow )
                        // If the ray section is fully lit or shadowed, we can break the loop
                        break;
                    // If the ray section is neither fully lit, nor shadowed, we have to go to the finer level
                    uiCurrTreeLevel--;
                    iLevelDataOffset -= int(g_uiMinMaxShadowMapResolution >> uiCurrTreeLevel);
                    fStepScale /= 2.f;
                };

                // If we are at the finest level, sample the shadow map with PCF
//                [branch]
                if( uiCurrTreeLevel <= uiMinLevel )
                {
                    IsInLight = 
       //             g_tex2DShadowMapArray.SampleCmpLevelZero( samComparison, float3(f3CurrShadowMapUVAndDepthInLightSpace.xy,fCascadeInd), fCurrDepthInLightSpace  ).x;
                	texture(g_tex2DShadowMapArray, float4(f3CurrShadowMapUVAndDepthInLightSpace.xy, fCascadeInd,fCurrDepthInLightSpace));
                }
            }
            else
            {
                IsInLight = 
//                g_tex2DShadowMapArray.SampleCmpLevelZero( samComparison, float3(f3CurrShadowMapUVAndDepthInLightSpace.xy,fCascadeInd), fCurrDepthInLightSpace ).x;
            	texture(g_tex2DShadowMapArray, float4(f3CurrShadowMapUVAndDepthInLightSpace.xy, fCascadeInd, fCurrDepthInLightSpace));
//if(uiCascadeInd == 2)
//            	return float3(f3CurrShadowMapUVAndDepthInLightSpace.xy, fCurrDepthInLightSpace);
//				IsInLight=max(1.0, IsInLight);
//if(uiCascadeInd == 4)
//            	return float3(IsInLight,fCascadeInd,0);
            }

            float fRemainingDist = max(fRayLength - fDistanceMarchedInCascade, 0);
            float fIntegrationStep = min(fRayStepLengthWS * fStepScale, fRemainingDist);
            float fIntegrationDist = fDistanceMarchedInCascade + fIntegrationStep/2;

#if SINGLE_SCATTERING_MODE == SINGLE_SCTR_MODE_INTEGRATION
            float3 f3CurrPos = f3RayStart + f3ViewDir * fIntegrationDist;

            // Calculate integration point height above the SPHERICAL Earth surface:
            float3 f3EarthCentreToPointDir = f3CurrPos - f3EarthCentre;
            float fDistToEarthCentre = length(f3EarthCentreToPointDir);
            f3EarthCentreToPointDir /= fDistToEarthCentre;
            float fHeightAboveSurface = fDistToEarthCentre - g_fEarthRadius;

            float2 f2ParticleDensity = exp( -fHeightAboveSurface / PARTICLE_SCALE_HEIGHT );

            // Do not use this branch as it only degrades performance
            //if( IsInLight == 0)
            //    continue;

            // Get net particle density from the integration point to the top of the atmosphere:
            float fCosSunZenithAngle = dot( f3EarthCentreToPointDir, g_f4DirOnLight.xyz );
            float2 f2NetParticleDensityToAtmTop = GetNetParticleDensity(fHeightAboveSurface, fCosSunZenithAngle);
        
            // Compute total particle density from the top of the atmosphere through the integraion point to camera
            float2 f2TotalParticleDensity = f2ParticleNetDensityFromCam + f2NetParticleDensityToAtmTop;
        
            // Update net particle density from the camera to the integration point:
            f2ParticleNetDensityFromCam += f2ParticleDensity * fIntegrationStep;

            // Get optical depth
            float3 f3TotalRlghOpticalDepth = g_f4RayleighExtinctionCoeff.rgb * f2TotalParticleDensity.x;
            float3 f3TotalMieOpticalDepth  = g_f4MieExtinctionCoeff.rgb      * f2TotalParticleDensity.y;
        
            // And total extinction for the current integration point:
            float3 f3TotalExtinction = exp( -(f3TotalRlghOpticalDepth + f3TotalMieOpticalDepth) );

            f2ParticleDensity *= fIntegrationStep * IsInLight;
            f3RayleighInscattering += f2ParticleDensity.x * f3TotalExtinction;
            f3MieInscattering      += f2ParticleDensity.y * f3TotalExtinction;
#endif

#if MULTIPLE_SCATTERING_MODE == MULTIPLE_SCTR_MODE_OCCLUDED || SINGLE_SCATTERING_MODE == SINGLE_SCTR_MODE_LUT
            // Store the distance where the ray first enters the light
            fDistToFirstLitSection = (fDistToFirstLitSection < 0 && IsInLight > 0) ? fTotalMarchedLength : fDistToFirstLitSection;
#endif
            f3CurrShadowMapUVAndDepthInLightSpace += f3ShadowMapUVAndDepthStep * fStepScale;
            if (uiCascadeInd == 2)
			{
//				return f3ShadowMapUVAndDepthStep;
//				return float3(fStepScale);
			}
            uiCurrSamplePos += 1 << uiCurrTreeLevel; // int -> float conversions are slow
            fDistanceMarchedInCascade += fRayStepLengthWS * fStepScale;

#if MULTIPLE_SCATTERING_MODE == MULTIPLE_SCTR_MODE_OCCLUDED || SINGLE_SCATTERING_MODE == SINGLE_SCTR_MODE_LUT
            fTotalLitLength += fIntegrationStep * IsInLight;
            fTotalMarchedLength += fIntegrationStep;
#endif
        }
    }

#if MULTIPLE_SCATTERING_MODE == MULTIPLE_SCTR_MODE_OCCLUDED || SINGLE_SCATTERING_MODE == SINGLE_SCTR_MODE_LUT
    // If the whole ray is in shadow, set the distance to the first lit section to the
    // total marched distance
    if( fDistToFirstLitSection < 0 )
        fDistToFirstLitSection = fTotalMarchedLength;
#endif

    float3 f3RemainingRayStart = float3(0);
    float fRemainingLength = 0;
    if( 
#if CASCADE_PROCESSING_MODE != CASCADE_PROCESSING_MODE_SINGLE_PASS
        int(uiCascadeInd) == g_iNumCascades-1 && 
#endif
        fRayEndCamSpaceZ > fCascadeEndCamSpaceZ 
       )
    {
        f3RemainingRayStart = f3RayEnd;
        f3RayEnd = f3CameraPos + fFullRayLength * f3ViewDir;
        fRemainingLength = length(f3RayEnd - f3RemainingRayStart);
#if SINGLE_SCATTERING_MODE == SINGLE_SCTR_MODE_INTEGRATION
        // Do not allow integration step to become less than 50 km
        // Maximum possible view ray length is 2023 km (from the top of the
        // atmosphere touching the Earth and then again to the top of the 
        // atmosphere).
        // For such ray, 41 integration step will be performed
        // Also assure that at least 20 steps are always performed
        float fMinStep = 50000.f;
        float fMumSteps = max(20, ceil(fRemainingLength/fMinStep) );
        ComputeInsctrIntegral(f3RemainingRayStart,
                              f3RayEnd,
                              f3EarthCentre,
                              g_f4DirOnLight.xyz,
                              f2ParticleNetDensityFromCam,
                              f3RayleighInscattering,
                              f3MieInscattering,
                              fMumSteps);
#endif
    }

    float3 f3InsctrIntegral = float3(0);

#if SINGLE_SCATTERING_MODE == SINGLE_SCTR_MODE_INTEGRATION
    // Apply phase functions
    // Note that cosTheta = dot(DirOnCamera, LightDir) = dot(ViewDir, DirOnLight) because
    // DirOnCamera = -ViewDir and LightDir = -DirOnLight
    ApplyPhaseFunctions(f3RayleighInscattering, f3MieInscattering, cosTheta);

    f3InsctrIntegral = f3RayleighInscattering + f3MieInscattering;
#endif

#if CASCADE_PROCESSING_MODE == CASCADE_PROCESSING_MODE_SINGLE_PASS
    // Note that the first cascade used for ray marching must contain camera within it
    // otherwise this expression might fail
    f3RayStart = f3RestrainedCameraPos;
#endif

#if SINGLE_SCATTERING_MODE == SINGLE_SCTR_MODE_LUT || MULTIPLE_SCATTERING_MODE == MULTIPLE_SCTR_MODE_OCCLUDED

#if MULTIPLE_SCATTERING_MODE == MULTIPLE_SCTR_MODE_OCCLUDED
    #if SINGLE_SCATTERING_MODE == SINGLE_SCTR_MODE_LUT
//        Texture3D<float3> tex3DSctrLUT = g_tex3DMultipleSctrLUT;
    	#define tex3DSctrLUT g_tex3DMultipleSctrLUT
    #elif SINGLE_SCATTERING_MODE == SINGLE_SCTR_MODE_NONE || SINGLE_SCATTERING_MODE == SINGLE_SCTR_MODE_INTEGRATION
//        Texture3D<float3> tex3DSctrLUT = g_tex3DHighOrderSctrLUT;
		#define tex3DSctrLUT g_tex3DHighOrderSctrLUT
    #endif
#else
//    Texture3D<float3> tex3DSctrLUT = g_tex3DSingleSctrLUT;
		#define tex3DSctrLUT g_tex3DSingleSctrLUT
#endif

    float3 f3MultipleScattering = float3(0);
    if( fTotalLitLength > 0 )
    {    
        float3 f3LitSectionStart = f3RayStart + fDistToFirstLitSection * f3ViewDir;
        float3 f3LitSectionEnd = f3LitSectionStart + fTotalLitLength * f3ViewDir;

        float3 f3ExtinctionToStart = GetExtinctionUnverified(f3RestrainedCameraPos, f3LitSectionStart, f3ViewDir, f3EarthCentre);
        float4 f4UVWQ = float4(-1);
        f3MultipleScattering = f3ExtinctionToStart * LookUpPrecomputedScattering(f3LitSectionStart, f3ViewDir, f3EarthCentre, g_f4DirOnLight.xyz, tex3DSctrLUT, f4UVWQ); 
        
        float3 f3ExtinctionToEnd = GetExtinctionUnverified(f3RestrainedCameraPos, f3LitSectionEnd, f3ViewDir,  f3EarthCentre);
        // To avoid artifacts, we must be consistent when performing look-ups into the scattering texture, i.e.
        // we must assure that if the first look-up is above (below) horizon, then the second look-up
        // is also above (below) horizon.
        // We provide previous look-up coordinates to the function so that it is able to figure out where the first look-up
        // was performed
        f3MultipleScattering -= f3ExtinctionToEnd * LookUpPrecomputedScattering(f3LitSectionEnd, f3ViewDir, f3EarthCentre, g_f4DirOnLight.xyz, tex3DSctrLUT, f4UVWQ);
        
        f3InsctrIntegral += max(f3MultipleScattering, 0);
    }

    // Add contribution from the reminder of the ray behind the largest cascade
    if( fRemainingLength > 0 )
    {
        float3 f3Extinction = GetExtinctionUnverified(f3RestrainedCameraPos, f3RemainingRayStart, f3ViewDir, f3EarthCentre);
        float4 f4UVWQ = float4(-1);
        float3 f3RemainingInsctr = 
            f3Extinction * LookUpPrecomputedScattering(f3RemainingRayStart, f3ViewDir, f3EarthCentre, g_f4DirOnLight.xyz, tex3DSctrLUT, f4UVWQ);
        
        f3Extinction = GetExtinctionUnverified(f3RestrainedCameraPos, f3RayEnd, f3ViewDir, f3EarthCentre);
        f3RemainingInsctr -= 
            f3Extinction * LookUpPrecomputedScattering(f3RayEnd, f3ViewDir, f3EarthCentre, g_f4DirOnLight.xyz, tex3DSctrLUT, f4UVWQ);

        f3InsctrIntegral += max(f3RemainingInsctr, 0);
    }
#undef tex3DSctrLUT
#endif

#if MULTIPLE_SCATTERING_MODE == MULTIPLE_SCTR_MODE_UNOCCLUDED
    {
        float3 f3HighOrderScattering = float3(0), f3Extinction = float3(0);
        
        float4 f4UVWQ = float4(-1);
        f3Extinction = GetExtinctionUnverified(f3RestrainedCameraPos, f3RayStart, f3ViewDir, f3EarthCentre);
        f3HighOrderScattering += f3Extinction * LookUpPrecomputedScattering(f3RayStart, f3ViewDir, f3EarthCentre, g_f4DirOnLight.xyz, g_tex3DHighOrderSctrLUT, f4UVWQ); 
        
        f3Extinction = GetExtinctionUnverified(f3RestrainedCameraPos, f3RayEnd, f3ViewDir, f3EarthCentre);
        // We provide previous look-up coordinates to the function so that it is able to figure out where the first look-up
        // was performed
        f3HighOrderScattering -= f3Extinction * LookUpPrecomputedScattering(f3RayEnd, f3ViewDir, f3EarthCentre, g_f4DirOnLight.xyz, g_tex3DHighOrderSctrLUT, f4UVWQ); 

        f3InsctrIntegral += f3HighOrderScattering;
    }
#endif

    return f3InsctrIntegral * g_f4ExtraterrestrialSunColor.rgb;
}