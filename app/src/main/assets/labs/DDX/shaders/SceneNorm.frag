#version 450

in vec3 v_Pos;
in vec3 v_Normal;

layout(location = 0) out vec4 OutNormal;
layout(location = 1) out vec4 OutNormalDDX;
layout(location = 2) out vec4 OutNormalDDY;


void main()
{
	OutNormal = vec4(v_Normal, 0.0);
	OutNormalDDX = dFdxFine(vec4(v_Normal, 0.0));
	OutNormalDDY = dFdyFine (vec4(v_Normal, 0.0));
}