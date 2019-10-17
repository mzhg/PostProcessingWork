#include "OceanLODData.glsl"

layout(location = 0) in float4 In_Position;
layout(location = 2) in float3 In_Normal;

uniform float _Speed;
uniform float _Direction;

out float2 vel;

void main()
{
    gl_Position = UnityObjectToClipPos(In_Position);
    vel = _Speed * float2(cos(_Direction * 6.283185), sin(_Direction * 6.283185));
}