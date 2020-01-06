#include "shade_fragment.glsl"

layout(location=0) out vec4 outColor;


void main(void)
{
    vec4 color = ShadeFragment();
    outColor = vec4(color.rgb * color.a, 1.0 - color.a);
}
