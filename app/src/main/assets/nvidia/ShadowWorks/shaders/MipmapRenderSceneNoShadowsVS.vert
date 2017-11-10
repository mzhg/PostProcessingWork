#include "MipmapSoftShadowCommon.glsl"

layout(location = 0) in float3 vPos;// : POSITION; ///< vertex position
layout(location = 1) in float3 vNorm;// : NORMAL; ///< vertex diffuse color (note that COLOR0 is clamped from 0..1)
layout(location = 2) in float2 vTCoord;// : TEXCOORD0; ///< vertex texture coords

out VS_OUT1
{
//    float4 vPos : SV_Position; ///< vertex position
    float4 vDiffColor;// : TEXCOORD0; ///< vertex diffuse color (note that COLOR0 is clamped from 0..1)
    float2 vTCoord;// : TEXCOORD1; ///< vertex texture coords
    float3 vNorm;// : TEXCOORD2;
}outvert;

void main()
{
    // transform the position from object space to clip space
    gl_Position = mul(float4(vPos, 1), mViewProj);
    outvert.vNorm = vNorm;
    outvert.vTCoord = vTCoord;
}