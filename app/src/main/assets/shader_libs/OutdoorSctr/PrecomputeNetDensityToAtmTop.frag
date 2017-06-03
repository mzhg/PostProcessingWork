#include "Scattering.frag"

in float4 m_f4UVAndScreenPos;
in float  m_fInstID;

layout(location = 0) out float2 OutColor;

float2 IntegrateParticleDensity(in float3 f3Start, 
                                in float3 f3End,
                                in float3 f3EarthCentre,
                                float fNumSteps )
{
    float3 f3Step = (f3End - f3Start) / fNumSteps;
    float fStepLen = length(f3Step);
        
    float fStartHeightAboveSurface = abs( length(f3Start - f3EarthCentre) - g_fEarthRadius );
    float2 f2PrevParticleDensity = exp( -fStartHeightAboveSurface / g_f2ParticleScaleHeight );

    float2 f2ParticleNetDensity = float2(0);
    for(float fStepNum = 1.0; fStepNum <= fNumSteps; fStepNum += 1.f)
    {
        float3 f3CurrPos = f3Start + f3Step * fStepNum;
        float fHeightAboveSurface = abs( length(f3CurrPos - f3EarthCentre) - g_fEarthRadius );
        float2 f2ParticleDensity = exp( -fHeightAboveSurface / g_f2ParticleScaleHeight );
        f2ParticleNetDensity += (f2ParticleDensity + f2PrevParticleDensity) * fStepLen / 2.f;
        f2PrevParticleDensity = f2ParticleDensity;
    }
    return f2ParticleNetDensity;
}

float2 IntegrateParticleDensityAlongRay(in float3 f3Pos, 
                                        in float3 f3RayDir,
                                        float3 f3EarthCentre, 
                                        /*uniform const*/ float fNumSteps,
                                        /*uniform const*/ bool bOccludeByEarth)
{
    if( bOccludeByEarth )
    {
        // If the ray intersects the Earth, return huge optical depth
        float2 f2RayEarthIsecs; 
        GetRaySphereIntersection(f3Pos, f3RayDir, f3EarthCentre, g_fEarthRadius, f2RayEarthIsecs);
        if( f2RayEarthIsecs.x > 0 )
            return float2(1e+20);
    }

    // Get intersection with the top of the atmosphere (the start point must always be under the top of it)
    //      
    //                     /
    //                .   /  . 
    //      .  '         /\         '  .
    //                  /  f2RayAtmTopIsecs.y > 0
    //                 *
    //                   f2RayAtmTopIsecs.x < 0
    //                  /
    //      
    float2 f2RayAtmTopIsecs;
    GetRaySphereIntersection(f3Pos, f3RayDir, f3EarthCentre, g_fAtmTopRadius, f2RayAtmTopIsecs);
    float fIntegrationDist = f2RayAtmTopIsecs.y;

    float3 f3RayEnd = f3Pos + f3RayDir * fIntegrationDist;

    return IntegrateParticleDensity(f3Pos, f3RayEnd, f3EarthCentre, fNumSteps);
}

void main()
{
	float2 f2UV = ProjToUV(m_f4UVAndScreenPos.zw);
    // Do not allow start point be at the Earth surface and on the top of the atmosphere
    float fStartHeight = clamp( lerp(0, g_fAtmTopHeight, f2UV.x), 10, g_fAtmTopHeight-10 );

    float fCosTheta = m_f4UVAndScreenPos.w;  //-In.m_f2PosPS.y;  From cos(0)--cos(PI) Cause DX y-axis from top to down
    float fSinTheta = sqrt( saturate(1.0 - fCosTheta*fCosTheta) );
    float3 f3RayStart = float3(0, 0, fStartHeight);
    float3 f3RayDir = float3(fSinTheta, 0, fCosTheta);
    
    float3 f3EarthCentre = float3(0,0,-g_fEarthRadius);

    const float fNumSteps = 200;
    OutColor = IntegrateParticleDensityAlongRay(f3RayStart, f3RayDir, f3EarthCentre, fNumSteps, true);
}