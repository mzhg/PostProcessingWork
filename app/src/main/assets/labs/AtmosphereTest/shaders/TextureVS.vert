layout(location = 0) in vec3 In_Position;
layout(location = 1) in vec2 In_Texcoord;

uniform mat4  uMVP;

out vec3 vSecondColor;
out vec2 vTexcoord;

void main()
{
    gl_Position = uMVP * vec4(In_Position, 1);
    vTexcoord.st = In_Texcoord.st;
    vSecondColor = vec3(1);
}