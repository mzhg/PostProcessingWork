#include "globals.glsl"

/*struct VS_INPUT
{
  float3 position: POSITION;
  float2 texCoords: TEXCOORD;
};*/

layout(location = 0) in vec3 In_Position;
layout(location = 1) in vec2 In_Texcoord;

out VS_OUTPUT
{
	float4 position/*: SV_POSITION*/;
  float2 texCoords/*: TEXCOORD*/;
}_output;

//VS_OUTPUT main(VS_INPUT input)
void main()
{
	_output.position = float4(In_Position,1.0f);
	_output.texCoords = In_Texcoord;
}
