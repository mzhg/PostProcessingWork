#include "Cloud_Common.glsl"
layout(location = 0) in float4 In_Pos;
layout(location = 1) in float2 In_Tex;

out SVSOutput
{
    float4 vTex;
    float4 vWorldPos;
}_output;

void main()
{
    gl_Position = float4(In_Pos.xy, 0, 1);

    // transform projection space to world space
    _output.vWorldPos = mul( _Input.vPosC, mC2W );
    // uv
    _output.vTex.xy = In_Tex + vPix;

    // compute blur direction
    _output.vTex.zw = In_Tex * vOff.xy + vOff.zw;
}