#include "Rain_Common.glsl"

layout(location = 0) in vec2 In_Pos;

out float3 m_worldPos;

void main()
{
    gl_Position =  float4(In_Pos.xy, 1.0, 1.0);
//    float4 unprojectedPos = mul( float4( In_Pos.xy, 0, 1 ), g_mInverseProjection );
//    unprojectedPos.xy *= g_Near;
//    unprojectedPos.z = g_Near;
//    unprojectedPos.w = 1; // TODO
//    m_worldPos = mul(unprojectedPos, g_mInvView).xyz;
    float4x4 viewProjInv = g_mInvView * g_mInverseProjection;
    float4 worldPos = viewProjInv * float4( In_Pos.xy, -1, 1 );  // near plane
    m_worldPos = worldPos.xyz/worldPos.w;
}