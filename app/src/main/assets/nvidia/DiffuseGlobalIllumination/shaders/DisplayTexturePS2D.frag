
in vec2 m_Tex;
out vec4 Out_Color;

layout(binding = 0) uniform sampler2D g_textureToDisplay2D;

void main()
{
//float4 col = saturate(g_textureToDisplay3D.Sample( samLinear, float3(f.Tex.xyz)) );
    vec4 col = texture(g_textureToDisplay2D, m_Tex);
    Out_Color = clamp(col, vec4(0), vec4(1));
}