#include "LookUpPrecomputedScattering.frag"

in float4 UVAndScreenPos;
in float  m_fInstID;

layout(location = 0) out float4 OutColor;

void main()
{
	float fU = ProjToUV(UVAndScreenPos.zw).x;
    float3 f3RayStart = float3(0,20,0);
    float3 f3EarthCentre =  -float3(0,1,0) * g_fEarthRadius;
    float fCosZenithAngle = clamp(fU * 2.0 - 1.0, -1.0, +1.0);
    float3 f3DirOnLight = float3(sqrt(clamp(1.0 - fCosZenithAngle*fCosZenithAngle, 0.0, 1.0)), fCosZenithAngle, 0);
    float3 f3SkyLight = float3(0);
    // Go through a number of random directions on the sphere
    int iNumSamples = textureSize(g_tex2DSphereRandomSampling, 0).x;
    for(int iSample = 0; iSample < iNumSamples; ++iSample)
    {
        // Get random direction
        float3 f3RandomDir = normalize( 
//        			g_tex2DSphereRandomSampling.Load(int3(iSample,0,0)) 
        			texelFetch(g_tex2DSphereRandomSampling, int2(iSample, 0), 0).rgb
        			);
        // Reflect directions from the lower hemisphere
        f3RandomDir.y = abs(f3RandomDir.y);
        // Get multiple scattered light radiance when looking in direction f3RandomDir (the light thus goes in direction -f3RandomDir)
        float4 f4UVWQ = float4(-1);
		float3 f3Sctr = LookUpPrecomputedScattering(f3RayStart, f3RandomDir, f3EarthCentre, f3DirOnLight, g_tex3DPreviousSctrOrder, f4UVWQ);
        // Accumulate ambient irradiance through the horizontal plane
        f3SkyLight += f3Sctr * dot(f3RandomDir, float3(0,1,0));
    }
    // Each sample covers 2 * PI / NUM_RANDOM_SPHERE_SAMPLES solid angle (integration is performed over
    // upper hemisphere)
    OutColor.rgb = f3SkyLight * 2.0 * PI / float(iNumSamples);
    OutColor.a = 0.0;
}