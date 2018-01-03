layout(location = 0) vec4 Position;

out vec4 m_Position;

void main()
{
    gl_Position =  Position;
    m_Position = Position;
}