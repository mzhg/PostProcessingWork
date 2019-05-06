
#include "FrostbiteCommon.glsl"

vec4 integrateDFG(vec3 N, vec3 V, float roughness)
{
    float NdotV = dot(N, V);
    vec3 accumulation = vec3(0);

    for(uint i = 0; i < gSampleCount; i++)
    {
        vec2 u = getHammersley(i, gSampleCount);

        // Specular GGX DFG integration (stored in RG)
        vec3 H = getGGXMicrofacet(u, N, roughness);
        vec3 L = reflect(-N, H);
        float NdotH = saturate(dot(N, H));
        float LdotH = saturate(dot(L, H));
        float NdotL = saturate(dot(N, L));

        // #TODO Using our evalSmithGGX (modified to undo optimized terms) looks bad in certain cases.
        float G = smithGGX(NdotL, NdotV, roughness);
        if(NdotL > 0 && G > 0)
        {
            float GVis = (G * LdotH) / (NdotV * NdotH);
            float Fc = fresnelSchlick(vec3(0), vec3(1), LdotH).r;
            accumulation.r += (1 - Fc) * GVis;
            accumulation.g += Fc * GVis;
        }

        // Disney Diffuse integration (stored in B)
        u = fract(u + 0.5);
        L = getCosHemisphereSample(u, N, getPerpendicularStark(N));
        NdotL = saturate(dot(N, L));
        if(NdotL > 0)
        {
            LdotH = saturate(dot(L, normalize(V + L)));
            NdotV = saturate(dot(N, V));
            accumulation.b += disneyDiffuseFresnel(NdotV, NdotL, LdotH, sqrt(roughness));
        }
    }

    return vec4(accumulation / float(gSampleCount), 1.0f);
}

in vec4 m_f4UVAndScreenPos;
uniform vec2 g_Viewport;

layout(location = 0) out vec4 Out_Color;

void main()
{
    // DFG texture will be sampled using
    // texC.x = NdotV
    // texC.y = roughness
    float Roughness = m_f4UVAndScreenPos.y - 0.5/g_Viewport.y;
    float NoV = m_f4UVAndScreenPos.x - 0.5/g_Viewport.x;

    const vec3 N = vec3(0, 0, 1);

    // texC.x is NdotV, calculate a valid V assuming constant N
    float theta = acos(NoV);
    const vec3 V = vec3(sin(theta), 0, cos(theta));

    Out_Color = integrateDFG(N, V, Roughness);
}