in vec2 m_texcoord;

out vec4 Out_Color;

layout(binding = 1) uniform sampler2D star_tex;

void main()
{
    Out_Color = texture(star_tex, m_texcoord);
}