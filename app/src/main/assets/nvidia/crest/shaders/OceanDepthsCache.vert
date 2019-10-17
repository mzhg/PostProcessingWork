#include "OceanLODData.glsl"

layout(location = 0) in float4 In_Position;
layout(location = 1) in float2 In_UV;

out float2 uv;

void main()
{
    gl_Position = UnityObjectToClipPos(In_Position);
    uv = In_UV;
}