#version 430

layout(location = 0) in vec3 vPosition;

flat out ivec3 cellIndex;
uniform vec3 v_gridDim;

void main()
{
	cellIndex = ivec3(vPosition);
	vec2 screenPos = (vec2(vPosition.xy) + 0.5) / v_gridDim.xy * 2.0 - 1.0;
	gl_Position = vec4(screenPos , 0.0,1.0);
}