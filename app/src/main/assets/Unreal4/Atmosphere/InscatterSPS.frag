#include "AtmosphereCommon.ush"
#include "AtmospherePrecomputeCommon.ush"

in vec2 OutTexCoord;
layout(location = 0) out float4 OutColor;

void Inscatter(float Radius, float Mu, float MuS, float Nu, out float3 RayMie) // InscatterS
{
    Radius = clamp(Radius, RadiusGround, RadiusAtmosphere);
    Mu = clamp(Mu, -1.0, 1.0);
    MuS = clamp(MuS, -1.0, 1.0);
    float Variation = sqrt(1.0 - Mu * Mu) * sqrt(1.0 - MuS * MuS);
    Nu = clamp(Nu, MuS * Mu - Variation, MuS * Mu + Variation);

    float CThetaMin = -sqrt(1.0 - (RadiusGround / Radius) * (RadiusGround / Radius));

    float3 V = float3(sqrt(1.0 - Mu * Mu), 0.0, Mu);
    float Sx = V.x == 0.0 ? 0.0 : (Nu - MuS * Mu) / V.x;
    float3 S = float3(Sx, sqrt(max(0.0, 1.0 - Sx * Sx - MuS * MuS)), MuS);

    RayMie = float3(0.f, 0.f, 0.f);

    // Integral over 4.PI around x with two nested loops over W directions (Theta,Phi) - Eq (7)
    for (int ITheta = 0; ITheta < InscatterSphericalIntegralSamples; ++ITheta)
	{
        float Theta = (float(ITheta) + 0.5) * DeltaTheta;
        float CTheta = cos(Theta);

        float GReflectance = 0.0;
        float DGround = 0.0;
        float3 GTransmittance = float3(0.f, 0.f, 0.f);
        if (CTheta < CThetaMin)
		{
			// If ground visible in direction W, Compute transparency GTransmittance between x and ground
            GReflectance = AverageGroundRelectance / PI;
            DGround = -Radius * CTheta - sqrt(Radius * Radius * (CTheta * CTheta - 1.0) + RadiusGround * RadiusGround);
            GTransmittance = TransmittanceWithDistance(RadiusGround, -(Radius * CTheta + DGround) / RadiusGround, DGround);
        }

        for (int IPhi = 0; IPhi < 2 * InscatterSphericalIntegralSamples; ++IPhi)
		{
            float Phi = (float(IPhi) + 0.5) * DeltaPhi;
            float Dw = DeltaTheta * DeltaPhi * sin(Theta);
            float3 W = float3(cos(Phi) * sin(Theta), sin(Phi) * sin(Theta), CTheta);

            float Nu1 = dot(S, W);
            float Nu2 = dot(V, W);
            float Pr2 = PhaseFunctionR(Nu2);
            float Pm2 = PhaseFunctionM(Nu2);

            // Compute irradiance received at ground in direction W (if ground visible) =deltaE
            float3 GNormal = (float3(0.0, 0.0, Radius) + DGround * W) / RadiusGround;
            float3 GIrradiance = Irradiance(AtmosphereDeltaETexture, AtmosphereDeltaETextureSampler, RadiusGround, dot(GNormal, S));

            float3 RayMie1; // light arriving at x from direction W

            // First term = light reflected from the ground and attenuated before reaching x, =T.alpha/PI.deltaE
            RayMie1 = GReflectance * GIrradiance * GTransmittance;

            // Second term = inscattered light, =deltaS
            if (FirstOrder == 1.0)
			{
                // First iteration is special because Rayleigh and Mie were stored separately,
                // without the phase functions factors; they must be reintroduced here
                float Pr1 = PhaseFunctionR(Nu1);
                float Pm1 = PhaseFunctionM(Nu1);
                float3 Ray1 = Texture4DSample(AtmosphereDeltaSRTexture, AtmosphereDeltaSRTextureSampler, Radius, W.z, MuS, Nu1).rgb;
                float3 Mie1 = Texture4DSample(AtmosphereDeltaSMTexture, AtmosphereDeltaSMTextureSampler, Radius, W.z, MuS, Nu1).rgb;
                RayMie1 += Ray1 * Pr1 + Mie1 * Pm1;
            }
			else
			{
                RayMie1 += Texture4DSample(AtmosphereDeltaSRTexture, AtmosphereDeltaSRTextureSampler, Radius, W.z, MuS, Nu1).rgb;
            }

            // Light coming from direction W and scattered in direction V
            // = light arriving at x from direction W (RayMie1) * SUM(scattering coefficient * phaseFunction) - Eq (7)
            RayMie += RayMie1 * (BetaRayleighScattering * exp(-(Radius - RadiusGround) / View.AtmosphericFogHeightScaleRayleigh) * Pr2 + BetaMieScattering * exp(-(Radius - RadiusGround) / HeightScaleMie) * Pm2) * Dw;
        }
    }

    // output RayMie = J[T.alpha/PI.deltaE + deltaS] (line 7 in algorithm 4.1)
}

void main()
{
    float3 RayMie;
    float Mu, MuS, Nu;
    GetMuMuSNu(Input.Vertex.OutTexCoord, AtmosphereR, DhdH, Mu, MuS, Nu);
    Inscatter(AtmosphereR, Mu, MuS, Nu, RayMie);
    OutColor = float4(RayMie, 1);
}