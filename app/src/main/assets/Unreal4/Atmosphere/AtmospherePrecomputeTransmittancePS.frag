// Disable USING_VERTEX_SHADER_LAYER as it would be an error for these shaders as they are intended to render to 2D targets only.
//#if USING_VERTEX_SHADER_LAYER != 0
//	#undef USING_VERTEX_SHADER_LAYER
//	#define USING_VERTEX_SHADER_LAYER 0
//#endif

#include "AtmosphereCommon.ush"
#include "AtmospherePrecomputeCommon.ush"

in vec4 m_f4UVAndScreenPos;

out float OutColor;

float OpticalDepth(float H, float Radius, float Mu)
{
    float Result = 0.0;
    float Dx = Limit(Radius, Mu) / float(TransmittanceIntegralSamples);
    float Xi = 0.0;
    float Yi = exp(-(Radius - RadiusGround) / H);
    for (int I = 1; I <= TransmittanceIntegralSamples; ++I)
	{
        float Xj = float(I) * Dx;
        float Yj = exp(-(sqrt(Radius * Radius + Xj * Xj + 2.0 * Xj * Radius * Mu) - RadiusGround) / H);
        Result += (Yi + Yj) / 2.0 * Dx;
        Xi = Xj;
        Yi = Yj;
    }
    return Mu < -sqrt(1.0 - (RadiusGround / Radius) * (RadiusGround / Radius)) ? 1e9 : Result;
}

void main()
{
    // RETURN_COLOR not needed unless writing to SceneColor
    float Radius, MuS;
    GetTransmittanceRMuS(Input.OutTexCoord, Radius, MuS);
    float3 Depth = BetaRayleighScattering * OpticalDepth(View.AtmosphericFogHeightScaleRayleigh, Radius, MuS) + BetaMieExtinction * OpticalDepth(HeightScaleMie, Radius, MuS);
    OutColor = float4(exp(-Depth), 0.f); // Eq (5)
}