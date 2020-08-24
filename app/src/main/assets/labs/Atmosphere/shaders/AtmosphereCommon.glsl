// The Dimension of the transmittance texture
const int TRANSMITTANCE_TEXTURE_WIDTH = 256;
const int TRANSMITTANCE_TEXTURE_HEIGHT = 64;

// The Dimension of the scattering texture
const int SCATTERING_TEXTURE_R_SIZE = 32;
const int SCATTERING_TEXTURE_MU_SIZE = 128;
const int SCATTERING_TEXTURE_MU_S_SIZE = 32;
const int SCATTERING_TEXTURE_NU_SIZE = 8;

// The Dimension of the irradiance texture
const int IRRADIANCE_TEXTURE_WIDTH = 64;
const int IRRADIANCE_TEXTURE_HEIGHT = 16;

// The physical unit conversion
const float m = 1.0;   // meters
const float nm = 1.0;
const float rad = 1.0;
const float sr = 1.0;
const float watt = 1.0;
const float lm = 1.0;
const float PI = 3.14159265358979323846;
const float km = 1000.0 * m;
const float m2 = m * m;
const float m3 = m * m * m;
const float pi = PI * rad;
const float deg = pi / 180.0;
const float watt_per_square_meter = watt / m2;
const float watt_per_square_meter_per_sr = watt / (m2 * sr);
const float watt_per_square_meter_per_nm = watt / (m2 * nm);
const float watt_per_square_meter_per_sr_per_nm = watt / (m2 * sr * nm);
const float watt_per_cubic_meter_per_sr_per_nm = watt / (m3 * sr * nm);
const float cd = lm / sr;
const float kcd = 1000.0 * cd;
const float cd_per_square_meter = cd / m2;
const float kcd_per_square_meter = kcd / m2;

#define assert(x)
//
struct DensityProfileLayer {
    float width;
    float exp_term;
    float exp_scale;
//    float linear_term;
//    float constant_term;
    uint linear_and_constant_term;
};


struct DensityProfile
{
    DensityProfileLayer layers[2];
};

struct AtmosphereParameters
{
    vec3 solar_irradiance;
    float sun_angular_radius;

    vec3 rayleigh_scattering;
    float bottom_radius;

    vec3 mie_scattering;
    float top_radius;

    vec3 mie_extinction;
    float mie_phase_function_g;

    vec3 ground_albedo;
    float mu_s_min;

    DensityProfile rayleigh_density;

    DensityProfile mie_density;

    DensityProfile absorption_density;
    vec3 absorption_extinction;
    float padding;
};

layout(binding = 0) uniform CBuffer0
{
    AtmosphereParameters ATMOSPHERE;
};

layout(binding = 0) uniform sampler2D transmittance_texture;
layout(binding = 1) uniform sampler3D single_rayleigh_scattering_texture;
layout(binding = 2) uniform sampler3D single_mie_scattering_texture;
layout(binding = 3) uniform sampler3D multiple_scattering_texture;
layout(binding = 4) uniform sampler2D irradiance_texture;
layout(binding = 5) uniform sampler3D scattering_density_texture;

// Clamp the value into the range[-1,1]
float ClampCosine(float mu)
{
    return clamp(mu, float(-1.0), float(1.0));
}

// Clamp the value into the range[o,d]
float ClampDistance(float d)
{
    return max(d, 0.0);
}

float ClampRadius(in AtmosphereParameters atmosphere, float r)
{
    return clamp(r, atmosphere.bottom_radius, atmosphere.top_radius);
}

float SafeSqrt(float a)
{
    return sqrt(max(a, 0.0));
}

float GetTextureCoordFromUnitRange(float x, int texture_size)
{
    return 0.5 / float(texture_size) + x * (1.0 - 1.0 / float(texture_size));
}

float GetUnitRangeFromTextureCoord(float u, int texture_size)
{
    return (u - 0.5 / float(texture_size)) / (1.0 - 1.0 / float(texture_size));
}

void GetRMuFromTransmittanceTextureUv(in AtmosphereParameters atmosphere, in vec2 uv, out float r, out float mu)
{
    assert(uv.x >= 0.0 && uv.x <= 1.0);
    assert(uv.y >= 0.0 && uv.y <= 1.0);
    float x_mu = GetUnitRangeFromTextureCoord(uv.x, TRANSMITTANCE_TEXTURE_WIDTH);
    float x_r = GetUnitRangeFromTextureCoord(uv.y, TRANSMITTANCE_TEXTURE_HEIGHT);
    float H = sqrt(atmosphere.top_radius * atmosphere.top_radius - atmosphere.bottom_radius * atmosphere.bottom_radius);
    float rho = H * x_r;
    r = sqrt(rho * rho + atmosphere.bottom_radius * atmosphere.bottom_radius);
    float d_min = atmosphere.top_radius - r;
    float d_max = rho + H;
    float d = d_min + x_mu * (d_max - d_min);
    mu = d == 0.0 * m ? 1.0 : (H * H - rho * rho - d * d) / (2.0 * r * d);
    mu = ClampCosine(mu);
}

