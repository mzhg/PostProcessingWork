layout(location = 0) in vec3 In_Position;

uniform mat4 g_ModelViewProj;
uniform mat4 g_Model;

out vec3 m_PositionWS;

void main()
{
    m_PositionWS = vec3(g_Model * vec4(In_Position, 1));
    gl_Position = g_ModelViewProj * vec4(In_Position, 1);
}