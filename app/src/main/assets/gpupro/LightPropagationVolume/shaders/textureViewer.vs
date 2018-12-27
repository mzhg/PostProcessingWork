#version 430

layout(location = 0) in vec2 vPosition;
layout(location = 1) in vec2 vUV;

out vec2 uv;

void main()
{
	gl_Position = vec4(vPosition,1.0,1.0);
	uv = vUV;
}