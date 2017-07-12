#include "Preprocessing.glsl"

in float4 m_f4UVAndScreenPos;

layout(location = 0) out float Out_fColor;

void main()
{
    float4 f4StartPointLUTCoords = float4(/*ProjToUV(In.m_f2PosPS)*/m_f4UVAndScreenPos.xy, g_GlobalCloudAttribs.f4Parameter.xy);

    float3 f3PosUSSpace, f3ViewRayUSSpace, f3LightDirUSSpace;
    ParticleScatteringLUTToWorldParams(f4StartPointLUTCoords, f3PosUSSpace, f3ViewRayUSSpace, f3LightDirUSSpace, false);

    // Intersect view ray with the unit sphere:
    float2 f2RayIsecs;
    // f3NormalizedStartPos  is located exactly on the surface; slightly move start pos inside the sphere
    // to avoid precision issues
    float3 f3BiasedPos = f3PosUSSpace + f3ViewRayUSSpace*1e-4;
    GetRaySphereIntersection(f3BiasedPos, f3ViewRayUSSpace, float3(0), 1.f, f2RayIsecs);
    if( f2RayIsecs.y < f2RayIsecs.x )
    {
        Out_fColor = 0;
        return;
    }

    float3 f3EndPos = f3BiasedPos + f3ViewRayUSSpace * f2RayIsecs.y;
    float fNumSteps = max(VOL_SCATTERING_IN_PARTICLE_LUT_DIM.w*2, NUM_INTEGRATION_STEPS)*2;
    float3 f3Step = (f3EndPos - f3PosUSSpace) / fNumSteps;
    float fStepLen = length(f3Step);
    float fCloudMassToCamera = 0;
    float fParticleRadius = g_GlobalCloudAttribs.fReferenceParticleRadius;
    float fInscattering = 0;

    float fPrevGatheredSctr = 0;
    SAMPLE_4D_LUT(g_tex3DGatheredScattering, VOL_SCATTERING_IN_PARTICLE_LUT_DIM, f4StartPointLUTCoords, 0, fPrevGatheredSctr);
    // Light attenuation == 1
    for(float fStepNum=1; fStepNum <= fNumSteps; ++fStepNum)
    {
        float3 f3CurrPos = f3PosUSSpace + f3Step * fStepNum;

        fCloudMassToCamera += fStepLen * fParticleRadius;
        float fAttenuationToCamera = exp( -g_GlobalCloudAttribs.fAttenuationCoeff * fCloudMassToCamera );

        float4 f4CurrDirLUTCoords = WorldParamsToParticleScatteringLUT(f3CurrPos, f3ViewRayUSSpace, f3LightDirUSSpace, false);
        float fGatheredScattering = 0;
        SAMPLE_4D_LUT(g_tex3DGatheredScattering, VOL_SCATTERING_IN_PARTICLE_LUT_DIM, f4CurrDirLUTCoords, 0, fGatheredScattering);
        fGatheredScattering *= fAttenuationToCamera;

        fInscattering += (fGatheredScattering + fPrevGatheredSctr) /2;
        fPrevGatheredSctr = fGatheredScattering;
    }

    Out_fColor = fInscattering * fStepLen * fParticleRadius * g_GlobalCloudAttribs.fScatteringCoeff;
}