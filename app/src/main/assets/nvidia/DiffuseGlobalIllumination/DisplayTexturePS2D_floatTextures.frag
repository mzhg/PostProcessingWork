
in vec2 m_Tex;
out vec4 Out_Color;

layout(binding = 0) uniform sampler2D g_textureToDisplay2D;
layout(binding = 1) uniform sampler2D g_textureToDisplay2D_1;
layout(binding = 2) uniform sampler2D g_textureToDisplay2D_2;
layout(binding = 3) uniform sampler2D g_textureToDisplay2D_3;

void main()
{
//float4 col = saturate(g_textureToDisplay3D.Sample( samLinear, float3(f.Tex.xyz)) );
    Out_Color.x = texture(g_textureToDisplay2D,   m_Tex).x;
    Out_Color.y = texture(g_textureToDisplay2D_1, m_Tex).x;
    Out_Color.z = texture(g_textureToDisplay2D_2, m_Tex).x;
    Out_Color.w = texture(g_textureToDisplay2D_3, m_Tex).x;
}