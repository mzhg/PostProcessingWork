#include "ocean_vessel.glsl"

layout(location = 0) out float4 color;
layout(location = 1) out float4 position;

in VS_OUT
{
//    float4 position : SV_Position;
    float3 world_normal /*: NORMAL*/;
    float3 world_pos /*: WORLD_POS*/;
    float3 eye_pos /*: EYE_POS*/;
    float2 rust_uv /*: TEXCOORD*/;
    float2 rustmap_uv /*: TEXCOORD2*/;
    float  model_z /*: Z*/;
}i;

void main()
{
    float3 nml = normalize(i.world_normal);
    float4 result = g_DiffuseColor;
    float rustmap = 1.0-g_texRustMap.Sample(g_samplerDiffuse,i.rustmap_uv).r;
    rustmap = 0.7*rustmap*abs(nml.z)*1.0*saturate(0.6-i.model_z*0.02);
    float4 rust = g_texRust.Sample(g_samplerDiffuse,i.rust_uv);
    result = lerp(result, result*rust,saturate(rustmap));

    float hemisphere_term = 0.5f + 0.25f * nml.y;	// Hemisphere lighting against world 'up'
    float3 lighting = hemisphere_term * (g_AmbientColor + g_LightningColor*0.5) + 2.0*g_LightColor * saturate(dot(nml.xzy,g_LightDirection));

    // adding lightnings
    lighting += 2.0 * g_LightningColor * saturate(dot(nml,normalize(g_LightningPosition.xzy-i.world_pos)));

    for(int ix = 0; ix != g_LightsNum; ++ix) {
        float3 pixel_to_light = g_SpotlightPosition[ix].xyz - i.eye_pos;
        float3 pixel_to_light_nml = normalize(pixel_to_light);
        float attn = saturate(dot(pixel_to_light_nml,nml));
        attn *= 1.f/dot(pixel_to_light,pixel_to_light);
        attn *= saturate(1.f*(-dot(g_SpotLightAxisAndCosAngle[ix].xyz,pixel_to_light_nml)-g_SpotLightAxisAndCosAngle[ix].w)/(1.f-g_SpotLightAxisAndCosAngle[ix].w));
        float shadow = 1.0f;
        #if ENABLE_SHADOWS
        if (attn * dot(g_SpotlightColor[ix].xyz, g_SpotlightColor[ix].xyz) > 0.01f)
        {
            shadow = GetShadowValue(g_SpotlightResource[ix], g_SpotlightMatrix[ix], i.eye_pos.xyz);
        }
            #endif
        lighting += attn * g_SpotlightColor[ix].xyz * shadow;
    }

    lighting *=2.0;
    result.rgb *= lighting;

    float fog_factor = exp(dot(i.eye_pos,i.eye_pos)*g_FogExponent);
    result.rgb = lerp(g_AmbientColor + g_LightningColor*0.5,result.rgb,fog_factor);

    color = result;
    position = float4(i.world_pos.xyz, 0);
}