#include "AtmosphereCommon.glsl"


vec3 GetSkyRadiance( in AtmosphereParameters atmosphere, vec3 camera, vec3 view_ray, float shadow_length, vec3 sun_direction, out vec3 transmittance)
{
    float r = length(camera);
    float rmu = dot(camera, view_ray);
    float distance_to_top_atmosphere_boundary = -rmu - sqrt(rmu * rmu - r * r + atmosphere.top_radius * atmosphere.top_radius);
    if (distance_to_top_atmosphere_boundary > 0.0 * m)
    {
        camera = camera + view_ray * distance_to_top_atmosphere_boundary;
        r = atmosphere.top_radius;
        rmu += distance_to_top_atmosphere_boundary;
    } else if (r > atmosphere.top_radius)
    {
        transmittance = vec3(1.0);
        return vec3(0.0 * watt_per_square_meter_per_sr_per_nm);
    }

    float mu = rmu / r;
    float mu_s = dot(camera, sun_direction) / r;
    float nu = dot(view_ray, sun_direction);
    bool ray_r_mu_intersects_ground = RayIntersectsGround(atmosphere, r, mu);
    transmittance = ray_r_mu_intersects_ground ? vec3(0.0) : GetTransmittanceToTopAtmosphereBoundary(atmosphere, transmittance_texture, r, mu);
    vec3 single_mie_scattering;
    vec3 scattering;
    if (shadow_length == 0.0 * m)
    {
        scattering = GetCombinedScattering( atmosphere, r, mu, mu_s, nu, ray_r_mu_intersects_ground, single_mie_scattering);
    } else
    {
        float d = shadow_length;
        float r_p = ClampRadius(atmosphere, sqrt(d * d + 2.0 * r * mu * d + r * r));
        float mu_p = (r * mu + d) / r_p;
        float mu_s_p = (r * mu_s + d * nu) / r_p;
        scattering = GetCombinedScattering(atmosphere, r_p, mu_p, mu_s_p, nu, ray_r_mu_intersects_ground, single_mie_scattering);
        vec3 shadow_transmittance = GetTransmittance(atmosphere, r, mu, shadow_length, ray_r_mu_intersects_ground);
        scattering = scattering * shadow_transmittance;
        single_mie_scattering = single_mie_scattering * shadow_transmittance;
    }

    return scattering * RayleighPhaseFunction(nu) + single_mie_scattering * MiePhaseFunction(atmosphere.mie_phase_function_g, nu);
}

vec3 GetSkyRadianceToPoint( in AtmosphereParameters atmosphere, vec3 camera, vec3 point, float shadow_length, vec3 sun_direction, out vec3 transmittance)
{
    vec3 view_ray = normalize(point - camera);
    float r = length(camera);
    float rmu = dot(camera, view_ray);
    float distance_to_top_atmosphere_boundary = -rmu - sqrt(rmu * rmu - r * r + atmosphere.top_radius * atmosphere.top_radius);
    if (distance_to_top_atmosphere_boundary > 0.0 * m)
    {
        camera = camera + view_ray * distance_to_top_atmosphere_boundary;
        r = atmosphere.top_radius;
        rmu += distance_to_top_atmosphere_boundary;
    }
    float mu = rmu / r;
    float mu_s = dot(camera, sun_direction) / r;
    float nu = dot(view_ray, sun_direction);
    float d = length(point - camera);
    bool ray_r_mu_intersects_ground = RayIntersectsGround(atmosphere, r, mu);
    transmittance = GetTransmittance(atmosphere, r, mu, d, ray_r_mu_intersects_ground);

    vec3 single_mie_scattering;
    vec3 scattering = GetCombinedScattering(atmosphere, r, mu, mu_s, nu, ray_r_mu_intersects_ground, single_mie_scattering);
    d = max(d - shadow_length, 0.0 * m);

    float r_p = ClampRadius(atmosphere, sqrt(d * d + 2.0 * r * mu * d + r * r));
    float mu_p = (r * mu + d) / r_p;
    float mu_s_p = (r * mu_s + d * nu) / r_p;
    vec3 single_mie_scattering_p;
    vec3 scattering_p = GetCombinedScattering(atmosphere, r_p, mu_p, mu_s_p, nu, ray_r_mu_intersects_ground, single_mie_scattering_p);
    vec3 shadow_transmittance = transmittance;
    if (shadow_length > 0.0 * m)
    {
        shadow_transmittance = GetTransmittance(atmosphere, r, mu, d, ray_r_mu_intersects_ground);
    }

    scattering = scattering - shadow_transmittance * scattering_p;
    single_mie_scattering = single_mie_scattering - shadow_transmittance * single_mie_scattering_p;

#ifdef COMBINED_SCATTERING_TEXTURES
    single_mie_scattering = GetExtrapolatedSingleMieScattering(atmosphere, vec4(scattering, single_mie_scattering.r));
#endif
    single_mie_scattering = single_mie_scattering * smoothstep(0.0,0.01, mu_s);
    return scattering * RayleighPhaseFunction(nu) + single_mie_scattering *MiePhaseFunction(atmosphere.mie_phase_function_g, nu);
}

