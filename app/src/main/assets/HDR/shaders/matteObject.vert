#version 300 es

in vec3 PosAttribute;
in vec3 myNormal;
in vec2 uvTexCoord;

uniform mat4 viewProjMatrix;
uniform mat4 ModelMatrix;
uniform vec3 eyePos;
out vec4 Position;
out vec3 Normal;
out vec3 IncidentVector;
out vec2 texcoord;
void main()
{
   vec4 P = ModelMatrix * vec4(PosAttribute, 1.0);
   vec3 N = normalize(mat3(ModelMatrix) * myNormal);
   vec3 I = P.xyz - eyePos;
   Position = P;
   Normal = N;
   IncidentVector = I;
   texcoord = uvTexCoord;
   gl_Position = viewProjMatrix * P;
}