#version 330

layout (location = 0) in vec3 a_pos;

uniform mat4 uMVP;
uniform float pointSize = 2.0f;

void main()
{
	gl_Position = uMVP * vec4(a_pos, 1.0);
	gl_PointSize = pointSize;
}