#include "InsctrLUTCoords2WorldParams.frag"

in float4 UVAndScreenPos;
in float  m_fInstID;

layout(location = 0) out float4 OutColor;

// This shader pre-computes the radiance of single scattering at a given point in given
// direction.
void main()
{
	// Get attributes for the current point
    float2 f2UV = ProjToUV(UVAndScreenPos.zw);
//    f2UV.y = 1.0 - f2UV.y;
    float fHeight, fCosViewZenithAngle, fCosSunZenithAngle, fCosSunViewAngle;
    InsctrLUTCoords2WorldParams(float4(f2UV, g_f2WQ), fHeight, fCosViewZenithAngle, fCosSunZenithAngle, fCosSunViewAngle );
    float3 f3EarthCentre =  - float3(0,1,0) * g_fEarthRadius;
    float3 f3RayStart = float3(0, fHeight, 0);
    float3 f3ViewDir = ComputeViewDir(fCosViewZenithAngle);
    float3 f3DirOnLight = ComputeLightDir(f3ViewDir, fCosSunZenithAngle, fCosSunViewAngle);
  
    // Intersect view ray with the top of the atmosphere and the Earth
    float4 f4Isecs;
    GetRaySphereIntersection2( f3RayStart, f3ViewDir, f3EarthCentre, 
                               float2(g_fEarthRadius, g_fAtmTopRadius), 
                               f4Isecs);
    float2 f2RayEarthIsecs  = f4Isecs.xy;
    float2 f2RayAtmTopIsecs = f4Isecs.zw;

    if(f2RayAtmTopIsecs.y <= 0.0)
    {
    	OutColor = float4(0);
        return;   // This is just a sanity check and should never happen
                  // as the start point is always under the top of the 
                  // atmosphere (look at InsctrLUTCoords2WorldParams())
    }

    // Set the ray length to the distance to the top of the atmosphere
    float fRayLength = f2RayAtmTopIsecs.y;
    // If ray hits Earth, limit the length by the distance to the surface
    if(f2RayEarthIsecs.x > 0)
        fRayLength = min(fRayLength, f2RayEarthIsecs.x);
    
    float3 f3RayEnd = f3RayStart + f3ViewDir * fRayLength;

    // Integrate single-scattering
    float3 f3Inscattering, f3Extinction;
    IntegrateUnshadowedInscattering(f3RayStart, 
                                    f3RayEnd,
                                    f3ViewDir,
                                    f3EarthCentre,
                                    f3DirOnLight.xyz,
                                    100.0,
                                    f3Inscattering,
                                    f3Extinction);

    OutColor.rgb = f3Inscattering;
    OutColor.a = 0;
//	OutColor = float4(f2UV, 0, 0);
}