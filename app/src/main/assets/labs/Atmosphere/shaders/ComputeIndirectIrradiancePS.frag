#include "AtmosphereCommon.glsl"

layout(location = 0) out vec3 delta_irradiance;
layout(location = 1) out vec3 irradiance;
uniform mat3 luminance_from_radiance;

uniform int scattering_order;

layout(binding = 0) uniform sampler3D single_rayleigh_scattering_texture;
layout(binding = 1) uniform sampler3D single_mie_scattering_texture;
layout(binding = 2) uniform sampler3D multiple_scattering_texture;

vec3 ComputeIndirectIrradiance( in AtmosphereParameters atmosphere, float r, float mu_s, int scattering_order)
{
    assert(r >= atmosphere.bottom_radius && r <= atmosphere.top_radius);
    assert(mu_s >= -1.0 && mu_s <= 1.0);
    assert(scattering_order >= 1);

    const int SAMPLE_COUNT = 32;
    const float dphi = pi / float(SAMPLE_COUNT);
    const float dtheta = pi / float(SAMPLE_COUNT);
    vec3 result = vec3(0.0 * watt_per_square_meter_per_nm);
    vec3 omega_s = vec3(sqrt(1.0 - mu_s * mu_s), 0.0, mu_s);
    for (int j = 0; j < SAMPLE_COUNT / 2; ++j)
    {
        float theta = (float(j) + 0.5) * dtheta;
        for (int i = 0; i < 2 * SAMPLE_COUNT; ++i)
        {
            float phi = (float(i) + 0.5) * dphi;
            vec3 omega =
            vec3(cos(phi) * sin(theta), sin(phi) * sin(theta), cos(theta));
            float domega = (dtheta / rad) * (dphi / rad) * sin(theta) * sr;
            float nu = dot(omega, omega_s);
            result += GetScattering(atmosphere, single_rayleigh_scattering_texture,
            single_mie_scattering_texture, multiple_scattering_texture,
            r, omega.z, mu_s, nu, false /* ray_r_theta_intersects_ground */,
            scattering_order) *
            omega.z * domega;
        }
    }
    return result;
}

vec3 ComputeIndirectIrradianceTexture( in AtmosphereParameters atmosphere, in vec2 frag_coord, int scattering_order)
{
    float r;
    float mu_s;
    GetRMuSFromIrradianceTextureUv(atmosphere, frag_coord / IRRADIANCE_TEXTURE_SIZE, r, mu_s);
    return ComputeIndirectIrradiance(atmosphere,r, mu_s, scattering_order);
}

void main()
{
    delta_irradiance = ComputeIndirectIrradianceTexture(ATMOSPHERE, gl_FragCoord.xy, scattering_order);

    irradiance = luminance_from_radiance * delta_irradiance;
}