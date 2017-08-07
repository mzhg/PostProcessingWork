#include "Cloth.h"

layout(location = 0) in uint In_State;
layout(location = 1) in vec3 In_Position;
layout(location = 2) in vec3 In_Normal;
layout(location = 3) in vec3 In_TangentX;
layout(location = 4) in vec2 In_Texcoord;

out float4 Position;

uniform mat4 Transform;
uniform mat4 ViewProjection;

out TangentSpace
{
    float3 Position /*: Position*/;
    float3 Normal /*: Normal*/;
    float3 TangentX /*: TangentX*/;
    float3 TangentY /*: TangentY*/;
}tangentSpace;

void main()
{
    tangentSpace.Position = In_Position;
    tangentSpace.Normal = In_Normal;
    tangentSpace.TangentX = In_TangentX;
    tangentSpace.TangentY = cross(tangentSpace.Normal, tangentSpace.TangentX);
}