#include "AtmosphereCommon.ush"
#include "AtmospherePrecomputeCommon.ush"

in vec2 OutTexCoord;
layout(location = 0) out float4 OutColor;

float3 Integrand(float Radius, float Mu, float MuS, float Nu, float T)
{
    float Ri = sqrt(Radius * Radius + T * T + 2.0 * Radius * Mu * T);
    float Mui = (Radius * Mu + T) / Ri;
    float MuSi = (Nu * T + MuS * Radius) / Ri;
    return Texture4DSample(AtmosphereDeltaJTexture, /*AtmosphereDeltaJTextureSampler,*/ Ri, Mui, MuSi, Nu).rgb * TransmittanceWithDistance(Radius, Mu, T);
}

float3 Inscatter(float Radius, float Mu, float MuS, float Nu) // InscatterN
{
    float3 RayMie = float3(0.f, 0.f, 0.f);
    float Dx = Limit(Radius, Mu) / float(InscatterIntegralSamples);
    float Xi = 0.0;
    float3 RayMiei = Integrand(Radius, Mu, MuS, Nu, 0.0);
    for (int I = 1; I <= InscatterIntegralSamples; ++I)
	{
        float Xj = float(I) * Dx;
        float3 RayMiej = Integrand(Radius, Mu, MuS, Nu, Xj);
        RayMie += (RayMiei + RayMiej) / 2.0 * Dx;
        Xi = Xj;
        RayMiei = RayMiej;
    }
    return RayMie;
}

void main()
{
    float3 Ray;
    float Mu, MuS, Nu;
    GetMuMuSNu(Input.Vertex.OutTexCoord, AtmosphereR, DhdH, Mu, MuS, Nu);
    Ray = Inscatter(AtmosphereR, Mu, MuS, Nu);
    OutColor = float4(Inscatter(AtmosphereR, Mu, MuS, Nu), 1);
}