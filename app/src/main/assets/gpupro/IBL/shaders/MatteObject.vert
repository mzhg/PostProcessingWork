layout(location = 0) in vec3 In_Position;
layout(location = 1) in vec3 In_Normal;

uniform mat3 g_NormalMat;
uniform mat4 g_World;
uniform mat4 g_ViewProj;

out vec4 m_PositionWS;
out vec3 m_Normal;

void main()
{
    m_PositionWS = g_World * vec4(In_Position, 1);
    gl_Position = g_ViewProj * m_PositionWS;
    m_Normal = g_NormalMat * In_Normal;
}