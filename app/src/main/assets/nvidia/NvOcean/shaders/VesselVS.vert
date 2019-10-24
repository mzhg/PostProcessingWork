#include "ocean_vessel.glsl"

layout(location = 0) in float4 vPos;
layout(location = 1) in float2 vTex;
layout(location = 2) in float3 vNml;

out VS_OUT
{
//    float4 position : SV_Position;
    float3 world_normal /*: NORMAL*/;
    float3 world_pos /*: WORLD_POS*/;
    float3 eye_pos /*: EYE_POS*/;
    float2 rust_uv /*: TEXCOORD*/;
    float2 rustmap_uv /*: TEXCOORD2*/;
    float  model_z /*: Z*/;
}o;

void main()
{
    gl_Position = mul(vPos, g_matWorldViewProj);
    o.world_normal = mul(vNml, (float3x3)g_matWorld);
    o.world_pos = mul(vPos, (float4x3)g_matWorld);
    o.eye_pos = mul(vPos, (float4x3)g_matWorldView);
    o.rust_uv = vPos.yz*0.006;
    o.rustmap_uv = float2(-vPos.z*0.000345 + 0.495,0.945-vPos.y*0.000345+  vPos.z*0.00001);//vTex;
    o.model_z = vPos.y - vPos.z*vPos.z*0.00005 + vPos.z*0.05 - 80.0;
}