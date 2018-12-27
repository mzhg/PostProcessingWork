#version 430

//Location is number from brackets from commnad glEnableVertexAttribArray (n);
layout(location = 0) in vec3 vPosition;

uniform mat4 vp;

void main()
{
	gl_Position = vp * vec4(vPosition,1.0);
}