
#include "Scattering.frag"

void InsctrLUTCoords2WorldParams(in float4 f4UVWQ,
                                 out float fHeight,
                                 out float fCosViewZenithAngle,
                                 out float fCosSunZenithAngle,
                                 out float fCosSunViewAngle)
{
	const float SafetyHeightMargin = 16.f;
#if NON_LINEAR_PARAMETERIZATION
	
    // Rescale to exactly 0,1 range
    f4UVWQ.xzw = saturate((f4UVWQ* PRECOMPUTED_SCTR_LUT_DIM - 0.5) / (PRECOMPUTED_SCTR_LUT_DIM-1.0)).xzw;

    f4UVWQ.x = pow( f4UVWQ.x, 1.0/HeightPower );
    // Allowable height range is limited to [SafetyHeightMargin, AtmTopHeight - SafetyHeightMargin] to
    // avoid numeric issues at the Earth surface and the top of the atmosphere
    fHeight = f4UVWQ.x * (g_fAtmTopHeight - 2.0*SafetyHeightMargin) + SafetyHeightMargin;

    fCosViewZenithAngle = TexCoord2ZenithAngle(f4UVWQ.y, fHeight, PRECOMPUTED_SCTR_LUT_DIM.y, ViewZenithPower);
    
    // Use Eric Bruneton's formula for cosine of the sun-zenith angle
    fCosSunZenithAngle = tan((2.0 * f4UVWQ.z - 1.0 + 0.26) * 1.1) / tan(1.26 * 1.1);

    f4UVWQ.w = sign(f4UVWQ.w - 0.5) * pow( abs((f4UVWQ.w - 0.5)*2.0), 1.0/SunViewPower)/2.0 + 0.5;
    fCosSunViewAngle = cos(f4UVWQ.w*PI);
#else
    // Rescale to exactly 0,1 range
    f4UVWQ = (f4UVWQ * PRECOMPUTED_SCTR_LUT_DIM - 0.5) / (PRECOMPUTED_SCTR_LUT_DIM-1.0);

    // Allowable height range is limited to [SafetyHeightMargin, AtmTopHeight - SafetyHeightMargin] to
    // avoid numeric issues at the Earth surface and the top of the atmosphere
    fHeight = f4UVWQ.x * (g_fAtmTopHeight - 2.0*SafetyHeightMargin) + SafetyHeightMargin;

    fCosViewZenithAngle = f4UVWQ.y * 2.0 - 1.0;  // remap [0,1] to [-1,1]
    fCosSunZenithAngle  = f4UVWQ.z * 2.0 - 1.0;
    fCosSunViewAngle    = f4UVWQ.w * 2.0 - 1.0;
#endif

    fCosViewZenithAngle = clamp(fCosViewZenithAngle, -1.0, +1.0);
    fCosSunZenithAngle  = clamp(fCosSunZenithAngle,  -1.0, +1.0);
    // Compute allowable range for the cosine of the sun view angle for the given
    // view zenith and sun zenith angles
    float D = (1.0 - fCosViewZenithAngle * fCosViewZenithAngle) * (1.0 - fCosSunZenithAngle  * fCosSunZenithAngle);
    
    // !!!!  IMPORTANT NOTE regarding NVIDIA hardware !!!!

    // There is a very weird issue on NVIDIA hardware with clamp(), saturate() and min()/max() 
    // functions. No matter what function is used, fCosViewZenithAngle and fCosSunZenithAngle
    // can slightly fall outside [-1,+1] range causing D to be negative
    // Using saturate(D), max(D, 0) and even D>0?D:0 does not work!
    // The only way to avoid taking the square root of negative value and obtaining NaN is 
    // to use max() with small positive value:
    D = sqrt( max(D, 1e-20) );
    
    // The issue was reproduceable on NV GTX 680, driver version 9.18.13.2723 (9/12/2013).
    // The problem does not arise on Intel hardware

    float2 f2MinMaxCosSunViewAngle = fCosViewZenithAngle*fCosSunZenithAngle + float2(-D, +D);
    // Clamp to allowable range
    fCosSunViewAngle    = clamp(fCosSunViewAngle, f2MinMaxCosSunViewAngle.x, f2MinMaxCosSunViewAngle.y);
}

