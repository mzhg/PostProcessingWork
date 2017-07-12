#include "Preprocessing.glsl"

in float4 m_f4UVAndScreenPos;

layout(location = 0) out float Out_fSingleSctr;
layout(location = 1) out float Out_fMultipleSctr;

void main()
{
    // Scattering on the surface of the sphere is stored in the last 4D-slice
    float4 f4LUTCoords = float4(/*ProjToUV(In.m_f2PosPS)*/m_f4UVAndScreenPos.xy, g_GlobalCloudAttribs.f4Parameter.x, 1 - 0.5/VOL_SCATTERING_IN_PARTICLE_LUT_DIM.w);
    // We only need directions into the sphere
    f4LUTCoords.z = (f4LUTCoords.z - 0.5/SRF_SCATTERING_IN_PARTICLE_LUT_DIM.z)/2.f + 0.5/VOL_SCATTERING_IN_PARTICLE_LUT_DIM.z;

    SAMPLE_4D_LUT(g_tex3DSingleScattering,   VOL_SCATTERING_IN_PARTICLE_LUT_DIM, f4LUTCoords, 0, Out_fSingleSctr);
    SAMPLE_4D_LUT(g_tex3DMultipleScattering, VOL_SCATTERING_IN_PARTICLE_LUT_DIM, f4LUTCoords, 0, Out_fMultipleSctr);
}