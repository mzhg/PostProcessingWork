#include "ocean_smoke.glsl"

out float4 OutColor;

in GS_PSM_PARTICLE_OUTPUT
{
//    float4 Position                      : SV_Position;
//    nointerpolation uint LayerIndex      : SV_RenderTargetArrayIndex;
    float3 TextureUVAndOpacity           /*: TEXCOORD0*/;
    flat uint SubLayer        /*: TEXCOORD1*/;
}In;

void main()
{
    float4 tex = GetParticleRGBA(g_samplerDiffuse,In.TextureUVAndOpacity.xy,In.TextureUVAndOpacity.z);

    float4 Output = tex.a;
    Output *= g_PSMOpacityMultiplier;
    if(In.SubLayer == 0)
    Output *= float4(1,0,0,0);
    else if(In.SubLayer == 1)
    Output *= float4(0,1,0,0);
    else if(In.SubLayer == 2)
    Output *= float4(0,0,1,0);
    else
    Output *= float4(0,0,0,1);

    OutColor = Output;
}