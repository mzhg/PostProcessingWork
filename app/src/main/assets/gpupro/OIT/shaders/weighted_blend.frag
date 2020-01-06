#include "shade_fragment.glsl"

uniform float uDepthScale;

layout(location=0) out vec4 oSumColor;
layout(location=1) out vec4 oSumWeight;

void main(void)
{
    vec4 color = ShadeFragment();

    // Assuming that the projection matrix is a perspective projection
    // gl_FragCoord.w returns the inverse of the oPos.w register from the vertex shader
    float viewDepth = abs(1.0 / gl_FragCoord.w);

    // Tuned to work well with FP16 accumulation buffers and 0.001 < linearDepth < 2.5
    // See Equation (9) from http://jcgt.org/published/0002/02/09/
    float linearDepth = viewDepth * uDepthScale;
    float weight = clamp(0.03 / (1e-5 + pow(linearDepth, 4.0)), 1e-2, 3e3);

    oSumColor = vec4(color.rgb * color.a, color.a) * weight;
    oSumWeight = vec4(color.a);
}
