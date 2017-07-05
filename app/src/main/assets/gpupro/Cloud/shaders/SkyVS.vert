#include "Cloud_Common.glsl"
layout(location = 0) in float4 In_Pos;

out float4 vWorldPos;

void main()
{
    gl_Position = In_Pos;
    vWorldPos = mul( In_Pos, mC2W );
}