#version 430

//Location is number from brackets from commnad glEnableVertexAttribArray (n);
layout(location = 0) in vec3 vPosition;
layout(location = 1) in vec2 vUV;
layout(location = 2) in vec3 vNormal;

uniform mat4 mvp;
uniform mat4 m;
//uniform mat3 mn;

out vec4 pos;
out vec3 worldNormal;
out vec2 uv;
//out vec3 viewNormal;

void main()
{
	gl_Position = mvp * vec4(vPosition,1.0);
	pos = m * vec4(vPosition,1.0);
	uv = vUV;
	worldNormal = normalize(vNormal);
	//viewNormal = normalize(mn*vNormal);
	//eyeNormal = normalize ( ( transpose( inverse( m ) ) * vec4(vNormal,1.0) ).xyz );
}