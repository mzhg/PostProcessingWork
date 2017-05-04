#version 300 es
in vec3 PosAttribute;
in vec3 myNormal;

uniform mat4 viewProjMatrix;   
uniform vec3 eyePos;

out vec4 Position;
out vec3 Normal;
out vec3 IncidentVector;

void main()
{
   vec4 P = vec4(PosAttribute, 1.0);
   vec3 N = normalize(myNormal);
   vec3 I = P.xyz - eyePos;
   Position = P;
   Normal = N;
   IncidentVector = I;
   gl_Position = viewProjMatrix * P;
}