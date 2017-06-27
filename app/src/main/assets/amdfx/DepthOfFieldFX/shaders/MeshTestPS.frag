#include "../../../shader_libs/PostProcessingHLSLCompatiable.glsl"

in float3 m_worldPos;
in float3 m_Normal;
in float2 m_TexCoord;

layout(binding = 0) uniform sampler2D g_t2dDiffuse;
layout(location = 0) out float4 Out_f4Color;

void main()
{
    float4 texColor = float4(1, 1, 1, 1);  //
    texColor        = texture(g_t2dDiffuse, m_TexCoord);

    Out_f4Color = max(vec4(0), texColor);
}