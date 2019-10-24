in vec4 m_f4UVAndScreenPos;


uniform vec4 g_UVOffsetBlur;
uniform float g_FadeAmount;
layout(binding = 0) uniform sampler2D g_texLocalFoamSource;

layout(binding = 0) uniform sampler2DMS g_texDepthMS;

out vec4 OutColor;
void main()
{
    vec2 UVOffset = g_UVOffsetBlur.xy;
    vec2 BlurUV = g_UVOffsetBlur.zw;
    float  Fade = g_FadeAmount;

    // blur with variable size kernel is done by doing 4 bilinear samples,
    // each sample is slightly offset from the center point
    float foam1	= texture(g_texLocalFoamSource, In.uv + UVOffset + BlurUV).r;    //g_samplerTrilinearClamp
    float foam2	= texture(g_texLocalFoamSource, In.uv + UVOffset - BlurUV).r;
    float foam3	= texture(g_texLocalFoamSource, In.uv + UVOffset + BlurUV*2.0).r;
    float foam4	= texture(g_texLocalFoamSource, In.uv + UVOffset - BlurUV*2.0).r;
    float sum = min(5.0,(foam1 + foam2 + foam3 + foam4)*0.25*Fade); // added clamping to 5
    OutColor = vec4(sum,sum,sum,sum);
}