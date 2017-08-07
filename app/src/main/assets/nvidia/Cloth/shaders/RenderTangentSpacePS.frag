
out vec3 m_Color;
layout(location = 0) out vec4 Out_f4Color;

void main()
{
    Out_f4Color = float4(m_Color,1);
}