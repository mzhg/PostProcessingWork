layout(location = 0) in vec3 In_Position;
layout(location = 1) in vec4 In_Color;

out vec4 m_Color;

void main()
{
    gl_Position = vec4(In_Position, 1);
    m_Color = In_Color;
}