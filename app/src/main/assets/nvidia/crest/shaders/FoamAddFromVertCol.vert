#include "OceanLODData.glsl"

layout(location = 0) in float4 In_Position;
layout(location = 1) in float4 In_Color;

out float4 color;

void main()
{
    gl_Position = UnityObjectToClipPos(In_Position);
    color = In_Color;
}