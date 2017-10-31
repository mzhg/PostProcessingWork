
in vec2 m_Texcoord;
out vec4 Out_Color;

layout(binding = 0) uniform sampler2D g_InputTex;

void main()
{
    Out_Color = texture(g_InputTex, m_Texcoord);
}