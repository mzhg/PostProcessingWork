#include "SSSS_Common.glsl"

float3 BumpMap(sampler2D normalTex, float2 texcoord) {
    /*float3 bump;
    bump.xy = normalTex.Sample(AnisotropicSampler, texcoord).ag * 2.0 - 1.0;
    bump.z = sqrt(1.0 - bump.x * bump.x - bump.y * bump.y);
    return normalize(bump);*/

    return texture(normalTex, texcoord).xyz * 2.0 - 1.0;
}

float Fresnel(float3 H, float3 view, float f0) {
    float base = 1.0 - dot(view, H);
    float exponential = pow(base, 5.0);
    return exponential + f0 * (1.0 - exponential);
}

float SpecularKSK(sampler2D beckmannTex, float3 normal, float3 light, float3 view) {
    float3 H = view + light;
    float3 halfn = normalize(H);

    float ndotl = max(dot(normal, light), 0.0);
    float ndoth = max(dot(normal, halfn), 0.0);

    const float roughness = 0.5;
    float ph = pow(2.0 * textureLod(beckmannTex, float2(ndoth, roughness), 0.).r, 10.0);  // LinearSampler
    float f = Fresnel(halfn, view, 0.028);
    float ksk = max(ph * f / dot(H, H), 0.0);

    return ndotl * ksk;
}

layout(binding = 0) uniform sampler2D g_DiffuseTex;
layout(binding = 1) uniform sampler2D g_NormalTex;
layout(binding = 2) uniform sampler2DShadow g_Shadow;
layout(binding = 3) uniform sampler2D beckmannTex;

float Shadow(float3 worldPosition, int i) {
    float4 shadowPosition = mul(float4(worldPosition, 1.0), lightViewProjectionNDC);
//    shadowPosition.xy /= shadowPosition.w;
//    return shadowMaps[i].SampleCmpLevelZero(ShadowSampler, shadowPosition.xy, shadowPosition.z / lights[i].range);
    return textureProjLod(g_Shadow, shadowPosition, 0.);
}

float ShadowPCF(float3 worldPosition, int samples, float width) {
    float4 shadowPosition = mul(float4(worldPosition, 1.0), lightViewProjectionNDC);
    shadowPosition.xyz /= shadowPosition.w;

//    float w, h;
//    shadowMaps[i].GetDimensions(w, h);
    ivec2 dim = textureSize(g_Shadow, 0);
    float2 texelSize = 1.0/float2(dim);

    float shadow = 0.0;
    float offset = (samples - 1.0) / 2.0;
    for (float x = -offset; x <= offset; x += 1.0) {
        for (float y = -offset; y <= offset; y += 1.0) {
            float2 pos = shadowPosition.xy + width * float2(x, y) * texelSize;
//            shadow += shadowMaps[i].SampleCmpLevelZero(ShadowSampler, pos, shadowPosition.z / lights[i].range);
            shadow += textureLod(g_Shadow, float3(pos, shadowPosition.z / lightRange), 0.);
        }
    }
    shadow /= samples * samples;
    return shadow;
}

in SceneV2P
{
    float2 texcoord;
    float3 tangentView;
    float3 worldPosition;
    float3 tangentLight;
    float3 normal;
    float3 tangent;
}_input;

layout(location=0) out float4 Out_Color;

//float4 SceneColor(SceneV2P input)
void main()
{
    float3 surfaceNormal = normalize(_input.normal);
    float3 surfaceTangent = normalize(_input.tangent);

    float3x3 matTangentToWorld = float3x3(
                                    surfaceTangent,
                                    cross(surfaceNormal, surfaceTangent),
                                    surfaceNormal);

    matTangentToWorld = transpose(matTangentToWorld);

    const float bumpiness = 1.;
    float3 tangentNormal = lerp(float3(0.0, 0.0, 1.0), BumpMap(g_NormalTex, _input.texcoord), bumpiness);
    tangentNormal = matTangentToWorld * tangentNormal;

    float3 view = cameraPosition.xyz - _input.worldPosition;
    float3 tangentView = normalize(view);

    float dotNL = max(dot(lightDir.xyz, tangentNormal), 0.);
    float specular = SpecularKSK(beckmannTex, tangentNormal, lightDir.xyz, tangentView);
//    specular = min(specular, 0.);

    float4 albedo = texture(g_DiffuseTex, _input.texcoord);  // AnisotropicSampler
    float shadow = ShadowPCF(_input.worldPosition, 3, 1.0);

    Out_Color.rgb = albedo.rgb * (0.2 + dotNL + specular) * shadow;
    Out_Color.a = 1;
    return;

    float4 color = float4(0.0, 0.0, 0.0, 0.0);
    float3 light =  _input.worldPosition - lightPos.xyz;

    float spot = dot(lightDir.xyz, normalize(light));
//    if (spot > falloffAngle)
    {
        float3 tangentLight = normalize(_input.tangentLight);

        //float shadow = Shadow(input.worldPosition, i);
        float shadow = ShadowPCF(_input.worldPosition, 3, 1.0);

        float dist = length(light);
        float curve = min(pow(dist / lightRange, 6.0), 1.0);
        float attenuation = lerp(1.0 / (1.0 + lightAttenuation * dist * dist), 0.0, curve);
        attenuation = 1.;

        spot = pow(spot, spotExponent);

        float3 diffuse = albedo.rgb * max(dot(tangentLight, tangentNormal)/*dot(-lightDir.xyz, _input.normal)*/, 0.0);

        float specular = SpecularKSK(beckmannTex, tangentNormal, tangentLight, tangentView);

        color.rgb += /*lights[i].color **/ shadow * attenuation * spot * (diffuse + specular);  // assum the color of light is white.
    }

    color.rgb += albedo.rgb * 0.2; //ambinet
    color.a = albedo.a;

    const bool fade = false;
    if (fade)
    {
        float d = abs(0.5 - _input.texcoord.x);
        color.rgb *= 1.0 - 10.0f * max(_input.texcoord.y - 0.76f + 0.22 * d * d, 0.0);
    }

    Out_Color = color;
}