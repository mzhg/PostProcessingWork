#include "AtmosphereCommon.ush"
#include "AtmospherePrecomputeCommon.ush"

in vec2 OutTexCoord;
layout(location = 0) out float4 OutColor0;
layout(location = 1) out float4 OutColor1;

void Inscatter(float Radius, float Mu, float MuS, float Nu, out float3 Ray, out float3 Mie) // For Inscatter 1
{
    Ray = float3(0, 0, 0);
    Mie = float3(0, 0, 0);
    float Dx = Limit(Radius, Mu) / float(InscatterIntegralSamples);
    float Xi = 0.0;
    float3 Rayi;
    float3 Miei;
    Integrand(Radius, Mu, MuS, Nu, 0.0, Rayi, Miei);
    for (int I = 1; I <= InscatterIntegralSamples; ++I)
	{
        float Xj = float(I) * Dx;
        float3 Rayj;
        float3 Miej;
        Integrand(Radius, Mu, MuS, Nu, Xj, Rayj, Miej);
        Ray += (Rayi + Rayj) / 2.0 * Dx;
        Mie += (Miei + Miej) / 2.0 * Dx;
        Xi = Xj;
        Rayi = Rayj;
        Miei = Miej;
    }
    Ray *= BetaRayleighScattering;
    Mie *= BetaMieScattering;
}

void main()
{
    float3 Ray;
    float3 Mie;
    float Mu, MuS, Nu;
    GetMuMuSNu(Input.Vertex.OutTexCoord, AtmosphereR, DhdH, Mu, MuS, Nu);
    Inscatter(AtmosphereR, Mu, MuS, Nu, Ray, Mie);
    // Store separately Rayleigh and Mie contributions, WITHOUT the phase function factor
	// (cf "Angular precision")
    OutColor0 = float4(Ray,1);
    OutColor1 = float4(Mie,1);
}