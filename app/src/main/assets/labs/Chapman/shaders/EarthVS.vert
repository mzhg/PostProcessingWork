layout(location = 0) in vec3 In_Position;
layout(location = 1) in vec3 In_Normal;
layout(location = 2) in vec2 In_Texcoord;

uniform mat4 g_ModelViewProj;
uniform mat3 g_NormalMat;

out vec2 m_Texcoord;
out vec3 m_Normal;

void main()
{
    m_Texcoord = In_Texcoord;
    m_Normal = g_NormalMat * In_Normal;

    gl_Position = g_ModelViewProj * vec4(In_Position, 1);
}