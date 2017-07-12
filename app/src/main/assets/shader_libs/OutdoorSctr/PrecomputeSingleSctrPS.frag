#include "Preprocessing.glsl"

in float4 m_f4UVAndScreenPos;

layout(location = 0) out float Out_fColor;

void main()
{
    float4 f4LUTCoords = float4(/*ProjToUV(In.m_f2PosPS)*/m_f4UVAndScreenPos.xy, g_GlobalCloudAttribs.f4Parameter.xy);

    float3 f3EntryPointUSSpace, f3ViewRayUSSpace, f3LightDirUSSpace;
    ParticleScatteringLUTToWorldParams(f4LUTCoords, f3EntryPointUSSpace, f3ViewRayUSSpace, f3LightDirUSSpace, false);

    // Intersect view ray with the unit sphere:
    float2 f2RayIsecs;
    // f3NormalizedStartPos  is located exactly on the surface; slightly move the start pos inside the sphere
    // to avoid precision issues
    float3 f3BiasedEntryPoint = f3EntryPointUSSpace + f3ViewRayUSSpace*1e-4;
    GetRaySphereIntersection(f3BiasedEntryPoint, f3ViewRayUSSpace, float3(0), 1.f, f2RayIsecs);
    if( f2RayIsecs.y < f2RayIsecs.x )
    {
        Out_fColor = 0.0;
        return;
    }
    float3 f3EndPos = f3BiasedEntryPoint + f3ViewRayUSSpace * f2RayIsecs.y;

    float fNumSteps = NUM_INTEGRATION_STEPS;
    float3 f3Step = (f3EndPos - f3EntryPointUSSpace) / fNumSteps;
    float fStepLen = length(f3Step);
    float fCloudMassToCamera = 0;
    float fParticleRadius = g_GlobalCloudAttribs.fReferenceParticleRadius;
    float fInscattering = 0;
    for(float fStepNum=0.5; fStepNum < fNumSteps; ++fStepNum)
    {
        float3 f3CurrPos = f3EntryPointUSSpace + f3Step * fStepNum;
        float fCloudMassToLight = 0;
        GetRaySphereIntersection(f3CurrPos, f3LightDirUSSpace, float3(0), 1.f, f2RayIsecs);
        if( f2RayIsecs.y > f2RayIsecs.x )
        {
            // Since we are using the light direction (not direction on light), we have to use
            // the first intersection point:
            fCloudMassToLight = abs(f2RayIsecs.x) * fParticleRadius;
        }

        float fTotalLightAttenuation = exp( -g_GlobalCloudAttribs.fAttenuationCoeff * (fCloudMassToLight + fCloudMassToCamera) );
        fInscattering += fTotalLightAttenuation * g_GlobalCloudAttribs.fScatteringCoeff;
        fCloudMassToCamera += fStepLen * fParticleRadius;
    }

    Out_fColor = fInscattering * fStepLen * fParticleRadius;
}