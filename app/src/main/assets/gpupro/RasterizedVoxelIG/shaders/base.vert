#include "globals.glsl"

layout(location = 0) in vec3 In_Position;
layout(location = 1) in vec2 In_Texcoord;
layout(location = 2) in vec3 In_Normal;
layout(location = 3) in vec4 In_Tangent;

/*struct VS_INPUT
{
  float3 position: POSITION;
  float2 texCoords: TEXCOORD;
  float3 normal: NORMAL;
  float4 tangent: TANGENT;
};*/

out VS_OUTPUT
{
//	float4 position: SV_POSITION;
    float2 texCoords/*: TEXCOORD*/;
	float3 posVS/*: POS_VS*/;
	float3 normal/*: NORMAL*/;
	float3 tangent/*: TANGENT*/;
	float3 binormal/*: BINORMAL*/;
}_output;

//VS_OUTPUT main(VS_INPUT input)
void main()
{
    float4 positionVS = mul(cameraUB.viewMatrix,float4(In_Position,1.0f));
    gl_Position = mul(cameraUB.projMatrix,positionVS);
    _output.texCoords = In_Texcoord;
    _output.posVS = positionVS.xyz;

    _output.normal = In_Normal;
	_output.tangent = In_Tangent.xyz;
	_output.binormal = cross(In_Normal,In_Tangent.xyz)*In_Tangent.w;
}

