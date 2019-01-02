#include "globals.glsl"

layout(location = 0) in vec3 In_Position;
layout(location = 1) in vec2 In_Texcoord;

out VS_OUT
{
    float4  position;
    float2 texCoords/*: TEXCOORD*/;
}_output;

void main()
{
    float4 positionVS = mul(cameraUB.viewMatrix,float4(In_Position,1.0f));
    gl_Position = mul(cameraUB.projMatrix,positionVS);
    _output.position = gl_Position / gl_Position.w;
    _output.texCoords = In_Texcoord;
}