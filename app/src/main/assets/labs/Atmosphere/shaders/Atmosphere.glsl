#include "AtmosphereCommon.glsl"


vec3 GetExtrapolatedSingleMieScattering( in AtmosphereParameters atmosphere, in vec4 scattering)
{
    if (scattering.r <= 0.0)
    {
        return vec3(0.0);
    }

    return scattering.rgb * scattering.a / scattering.r * (atmosphere.rayleigh_scattering.r / atmosphere.mie_scattering.r) * (atmosphere.mie_scattering / atmosphere.rayleigh_scattering);
}

vec3 GetCombinedScattering( in AtmosphereParameters atmosphere, float r, float mu, float mu_s, float nu, bool ray_r_mu_intersects_ground, out vec3 single_mie_scattering)
{
    vec4 uvwz = GetScatteringTextureUvwzFromRMuMuSNu(atmosphere, r, mu, mu_s, nu, ray_r_mu_intersects_ground);
    float tex_coord_x = uvwz.x * float(SCATTERING_TEXTURE_NU_SIZE - 1);
    float tex_x = floor(tex_coord_x);
    float lerp = tex_coord_x - tex_x;
    vec3 uvw0 = vec3((tex_x + uvwz.y) / float(SCATTERING_TEXTURE_NU_SIZE), uvwz.z, uvwz.w);
    vec3 uvw1 = vec3((tex_x + 1.0 + uvwz.y) / float(SCATTERING_TEXTURE_NU_SIZE), uvwz.z, uvwz.w);
#ifdef COMBINED_SCATTERING_TEXTURES
    vec4 combined_scattering = texture(single_rayleigh_scattering_texture, uvw0) * (1.0 - lerp) + texture(single_rayleigh_scattering_texture, uvw1) * lerp;
    vec3 scattering = vec3(combined_scattering);
    single_mie_scattering = GetExtrapolatedSingleMieScattering(atmosphere, combined_scattering);
#else
    vec3 scattering = vec3(texture(single_rayleigh_scattering_texture, uvw0) * (1.0 - lerp) + texture(single_rayleigh_scattering_texture, uvw1) * lerp);
    single_mie_scattering = vec3(texture(single_mie_scattering_texture, uvw0) * (1.0 - lerp) + texture(single_mie_scattering_texture, uvw1) * lerp);
#endif
    return scattering;
}

vec3 GetTransmittanceToSun( in AtmosphereParameters atmosphere, float r, float mu_s)
{
    float sin_theta_h = atmosphere.bottom_radius / r;
    float cos_theta_h = -sqrt(max(1.0 - sin_theta_h * sin_theta_h, 0.0));
    return GetTransmittanceToTopAtmosphereBoundary(atmosphere, r, mu_s) *
    smoothstep(-sin_theta_h * atmosphere.sun_angular_radius / rad, sin_theta_h * atmosphere.sun_angular_radius / rad, mu_s - cos_theta_h);
}

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
    }
    else if (r > atmosphere.top_radius)
    {
        transmittance = vec3(1.0);
        return vec3(0.0 * watt_per_square_meter_per_sr_per_nm);
    }

    float mu = rmu / r;
    float mu_s = dot(camera, sun_direction) / r;
    float nu = dot(view_ray, sun_direction);
    bool ray_r_mu_intersects_ground = RayIntersectsGround(atmosphere, r, mu);
    transmittance = ray_r_mu_intersects_ground ? vec3(0.0) : GetTransmittanceToTopAtmosphereBoundary(atmosphere, r, mu);
    vec3 single_mie_scattering;
    vec3 scattering;
    if (shadow_length == 0.0)
    {
        scattering = GetCombinedScattering( atmosphere, r, mu, mu_s, nu, ray_r_mu_intersects_ground, single_mie_scattering);
    }
    else
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

    sky_irradiance = GetIrradiance(atmosphere, r, mu_s) * (1.0 + dot(normal, point) / r) * 0.5;
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
    return GetSunAndSkyIrradiance(ATMOSPHERE, p, normal, sun_direction, sky_irradiance);
}
    #endif

vec3 GetSolarLuminance()
{
    return ATMOSPHERE.solar_irradiance / (PI * ATMOSPHERE.sun_angular_radius * ATMOSPHERE.sun_angular_radius) * ATMOSPHERE.SUN_SPECTRAL_RADIANCE_TO_LUMINANCE;
}

vec3 GetSkyLuminance(vec3 camera, vec3 view_ray, float shadow_length, vec3 sun_direction, out vec3 transmittance)
{
    return GetSkyRadiance(ATMOSPHERE, camera, view_ray, shadow_length, sun_direction, transmittance) *ATMOSPHERE.SKY_SPECTRAL_RADIANCE_TO_LUMINANCE;
}

vec3 GetSkyLuminanceToPoint(vec3 camera, vec3 point, float shadow_length, vec3 sun_direction, out vec3 transmittance)
{
    return GetSkyRadianceToPoint(ATMOSPHERE, camera, point, shadow_length, sun_direction, transmittance) * ATMOSPHERE.SKY_SPECTRAL_RADIANCE_TO_LUMINANCE;
}

vec3 GetSunAndSkyIlluminance(vec3 p, vec3 normal, vec3 sun_direction, out vec3 sky_irradiance)
{
    vec3 sun_irradiance = GetSunAndSkyIrradiance(ATMOSPHERE, p, normal, sun_direction, sky_irradiance);

    sky_irradiance *= ATMOSPHERE.SKY_SPECTRAL_RADIANCE_TO_LUMINANCE;
    return sun_irradiance * ATMOSPHERE.SUN_SPECTRAL_RADIANCE_TO_LUMINANCE;
}