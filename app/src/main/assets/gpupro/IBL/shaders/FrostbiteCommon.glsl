#define M_PI     3.14159265358979323846
#define M_PI2    6.28318530717958647692
#define M_INV_PI 0.3183098861837906715

const uint gSampleCount = 1024;

float radicalInverse(uint i)
{
    i = (i & 0x55555555u) << 1 | (i & 0xAAAAAAAAu) >> 1;
    i = (i & 0x33333333u) << 2 | (i & 0xCCCCCCCCu) >> 2;
    i = (i & 0x0F0F0F0Fu) << 4 | (i & 0xF0F0F0F0u) >> 4;
    i = (i & 0x00FF00FFu) << 8 | (i & 0xFF00FF00u) >> 8;
    i = (i << 16) | (i >> 16);
    return float(i) * 2.3283064365386963e-10f;
}

vec2 getHammersley(uint i, uint N)
{
    return vec2(float(i) / float(N), radicalInverse(i));
}

// Utility function to get a vector perpendicular to an input vector
//    (from "Efficient Construction of Perpendicular Vectors Without Branching")
vec3 getPerpendicularStark(vec3 u)
{
    vec3 a = abs(u);
    uint xm = ((a.x - a.y) < 0 && (a.x - a.z) < 0) ? 1 : 0;
    uint ym = (a.y - a.z) < 0 ? (1u ^ xm) : 0;
    uint zm = 1u ^ (xm | ym);
    return cross(u, vec3(xm, ym, zm));
}

vec3 getPerpendicularSimple(vec3 u)
{
    vec3 up = abs(u.z) < 0.999999f ? vec3(0, 0, 1) : vec3(1, 0, 0);
    return normalize(cross(up, u));
}

/** Get a GGX half vector / microfacet normal, sampled according to the GGX distribution
    When using this function to sample, the probability density is pdf = D * NdotH / (4 * HdotV)

    \param[in] u Uniformly distributed random numbers between 0 and 1
    \param[in] N Surface normal
    \param[in] roughness Roughness^2 of material
*/
vec3 getGGXMicrofacet(vec2 u, vec3 N, float roughness)
{
    float a2 = roughness * roughness;

    float phi = M_PI2 * u.x;
    float cosTheta = sqrt(max(0, (1 - u.y)) / (1 + (a2 * a2 - 1) * u.y));
    float sinTheta = sqrt(max(0, 1 - cosTheta * cosTheta));

    // Tangent space H
    vec3 tH;
    tH.x = sinTheta * cos(phi);
    tH.y = sinTheta * sin(phi);
    tH.z = cosTheta;

    vec3 T = getPerpendicularStark(N);
    vec3 B = normalize(cross(N, T));

    // World space H
    return normalize(T * tH.x + B * tH.y + N * tH.z);
}

float smithGGX(float NdotL, float NdotV, float roughness)
{
    float k = ((roughness + 1) * (roughness + 1)) / 8;
    float g1 = NdotL / (NdotL * (1 - k) + k);
    float g2 = NdotV / (NdotV * (1 - k) + k);
    return g1 * g2;
}

vec3 fresnelSchlick(vec3 f0, vec3 f90, float u)
{
    return f0 + (f90 - f0) * pow(1 - u, 5);
}

/** Get a cosine-weighted random vector centered around a specified normal direction.

    \param[in] u Uniformly distributed random numbers between 0 and 1
    \param[in] N Surface normal
    \param[in] T A vector perpendicular to N
*/
vec3 getCosHemisphereSample(vec2 u, vec3 N, vec3 T)
{
    vec3 B = normalize(cross(N, T));

    float r = sqrt(u.x);
    float phi = u.y * M_PI2;

    vec3 L = vec3(r * cos(phi),
                    r * sin(phi),
                    sqrt(max(0.0f, 1.0f - u.x)));

    return normalize(T * L.x + B * L.y + N * L.z);
}

#define saturate(x) clamp(x, 0.0, 1.0)

/** Disney's diffuse term. Based on https://disney-animation.s3.amazonaws.com/library/s2012_pbs_disney_brdf_notes_v2.pdf
*/
float disneyDiffuseFresnel(float NdotV, float NdotL, float LdotH, float linearRoughness)
{
    float fd90 = 0.5 + 2 * LdotH * LdotH * linearRoughness;
    float fd0 = 1;
    float lightScatter = fresnelSchlick(vec3(fd0), vec3(fd90), NdotL).r;
    float viewScatter = fresnelSchlick(vec3(fd0), vec3(fd90), NdotV).r;
    return lightScatter * viewScatter;
}