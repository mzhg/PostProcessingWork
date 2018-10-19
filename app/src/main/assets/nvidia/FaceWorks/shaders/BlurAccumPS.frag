#include "SSSS_Common.glsl"

layout(binding=1) uniform sampler2D tex2;
layout(binding=2) uniform sampler2D depthTex;

layout(location=0) out float4 gaussian;
layout(location=1) out float4 final;

in vec4 m_f4UVAndScreenPos;

void main()
{
    float w[7] = float[7](
        0.006,
        0.061,
        0.242,
        0.382,
        0.242,
        0.061,
        0.006
    );

    const float sssLevel = 1.;
    const float2 pixelSize = 1.0/float2(1280, 720);
    const float width = 3.;
    const float maxdd = 0.001;
    const float correction = 1000.;

    float2 step = sssLevel * width * pixelSize * float2(0.0, 1.0);

    float4 color = texture(tex2, m_f4UVAndScreenPos.xy);   // LinearSampler
    color.rgb *= w[3];

    float depth = textureLod(depthTex, m_f4UVAndScreenPos.xy, 0.).r;   // PointSampler
    float2 s_y = float2(sssLevel / (depth + correction * min(abs(ddy(depth)), maxdd)));
//    float2 finalWidth = width * s_y * pixelSize * float2(0.0, 1.0); // step = sssLevel * width * pixelSize * float2(0.0, 1.0)
    float2 finalWidth = width * s_y * pixelSize * float2(.0, 1.0);

    float2 offset = m_f4UVAndScreenPos.xy - finalWidth;
//    [unroll]
    for (int i = 0; i < 3; i++) {
        color.rgb += w[i] * texture(tex2, offset).rgb;
        offset += finalWidth / 3.0;
    }
    offset += finalWidth / 3.0;
//    [unroll]
    for (int i = 4; i < 7; i++) {
        color.rgb += w[i] * texture(tex2, offset).rgb;
        offset += finalWidth / 3.0;
    }

    gaussian = color;
    final = color;
}