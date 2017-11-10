#include "MipmapSoftShadowCommon.glsl"

layout(location = 0) in float3 vPos;// : POSITION; ///< vertex position
layout(location = 1) in float3 vNorm;// : NORMAL; ///< vertex diffuse color (note that COLOR0 is clamped from 0..1)
layout(location = 2) in float2 vTCoord;// : TEXCOORD0; ///< vertex texture coords

out VS_OUT0
{
//    float4 vPos; : SV_Position; ///< vertex position
    float4 vDiffColor;// : COLOR0; ///< vertex diffuse color (note that COLOR0 is clamped from 0..1)
    float2 vTCoord;// : TEXCOORD0; ///< vertex texture coords
    float4 vLightPos;// : TEXCOORD2;
}outvert;

void main()
{
    // transform the position from object space to clip space
    gl_Position = mul(float4(vPos, 1), mViewProj);
    outvert.vLightPos = mul(float4(vPos, 1), mLightView);
    // compute light direction
    float3 vLightDir = normalize(g_vLightPos - vPos);
    // compute lighting
    outvert.vDiffColor = (g_vMaterialKd * g_vLightFlux);
    outvert.vDiffColor.xyz *= max(0, dot(vNorm, vLightDir));
    outvert.vTCoord = vTCoord;
}