#version 110
attribute vec3 a_Position;

uniform mat4 u_ViewProjMatrix;

void main() 
{
    gl_Position = u_ViewProjMatrix * vec4(a_Position, 1.0);
}
