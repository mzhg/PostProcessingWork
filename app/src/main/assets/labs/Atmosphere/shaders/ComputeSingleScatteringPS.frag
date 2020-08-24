#include "AtmosphereCommon.glsl"

layout(location = 0) out vec3 delta_rayleigh;
layout(location = 1) out vec3 delta_mie;
layout(location = 2) out vec4 scattering;
layout(location = 3) out vec3 single_mie_scattering;

uniform mat3 luminance_from_radiance;
uniform int layer;

layout(binding = 0) uniform sampler2D transmittance_texture;

vec3 GetTransmittanceToTopAtmosphereBoundary(in AtmosphereParameters atmosphere, Length r, Number mu)
{
    assert(r >= atmosphere.bottom_radius && r <= atmosphere.top_radius);
    vec2 uv = GetTransmittanceTextureUvFromRMu(atmosphere, r, mu);
    return vec3(texture(transmittance_texture, uv));
}

vec3 GetTransmittance( in AtmosphereParameters atmosphere, float r, float mu, float d, bool ray_r_mu_intersects_ground)
{
    assert(r >= atmosphere.bottom_radius && r <= atmosphere.top_radius);
    assert(mu >= -1.0 && mu <= 1.0);
    assert(d >= 0.0);

    float r_d = ClampRadius(atmosphere, sqrt(d * d + 2.0 * r * mu * d + r * r));
    float mu_d = ClampCosine((r * mu + d) / r_d);
    if (ray_r_mu_intersects_ground)
    {
        return min(GetTransmittanceToTopAtmosphereBoundary(atmosphere, transmittance_texture, r_d, -mu_d) /
        GetTransmittanceToTopAtmosphereBoundary(atmosphere, transmittance_texture, r, -mu), vec3(1.0));
    } else {
        return min(GetTransmittanceToTopAtmosphereBoundary(atmosphere, transmittance_texture, r, mu) /
        GetTransmittanceToTopAtmosphereBoundary(atmosphere, transmittance_texture, r_d, mu_d), vec3(1.0));
    }
}

vec3 GetTransmittanceToSun( in AtmosphereParameters atmosphere, float r, float mu_s)
{
    float sin_theta_h = atmosphere.bottom_radius / r;
    float cos_theta_h = -sqrt(max(1.0 - sin_theta_h * sin_theta_h, 0.0));
    return GetTransmittanceToTopAtmosphereBoundary(atmosphere, r, mu_s) * smoothstep(-sin_theta_h * atmosphere.sun_angular_radius / rad, sin_theta_h * atmosphere.sun_angular_radius / rad, mu_s - cos_theta_h);
}

void ComputeSingleScatteringIntegrand(in AtmosphereParameters atmosphere, float r, float mu, float mu_s, float nu, float d, bool ray_r_mu_intersects_ground,
out vec3 rayleigh, out vec3 mie)
{
    float r_d = ClampRadius(atmosphere, sqrt(d * d + 2.0 * r * mu * d + r * r));
    float mu_s_d = ClampCosine((r * mu_s + d * nu) / r_d);
    vec3 transmittance = GetTransmittance(atmosphere, r, mu, d, ray_r_mu_intersects_ground) * GetTransmittanceToSun(atmosphere, r_d, mu_s_d);
    rayleigh = transmittance * GetProfileDensity(atmosphere.rayleigh_density, r_d - atmosphere.bottom_radius);
    mie = transmittance * GetProfileDensity(atmosphere.mie_density, r_d - atmosphere.bottom_radius);
}

void ComputeSingleScattering( in AtmosphereParameters atmosphere, float r, float mu, float mu_s, float nu, bool ray_r_mu_intersects_ground, out vec3 rayleigh, out vec3 mie)
{
    assert(r >= atmosphere.bottom_radius && r <= atmosphere.top_radius);
    assert(mu >= -1.0 && mu <= 1.0);
    assert(mu_s >= -1.0 && mu_s <= 1.0);
    assert(nu >= -1.0 && nu <= 1.0);
    const int SAMPLE_COUNT = 50;
    float dx = DistanceToNearestAtmosphereBoundary(atmosphere, r, mu, ray_r_mu_intersects_ground) / float(SAMPLE_COUNT);
    vec3 rayleigh_sum = vec3(0.0);
    vec3 mie_sum = vec3(0.0);
    for (int i = 0; i <= SAMPLE_COUNT; ++i)
    {
        float d_i = float(i) * dx;
        vec3 rayleigh_i;
        vec3 mie_i;
        ComputeSingleScatteringIntegrand(atmosphere, r, mu, mu_s, nu, d_i, ray_r_mu_intersects_ground, rayleigh_i, mie_i);
        float weight_i = (i == 0 || i == SAMPLE_COUNT) ? 0.5 : 1.0;
        rayleigh_sum += rayleigh_i * weight_i;
        mie_sum += mie_i * weight_i;
    }
    rayleigh = rayleigh_sum * dx * atmosphere.solar_irradiance *
    atmosphere.rayleigh_scattering;
    mie = mie_sum * dx * atmosphere.solar_irradiance * atmosphere.mie_scattering;
}

void ComputeSingleScatteringTexture(in AtmosphereParameters atmosphere,in vec3 frag_coord, out vec3 rayleigh, out vec3 mie)
{
    float r;
    float mu;
    float mu_s;
    float nu;
    bool ray_r_mu_intersects_ground;
    GetRMuMuSNuFromScatteringTextureFragCoord(atmosphere, frag_coord, r, mu, mu_s, nu, ray_r_mu_intersects_ground);
    ComputeSingleScattering(atmosphere, r, mu, mu_s, nu, ray_r_mu_intersects_ground, rayleigh, mie);
}

void main()
{
    ComputeSingleScatteringTexture(ATMOSPHERE, vec3(gl_FragCoord.xy, layer + 0.5), delta_rayleigh, delta_mie);
    scattering = vec4(luminance_from_radiance * delta_rayleigh.rgb, (luminance_from_radiance * delta_mie).r);
    single_mie_scattering = luminance_from_radiance * delta_mie;
}