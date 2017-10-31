layout(location = 0) in vec4 In_Position;
layout(location = 1) in vec2 In_Texcoord;

out vec2 m_tex0;

void main()
{
    m_tex0 = In_Texcoord;
    gl_Position = In_Position;
}