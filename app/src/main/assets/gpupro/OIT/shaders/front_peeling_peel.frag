#include "shade_fragment.glsl"

layout(location=0) out vec4 outColor;

uniform sampler2D DepthTex;
uniform vec2 ViewSize;
vec4 ShadeFragment();

void main(void)
{
    // Bit-exact comparison between FP32 z-buffer and fragment depth
    float frontDepth = texture(DepthTex, gl_FragCoord.xy/ViewSize).r;
    if (gl_FragCoord.z <= frontDepth) {
        discard;
    }
    
    // Shade all the fragments behind the z-buffer
    vec4 color = ShadeFragment();
    outColor = vec4(color.rgb * color.a, color.a);
}
