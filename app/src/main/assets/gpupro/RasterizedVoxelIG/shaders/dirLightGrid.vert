#include "globals.glsl"

layout(location = 0) in vec3 In_Position;
layout(location = 1) in vec2 In_Texcoord;

/*struct VS_INPUT
{
  float3 position: POSITION;
  float2 texCoords: TEXCOORD;
};*/

out VS_OUTPUT
{
	float4 position/*: SV_POSITION*/;
	flat int instanceID/*: INSTANCE_ID*/;
}_output;

//VS_OUTPUT main(VS_INPUT input,uint instanceID: SV_InstanceID)
void main()
{
    _output.position = float4(In_Position,1.0f);  // todo
	_output.instanceID = instanceID;
}
