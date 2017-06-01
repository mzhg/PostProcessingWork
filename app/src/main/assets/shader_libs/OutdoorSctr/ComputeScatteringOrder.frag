#include "InsctrLUTCoords2WorldParams.frag"
#include "LookUpPrecomputedScattering.frag"

in float4 UVAndScreenPos;
in float  m_fInstID;

layout(location = 0) out float3 OutColor;

// This shader computes in-scattering order for a given point and direction. It performs integration of the 
// light scattered at particular point along the ray, see eq. (11) in [Bruneton and Neyret 08].
void main()
{
	// Get attributes for the current point
    float2 f2UV = ProjToUV(UVAndScreenPos.zw);
    float fHeight, fCosViewZenithAngle, fCosSunZenithAngle, fCosSunViewAngle;
    InsctrLUTCoords2WorldParams(float4(f2UV, g_f2WQ), fHeight, fCosViewZenithAngle, fCosSunZenithAngle, fCosSunViewAngle );
    float3 f3EarthCentre =  - float3(0,1,0) * g_fEarthRadius;
    float3 f3RayStart = float3(0, fHeight, 0);
    float3 f3ViewDir = ComputeViewDir(fCosViewZenithAngle);
    float3 f3DirOnLight = ComputeLightDir(f3ViewDir, fCosSunZenithAngle, fCosSunViewAngle);
    
    // Intersect the ray with the atmosphere and Earth
    float4 f4Isecs;
    GetRaySphereIntersection2( f3RayStart, f3ViewDir, f3EarthCentre, 
                               float2(g_fEarthRadius, g_fAtmTopRadius), 
                               f4Isecs);
    float2 f2RayEarthIsecs  = f4Isecs.xy;
    float2 f2RayAtmTopIsecs = f4Isecs.zw;

    if(f2RayAtmTopIsecs.y <= 0.0)
    {
    	OutColor = float3(0);
        return;   // This is just a sanity check and should never happen
                  // as the start point is always under the top of the 
                  // atmosphere (look at InsctrLUTCoords2WorldParams())
    }

    float fRayLength = f2RayAtmTopIsecs.y;
    if(f2RayEarthIsecs.x > 0)
        fRayLength = min(fRayLength, f2RayEarthIsecs.x);
    
    float3 f3RayEnd = f3RayStart + f3ViewDir * fRayLength;

    const float fNumSamples = 64.0;
    float fStepLen = fRayLength / fNumSamples;

    float4 f4UVWQ = float4(-1);
    float3 f3PrevSctrRadiance = LookUpPrecomputedScattering(f3RayStart, f3ViewDir, f3EarthCentre, f3DirOnLight.xyz, g_tex3DPointwiseSctrRadiance, f4UVWQ); 
    float2 f2PrevParticleDensity = exp( -fHeight / g_f2ParticleScaleHeight );

    float2 f2NetParticleDensityFromCam = float2(0);
    float3 f3Inscattering = float3(0);

    for(float fSample=1.0; fSample <= fNumSamples; ++fSample)
    {
        float3 f3Pos = lerp(f3RayStart, f3RayEnd, fSample/fNumSamples);

        float fCurrHeight = length(f3Pos - f3EarthCentre) - g_fEarthRadius;
        float2 f2ParticleDensity = exp( -fCurrHeight / g_f2ParticleScaleHeight );

        f2NetParticleDensityFromCam += (f2PrevParticleDensity + f2ParticleDensity) * (fStepLen / 2.f);
        f2PrevParticleDensity = f2ParticleDensity;
        
        // Get optical depth
        float3 f3RlghOpticalDepth = g_f4RayleighExtinctionCoeff.rgb * f2NetParticleDensityFromCam.x;
        float3 f3MieOpticalDepth  = g_f4MieExtinctionCoeff.rgb      * f2NetParticleDensityFromCam.y;
        
        // Compute extinction from the camera for the current integration point:
        float3 f3ExtinctionFromCam = exp( -(f3RlghOpticalDepth + f3MieOpticalDepth) );

        // Get attenuated scattered light radiance in the current point
        float4 f4UVWQ = float4(-1);
        float3 f3SctrRadiance = f3ExtinctionFromCam * LookUpPrecomputedScattering(f3Pos, f3ViewDir, f3EarthCentre, f3DirOnLight.xyz, g_tex3DPointwiseSctrRadiance, f4UVWQ); 
        // Update in-scattering integral
        f3Inscattering += (f3SctrRadiance +  f3PrevSctrRadiance) * (fStepLen/2.f);
        f3PrevSctrRadiance = f3SctrRadiance;
    }

    OutColor = f3Inscattering;
}