void GetRaySphereIntersection2(in float3 f3RayOrigin,
                               in float3 f3RayDirection,
                               in float3 f3SphereCenter,
                               in float2 f2SphereRadius,
                               out float4 f4Intersections)
{
    // http://wiki.cgsociety.org/index.php/Ray_Sphere_Intersection
    f3RayOrigin -= f3SphereCenter;
    float A = dot(f3RayDirection, f3RayDirection);
    float B = 2.0 * dot(f3RayOrigin, f3RayDirection);
    float2 C = dot(f3RayOrigin,f3RayOrigin) - f2SphereRadius*f2SphereRadius;
    float2 D = B*B - 4.0*A*C;
    // If discriminant is negative, there are no real roots hence the ray misses the
    // sphere
//    float2 f2RealRootMask = (D.xy >= float2(0));
	float2 f2RealRootMask = float2(greaterThanEqual(D.xy, float2(0)));
    D = sqrt( max(D,0) );
    f4Intersections =   f2RealRootMask.xxyy * float4(-B - D.x, -B + D.x, -B - D.y, -B + D.y) / (2*A) + 
                      (1-f2RealRootMask.xxyy) * float4(-1,-1,-1,-1);
}

float2 GetNetParticleDensity(in float fHeightAboveSurface,
                             in float fCosZenithAngle)
{
    float fRelativeHeightAboveSurface = fHeightAboveSurface / g_fAtmTopHeight;
//    return g_tex2DOccludedNetDensityToAtmTop.SampleLevel(samLinearClamp, float2(fRelativeHeightAboveSurface, fCosZenithAngle*0.5+0.5), 0).xy;
	return textureLod(g_tex2DOccludedNetDensityToAtmTop, float2(fRelativeHeightAboveSurface, fCosZenithAngle*0.5+0.5), 0.0).xy;
}

// This function computes atmospheric properties in the given point
void GetAtmosphereProperties(in float3 f3Pos,
                             in float3 f3EarthCentre,
                             in float3 f3DirOnLight,
                             out float2 f2ParticleDensity,
                             out float2 f2NetParticleDensityToAtmTop)
{
    // Calculate the point height above the SPHERICAL Earth surface:
    float3 f3EarthCentreToPointDir = f3Pos - f3EarthCentre;
    float fDistToEarthCentre = length(f3EarthCentreToPointDir);
    f3EarthCentreToPointDir /= fDistToEarthCentre;
    float fHeightAboveSurface = fDistToEarthCentre - g_fEarthRadius;

    f2ParticleDensity = exp( -fHeightAboveSurface / g_f2ParticleScaleHeight );

    // Get net particle density from the integration point to the top of the atmosphere:
    float fCosSunZenithAngleForCurrPoint = dot( f3EarthCentreToPointDir, f3DirOnLight );
    f2NetParticleDensityToAtmTop = GetNetParticleDensity(fHeightAboveSurface, fCosSunZenithAngleForCurrPoint);
}

// This function computes differential inscattering for the given particle densities 
// (without applying phase functions)
void ComputePointDiffInsctr(in float2 f2ParticleDensityInCurrPoint,
                            in float2 f2NetParticleDensityFromCam,
                            in float2 f2NetParticleDensityToAtmTop,
                            out float3 f3DRlghInsctr,
                            out float3 f3DMieInsctr)
{
    // Compute total particle density from the top of the atmosphere through the integraion point to camera
    float2 f2TotalParticleDensity = f2NetParticleDensityFromCam + f2NetParticleDensityToAtmTop;
        
    // Get optical depth
    float3 f3TotalRlghOpticalDepth = g_f4RayleighExtinctionCoeff.rgb * f2TotalParticleDensity.x;
    float3 f3TotalMieOpticalDepth  = g_f4MieExtinctionCoeff.rgb      * f2TotalParticleDensity.y;
        
    // And total extinction for the current integration point:
    float3 f3TotalExtinction = exp( -(f3TotalRlghOpticalDepth + f3TotalMieOpticalDepth) );

    f3DRlghInsctr = f2ParticleDensityInCurrPoint.x * f3TotalExtinction;
    f3DMieInsctr  = f2ParticleDensityInCurrPoint.y * f3TotalExtinction; 
}

