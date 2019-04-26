layout(location = 0) in vec3 In_Position;
layout(location = 1) in vec3 In_Normal;

uniform mat3 g_NormalMat;
uniform mat4 g_MVP;

out vec3 m_Normal;

void main()
{
    gl_Position = g_MVP * vec4(In_Position, 1);
    m_Normal = g_NormalMat * In_Normal;
}