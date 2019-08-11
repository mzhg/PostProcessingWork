#include "AtmosphereCommon.ush"
#include "AtmospherePrecomputeCommon.ush"

in vec4 m_f4UVAndScreenPos;

out float OutColor;

void main()
{
    float Radius, MuS;
    GetIrradianceRMuS(Input.OutTexCoord, Radius, MuS);
    float3 S = float3(sqrt(max(1.0 - MuS * MuS, 0.0)), 0.0, MuS);

    float3 Result = float3(0.f, 0.f, 0.f);
    // integral over 2.PI around x with two nested loops over W directions (Theta,Phi) -- Eq (15)
    for (int IPhi = 0; IPhi < 4 * IrradianceIntegralSamplesHalf; ++IPhi)
    {
        float Phi = (float(IPhi) + 0.5) * DeltaPhi;
        for (int ITheta = 0; ITheta < IrradianceIntegralSamplesHalf; ++ITheta)
        {
            float Theta = (float(ITheta) + 0.5) * DeltaTheta;
            float Dw = DeltaTheta * DeltaPhi * sin(Theta);
            float3 W = float3(cos(Phi) * sin(Theta), sin(Phi) * sin(Theta), cos(Theta));
            float Nu = dot(S, W);
            if (FirstOrder == 1.0)
            {
                // first iteration is special because Rayleigh and Mie were stored separately,
                // without the phase functions factors; they must be reintroduced here
                float Pr1 = PhaseFunctionR(Nu);
                float Pm1 = PhaseFunctionM(Nu);
                float3 Ray1 = Texture4DSample(AtmosphereDeltaSRTexture, AtmosphereDeltaSRTextureSampler, Radius, W.z, MuS, Nu).rgb;
                float3 Mie1 = Texture4DSample(AtmosphereDeltaSMTexture, AtmosphereDeltaSMTextureSampler, Radius, W.z, MuS, Nu).rgb;
                Result += (Ray1 * Pr1 + Mie1 * Pm1) * W.z * Dw;
            }
            else
            {
                Result += Texture4DSample(AtmosphereDeltaSRTexture, AtmosphereDeltaSRTextureSampler, Radius, W.z, MuS, Nu).rgb * W.z * Dw;
            }
        }
    }

    OutColor = float4(Result, 0.0);
}