#include "globals.glsl"

layout(location = 0) in vec3 In_Position;

//VS_OUTPUT main(VS_INPUT input)
void main()
{
	gl_Position = mul(dirLightUB.shadowViewProjMatrix,float4(In_Position,1.0f));
}