void ComputeInsctrIntegral(in float3 f3RayStart,
                           in float3 f3RayEnd,
                           in float3 f3EarthCentre,
                           in float3 f3DirOnLight,
                           inout float2 f2NetParticleDensityFromCam,
                           inout float3 f3RayleighInscattering,
                           inout float3 f3MieInscattering,
                           /*uniform const*/ float fNumSteps)
{
    float3 f3Step = (f3RayEnd - f3RayStart) / fNumSteps;
    float fStepLen = length(f3Step);

#if TRAPEZOIDAL_INTEGRATION
    // For trapezoidal integration we need to compute some variables for the starting point of the ray
    float2 f2PrevParticleDensity = float2(0);
    float2 f2NetParticleDensityToAtmTop = float2(0);
    GetAtmosphereProperties(f3RayStart, f3EarthCentre, f3DirOnLight, f2PrevParticleDensity, f2NetParticleDensityToAtmTop);

    float3 f3PrevDiffRInsctr = float3(0), f3PrevDiffMInsctr = float3(0);
    ComputePointDiffInsctr(f2PrevParticleDensity, f2NetParticleDensityFromCam, f2NetParticleDensityToAtmTop, f3PrevDiffRInsctr, f3PrevDiffMInsctr);
#endif


#if TRAPEZOIDAL_INTEGRATION
    // With trapezoidal integration, we will evaluate the function at the end of each section and 
    // compute area of a trapezoid
    for(float fStepNum = 1.f; fStepNum <= fNumSteps; fStepNum += 1.f)
#else
    // With stair-step integration, we will evaluate the function at the middle of each section and 
    // compute area of a rectangle
    for(float fStepNum = 0.5f; fStepNum < fNumSteps; fStepNum += 1.f)
#endif
    {
        float3 f3CurrPos = f3RayStart + f3Step * fStepNum;
        float2 f2ParticleDensity, f2NetParticleDensityToAtmTop;
        GetAtmosphereProperties(f3CurrPos, f3EarthCentre, f3DirOnLight, f2ParticleDensity, f2NetParticleDensityToAtmTop);

        // Accumulate net particle density from the camera to the integration point:
#if TRAPEZOIDAL_INTEGRATION
        f2NetParticleDensityFromCam += (f2PrevParticleDensity + f2ParticleDensity) * (fStepLen / 2.f);
        f2PrevParticleDensity = f2ParticleDensity;
#else
        f2NetParticleDensityFromCam += f2ParticleDensity * fStepLen;
#endif

        float3 f3DRlghInsctr, f3DMieInsctr;
        ComputePointDiffInsctr(f2ParticleDensity, f2NetParticleDensityFromCam, f2NetParticleDensityToAtmTop, f3DRlghInsctr, f3DMieInsctr);

#if TRAPEZOIDAL_INTEGRATION
        f3RayleighInscattering += (f3DRlghInsctr + f3PrevDiffRInsctr) * (fStepLen / 2.f);
        f3MieInscattering      += (f3DMieInsctr  + f3PrevDiffMInsctr) * (fStepLen / 2.f);

        f3PrevDiffRInsctr = f3DRlghInsctr;
        f3PrevDiffMInsctr = f3DMieInsctr;
#else
        f3RayleighInscattering += f3DRlghInsctr * fStepLen;
        f3MieInscattering      += f3DMieInsctr * fStepLen;
#endif
    }
}

void IntegrateUnshadowedInscattering(in float3 f3RayStart, 
                                     in float3 f3RayEnd,
                                     in float3 f3ViewDir,
                                     in float3 f3EarthCentre,
                                     in float3 f3DirOnLight,
                                     /*uniform const*/ float fNumSteps,
                                     out float3 f3Inscattering,
                                     out float3 f3Extinction)
{
    float2 f2NetParticleDensityFromCam = float2(0);
    float3 f3RayleighInscattering = float3(0);
    float3 f3MieInscattering = float3(0);
    ComputeInsctrIntegral( f3RayStart,
                           f3RayEnd,
                           f3EarthCentre,
                           f3DirOnLight,
                           f2NetParticleDensityFromCam,
                           f3RayleighInscattering,
                           f3MieInscattering,
                           fNumSteps);

    float3 f3TotalRlghOpticalDepth = g_f4RayleighExtinctionCoeff.rgb * f2NetParticleDensityFromCam.x;
    float3 f3TotalMieOpticalDepth  = g_f4MieExtinctionCoeff.rgb      * f2NetParticleDensityFromCam.y;
    f3Extinction = exp( -(f3TotalRlghOpticalDepth + f3TotalMieOpticalDepth) );

    // Apply phase function
    // Note that cosTheta = dot(DirOnCamera, LightDir) = dot(ViewDir, DirOnLight) because
    // DirOnCamera = -ViewDir and LightDir = -DirOnLight
    float cosTheta = dot(f3ViewDir, f3DirOnLight);
    ApplyPhaseFunctions(f3RayleighInscattering, f3MieInscattering, cosTheta);

    f3Inscattering = f3RayleighInscattering + f3MieInscattering;
}