#include "../../../shader_libs/ShadowWork/ShadowWork.glsl"

uniform int g_useTexture;
uniform vec3 g_podiumCenterWorld;
uniform vec3 g_lightPos;
uniform bool g_useDiffuse;

in vec4 worldPosition;
in vec4 lightPosition;
in vec3 normal;

layout(binding = 0) uniform sampler2D g_rockDiffuse;
layout(binding = 1) uniform sampler2D g_groundDiffuse;
layout(binding = 2) uniform sampler2D g_groundNormal;
layout(binding = 3) uniform sampler2D g_ShadowDepth;
layout(binding = 4) uniform sampler2DShadow g_ShadowMap;

bool isBlack(vec3 c)
{
    return (dot(c, c) == 0.0f);
}

vec2 cubeMapTexCoords(vec3 v)
{
    vec2 uv;
    if (abs(v.x) > abs(v.y) && abs(v.x) > abs(v.z))
        uv = vec2(v.y / abs(v.x), v.z / abs(v.x));
    if (abs(v.y) > abs(v.x) && abs(v.y) > abs(v.z))
        uv = vec2(v.x / abs(v.y), v.z / abs(v.y));
    else
        uv = vec2(v.x / abs(v.z), v.y / abs(v.z));
    return uv * 0.5f + 0.5f;
}

vec4 shade(vec3 worldPos, vec3 normal)
{
    vec3 lightDir = normalize(g_lightPos - worldPos);
    if (g_useTexture == 1)
    {
        vec2 uv = (worldPos.xz * 0.5f + 0.5f) * 2.0f;
        vec3 diffuse = texture(g_groundDiffuse, uv).xyz;
        normal = texture(g_groundNormal, uv).xzy * 2.0f - 1.0f;
        diffuse *= max(dot(lightDir, normal), 0.0f);
        diffuse *= pow(dot(lightDir, normalize(g_lightPos)), 40.0f);
        return vec4(diffuse, 1.0f);
    }
    else if (g_useTexture == 2)
    {
        vec2 uv = cubeMapTexCoords(normalize(worldPos.xyz - g_podiumCenterWorld));
        vec3 diffuse = texture2D(g_rockDiffuse, uv).xyz * 1.2f;
        diffuse *= max(dot(lightDir, normal), 0.0f);
        return vec4(diffuse, 1.0f);
    }
    else
    {
        float x = max(dot(lightDir, normal), 0.0f);
        vec4 diffuse = vec4(x, x, x, 1.0f);
        return g_useDiffuse ? diffuse : vec4(1.0f, 1.0f, 1.0f, 1.0f);
    }
}

layout(location = 0) out vec4 OutColor;

void main()
{
//    vec2 uv = lightPosition.xy / lightPosition.w;
//    float z = lightPosition.z / lightPosition.w;

    // Compute gradient using ddx/ddy before any branching
//    vec2 dz_duv = depthGradient(uv, z);
    float shadow = CaculateShadows(worldPosition.xyz, g_ShadowMap, g_ShadowDepth);
    OutColor = shade(worldPosition.xyz, normal) * shadow;

    /*if (isBlack(color.rgb))
    {
        gl_FragColor = color;
    }
    else
    {
        // Eye-space z from the light's point of view
        float zEye = -(g_lightView * worldPosition).z;
        float shadow = 1.0f;
        switch (g_shadowTechnique)
        {
            case 1:
                shadow = pcssShadow(uv, z, dz_duv, zEye);
                break;

            case 2:
                shadow = pcfShadow(uv, z, dz_duv, zEye);
                break;
        }
        gl_FragColor = color * shadow;
    }*/
}