vec3 GetSunAndSkyIrradiance( in AtmosphereParameters atmosphere, vec3 point, vec3 normal, vec3 sun_direction, out vec3 sky_irradiance)
{
    float r = length(point);
    float mu_s = dot(point, sun_direction) / r;

    sky_irradiance = GetIrradiance(atmosphere, irradiance_texture, r, mu_s) * (1.0 + dot(normal, point) / r) * 0.5;
    return atmosphere.solar_irradiance * GetTransmittanceToSun(atmosphere, r, mu_s) * max(dot(normal, sun_direction), 0.0);
}

#ifdef RADIANCE_API_ENABLED
vec3 GetSolarRadiance()
{
    return ATMOSPHERE.solar_irradiance / (PI * ATMOSPHERE.sun_angular_radius * ATMOSPHERE.sun_angular_radius);
}

vec3 GetSkyRadiance(vec3 camera, vec3 view_ray, float shadow_length, vec3 sun_direction, out vec3 transmittance)
{
    return GetSkyRadiance(ATMOSPHERE, camera, view_ray, shadow_length, sun_direction, transmittance);
}

vec3 GetSkyRadianceToPoint(vec3 camera, vec3 point, float shadow_length,  vec3 sun_direction, out vec3 transmittance)
{
    return GetSkyRadianceToPoint(ATMOSPHERE, camera, point, shadow_length, sun_direction, transmittance);
}

vec3 GetSunAndSkyIrradiance(vec3 p, vec3 normal, vec3 sun_direction, out vec3 sky_irradiance)
{
    return GetSunAndSkyIrradiance(ATMOSPHERE, transmittance_texture, p, normal, sun_direction, sky_irradiance);
}

#endif  // end RADIANCE_API_ENABLED

vec3 GetSolarLuminance()
{
    return ATMOSPHERE.solar_irradiance / (PI * ATMOSPHERE.sun_angular_radius * ATMOSPHERE.sun_angular_radius) * SUN_SPECTRAL_RADIANCE_TO_LUMINANCE;
}

vec3 GetSkyLuminance(vec3 camera, vec3 view_ray, Length shadow_length, vec3 sun_direction, out vec3 transmittance)
{
    return GetSkyRadiance(ATMOSPHERE, camera, view_ray, shadow_length, sun_direction, transmittance) *SKY_SPECTRAL_RADIANCE_TO_LUMINANCE;
}

vec3 GetSkyLuminanceToPoint(vec3 camera, vec3 point, Length shadow_length, vec3 sun_direction, out vec3 transmittance)
{
    return GetSkyRadianceToPoint(ATMOSPHERE, camera, point, shadow_length, sun_direction, transmittance) * SKY_SPECTRAL_RADIANCE_TO_LUMINANCE;
}

vec3 GetSunAndSkyIlluminance(vec3 p, vec3 normal, vec3 sun_direction, out IrradianceSpectrum sky_irradiance)
{
    vec3 sun_irradiance = GetSunAndSkyIrradiance(ATMOSPHERE, p, normal, sun_direction, sky_irradiance);

    sky_irradiance *= SKY_SPECTRAL_RADIANCE_TO_LUMINANCE;
    return sun_irradiance * SUN_SPECTRAL_RADIANCE_TO_LUMINANCE;
}