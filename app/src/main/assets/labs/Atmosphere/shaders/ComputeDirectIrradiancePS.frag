#include "AtmosphereCommon.glsl"

layout(location = 0) out vec3 delta_irradiance;
layout(location = 1) out vec3 irradiance;

const vec2 IRRADIANCE_TEXTURE_SIZE = vec2(IRRADIANCE_TEXTURE_WIDTH, IRRADIANCE_TEXTURE_HEIGHT);

vec3 ComputeDirectIrradiance(in AtmosphereParameters atmosphere, float r, float mu_s)
{
    assert(r >= atmosphere.bottom_radius && r <= atmosphere.top_radius);
    assert(mu_s >= -1.0 && mu_s <= 1.0);
    float alpha_s = atmosphere.sun_angular_radius / rad;
    float average_cosine_factor = mu_s < -alpha_s ? 0.0 : (mu_s > alpha_s ? mu_s : (mu_s + alpha_s) * (mu_s + alpha_s) / (4.0 * alpha_s));
    return atmosphere.solar_irradiance * GetTransmittanceToTopAtmosphereBoundary(atmosphere, r, mu_s) * average_cosine_factor;
}

vec3 ComputeDirectIrradianceTexture(in AtmosphereParameters atmosphere, in vec2 frag_coord)
{
    float r;
    float mu_s;
    GetRMuSFromIrradianceTextureUv(atmosphere, frag_coord / IRRADIANCE_TEXTURE_SIZE, r, mu_s);
    return ComputeDirectIrradiance(atmosphere, r, mu_s);
}

void main()
{
    delta_irradiance = ComputeDirectIrradianceTexture(ATMOSPHERE, gl_FragCoord.xy);
    irradiance = vec3(0.0);
}