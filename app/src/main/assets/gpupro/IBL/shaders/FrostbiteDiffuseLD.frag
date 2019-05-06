#include "FrostbiteCommon.glsl"

layout(binding = 0) uniform samplerCube gInputTex;
layout(location = 0) out vec4 Out_Irradiance;

vec4 integrateDiffuseLD(vec3 N)
{
    vec3 accumulation = vec3(0);
    float accBrdf = 0;
    vec3 V = N;
    for (uint i = 0; i < gSampleCount; i++)
    {
        vec2 u = getHammersley(i, gSampleCount);
        // Less artifacts when using basic perpendicular calculation
        vec3 L = getCosHemisphereSample(u, N, getPerpendicularSimple(N));
        float NdotL = dot(N, L);
        if (NdotL > 0)
        {
            float LdotH = saturate(dot(L, normalize(V + L)));
            float NdotV = saturate(dot(N, V));
            float brdf = disneyDiffuseFresnel(NdotV, NdotL, LdotH, 0);
            accumulation += texture(gInputTex, L).rgb * brdf;
            accBrdf += brdf;
        }
    }

    return vec4(accumulation / accBrdf, 1.0f);
}

in vec3 m_Normal;

void main()
{
    vec3 N = normalize(m_Normal);
    Out_Irradiance = integrateDiffuseLD(N);
}