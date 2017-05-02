#include "PostProcessingCommonPS.frag"

uniform sampler2D g_Texture;
// 1/RenderTargetWidth, 1/RenderTargetHeight, see Fxaa.. header for more details (used for VS and PS)
uniform vec2 g_f2FrameRcpFrame;


// Calculates the luminosity of a sample.
float FxaaLuma(vec3 rgb) { return rgb.y * (0.587/0.299) + rgb.x; }

void main()
{
    float FXAA_SPAN_MAX = 8.0;
    float FXAA_REDUCE_MUL = 1.0/8.0;
    float FXAA_REDUCE_MIN = 1.0/128.0;

    vec2 TexCoord = m_f4UVAndScreenPos.xy;

    // Sample 4 texels including the middle one.
    // Since the texture is in UV coordinate system, the Y is
    // therefore, North direction is -ve and south is +ve.
    vec3 rgbNW = texture(g_Texture, TexCoord + vec2(-1,-1) * g_f2FrameRcpFrame).rgb;
    vec3 rgbNE = texture(g_Texture, TexCoord + vec2(+1,-1) * g_f2FrameRcpFrame).rgb;
    vec3 rgbSW = texture(g_Texture, TexCoord + vec2(-1,+1) * g_f2FrameRcpFrame).rgb;
    vec3 rgbSE = texture(g_Texture, TexCoord + vec2(+1,+1) * g_f2FrameRcpFrame).rgb;
    vec3 rgbM  = texture(g_Texture, TexCoord).rgb;

    float lumaNW = FxaaLuma(rgbNW);   // Top-Left
    float lumaNE = FxaaLuma(rgbNE);   // Top-Right
    float lumaSW = FxaaLuma(rgbSW);   // Bottom-Left
    float lumaSE = FxaaLuma(rgbSE);   // Bottom-Right
    float lumaM  = FxaaLuma(rgbM);    // Middle

    // Get the edge direction, since the y components are inverted
    // be careful to invert the resultant x
    vec2 dir;
    dir.x = -((lumaNW + lumaNE) - (lumaSW + lumaSE));
    dir.y = +((lumaNW + lumaSW) - (lumaNE + lumaSE));

    // Now, we know which direction to blur,
    // But far we need to blur in the direction?
    float dirReduce = max((lumaNW + lumaNE + lumaSW + lumaSE) * (0.25 * FXAA_REDUCE_MUL), FXAA_REDUCE_MIN);
    float rcpDirMin = 1.0/(min(abs(dir.x), abs(dir.y)) + dirReduce);

    dir = min(vec2(FXAA_SPAN_MAX), max(vec2(-FXAA_SPAN_MAX), dir*rcpDirMin))/FBS;

    vec3 rgbA = 0.5 * (texture(g_Texture, TexCoord + dir*(1.0/3.0-0.5)).rgb +
                       texture(g_Texture, TexCoord + dir*(2.0/3.0-0.5)).rgb);
    vec3 rgbB = rgbA*0.5 + 0.25*(texture(g_Texture, TexCoord + dir*(0.0/3.0 - 0.5)).rgb +
                                 texture(g_Texture, TexCoord + dir*(3.0/3.0 - 0.5)).rgb);

    float lumaB = FxaaLuma(rgbB);
    float lumaMin = min(lumaM, min(min(lumaNW, lumaNE), min(lumaSW, lumaSE)));
    float lumaMax = max(lumaM, max(max(lumaNW, lumaNE), max(lumaSW, lumaSE)));

    if((lumaB<lumaMin) || (lumaB>lumaMax))
    {
        outColor = vec4(rgbA, 1.0);
    }
    else
    {
        outColor = vec4(rgbB, 1.0);
    }
}
