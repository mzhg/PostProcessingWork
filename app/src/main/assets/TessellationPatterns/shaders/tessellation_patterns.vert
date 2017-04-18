#version 330 core

layout( location = 0) in vec3 f3Position;

void main()
{
   gl_Position = vec4(f3Position, 1);
}