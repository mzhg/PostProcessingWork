#include "AtmosphereCommon.glsl"

layout(location = 0) out vec3 transmittance;

float ComputeOpticalLengthToTopAtmosphereBoundary(in AtmosphereParameters atmosphere, in DensityProfile profile, float r, float mu)
{
    assert(r >= atmosphere.bottom_radius && r <= atmosphere.top_radius);
    assert(mu >= -1.0 && mu <= 1.0);
    const int SAMPLE_COUNT = 500;
    float dx = DistanceToTopAtmosphereBoundary(atmosphere, r, mu) / float(SAMPLE_COUNT);
    float result = 0.0;
    for (int i = 0; i <= SAMPLE_COUNT; ++i)
    {
        float d_i = float(i) * dx;
        float r_i = sqrt(d_i * d_i + 2.0 * r * mu * d_i + r * r);
        float y_i = GetProfileDensity(profile, r_i - atmosphere.bottom_radius);
        float weight_i = i == 0 || i == SAMPLE_COUNT ? 0.5 : 1.0;
        result += y_i * weight_i * dx;
    }
    return result;
}

vec3 ComputeTransmittanceToTopAtmosphereBoundary(in AtmosphereParameters atmosphere, float r, float mu) {
    assert(r >= atmosphere.bottom_radius && r <= atmosphere.top_radius);
    assert(mu >= -1.0 && mu <= 1.0);
    return exp(-(atmosphere.rayleigh_scattering *ComputeOpticalLengthToTopAtmosphereBoundary(
    atmosphere, atmosphere.rayleigh_density, r, mu) +
    atmosphere.mie_extinction *
    ComputeOpticalLengthToTopAtmosphereBoundary(
    atmosphere, atmosphere.mie_density, r, mu) +
    atmosphere.absorption_extinction *
    ComputeOpticalLengthToTopAtmosphereBoundary(
    atmosphere, atmosphere.absorption_density, r, mu)));
}

vec3 ComputeTransmittanceToTopAtmosphereBoundaryTexture(in AtmosphereParameters atmosphere, in vec2 frag_coord)
{
    const vec2 TRANSMITTANCE_TEXTURE_SIZE =
    vec2(TRANSMITTANCE_TEXTURE_WIDTH, TRANSMITTANCE_TEXTURE_HEIGHT);
    float r;
    float mu;  // cos(viewZenthAngle)

    GetRMuFromTransmittanceTextureUv(atmosphere, frag_coord / TRANSMITTANCE_TEXTURE_SIZE, r, mu);

    return ComputeTransmittanceToTopAtmosphereBoundary(atmosphere, r, mu);
}

void main()
{
    transmittance = ComputeTransmittanceToTopAtmosphereBoundaryTexture(
    ATMOSPHERE, gl_FragCoord.xy);
}