in vec4 m_f4UVAndScreenPos;
out vec4 Out_Color;

uniform float g_BufferWidthInv;
uniform float g_BufferHeightInv;

layout(binding = 0) uniform sampler2D s0;
layout(binding = 1) uniform sampler2D s1;
layout(binding = 2) uniform sampler2D s2;

void main()
{
    vec4 vColor = texture(s0,  m_f4UVAndScreenPos.xy );   // samplerPointClamp
    float vLum = texture(s1, vec2(0,0) ).r;               // samplerPointClamp
    vec3 vBloom = texture(s2,  m_f4UVAndScreenPos.xy ).rgb;  // samplerLinearClamp

    // Tone mapping
    vColor.rgb *= MIDDLE_GRAY / ( vLum + 0.001f );
    vColor.rgb *= ( 1.0f + vColor.rgb/LUM_WHITE );
    vColor.rgb /= ( 1.0f + vColor.rgb );

    vColor.rgb += 0.6f * vBloom;
    vColor.a = 1.0f;

    Out_Color = vColor;
}