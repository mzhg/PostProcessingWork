#version 330

layout (location = 0) in vec3 a_pos;
layout (location = 1) in float radius;

out vec3 vs_pos;
out float vs_radius;

void main()
{
	vs_pos = a_pos;
	vs_radius = radius;
}

