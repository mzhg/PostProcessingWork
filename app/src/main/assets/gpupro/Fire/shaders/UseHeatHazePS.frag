layout(location = 0) out vec4 Out_f4Color;

in vec2 m_tex0;

layout(binding = 0) uniform sampler2D inputToDist;
layout(binding = 1) uniform sampler2D blurredInputToDist;
layout(binding = 2) uniform sampler2D distortionMap;

void main()
{
    float3 distVec = texture( distortionMap, m_tex0).xyz;
    float2 tex = m_tex0 + 0.25*distVec.xy;
    float3 color1 = texture( inputToDist, tex );
    float3 color2 = texture( blurredInputToDist, tex );
    float x = saturate(distVec.b);

    Out_f4Color = /*color1*(1.0-x)+color2*x*/ mix(color1, color2, x);
}