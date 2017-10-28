layout(location = 0) out vec4 Out_f4Color;

in vec2 m_tex0;
in float m_tex1;

layout(binding = 0) uniform sampler2D baseDistMap;

void main()
{
    Out_f4Color = texture(baseDistMap, m_tex0);
    Out_f4Color.a = m_tex1;
}