#include "globals.glsl"

/*struct VS_INPUT
{
  float3 position: POSITION;
  float2 texCoords: TEXCOORD;
  float3 normal: NORMAL;
  float4 tangent: TANGENT;
};*/

layout(location = 0) in vec3 In_Position;
layout(location = 1) in vec2 In_Texcoord;
layout(location = 2) in vec3 In_Normal;
layout(location = 3) in vec4 In_Tangent;

out VS_OUTPUT
{
	float4 position/*: SV_POSITION*/;
    float2 texCoords/*: TEXCOORD*/;
	float3 normal/*: NORMAL*/;
}_output;

//VS_OUTPUT main(VS_INPUT input)
void main()
{
	_output.position = float4(In_Position,1.0f);
	_output.texCoords = In_Texcoord;
	_output.normal = In_Normal;
}

