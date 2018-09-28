layout(location = 0) out vec4 Out_Color;

uniform sampler2D g_Texture;

in vec2 m_Texcoord;
in vec3 m_Normal;

void main()
{
    Out_Color = texture(g_Texture, m_Texcoord);
}