

//float4 RenderClothPositionVS(Particle particle, out float4 Position : Position) : SV_Position

#include "Cloth.h"

layout(location = 0) in uint In_State;
layout(location = 1) in vec3 In_Position;

out float4 Position;

uniform sampler2D Transform;
uniform sampler2D ViewProjection;

void main()
{
    Position = mul(float4(In_Position, 1), Transform);
    Position.xyz += 0.1 * normalize(Position.xyz);
    gl_Position =  mul(float4(In_Position, 1), ViewProjection);
}