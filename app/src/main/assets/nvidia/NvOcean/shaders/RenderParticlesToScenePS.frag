#include "ocean_smoke.glsl"

out float4 OutColor;

in GS_SCENE_PARTICLE_OUTPUT
{
//    float4 Position               : SV_Position;
    float3 TextureUVAndOpacity    /*: TEXCOORD0*/;
    float3 PSMCoords              /*: PSMCoords*/;
    float  FogFactor              /*: FogFactor*/;
}In;

void main()
{
    float3 illumination = g_LightColor*0.5 + g_AmbientColor*0.5 + g_LightningColor*0.5;
    illumination *= CalcPSMShadowFactor(In.PSMCoords);

    float4 tex = GetParticleRGBA(/*g_samplerDiffuse,*/In.TextureUVAndOpacity.xy,In.TextureUVAndOpacity.z);

    float4 Output = float4(illumination * tex.rgb, tex.a);
    Output.rgb = lerp(g_AmbientColor*0.5 + g_LightningColor*0.5,Output.rgb,In.FogFactor);

    OutColor = Output;
}