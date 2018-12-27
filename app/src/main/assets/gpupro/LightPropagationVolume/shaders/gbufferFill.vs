#version 430

//Location is number from brackets from commnad glEnableVertexAttribArray (n);
layout(location = 0) in vec3 vPosition;
layout(location = 1) in vec2 vUv;
layout(location = 2) in vec3 vNormal;

uniform mat4 mvp;
uniform mat4 mv;
uniform mat3 mn;

//out vec3 v_viewPos;
out vec2 v_tc;
out vec3 v_viewNormal;

void main()
{
	//v_viewPos = (mv * vec4(vPosition,1.0)).xyz;
	v_tc = vUv;
	v_viewNormal = normalize(mn*vNormal);
	gl_Position = mvp * vec4(vPosition,1.0);
}