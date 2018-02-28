#version 330

layout (location = 0) in vec4 a_pos;
//layout (location = 1) in float radius;

out vec3 vs_pos;
flat out float vs_radius;

void main()
{
	vs_pos = a_pos.xyz;
	vs_radius = a_pos.w;
}

