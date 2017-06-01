#include "InsctrLUTCoords2WorldParams.frag"
#include "LookUpPrecomputedScattering.frag"

in float4 UVAndScreenPos;
in float  m_fInstID;

layout(location = 0) out float3 OutColor;

// This shader pre-computes the radiance of light scattered at a given point in given
// direction. It multiplies the previous order in-scattered light with the phase function 
// for each type of particles and integrates the result over the whole set of directions,
// see eq. (7) in [Bruneton and Neyret 08].
void main()
{
	// Get attributes for the current point
    float2 f2UV = ProjToUV(UVAndScreenPos.zw);
    float fHeight, fCosViewZenithAngle, fCosSunZenithAngle, fCosSunViewAngle;
    InsctrLUTCoords2WorldParams( float4(f2UV, g_f2WQ), fHeight, fCosViewZenithAngle, fCosSunZenithAngle, fCosSunViewAngle );
    float3 f3EarthCentre =  - float3(0,1,0) * g_fEarthRadius;
    float3 f3RayStart = float3(0, fHeight, 0);
    float3 f3ViewDir = ComputeViewDir(fCosViewZenithAngle);
    float3 f3DirOnLight = ComputeLightDir(f3ViewDir, fCosSunZenithAngle, fCosSunViewAngle);
    
    // Compute particle density scale factor
    float2 f2ParticleDensity = exp( -fHeight / g_f2ParticleScaleHeight );
    
    float3 f3SctrRadiance = float3(0);
    // Go through a number of samples randomly distributed over the sphere
    for(int iSample = 0; iSample < NUM_RANDOM_SPHERE_SAMPLES; ++iSample)
    {
        // Get random direction
        float3 f3RandomDir = normalize( 
        			//g_tex2DSphereRandomSampling.Load(int3(iSample,0,0)) 
        			texelFetch(g_tex2DSphereRandomSampling, int2(iSample,0), 0).rgb
        		);
        // Get the previous order in-scattered light when looking in direction f3RandomDir (the light thus goes in direction -f3RandomDir)
        float4 f4UVWQ = float4(-1);
        float3 f3PrevOrderSctr = LookUpPrecomputedScattering(f3RayStart, f3RandomDir, f3EarthCentre, f3DirOnLight.xyz, g_tex3DPreviousSctrOrder, f4UVWQ); 
        
        // Apply phase functions for each type of particles
        // Note that total scattering coefficients are baked into the angular scattering coeffs
        float3 f3DRlghInsctr = f2ParticleDensity.x * f3PrevOrderSctr;
        float3 f3DMieInsctr  = f2ParticleDensity.y * f3PrevOrderSctr;
        float fCosTheta = dot(f3ViewDir, f3RandomDir);
        ApplyPhaseFunctions(f3DRlghInsctr, f3DMieInsctr, fCosTheta);

        f3SctrRadiance += f3DRlghInsctr + f3DMieInsctr;
    }
    // Since we tested N random samples, each sample covered 4*Pi / N solid angle
    // Note that our phase function is normalized to 1 over the sphere. For instance,
    // uniform phase function would be p(theta) = 1 / (4*Pi).
    // Notice that for uniform intensity I if we get N samples, we must obtain exactly I after
    // numeric integration
    OutColor = f3SctrRadiance * 4.0*PI / NUM_RANDOM_SPHERE_SAMPLES;
}