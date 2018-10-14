#include "SSSS_Common.glsl"

out vec4 m_f4UVAndScreenPos;
void main()
{
    int idx = gl_VertexID % 3;  // allows rendering multiple fullscreen triangles
    m_f4UVAndScreenPos.xy = vec2((idx << 1) & 2, idx & 2);
    gl_Position = vec4(m_f4UVAndScreenPos.xy * 2.0 - 1.0, depth, 1);
    m_f4UVAndScreenPos.zw = gl_Position.xy;
}