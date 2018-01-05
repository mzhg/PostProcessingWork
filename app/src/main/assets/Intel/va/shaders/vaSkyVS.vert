layout(location = 0) in vec4 Position;

out vec4 m_Position;

out gl_PerVertex
{
    vec4 gl_Position;
};

void main()
{
    gl_Position =  Position;
    m_Position = Position;
}