float DistanceToTopAtmosphereBoundary(in AtmosphereParameters atmosphere, float r, float mu)
{
    assert(r <= atmosphere.top_radius);
    assert(mu >= -1.0 && mu <= 1.0);
    float discriminant = r * r * (mu * mu - 1.0) + atmosphere.top_radius * atmosphere.top_radius;
    return ClampDistance(-r * mu + SafeSqrt(discriminant));
}

float GetLayerDensity(in DensityProfileLayer layer, float altitude)
{
    vec2 float16 = unpackHalf2x16(layer.linear_and_constant_term);
    float linear_term = float16.x;
    float constant_term = float16.y;
    float density = layer.exp_term * exp(layer.exp_scale * altitude) + linear_term * altitude + constant_term;
    return clamp(density, 0.0, 1.0);
}

float GetProfileDensity(in DensityProfile profile, float altitude)
{
    return altitude < profile.layers[0].width ? GetLayerDensity(profile.layers[0], altitude) : GetLayerDensity(profile.layers[1], altitude);
}

void GetRMuSFromIrradianceTextureUv(in AtmosphereParameters atmosphere, in vec2 uv, out float r, out float mu_s)
{
    assert(uv.x >= 0.0 && uv.x <= 1.0);
    assert(uv.y >= 0.0 && uv.y <= 1.0);
    float x_mu_s = GetUnitRangeFromTextureCoord(uv.x, IRRADIANCE_TEXTURE_WIDTH);
    float x_r = GetUnitRangeFromTextureCoord(uv.y, IRRADIANCE_TEXTURE_HEIGHT);
    r = atmosphere.bottom_radius + x_r * (atmosphere.top_radius - atmosphere.bottom_radius);
    mu_s = ClampCosine(2.0 * x_mu_s - 1.0);
}

vec2 GetTransmittanceTextureUvFromRMu(in AtmosphereParameters atmosphere, float r, float mu)
{
    assert(r >= atmosphere.bottom_radius && r <= atmosphere.top_radius);
    assert(mu >= -1.0 && mu <= 1.0);
    float H = sqrt(atmosphere.top_radius * atmosphere.top_radius - atmosphere.bottom_radius * atmosphere.bottom_radius);
    float rho = SafeSqrt(r * r - atmosphere.bottom_radius * atmosphere.bottom_radius);
    float d = DistanceToTopAtmosphereBoundary(atmosphere, r, mu);
    float d_min = atmosphere.top_radius - r;
    float d_max = rho + H;
    float x_mu = (d - d_min) / (d_max - d_min);
    float x_r = rho / H;
    return vec2(GetTextureCoordFromUnitRange(x_mu, TRANSMITTANCE_TEXTURE_WIDTH), GetTextureCoordFromUnitRange(x_r, TRANSMITTANCE_TEXTURE_HEIGHT));
}

void GetRMuMuSNuFromScatteringTextureUvwz(in AtmosphereParameters atmosphere, in vec4 uvwz, out float r, out float mu, out float mu_s, out float nu, out bool ray_r_mu_intersects_ground)
{
    assert(uvwz.x >= 0.0 && uvwz.x <= 1.0);
    assert(uvwz.y >= 0.0 && uvwz.y <= 1.0);
    assert(uvwz.z >= 0.0 && uvwz.z <= 1.0);
    assert(uvwz.w >= 0.0 && uvwz.w <= 1.0);

    float H = sqrt(atmosphere.top_radius * atmosphere.top_radius - atmosphere.bottom_radius * atmosphere.bottom_radius);
    float rho = H * GetUnitRangeFromTextureCoord(uvwz.w, SCATTERING_TEXTURE_R_SIZE);
    r = sqrt(rho * rho + atmosphere.bottom_radius * atmosphere.bottom_radius);
    if (uvwz.z < 0.5) {
        float d_min = r - atmosphere.bottom_radius;
        float d_max = rho;
        float d = d_min + (d_max - d_min) * GetUnitRangeFromTextureCoord(1.0 - 2.0 * uvwz.z, SCATTERING_TEXTURE_MU_SIZE / 2);
        mu = d == 0.0 ? -1.0 :ClampCosine(-(rho * rho + d * d) / (2.0 * r * d));
        ray_r_mu_intersects_ground = true;
    } else {
        float d_min = atmosphere.top_radius - r;
        float d_max = rho + H;
        float d = d_min + (d_max - d_min) * GetUnitRangeFromTextureCoord(2.0 * uvwz.z - 1.0, SCATTERING_TEXTURE_MU_SIZE / 2);
        mu = d == 0.0 ? 1.0 :ClampCosine((H * H - rho * rho - d * d) / (2.0 * r * d));
        ray_r_mu_intersects_ground = false;
    }

    float x_mu_s = GetUnitRangeFromTextureCoord(uvwz.y, SCATTERING_TEXTURE_MU_S_SIZE);
    float d_min = atmosphere.top_radius - atmosphere.bottom_radius;
    float d_max = H;
    float A = -2.0 * atmosphere.mu_s_min * atmosphere.bottom_radius / (d_max - d_min);
    float a = (A - x_mu_s * A) / (1.0 + x_mu_s * A);
    float d = d_min + min(a, A) * (d_max - d_min);
    mu_s = d == 0.0 ? 1.0 : ClampCosine((H * H - d * d) / (2.0 * atmosphere.bottom_radius * d));
    nu = ClampCosine(uvwz.x * 2.0 - 1.0);
}

