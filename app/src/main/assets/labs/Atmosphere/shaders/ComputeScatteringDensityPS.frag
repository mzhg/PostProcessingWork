#include "AtmosphereCommon.glsl"

layout(location = 0) out vec3 scattering_density;

uniform int scattering_order;
uniform int layer;

vec4 GetScatteringTextureUvwzFromRMuMuSNu(in AtmosphereParameters atmosphere, float r, float mu, float mu_s, float nu, bool ray_r_mu_intersects_ground)
{
    assert(r >= atmosphere.bottom_radius && r <= atmosphere.top_radius);
    assert(mu >= -1.0 && mu <= 1.0);
    assert(mu_s >= -1.0 && mu_s <= 1.0);
    assert(nu >= -1.0 && nu <= 1.0);

    float H = sqrt(atmosphere.top_radius * atmosphere.top_radius - atmosphere.bottom_radius * atmosphere.bottom_radius);
    float rho = SafeSqrt(r * r - atmosphere.bottom_radius * atmosphere.bottom_radius);
    float u_r = GetTextureCoordFromUnitRange(rho / H, SCATTERING_TEXTURE_R_SIZE);
    float r_mu = r * mu;
    float discriminant = r_mu * r_mu - r * r + atmosphere.bottom_radius * atmosphere.bottom_radius;
    float u_mu;
    if (ray_r_mu_intersects_ground) {
        float d = -r_mu - SafeSqrt(discriminant);
        float d_min = r - atmosphere.bottom_radius;
        float d_max = rho;
        u_mu = 0.5 - 0.5 * GetTextureCoordFromUnitRange(d_max == d_min ? 0.0 : (d - d_min) / (d_max - d_min), SCATTERING_TEXTURE_MU_SIZE / 2);
    } else {
        float d = -r_mu + SafeSqrt(discriminant + H * H);
        float d_min = atmosphere.top_radius - r;
        float d_max = rho + H;
        u_mu = 0.5 + 0.5 * GetTextureCoordFromUnitRange((d - d_min) / (d_max - d_min), SCATTERING_TEXTURE_MU_SIZE / 2);
    }
    float d = DistanceToTopAtmosphereBoundary(atmosphere, atmosphere.bottom_radius, mu_s);
    float d_min = atmosphere.top_radius - atmosphere.bottom_radius;
    float d_max = H;
    float a = (d - d_min) / (d_max - d_min);
    float A = -2.0 * atmosphere.mu_s_min * atmosphere.bottom_radius / (d_max - d_min);
    float u_mu_s = GetTextureCoordFromUnitRange(max(1.0 - a / A, 0.0) / (1.0 + a), SCATTERING_TEXTURE_MU_S_SIZE);
    float u_nu = (nu + 1.0) / 2.0;
    return vec4(u_nu, u_mu_s, u_mu, u_r);
}

vec3 ComputeScatteringDensity( in AtmosphereParameters atmosphere, float r, float mu, float mu_s, float nu, int scattering_order)
{
    assert(r >= atmosphere.bottom_radius && r <= atmosphere.top_radius);
    assert(mu >= -1.0 && mu <= 1.0);
    assert(mu_s >= -1.0 && mu_s <= 1.0);
    assert(nu >= -1.0 && nu <= 1.0);
    assert(scattering_order >= 2);

    vec3 zenith_direction = vec3(0.0, 0.0, 1.0);
    vec3 omega = vec3(sqrt(1.0 - mu * mu), 0.0, mu);
    float sun_dir_x = omega.x == 0.0 ? 0.0 : (nu - mu * mu_s) / omega.x;
    float sun_dir_y = sqrt(max(1.0 - sun_dir_x * sun_dir_x - mu_s * mu_s, 0.0));
    vec3 omega_s = vec3(sun_dir_x, sun_dir_y, mu_s);
    const int SAMPLE_COUNT = 16;
    const float dphi = pi / float(SAMPLE_COUNT);
    const float dtheta = pi / float(SAMPLE_COUNT);
    vec3 rayleigh_mie =vec3(0.0 * watt_per_cubic_meter_per_sr_per_nm);
    for (int l = 0; l < SAMPLE_COUNT; ++l)
    {
        float theta = (float(l) + 0.5) * dtheta;
        float cos_theta = cos(theta);
        float sin_theta = sin(theta);
        bool ray_r_theta_intersects_ground = RayIntersectsGround(atmosphere, r, cos_theta);
        float distance_to_ground = 0.0 * m;
        vec3 transmittance_to_ground = vec3(0.0);
        vec3 ground_albedo = vec3(0.0);
        if (ray_r_theta_intersects_ground)
        {
            distance_to_ground = DistanceToBottomAtmosphereBoundary(atmosphere, r, cos_theta);
            transmittance_to_ground = GetTransmittance(atmosphere, transmittance_texture, r, cos_theta, distance_to_ground, true /* ray_intersects_ground */);
            ground_albedo = atmosphere.ground_albedo;
        }

        for (int m = 0; m < 2 * SAMPLE_COUNT; ++m)
        {
            float phi = (float(m) + 0.5) * dphi;
            vec3 omega_i = vec3(cos(phi) * sin_theta, sin(phi) * sin_theta, cos_theta);
            float domega_i = (dtheta / rad) * (dphi / rad) * sin(theta) * sr;
            float nu1 = dot(omega_s, omega_i);
            vec3 incident_radiance = GetScattering(atmosphere, single_rayleigh_scattering_texture, single_mie_scattering_texture, multiple_scattering_texture, r, omega_i.z, mu_s, nu1,
            ray_r_theta_intersects_ground, scattering_order - 1);
            vec3 ground_normal = normalize(zenith_direction * r + omega_i * distance_to_ground);
            vec3 ground_irradiance = GetIrradiance(atmosphere, irradiance_texture, atmosphere.bottom_radius, dot(ground_normal, omega_s));
            incident_radiance += transmittance_to_ground * ground_albedo * (1.0 / (PI * sr)) * ground_irradiance;
            float nu2 = dot(omega, omega_i);
            float rayleigh_density = GetProfileDensity(atmosphere.rayleigh_density, r - atmosphere.bottom_radius);
            float mie_density = GetProfileDensity(atmosphere.mie_density, r - atmosphere.bottom_radius);

            rayleigh_mie += incident_radiance * (atmosphere.rayleigh_scattering * rayleigh_density *RayleighPhaseFunction(nu2) +
            atmosphere.mie_scattering * mie_density *MiePhaseFunction(atmosphere.mie_phase_function_g, nu2)) *domega_i;
        }
    }
    return rayleigh_mie;
}

vec3 ComputeScatteringDensityTexture(in AtmosphereParameters atmosphere, in vec3 frag_coord, int scattering_order)
{
    float r;
    float mu;
    float mu_s;
    float nu;
    bool ray_r_mu_intersects_ground;
    GetRMuMuSNuFromScatteringTextureFragCoord(atmosphere, frag_coord, r, mu, mu_s, nu, ray_r_mu_intersects_ground);
    return ComputeScatteringDensity(atmosphere, transmittance_texture, single_rayleigh_scattering_texture, single_mie_scattering_texture,
    multiple_scattering_texture, irradiance_texture, r, mu, mu_s, nu, scattering_order);
}

void main()
{
    scattering_density = ComputeScatteringDensityTexture( ATMOSPHERE, vec3(gl_FragCoord.xy, layer + 0.5), scattering_order);
}