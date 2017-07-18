#include "Preprocessing.glsl"

in float4 m_f4UVAndScreenPos;

layout(location = 0) out float4 Out_fColor;

// This shader computes level 0 of the maximum density mip map
void main()
{
    float3 f3NormalizedStartPos, f3RayDir;
    OpticalDepthLUTCoordsToWorldParams( float4(m_f4UVAndScreenPos.xy/*ProjToUV(In.m_f2PosPS)*/, g_GlobalCloudAttribs.f4Parameter.xy), f3NormalizedStartPos, f3RayDir );

    // Intersect the view ray with the unit sphere:
    float2 f2RayIsecs;
    // f3NormalizedStartPos  is located exactly on the surface; slightly move start pos inside the sphere
    // to avoid precision issues
    GetRaySphereIntersection(f3NormalizedStartPos + f3RayDir*1e-4, f3RayDir, float3(0), 1.f, f2RayIsecs);

    if( f2RayIsecs.x > f2RayIsecs.y )
    {
//        Out_fColor = float4(m_f4UVAndScreenPos.xy, f2RayIsecs.xy);
        Out_fColor = float4(0);
        return;
    }

    float3 f3EndPos = f3NormalizedStartPos + f3RayDir * f2RayIsecs.y;
    float fNumSteps = NUM_INTEGRATION_STEPS;
    float3 f3Step = (f3EndPos - f3NormalizedStartPos) / fNumSteps;
    float fTotalDensity = 0.0;
    for(float fStepNum=0.5; fStepNum < fNumSteps; fStepNum += 1.0)
    {
        float3 f3CurrPos = f3NormalizedStartPos + f3Step * fStepNum;

        float fDensity = ComputeDensity(f3CurrPos);
        fTotalDensity += fDensity;
    }
    Out_fColor = float4(fTotalDensity / fNumSteps, 0,0,0);
}