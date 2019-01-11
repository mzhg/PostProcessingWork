in vec4 m_f4UVAndScreenPos;

layout(binding = 0) uniform sampler2D g_VelocityHeightMap;

out vec4 Out_Color;

void main()
{
    float h0 = textureLodOffset(g_VelocityHeightMap, m_f4UVAndScreenPos.xy, 0.0, ivec2(1, 0) ).y;
    float h1 = textureLodOffset(g_VelocityHeightMap, m_f4UVAndScreenPos.xy, 0.0, ivec2(0, 1) ).y;
    float h2 = textureLodOffset(g_VelocityHeightMap, m_f4UVAndScreenPos.xy, 0.0, ivec2(-1, 0) ).y;
    float h3 = textureLodOffset(g_VelocityHeightMap, m_f4UVAndScreenPos.xy, 0.0, ivec2(0, -1) ).y;

    Out_Color = vec4(h2 - h0, h1-h3, 0,0);
}