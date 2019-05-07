#include "FrostbiteCommon.glsl"

layout(binding = 0) uniform samplerCube gInputTex;
layout(location = 0) out vec4 Out_Irradiance;
uniform float g_Roughness = 0.2;

float evalGGX(float roughness, float NdotH)
{
    float a2 = roughness * roughness;
    float d = ((NdotH * a2 - NdotH) * NdotH + 1);
    return a2 / (d * d);
}

float evalSmithGGX(float NdotL, float NdotV, float roughness)
{
    // Optimized version of Smith, already taking into account the division by (4 * NdotV)
    float a2 = roughness * roughness;
    // `NdotV *` and `NdotL *` are inversed. It's not a mistake.
    float ggxv = NdotL * sqrt((-NdotV * a2 + NdotV) * NdotV + a2);
    float ggxl = NdotV * sqrt((-NdotL * a2 + NdotL) * NdotL + a2);
    return 0.5f / (ggxv + ggxl);
}

vec4 integrateSpecularLD(vec3 V, vec3 N, float roughness)
{
    // Resource Dimensions
    float width, height, mipCount;
//    gInputTex.GetDimensions(0, width, height, mipCount);
    width = textureSize(gInputTex, 0).x;
    mipCount = textureQueryLevels(gInputTex);

    float cubeWidth = width;

    vec3 accBrdf = vec3(0);
    float accBrdfWeight = 0;
    float NdotV = saturate(dot(N, V));
    for (uint i = 0; i < gSampleCount; i++)
    {
        vec2 u = getHammersley(i, gSampleCount);
        vec3 H = getGGXMicrofacet(u, N, roughness);
        vec3 L = reflect(-N, H);
        float NdotL = dot(N, L);

        if(NdotL > 0)
        {
            float NdotH = saturate(dot(N, H));
            float LdotH = saturate(dot(L, H));

            // Our GGX function does not include division by PI
            float pdf = (evalGGX(roughness, NdotH) * M_INV_PI) * NdotH / (4 * LdotH);

            float omegaS = 1 / (gSampleCount * pdf);
            float omegaP = 4.0 * M_PI / (6 * cubeWidth * cubeWidth);
            float mipLevel = clamp(0.5 * log2(omegaS / omegaP), 0, mipCount);

//            vec2 uv = dirToSphericalCrd(L);
            vec3 Li = textureLod(gInputTex, L, mipLevel).rgb;
            float weight = NdotL;
            weight *= evalSmithGGX(NdotL, NdotV, roughness);
            weight *= LdotH / NdotH; // this is likely 1
            weight *= saturate(1 - saturate(pow(1 - LdotH, 5))); // neglecting bias

            accBrdf += Li * weight;
            accBrdfWeight += weight;
        }
    }

    return vec4(accBrdf / accBrdfWeight, 1.0f);
}

in vec3 m_Normal;

void main()
{
    vec3 N = normalize(m_Normal);
    Out_Irradiance = integrateSpecularLD(N, N, g_Roughness);
}