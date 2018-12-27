#version 430

//For MRT attachment 0
layout(location = 0) out vec4 color;
layout(location = 1) out vec4 normals;
layout(location = 2) out float fragmentdepth;

uniform sampler2D colorTex;

in vec3 v_viewPos;
in vec2 v_tc;
in vec3 v_viewNormal;

void main()
{
	fragmentdepth = gl_FragCoord.z;
	//position = vec4(v_viewPos,1.0);
	color = texture(colorTex,v_tc);
	normals = vec4(v_viewNormal,1.0);
}