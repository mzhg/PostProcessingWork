layout(location = 0) in vec4 In_Position;
layout(location = 1) in vec4 In_Texcoord0;
layout(location = 2) in vec4 In_Texcoord1;
layout(location = 3) in vec3 In_Texcoord2;

out vec4 m_tex1;
out vec4 m_tex2;
out vec3 m_tex3;

void main()
{
    m_tex1 = In_Texcoord0;
    m_tex2 = In_Texcoord1;
    m_tex3 = In_Texcoord2;

    gl_Position = In_Position;
}