#include "AtmosphereCommon.glsl"

layout(location = 0) out vec3 delta_multiple_scattering;
layout(location = 1) out vec4 scattering;

uniform mat3 luminance_from_radiance;
uniform int layer;

vec3 ComputeMultipleScattering( in AtmosphereParameters atmosphere, float r, float mu, float mu_s, float nu, bool ray_r_mu_intersects_ground)
{
    assert(r >= atmosphere.bottom_radius && r <= atmosphere.top_radius);
    assert(mu >= -1.0 && mu <= 1.0);
    assert(mu_s >= -1.0 && mu_s <= 1.0);
    assert(nu >= -1.0 && nu <= 1.0);
    const int SAMPLE_COUNT = 50;

    float dx = DistanceToNearestAtmosphereBoundary( atmosphere, r, mu, ray_r_mu_intersects_ground) / float(SAMPLE_COUNT);
    vec3 rayleigh_mie_sum = vec3(0.0 * watt_per_square_meter_per_sr_per_nm);
    for (int i = 0; i <= SAMPLE_COUNT; ++i)
    {
        float d_i = float(i) * dx;
        float r_i = ClampRadius(atmosphere, sqrt(d_i * d_i + 2.0 * r * mu * d_i + r * r));
        float mu_i = ClampCosine((r * mu + d_i) / r_i);
        float mu_s_i = ClampCosine((r * mu_s + d_i * nu) / r_i);
        vec3 rayleigh_mie_i = GetScattering( atmosphere, scattering_density_texture, r_i, mu_i, mu_s_i, nu, ray_r_mu_intersects_ground)
        * GetTransmittance(atmosphere, r, mu, d_i, ray_r_mu_intersects_ground) * dx;
        float weight_i = (i == 0 || i == SAMPLE_COUNT) ? 0.5 : 1.0;
        rayleigh_mie_sum += rayleigh_mie_i * weight_i;
    }
    return rayleigh_mie_sum;
}

vec3 ComputeMultipleScatteringTexture( in AtmosphereParameters atmosphere, in vec3 frag_coord, out float nu)
{
    float r;
    float mu;
    float mu_s;
    bool ray_r_mu_intersects_ground;
    GetRMuMuSNuFromScatteringTextureFragCoord(atmosphere, frag_coord, r, mu, mu_s, nu, ray_r_mu_intersects_ground);

    return ComputeMultipleScattering(atmosphere, r, mu, mu_s, nu, ray_r_mu_intersects_ground);
}

void main()
{
    float nu;
    delta_multiple_scattering = ComputeMultipleScatteringTexture(ATMOSPHERE, vec3(gl_FragCoord.xy, layer + 0.5), nu);

    scattering = vec4(luminance_from_radiance * delta_multiple_scattering.rgb / RayleighPhaseFunction(nu), 0.0);
}