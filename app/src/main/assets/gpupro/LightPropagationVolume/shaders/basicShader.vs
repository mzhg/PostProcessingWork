#version 430

//Location is number from brackets from commnad glEnableVertexAttribArray (n);
layout(location = 0) in vec3 vPosition;
layout(location = 1) in vec2 vUv;
layout(location = 2) in vec3 vNormal;

out vec3 eyePosition,eyeNormal,eyeLightPos;
out vec2 uv;
out vec4 shadowCoord;
out vec3 worldPos, worldNorm;

uniform mat4 mvp;
uniform mat3 mn;
uniform mat4 mv;
uniform mat4 v;
uniform vec3 vLightPos;
uniform mat4 shadowMatrix;

void main()
{
	gl_Position = mvp * vec4(vPosition,1.0);
	eyePosition = (mv*vec4(vPosition,1)).xyz;
	eyeNormal = normalize(mn*vNormal);
	worldNorm = normalize(vNormal);
	worldPos = vPosition;
	eyeLightPos = (v*vec4(vLightPos,1)).xyz;
	uv = vUv;
	shadowCoord = (shadowMatrix * vec4(vPosition,1.0));
}