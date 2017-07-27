#version 400

layout(location = 0) in vec4 position;
layout(location = 1) in vec2 texcoord;


out PSIn_Diffuse
{
	vec2 texcoord;
	vec3 positionWS;
}_output;

uniform mat4 g_ModelViewProjectionMatrix;

void main()
{
	gl_Position = g_ModelViewProjectionMatrix * position;
	
	_output.positionWS = gl_Position.xyz;
	_output.texcoord = texcoord;
}