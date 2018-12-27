#version 430

layout(location = 0) in vec3 vPosition;

flat out ivec3 cellIndex;

void main()
{
	cellIndex = ivec3(vPosition);
	gl_Position = vec4(0.0,0.0,0.0,1.0); //[0,0,0] because vPosition is not in NDC!!
}