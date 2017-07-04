#include "Cloud_Common.glsl"
layout(location = 0) in float4 In_Pos;
layout(location = 1) in float2 In_Tex;

out SVSOutput
{
    float2 vTex;
    float4 vWorldPos;
}_output;

void main()
{
    gl_Position = In_Pos;

    // transform projection space to world space
    _output.vWorldPos = mul( In_Pos, mC2W );
    // uv
    _output.vTex = In_Tex;
}