void GetRMuMuSNuFromScatteringTextureFragCoord(in AtmosphereParameters atmosphere, in vec3 frag_coord, out float r, out float mu, out float mu_s, out float nu,
out bool ray_r_mu_intersects_ground)
{
    const vec4 SCATTERING_TEXTURE_SIZE = vec4(
    SCATTERING_TEXTURE_NU_SIZE - 1,
    SCATTERING_TEXTURE_MU_S_SIZE,
    SCATTERING_TEXTURE_MU_SIZE,
    SCATTERING_TEXTURE_R_SIZE);

    float frag_coord_nu = floor(frag_coord.x / float(SCATTERING_TEXTURE_MU_S_SIZE));
    float frag_coord_mu_s = mod(frag_coord.x, float(SCATTERING_TEXTURE_MU_S_SIZE));
    vec4 uvwz = vec4(frag_coord_nu, frag_coord_mu_s, frag_coord.y, frag_coord.z) / SCATTERING_TEXTURE_SIZE;

    GetRMuMuSNuFromScatteringTextureUvwz(atmosphere, uvwz, r, mu, mu_s, nu, ray_r_mu_intersects_ground);

    nu = clamp(nu, mu * mu_s - sqrt((1.0 - mu * mu) * (1.0 - mu_s * mu_s)),
    mu * mu_s + sqrt((1.0 - mu * mu) * (1.0 - mu_s * mu_s)));
}

vec3 GetScattering( in AtmosphereParameters atmosphere, sampler3D scattering_texture, float r, float mu, float mu_s, float nu, bool ray_r_mu_intersects_ground)
{
    vec4 uvwz = GetScatteringTextureUvwzFromRMuMuSNu(atmosphere, r, mu, mu_s, nu, ray_r_mu_intersects_ground);
    float tex_coord_x = uvwz.x * float(SCATTERING_TEXTURE_NU_SIZE - 1);
    float tex_x = floor(tex_coord_x);
    float lerp = tex_coord_x - tex_x;
    vec3 uvw0 = vec3((tex_x + uvwz.y) / float(SCATTERING_TEXTURE_NU_SIZE), uvwz.z, uvwz.w);
    vec3 uvw1 = vec3((tex_x + 1.0 + uvwz.y) / float(SCATTERING_TEXTURE_NU_SIZE), uvwz.z, uvwz.w);
    return texture(scattering_texture, uvw0).rgb * (1.0 - lerp) + texture(scattering_texture, uvw1) * lerp.rgb;
}

vec3 GetScattering( in AtmosphereParameters atmosphere, sampler3D single_rayleigh_scattering_texture,
    sampler3D single_mie_scattering_texture,sampler3D multiple_scattering_texture,
    float r, float mu, float mu_s, float nu, bool ray_r_mu_intersects_ground, int scattering_order)
{
    if (scattering_order == 1)
    {
        vec3 rayleigh = GetScattering(atmosphere, single_rayleigh_scattering_texture, r, mu, mu_s, nu, ray_r_mu_intersects_ground);
        vec3 mie = GetScattering(atmosphere, single_mie_scattering_texture, r, mu, mu_s, nu, ray_r_mu_intersects_ground);
        return rayleigh * RayleighPhaseFunction(nu) + mie * MiePhaseFunction(atmosphere.mie_phase_function_g, nu);
    } else
    {
        return GetScattering(atmosphere, multiple_scattering_texture, r, mu, mu_s, nu, ray_r_mu_intersects_ground);
    }
}


