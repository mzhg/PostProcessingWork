#version 400

layout(location = 0) in vec4 Position;
layout(location = 1) in vec3 Normal;

uniform mat4 u_MVP;
uniform mat3 u_Norm;
out vec3 v_Pos;
out vec3 v_Normal;

void main()
{
	v_Pos = Position.xyz;
	v_Normal = u_Norm * Normal;
	gl_Position = u_MVP * Position;
}