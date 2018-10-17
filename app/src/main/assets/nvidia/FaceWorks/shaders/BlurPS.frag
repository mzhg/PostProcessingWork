#include "SSSS_Common.glsl"

layout(binding=0) uniform sampler2D tex1;
layout(binding=2) uniform sampler2D depthTex;

layout(location=0) out float4 Out_Color;

in vec4 m_f4UVAndScreenPos;

void main()
{
    const float w[7] = float[7](
            0.006,
            0.061,
            0.242,
            0.382,
            0.242,
            0.061,
            0.006
    );

    float2 step = sssLevel * width * pixelSize * float2(1.0, 0.0);

    float4 color = texture(tex1, m_f4UVAndScreenPos.xy);    // LinearSampler
    color.rgb *= w[3];

    float depth = texture(depthTex, m_f4UVAndScreenPos.xy).r;    // PointSampler
    float2 s_x = step / (depth + correction * min(abs(ddx(depth)), maxdd));
    float2 finalWidth = color.a * s_x; // step = sssLevel * width * pixelSize * float2(1.0, 0.0)

    float2 offset = m_f4UVAndScreenPos.xy - finalWidth;
//    [unroll]
    for (int i = 0; i < 3; i++) {
        color.rgb += w[i] * texture(tex1, offset).rgb;   // LinearSampler
        offset += finalWidth / 3.0;
    }
    offset += finalWidth / 3.0;
//    [unroll]
    for (i = 4; i < 7; i++) {
        color.rgb += w[i] * texture(tex1, offset).rgb;
        offset += finalWidth / 3.0;
    }

    Out_Color = color;
}