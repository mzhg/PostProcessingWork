layout(location = 0) in vec4 In_Position;
layout(location = 1) in vec2 In_Texcoord;

out vec2 m_Tex;

out gl_PerVertex
{
    vec4 gl_Position;
};

void main()
{
    m_Tex = In_Texcoord;
    gl_Position = In_Position;
}