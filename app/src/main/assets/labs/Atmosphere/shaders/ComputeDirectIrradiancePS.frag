#include "AtmosphereCommon.glsl"

layout(location = 0) out vec3 delta_irradiance;
layout(location = 1) out vec3 irradiance;

const vec2 IRRADIANCE_TEXTURE_SIZE = vec2(IRRADIANCE_TEXTURE_WIDTH, IRRADIANCE_TEXTURE_HEIGHT);

vec3 GetTransmittanceToTopAtmosphereBoundary(in AtmosphereParameters atmosphere, float r, float mu)
{
    assert(r >= atmosphere.bottom_radius && r <= atmosphere.top_radius);
    vec2 uv = GetTransmittanceTextureUvFromRMu(atmosphere, r, mu);
    return texture(transmittance_texture, uv).rgb;
}

vec3 ComputeDirectIrradiance(in AtmosphereParameters atmosphere, float r, float mu_s)
{
    assert(r >= atmosphere.bottom_radius && r <= atmosphere.top_radius);
    assert(mu_s >= -1.0 && mu_s <= 1.0);
    float alpha_s = atmosphere.sun_angular_radius / rad;
    float average_cosine_factor = mu_s < -alpha_s ? 0.0 : (mu_s > alpha_s ? mu_s : (mu_s + alpha_s) * (mu_s + alpha_s) / (4.0 * alpha_s));
    return atmosphere.solar_irradiance * GetTransmittanceToTopAtmosphereBoundary(atmosphere, transmittance_texture, r, mu_s) * average_cosine_factor;
}

vec3 ComputeDirectIrradianceTexture(in AtmosphereParameters atmosphere, in vec2 frag_coord)
{
    float r;
    float mu_s;
    GetRMuSFromIrradianceTextureUv(atmosphere, frag_coord / IRRADIANCE_TEXTURE_SIZE, r, mu_s);
    return ComputeDirectIrradiance(atmosphere, transmittance_texture, r, mu_s);
}

void main()
{
    delta_irradiance = ComputeDirectIrradianceTexture(ATMOSPHERE, transmittance_texture, gl_FragCoord.xy);
    irradiance = vec3(0.0);
}