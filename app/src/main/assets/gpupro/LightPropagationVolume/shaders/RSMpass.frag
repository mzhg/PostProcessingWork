#version 430

in vec4 pos;
in vec3 worldNormal;
in vec2 uv;
//in vec3 viewNormal;

uniform sampler2D matTex;
uniform vec3 v_lightPos;

layout(location=0) out vec4 worldPos;
layout(location=1) out vec4 normal;
layout(location=2) out vec4 flux;

void main() {

	vec3 lightColor = vec3(1.0,1.0,1.0);
	vec3 diffuse = texture(matTex,uv).rgb;
    vec3 lightDir = normalize( v_lightPos - pos.xyz );

	worldPos = pos;
	normal = vec4(worldNormal,1.0);

	flux = vec4(lightColor * diffuse * clamp( dot( lightDir, worldNormal ),0.0,1.0 ), 1.0);

}