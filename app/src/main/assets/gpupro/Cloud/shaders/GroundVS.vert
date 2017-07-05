#include "Cloud_Common.glsl"
layout(location = 0) in float4 In_Pos;
layout(location = 1) in float4 In_Tex;
layout(location = 2) in float3 In_Normal;

out SVSOutput
{
    float4 vTex;//         : TEXCOORD0;
    float3 vWorldNormal;// : TEXCOORD1;
    float3 vWorldPos;//    : TEXCOORD2;
    float4 vShadowPos;//   : TEXCOORD3;
}_Output;

void main()
{
    // projection
    gl_Position = mul( In_Pos, mL2C);

    // compute world normal vector
    _Output.vWorldNormal = mul( In_Normal, float3x3(mL2W) );
    // compute world position
    _Output.vWorldPos = mul( In_Pos, mL2W ).xyz;

    // shadowmap position
    _Output.vShadowPos = mul( In_Pos, mL2S);

    // copy uvs
    _Output.vTex = In_Tex;
}