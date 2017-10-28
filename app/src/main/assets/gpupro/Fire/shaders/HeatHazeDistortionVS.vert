layout(location = 0) in vec4 In_Position;
layout(location = 1) in vec4 In_Texcoord;

out vec2 m_tex0;
out float m_tex1;

uniform mat4 g_ModelViewProj;

void main()
{
    gl_Position = g_ModelViewProj * In_Position;
    m_tex0 = In_Texcoord.xy;
    m_tex1 = In_Texcoord.z;
}
