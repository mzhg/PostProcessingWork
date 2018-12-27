#version 430

//For MRT attachment 0
layout(location = 0) out vec4 final_color;
uniform vec3 color;

void main()
{
	final_color = vec4(color,1.0);
	
}