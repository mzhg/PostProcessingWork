#include "Preprocessing.glsl"

in float4 m_f4UVAndScreenPos;

layout(location = 0) out float Out_fColor;

void main()
{
    float4 f4LUTCoords = float4(/*ProjToUV(In.m_f2PosPS)*/m_f4UVAndScreenPos.xy, g_GlobalCloudAttribs.f4Parameter.xy);

    float3 f3PosUSSpace, f3ViewRayUSSpace, f3LightDirUSSpace;
    ParticleScatteringLUTToWorldParams(f4LUTCoords, f3PosUSSpace, f3ViewRayUSSpace, f3LightDirUSSpace, false);

    float3 f3LocalX, f3LocalY, f3LocalZ;
    ConstructLocalFrameXYZ(-normalize(f3PosUSSpace), f3LightDirUSSpace, f3LocalX, f3LocalY, f3LocalZ);

    float fGatheredScattering = 0.0;
    float fTotalSolidAngle = 0.0;
    const float fNumZenithAngles = VOL_SCATTERING_IN_PARTICLE_LUT_DIM.z;
    const float fNumAzimuthAngles = VOL_SCATTERING_IN_PARTICLE_LUT_DIM.y;
    const float fZenithSpan = PI;
    const float fAzimuthSpan = 2.0*PI;
    for(float ZenithAngleNum = 0.5; ZenithAngleNum < fNumZenithAngles; ++ZenithAngleNum)
        for(float AzimuthAngleNum = 0.5; AzimuthAngleNum < fNumAzimuthAngles; ++AzimuthAngleNum)
        {
            float ZenithAngle = ZenithAngleNum/fNumZenithAngles * fZenithSpan;
            float AzimuthAngle = (AzimuthAngleNum/fNumAzimuthAngles - 0.5) * fAzimuthSpan;
            float3 f3CurrDir = GetDirectionInLocalFrameXYZ(f3LocalX, f3LocalY, f3LocalZ, ZenithAngle, AzimuthAngle);
            float4 f4CurrDirLUTCoords = WorldParamsToParticleScatteringLUT(f3PosUSSpace, f3CurrDir, f3LightDirUSSpace, false);
            float fCurrDirScattering = 0;
            SAMPLE_4D_LUT(g_tex3DPrevSctrOrder, VOL_SCATTERING_IN_PARTICLE_LUT_DIM, f4CurrDirLUTCoords, 0, fCurrDirScattering);
            if( g_GlobalCloudAttribs.f4Parameter.w == 1.0 )
            {
                fCurrDirScattering *= HGPhaseFunc( dot(-f3CurrDir, f3LightDirUSSpace) );
            }
            fCurrDirScattering *= HGPhaseFunc( dot(f3CurrDir, f3ViewRayUSSpace), 0.7 );

            float fdZenithAngle = fZenithSpan / fNumZenithAngles;
            float fdAzimuthAngle = fAzimuthSpan / fNumAzimuthAngles * sin(ZenithAngle);
            float fDiffSolidAngle = fdZenithAngle * fdAzimuthAngle;
            fTotalSolidAngle += fDiffSolidAngle;
            fGatheredScattering += fCurrDirScattering * fDiffSolidAngle;
        }

    // Total solid angle should be 4*PI. Renormalize to fix discretization issues
    fGatheredScattering *= 4.0*PI / fTotalSolidAngle;

    Out_fColor = fGatheredScattering;
}