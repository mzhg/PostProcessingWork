layout(location = 0) in vec4 In_Position;

uniform mat4 g_View;
uniform mat4 g_Proj;

out vec3 m_Normal;

void main()
{
    vec4 worldPos = g_View * In_Position;
    gl_Position = g_Proj * worldPos;
    m_Normal = In_Position.xyz;
}