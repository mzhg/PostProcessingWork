layout(location = 0) in vec4 In_Position;
layout(location = 1) in vec3 In_Texcoord;

out VS_PROP_OUTPUT
{
 vec3 m_Tex;
 vec4 m_Pos;
}_output;

void main()
{
    _output.m_Tex = In_Texcoord;
    _output.m_Pos = In_Position;
}