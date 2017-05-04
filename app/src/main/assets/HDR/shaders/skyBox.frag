#version 300 es
precision highp float;
in vec3 TexCoord;
uniform samplerCube envMap;

out vec4 FragColor;

void main()
{
	FragColor = texture(envMap, TexCoord);
	FragColor.a = gl_FragCoord.z;
}