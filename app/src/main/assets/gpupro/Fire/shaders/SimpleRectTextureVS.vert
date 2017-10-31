layout(location = 0) in vec4 In_Position;
layout(location = 1) in vec2 In_Texcoord;

uniform mat4 g_MVP;
out vec2 m_Texcoord;

void main()
{
    m_Texcoord = In_Texcoord;
    gl_Position = g_MVP * In_Position;
}