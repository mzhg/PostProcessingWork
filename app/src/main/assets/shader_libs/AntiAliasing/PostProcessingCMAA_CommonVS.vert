
uniform float fZValue = 0.0;

void main()
{
    int idx = gl_VertexID % 3;
    vec2 m_f4UVAndScreenPos = vec2((idx << 1) & 2, idx & 2);
    gl_Position = vec4(m_f4UVAndScreenPos.xy * 2.0 - 1.0, fZValue, 1);
}