#version 300 es

in vec3 PosAttribute;
uniform mat4 viewMatrix;
uniform mat4 ProjMatrix;
out vec3 TexCoord;

void main()
{
	TexCoord  = mat3(viewMatrix) * vec3(PosAttribute.xyz);
	gl_Position = ProjMatrix * vec4(PosAttribute, 1.0);
}