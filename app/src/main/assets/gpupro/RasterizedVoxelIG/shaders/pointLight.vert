#include "globals.glsl"

layout(location = 0) in vec3 In_Position;

out VS_OUTPUT
{
//	float4 position: SV_POSITION;
	float4 screenPos/*: SCREEN_POS*/;
	float3 viewRay/*: VIEW_RAY*/;
}_output;

//VS_OUTPUT main(VS_INPUT input)
void main()
{
	float4 positionWS = mul(pointLightUB.worldMatrix,float4(In_Position,1.0f));
	gl_Position = mul(cameraUB.viewProjMatrix,positionWS);
    _output.screenPos = gl_Position;
	_output.screenPos.xy = (float2(_output.screenPos.x,-_output.screenPos.y)+_output.screenPos.ww)*0.5f;
    _output.viewRay = positionWS.xyz-cameraUB.position;
}