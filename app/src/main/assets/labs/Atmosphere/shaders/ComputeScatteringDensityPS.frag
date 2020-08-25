#include "AtmosphereCommon.glsl"

layout(location = 0) out vec3 scattering_density;

uniform int scattering_order;

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
            transmittance_to_ground = GetTransmittance(atmosphere, r, cos_theta, distance_to_ground, true /* ray_intersects_ground */);
            ground_albedo = atmosphere.ground_albedo;
        }

        for (int m = 0; m < 2 * SAMPLE_COUNT; ++m)
        {
            float phi = (float(m) + 0.5) * dphi;
            vec3 omega_i = vec3(cos(phi) * sin_theta, sin(phi) * sin_theta, cos_theta);
            float domega_i = (dtheta / rad) * (dphi / rad) * sin(theta) * sr;
            float nu1 = dot(omega_s, omega_i);
            vec3 incident_radiance = GetScattering(atmosphere, r, omega_i.z, mu_s, nu1, ray_r_theta_intersects_ground, scattering_order - 1);
            vec3 ground_normal = normalize(zenith_direction * r + omega_i * distance_to_ground);
            vec3 ground_irradiance = GetIrradiance(atmosphere, atmosphere.bottom_radius, dot(ground_normal, omega_s));
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
    return ComputeScatteringDensity(atmosphere, r, mu, mu_s, nu, scattering_order);
}

void main()
{
    scattering_density = ComputeScatteringDensityTexture( ATMOSPHERE, vec3(gl_FragCoord.xy, gl_Layer + 0.5), scattering_order);
}